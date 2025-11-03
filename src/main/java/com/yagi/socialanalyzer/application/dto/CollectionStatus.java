package com.yagi.socialanalyzer.application.dto;

import com.yagi.socialanalyzer.domain.valueobjects.ProjectStatus;
import java.time.Instant;

/**
 * Data collection status DTO.
 */
public record CollectionStatus(
    String projectId,
    String projectName,
    ProjectStatus status,
    long totalPostsCollected,
    long postsCollectedToday,
    String currentPlatform,
    double completionPercentage,
    Instant lastCollectionTime,
    String errorMessage
) {
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }
    
    public boolean isActive() {
        return status == ProjectStatus.COLLECTING;
    }
}
