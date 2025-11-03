# Service Contracts: Internal Interfaces

**Feature**: Disaster Social Media Analysis  
**Date**: 2025-10-31  
**Phase**: 1 - Interface Design

## Overview

This document defines the internal service interfaces for the Java application,
following DDD principles with clear separation between domain, application, and
infrastructure layers.

---

## Domain Layer Interfaces

### IProjectRepository

Persistence interface for DisasterProject aggregate.

```java
package com.yagi.socialanalyzer.domain.repositories;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.valueobjects.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing DisasterProject persistence.
 * Implementations: SQLiteProjectRepository
 */
public interface IProjectRepository {

    /**
     * Save a new project or update existing.
     *
     * @param project The project to save
     * @return Saved project with updated timestamps
     * @throws RepositoryException if save fails
     */
    DisasterProject save(DisasterProject project);

    /**
     * Find project by unique identifier.
     *
     * @param id Project UUID
     * @return Optional containing project if found
     */
    Optional<DisasterProject> findById(UUID id);

    /**
     * Find all projects matching status.
     *
     * @param status Filter by project status
     * @return List of matching projects (empty if none)
     */
    List<DisasterProject> findByStatus(ProjectStatus status);

    /**
     * Find all projects, ordered by updatedAt descending.
     *
     * @return All projects (most recent first)
     */
    List<DisasterProject> findAll();

    /**
     * Delete project and all associated data (cascades to posts, analyses).
     *
     * @param id Project UUID to delete
     * @return true if deleted, false if not found
     * @throws RepositoryException if deletion fails mid-operation
     */
    boolean delete(UUID id);

    /**
     * Check if project name already exists (case-insensitive).
     *
     * @param name Project name to check
     * @return true if name exists
     */
    boolean existsByName(String name);
}
```

---

### IPostRepository

Persistence interface for SocialMediaPost aggregate.

```java
package com.yagi.socialanalyzer.domain.repositories;

import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPostRepository {

    /**
     * Save post metadata to database, full content to JSON file.
     *
     * @param post The post to save
     * @return Saved post with filePath populated
     * @throws RepositoryException if save fails
     */
    SocialMediaPost save(SocialMediaPost post);

    /**
     * Batch save posts (optimized for bulk collection).
     *
     * @param posts List of posts to save
     * @return Count of successfully saved posts
     * @throws RepositoryException if batch transaction fails
     */
    int saveBatch(List<SocialMediaPost> posts);

    /**
     * Find post by composite ID (platform_externalId).
     *
     * @param id Post composite ID
     * @return Optional containing post if found (with content loaded from JSON)
     */
    Optional<SocialMediaPost> findById(String id);

    /**
     * Find all posts for a project within date range.
     *
     * @param projectId Project UUID
     * @param startDate Start instant (inclusive)
     * @param endDate End instant (inclusive)
     * @return List of posts ordered by publishedAt ascending
     */
    List<SocialMediaPost> findByProjectAndDateRange(UUID projectId, Instant startDate, Instant endDate);

    /**
     * Find posts by project and platform.
     *
     * @param projectId Project UUID
     * @param platform Platform ID (e.g., "twitter")
     * @return List of posts from specified platform
     */
    List<SocialMediaPost> findByProjectAndPlatform(UUID projectId, String platform);

    /**
     * Count total posts for a project.
     *
     * @param projectId Project UUID
     * @return Total post count
     */
    long countByProject(UUID projectId);

    /**
     * Delete all posts for a project (triggered by project deletion cascade).
     *
     * @param projectId Project UUID
     * @return Count of deleted posts
     */
    int deleteByProject(UUID projectId);
}
```

---

### ISentimentAnalyzer

Domain service for sentiment classification.

