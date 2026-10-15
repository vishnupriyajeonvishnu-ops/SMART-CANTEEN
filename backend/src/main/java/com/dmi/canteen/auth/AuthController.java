package com.dmi.canteen.auth;

import com.dmi.canteen.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for Captcha, Student Registration, Login & Logout")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    @Operation(summary = "Generate visual image captcha", description = "Returns base64 image data and a 5-minute captcha session ID")
    public ResponseEntity<ApiResponse<CaptchaService.CaptchaResponse>> getCaptcha(
            @RequestParam(name = "oldCaptchaId", required = false) String oldCaptchaId) {
        CaptchaService.CaptchaResponse response = captchaService.generateCaptcha(oldCaptchaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(ApiResponse.success("Captcha generated", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new student", description = "Validates 12-digit register number, credentials, and captcha")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerStudent(request);
        return ResponseEntity.ok(ApiResponse.success("Student registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Requires Email, Username, Password, and Captcha code. Enforces 5-attempt lockout.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Client discards session/token")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", "SESSION_TERMINATED"));
    }
}
