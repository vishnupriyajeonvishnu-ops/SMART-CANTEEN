package com.dmi.canteen.billing;

import com.dmi.canteen.auth.UserPrincipal;
import com.dmi.canteen.common.ApiResponse;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentService;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Billing", description = "Endpoints for viewing and thermal receipt printing of canteen bills")
public class BillingController {

    private final BillingService billingService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public BillingController(BillingService billingService, PaymentService paymentService, UserRepository userRepository) {
        this.billingService = billingService;
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getDetails()).getUserId();
        }
        if (auth != null && auth.getName() != null) {
            return userRepository.findByUsername(auth.getName())
                    .map(User::getId)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }
        throw new RuntimeException("No authenticated user found");
    }

    @GetMapping("/student/bills")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get current student's bills")
    public ResponseEntity<ApiResponse<List<BillDto>>> getStudentBills() {
        Long userId = getAuthenticatedUserId();
        List<BillDto> list = billingService.getBillsForUser(userId).stream()
                .map(BillDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/student/bills/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get single bill details")
    public ResponseEntity<ApiResponse<BillDto>> getStudentBillById(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        Bill bill = billingService.getBillById(id, userId, false);
        return ResponseEntity.ok(ApiResponse.success(BillDto.fromEntity(bill)));
    }

    @GetMapping(value = "/student/bills/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get print-ready 80mm thermal receipt HTML for student")
    public ResponseEntity<String> printStudentBill(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        String html = billingService.generatePrintableBillHtml(id, userId, false);
        return ResponseEntity.ok(html);
    }

    @GetMapping("/staff/bills")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Get all bills for staff / manager lookup")
    public ResponseEntity<ApiResponse<List<BillDto>>> getAllBills() {
        List<BillDto> list = billingService.getAllBills().stream()
                .map(BillDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/staff/bills/pending")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Get pending orders awaiting billing verification")
    public ResponseEntity<ApiResponse<List<Payment>>> getPendingBills() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPendingVerifications()));
    }

    @PostMapping("/staff/bills/{id}/verify")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Verify pending bill/payment and generate official invoice")
    public ResponseEntity<ApiResponse<BillDto>> verifyBill(@PathVariable Long id) {
        Long staffId = getAuthenticatedUserId();
        Bill bill = paymentService.verifyPayment(id, staffId);
        return ResponseEntity.ok(ApiResponse.success("Bill verified and generated successfully", BillDto.fromEntity(bill)));
    }

    @GetMapping(value = "/staff/bills/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Get print-ready 80mm thermal receipt HTML for staff")
    public ResponseEntity<String> printStaffBill(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        String html = billingService.generatePrintableBillHtml(id, userId, true);
        return ResponseEntity.ok(html);
    }
}