```java
package com.yagi.socialanalyzer.domain.services;

import com.yagi.socialanalyzer.domain.entities.SentimentResult;
import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import java.util.List;

/**
 * Sentiment analysis domain service.
 * Implementations: JavaLexiconSentimentAnalyzer, PythonApiSentimentAnalyzer
 */
public interface ISentimentAnalyzer {

    /**
     * Analyze sentiment of a single post.
     *
     * @param post The post to analyze
     * @return Sentiment result with label, confidence, scores
     * @throws AnalysisException if analysis fails
     */
    SentimentResult analyze(SocialMediaPost post);

    /**
     * Batch analyze posts for efficiency.
     *
     * @param posts List of posts to analyze
     * @return List of sentiment results (same order as input)
     * @throws AnalysisException if batch analysis fails
     */
    List<SentimentResult> analyzeBatch(List<SocialMediaPost> posts);

    /**
     * Get analyzer type identifier.
     *
     * @return Type string (e.g., "java_lexicon", "python_phobert")
     */
    String getAnalyzerType();

    /**
     * Check if analyzer supports specific language.
     *
     * @param languageCode ISO 639-1 code (e.g., "vi", "en")
     * @return true if language supported
     */
    boolean supportsLanguage(String languageCode);
}
```

---

### IDamageClassifier

Domain service for damage categorization.

```java
package com.yagi.socialanalyzer.domain.services;

import com.yagi.socialanalyzer.domain.entities.DamageCategory;
import com.yagi.socialanalyzer.domain.entities.DamageClassification;
import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import java.util.List;

/**
 * Damage categorization domain service.
 * Implementation: KeywordBasedDamageClassifier
 */
public interface IDamageClassifier {

    /**
     * Classify post into damage categories (multi-label).
     *
     * @param post The post to classify
     * @param categories Available damage categories
     * @param minimumMatches Minimum keyword matches to assign category
     * @return List of matched categories (empty if none match)
     */
    List<DamageClassification> classify(SocialMediaPost post,
                                        List<DamageCategory> categories,
                                        int minimumMatches);

    /**
     * Batch classify posts.
     *
     * @param posts List of posts to classify
     * @param categories Available damage categories
     * @param minimumMatches Minimum keyword matches threshold
     * @return List of lists (outer list matches posts, inner list is classifications per post)
     */
    List<List<DamageClassification>> classifyBatch(List<SocialMediaPost> posts,
                                                    List<DamageCategory> categories,
                                                    int minimumMatches);
}
```

---

## Application Layer Interfaces

### IDataCollectionService

Orchestrates data collection from multiple platforms.

```java
package com.yagi.socialanalyzer.application.services;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import java.util.List;
import java.util.function.Consumer;

/**
 * Application service coordinating data collection workflow.
 * Implementation: DataCollectionService
 */
public interface IDataCollectionService {

    /**
     * Start data collection for a project.
     *
     * @param project The project to collect data for
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @return Total posts collected across all platforms
     * @throws CollectionException if collection fails
     */
    int startCollection(DisasterProject project, Consumer<Double> progressCallback);

    /**
     * Pause ongoing collection.
     *
     * @param projectId Project UUID
     */
    void pauseCollection(UUID projectId);

    /**
     * Resume paused collection.
     *
     * @param projectId Project UUID
     * @param progressCallback Progress callback
     * @return Additional posts collected
     */
    int resumeCollection(UUID projectId, Consumer<Double> progressCallback);

    /**
     * Get collection status.
     *
     * @param projectId Project UUID
     * @return Status object with current platform, progress percentage, posts collected
     */
    CollectionStatus getStatus(UUID projectId);
}
```

---

### IAnalysisService

Orchestrates sentiment and damage analysis.

```java
package com.yagi.socialanalyzer.application.services;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import java.util.function.Consumer;

public interface IAnalysisService {

    /**
     * Run full analysis (sentiment + damage) for project posts.
     *
     * @param project The project to analyze
     * @param progressCallback Callback for progress updates
     * @return Analysis summary with counts and statistics
     * @throws AnalysisException if analysis fails
     */
    AnalysisSummary runAnalysis(DisasterProject project, Consumer<Double> progressCallback);

    /**
     * Run sentiment analysis only.
     *
     * @param project The project
     * @param progressCallback Progress callback
     * @return Sentiment analysis summary
     */
    SentimentSummary runSentimentAnalysis(DisasterProject project, Consumer<Double> progressCallback);

    /**
     * Run damage categorization only.
     *
     * @param project The project
     * @param progressCallback Progress callback
     * @return Damage analysis summary
     */
    DamageSummary runDamageAnalysis(DisasterProject project, Consumer<Double> progressCallback);
}
```

