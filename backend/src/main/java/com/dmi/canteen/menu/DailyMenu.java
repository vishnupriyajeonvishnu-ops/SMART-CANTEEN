package com.dmi.canteen.menu;

import com.dmi.canteen.food.FoodItem;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_menu", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"food_item_id", "menu_date"})
})
public class DailyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "special_note", length = 200)
    private String specialNote;

    public DailyMenu() {}

    public DailyMenu(FoodItem foodItem, LocalDate menuDate, boolean available, String specialNote) {
        this.foodItem = foodItem;
        this.menuDate = menuDate;
        this.available = available;
        this.specialNote = specialNote;
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

    public LocalDate getMenuDate() {
        return menuDate;
    }

    public void setMenuDate(LocalDate menuDate) {
        this.menuDate = menuDate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getSpecialNote() {
        return specialNote;
    }

    public void setSpecialNote(String specialNote) {
        this.specialNote = specialNote;
    }
}
