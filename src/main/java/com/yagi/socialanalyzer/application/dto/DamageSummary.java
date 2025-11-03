package com.yagi.socialanalyzer.application.dto;

import java.util.List;

/**
 * Damage categorization summary DTO.
 */
public record DamageSummary(
    String projectId,
    long totalClassified,
    List<CategoryCount> categoryCounts,
    double averageConfidence
) {
    public record CategoryCount(
        String categoryId,
        String categoryName,
        long count,
        String color
    ) {
        public double getPercentage(long total) {
            return total > 0 ? (count * 100.0) / total : 0.0;
        }
    }
    
    public CategoryCount getMostFrequentCategory() {
        return categoryCounts.stream()
            .max((c1, c2) -> Long.compare(c1.count(), c2.count()))
            .orElse(null);
    }
}
