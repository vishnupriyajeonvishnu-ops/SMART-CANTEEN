package com.dmi.canteen.order;

import com.dmi.canteen.auth.UserPrincipal;
import com.dmi.canteen.common.ApiResponse;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentService;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Orders & Payments", description = "Student order placement, tracking, and UPI payment operations")
public class StudentOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public StudentOrderController(OrderService orderService, PaymentService paymentService, UserRepository userRepository) {
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

    @PostMapping("/orders")
    @Operation(summary = "Place order", description = "Pessimistic lock stock deduction, idempotent check, backend calculated total")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            @Valid @RequestBody CreateOrderRequest request) {

        if (request.getIdempotencyKey() == null && idempotencyHeader != null) {
            request.setIdempotencyKey(idempotencyHeader);
        }

        Long userId = getAuthenticatedUserId();
        OrderDto order = orderService.createOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get my orders list")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getMyOrders() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(userId)));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get single order details with ownership verification")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable String id) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByIdentifier(id, userId, false)));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel placed order and restore inventory stock")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", orderService.cancelOrder(id, userId, false)));
    }

    @PostMapping("/payments/upi-utr")
    public ResponseEntity<ApiResponse<Payment>> submitUtr(@RequestBody Map<String, Object> body) {
        Long userId = getAuthenticatedUserId();
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Object utrObj = body.get("utrNumber");
        if (utrObj == null) {
            utrObj = body.get("utrReference");
        }
        if (utrObj == null) {
            throw new IllegalArgumentException("UTR reference number is required");
        }
        String utr = utrObj.toString();

        Payment payment = paymentService.submitUpiUtr(orderId, userId, utr);
        return ResponseEntity.ok(ApiResponse.success("UTR submitted successfully. Pending staff confirmation.", payment));
    }

    @GetMapping("/upi-config")
    @Operation(summary = "Get college UPI ID and payee name for dynamic QR generation")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUpiConfig() {
        Map<String, String> config = Map.of(
                "upiId", paymentService.getUpiId(),
                "payeeName", paymentService.getPayeeName()
        );
        return ResponseEntity.ok(ApiResponse.success(config));
    }
}
