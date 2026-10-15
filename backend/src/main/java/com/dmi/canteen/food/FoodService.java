package com.dmi.canteen.food;

import com.dmi.canteen.category.Category;
import com.dmi.canteen.category.CategoryRepository;
import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.counter.Counter;
import com.dmi.canteen.counter.CounterRepository;
import com.dmi.canteen.inventory.Inventory;
import com.dmi.canteen.inventory.InventoryRepository;
import com.dmi.canteen.inventory.InventoryService;
import com.dmi.canteen.menu.DailyMenu;
import com.dmi.canteen.menu.DailyMenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodService {

    private final FoodItemRepository foodItemRepository;
    private final CategoryRepository categoryRepository;
    private final CounterRepository counterRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final DailyMenuRepository dailyMenuRepository;

    public FoodService(FoodItemRepository foodItemRepository,
                       CategoryRepository categoryRepository,
                       CounterRepository counterRepository,
                       InventoryRepository inventoryRepository,
                       InventoryService inventoryService,
                       DailyMenuRepository dailyMenuRepository) {
        this.foodItemRepository = foodItemRepository;
        this.categoryRepository = categoryRepository;
        this.counterRepository = counterRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.dailyMenuRepository = dailyMenuRepository;
    }

    @Transactional(readOnly = true)
    public List<FoodItemDto> getAllFoodItems(boolean includeInactive) {
        List<FoodItem> items = includeInactive ? foodItemRepository.findAll() : foodItemRepository.findByActiveTrue();
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodItemDto getFoodItemById(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
        return toDto(item);
    }

    @Transactional
    public FoodItemDto createFoodItem(FoodItemRequest req) {
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + req.getCategoryId()));

        Counter counter = counterRepository.findById(req.getCounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + req.getCounterId()));

        FoodItem item = new FoodItem(
                req.getName().trim(),
                category,
                counter,
                req.getDescription(),
                req.getPrice(),
                req.isVeg(),
                req.getImageUrl()
        );

        FoodItem saved = foodItemRepository.save(item);

        int initialStock = req.getInitialStock() != null && req.getInitialStock() >= 0 ? req.getInitialStock() : 50;
        inventoryService.createInventoryForFood(saved, initialStock, 10);

        // Automatically make available in today's menu
        try {
            LocalDate today = LocalDate.now();
            if (dailyMenuRepository.findByFoodItemIdAndMenuDate(saved.getId(), today).isEmpty()) {
                dailyMenuRepository.save(new DailyMenu(saved, today, true, "Freshly added to canteen menu"));
            }
        } catch (Exception ignored) {}

        return toDto(saved);
    }

    @Transactional
    public FoodItemDto updateFoodItem(Long id, FoodItemRequest req) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + req.getCategoryId()));

        Counter counter = counterRepository.findById(req.getCounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + req.getCounterId()));

        item.setName(req.getName().trim());
        item.setCategory(category);
        item.setCounter(counter);
        item.setDescription(req.getDescription());
        item.setPrice(req.getPrice());
        item.setVeg(req.isVeg());
        if (req.getImageUrl() != null && !req.getImageUrl().trim().isEmpty()) {
            item.setImageUrl(req.getImageUrl().trim());
        }

        FoodItem updated = foodItemRepository.save(item);

        // Update inventory stock if requested
        if (req.getInitialStock() != null && req.getInitialStock() >= 0) {
            inventoryRepository.findByFoodItemId(updated.getId()).ifPresent(inv -> {
                inv.setCurrentStock(req.getInitialStock());
                inventoryRepository.save(inv);
            });
        }

        return toDto(updated);
    }

    @Transactional
    public void deactivateFoodItem(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
        item.setActive(false);
        foodItemRepository.save(item);
    }

    @Transactional
    public FoodItemDto reactivateFoodItem(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
        item.setActive(true);
        return toDto(foodItemRepository.save(item));
    }

    @Transactional
    public FoodItemDto toggleFoodItemStatus(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
        item.setActive(!item.isActive());
        return toDto(foodItemRepository.save(item));
    }

    @Transactional
    public void deleteFoodItem(Long id, boolean forcePermanent) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
        if (forcePermanent) {
            try {
                inventoryRepository.findByFoodItemId(id).ifPresent(inventoryRepository::delete);
                foodItemRepository.delete(item);
                return;
            } catch (Exception e) {
                // If referenced by past orders, fallback to soft deactivate
                item.setActive(false);
                foodItemRepository.save(item);
                return;
            }
        }
        item.setActive(false);
        foodItemRepository.save(item);
    }

    public String uploadAndOptimizeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please select an image file to upload");
        }

        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "dish.jpg";
        String ext = "jpg";
        if (orig.toLowerCase().endsWith(".png")) ext = "png";

        String filename = "dish_upload_" + System.currentTimeMillis() + "." + ext;

        try {
            BufferedImage origImg = ImageIO.read(file.getInputStream());
            if (origImg == null) {
                throw new BusinessException("Invalid image file format. Supported: JPG, PNG, WebP.");
            }

            int origW = origImg.getWidth();
            int origH = origImg.getHeight();
            int maxW = 450;
            int maxH = 320;
            double ratio = Math.min((double) maxW / origW, (double) maxH / origH);
            if (ratio > 1.0) ratio = 1.0;
            int newW = Math.max(1, (int) (origW * ratio));
            int newH = Math.max(1, (int) (origH * ratio));

            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(origImg, 0, 0, newW, newH, null);
            g.dispose();

            // Store across directories so it is instantly available without server restart
            String[] targetDirs = new String[]{
                    "src/main/resources/static/assets/dishes",
                    "target/classes/static/assets/dishes",
                    "../frontend/assets/dishes"
            };

            for (String p : targetDirs) {
                File dir = new File(p);
                if (dir.exists() || dir.mkdirs()) {
                    File target = new File(dir, filename);
                    ImageIO.write(resized, "jpg", target);
                }
            }

            return "assets/dishes/" + filename;
        } catch (Exception e) {
            throw new BusinessException("Failed to upload image: " + e.getMessage());
        }
    }

    public FoodItemDto toDto(FoodItem item) {
        FoodItemDto dto = new FoodItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCategoryId(item.getCategory().getId());
        dto.setCategoryName(item.getCategory().getName());
        dto.setCounterId(item.getCounter().getId());
        dto.setCounterName(item.getCounter().getName());
        dto.setCounterCode(item.getCounter().getCode());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setVeg(item.isVeg());
        dto.setImageUrl(item.getImageUrl());
        dto.setActive(item.isActive());

        inventoryRepository.findByFoodItemId(item.getId()).ifPresent(inv -> {
            dto.setCurrentStock(inv.getCurrentStock());
        });

        return dto;
    }
}
