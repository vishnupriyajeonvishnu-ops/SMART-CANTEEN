package com.dmi.canteen.feedback;

import java.util.List;
import java.util.Map;

public class FeedbackSummaryDto {
    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> starCounts;
    private List<Feedback> recentFeedback;

    public FeedbackSummaryDto() {}

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Map<Integer, Long> getStarCounts() {
        return starCounts;
    }

    public void setStarCounts(Map<Integer, Long> starCounts) {
        this.starCounts = starCounts;
    }

    public List<Feedback> getRecentFeedback() {
        return recentFeedback;
    }

    public void setRecentFeedback(List<Feedback> recentFeedback) {
        this.recentFeedback = recentFeedback;
    }
}
