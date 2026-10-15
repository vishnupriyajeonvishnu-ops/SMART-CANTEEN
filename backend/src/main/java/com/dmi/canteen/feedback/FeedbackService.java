package com.dmi.canteen.feedback;

import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.order.Order;
import com.dmi.canteen.order.OrderItem;
import com.dmi.canteen.order.OrderItemRepository;
import com.dmi.canteen.order.OrderRepository;
import com.dmi.canteen.user.User;
import com.dmi.canteen.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Feedback submitFeedback(Long userId, FeedbackRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + req.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("You can only review your own orders.");
        }

        OrderItem orderItem = null;
        if (req.getOrderItemId() != null) {
            orderItem = orderItemRepository.findById(req.getOrderItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + req.getOrderItemId()));

            // One feedback per order item rule
            if (feedbackRepository.existsByOrderIdAndOrderItemId(order.getId(), orderItem.getId())) {
                throw new BusinessException("You have already submitted feedback for this item.");
            }
        }

        Feedback feedback = new Feedback(
                order,
                orderItem,
                user,
                req.getRating(),
                req.getComment(),
                req.getFeedbackType()
        );

        return feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getMyFeedback(Long userId) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryDto getFeedbackSummary() {
        FeedbackSummaryDto dto = new FeedbackSummaryDto();
        Double avg = feedbackRepository.getAverageRating();
        dto.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);

        List<Feedback> all = feedbackRepository.findAllByOrderByCreatedAtDesc();
        dto.setTotalReviews(all.size());
        dto.setRecentFeedback(all.stream().limit(10).toList());

        Map<Integer, Long> counts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            counts.put(i, 0L);
        }
        List<Object[]> distribution = feedbackRepository.getRatingDistribution();
        for (Object[] row : distribution) {
            int stars = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            counts.put(stars, count);
        }
        dto.setStarCounts(counts);

        return dto;
    }
}
