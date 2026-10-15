package com.dmi.canteen.food;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByActiveTrue();
    List<FoodItem> findByCategoryIdAndActiveTrue(Long categoryId);
    List<FoodItem> findByCounterIdAndActiveTrue(Long counterId);
    Optional<FoodItem> findByNameIgnoreCase(String name);
}
