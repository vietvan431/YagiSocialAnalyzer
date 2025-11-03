package com.yagi.socialanalyzer.ui.tasks;

import com.yagi.socialanalyzer.application.services.IDataCollectionService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * JavaFX Task for background data collection with progress updates.
 */
public class DataCollectionTask extends Task<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(DataCollectionTask.class);
    
    private final UUID projectId;
    private IDataCollectionService collectionService;
    private volatile boolean paused = false;
    
    public DataCollectionTask(UUID projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Set the collection service (dependency injection).
     */
    public void setCollectionService(IDataCollectionService collectionService) {
        this.collectionService = collectionService;
    }
    
    @Override
    protected Integer call() throws Exception {
        if (collectionService == null) {
            throw new IllegalStateException("Collection service not set");
        }
        
        logger.info("Starting collection task for project: {}", projectId);
        
        try {
            int totalCollected = collectionService.startCollection(
                projectId,
                progress -> updateProgress(progress, 1.0),
                status -> updateMessage(status)
            );
            
            return totalCollected;
            
        } catch (Exception e) {
            logger.error("Collection task failed", e);
            throw e;
        }
    }
    
    /**
     * Pause the collection.
     */
    public void pause() {
        if (collectionService != null && !paused) {
            try {
                collectionService.pauseCollection(projectId);
                paused = true;
                updateMessage("Collection paused");
                logger.info("Collection paused");
            } catch (Exception e) {
                logger.error("Failed to pause collection", e);
            }
        }
    }
    
    /**
     * Resume the collection.
     */
    public void resume() {
        if (collectionService != null && paused) {
            try {
                collectionService.resumeCollection(projectId);
                paused = false;
                updateMessage("Collection resumed");
                logger.info("Collection resumed");
            } catch (Exception e) {
                logger.error("Failed to resume collection", e);
            }
        }
    }
    
    @Override
    protected void cancelled() {
        super.cancelled();
        if (collectionService != null) {
            collectionService.cancelCollection(projectId);
            logger.info("Collection cancelled");
        }
    }
}
