package com.dmi.canteen.food;

import com.dmi.canteen.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Food Items", description = "Management and viewing of canteen food items")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("/food/{id}")
    @Operation(summary = "Get single food item details")
    public ResponseEntity<ApiResponse<FoodItemDto>> getFoodItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(foodService.getFoodItemById(id)));
    }

    @GetMapping("/staff/food")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "List all food items for staff/management")
    public ResponseEntity<ApiResponse<List<FoodItemDto>>> getAllFoodItems() {
        return ResponseEntity.ok(ApiResponse.success(foodService.getAllFoodItems(true)));
    }

    @PostMapping("/staff/food")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Create new food item with initial inventory stock")
    public ResponseEntity<ApiResponse<FoodItemDto>> createFoodItem(@Valid @RequestBody FoodItemRequest request) {
        FoodItemDto created = foodService.createFoodItem(request);
        return ResponseEntity.ok(ApiResponse.success("Food item created successfully", created));
    }

    @PutMapping("/staff/food/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Update food item details")
    public ResponseEntity<ApiResponse<FoodItemDto>> updateFoodItem(@PathVariable Long id, @Valid @RequestBody FoodItemRequest request) {
        FoodItemDto updated = foodService.updateFoodItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Food item updated successfully", updated));
    }

    @DeleteMapping("/staff/food/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Deactivate or delete food item")
    public ResponseEntity<ApiResponse<String>> deleteFoodItem(@PathVariable Long id,
                                                               @RequestParam(defaultValue = "false") boolean permanent) {
        foodService.deleteFoodItem(id, permanent);
        return ResponseEntity.ok(ApiResponse.success(
                permanent ? "Food item deleted permanently" : "Food item deactivated successfully",
                permanent ? "DELETED" : "DEACTIVATED"));
    }

    @PatchMapping("/staff/food/{id}/reactivate")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Reactivate food item")
    public ResponseEntity<ApiResponse<FoodItemDto>> reactivateFoodItem(@PathVariable Long id) {
        FoodItemDto reactivated = foodService.reactivateFoodItem(id);
        return ResponseEntity.ok(ApiResponse.success("Food item reactivated successfully", reactivated));
    }

    @PutMapping("/staff/food/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Toggle active/inactive status of food item")
    public ResponseEntity<ApiResponse<FoodItemDto>> toggleFoodItemStatus(@PathVariable Long id) {
        FoodItemDto toggled = foodService.toggleFoodItemStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Food item status toggled successfully", toggled));
    }

    @PostMapping(value = "/staff/food/upload-image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Upload and auto-optimize food photo")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> uploadFoodImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String relativeUrl = foodService.uploadAndOptimizeImage(file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded and optimized successfully", java.util.Map.of("url", relativeUrl)));
    }
}
