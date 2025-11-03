package com.yagi.socialanalyzer.application.services;

import com.yagi.socialanalyzer.application.dto.CollectionStatus;
import com.yagi.socialanalyzer.domain.exceptions.CollectionException;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Interface for data collection service.
 */
public interface IDataCollectionService {
    
    /**
     * Start data collection for a project.
     * 
     * @param projectId Project ID
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @param statusCallback Callback for status messages
     * @return Total number of posts collected
     */
    int startCollection(UUID projectId, Consumer<Double> progressCallback,
                       Consumer<String> statusCallback) throws CollectionException;
    
    /**
     * Pause ongoing collection.
     */
    void pauseCollection(UUID projectId) throws CollectionException;
    
    /**
     * Resume paused collection.
     */
    void resumeCollection(UUID projectId) throws CollectionException;
    
    /**
     * Get current collection status.
     */
    CollectionStatus getStatus(UUID projectId) throws CollectionException;
    
    /**
     * Cancel ongoing collection.
     */
    void cancelCollection(UUID projectId);
}
