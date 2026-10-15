package com.dmi.canteen.feedback;

import com.dmi.canteen.auth.UserPrincipal;
import com.dmi.canteen.common.ApiResponse;
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

@RestController
@RequestMapping("/api")
@Tag(name = "Feedback & Ratings", description = "Student food reviews and management rating analytics")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    public FeedbackController(FeedbackService feedbackService, UserRepository userRepository) {
        this.feedbackService = feedbackService;
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

    @PostMapping("/student/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit rating & comment (1 to 5 stars) for an order or item")
    public ResponseEntity<ApiResponse<Feedback>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        Long userId = getAuthenticatedUserId();
        Feedback saved = feedbackService.submitFeedback(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted successfully", saved));
    }

    @GetMapping("/student/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get current student's submitted reviews")
    public ResponseEntity<ApiResponse<List<Feedback>>> getMyFeedback() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getMyFeedback(userId)));
    }

    @GetMapping("/staff/feedback")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Get all customer feedback for staff view")
    public ResponseEntity<ApiResponse<List<Feedback>>> getAllFeedback() {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getAllFeedback()));
    }

    @GetMapping("/management/feedback")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get rating analytics, average star rating and breakdown for management")
    public ResponseEntity<ApiResponse<FeedbackSummaryDto>> getFeedbackSummary() {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getFeedbackSummary()));
    }
}
