package com.dmi.canteen;

import com.dmi.canteen.auth.CaptchaService;
import com.dmi.canteen.category.Category;
import com.dmi.canteen.category.CategoryRepository;
import com.dmi.canteen.counter.Counter;
import com.dmi.canteen.counter.CounterRepository;
import com.dmi.canteen.food.FoodItem;
import com.dmi.canteen.food.FoodItemRepository;
import com.dmi.canteen.inventory.Inventory;
import com.dmi.canteen.inventory.InventoryRepository;
import com.dmi.canteen.order.*;
import com.dmi.canteen.payment.PaymentMethod;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CaptchaAndOrderIdIntegrationTest {

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private OrderService orderService;

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
    private PasswordEncoder passwordEncoder;

    private User studentUser;
    private FoodItem samosa;

    @BeforeEach
    public void setup() {
        Counter counter = counterRepository.save(new Counter("Snack Counter", "SC-99"));
        Category category = categoryRepository.save(new Category("Hot Snacks", "Snacks"));
        samosa = foodItemRepository.save(new FoodItem("Samosa", category, counter, "Crispy samosa", BigDecimal.valueOf(15.00), true, ""));
        inventoryRepository.save(new Inventory(samosa, 100, 10));

        studentUser = new User("qa_student_" + System.currentTimeMillis(), "qa_student_" + System.currentTimeMillis() + "@dmi.edu",
                passwordEncoder.encode("Pass@123"), "QA Student", "9876543299", Role.ROLE_STUDENT);
        studentUser = userRepository.save(studentUser);
        studentRepository.save(new Student(studentUser, "310622104999", "IT", 2));
    }

    @Test
    @DisplayName("CAPTCHA: generation, image payload, refresh uniqueness, and validation")
    public void testCaptchaLifecycleAndValidation() {
        // 1. Generate Captcha
        CaptchaService.CaptchaResponse c1 = captchaService.generateCaptcha();
        assertNotNull(c1);
        assertNotNull(c1.getCaptchaId());
        assertNotNull(c1.getImageBase64());
        assertNotNull(c1.getCaptchaImage(), "Both getCaptchaImage and getImageBase64 must return the image");
        assertTrue(c1.getImageBase64().startsWith("data:image/png;base64,"));
        assertEquals(c1.getImageBase64(), c1.getCaptchaImage());

        String c1Code = captchaService.getStoredCode(c1.getCaptchaId());
        assertNotNull(c1Code);
        assertEquals(5, c1Code.length(), "Captcha code must be exactly 5 characters");

        // 2. Refresh invalidates old captcha so it cannot be reused
        CaptchaService.CaptchaResponse c2 = captchaService.generateCaptcha(c1.getCaptchaId());
        assertNotEquals(c1.getCaptchaId(), c2.getCaptchaId(), "Refreshed captcha must have a different ID");
        assertFalse(captchaService.validateCaptcha(c1.getCaptchaId(), c1Code),
                "Old captcha must be invalidated and unusable after refresh");

        String c2Code = captchaService.getStoredCode(c2.getCaptchaId());
        assertNotNull(c2Code);

        // 3. Reject incorrect and empty codes
        assertFalse(captchaService.validateCaptcha(c2.getCaptchaId(), "WRONG"),
                "Incorrect captcha must be rejected");
        assertFalse(captchaService.validateCaptcha(c2.getCaptchaId(), ""),
                "Empty captcha must be rejected");
        assertFalse(captchaService.validateCaptcha(c2.getCaptchaId(), "   "),
                "Whitespace-only captcha must be rejected");

        // 4. Case-insensitivity and whitespace tolerance: lowercase & spaces around valid code
        String testInputWithSpaces = "  " + c2Code.toLowerCase() + "  ";
        assertTrue(captchaService.validateCaptcha(c2.getCaptchaId(), testInputWithSpaces),
                "Case-insensitive input with whitespace must be validated successfully");

        // 5. Cannot be reused after successful verification (single-use)
        assertFalse(captchaService.validateCaptcha(c2.getCaptchaId(), c2Code),
                "Successfully validated captcha must not be reusable");

        // 6. Validation fails for null/expired IDs
        assertFalse(captchaService.validateCaptcha(null, "ANY"));
        assertFalse(captchaService.validateCaptcha("non-existent-id", "ANY"));
    }

    @Test
    @Transactional
    @DisplayName("Order ID: generation, database persistence, DTO aliasing, and lookup")
    public void testOrderCreationAndIdGeneration() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setOrderType(OrderType.TAKE_AWAY);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setIdempotencyKey("idem-test-" + System.currentTimeMillis());
        req.setItems(List.of(new OrderItemRequest(samosa.getId(), 2)));

        OrderDto created = orderService.createOrder(studentUser.getId(), req);

        // Verify reliable order identification
        assertNotNull(created, "Created order DTO must not be null");
        assertNotNull(created.getId(), "Database numeric ID must be populated");
        assertNotNull(created.getOrderId(), "orderId alias must match database id");
        assertEquals(created.getId(), created.getOrderId(), "getOrderId() must equal getId()");
        assertNotNull(created.getOrderNumber(), "Order unique display number must be generated");
        assertTrue(created.getOrderNumber().startsWith("ORD-"), "Order number format should start with prefix");
        assertNotNull(created.getCreatedAt(), "Creation date must be set");
        assertNotNull(created.getOrderDate(), "orderDate alias must be set");

        // Verify lookup by numeric ID and orderNumber
        OrderDto fetchedById = orderService.getOrderByIdentifier(String.valueOf(created.getId()), studentUser.getId(), false);
        assertEquals(created.getId(), fetchedById.getId());
        assertEquals(created.getOrderNumber(), fetchedById.getOrderNumber());

        OrderDto fetchedByNum = orderService.getOrderByIdentifier(created.getOrderNumber(), studentUser.getId(), false);
        assertEquals(created.getId(), fetchedByNum.getId());
        assertEquals(created.getOrderNumber(), fetchedByNum.getOrderNumber());

        // Verify idempotency / duplicate submission protection
        OrderDto duplicateCall = orderService.createOrder(studentUser.getId(), req);
        assertEquals(created.getId(), duplicateCall.getId(), "Repeated submission with same idempotency key must return existing order");
    }

    @Test
    @Transactional
    @DisplayName("Order ID: concurrency uniqueness and collision prevention")
    public void testOrderNumberUniquenessAcrossMultipleOrders() {
        Set<String> orderNumbers = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setOrderType(OrderType.DINE_IN);
            req.setPaymentMethod(PaymentMethod.CASH);
            req.setIdempotencyKey("idem-concurrent-" + i + "-" + System.nanoTime());
            req.setItems(List.of(new OrderItemRequest(samosa.getId(), 1)));

            OrderDto order = orderService.createOrder(studentUser.getId(), req);
            assertNotNull(order.getOrderNumber());
            boolean isNew = orderNumbers.add(order.getOrderNumber());
            assertTrue(isNew, "Order number must be completely unique: " + order.getOrderNumber());
        }
        assertEquals(5, orderNumbers.size());
    }
}