---

### IExportService

Handles report generation and export.

```java
package com.yagi.socialanalyzer.application.services;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.entities.ExportReport;
import com.yagi.socialanalyzer.domain.valueobjects.ReportType;
import com.yagi.socialanalyzer.domain.valueobjects.ReportFormat;
import java.nio.file.Path;
import java.util.Map;

public interface IExportService {

    /**
     * Generate report for project.
     *
     * @param project The project to report on
     * @param type Report type (SENTIMENT_ANALYSIS, DAMAGE_CATEGORIZATION, COMBINED)
     * @param format Output format (CSV, PDF, JSON)
     * @param parameters Optional filters (date range, platforms, etc.)
     * @return Generated report metadata
     * @throws ExportException if generation fails
     */
    ExportReport generateReport(DisasterProject project,
                               ReportType type,
                               ReportFormat format,
                               Map<String, Object> parameters);

    /**
     * Get path to generated report file.
     *
     * @param report Report metadata
     * @return Path to file on disk
     */
    Path getReportPath(ExportReport report);
}
```

---

## Infrastructure Layer Interfaces

### IPlatformDataSource

Interface for platform-specific data collectors.

```java
package com.yagi.socialanalyzer.infrastructure.datasources;

import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import java.time.Instant;
import java.util.List;

/**
 * Platform-specific data collection interface.
 * Implementations: TwitterDataSource, RedditDataSource, FacebookSeleniumDataSource, etc.
 */
public interface IPlatformDataSource {

    /**
     * Get platform identifier.
     *
     * @return Platform ID (e.g., "twitter", "reddit")
     */
    String getPlatformId();

    /**
     * Authenticate with platform.
     *
     * @param credentials Platform-specific credentials
     * @throws AuthenticationException if auth fails
     */
    void authenticate(Map<String, String> credentials);

    /**
     * Search posts by keywords and date range.
     *
     * @param keywords Search keywords
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param maxResults Maximum posts to retrieve
     * @return List of collected posts
     * @throws CollectionException if collection fails
     */
    List<SocialMediaPost> searchPosts(List<String> keywords,
                                     Instant startDate,
                                     Instant endDate,
                                     int maxResults);

    /**
     * Check if platform is currently reachable.
     *
     * @return true if platform API/website is accessible
     */
    boolean isAvailable();

    /**
     * Get current rate limit status.
     *
     * @return Remaining requests in current window
     */
    int getRemainingRateLimit();
}
```

---

### ICredentialStore

Secure credential storage interface.

```java
package com.yagi.socialanalyzer.infrastructure.security;

import java.util.Optional;

/**
 * Secure credential storage interface.
 * Implementation: EncryptedCredentialStore (AES-256 + OS keyring)
 */
public interface ICredentialStore {

    /**
     * Store credential securely.
     *
     * @param key Credential key (e.g., "twitter_api_key")
     * @param value Credential value (plaintext, will be encrypted)
     * @throws SecurityException if encryption fails
     */
    void store(String key, String value);

    /**
     * Retrieve credential.
     *
     * @param key Credential key
     * @return Decrypted credential value, or empty if not found
     * @throws SecurityException if decryption fails
     */
    Optional<String> retrieve(String key);

    /**
     * Delete credential.
     *
     * @param key Credential key
     * @return true if deleted, false if not found
     */
    boolean delete(String key);

    /**
     * List all stored credential keys (not values).
     *
     * @return List of credential keys
     */
    List<String> listKeys();
}
```

---

### IConfigurationProvider

Configuration loading interface.

