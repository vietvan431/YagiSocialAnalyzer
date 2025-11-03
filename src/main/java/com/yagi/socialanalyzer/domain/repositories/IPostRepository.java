package com.yagi.socialanalyzer.domain.repositories;

import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SocialMediaPost persistence.
 */
public interface IPostRepository {
    
    /**
     * Save a single post.
     */
    void save(SocialMediaPost post) throws RepositoryException;
    
    /**
     * Save a batch of posts (optimized).
     */
    void saveBatch(List<SocialMediaPost> posts) throws RepositoryException;
    
    /**
     * Find post by composite ID.
     */
    Optional<SocialMediaPost> findById(UUID projectId, String platform, String postId) 
        throws RepositoryException;
    
    /**
     * Find posts by project and date range.
     */
    List<SocialMediaPost> findByProjectAndDateRange(UUID projectId, LocalDate startDate, 
        LocalDate endDate) throws RepositoryException;
    
    /**
     * Find posts by project and platform.
     */
    List<SocialMediaPost> findByProjectAndPlatform(UUID projectId, String platform) 
        throws RepositoryException;
    
    /**
     * Count posts by project.
     */
    long countByProject(UUID projectId) throws RepositoryException;
    
    /**
     * Delete all posts for a project.
     */
    void deleteByProject(UUID projectId) throws RepositoryException;
    
    /**
     * Check if post exists.
     */
    boolean exists(UUID projectId, String platform, String postId) throws RepositoryException;
}
