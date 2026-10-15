package com.dmi.canteen.menu;

import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.food.FoodItem;
import com.dmi.canteen.food.FoodItemDto;
import com.dmi.canteen.food.FoodItemRepository;
import com.dmi.canteen.food.FoodService;
import com.dmi.canteen.inventory.Inventory;
import com.dmi.canteen.inventory.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DailyMenuService {

    private final DailyMenuRepository dailyMenuRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodService foodService;
    private final InventoryRepository inventoryRepository;

    public DailyMenuService(DailyMenuRepository dailyMenuRepository,
                            FoodItemRepository foodItemRepository,
                            FoodService foodService,
                            InventoryRepository inventoryRepository) {
        this.dailyMenuRepository = dailyMenuRepository;
        this.foodItemRepository = foodItemRepository;
        this.foodService = foodService;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<FoodItemDto> getTodayMenu() {
        LocalDate today = LocalDate.now();
        List<DailyMenu> menuEntries = dailyMenuRepository.findActiveItemsForDate(today);

        List<FoodItemDto> result = new ArrayList<>();
        for (DailyMenu entry : menuEntries) {
            FoodItem food = entry.getFoodItem();
            FoodItemDto dto = foodService.toDto(food);
            dto.setAvailableInMenu(entry.isAvailable());

            // Check inventory stock
            Optional<Inventory> inv = inventoryRepository.findByFoodItemId(food.getId());
            if (inv.isPresent()) {
                int stock = inv.get().getCurrentStock();
                dto.setCurrentStock(stock);
                if (stock <= 0) {
                    dto.setAvailableInMenu(false); // Sold out automatically
                }
            } else {
                dto.setCurrentStock(0);
                dto.setAvailableInMenu(false);
            }
            result.add(dto);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DailyMenu> getTodayMenuEntries() {
        return dailyMenuRepository.findByMenuDate(LocalDate.now());
    }

    @Transactional
    public DailyMenu toggleItemInTodayMenu(Long foodItemId, boolean available, String note) {
        LocalDate today = LocalDate.now();
        FoodItem food = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found: " + foodItemId));

        DailyMenu menu = dailyMenuRepository.findByFoodItemIdAndMenuDate(foodItemId, today)
                .orElse(new DailyMenu(food, today, available, note));

        menu.setAvailable(available);
        if (note != null) {
            menu.setSpecialNote(note);
        }
        return dailyMenuRepository.save(menu);
    }

    @Transactional
    public void setTodayMenu(List<Long> foodItemIds) {
        LocalDate today = LocalDate.now();
        List<DailyMenu> existing = dailyMenuRepository.findByMenuDate(today);
        for (DailyMenu dm : existing) {
            dm.setAvailable(false);
            dailyMenuRepository.save(dm);
        }

        for (Long foodId : foodItemIds) {
            toggleItemInTodayMenu(foodId, true, null);
        }
    }
}
