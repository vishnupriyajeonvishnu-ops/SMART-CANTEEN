package com.dmi.canteen.inventory;

import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.food.FoodItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Inventory> getLowStockAlerts() {
        return inventoryRepository.findLowStockItems();
    }

    @Transactional(readOnly = true)
    public Inventory getByFoodItemId(Long foodItemId) {
        return inventoryRepository.findByFoodItemId(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for food item id: " + foodItemId));
    }

    @Transactional
    public Inventory createInventoryForFood(FoodItem foodItem, int initialStock, int minThreshold) {
        Inventory inventory = new Inventory(foodItem, initialStock, minThreshold);
        return inventoryRepository.save(inventory);
    }

    /**
     * Pessimistic locking stock check and deduction in one transactional method.
     * Guaranteed safe under concurrent order submissions.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void reserveAndDeduct(Map<Long, Integer> itemQuantities) {
        for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
            Long foodItemId = entry.getKey();
            int requestedQty = entry.getValue();

            Inventory inventory = inventoryRepository.findByFoodItemIdWithLock(foodItemId)
                    .orElseThrow(() -> new BusinessException("Inventory not configured for food item: " + foodItemId));

            if (inventory.getCurrentStock() < requestedQty) {
                String itemName = inventory.getFoodItem() != null ? inventory.getFoodItem().getName() : "Item #" + foodItemId;
                throw new BusinessException(String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                        itemName, inventory.getCurrentStock(), requestedQty));
            }

            int remaining = inventory.getCurrentStock() - requestedQty;
            inventory.setCurrentStock(remaining);
            inventoryRepository.save(inventory);
            log.info("Deducted {} units of foodItemId {}. Remaining stock: {}", requestedQty, foodItemId, remaining);
        }
    }

    /**
     * Restores stock when an unpaid or cancelled order is rolled back.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void restoreStock(Map<Long, Integer> itemQuantities) {
        for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
            Long foodItemId = entry.getKey();
            int returnQty = entry.getValue();

            inventoryRepository.findByFoodItemIdWithLock(foodItemId).ifPresent(inventory -> {
                int restored = inventory.getCurrentStock() + returnQty;
                inventory.setCurrentStock(restored);
                inventoryRepository.save(inventory);
                log.info("Restored {} units of foodItemId {}. Current stock: {}", returnQty, foodItemId, restored);
            });
        }
    }

    @Transactional
    public Inventory restock(Long foodItemId, int additionalUnits) {
        if (additionalUnits <= 0) {
            throw new BusinessException("Restock quantity must be positive");
        }
        Inventory inventory = inventoryRepository.findByFoodItemIdWithLock(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for food item: " + foodItemId));

        inventory.setCurrentStock(inventory.getCurrentStock() + additionalUnits);
        inventory.setLastRestockedAt(LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory updateStockDirectly(Long foodItemId, int newStock, Integer newThreshold) {
        if (newStock < 0) {
            throw new BusinessException("Stock cannot be negative");
        }
        Inventory inventory = inventoryRepository.findByFoodItemIdWithLock(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for food item: " + foodItemId));

        inventory.setCurrentStock(newStock);
        if (newThreshold != null && newThreshold >= 0) {
            inventory.setMinimumThreshold(newThreshold);
        }
        inventory.setLastRestockedAt(LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }
}
