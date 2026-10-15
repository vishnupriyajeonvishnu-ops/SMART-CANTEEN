package com.dmi.canteen.payment;

import com.dmi.canteen.billing.Bill;
import com.dmi.canteen.billing.BillingService;
import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.inventory.InventoryService;
import com.dmi.canteen.order.Order;
import com.dmi.canteen.order.OrderItem;
import com.dmi.canteen.order.OrderRepository;
import com.dmi.canteen.order.OrderStatus;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentSummaryRepository paymentSummaryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final BillingService billingService;
    private final PaymentGateway paymentGateway;

    @Value("${upi.id:mahakavibharath02@okaxis}")
    private String upiId;

    @Value("${upi.payee-name:Kavi}")
    private String payeeName;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentSummaryRepository paymentSummaryRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          InventoryService inventoryService,
                          BillingService billingService,
                          PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentSummaryRepository = paymentSummaryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.billingService = billingService;
        this.paymentGateway = paymentGateway;
    }

    public String getUpiId() {
        return upiId;
    }

    public String getPayeeName() {
        return payeeName;
    }

    @Transactional
    public Payment initializePayment(Order order, PaymentMethod method) {
        PaymentStatus initialStatus = (method == PaymentMethod.CASH) ? PaymentStatus.CASH_PENDING : PaymentStatus.PENDING;
        Payment payment = new Payment(order, method, initialStatus, order.getTotalAmount());
        return paymentRepository.save(payment);
    }

    /**
     * Student submits 12-digit UTR reference for GPAY_UPI payment.
     */
    @Transactional
    public Payment submitUpiUtr(Long orderId, Long studentUserId, String utrReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(studentUserId)) {
            throw new BusinessException("You are not authorized to submit payment for this order.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot submit payment for a cancelled order.");
        }

        if (utrReference == null || utrReference.trim().length() < 8) {
            throw new BusinessException("Please provide a valid UTR reference number (minimum 8 characters).");
        }

        String cleanUtr = utrReference.trim();

        // Duplicate UTR protection
        Optional<Payment> existingWithUtr = paymentRepository.findByUtrReference(cleanUtr);
        if (existingWithUtr.isPresent() && !existingWithUtr.get().getOrder().getId().equals(orderId)) {
            throw new BusinessException("This UTR Reference (" + cleanUtr + ") has already been submitted for another order.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for order id: " + orderId));

        payment.setUtrReference(cleanUtr);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING); // Pending staff verification

        return paymentRepository.save(payment);
    }

    /**
     * Staff verifies UPI or Cash payment.
     */
    @Transactional
    public Bill verifyPayment(Long paymentId, Long staffUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found with id: " + staffUserId));

        Order order = payment.getOrder();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is cancelled; cannot verify payment.");
        }

        if (payment.getPaymentMethod() == PaymentMethod.GPAY_UPI) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setPaymentStatus(PaymentStatus.VERIFIED);
        }

        payment.setVerifiedByStaff(staff);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setRejectionReason(null);
        Payment savedPayment = paymentRepository.save(payment);

        // Update Order to CONFIRMED
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Generate Bill automatically
        Bill bill = billingService.generateBillForOrder(order, savedPayment);

        // Update Daily Payment Summary
        updateDailyPaymentSummary(savedPayment);

        log.info("Payment #{} verified by staff #{}. Bill generated: {}", paymentId, staffUserId, bill.getBillNumber());
        return bill;
    }

    /**
     * Staff rejects invalid UTR or unreceived payment.
     */
    @Transactional
    public Payment rejectPayment(Long paymentId, Long staffUserId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found with id: " + staffUserId));

        Order order = payment.getOrder();

        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setVerifiedByStaff(staff);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setRejectionReason(reason != null ? reason : "Payment rejected by canteen staff");
        Payment savedPayment = paymentRepository.save(payment);

        // Cancel order and return stock
        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            // Restore stock back to inventory
            Map<Long, Integer> itemsToRestore = new HashMap<>();
            for (OrderItem item : order.getItems()) {
                itemsToRestore.put(item.getFoodItem().getId(), item.getQuantity());
            }
            inventoryService.restoreStock(itemsToRestore);
            log.info("Payment rejected. Order #{} cancelled and inventory stock restored.", order.getId());
        }

        return savedPayment;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPendingVerifications() {
        return paymentRepository.findByPaymentStatusInOrderByPaymentTimeDesc(
                List.of(PaymentStatus.PENDING, PaymentStatus.CASH_PENDING));
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional
    public void updateDailyPaymentSummary(Payment payment) {
        LocalDate today = LocalDate.now();
        PaymentSummary summary = paymentSummaryRepository.findBySummaryDate(today)
                .orElseGet(() -> new PaymentSummary(today));

        BigDecimal amount = payment.getAmount();
        summary.setTotalRevenue(summary.getTotalRevenue().add(amount));
        if (payment.getPaymentMethod() == PaymentMethod.GPAY_UPI) {
            summary.setTotalOnlineAmount(summary.getTotalOnlineAmount().add(amount));
        } else {
            summary.setTotalCashAmount(summary.getTotalCashAmount().add(amount));
        }
        summary.setTotalOrdersCount(summary.getTotalOrdersCount() + 1);
        paymentSummaryRepository.save(summary);
    }
}
