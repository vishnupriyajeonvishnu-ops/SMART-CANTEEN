package com.dmi.canteen.chatbot;

import com.dmi.canteen.feedback.FeedbackRepository;
import com.dmi.canteen.food.FoodItemDto;
import com.dmi.canteen.inventory.Inventory;
import com.dmi.canteen.inventory.InventoryService;
import com.dmi.canteen.menu.DailyMenuService;
import com.dmi.canteen.order.Order;
import com.dmi.canteen.order.OrderRepository;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentRepository;
import com.dmi.canteen.payment.PaymentService;
import com.dmi.canteen.report.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SmartChatbotServiceImpl implements ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(SmartChatbotServiceImpl.class);

    private final DailyMenuService dailyMenuService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final ReportService reportService;
    private final FeedbackRepository feedbackRepository;
    private final PaymentService paymentService;

    @Value("${llm.api-key:}")
    private String llmApiKey;

    @Value("${llm.model:gemini-1.5-flash}")
    private String llmModel;

    public SmartChatbotServiceImpl(DailyMenuService dailyMenuService,
                                   OrderRepository orderRepository,
                                   PaymentRepository paymentRepository,
                                   InventoryService inventoryService,
                                   ReportService reportService,
                                   FeedbackRepository feedbackRepository,
                                   PaymentService paymentService) {
        this.dailyMenuService = dailyMenuService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
        this.reportService = reportService;
        this.feedbackRepository = feedbackRepository;
        this.paymentService = paymentService;
    }

    @Override
    public ChatbotResponse processMessage(String message, Long userId, String role) {
        if (message == null || message.trim().isEmpty()) {
            return getDefaultWelcome(role);
        }

        String query = message.trim().toLowerCase();

        // 1. Role: MANAGER specific queries
        if ("ROLE_MANAGER".equalsIgnoreCase(role)) {
            if (query.contains("revenue") || query.contains("sales") || query.contains("collection") || query.contains("today's revenue")) {
                ReportService.RevenueReportDto report = reportService.generateReport("daily", null, null);
                String reply = String.format("📊 **Today's Revenue Overview:**\n• Total Revenue: **₹%s**\n• Online (UPI): **₹%s**\n• Cash: **₹%s**\n• Orders Completed: **%d**",
                        report.getTotalRevenue(), report.getOnlineRevenue(), report.getCashRevenue(), report.getTotalOrders());
                return new ChatbotResponse(reply, List.of("Online vs Cash", "Top Selling Item", "Average Rating"), "REVENUE_INQUIRY");
            }
            if (query.contains("online") || query.contains("cash") || query.contains("breakdown")) {
                ReportService.RevenueReportDto report = reportService.generateReport("daily", null, null);
                String reply = String.format("💳 **Payment Method Breakdown Today:**\n• UPI/GPay: ₹%s\n• Cash Counter: ₹%s",
                        report.getOnlineRevenue(), report.getCashRevenue());
                return new ChatbotResponse(reply, List.of("Today's Revenue", "Top Selling Item", "Export CSV"), "PAYMENT_BREAKDOWN");
            }
            if (query.contains("top") || query.contains("popular") || query.contains("best")) {
                ReportService.RevenueReportDto report = reportService.generateReport("daily", null, null);
                if (report.getTopSellingItems().isEmpty()) {
                    return new ChatbotResponse("No food sales recorded yet today.", List.of("Today's Revenue", "Average Rating"), "TOP_ITEMS");
                }
                StringBuilder sb = new StringBuilder("🏆 **Top Selling Foods Today:**\n");
                for (ReportService.FoodItemSalesDto item : report.getTopSellingItems().stream().limit(5).toList()) {
                    sb.append("• ").append(item.getItemName()).append(": **").append(item.getTotalQuantity()).append(" sold** (₹").append(item.getTotalRevenue()).append(")\n");
                }
                return new ChatbotResponse(sb.toString(), List.of("Today's Revenue", "Average Rating"), "TOP_ITEMS");
            }
            if (query.contains("rating") || query.contains("feedback") || query.contains("review")) {
                Double avg = feedbackRepository.getAverageRating();
                long count = feedbackRepository.count();
                String reply = String.format("⭐ **Canteen Feedback Summary:**\n• Average Rating: **%.1f / 5.0**\n• Total Reviews: **%d**",
                        (avg != null ? avg : 0.0), count);
                return new ChatbotResponse(reply, List.of("Today's Revenue", "Top Selling Item"), "RATING_SUMMARY");
            }
        }

        // 2. Role: STAFF specific queries
        if ("ROLE_STAFF".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role)) {
            if (query.contains("stock") || query.contains("low stock") || query.contains("inventory")) {
                List<Inventory> lowStock = inventoryService.getLowStockAlerts();
                if (lowStock.isEmpty()) {
                    return new ChatbotResponse("✅ All inventory items are currently stocked above their minimum thresholds.",
                            List.of("Pending Verifications", "Today's Orders"), "STOCK_STATUS");
                }
                StringBuilder sb = new StringBuilder("⚠️ **Low Stock Warning:**\n");
                for (Inventory inv : lowStock) {
                    sb.append("• ").append(inv.getFoodItem().getName()).append(": **").append(inv.getCurrentStock())
                            .append(" left** (Threshold: ").append(inv.getMinimumThreshold()).append(")\n");
                }
                return new ChatbotResponse(sb.toString(), List.of("Pending Verifications", "Today's Orders"), "LOW_STOCK");
            }
            if (query.contains("verify") || query.contains("pending") || query.contains("utr") || query.contains("queue")) {
                List<Payment> pending = paymentService.getPendingVerifications();
                if (pending.isEmpty()) {
                    return new ChatbotResponse("✅ There are no pending payment verifications in the queue right now.",
                            List.of("Low Stock Alerts", "Today's Orders"), "PENDING_VERIFICATIONS");
                }
                StringBuilder sb = new StringBuilder("🔔 **Pending Payment Verifications (" + pending.size() + "):**\n");
                for (Payment p : pending.stream().limit(5).toList()) {
                    sb.append("• Order #").append(p.getOrder().getOrderNumber()).append(" - ₹").append(p.getAmount())
                            .append(" (").append(p.getPaymentMethod()).append(p.getUtrReference() != null ? " UTR: " + p.getUtrReference() : "").append(")\n");
                }
                return new ChatbotResponse(sb.toString(), List.of("Low Stock Alerts", "Today's Orders"), "PENDING_VERIFICATIONS");
            }
        }

        // 3. STUDENT & PUBLIC queries

        // Budget query (e.g. "under 50", "under 100", "under rs 40")
        Pattern pricePattern = Pattern.compile("under\\s*(?:rs\\.?\\s*)?(\\d+)");
        Matcher priceMatcher = pricePattern.matcher(query);
        if (priceMatcher.find()) {
            int maxPrice = Integer.parseInt(priceMatcher.group(1));
            List<FoodItemDto> menu = dailyMenuService.getTodayMenu();
            List<FoodItemDto> budgetItems = menu.stream()
                    .filter(i -> i.getPrice().compareTo(BigDecimal.valueOf(maxPrice)) <= 0)
                    .collect(Collectors.toList());

            if (budgetItems.isEmpty()) {
                return new ChatbotResponse("Sorry, no menu items found under ₹" + maxPrice + " today.",
                        List.of("Today's Menu", "How to pay via UPI", "My Order Status"), "BUDGET_SEARCH");
            }
            StringBuilder sb = new StringBuilder("💰 **Picks under ₹" + maxPrice + " today:**\n");
            for (FoodItemDto item : budgetItems) {
                sb.append("• ").append(item.isVeg() ? "🟢 " : "🔴 ").append(item.getName())
                        .append(" - **₹").append(item.getPrice()).append("** (").append(item.getCounterName()).append(")\n");
            }
            return new ChatbotResponse(sb.toString(), List.of("Today's Menu", "How to pay via UPI"), "BUDGET_SEARCH");
        }

        // Today's menu query
        if (query.contains("menu") || query.contains("available") || query.contains("food") || query.contains("dishes") || query.contains("what can i eat")) {
            List<FoodItemDto> menu = dailyMenuService.getTodayMenu();
            if (menu.isEmpty()) {
                return new ChatbotResponse("Today's canteen menu is being prepared. Please check back in a few minutes!",
                        List.of("How to pay via UPI", "Counters"), "MENU_INQUIRY");
            }
            StringBuilder sb = new StringBuilder("🍽️ **Today's Live Canteen Menu:**\n");
            for (FoodItemDto item : menu.stream().limit(8).toList()) {
                String status = item.getCurrentStock() <= 0 ? "*(Sold Out)*" : "₹" + item.getPrice();
                sb.append("• ").append(item.isVeg() ? "🟢 " : "🔴 ").append(item.getName())
                        .append(" - **").append(status).append("** (").append(item.getCounterName()).append(")\n");
            }
            if (menu.size() > 8) {
                sb.append("...and ").append(menu.size() - 8).append(" more dishes in the canteen menu!");
            }
            return new ChatbotResponse(sb.toString(), List.of("Picks under 50", "Sold out items", "How to pay via UPI"), "MENU_INQUIRY");
        }

        // Sold out items query
        if (query.contains("sold out") || query.contains("out of stock") || query.contains("finished")) {
            List<FoodItemDto> menu = dailyMenuService.getTodayMenu();
            List<FoodItemDto> soldOut = menu.stream().filter(i -> i.getCurrentStock() <= 0).toList();
            if (soldOut.isEmpty()) {
                return new ChatbotResponse("🎉 Great news! All dishes on today's menu are currently in stock.",
                        List.of("Today's Menu", "Picks under 50"), "SOLDOUT_INQUIRY");
            }
            StringBuilder sb = new StringBuilder("❌ **Currently Sold Out:**\n");
            for (FoodItemDto item : soldOut) {
                sb.append("• ").append(item.getName()).append(" (").append(item.getCounterName()).append(")\n");
            }
            return new ChatbotResponse(sb.toString(), List.of("Today's Menu", "Picks under 50"), "SOLDOUT_INQUIRY");
        }

        // Student's Order Status query
        if (query.contains("my order") || query.contains("order status") || query.contains("where is my food") || query.contains("track")) {
            if (userId == null) {
                return new ChatbotResponse("Please log in to your student account to check live order status.",
                        List.of("Login", "Today's Menu", "How to pay via UPI"), "AUTH_REQUIRED");
            }
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (orders.isEmpty()) {
                return new ChatbotResponse("You haven't placed any orders yet today. Browse today's menu to place your first order!",
                        List.of("Today's Menu", "Picks under 50"), "NO_ORDERS");
            }
            Order latest = orders.get(0);
            Optional<Payment> pOpt = paymentRepository.findByOrderId(latest.getId());
            String payStatus = pOpt.map(p -> p.getPaymentStatus().toString()).orElse("PENDING");

            String reply = String.format("📦 **Your Latest Order:**\n• Order No: **%s**\n• Status: **%s**\n• Payment: **%s**\n• Amount: **₹%s**\n• Type: %s",
                    latest.getOrderNumber(), latest.getStatus(), payStatus, latest.getTotalAmount(), latest.getOrderType());
            return new ChatbotResponse(reply, List.of("View All Orders", "Today's Menu", "How to pay via UPI"), "ORDER_STATUS");
        }

        // How to pay via UPI
        if (query.contains("pay") || query.contains("upi") || query.contains("gpay") || query.contains("google pay") || query.contains("utr")) {
            String reply = "📱 **How to Pay with Google Pay / UPI:**\n" +
                    "1. Place your order on the student dashboard.\n" +
                    "2. Scan the dynamic or static QR code displayed on the screen.\n" +
                    "3. Complete the payment in your GPay/PhonePe/PayTM app.\n" +
                    "4. Copy the **12-digit UPI Reference / UTR Number** from your payment receipt.\n" +
                    "5. Paste the 12-digit UTR in the payment screen and submit.\n" +
                    "6. Canteen staff will verify and confirm your order instantly!";
            return new ChatbotResponse(reply, List.of("Today's Menu", "My Order Status", "Counters"), "PAYMENT_HELP");
        }

        // Counters info
        if (query.contains("counter") || query.contains("where to collect") || query.contains("collection")) {
            String reply = "🏪 **DMI Canteen Collection Counters:**\n" +
                    "• **Food Counter (FC-01):** Meals, Biryani, Dosa, Parotta, Fried Rice, Noodles, Snacks.\n" +
                    "• **Tea/Coffee Counter (TC-01):** Filter Coffee, Masala Tea, Badam Milk, Fresh Fruit Juices, Cold Beverages.\n" +
                    "Check the counter badge on your order items to collect from the right counter!";
            return new ChatbotResponse(reply, List.of("Today's Menu", "My Order Status"), "COUNTER_INFO");
        }

        // Generic greeting
        return getDefaultWelcome(role);
    }

    private ChatbotResponse getDefaultWelcome(String role) {
        if ("ROLE_MANAGER".equalsIgnoreCase(role)) {
            return new ChatbotResponse(
                    "👋 Hello Manager! I can provide real-time updates on today's revenue, online vs cash breakdown, top selling dishes, and customer feedback.",
                    List.of("Today's Revenue", "Online vs Cash", "Top Selling Item", "Average Rating"),
                    "WELCOME_MANAGER"
            );
        } else if ("ROLE_STAFF".equalsIgnoreCase(role)) {
            return new ChatbotResponse(
                    "👋 Hello Staff member! I can help you monitor live inventory stock, low-stock warnings, pending UTR payments, and order counts.",
                    List.of("Low Stock Alerts", "Pending Verifications", "Today's Menu"),
                    "WELCOME_STAFF"
            );
        } else {
            return new ChatbotResponse(
                    "👋 Welcome to DMI Canteen Assistant! I can help you check today's menu, budget dishes under ₹50, track your active orders, or explain how to pay via UPI.",
                    List.of("Today's Menu", "Picks under 50", "My Order Status", "How to pay via UPI"),
                    "WELCOME_STUDENT"
            );
        }
    }
}
