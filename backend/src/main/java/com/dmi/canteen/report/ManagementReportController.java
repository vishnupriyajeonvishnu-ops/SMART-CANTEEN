package com.dmi.canteen.report;

import com.dmi.canteen.common.ApiResponse;
import com.dmi.canteen.inventory.InventoryService;
import com.dmi.canteen.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/management")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Management & Reports", description = "Executive sales reports, revenue trends, and CSV analytics export")
public class ManagementReportController {

    private final ReportService reportService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    public ManagementReportController(ReportService reportService,
                                      InventoryService inventoryService,
                                      PaymentService paymentService) {
        this.reportService = reportService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Management dashboard KPI overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardKpis() {
        ReportService.RevenueReportDto todayReport = reportService.generateReport("daily", null, null);

        Map<String, Object> data = new HashMap<>();
        data.put("todayRevenue", todayReport.getTotalRevenue());
        data.put("onlineRevenue", todayReport.getOnlineRevenue());
        data.put("cashRevenue", todayReport.getCashRevenue());
        data.put("completedOrdersCount", todayReport.getTotalOrders());
        data.put("topSellingItems", todayReport.getTopSellingItems());
        data.put("counterWiseSales", todayReport.getCounterWiseSales());
        data.put("categoryWiseSales", todayReport.getCategoryWiseSales());
        data.put("lowStockCount", inventoryService.getLowStockAlerts().size());
        data.put("pendingVerificationsCount", paymentService.getPendingVerifications().size());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/reports/sales")
    @Operation(summary = "Sales revenue report by period (daily, weekly, monthly, custom)")
    public ResponseEntity<ApiResponse<ReportService.RevenueReportDto>> getSalesReport(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportService.RevenueReportDto report = reportService.generateReport(period, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports/export-csv")
    @Operation(summary = "Export sales report to downloadable CSV")
    public ResponseEntity<String> exportCsvReport(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String csv = reportService.generateCsvReport(period, startDate, endDate);
        String filename = "dmi_canteen_sales_" + period + "_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
