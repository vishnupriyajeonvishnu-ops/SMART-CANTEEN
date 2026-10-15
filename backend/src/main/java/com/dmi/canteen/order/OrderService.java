package com.dmi.canteen.order;

import com.dmi.canteen.billing.Bill;
import com.dmi.canteen.billing.BillRepository;
import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.food.FoodItem;
import com.dmi.canteen.food.FoodItemRepository;
import com.dmi.canteen.inventory.InventoryService;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentRepository;
import com.dmi.canteen.payment.PaymentService;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        FoodItemRepository foodItemRepository,
                        UserRepository userRepository,
                        InventoryService inventoryService,
                        PaymentService paymentService,
                        PaymentRepository paymentRepository,
                        BillRepository billRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.foodItemRepository = foodItemRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
    }

    /**
     * Creates an order with strict stock deduction and idempotency support.
     * Order total calculated solely on the backend.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderDto createOrder(Long userId, CreateOrderRequest req) {
        // 1. Idempotency Check
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent request detected. Returning existing order #{}", existing.get().getId());
                return toDto(existing.get());
            }
        }

        // 2. Fetch User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item.");
        }

        // 3. Aggregate item quantities and fetch food items
        Map<Long, Integer> itemQuantities = new HashMap<>();
        for (OrderItemRequest itemReq : req.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                throw new BusinessException("Quantity must be greater than zero.");
            }
            itemQuantities.merge(itemReq.getFoodItemId(), itemReq.getQuantity(), Integer::sum);
        }

        // 4. Stock check + deduction in ONE @Transactional method with pessimistic lock
        inventoryService.reserveAndDeduct(itemQuantities);

        // 5. Build order and order items with snapshots and calculate total
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long countToday = orderRepository.countOrdersForDay(startOfDay, endOfDay) + 1;
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber = String.format("ORD-%s-%04d", dateStr, countToday);

        // Ensure collision-free unique order ID generation even under concurrent requests
        int attempts = 0;
        while (orderRepository.findByOrderNumber(orderNumber).isPresent() && attempts < 100) {
            countToday++;
            orderNumber = String.format("ORD-%s-%04d", dateStr, countToday);
            attempts++;
        }
        if (orderRepository.findByOrderNumber(orderNumber).isPresent()) {
            orderNumber = String.format("ORD-%s-%04d-%s", dateStr, countToday, UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        }

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setOrderType(req.getOrderType());
        order.setStatus(OrderStatus.PLACED);
        order.setIdempotencyKey(req.getIdempotencyKey());

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
            Long foodItemId = entry.getKey();
            int qty = entry.getValue();

            FoodItem foodItem = foodItemRepository.findById(foodItemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Food item not found: " + foodItemId));

            if (!foodItem.isActive()) {
                throw new BusinessException("Item '" + foodItem.getName() + "' is no longer available.");
            }

            BigDecimal unitPrice = foodItem.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            calculatedTotal = calculatedTotal.add(subtotal);

            String counterName = foodItem.getCounter() != null ? foodItem.getCounter().getName() : "Main Counter";
            OrderItem orderItem = new OrderItem(foodItem, foodItem.getName(), unitPrice, qty, subtotal, counterName);
            order.addItem(orderItem);
        }

        order.setTotalAmount(calculatedTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        Order savedOrder = orderRepository.save(order);

        // 6. Initialize Payment record
        paymentService.initializePayment(savedOrder, req.getPaymentMethod());

        log.info("Order created: #{} ({}) total: ₹{}", savedOrder.getId(), savedOrder.getOrderNumber(), calculatedTotal);
        return toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId, Long userId, boolean isStaffOrManager) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isStaffOrManager && !order.getUser().getId().equals(userId)) {
            throw new BusinessException("You do not have permission to view this order.");
        }

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByIdentifier(String identifier, Long userId, boolean isStaffOrManager) {
        Order order = null;
        try {
            Long numId = Long.parseLong(identifier);
            order = orderRepository.findById(numId).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (order == null) {
            order = orderRepository.findByOrderNumber(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id or number: " + identifier));
        }

        if (!isStaffOrManager && !order.getUser().getId().equals(userId)) {
            throw new BusinessException("You do not have permission to view this order.");
        }

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getMyOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot update status of a cancelled order.");
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        log.info("Order #{} status updated to {}", orderId, newStatus);
        return toDto(updated);
    }

    @Transactional
    public OrderDto cancelOrder(Long orderId, Long userId, boolean isStaff) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isStaff && !order.getUser().getId().equals(userId)) {
            throw new BusinessException("You do not have permission to cancel this order.");
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.READY) {
            throw new BusinessException("Cannot cancel order that is already " + order.getStatus() + ".");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toDto(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        // Restore inventory stock
        Map<Long, Integer> itemsToRestore = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            itemsToRestore.put(item.getFoodItem().getId(), item.getQuantity());
        }
        inventoryService.restoreStock(itemsToRestore);

        log.info("Order #{} cancelled. Stock restored.", orderId);
        return toDto(updated);
    }

    public OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setCustomerName(order.getUser().getFullName());
        dto.setOrderType(order.getOrderType());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());

        // Attach Payment info
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            dto.setPaymentId(payment.getId());
            dto.setPaymentMethod(payment.getPaymentMethod());
            dto.setPaymentStatus(payment.getPaymentStatus());
            dto.setUtrReference(payment.getUtrReference());
        });

        // Attach Bill info
        billRepository.findByOrderId(order.getId()).ifPresent(bill -> {
            dto.setBillId(bill.getId());
            dto.setBillNumber(bill.getBillNumber());
        });

        List<OrderItemDto> itemDtos = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setFoodItemId(item.getFoodItem() != null ? item.getFoodItem().getId() : null);
                itemDto.setItemName(item.getItemName());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setSubtotal(item.getSubtotal());
                itemDto.setCounterName(item.getCounterName());
                itemDtos.add(itemDto);
            }
        }
        dto.setItems(itemDtos);

        return dto;
    }
}
