package com.dmi.canteen.chatbot;

import com.dmi.canteen.auth.UserPrincipal;
import com.dmi.canteen.common.ApiResponse;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "AI Chatbot", description = "Contextual role-aware chatbot assistant")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final UserRepository userRepository;

    // Simple per-client rate limiter (max 30 requests per minute)
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    public ChatbotController(ChatbotService chatbotService, UserRepository userRepository) {
        this.chatbotService = chatbotService;
        this.userRepository = userRepository;
    }

    @PostMapping("/message")
    @Operation(summary = "Send message to AI Canteen Assistant", description = "Role-aware responses with live menu, order status, or revenue metrics")
    public ResponseEntity<ApiResponse<ChatbotResponse>> sendMessage(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");

        Long userId = null;
        String role = "GUEST";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (auth.getDetails() instanceof UserPrincipal principal) {
                userId = principal.getUserId();
                role = principal.getRole();
            } else if (auth.getName() != null) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null) {
                    userId = user.getId();
                    role = user.getRole().name();
                }
            }
        }

        ChatbotResponse response = chatbotService.processMessage(message, userId, role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
