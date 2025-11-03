package com.yagi.socialanalyzer.domain.entities;

import com.yagi.socialanalyzer.domain.valueobjects.EngagementMetrics;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Social Media Post entity representing a collected post from any platform.
 */
public class SocialMediaPost {
    
    private final UUID projectId;
    private final String platform;
    private final String postId;
    private String author;
    private String content;
    private Instant publishedAt;
    private EngagementMetrics engagementMetrics;
    private String url;
    private final Instant collectedAt;
    private String dataFilePath;
    
    /**
     * Create a new social media post.
     */
    public SocialMediaPost(UUID projectId, String platform, String postId, String author,
                          String content, Instant publishedAt, EngagementMetrics engagementMetrics,
                          String url, Instant collectedAt, String dataFilePath) {
        this.projectId = Objects.requireNonNull(projectId, "Project ID cannot be null");
        this.platform = Objects.requireNonNull(platform, "Platform cannot be null");
        this.postId = Objects.requireNonNull(postId, "Post ID cannot be null");
        this.collectedAt = Objects.requireNonNull(collectedAt, "Collected at cannot be null");
        
        setAuthor(author);
        setContent(content);
        setPublishedAt(publishedAt);
        setEngagementMetrics(engagementMetrics);
        setUrl(url);
        setDataFilePath(dataFilePath);
    }
    
    // Setters with validation
    
    public void setAuthor(String author) {
        Objects.requireNonNull(author, "Author cannot be null");
        String trimmed = author.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Author cannot be empty");
        }
        this.author = trimmed;
    }
    
    public void setContent(String content) {
        Objects.requireNonNull(content, "Content cannot be null");
        this.content = content;
    }
    
    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = Objects.requireNonNull(publishedAt, "Published at cannot be null");
    }
    
    public void setEngagementMetrics(EngagementMetrics engagementMetrics) {
        this.engagementMetrics = Objects.requireNonNull(engagementMetrics, 
            "Engagement metrics cannot be null");
    }
    
    public void setUrl(String url) {
        this.url = url; // Can be null
    }
    
    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath; // Can be null initially
    }
    
    // Getters
    
    public UUID getProjectId() {
        return projectId;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public String getPostId() {
        return postId;
    }
    
    public String getCompositeId() {
        return projectId + ":" + platform + ":" + postId;
    }
    
    public String author() {
        return author;
    }
    
    public String getContent() {
        return content;
    }
    
    public Instant getPublishedAt() {
        return publishedAt;
    }
    
    public EngagementMetrics getEngagementMetrics() {
        return engagementMetrics;
    }
    
    public String getUrl() {
        return url;
    }
    
    public Instant getCollectedAt() {
        return collectedAt;
    }
    
    public String getDataFilePath() {
        return dataFilePath;
    }
    
    /**
     * Get content preview (first 100 characters).
     */
    public String getContentPreview() {
        if (content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100) + "...";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocialMediaPost that = (SocialMediaPost) o;
        return Objects.equals(projectId, that.projectId) &&
               Objects.equals(platform, that.platform) &&
               Objects.equals(postId, that.postId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(projectId, platform, postId);
    }
    
    @Override
    public String toString() {
        return "SocialMediaPost{" +
                "platform='" + platform + '\'' +
                ", postId='" + postId + '\'' +
                ", author='" + author + '\'' +
                ", publishedAt=" + publishedAt +
                '}';
    }
}
