package com.dmi.canteen.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByUtrReference(String utrReference);

    boolean existsByUtrReference(String utrReference);

    List<Payment> findByPaymentStatusOrderByPaymentTimeDesc(PaymentStatus status);

    List<Payment> findByPaymentStatusInOrderByPaymentTimeDesc(List<PaymentStatus> statuses);

    // Shared definition of completed sale: paymentStatus in (SUCCESS, VERIFIED) and order.status != CANCELLED
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus IN ('SUCCESS', 'VERIFIED') " +
           "AND p.order.status != 'CANCELLED' " +
           "AND p.paymentTime >= :start AND p.paymentTime < :end")
    BigDecimal sumTotalRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus = 'SUCCESS' " +
           "AND p.paymentMethod = 'GPAY_UPI' " +
           "AND p.order.status != 'CANCELLED' " +
           "AND p.paymentTime >= :start AND p.paymentTime < :end")
    BigDecimal sumOnlineRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus = 'VERIFIED' " +
           "AND p.paymentMethod = 'CASH' " +
           "AND p.order.status != 'CANCELLED' " +
           "AND p.paymentTime >= :start AND p.paymentTime < :end")
    BigDecimal sumCashRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM Payment p " +
           "WHERE p.paymentStatus IN ('SUCCESS', 'VERIFIED') " +
           "AND p.order.status != 'CANCELLED' " +
           "AND p.paymentTime >= :start AND p.paymentTime < :end")
    long countCompletedSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT p FROM Payment p " +
           "WHERE p.paymentStatus IN ('SUCCESS', 'VERIFIED') " +
           "AND p.order.status != 'CANCELLED' " +
           "AND p.paymentTime >= :start AND p.paymentTime < :end ORDER BY p.paymentTime DESC")
    List<Payment> findCompletedPaymentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
