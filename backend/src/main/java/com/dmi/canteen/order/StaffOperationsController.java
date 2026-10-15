package com.dmi.canteen.order;

import com.dmi.canteen.auth.UserPrincipal;
import com.dmi.canteen.billing.Bill;
import com.dmi.canteen.common.ApiResponse;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentService;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
@Tag(name = "Staff Operations", description = "Order processing board, payment verification queue, and status updates")
public class StaffOperationsController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public StaffOperationsController(OrderService orderService, PaymentService paymentService, UserRepository userRepository) {
        this.orderService = orderService;
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

    @GetMapping("/orders")
    @Operation(summary = "Get all orders for the live order board")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "Advance order status (PREPARING -> READY -> COMPLETED)")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        OrderStatus status = OrderStatus.valueOf(body.get("status").toUpperCase());
        OrderDto updated = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updated));
    }

    @GetMapping("/payments/pending")
    @Operation(summary = "Pending UTR and Cash payment verification queue")
    public ResponseEntity<ApiResponse<List<Payment>>> getPendingPayments() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPendingVerifications()));
    }

    @PostMapping("/payments/{id}/verify")
    @Operation(summary = "Staff verifies payment, marks order CONFIRMED and automatically generates unique Bill")
    public ResponseEntity<ApiResponse<com.dmi.canteen.billing.BillDto>> verifyPayment(@PathVariable Long id) {
        Long staffId = getAuthenticatedUserId();
        Bill bill = paymentService.verifyPayment(id, staffId);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and Bill generated", com.dmi.canteen.billing.BillDto.fromEntity(bill)));
    }

    @PostMapping("/payments/{id}/reject")
    @Operation(summary = "Staff rejects payment, cancels order, and returns inventory stock")
    public ResponseEntity<ApiResponse<Payment>> rejectPayment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Long staffId = getAuthenticatedUserId();
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Payment rejected by staff";
        Payment rejected = paymentService.rejectPayment(id, staffId, reason);
        return ResponseEntity.ok(ApiResponse.success("Payment rejected and stock restored", rejected));
    }
}
