package com.dmi.canteen.counter;

import com.dmi.canteen.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/counters")
@Tag(name = "Counters", description = "Canteen food and beverage collection counters")
public class CounterController {

    private final CounterService counterService;

    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @GetMapping
    @Operation(summary = "Get active counters", description = "Returns all active collection counters")
    public ResponseEntity<ApiResponse<List<Counter>>> getCounters() {
        return ResponseEntity.ok(ApiResponse.success(counterService.getActiveCounters()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Get all counters including inactive (Staff/Manager)")
    public ResponseEntity<ApiResponse<List<Counter>>> getAllCounters() {
        return ResponseEntity.ok(ApiResponse.success(counterService.getAllCounters()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Create counter (Staff/Manager)")
    public ResponseEntity<ApiResponse<Counter>> createCounter(@RequestBody Counter counter) {
        return ResponseEntity.ok(ApiResponse.success("Counter created", counterService.createCounter(counter)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Update counter (Staff/Manager)")
    public ResponseEntity<ApiResponse<Counter>> updateCounter(@PathVariable Long id, @RequestBody Counter counter) {
        return ResponseEntity.ok(ApiResponse.success("Counter updated", counterService.updateCounter(id, counter)));
    }
}
