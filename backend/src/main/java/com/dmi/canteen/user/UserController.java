package com.dmi.canteen.user;

import com.dmi.canteen.auth.UserDto;
import com.dmi.canteen.auth.UserPrincipal;
import com.dmi.canteen.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "Current logged-in user profile management")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
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

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns full profile including role, student/staff specifics")
    public ResponseEntity<ApiResponse<UserDto>> getMe() {
        Long userId = getAuthenticatedUserId();
        UserDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update full name, phone number or change password")
    public ResponseEntity<ApiResponse<UserDto>> updateMe(@RequestBody UpdateProfileRequest request) {
        Long userId = getAuthenticatedUserId();
        UserDto updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }
}
