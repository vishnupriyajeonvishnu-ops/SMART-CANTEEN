package com.dmi.canteen.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByOrderId(Long orderId);

    Optional<Bill> findByBillNumber(String billNumber);

    List<Bill> findByUserIdOrderByBillDateDesc(Long userId);

    Optional<Bill> findByIdAndUserId(Long id, Long userId);

    List<Bill> findAllByOrderByBillDateDesc();

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.billDate >= :startOfDay AND b.billDate < :endOfDay")
    long countBillsForDay(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