```java
package com.yagi.socialanalyzer.infrastructure.config;

import com.yagi.socialanalyzer.domain.entities.DataSource;
import com.yagi.socialanalyzer.domain.entities.DamageCategory;
import java.util.List;

/**
 * Configuration provider for platforms and damage categories.
 * Implementation: YamlConfigurationProvider
 */
public interface IConfigurationProvider {

    /**
     * Load platform configurations from YAML.
     *
     * @return List of configured data sources
     * @throws ConfigurationException if load fails
     */
    List<DataSource> loadPlatforms();

    /**
     * Load damage categories from YAML.
     *
     * @return List of damage categories
     * @throws ConfigurationException if load fails
     */
    List<DamageCategory> loadDamageCategories();

    /**
     * Reload configurations (hot reload).
     */
    void reload();
}
```

---

## DTO Objects

### CollectionStatus

```java
package com.yagi.socialanalyzer.application.dto;

import java.util.UUID;

public record CollectionStatus(
    UUID projectId,
    String currentPlatform,
    double progressPercentage,
    long postsCollected,
    boolean isRunning,
    String statusMessage
) {}
```

### AnalysisSummary

```java
package com.yagi.socialanalyzer.application.dto;

public record AnalysisSummary(
    long totalPostsAnalyzed,
    SentimentSummary sentimentSummary,
    DamageSummary damageSummary,
    long durationMillis
) {}
```

### SentimentSummary

```java
package com.yagi.socialanalyzer.application.dto;

public record SentimentSummary(
    long positiveCount,
    long negativeCount,
    long neutralCount,
    double averageConfidence
) {}
```

### DamageSummary

```java
package com.yagi.socialanalyzer.application.dto;

import java.util.Map;

public record DamageSummary(
    Map<String, Long> categoryDistribution,  // categoryId -> count
    long totalPostsClassified,
    long postsWithMultipleCategories
) {}
```

---

## Exception Hierarchy

```java
// Base exception
package com.yagi.socialanalyzer.domain.exceptions;

public class DomainException extends RuntimeException {
    public DomainException(String message) { super(message); }
    public DomainException(String message, Throwable cause) { super(message, cause); }
}

// Specific exceptions
public class RepositoryException extends DomainException {}
public class AnalysisException extends DomainException {}
public class CollectionException extends DomainException {}
public class AuthenticationException extends DomainException {}
public class ConfigurationException extends DomainException {}
public class ExportException extends DomainException {}
public class SecurityException extends DomainException {}
```

---

## Interface Usage Examples

### Typical Collection Workflow

```java
// In JavaFX Task
IDataCollectionService collectionService = ...;
DisasterProject project = ...;

Task<Integer> collectionTask = new Task<>() {
    @Override
    protected Integer call() throws Exception {
        return collectionService.startCollection(project, progress -> {
            updateProgress(progress, 1.0);
            updateMessage(String.format("Collecting... %.0f%%", progress * 100));
        });
    }
};

collectionTask.setOnSucceeded(e -> {
    int totalPosts = collectionTask.getValue();
    showAlert("Collection complete", totalPosts + " posts collected");
});

new Thread(collectionTask).start();
```

### Analysis Pipeline

```java
IAnalysisService analysisService = ...;
DisasterProject project = ...;

AnalysisSummary summary = analysisService.runAnalysis(project, progress -> {
    progressBar.setProgress(progress);
});

System.out.println("Positive: " + summary.sentimentSummary().positiveCount());
System.out.println("Negative: " + summary.sentimentSummary().negativeCount());
```

---

## Summary

These interfaces define clear contracts between layers:

- **Domain repositories**: Persistence abstraction (no SQL in domain)
- **Domain services**: Stateless business logic (sentiment, damage
  classification)
- **Application services**: Orchestration and workflow coordination
- **Infrastructure interfaces**: External system adapters (platforms,
  credentials, config)

All interfaces follow OOP principles: single responsibility, dependency
inversion (domain doesn't depend on infrastructure), and clear separation of
concerns. Implementations will be injected via constructor-based dependency
injection.
