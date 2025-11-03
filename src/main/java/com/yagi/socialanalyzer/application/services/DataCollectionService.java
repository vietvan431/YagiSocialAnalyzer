package com.yagi.socialanalyzer.application.services;

import com.yagi.socialanalyzer.application.dto.CollectionStatus;
import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import com.yagi.socialanalyzer.domain.exceptions.CollectionException;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import com.yagi.socialanalyzer.domain.repositories.IPostRepository;
import com.yagi.socialanalyzer.domain.repositories.IProjectRepository;
import com.yagi.socialanalyzer.infrastructure.datasources.IPlatformDataSource;
import com.yagi.socialanalyzer.infrastructure.persistence.JsonFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Data collection service orchestrating multiple platform data sources.
 */
public class DataCollectionService implements IDataCollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataCollectionService.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    
    private final IProjectRepository projectRepository;
    private final IPostRepository postRepository;
    private final JsonFileManager jsonFileManager;
    private final Map<String, IPlatformDataSource> dataSources;
    private final Map<UUID, Boolean> activeCollections;
    private final Map<UUID, Boolean> pausedCollections;
    
    public DataCollectionService(IProjectRepository projectRepository,
                                IPostRepository postRepository,
                                JsonFileManager jsonFileManager,
                                Map<String, IPlatformDataSource> dataSources) {
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.postRepository = Objects.requireNonNull(postRepository);
        this.jsonFileManager = Objects.requireNonNull(jsonFileManager);
        this.dataSources = new HashMap<>(dataSources);
        this.activeCollections = new ConcurrentHashMap<>();
        this.pausedCollections = new ConcurrentHashMap<>();
    }
    
    @Override
    public int startCollection(UUID projectId, Consumer<Double> progressCallback,
                              Consumer<String> statusCallback) throws CollectionException {
        logger.info("Starting data collection for project: {}", projectId);
        
        try {
            // Load project
            DisasterProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CollectionException("Project not found: " + projectId));
            
            // Mark collection as active and update status to COLLECTING
            activeCollections.put(projectId, true);
            project.startCollection();  // DRAFT → COLLECTING
            projectRepository.save(project);
            
            int totalCollected = 0;
            Set<String> platforms = project.getDataSources();
            int platformCount = platforms.size();
            int currentPlatformIndex = 0;
            
            // Collect from each platform
            for (String platformId : platforms) {
                if (!activeCollections.getOrDefault(projectId, false)) {
                    logger.info("Collection cancelled for project: {}", projectId);
                    break;
                }
                
                if (pausedCollections.getOrDefault(projectId, false)) {
                    logger.info("Collection paused for project: {}", projectId);
                    updateStatus(statusCallback, "Collection paused");
                    waitForResume(projectId);
                }
                
                IPlatformDataSource dataSource = dataSources.get(platformId);
                if (dataSource == null || !dataSource.isAvailable()) {
                    logger.warn("Platform not available: {}", platformId);
                    continue;
                }
                
                updateStatus(statusCallback, "Collecting from " + platformId + "...");
                
                try {
                    List<SocialMediaPost> posts = collectFromPlatform(
                        project, dataSource, platformId);
                    
                    if (!posts.isEmpty()) {
                        savePosts(project, posts, platformId);
                        totalCollected += posts.size();
                        
                        // Update project count after each batch (T062)
                        project.incrementPostCount(posts.size());
                        projectRepository.save(project);
                        
                        logger.debug("Updated project total posts: {}", project.getTotalPostsCollected());
                    }
                    
                } catch (Exception e) {
                    logger.error("Error collecting from platform: {}", platformId, e);
                    updateStatus(statusCallback, "Error on " + platformId + ": " + e.getMessage());
                    // Don't fail entire collection, continue with other platforms
                }
                
                // Update progress
                currentPlatformIndex++;
                double progress = (double) currentPlatformIndex / platformCount;
                updateProgress(progressCallback, progress);
            }
            
            // Mark collection complete (T061: COLLECTING → COMPLETED)
            activeCollections.remove(projectId);
            if (!pausedCollections.getOrDefault(projectId, false)) {
                project.completeCollection();
                projectRepository.save(project);
                logger.info("Project status updated to COMPLETED");
            }
            
            logger.info("Collection completed. Total posts collected: {}", totalCollected);
            updateStatus(statusCallback, "Collection complete: " + totalCollected + " posts");
            
            return totalCollected;
            
        } catch (RepositoryException e) {
            logger.error("Repository error during collection", e);
            throw new CollectionException("Collection failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during collection", e);
            // Try to mark project as failed if possible
            try {
                DisasterProject project = projectRepository.findById(projectId).orElse(null);
                if (project != null) {
                    // Note: We don't have a FAILED status in the enum, so we'll keep it in current state
                    // In a real implementation, add ProjectStatus.FAILED
                    projectRepository.save(project);
                }
            } catch (Exception saveError) {
                logger.error("Failed to update project after error", saveError);
            }
            throw new CollectionException("Collection failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void pauseCollection(UUID projectId) throws CollectionException {
        logger.info("Pausing collection for project: {}", projectId);
        pausedCollections.put(projectId, true);
        
        try {
            DisasterProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CollectionException("Project not found: " + projectId));
            project.pauseCollection();
            projectRepository.save(project);
        } catch (RepositoryException e) {
            throw new CollectionException("Failed to pause collection: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void resumeCollection(UUID projectId) throws CollectionException {
        logger.info("Resuming collection for project: {}", projectId);
        pausedCollections.put(projectId, false);
        
        synchronized (pausedCollections) {
            pausedCollections.notifyAll();
        }
    }
    
    @Override
    public CollectionStatus getStatus(UUID projectId) throws CollectionException {
        try {
            DisasterProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CollectionException("Project not found: " + projectId));
            
            long totalPosts = postRepository.countByProject(projectId);
            
            return new CollectionStatus(
                projectId.toString(),
                project.getName(),
                project.getStatus(),
                totalPosts,
                0, // postsCollectedToday - would need additional query
                null, // currentPlatform
                0.0, // completionPercentage
                Instant.now(),
                null
            );
            
        } catch (RepositoryException e) {
            throw new CollectionException("Failed to get status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void cancelCollection(UUID projectId) {
        logger.info("Cancelling collection for project: {}", projectId);
        activeCollections.put(projectId, false);
        pausedCollections.put(projectId, false);
    }
    
    private List<SocialMediaPost> collectFromPlatform(DisasterProject project,
                                                      IPlatformDataSource dataSource,
                                                      String platformId) throws CollectionException {
        List<String> keywords = new ArrayList<>(project.getKeywords());
        LocalDate startDate = project.getStartDate();
        LocalDate endDate = project.getEndDate();
        
        int maxResults = 1000; // Default max per platform
        
        return retryWithBackoff(() -> 
            dataSource.searchPosts(keywords, startDate, endDate, maxResults)
        );
    }
    
    private void savePosts(DisasterProject project, List<SocialMediaPost> posts, 
                          String platformId) throws RepositoryException {
        // Save in batches
        for (int i = 0; i < posts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, posts.size());
            List<SocialMediaPost> batch = posts.subList(i, end);
            
            // Save metadata to database
            postRepository.saveBatch(batch);
            
            // Save full data to JSON
            if (!batch.isEmpty()) {
                LocalDate date = batch.get(0).getPublishedAt()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                jsonFileManager.savePosts(project.getId(), platformId, date, batch);
            }
            
            logger.debug("Saved batch of {} posts", batch.size());
        }
    }
    
    private <T> T retryWithBackoff(ThrowingSupplier<T> operation) throws CollectionException {
        int attempt = 0;
        long backoff = INITIAL_BACKOFF_MS;
        
        while (attempt < MAX_RETRIES) {
            try {
                return operation.get();
            } catch (CollectionException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw e;
                }
                
                logger.warn("Attempt {} failed, retrying after {}ms: {}", 
                    attempt, backoff, e.getMessage());
                
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CollectionException("Collection interrupted", ie);
                }
                
                backoff *= 2; // Exponential backoff
            }
        }
        
        throw new CollectionException("Max retries exceeded");
    }
    
    private void waitForResume(UUID projectId) {
        synchronized (pausedCollections) {
            while (pausedCollections.getOrDefault(projectId, false)) {
                try {
                    pausedCollections.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private void updateProgress(Consumer<Double> callback, double progress) {
        if (callback != null) {
            callback.accept(progress);
        }
    }
    
    private void updateStatus(Consumer<String> callback, String status) {
        if (callback != null) {
            callback.accept(status);
        }
    }
    
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws CollectionException;
    }
}
