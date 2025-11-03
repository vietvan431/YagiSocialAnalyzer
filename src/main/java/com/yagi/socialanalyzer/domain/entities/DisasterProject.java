package com.yagi.socialanalyzer.domain.entities;

import com.yagi.socialanalyzer.domain.valueobjects.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Disaster Project entity representing a research project for a specific disaster event.
 * Aggregate root for the Project Management context.
 */
public class DisasterProject {
    
    private final UUID id;
    private String name;
    private String disasterName;
    private String region;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<String> keywords;
    private Set<String> dataSources;
    private ProjectStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private long totalPostsCollected;
    
    /**
     * Create a new disaster project.
     */
    public DisasterProject(UUID id, String name, String disasterName, String region,
                          LocalDate startDate, LocalDate endDate, Set<String> keywords,
                          Set<String> dataSources) {
        this.id = Objects.requireNonNull(id, "Project ID cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = ProjectStatus.DRAFT;
        this.totalPostsCollected = 0;
        
        setName(name);
        setDisasterName(disasterName);
        setRegion(region);
        setDateRange(startDate, endDate);
        setKeywords(keywords);
        setDataSources(dataSources);
    }
    
    /**
     * Reconstitute project from persistence.
     */
    public DisasterProject(UUID id, String name, String disasterName, String region,
                          LocalDate startDate, LocalDate endDate, Set<String> keywords,
                          Set<String> dataSources, ProjectStatus status,
                          Instant createdAt, Instant updatedAt, long totalPostsCollected) {
        this.id = Objects.requireNonNull(id, "Project ID cannot be null");
        this.name = name;
        this.disasterName = disasterName;
        this.region = region;
        this.startDate = startDate;
        this.endDate = endDate;
        this.keywords = new HashSet<>(keywords);
        this.dataSources = new HashSet<>(dataSources);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalPostsCollected = totalPostsCollected;
    }
    
    // Setters with validation
    
    public void setName(String name) {
        Objects.requireNonNull(name, "Project name cannot be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty");
        }
        if (trimmed.length() > 200) {
            throw new IllegalArgumentException("Project name cannot exceed 200 characters");
        }
        this.name = trimmed;
        this.updatedAt = Instant.now();
    }
    
    public void setDisasterName(String disasterName) {
        Objects.requireNonNull(disasterName, "Disaster name cannot be null");
        String trimmed = disasterName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Disaster name cannot be empty");
        }
        if (trimmed.length() > 200) {
            throw new IllegalArgumentException("Disaster name cannot exceed 200 characters");
        }
        this.disasterName = trimmed;
        this.updatedAt = Instant.now();
    }
    
    public void setRegion(String region) {
        Objects.requireNonNull(region, "Region cannot be null");
        String trimmed = region.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Region cannot be empty");
        }
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Region cannot exceed 100 characters");
        }
        this.region = trimmed;
        this.updatedAt = Instant.now();
    }
    
    public void setDateRange(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = Instant.now();
    }
    
    public void setKeywords(Set<String> keywords) {
        Objects.requireNonNull(keywords, "Keywords cannot be null");
        if (keywords.isEmpty()) {
            throw new IllegalArgumentException("At least one keyword is required");
        }
        if (keywords.size() > 50) {
            throw new IllegalArgumentException("Cannot exceed 50 keywords");
        }
        
        // Validate each keyword
        Set<String> validated = new HashSet<>();
        for (String keyword : keywords) {
            String trimmed = keyword.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Keywords cannot be empty");
            }
            if (trimmed.length() > 100) {
                throw new IllegalArgumentException("Keyword cannot exceed 100 characters: " + trimmed);
            }
            validated.add(trimmed);
        }
        
        this.keywords = validated;
        this.updatedAt = Instant.now();
    }
    
    public void setDataSources(Set<String> dataSources) {
        Objects.requireNonNull(dataSources, "Data sources cannot be null");
        if (dataSources.isEmpty()) {
            throw new IllegalArgumentException("At least one data source is required");
        }
        this.dataSources = new HashSet<>(dataSources);
        this.updatedAt = Instant.now();
    }
    
    // State transitions
    
    public void startCollection() {
        if (status != ProjectStatus.DRAFT && status != ProjectStatus.PAUSED) {
            throw new IllegalStateException(
                "Can only start collection from DRAFT or PAUSED status, current: " + status);
        }
        this.status = ProjectStatus.COLLECTING;
        this.updatedAt = Instant.now();
    }
    
    public void pauseCollection() {
        if (status != ProjectStatus.COLLECTING) {
            throw new IllegalStateException("Can only pause when COLLECTING, current: " + status);
        }
        this.status = ProjectStatus.PAUSED;
        this.updatedAt = Instant.now();
    }
    
    public void completeCollection() {
        if (status != ProjectStatus.COLLECTING) {
            throw new IllegalStateException("Can only complete from COLLECTING, current: " + status);
        }
        this.status = ProjectStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }
    
    public void startAnalysis() {
        if (status != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("Can only analyze COMPLETED projects, current: " + status);
        }
        this.status = ProjectStatus.ANALYZING;
        this.updatedAt = Instant.now();
    }
    
    public void completeAnalysis() {
        if (status != ProjectStatus.ANALYZING) {
            throw new IllegalStateException("Can only complete from ANALYZING, current: " + status);
        }
        this.status = ProjectStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }
    
    public void archive() {
        if (status != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("Can only archive COMPLETED projects, current: " + status);
        }
        this.status = ProjectStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }
    
    public void incrementPostCount(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count increment cannot be negative");
        }
        this.totalPostsCollected += count;
        this.updatedAt = Instant.now();
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisasterName() {
        return disasterName;
    }
    
    public String getRegion() {
        return region;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public Set<String> getKeywords() {
        return new HashSet<>(keywords);
    }
    
    public Set<String> getDataSources() {
        return new HashSet<>(dataSources);
    }
    
    public ProjectStatus getStatus() {
        return status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public long getTotalPostsCollected() {
        return totalPostsCollected;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisasterProject that = (DisasterProject) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "DisasterProject{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", disasterName='" + disasterName + '\'' +
                ", status=" + status +
                ", postsCollected=" + totalPostsCollected +
                '}';
    }
}
