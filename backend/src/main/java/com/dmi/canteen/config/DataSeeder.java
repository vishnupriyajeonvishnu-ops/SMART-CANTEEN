package com.dmi.canteen.config;

import com.dmi.canteen.billing.Bill;
import com.dmi.canteen.billing.BillRepository;
import com.dmi.canteen.category.Category;
import com.dmi.canteen.category.CategoryRepository;
import com.dmi.canteen.counter.Counter;
import com.dmi.canteen.counter.CounterRepository;
import com.dmi.canteen.feedback.Feedback;
import com.dmi.canteen.feedback.FeedbackRepository;
import com.dmi.canteen.feedback.FeedbackType;
import com.dmi.canteen.food.FoodItem;
import com.dmi.canteen.food.FoodItemRepository;
import com.dmi.canteen.inventory.Inventory;
import com.dmi.canteen.inventory.InventoryRepository;
import com.dmi.canteen.menu.DailyMenu;
import com.dmi.canteen.menu.DailyMenuRepository;
import com.dmi.canteen.order.*;
import com.dmi.canteen.payment.*;
import com.dmi.canteen.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final CounterRepository counterRepository;
    private final CategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final InventoryRepository inventoryRepository;
    private final DailyMenuRepository dailyMenuRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentSummaryRepository paymentSummaryRepository;
    private final BillRepository billRepository;
    private final FeedbackRepository feedbackRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      StaffRepository staffRepository,
                      StudentRepository studentRepository,
                      CounterRepository counterRepository,
                      CategoryRepository categoryRepository,
                      FoodItemRepository foodItemRepository,
                      InventoryRepository inventoryRepository,
                      DailyMenuRepository dailyMenuRepository,
                      OrderRepository orderRepository,
                      OrderItemRepository orderItemRepository,
                      PaymentRepository paymentRepository,
                      PaymentSummaryRepository paymentSummaryRepository,
                      BillRepository billRepository,
                      FeedbackRepository feedbackRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.counterRepository = counterRepository;
        this.categoryRepository = categoryRepository;
        this.foodItemRepository = foodItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.dailyMenuRepository = dailyMenuRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.paymentSummaryRepository = paymentSummaryRepository;
        this.billRepository = billRepository;
        this.feedbackRepository = feedbackRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedStudentUsers();
        seedCounters();
        seedCategories();
        seedFoodItemsAndInventory();
        seedDailyMenu();
        seedBillsAndReports();
        seedFeedbacks();
        log.info("Data seeding completed successfully for DMI College Canteen.");
    }

    private void seedUsers() {
        // Staff Demo Account: staff@dmicanteen.com / Staff@123 / username: staff_demo
        if (!userRepository.existsByUsername("staff_demo")) {
            User staffUser = new User();
            staffUser.setUsername("staff_demo");
            staffUser.setEmail("staff@dmicanteen.com");
            staffUser.setFullName("DMI Canteen Operator");
            staffUser.setPhone("9876543210");
            staffUser.setPassword(passwordEncoder.encode("Staff@123"));
            staffUser.setRole(Role.ROLE_STAFF);
            staffUser.setActive(true);
            User savedStaffUser = userRepository.save(staffUser);

            Staff staff = new Staff(savedStaffUser, "DMI-STF-001", "Canteen Supervisor");
            staffRepository.save(staff);
            log.info("Seeded demo Staff account: staff@dmicanteen.com / Staff@123");
        }

        // Management Demo Account: management@dmicanteen.com / Manager@123 / username: manager_demo
        if (!userRepository.existsByUsername("manager_demo")) {
            User managerUser = new User();
            managerUser.setUsername("manager_demo");
            managerUser.setEmail("management@dmicanteen.com");
            managerUser.setFullName("Canteen General Manager");
            managerUser.setPhone("9876543211");
            managerUser.setPassword(passwordEncoder.encode("Manager@123"));
            managerUser.setRole(Role.ROLE_MANAGER);
            managerUser.setActive(true);
            User savedManagerUser = userRepository.save(managerUser);

            Staff managerStaff = new Staff(savedManagerUser, "DMI-MGR-001", "Head of Canteen Operations");
            staffRepository.save(managerStaff);
            log.info("Seeded demo Manager account: management@dmicanteen.com / Manager@123");
        }
    }

    private void seedCounters() {
        if (counterRepository.count() == 0) {
            counterRepository.save(new Counter("Food Counter", "FC-01"));
            counterRepository.save(new Counter("Tea/Coffee Counter", "TC-01"));
            log.info("Seeded counters: Food Counter (FC-01) and Tea/Coffee Counter (TC-01)");
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new Category("Breakfast", "Fresh morning breakfast items and tiffin"));
            categoryRepository.save(new Category("Lunch & Meals", "Nutritious and delicious hot lunch options"));
            categoryRepository.save(new Category("Evening Snacks", "Crispy hot evening refreshments"));
            categoryRepository.save(new Category("Beverages & Drinks", "Hot tea, filter coffee and chilled drinks"));
            log.info("Seeded food categories.");
        }
    }

    private void seedFoodItemsAndInventory() {
        if (foodItemRepository.count() == 0) {
            Counter foodCounter = counterRepository.findByNameIgnoreCase("Food Counter")
                    .orElseGet(() -> counterRepository.save(new Counter("Food Counter", "FC-01")));
            Counter drinkCounter = counterRepository.findByNameIgnoreCase("Tea/Coffee Counter")
                    .orElseGet(() -> counterRepository.save(new Counter("Tea/Coffee Counter", "TC-01")));

            Category breakfast = categoryRepository.findByNameIgnoreCase("Breakfast").orElse(null);
            Category lunch = categoryRepository.findByNameIgnoreCase("Lunch & Meals").orElse(null);
            Category snacks = categoryRepository.findByNameIgnoreCase("Evening Snacks").orElse(null);
            Category beverages = categoryRepository.findByNameIgnoreCase("Beverages & Drinks").orElse(null);

            Object[][] foodData = new Object[][]{
                    // Breakfast
                    {"Idli with Sambar & Chutney", breakfast, foodCounter, "Steamed soft rice cakes served with hot lentil sambar and coconut chutney", "30.00", true, 60, "assets/dishes/idli.jpg"},
                    {"Medu Vada (2 pcs)", breakfast, foodCounter, "Golden crispy lentil doughnuts spiced with black pepper and ginger", "30.00", true, 50, "assets/dishes/vada.jpg"},
                    {"Ghee Masala Dosa", breakfast, foodCounter, "Crispy golden crepe roasted with pure desi ghee and spiced potato masala", "55.00", true, 45, "assets/dishes/dosa.jpg"},
                    {"Poori Masala (3 pcs)", breakfast, foodCounter, "Puffy deep-fried wheat breads served with rich potato bhaji", "45.00", true, 40, "assets/dishes/poori.jpg"},
                    {"Ven Pongal & Vada", breakfast, foodCounter, "Traditional rice and moong dal porridge tempered with cumin, pepper, and cashews", "50.00", true, 35, "assets/dishes/pongal.jpg"},

                    // Lunch & Meals
                    {"South Indian Veg Thali", lunch, foodCounter, "Rice, Sambar, Rasam, Kara Kuzhambu, Kootu, Poriyal, Appalam, Curd and Sweet", "80.00", true, 50, "assets/dishes/meals.jpg"},
                    {"Special Chicken Biryani", lunch, foodCounter, "Fragrant Seeraga Samba rice cooked with tender chicken and authentic spices", "130.00", false, 65, "assets/dish-biryani.jpg"},
                    {"Egg Fried Rice", lunch, foodCounter, "Wok-tossed basmati rice with scrambled farm eggs and garden spring onions", "80.00", false, 40, "assets/dish-fried-rice.jpg"},
                    {"Veg Fried Rice & Manchurian", lunch, foodCounter, "Classic vegetable fried rice paired with savory vegetable manchurian gravy", "85.00", true, 40, "assets/dish-noodles.jpg"},
                    {"Malabar Parotta with Veg Kurma", lunch, foodCounter, "Flaky layered parottas served with fragrant vegetable coconut kurma", "50.00", true, 50, "assets/dishes/poori.jpg"},
                    {"Malabar Parotta with Chicken Salna", lunch, foodCounter, "Flaky layered parottas served with rich spiced chicken salna gravy", "80.00", false, 55, "assets/dish-thali.jpg"},
                    {"Curd Rice with Pickle", lunch, foodCounter, "Cooling seasoned yogurt rice with mustard seeds, green chillies, and mango pickle", "40.00", true, 45, "assets/dishes/curd-rice.jpg"},
                    {"Sambar Rice with Potato Fry", lunch, foodCounter, "Comforting hot rice mashed with aromatic sambar, served with crispy spiced potato fry", "50.00", true, 45, "assets/dishes/tomato-rice.jpg"},

                    // Evening Snacks
                    {"Crispy Veg Samosa (2 pcs)", snacks, foodCounter, "Golden crust pastries stuffed with spiced potatoes and green peas", "25.00", true, 70, "assets/dishes/samosa.jpg"},
                    {"Onion Pakoda (100g)", snacks, foodCounter, "Crispy onion fritters seasoned with fennel and curry leaves", "35.00", true, 50, "assets/dishes/samosa.jpg"},
                    {"Egg Puff", snacks, foodCounter, "Flaky baked puff pastry stuffed with hard-boiled egg and onion masala", "25.00", false, 60, "assets/dishes/puffs.jpg"},
                    {"Vegetable Puff", snacks, foodCounter, "Light and crispy baked puff pastry loaded with spiced mixed vegetable filling", "20.00", true, 50, "assets/dishes/puffs.jpg"},
                    {"Bread Omelette", snacks, foodCounter, "Toasted bread slices wrapped in double-egg masala omelette with onions and chillies", "40.00", false, 45, "assets/dishes/sandwich.jpg"},

                    // Beverages
                    {"South Indian Filter Coffee", beverages, drinkCounter, "Authentic decoction brewed fresh with frothy frothed milk", "20.00", true, 100, "assets/dishes/coffee.jpg"},
                    {"Masala Chai / Ginger Tea", beverages, drinkCounter, "Strong Assam tea infused with fresh ginger, cardamom and aromatic spices", "15.00", true, 120, "assets/dishes/tea.jpg"},
                    {"Chilled Badam Milk", beverages, drinkCounter, "Rich milk blended with almonds, saffron and cardamom, served ice-cold", "30.00", true, 50, "assets/dishes/rose-milk.jpg"},
                    {"Fresh Lime Soda (Sweet/Salt)", beverages, drinkCounter, "Refreshing fresh lime juice infused with chilled club soda", "25.00", true, 60, "assets/dishes/fresh-lime-juice.jpg"},
                    {"Rose Milk", beverages, drinkCounter, "Chilled creamy milk infused with fragrant rose syrup and sabja seeds", "30.00", true, 50, "assets/dishes/rose-milk.jpg"}
            };

            for (Object[] f : foodData) {
                String name = (String) f[0];
                Category cat = (Category) f[1];
                Counter cnt = (Counter) f[2];
                String desc = (String) f[3];
                BigDecimal price = new BigDecimal((String) f[4]);
                boolean veg = (Boolean) f[5];
                int stock = (Integer) f[6];
                String imgUrl = (String) f[7];

                FoodItem item = new FoodItem(name, cat, cnt, desc, price, veg, imgUrl);
                FoodItem saved = foodItemRepository.save(item);

                Inventory inv = new Inventory(saved, stock, 10);
                inventoryRepository.save(inv);
            }
            log.info("Seeded 23 authentic food items with optimized image paths & inventory stock.");
        } else {
            // If items already exist, ensure image URLs are populated with optimized images
            List<FoodItem> existing = foodItemRepository.findAll();
            for (FoodItem item : existing) {
                if (item.getImageUrl() == null || item.getImageUrl().trim().isEmpty() || item.getImageUrl().contains("via.placeholder")) {
                    String n = item.getName().toLowerCase();
                    if (n.contains("idli")) item.setImageUrl("assets/dishes/idli.jpg");
                    else if (n.contains("vada")) item.setImageUrl("assets/dishes/vada.jpg");
                    else if (n.contains("dosa")) item.setImageUrl("assets/dishes/dosa.jpg");
                    else if (n.contains("poori")) item.setImageUrl("assets/dishes/poori.jpg");
                    else if (n.contains("pongal")) item.setImageUrl("assets/dishes/pongal.jpg");
                    else if (n.contains("biryani")) item.setImageUrl("assets/dish-biryani.jpg");
                    else if (n.contains("fried rice")) item.setImageUrl("assets/dish-fried-rice.jpg");
                    else if (n.contains("curd rice")) item.setImageUrl("assets/dishes/curd-rice.jpg");
                    else if (n.contains("thali") || n.contains("meal")) item.setImageUrl("assets/dishes/meals.jpg");
                    else if (n.contains("samosa") || n.contains("pakoda")) item.setImageUrl("assets/dishes/samosa.jpg");
                    else if (n.contains("puff")) item.setImageUrl("assets/dishes/puffs.jpg");
                    else if (n.contains("sandwich") || n.contains("omelette")) item.setImageUrl("assets/dishes/sandwich.jpg");
                    else if (n.contains("coffee")) item.setImageUrl("assets/dishes/coffee.jpg");
                    else if (n.contains("tea") || n.contains("chai")) item.setImageUrl("assets/dishes/tea.jpg");
                    else if (n.contains("lime") || n.contains("soda")) item.setImageUrl("assets/dishes/fresh-lime-juice.jpg");
                    else if (n.contains("rose") || n.contains("badam")) item.setImageUrl("assets/dishes/rose-milk.jpg");
                    else item.setImageUrl("assets/hero-food-platter.jpg");
                    foodItemRepository.save(item);
                }
            }
        }
    }

    private void seedDailyMenu() {
        LocalDate today = LocalDate.now();
        List<FoodItem> items = foodItemRepository.findByActiveTrue();
        for (FoodItem item : items) {
            if (dailyMenuRepository.findByFoodItemIdAndMenuDate(item.getId(), today).isEmpty()) {
                DailyMenu dm = new DailyMenu(item, today, true, "Freshly prepared today");
                dailyMenuRepository.save(dm);
            }
        }
        log.info("Seeded today's daily menu with all active dishes.");
    }

    private void seedStudentUsers() {
        if (!userRepository.existsByUsername("student_demo")) {
            User u1 = new User("student_demo", "student@dmicanteen.com", passwordEncoder.encode("Student@123"), "Sneha S", "9876543212", Role.ROLE_STUDENT);
            u1.setActive(true);
            User s1 = userRepository.save(u1);
            if (!studentRepository.existsByRegisterNumber("310622104901")) {
                studentRepository.save(new Student(s1, "310622104901", "Computer Science", 3));
            }
        }

        if (!userRepository.existsByUsername("rahul_k")) {
            User u2 = new User("rahul_k", "rahul@dmicanteen.com", passwordEncoder.encode("Student@123"), "Rahul K", "9876543213", Role.ROLE_STUDENT);
            u2.setActive(true);
            User s2 = userRepository.save(u2);
            if (!studentRepository.existsByRegisterNumber("310622104915")) {
                studentRepository.save(new Student(s2, "310622104915", "Electronics & Comm", 2));
            }
        }

        if (!userRepository.existsByUsername("priya_m")) {
            User u3 = new User("priya_m", "priya@dmicanteen.com", passwordEncoder.encode("Student@123"), "Priya M", "9876543214", Role.ROLE_STUDENT);
            u3.setActive(true);
            User s3 = userRepository.save(u3);
            if (!studentRepository.existsByRegisterNumber("310622104932")) {
                studentRepository.save(new Student(s3, "310622104932", "Information Technology", 4));
            }
        }

        if (!userRepository.existsByUsername("karthik_v")) {
            User u4 = new User("karthik_v", "karthik@dmicanteen.com", passwordEncoder.encode("Student@123"), "Karthik V", "9876543215", Role.ROLE_STUDENT);
            u4.setActive(true);
            User s4 = userRepository.save(u4);
            if (!studentRepository.existsByRegisterNumber("310622104948")) {
                studentRepository.save(new Student(s4, "310622104948", "Mechanical Engg", 1));
            }
        }
    }

    private static class SeedItem {
        final FoodItem item;
        final int qty;
        SeedItem(FoodItem item, int qty) { this.item = item; this.qty = qty; }
    }

    private void seedBillsAndReports() {
        if (billRepository.count() > 0) return;

        User staff = userRepository.findByUsername("staff_demo").orElse(null);
        User sneha = userRepository.findByUsername("student_demo").orElse(null);
        User rahul = userRepository.findByUsername("rahul_k").orElse(null);
        User priya = userRepository.findByUsername("priya_m").orElse(null);
        User karthik = userRepository.findByUsername("karthik_v").orElse(null);

        if (staff == null || sneha == null || rahul == null || priya == null || karthik == null) return;

        List<FoodItem> allFood = foodItemRepository.findAll();
        FoodItem dosa = allFood.stream().filter(f -> f.getName().toLowerCase().contains("dosa")).findFirst().orElse(null);
        FoodItem coffee = allFood.stream().filter(f -> f.getName().toLowerCase().contains("coffee")).findFirst().orElse(null);
        FoodItem vada = allFood.stream().filter(f -> f.getName().toLowerCase().contains("vada")).findFirst().orElse(null);
        FoodItem chai = allFood.stream().filter(f -> f.getName().toLowerCase().contains("chai")).findFirst().orElse(null);
        FoodItem idli = allFood.stream().filter(f -> f.getName().toLowerCase().contains("idli")).findFirst().orElse(null);
        FoodItem poori = allFood.stream().filter(f -> f.getName().toLowerCase().contains("poori")).findFirst().orElse(null);
        FoodItem roseMilk = allFood.stream().filter(f -> f.getName().toLowerCase().contains("rose milk")).findFirst().orElse(null);
        FoodItem sambarRice = allFood.stream().filter(f -> f.getName().toLowerCase().contains("sambar rice")).findFirst().orElse(null);
        FoodItem samosa = allFood.stream().filter(f -> f.getName().toLowerCase().contains("samosa")).findFirst().orElse(null);
        FoodItem badamMilk = allFood.stream().filter(f -> f.getName().toLowerCase().contains("badam milk")).findFirst().orElse(null);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        String tStr = today.toString().replace("-", "");
        String yStr = yesterday.toString().replace("-", "");

        // Order 1 (Today): Sneha - 2 Dosa + 2 Coffee = 150.00 (UPI)
        createSingleSeedBill(sneha, staff, "ORD-" + tStr + "-0001", "DMI-" + tStr + "-0001",
                PaymentMethod.GPAY_UPI, PaymentStatus.SUCCESS, "428910482910",
                today.atTime(8, 45),
                List.of(new SeedItem(dosa, 2), new SeedItem(coffee, 2)));

        // Order 2 (Today): Rahul - 1 Vada + 1 Chai = 45.00 (Cash)
        createSingleSeedBill(rahul, staff, "ORD-" + tStr + "-0002", "DMI-" + tStr + "-0002",
                PaymentMethod.CASH, PaymentStatus.VERIFIED, null,
                today.atTime(9, 15),
                List.of(new SeedItem(vada, 1), new SeedItem(chai, 1)));

        // Order 3 (Today): Priya - 2 Idli + 1 Coffee = 80.00 (UPI)
        createSingleSeedBill(priya, staff, "ORD-" + tStr + "-0003", "DMI-" + tStr + "-0003",
                PaymentMethod.GPAY_UPI, PaymentStatus.SUCCESS, "519283746192",
                today.atTime(9, 40),
                List.of(new SeedItem(idli, 2), new SeedItem(coffee, 1)));

        // Order 4 (Today): Karthik - 1 Poori + 1 Rose Milk = 75.00 (UPI)
        createSingleSeedBill(karthik, staff, "ORD-" + tStr + "-0004", "DMI-" + tStr + "-0004",
                PaymentMethod.GPAY_UPI, PaymentStatus.SUCCESS, "618294719283",
                today.atTime(10, 10),
                List.of(new SeedItem(poori, 1), new SeedItem(roseMilk, 1)));

        // Order 5 (Yesterday): Sneha - 2 Sambar Rice = 100.00 (Cash)
        createSingleSeedBill(sneha, staff, "ORD-" + yStr + "-0001", "DMI-" + yStr + "-0001",
                PaymentMethod.CASH, PaymentStatus.VERIFIED, null,
                yesterday.atTime(13, 0),
                List.of(new SeedItem(sambarRice, 2)));

        // Order 6 (Yesterday): Rahul - 2 Samosa + 1 Badam Milk = 80.00 (UPI)
        createSingleSeedBill(rahul, staff, "ORD-" + yStr + "-0002", "DMI-" + yStr + "-0002",
                PaymentMethod.GPAY_UPI, PaymentStatus.SUCCESS, "719284719204",
                yesterday.atTime(17, 30),
                List.of(new SeedItem(samosa, 2), new SeedItem(badamMilk, 1)));

        // Order 7 (Pending Verification for Staff Billing to verify):
        createPendingSeedOrder(karthik, "ORD-" + tStr + "-0005",
                PaymentMethod.GPAY_UPI, "829104829105", today.atTime(11, 20),
                List.of(new SeedItem(dosa, 1), new SeedItem(roseMilk, 1)));

        // Update PaymentSummary for today and yesterday
        updatePaymentSummaryForDate(today);
        updatePaymentSummaryForDate(yesterday);

        log.info("Seeded 6 authentic Bills, 1 Pending Order for Staff Verification, and Payment Summaries for Reports.");
    }

    private void createSingleSeedBill(User student, User staff, String orderNumber, String billNumber,
                                      PaymentMethod method, PaymentStatus status, String utr,
                                      LocalDateTime time, List<SeedItem> items) {
        Order order = new Order();
        order.setUser(student);
        order.setOrderNumber(orderNumber);
        order.setOrderType(OrderType.DINE_IN);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(time);
        order.setUpdatedAt(time);

        BigDecimal total = BigDecimal.ZERO;
        for (SeedItem si : items) {
            if (si.item != null) {
                BigDecimal sub = si.item.getPrice().multiply(BigDecimal.valueOf(si.qty));
                total = total.add(sub);
                String cntName = si.item.getCounter() != null ? si.item.getCounter().getName() : "Counter";
                order.addItem(new OrderItem(si.item, si.item.getName(), si.item.getPrice(), si.qty, sub, cntName));
            }
        }
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        Payment payment = new Payment(order, method, status, total);
        payment.setUtrReference(utr);
        payment.setPaymentTime(time);
        payment.setVerifiedByStaff(staff);
        payment.setVerifiedAt(time.plusMinutes(5));
        payment = paymentRepository.save(payment);

        Bill bill = new Bill(billNumber, order, student, payment, total, BigDecimal.ZERO, total);
        bill.setBillDate(time.plusMinutes(5));
        billRepository.save(bill);
    }

    private void createPendingSeedOrder(User student, String orderNumber,
                                        PaymentMethod method, String utr,
                                        LocalDateTime time, List<SeedItem> items) {
        Order order = new Order();
        order.setUser(student);
        order.setOrderNumber(orderNumber);
        order.setOrderType(OrderType.TAKE_AWAY);
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(time);
        order.setUpdatedAt(time);

        BigDecimal total = BigDecimal.ZERO;
        for (SeedItem si : items) {
            if (si.item != null) {
                BigDecimal sub = si.item.getPrice().multiply(BigDecimal.valueOf(si.qty));
                total = total.add(sub);
                String cntName = si.item.getCounter() != null ? si.item.getCounter().getName() : "Counter";
                order.addItem(new OrderItem(si.item, si.item.getName(), si.item.getPrice(), si.qty, sub, cntName));
            }
        }
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        Payment payment = new Payment(order, method, PaymentStatus.PENDING, total);
        payment.setUtrReference(utr);
        payment.setPaymentTime(time);
        paymentRepository.save(payment);
    }

    private void updatePaymentSummaryForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Payment> list = paymentRepository.findCompletedPaymentsBetween(start, end);
        if (list.isEmpty()) return;

        PaymentSummary summary = paymentSummaryRepository.findBySummaryDate(date)
                .orElseGet(() -> new PaymentSummary(date));

        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal onlineRev = BigDecimal.ZERO;
        BigDecimal cashRev = BigDecimal.ZERO;

        for (Payment p : list) {
            totalRev = totalRev.add(p.getAmount());
            if (p.getPaymentMethod() == PaymentMethod.GPAY_UPI) {
                onlineRev = onlineRev.add(p.getAmount());
            } else {
                cashRev = cashRev.add(p.getAmount());
            }
        }

        summary.setTotalRevenue(totalRev);
        summary.setTotalOnlineAmount(onlineRev);
        summary.setTotalCashAmount(cashRev);
        summary.setTotalOrdersCount(list.size());
        paymentSummaryRepository.save(summary);
    }

    private void seedFeedbacks() {
        if (feedbackRepository.count() > 0) return;

        User sneha = userRepository.findByUsername("student_demo").orElse(null);
        User rahul = userRepository.findByUsername("rahul_k").orElse(null);
        User priya = userRepository.findByUsername("priya_m").orElse(null);
        User karthik = userRepository.findByUsername("karthik_v").orElse(null);

        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) return;

        Order o1 = orders.get(0);
        OrderItem it1 = o1.getItems().isEmpty() ? null : o1.getItems().get(0);
        Feedback fb1 = new Feedback(o1, it1, sneha, 5, "Crispy ghee masala dosa was freshly roasted and delicious with coconut chutney!", FeedbackType.FOOD);
        feedbackRepository.save(fb1);

        if (orders.size() > 1) {
            Order o2 = orders.get(1);
            OrderItem it2 = o2.getItems().isEmpty() ? null : o2.getItems().get(0);
            Feedback fb2 = new Feedback(o2, it2, rahul, 5, "Instant order pickup at counter via token number. Saved so much time during morning break.", FeedbackType.SERVICE);
            feedbackRepository.save(fb2);
        }

        if (orders.size() > 2) {
            Order o3 = orders.get(2);
            OrderItem it3 = o3.getItems().isEmpty() ? null : o3.getItems().get(0);
            Feedback fb3 = new Feedback(o3, it3, priya, 4, "Authentic strong filter coffee with rich decoction. Perfect afternoon energy boost.", FeedbackType.FOOD);
            feedbackRepository.save(fb3);
        }

        if (orders.size() > 3) {
            Order o4 = orders.get(3);
            OrderItem it4 = o4.getItems().isEmpty() ? null : o4.getItems().get(0);
            Feedback fb4 = new Feedback(o4, it4, karthik, 5, "Poori masala was hot and puffy, delicious potato bhaji.", FeedbackType.FOOD);
            feedbackRepository.save(fb4);
        }

        if (orders.size() > 4) {
            Order o5 = orders.get(4);
            OrderItem it5 = o5.getItems().isEmpty() ? null : o5.getItems().get(0);
            Feedback fb5 = new Feedback(o5, it5, sneha, 4, "Quick payment verification and bill printing at counter. Very polite staff.", FeedbackType.SERVICE);
            feedbackRepository.save(fb5);
        }

        log.info("Seeded 5 authentic Student Feedbacks across Food and Service.");
    }
}
