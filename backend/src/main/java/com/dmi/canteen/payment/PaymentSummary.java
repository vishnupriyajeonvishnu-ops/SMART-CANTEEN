package com.dmi.canteen.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_summary")
public class PaymentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_date", nullable = false, unique = true)
    private LocalDate summaryDate;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_online_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOnlineAmount = BigDecimal.ZERO;

    @Column(name = "total_cash_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCashAmount = BigDecimal.ZERO;

    @Column(name = "total_orders_count", nullable = false)
    private int totalOrdersCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public PaymentSummary() {}

    public PaymentSummary(LocalDate summaryDate) {
        this.summaryDate = summaryDate;
        this.totalRevenue = BigDecimal.ZERO;
        this.totalOnlineAmount = BigDecimal.ZERO;
        this.totalCashAmount = BigDecimal.ZERO;
        this.totalOrdersCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSummaryDate() {
        return summaryDate;
    }

    public void setSummaryDate(LocalDate summaryDate) {
        this.summaryDate = summaryDate;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalOnlineAmount() {
        return totalOnlineAmount;
    }

    public void setTotalOnlineAmount(BigDecimal totalOnlineAmount) {
        this.totalOnlineAmount = totalOnlineAmount;
    }

    public BigDecimal getTotalCashAmount() {
        return totalCashAmount;
    }

    public void setTotalCashAmount(BigDecimal totalCashAmount) {
        this.totalCashAmount = totalCashAmount;
    }

    public int getTotalOrdersCount() {
        return totalOrdersCount;
    }

    public void setTotalOrdersCount(int totalOrdersCount) {
        this.totalOrdersCount = totalOrdersCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
