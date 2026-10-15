package com.dmi.canteen.report;

import com.dmi.canteen.order.OrderItem;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentMethod;
import com.dmi.canteen.payment.PaymentRepository;
import com.dmi.canteen.payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");

    private final PaymentRepository paymentRepository;

    public ReportService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public static class RevenueReportDto {
        private String period;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal onlineRevenue = BigDecimal.ZERO;
        private BigDecimal cashRevenue = BigDecimal.ZERO;
        private long totalOrders = 0;
        private Map<String, BigDecimal> counterWiseSales = new HashMap<>();
        private Map<String, BigDecimal> categoryWiseSales = new HashMap<>();
        private List<FoodItemSalesDto> topSellingItems = new ArrayList<>();
        private List<PaymentTransactionDto> completedTransactions = new ArrayList<>();

        public RevenueReportDto() {}

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getOnlineRevenue() { return onlineRevenue; }
        public void setOnlineRevenue(BigDecimal onlineRevenue) { this.onlineRevenue = onlineRevenue; }
        public BigDecimal getCashRevenue() { return cashRevenue; }
        public void setCashRevenue(BigDecimal cashRevenue) { this.cashRevenue = cashRevenue; }
        public long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
        public Map<String, BigDecimal> getCounterWiseSales() { return counterWiseSales; }
        public void setCounterWiseSales(Map<String, BigDecimal> counterWiseSales) { this.counterWiseSales = counterWiseSales; }
        public Map<String, BigDecimal> getCategoryWiseSales() { return categoryWiseSales; }
        public void setCategoryWiseSales(Map<String, BigDecimal> categoryWiseSales) { this.categoryWiseSales = categoryWiseSales; }
        public List<FoodItemSalesDto> getTopSellingItems() { return topSellingItems; }
        public void setTopSellingItems(List<FoodItemSalesDto> topSellingItems) { this.topSellingItems = topSellingItems; }
        public List<PaymentTransactionDto> getCompletedTransactions() { return completedTransactions; }
        public void setCompletedTransactions(List<PaymentTransactionDto> completedTransactions) { this.completedTransactions = completedTransactions; }
    }

    public static class FoodItemSalesDto {
        private String itemName;
        private int totalQuantity;
        private BigDecimal totalRevenue;

        public FoodItemSalesDto(String itemName, int totalQuantity, BigDecimal totalRevenue) {
            this.itemName = itemName;
            this.totalQuantity = totalQuantity;
            this.totalRevenue = totalRevenue;
        }

        public String getItemName() { return itemName; }
        public int getTotalQuantity() { return totalQuantity; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
    }

    public static class PaymentTransactionDto {
        private Long paymentId;
        private String orderNumber;
        private String customerName;
        private PaymentMethod method;
        private PaymentStatus status;
        private BigDecimal amount;
        private String utrReference;
        private LocalDateTime paymentTime;

        public PaymentTransactionDto() {}

        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public PaymentMethod getMethod() { return method; }
        public void setMethod(PaymentMethod method) { this.method = method; }
        public PaymentStatus getStatus() { return status; }
        public void setStatus(PaymentStatus status) { this.status = status; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getUtrReference() { return utrReference; }
        public void setUtrReference(String utrReference) { this.utrReference = utrReference; }
        public LocalDateTime getPaymentTime() { return paymentTime; }
        public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
    }

    @Transactional(readOnly = true)
    public RevenueReportDto generateReport(String periodType, LocalDate customStartDate, LocalDate customEndDate) {
        LocalDate today = LocalDate.now(KOLKATA_ZONE);
        LocalDateTime start;
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        if ("weekly".equalsIgnoreCase(periodType)) {
            start = today.minusDays(7).atStartOfDay();
        } else if ("monthly".equalsIgnoreCase(periodType)) {
            start = today.minusDays(30).atStartOfDay();
        } else if ("custom".equalsIgnoreCase(periodType) && customStartDate != null && customEndDate != null) {
            start = customStartDate.atStartOfDay();
            end = customEndDate.plusDays(1).atStartOfDay();
        } else {
            // Default: daily (today)
            start = today.atStartOfDay();
            periodType = "daily";
        }

        RevenueReportDto report = new RevenueReportDto();
        report.setPeriod(periodType);
        report.setStartDate(start);
        report.setEndDate(end);

        BigDecimal total = paymentRepository.sumTotalRevenueBetween(start, end);
        BigDecimal online = paymentRepository.sumOnlineRevenueBetween(start, end);
        BigDecimal cash = paymentRepository.sumCashRevenueBetween(start, end);
        long count = paymentRepository.countCompletedSalesBetween(start, end);

        report.setTotalRevenue(total != null ? total : BigDecimal.ZERO);
        report.setOnlineRevenue(online != null ? online : BigDecimal.ZERO);
        report.setCashRevenue(cash != null ? cash : BigDecimal.ZERO);
        report.setTotalOrders(count);

        List<Payment> payments = paymentRepository.findCompletedPaymentsBetween(start, end);

        Map<String, BigDecimal> counterSales = new HashMap<>();
        Map<String, BigDecimal> categorySales = new HashMap<>();
        Map<String, Integer> itemQuantities = new HashMap<>();
        Map<String, BigDecimal> itemRevenues = new HashMap<>();
        List<PaymentTransactionDto> transactions = new ArrayList<>();

        for (Payment p : payments) {
            PaymentTransactionDto tx = new PaymentTransactionDto();
            tx.setPaymentId(p.getId());
            tx.setOrderNumber(p.getOrder().getOrderNumber());
            tx.setCustomerName(p.getOrder().getUser().getFullName());
            tx.setMethod(p.getPaymentMethod());
            tx.setStatus(p.getPaymentStatus());
            tx.setAmount(p.getAmount());
            tx.setUtrReference(p.getUtrReference());
            tx.setPaymentTime(p.getPaymentTime());
            transactions.add(tx);

            if (p.getOrder().getItems() != null) {
                for (OrderItem item : p.getOrder().getItems()) {
                    String counter = item.getCounterName() != null ? item.getCounterName() : "General Counter";
                    counterSales.merge(counter, item.getSubtotal(), BigDecimal::add);

                    if (item.getFoodItem() != null && item.getFoodItem().getCategory() != null) {
                        String category = item.getFoodItem().getCategory().getName();
                        categorySales.merge(category, item.getSubtotal(), BigDecimal::add);
                    }

                    itemQuantities.merge(item.getItemName(), item.getQuantity(), Integer::sum);
                    itemRevenues.merge(item.getItemName(), item.getSubtotal(), BigDecimal::add);
                }
            }
        }

        report.setCounterWiseSales(counterSales);
        report.setCategoryWiseSales(categorySales);
        report.setCompletedTransactions(transactions);

        List<FoodItemSalesDto> topItems = itemQuantities.entrySet().stream()
                .map(e -> new FoodItemSalesDto(e.getKey(), e.getValue(), itemRevenues.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .sorted((a, b) -> Integer.compare(b.getTotalQuantity(), a.getTotalQuantity()))
                .limit(10)
                .collect(Collectors.toList());
        report.setTopSellingItems(topItems);

        return report;
    }

    /**
     * Exports completed sales report as CSV text.
     */
    @Transactional(readOnly = true)
    public String generateCsvReport(String period, LocalDate customStartDate, LocalDate customEndDate) {
        RevenueReportDto report = generateReport(period, customStartDate, customEndDate);

        StringBuilder csv = new StringBuilder();
        csv.append("DMI COLLEGE OF ENGINEERING - CANTEEN REVENUE & SALES REPORT\n");
        csv.append("Period: ").append(report.getPeriod()).append("\n");
        csv.append("Generated At: ").append(LocalDateTime.now(KOLKATA_ZONE)).append("\n");
        csv.append("Total Revenue: Rs. ").append(report.getTotalRevenue()).append("\n");
        csv.append("Online (UPI) Revenue: Rs. ").append(report.getOnlineRevenue()).append("\n");
        csv.append("Cash Revenue: Rs. ").append(report.getCashRevenue()).append("\n");
        csv.append("Completed Orders: ").append(report.getTotalOrders()).append("\n\n");

        csv.append("TRANSACTIONS\n");
        csv.append("Payment ID,Order Number,Customer Name,Method,Status,Amount,UTR Reference,Time\n");
        for (PaymentTransactionDto tx : report.getCompletedTransactions()) {
            csv.append(tx.getPaymentId()).append(",")
                    .append(tx.getOrderNumber()).append(",")
                    .append("\"").append(tx.getCustomerName()).append("\",")
                    .append(tx.getMethod()).append(",")
                    .append(tx.getStatus()).append(",")
                    .append(tx.getAmount()).append(",")
                    .append(tx.getUtrReference() != null ? tx.getUtrReference() : "N/A").append(",")
                    .append(tx.getPaymentTime()).append("\n");
        }

        csv.append("\nTOP SELLING FOOD ITEMS\n");
        csv.append("Item Name,Quantity Sold,Total Revenue\n");
        for (FoodItemSalesDto item : report.getTopSellingItems()) {
            csv.append("\"").append(item.getItemName()).append("\",")
                    .append(item.getTotalQuantity()).append(",")
                    .append(item.getTotalRevenue()).append("\n");
        }

        return csv.toString();
    }
}
