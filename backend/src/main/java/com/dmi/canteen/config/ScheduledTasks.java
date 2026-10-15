package com.dmi.canteen.config;

import com.dmi.canteen.inventory.InventoryService;
import com.dmi.canteen.order.Order;
import com.dmi.canteen.order.OrderItem;
import com.dmi.canteen.order.OrderRepository;
import com.dmi.canteen.order.OrderStatus;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentMethod;
import com.dmi.canteen.payment.PaymentRepository;
import com.dmi.canteen.payment.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final int EXPIRY_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;

    public ScheduledTasks(OrderRepository orderRepository,
                          PaymentRepository paymentRepository,
                          InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Runs every 60 seconds to clean up unpaid UPI orders that have exceeded the 10-minute payment window.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireUnpaidUpiOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRY_MINUTES);
        List<Order> placedOrders = orderRepository.findExpiredOrders(OrderStatus.PLACED, cutoff);

        for (Order order : placedOrders) {
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(order.getId());
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                // If it's a UPI order with no verified payment or no UTR submitted within 10 minutes
                if (payment.getPaymentMethod() == PaymentMethod.GPAY_UPI &&
                        payment.getPaymentStatus() == PaymentStatus.PENDING &&
                        payment.getUtrReference() == null) {

                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    payment.setRejectionReason("Payment timed out (exceeded 10-minute window)");
                    paymentRepository.save(payment);

                    // Release stock
                    Map<Long, Integer> itemsToRestore = new HashMap<>();
                    for (OrderItem item : order.getItems()) {
                        itemsToRestore.put(item.getFoodItem().getId(), item.getQuantity());
                    }
                    inventoryService.restoreStock(itemsToRestore);

                    log.info("Expired unpaid order #{} ({}) after 10-minute timeout. Stock restored.",
                            order.getId(), order.getOrderNumber());
                }
            }
        }
    }
}
