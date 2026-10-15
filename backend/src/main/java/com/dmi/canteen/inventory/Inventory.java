package com.dmi.canteen.inventory;

import com.dmi.canteen.food.FoodItem;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "food_item_id", nullable = false, unique = true)
    private FoodItem foodItem;

    @Column(name = "current_stock", nullable = false)
    private int currentStock = 0;

    @Column(name = "minimum_threshold", nullable = false)
    private int minimumThreshold = 10;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockStatus status = StockStatus.AVAILABLE;

    @PrePersist
    @PreUpdate
    public void updateStatus() {
        if (currentStock <= 0) {
            status = StockStatus.OUT_OF_STOCK;
            currentStock = 0;
        } else if (currentStock <= minimumThreshold) {
            status = StockStatus.LOW_STOCK;
        } else {
            status = StockStatus.AVAILABLE;
        }
    }

    public Inventory() {}

    public Inventory(FoodItem foodItem, int currentStock, int minimumThreshold) {
        this.foodItem = foodItem;
        this.currentStock = currentStock;
        this.minimumThreshold = minimumThreshold;
        this.lastRestockedAt = LocalDateTime.now();
        updateStatus();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
        updateStatus();
    }

    public int getMinimumThreshold() {
        return minimumThreshold;
    }

    public void setMinimumThreshold(int minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

    public LocalDateTime getLastRestockedAt() {
        return lastRestockedAt;
    }

    public void setLastRestockedAt(LocalDateTime lastRestockedAt) {
        this.lastRestockedAt = lastRestockedAt;
    }

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = status;
    }
}
