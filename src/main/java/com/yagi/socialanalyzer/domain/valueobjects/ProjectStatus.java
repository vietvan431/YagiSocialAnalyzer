package com.yagi.socialanalyzer.domain.valueobjects;

/**
 * Project status enum representing the lifecycle state of a disaster project.
 */
public enum ProjectStatus {
    /** Newly created, not started collection */
    DRAFT,
    
    /** Active data collection in progress */
    COLLECTING,
    
    /** Collection paused, can resume */
    PAUSED,
    
    /** Collection finished */
    COMPLETED,
    
    /** Analysis in progress */
    ANALYZING,
    
    /** Completed and archived */
    ARCHIVED
}
