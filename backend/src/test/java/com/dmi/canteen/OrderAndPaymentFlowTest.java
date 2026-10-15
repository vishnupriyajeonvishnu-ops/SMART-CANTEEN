package com.dmi.canteen;

import com.dmi.canteen.billing.Bill;
import com.dmi.canteen.billing.BillRepository;
import com.dmi.canteen.category.Category;
import com.dmi.canteen.category.CategoryRepository;
import com.dmi.canteen.counter.Counter;
import com.dmi.canteen.counter.CounterRepository;
import com.dmi.canteen.food.FoodItem;
import com.dmi.canteen.food.FoodItemRepository;
import com.dmi.canteen.inventory.Inventory;
import com.dmi.canteen.inventory.InventoryRepository;
import com.dmi.canteen.order.*;
import com.dmi.canteen.payment.*;
import com.dmi.canteen.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class OrderAndPaymentFlowTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User studentUser;
    private User staffUser;
    private FoodItem dosa;
    private FoodItem coffee;

    @BeforeEach
    public void setup() {
        Counter foodCounter = counterRepository.save(new Counter("Dosa Counter", "DC-01"));
        Counter drinkCounter = counterRepository.save(new Counter("Drink Counter", "DK-01"));

        Category tiffin = categoryRepository.save(new Category("Tiffin", "Morning tiffin"));
        Category drinks = categoryRepository.save(new Category("Drinks", "Beverages"));

        dosa = foodItemRepository.save(new FoodItem("Ghee Roast Dosa", tiffin, foodCounter, "Hot dosa", BigDecimal.valueOf(50.00), true, ""));
        coffee = foodItemRepository.save(new FoodItem("Filter Coffee", drinks, drinkCounter, "Hot coffee", BigDecimal.valueOf(20.00), true, ""));

        inventoryRepository.save(new Inventory(dosa, 30, 5));
        inventoryRepository.save(new Inventory(coffee, 50, 10));

        studentUser = new User("test_student_101", "student101@dmi.edu", passwordEncoder.encode("Secret@123"), "Sneha Student", "9876543212", Role.ROLE_STUDENT);
        studentUser = userRepository.save(studentUser);
        studentRepository.save(new Student(studentUser, "310622104001", "CSE", 3));

        staffUser = new User("test_staff_101", "staff101@dmi.edu", passwordEncoder.encode("Staff@123"), "Staff Operator", "9876543213", Role.ROLE_STAFF);
        staffUser = userRepository.save(staffUser);
    }

    @Test
    @Transactional
    @DisplayName("End-to-end Order, UPI UTR submission, staff verification, and automatic bill generation")
    public void testCompleteOrderAndPaymentFlow() {
        // 1. Student places order (2 Dosas @ 50 + 1 Coffee @ 20 = 120.00)
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.DINE_IN);
        req.setPaymentMethod(PaymentMethod.GPAY_UPI);
        req.setIdempotencyKey("test-idem-" + System.currentTimeMillis());
        req.setItems(List.of(
                new OrderItemRequest(dosa.getId(), 2),
                new OrderItemRequest(coffee.getId(), 1)
        ));

        OrderDto orderDto = orderService.createOrder(studentUser.getId(), req);

        assertNotNull(orderDto);
        assertNotNull(orderDto.getOrderNumber());
        assertEquals(0, new BigDecimal("120.00").compareTo(orderDto.getTotalAmount()), "Total amount must be exactly 120.00");
        assertEquals(OrderStatus.PLACED, orderDto.getStatus());
        assertEquals(PaymentStatus.PENDING, orderDto.getPaymentStatus());

        // 2. Student submits 12-digit UTR reference
        String sampleUtr = "202610149999";
        Payment payment = paymentService.submitUpiUtr(orderDto.getId(), studentUser.getId(), sampleUtr);
        assertEquals(sampleUtr, payment.getUtrReference());
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());

        // 3. Staff verifies payment
        Bill bill = paymentService.verifyPayment(payment.getId(), staffUser.getId());

        assertNotNull(bill);
        assertNotNull(bill.getBillNumber());
        assertTrue(bill.getBillNumber().startsWith("DMI-"));
        assertEquals(0, new BigDecimal("120.00").compareTo(bill.getGrandTotal()), "Bill amount must match order total exactly");

        // 4. Verify order is now CONFIRMED
        OrderDto verifiedOrder = orderService.getOrderById(orderDto.getId(), studentUser.getId(), false);
        assertEquals(OrderStatus.CONFIRMED, verifiedOrder.getStatus());
        assertEquals(PaymentStatus.SUCCESS, verifiedOrder.getPaymentStatus());
    }
}
