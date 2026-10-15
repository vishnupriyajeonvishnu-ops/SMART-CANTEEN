package com.dmi.canteen;

import com.dmi.canteen.category.Category;
import com.dmi.canteen.category.CategoryRepository;
import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.counter.Counter;
import com.dmi.canteen.counter.CounterRepository;
import com.dmi.canteen.food.FoodItem;
import com.dmi.canteen.food.FoodItemRepository;
import com.dmi.canteen.inventory.Inventory;
import com.dmi.canteen.inventory.InventoryRepository;
import com.dmi.canteen.inventory.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CounterRepository counterRepository;

    private Long testFoodId;

    @BeforeEach
    public void setup() {
        Counter counter = counterRepository.save(new Counter("Test Counter", "TC-TEST"));
        Category category = categoryRepository.save(new Category("Test Category", "Desc"));
        FoodItem foodItem = foodItemRepository.save(new FoodItem(
                "Last Item Samosa", category, counter, "Test description",
                BigDecimal.valueOf(25.0), true, ""
        ));
        testFoodId = foodItem.getId();

        // Stock exactly 5 units
        Inventory inventory = new Inventory(foodItem, 5, 2);
        inventoryRepository.save(inventory);
    }

    @Test
    @DisplayName("Concurrent orders attempting to buy 1 unit each when stock is 5: Exactly 5 succeed, 5 fail, remaining stock is 0 (never negative)")
    public void testConcurrentStockDeduction() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // ensure all threads start simultaneously
                    inventoryService.reserveAndDeduct(Map.of(testFoodId, 1));
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release threads
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS), "Threads timed out");
        executor.shutdown();

        assertEquals(5, successCount.get(), "Exactly 5 threads should successfully reserve 1 unit");
        assertEquals(5, failCount.get(), "5 threads should fail due to stock depletion");

        Inventory updatedInventory = inventoryRepository.findByFoodItemId(testFoodId).orElseThrow();
        assertEquals(0, updatedInventory.getCurrentStock(), "Remaining stock must be strictly 0, never negative");
    }
}
