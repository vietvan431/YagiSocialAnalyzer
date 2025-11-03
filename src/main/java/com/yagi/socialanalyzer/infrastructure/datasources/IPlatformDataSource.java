package com.yagi.socialanalyzer.infrastructure.datasources;

import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import com.yagi.socialanalyzer.domain.exceptions.CollectionException;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface for platform-specific data sources (API or Selenium).
 */
public interface IPlatformDataSource {
    
    /**
     * Get platform identifier.
     */
    String getPlatformId();
    
    /**
     * Authenticate with the platform.
     */
    void authenticate(String credentials) throws CollectionException;
    
    /**
     * Search for posts matching keywords in date range.
     * 
     * @param keywords List of search keywords
     * @param startDate Start date for search
     * @param endDate End date for search
     * @param maxResults Maximum number of results to return
     * @return List of social media posts
     */
    List<SocialMediaPost> searchPosts(List<String> keywords, LocalDate startDate, 
                                     LocalDate endDate, int maxResults) throws CollectionException;
    
    /**
     * Check if platform is available and accessible.
     */
    boolean isAvailable();
    
    /**
     * Get remaining rate limit quota.
     * Returns -1 if rate limit is not applicable or unknown.
     */
    int getRemainingRateLimit();
    
    /**
     * Close and cleanup resources.
     */
    void close();
}
