package com.dmi.canteen.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    Optional<Feedback> findByOrderIdAndOrderItemId(Long orderId, Long orderItemId);

    boolean existsByOrderIdAndOrderItemId(Long orderId, Long orderItemId);

    @Query("SELECT COALESCE(AVG(f.rating), 0.0) FROM Feedback f")
    Double getAverageRating();

    @Query("SELECT f.rating, COUNT(f) FROM Feedback f GROUP BY f.rating ORDER BY f.rating DESC")
    List<Object[]> getRatingDistribution();

    @Query("SELECT f.orderItem.foodItem.id, f.orderItem.foodItem.name, AVG(f.rating), COUNT(f) " +
           "FROM Feedback f WHERE f.orderItem IS NOT NULL " +
           "GROUP BY f.orderItem.foodItem.id, f.orderItem.foodItem.name")
    List<Object[]> getFoodRatingStats();
}
