package com.dmi.canteen.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PaymentSummaryRepository extends JpaRepository<PaymentSummary, Long> {
    Optional<PaymentSummary> findBySummaryDate(LocalDate summaryDate);
}
