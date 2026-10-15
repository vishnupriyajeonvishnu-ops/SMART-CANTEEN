package com.dmi.canteen.inventory;

import com.dmi.canteen.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/inventory")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
@Tag(name = "Inventory Management", description = "Stock level controls and low-stock alerts for staff")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List all inventory items")
    public ResponseEntity<ApiResponse<List<Inventory>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllInventory()));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get items at or below minimum threshold")
    public ResponseEntity<ApiResponse<List<Inventory>>> getLowStockAlerts() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockAlerts()));
    }

    @PutMapping("/{foodItemId}/restock")
    @Operation(summary = "Quick restock addition")
    public ResponseEntity<ApiResponse<Inventory>> restock(
            @PathVariable Long foodItemId,
            @RequestBody Map<String, Integer> body) {
        int additionalUnits = body.getOrDefault("quantity", 0);
        Inventory updated = inventoryService.restock(foodItemId, additionalUnits);
        return ResponseEntity.ok(ApiResponse.success("Stock replenished successfully", updated));
    }

    @PutMapping("/{foodItemId}/update")
    @Operation(summary = "Set stock level and threshold directly")
    public ResponseEntity<ApiResponse<Inventory>> updateStockDirectly(
            @PathVariable Long foodItemId,
            @RequestBody Map<String, Integer> body) {
        int currentStock = body.getOrDefault("currentStock", 0);
        Integer threshold = body.get("minimumThreshold");
        Inventory updated = inventoryService.updateStockDirectly(foodItemId, currentStock, threshold);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully", updated));
    }
}
