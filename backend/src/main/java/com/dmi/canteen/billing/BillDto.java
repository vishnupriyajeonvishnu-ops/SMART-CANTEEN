package com.dmi.canteen.billing;

import com.dmi.canteen.order.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BillDto {

    private Long id;
    private String billNumber;
    private Long orderId;
    private String orderType;
    private LocalDateTime billDate;
    private String paymentMethod;
    private String utrNumber;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String studentName;
    private String registerNumber;
    private String counterName;
    private List<BillItemDto> items = new ArrayList<>();

    public BillDto() {}

    public static BillDto fromEntity(Bill bill) {
        if (bill == null) return null;
        BillDto dto = new BillDto();
        dto.setId(bill.getId());
        dto.setBillNumber(bill.getBillNumber());
        dto.setBillDate(bill.getBillDate());
        dto.setTotalAmount(bill.getGrandTotal());
        dto.setTaxAmount(bill.getTaxAmount());

        if (bill.getUser() != null) {
            dto.setStudentName(bill.getUser().getFullName());
            if (bill.getUser().getStudent() != null) {
                dto.setRegisterNumber(bill.getUser().getStudent().getRegisterNumber());
            } else {
                dto.setRegisterNumber(bill.getUser().getUsername());
            }
        }

        if (bill.getPayment() != null) {
            dto.setPaymentMethod(bill.getPayment().getPaymentMethod().name());
            dto.setUtrNumber(bill.getPayment().getUtrReference());
        }

        if (bill.getOrder() != null) {
            dto.setOrderId(bill.getOrder().getId());
            dto.setOrderType(bill.getOrder().getOrderType().name());

            if (bill.getOrder().getItems() != null) {
                List<BillItemDto> itemDtos = bill.getOrder().getItems().stream()
                        .map(BillItemDto::fromEntity)
                        .collect(Collectors.toList());
                dto.setItems(itemDtos);

                String counters = bill.getOrder().getItems().stream()
                        .map(it -> it.getFoodItem().getCounter().getName())
                        .distinct()
                        .collect(Collectors.joining(", "));
                dto.setCounterName(counters);
            }
        }

        return dto;
    }

    public static class BillItemDto {
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;

        public BillItemDto() {}

        public BillItemDto(String itemName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }

        public static BillItemDto fromEntity(OrderItem it) {
            return new BillItemDto(
                    it.getItemName(),
                    it.getQuantity(),
                    it.getUnitPrice(),
                    it.getSubtotal()
            );
        }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public LocalDateTime getBillDate() { return billDate; }
    public void setBillDate(LocalDateTime billDate) { this.billDate = billDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getUtrNumber() { return utrNumber; }
    public void setUtrNumber(String utrNumber) { this.utrNumber = utrNumber; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRegisterNumber() { return registerNumber; }
    public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }

    public String getCounterName() { return counterName; }
    public void setCounterName(String counterName) { this.counterName = counterName; }

    public List<BillItemDto> getItems() { return items; }
    public void setItems(List<BillItemDto> items) { this.items = items; }
}
