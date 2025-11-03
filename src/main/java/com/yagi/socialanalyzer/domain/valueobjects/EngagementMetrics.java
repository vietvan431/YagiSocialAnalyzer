package com.yagi.socialanalyzer.domain.valueobjects;

/**
 * Value object representing social media post engagement metrics.
 * All metrics must be non-negative.
 */
public record EngagementMetrics(
    int likes,
    int shares,
    int comments,
    long views
) {
    
    public EngagementMetrics {
        if (likes < 0) {
            throw new IllegalArgumentException("Likes cannot be negative");
        }
        if (shares < 0) {
            throw new IllegalArgumentException("Shares cannot be negative");
        }
        if (comments < 0) {
            throw new IllegalArgumentException("Comments cannot be negative");
        }
        if (views < 0) {
            throw new IllegalArgumentException("Views cannot be negative");
        }
    }
    
    /**
     * Calculate total engagement (sum of likes, shares, comments).
     *
     * @return total engagement count
     */
    public int getTotalEngagement() {
        return likes + shares + comments;
    }
    
    /**
     * Check if this post has any engagement.
     *
     * @return true if any engagement metric > 0
     */
    public boolean hasEngagement() {
        return likes > 0 || shares > 0 || comments > 0 || views > 0;
    }
}
