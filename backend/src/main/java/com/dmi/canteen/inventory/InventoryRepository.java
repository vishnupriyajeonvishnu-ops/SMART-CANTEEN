package com.dmi.canteen.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByFoodItemId(Long foodItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.foodItem.id = :foodItemId")
    Optional<Inventory> findByFoodItemIdWithLock(@Param("foodItemId") Long foodItemId);

    List<Inventory> findByStatus(StockStatus status);

    @Query("SELECT i FROM Inventory i WHERE i.currentStock <= i.minimumThreshold")
    List<Inventory> findLowStockItems();
}
