package com.dmi.canteen;

import com.dmi.canteen.billing.Bill;
import com.dmi.canteen.billing.BillDto;
import com.dmi.canteen.billing.BillingService;
import com.dmi.canteen.feedback.Feedback;
import com.dmi.canteen.feedback.FeedbackService;
import com.dmi.canteen.food.FoodItem;
import com.dmi.canteen.food.FoodItemRepository;
import com.dmi.canteen.order.*;
import com.dmi.canteen.payment.*;
import com.dmi.canteen.report.ReportService;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class FeaturesVerificationTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Test
    @DisplayName("Verify Dummy Data: Bills, Feedback, and Sales Reports are populated")
    public void testDummyDataPopulated() {
        // 1. Bills
        List<Bill> bills = billingService.getAllBills();
        assertNotNull(bills);
        assertTrue(bills.size() >= 6, "Must have at least 6 seeded bills, found: " + bills.size());
        for (Bill b : bills) {
            assertNotNull(b.getBillNumber());
            assertTrue(b.getBillNumber().startsWith("DMI-"));
            assertNotNull(b.getGrandTotal());
            assertTrue(b.getGrandTotal().compareTo(BigDecimal.ZERO) > 0);
            assertNotNull(b.getOrder());
            assertNotNull(b.getUser());
        }

        // 2. Feedback
        List<Feedback> feedbacks = feedbackService.getAllFeedback();
        assertNotNull(feedbacks);
        assertTrue(feedbacks.size() >= 5, "Must have at least 5 seeded feedbacks, found: " + feedbacks.size());
        for (Feedback fb : feedbacks) {
            assertTrue(fb.getRating() >= 1 && fb.getRating() <= 5);
            assertNotNull(fb.getComment());
            assertFalse(fb.getComment().trim().isEmpty());
            assertNotNull(fb.getFeedbackType());
        }

        // 3. Reports
        ReportService.RevenueReportDto dailyReport = reportService.generateReport("daily", null, null);
        assertNotNull(dailyReport);
        assertTrue(dailyReport.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0, "Daily revenue must be greater than 0");
        assertTrue(dailyReport.getTotalOrders() > 0, "Daily orders count must be greater than 0");
        assertFalse(dailyReport.getTopSellingItems().isEmpty(), "Top selling items must not be empty");

        ReportService.RevenueReportDto weeklyReport = reportService.generateReport("weekly", null, null);
        assertNotNull(weeklyReport);
        assertTrue(weeklyReport.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @Transactional
    @DisplayName("Verify Student Order & Payment -> Transmitted to Staff Billing -> Staff Verifies Bill")
    public void testStudentOrderAndStaffBillingVerification() {
        User student = userRepository.findByUsername("student_demo").orElseThrow();
        User staff = userRepository.findByUsername("staff_demo").orElseThrow();
        FoodItem item = foodItemRepository.findAll().stream().filter(FoodItem::isActive).findFirst().orElseThrow();

        // 1. Student places order
        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setOrderType(OrderType.DINE_IN);
        orderReq.setPaymentMethod(PaymentMethod.GPAY_UPI);
        orderReq.setItems(List.of(new OrderItemRequest(item.getId(), 2)));
        OrderDto createdOrder = orderService.createOrder(student.getId(), orderReq);

        assertNotNull(createdOrder.getId());
        assertNotNull(createdOrder.getOrderNumber());

        // 2. Student submits payment UTR
        String testUtr = "998877665544";
        Payment payment = paymentService.submitUpiUtr(createdOrder.getId(), student.getId(), testUtr);
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertEquals(testUtr, payment.getUtrReference());

        // 3. Order and Bill details are now at Staff Billing queue
        List<Payment> pendingQueue = paymentService.getPendingVerifications();
        boolean inQueue = pendingQueue.stream().anyMatch(p -> p.getOrder().getId().equals(createdOrder.getId()));
        assertTrue(inQueue, "Student order must appear in staff billing verification queue");

        // 4. Staff verifies the bill at the counter
        Bill verifiedBill = paymentService.verifyPayment(payment.getId(), staff.getId());
        assertNotNull(verifiedBill);
        assertNotNull(verifiedBill.getBillNumber());
        assertTrue(verifiedBill.getBillNumber().startsWith("DMI-"));
        assertEquals(createdOrder.getId(), verifiedBill.getOrder().getId());

        // 5. Verify Order is now CONFIRMED and Bill is retrievable
        OrderDto confirmedOrder = orderService.getOrderById(createdOrder.getId(), student.getId(), false);
        assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getStatus());
        assertEquals(PaymentStatus.SUCCESS, confirmedOrder.getPaymentStatus());

        Bill billForOrder = billingService.getBillByOrderId(createdOrder.getId()).orElseThrow();
        assertEquals(verifiedBill.getBillNumber(), billForOrder.getBillNumber());
    }
}
