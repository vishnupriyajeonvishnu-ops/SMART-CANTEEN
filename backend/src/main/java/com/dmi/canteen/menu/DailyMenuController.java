package com.dmi.canteen.menu;

import com.dmi.canteen.common.ApiResponse;
import com.dmi.canteen.food.FoodItemDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Daily Menu", description = "Endpoints for viewing and updating daily canteen menus")
public class DailyMenuController {

    private final DailyMenuService dailyMenuService;

    public DailyMenuController(DailyMenuService dailyMenuService) {
        this.dailyMenuService = dailyMenuService;
    }

    @GetMapping("/menu/today")
    @Operation(summary = "Get today's live menu", description = "Public & student access to today's active items, stock, counter and category")
    public ResponseEntity<ApiResponse<List<FoodItemDto>>> getTodayMenu() {
        return ResponseEntity.ok(ApiResponse.success(dailyMenuService.getTodayMenu()));
    }

    @GetMapping("/staff/menu/daily")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Get daily menu list for staff manager")
    public ResponseEntity<ApiResponse<List<DailyMenu>>> getStaffDailyMenu() {
        return ResponseEntity.ok(ApiResponse.success(dailyMenuService.getTodayMenuEntries()));
    }

    @PostMapping("/staff/menu/toggle")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Toggle food item inclusion in today's menu")
    public ResponseEntity<ApiResponse<DailyMenu>> toggleItem(@RequestBody Map<String, Object> body) {
        Long foodItemId = Long.valueOf(body.get("foodItemId").toString());
        boolean available = Boolean.parseBoolean(body.get("available").toString());
        String note = body.containsKey("note") && body.get("note") != null ? body.get("note").toString() : null;

        DailyMenu updated = dailyMenuService.toggleItemInTodayMenu(foodItemId, available, note);
        return ResponseEntity.ok(ApiResponse.success("Daily menu updated", updated));
    }

    @PostMapping("/staff/menu/bulk")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Bulk set today's active items")
    public ResponseEntity<ApiResponse<String>> setBulkMenu(@RequestBody List<Long> foodItemIds) {
        dailyMenuService.setTodayMenu(foodItemIds);
        return ResponseEntity.ok(ApiResponse.success("Daily menu saved", "UPDATED"));
    }
}
