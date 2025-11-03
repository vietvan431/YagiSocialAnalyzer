# Data Model: Disaster Social Media Analysis System

**Feature**: Disaster Social Media Analysis  
**Date**: 2025-10-31  
**Phase**: 1 - Domain Model Design

## Overview

This document defines the core domain entities, their relationships, validation
rules, and state transitions for the disaster social media analysis system. The
model follows Domain-Driven Design principles with clear bounded contexts.

## Domain Contexts

### 1. Project Management Context

Manages disaster research projects and their configurations.

### 2. Data Collection Context

Handles social media post collection from multiple platforms.

### 3. Analysis Context

Performs sentiment analysis and damage categorization on collected data.

### 4. Configuration Context

Manages platform definitions, keywords, and analysis parameters.

---

## Core Entities

### DisasterProject

Represents a research project focused on a specific disaster event.

**Attributes**:

| Field                 | Type          | Constraints                                  | Description                                             |
| --------------------- | ------------- | -------------------------------------------- | ------------------------------------------------------- |
| `id`                  | UUID          | Primary key, not null                        | Unique project identifier                               |
| `name`                | String        | Not null, max 200 chars, unique              | User-friendly project name                              |
| `disasterName`        | String        | Not null, max 200 chars                      | Disaster event name (e.g., "Typhoon Yagi")              |
| `region`              | String        | Not null, max 100 chars                      | Geographic region (e.g., "Vietnam", "Northern Vietnam") |
| `startDate`           | LocalDate     | Not null, <= endDate                         | Collection period start (Vietnam ICT timezone)          |
| `endDate`             | LocalDate     | Not null, >= startDate, >= today allowed     | Collection period end (can be future for ongoing)       |
| `keywords`            | Set<String>   | Not empty, 1-50 keywords, max 100 chars each | Search keywords (multilingual)                          |
| `dataSources`         | Set<String>   | Not empty, valid platform IDs                | Enabled platform IDs (e.g., "twitter", "reddit")        |
| `status`              | ProjectStatus | Not null, enum                               | Current project state                                   |
| `createdAt`           | Instant       | Not null, auto-set                           | Creation timestamp (UTC)                                |
| `updatedAt`           | Instant       | Not null, auto-update                        | Last modification timestamp (UTC)                       |
| `totalPostsCollected` | long          | >= 0                                         | Cached count of collected posts                         |

**Enum: ProjectStatus**

- `DRAFT`: Newly created, not started collection
- `COLLECTING`: Active data collection in progress
- `PAUSED`: Collection paused, can resume
- `COMPLETED`: Collection finished
- `ANALYZING`: Analysis in progress
- `ARCHIVED`: Completed and archived

**Validation Rules**:

- Keywords must contain at least one non-whitespace character
- Date range must be <= 365 days (prevent accidental year-long collections)
- At least one data source must be selected
- Status transitions follow state machine (see below)

**State Transitions**:

```text
DRAFT → COLLECTING (start collection)
COLLECTING → PAUSED (pause)
PAUSED → COLLECTING (resume)
COLLECTING → COMPLETED (finish)
COMPLETED → ANALYZING (start analysis)
ANALYZING → COMPLETED (analysis done)
COMPLETED → ARCHIVED (archive)
```

**Relationships**:

- One-to-many with `SocialMediaPost`
- One-to-many with `SentimentResult`
- One-to-many with `DamageClassification`

---

### SocialMediaPost

Represents a collected social media post with metadata.

**Attributes**:

| Field               | Type                | Constraints                                              | Description                                 |
| ------------------- | ------------------- | -------------------------------------------------------- | ------------------------------------------- |
| `id`                | String              | Primary key, not null, format: `{platform}_{externalId}` | Unique post identifier                      |
| `projectId`         | UUID                | Foreign key, not null                                    | Reference to DisasterProject                |
| `platform`          | String              | Not null, valid platform ID                              | Source platform (e.g., "twitter")           |
| `externalId`        | String              | Not null                                                 | Platform-specific post ID                   |
| `content`           | String              | Not null, max 50,000 chars                               | Post text content                           |
| `author`            | String              | Not null, max 200 chars                                  | Anonymized author identifier                |
| `publishedAt`       | Instant             | Not null                                                 | Original post timestamp (UTC)               |
| `collectedAt`       | Instant             | Not null, auto-set                                       | Collection timestamp (UTC)                  |
| `language`          | String              | ISO 639-1 code (e.g., "vi", "en")                        | Detected language                           |
| `locationTags`      | List<String>        | Optional                                                 | Geographic tags if available                |
| `engagementMetrics` | EngagementMetrics   | Embedded object                                          | Likes, shares, comments counts              |
| `rawMetadata`       | Map<String, Object> | JSON blob                                                | Platform-specific extra data                |
| `filePath`          | String              | Not null                                                 | Path to JSON file containing full post data |

**Embedded Object: EngagementMetrics**

| Field      | Type | Constraints         |
| ---------- | ---- | ------------------- |
| `likes`    | int  | >= 0                |
| `shares`   | int  | >= 0                |
| `comments` | int  | >= 0                |
| `views`    | long | >= 0 (if available) |

**Validation Rules**:

- Content must not be empty after trimming whitespace
- PublishedAt must be within project date range (tolerance: ±7 days)
- Language code must be valid ISO 639-1 (or null if detection fails)

**Relationships**:

- Many-to-one with `DisasterProject`
- One-to-one with `SentimentResult` (optional, created after analysis)
- One-to-many with `DamageClassification` (multi-label)

---

### DataSource

Represents a social media platform configuration.

**Attributes**:

| Field             | Type                | Constraints                      | Description                                       |
| ----------------- | ------------------- | -------------------------------- | ------------------------------------------------- |
| `id`              | String              | Primary key, not null, lowercase | Platform identifier (e.g., "twitter")             |
| `name`            | String              | Not null, max 100 chars          | Display name (e.g., "Twitter/X")                  |
| `type`            | DataSourceType      | Not null, enum                   | Collection method                                 |
| `baseUrl`         | String              | Valid URL                        | API endpoint or website base URL                  |
| `authType`        | AuthenticationType  | Not null, enum                   | Authentication method                             |
| `rateLimit`       | int                 | > 0                              | Requests per time window                          |
| `rateLimitWindow` | Duration            | Not null                         | Time window for rate limit (e.g., PT15M = 15 min) |
| `enabled`         | boolean             | Not null                         | Whether platform is active                        |
| `configParams`    | Map<String, String> | JSON                             | Platform-specific parameters                      |

**Enum: DataSourceType**

- `API`: Official platform API
- `SELENIUM`: Web scraping via Selenium

**Enum: AuthenticationType**

- `NONE`: No authentication
- `API_KEY`: Single API key
- `OAUTH2`: OAuth 2.0 flow
- `SESSION_LOGIN`: Username/password via Selenium

**Validation Rules**:

- `baseUrl` must be valid HTTPS URL (HTTP allowed only for localhost)
- `rateLimit` must match platform documented limits
- `configParams` validated against platform-specific schema

**Relationships**:

- Referenced by `DisasterProject.dataSources` (many-to-many conceptually)

---

### SentimentResult

Represents sentiment analysis result for a post.

**Attributes**:

| Field            | Type           | Constraints                   | Description                                            |
| ---------------- | -------------- | ----------------------------- | ------------------------------------------------------ |
| `id`             | UUID           | Primary key, not null         | Unique result identifier                               |
| `postId`         | String         | Foreign key, not null, unique | Reference to SocialMediaPost                           |
| `projectId`      | UUID           | Foreign key, not null         | Reference to DisasterProject                           |
| `sentimentLabel` | SentimentLabel | Not null, enum                | Classified sentiment                                   |
| `confidence`     | double         | 0.0 to 1.0                    | Classification confidence score                        |
| `positiveScore`  | double         | 0.0 to 1.0                    | Positive sentiment score                               |
| `negativeScore`  | double         | 0.0 to 1.0                    | Negative sentiment score                               |
| `neutralScore`   | double         | 0.0 to 1.0                    | Neutral sentiment score                                |
| `analyzedAt`     | Instant        | Not null, auto-set            | Analysis timestamp (UTC)                               |
| `analyzerType`   | String         | Not null                      | Analyzer used (e.g., "java_lexicon", "python_phobert") |
| `explanation`    | String         | Optional, max 500 chars       | Key words/phrases influencing classification           |

**Enum: SentimentLabel**

- `POSITIVE`: Optimistic, hopeful, supportive content
- `NEGATIVE`: Fearful, angry, sad, critical content
- `NEUTRAL`: Factual, informational, balanced content

**Validation Rules**:

- `positiveScore + negativeScore + neutralScore == 1.0` (±0.01 tolerance for
  rounding)
- `sentimentLabel` matches highest score (e.g., if `negativeScore` highest,
  label is `NEGATIVE`)
- `confidence >= 0.5` recommended for classification (below 0.5 may indicate
  ambiguous content)

**Relationships**:

- One-to-one with `SocialMediaPost`
- Many-to-one with `DisasterProject`

---

### DamageCategory

Represents a damage type with associated keywords.

**Attributes**:

| Field                | Type         | Constraints                                 | Description                                   |
| -------------------- | ------------ | ------------------------------------------- | --------------------------------------------- |
| `id`                 | String       | Primary key, not null, lowercase_underscore | Category identifier (e.g., "affected_people") |
| `name`               | String       | Not null, max 100 chars                     | Display name (e.g., "Affected People")        |
| `keywordsVietnamese` | List<String> | Not empty, max 200 keywords                 | Vietnamese trigger keywords                   |
| `keywordsEnglish`    | List<String> | Not empty, max 200 keywords                 | English trigger keywords                      |
| `description`        | String       | Optional, max 500 chars                     | Category description for users                |
| `priority`           | int          | 1-10                                        | Display priority (1 = highest)                |
| `color`              | String       | Hex color code (#RRGGBB)                    | Chart color for visualization                 |
| `version`            | int          | >= 1                                        | Configuration version (increments on edit)    |

**Predefined Categories**:

| ID                       | Name                     | Priority | Color                 |
| ------------------------ | ------------------------ | -------- | --------------------- |
| `affected_people`        | Affected People          | 1        | #E53935 (red)         |
| `economic_disruption`    | Economic Disruption      | 2        | #FB8C00 (orange)      |
| `damaged_buildings`      | Damaged Houses/Buildings | 3        | #F4511E (deep orange) |
| `lost_property`          | Lost Personal Property   | 4        | #8E24AA (purple)      |
| `damaged_infrastructure` | Damaged Infrastructure   | 5        | #3949AB (indigo)      |
| `other`                  | Other                    | 6        | #757575 (gray)        |

**Validation Rules**:

- Keywords must not be empty strings after trimming
- No duplicate keywords within same language list
- At least one keyword in Vietnamese OR English (multilingual posts supported)

**Relationships**:

- One-to-many with `DamageClassification`

---

### DamageClassification

Represents damage category assignment for a post (supports multi-label).

**Attributes**:

| Field             | Type         | Constraints           | Description                                          |
| ----------------- | ------------ | --------------------- | ---------------------------------------------------- |
| `id`              | UUID         | Primary key, not null | Unique classification identifier                     |
| `postId`          | String       | Foreign key, not null | Reference to SocialMediaPost                         |
| `projectId`       | UUID         | Foreign key, not null | Reference to DisasterProject                         |
| `categoryId`      | String       | Foreign key, not null | Reference to DamageCategory                          |
| `confidence`      | double       | 0.0 to 1.0            | Classification confidence (based on keyword matches) |
| `matchedKeywords` | List<String> | Not empty             | Keywords that triggered this category                |
| `analyzedAt`      | Instant      | Not null, auto-set    | Classification timestamp (UTC)                       |
| `categoryVersion` | int          | >= 1                  | DamageCategory version used for classification       |

**Validation Rules**:

- A post can have multiple `DamageClassification` records (one per assigned
  category)
- `matchedKeywords` must be subset of category's keyword list
- `confidence` calculated as: `min(1.0, matchedKeywords.size() / 3.0)` (3+
  matches = 1.0 confidence)

**Relationships**:

- Many-to-one with `SocialMediaPost`
- Many-to-one with `DisasterProject`
- Many-to-one with `DamageCategory`

---

### AnalysisConfiguration

Represents user-defined analysis parameters.

**Attributes**:

| Field                         | Type                | Constraints                                  | Description                                 |
| ----------------------------- | ------------------- | -------------------------------------------- | ------------------------------------------- |
| `id`                          | UUID                | Primary key, not null                        | Configuration identifier                    |
| `projectId`                   | UUID                | Foreign key, not null, unique                | Associated project (one config per project) |
| `sentimentBackend`            | String              | Enum: "java_lexicon", "python_api"           | Sentiment analyzer to use                   |
| `sentimentThreshold`          | double              | 0.0 to 1.0, default 0.5                      | Minimum confidence to classify              |
| `pythonApiUrl`                | String              | Valid URL if sentimentBackend = "python_api" | Python API endpoint                         |
| `damageClassificationEnabled` | boolean             | Default true                                 | Whether to run damage categorization        |
| `minimumKeywordMatches`       | int                 | 1-10, default 1                              | Minimum matches to assign category          |
| `customSettings`              | Map<String, Object> | JSON                                         | Future extensibility                        |

**Validation Rules**:

- If `sentimentBackend = "python_api"`, `pythonApiUrl` must be non-null and
  reachable
- `sentimentThreshold` typically 0.5-0.7 for balanced precision/recall

**Relationships**:

- One-to-one with `DisasterProject`

---

### ExportReport

Represents generated analysis report.

**Attributes**:

| Field           | Type                | Constraints           | Description                                        |
| --------------- | ------------------- | --------------------- | -------------------------------------------------- |
| `id`            | UUID                | Primary key, not null | Report identifier                                  |
| `projectId`     | UUID                | Foreign key, not null | Associated project                                 |
| `reportType`    | ReportType          | Not null, enum        | Type of report                                     |
| `format`        | ReportFormat        | Not null, enum        | Output format                                      |
| `generatedAt`   | Instant             | Not null, auto-set    | Generation timestamp                               |
| `filePath`      | String              | Not null              | Path to generated file                             |
| `parameters`    | Map<String, Object> | JSON                  | Filters/options used (e.g., date range, platforms) |
| `fileSizeBytes` | long                | >= 0                  | File size for storage management                   |

**Enum: ReportType**

- `SENTIMENT_ANALYSIS`: Sentiment timeline and statistics
- `DAMAGE_CATEGORIZATION`: Damage distribution and breakdown
- `COMBINED`: Both sentiment and damage analysis

**Enum: ReportFormat**

- `CSV`: Comma-separated values
- `PDF`: PDF document with charts
- `JSON`: Structured JSON export

**Validation Rules**:

- File path must exist and be readable
- Generated reports stored in `data/{project_id}/reports/` directory

**Relationships**:

- Many-to-one with `DisasterProject`

---

## Entity Relationship Diagram

```text
┌─────────────────┐
│ DisasterProject │
│ PK: id          │
└────────┬────────┘
         │
         ├──── 1:N ────┐
         │             │
         │      ┌──────▼──────────┐
         │      │ SocialMediaPost │
         │      │ PK: id          │
         │      │ FK: projectId   │
         │      └──────┬──────────┘
         │             │
         │             ├──── 1:1 ────┐
         │             │              │
         │             │       ┌──────▼────────┐
         │             │       │SentimentResult│
         │             │       │ PK: id        │
         │             │       │ FK: postId    │
         │             │       └───────────────┘
         │             │
         │             └──── 1:N ────┐
         │                            │
         │                     ┌──────▼────────────────┐
         │                     │DamageClassification   │
         │                     │ PK: id                │
         │                     │ FK: postId            │
         │                     │ FK: categoryId        │
         │                     └──────┬────────────────┘
         │                            │
         │                            │ N:1
         │                            │
         │                     ┌──────▼──────────┐
         │                     │ DamageCategory  │
         │                     │ PK: id          │
         │                     └─────────────────┘
         │
         ├──── 1:1 ────┐
         │             │
         │      ┌──────▼──────────────────┐
         │      │ AnalysisConfiguration   │
         │      │ PK: id                  │
         │      │ FK: projectId (unique)  │
         │      └─────────────────────────┘
         │
         └──── 1:N ────┐
                       │
                ┌──────▼──────────┐
                │  ExportReport   │
                │  PK: id         │
                │  FK: projectId  │
                └─────────────────┘

┌───────────────┐
│  DataSource   │
│  PK: id       │
└───────────────┘
  (Referenced by DisasterProject.dataSources)
```

---

## Value Objects

### DateRange

Encapsulates start and end date with validation.

```java
public record DateRange(LocalDate start, LocalDate end) {
    public DateRange {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw new IllegalArgumentException("Date range cannot exceed 365 days");
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
```

### Keyword

Encapsulates a search keyword with validation.

```java
public record Keyword(String value) {
    public Keyword {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be empty");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("Keyword cannot exceed 100 characters");
        }
    }

    public String normalized() {
        return value.trim().toLowerCase();
    }
}
```

---

## Aggregates and Bounded Contexts

### Project Aggregate

**Root**: `DisasterProject`

**Components**:

- `DisasterProject` (root)
- `AnalysisConfiguration`
- `ExportReport`

**Invariants**:

- Project must have at least one keyword
- Project must have at least one data source
- Status transitions must follow state machine
- Configuration exists for every project (created on project creation)

**Operations**:

- `createProject(name, disasterName, region, dateRange, keywords, dataSources)`
- `startCollection()` → status = COLLECTING
- `pauseCollection()` → status = PAUSED
- `completeCollection()` → status = COMPLETED
- `generateReport(type, format, parameters)` → creates `ExportReport`

### Post Aggregate

**Root**: `SocialMediaPost`

**Components**:

- `SocialMediaPost` (root)
- `SentimentResult` (optional, created after analysis)
- `DamageClassification` (multiple, created after analysis)

**Invariants**:

- Post content must not be empty
- Sentiment result (if exists) must have valid scores summing to 1.0
- Damage classifications (if exist) reference valid categories

**Operations**:

- `createPost(platform, externalId, content, author, publishedAt, ...)`
- `analyzeSentiment(analyzer)` → creates `SentimentResult`
- `categorizeDamage(classifier)` → creates `DamageClassification` records

### Configuration Aggregate

**Root**: `DamageCategory`

**Components**:

- `DamageCategory` (predefined set, user-extendable)

**Invariants**:

- Categories must have unique IDs
- Categories must have at least one keyword (Vietnamese or English)

**Operations**:

- `addCategory(id, name, keywordsVi, keywordsEn, ...)`
- `updateKeywords(categoryId, newKeywords)` → increments version
- `removeCategory(categoryId)` → soft delete (mark disabled)

---

## Persistence Strategy

### SQLite Schema

```sql
-- Projects table
CREATE TABLE projects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    disaster_name TEXT NOT NULL,
    region TEXT NOT NULL,
    start_date TEXT NOT NULL,  -- ISO 8601 date
    end_date TEXT NOT NULL,
    keywords TEXT NOT NULL,    -- JSON array
    data_sources TEXT NOT NULL, -- JSON array
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,  -- ISO 8601 timestamp
    updated_at TEXT NOT NULL,
    total_posts_collected INTEGER DEFAULT 0
);

-- Posts metadata (lightweight references, full data in JSON files)
CREATE TABLE posts_metadata (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    external_id TEXT NOT NULL,
    published_at TEXT NOT NULL,
    collected_at TEXT NOT NULL,
    language TEXT,
    file_path TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_posts_project ON posts_metadata(project_id);
CREATE INDEX idx_posts_published ON posts_metadata(published_at);

-- Sentiment results
CREATE TABLE sentiment_results (
    id TEXT PRIMARY KEY,
    post_id TEXT NOT NULL UNIQUE,
    project_id TEXT NOT NULL,
    sentiment_label TEXT NOT NULL,
    confidence REAL NOT NULL,
    positive_score REAL NOT NULL,
    negative_score REAL NOT NULL,
    neutral_score REAL NOT NULL,
    analyzed_at TEXT NOT NULL,
    analyzer_type TEXT NOT NULL,
    explanation TEXT,
    FOREIGN KEY (post_id) REFERENCES posts_metadata(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_sentiment_project ON sentiment_results(project_id);
CREATE INDEX idx_sentiment_label ON sentiment_results(sentiment_label);

-- Damage categories
CREATE TABLE damage_categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    keywords_vietnamese TEXT NOT NULL, -- JSON array
    keywords_english TEXT NOT NULL,    -- JSON array
    description TEXT,
    priority INTEGER NOT NULL,
    color TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1
);

-- Damage classifications
CREATE TABLE damage_classifications (
    id TEXT PRIMARY KEY,
    post_id TEXT NOT NULL,
    project_id TEXT NOT NULL,
    category_id TEXT NOT NULL,
    confidence REAL NOT NULL,
    matched_keywords TEXT NOT NULL,  -- JSON array
    analyzed_at TEXT NOT NULL,
    category_version INTEGER NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts_metadata(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES damage_categories(id)
);

CREATE INDEX idx_damage_project ON damage_classifications(project_id);
CREATE INDEX idx_damage_category ON damage_classifications(category_id);

-- Analysis configurations
CREATE TABLE analysis_configurations (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL UNIQUE,
    sentiment_backend TEXT NOT NULL,
    sentiment_threshold REAL NOT NULL,
    python_api_url TEXT,
    damage_classification_enabled INTEGER NOT NULL,
    minimum_keyword_matches INTEGER NOT NULL,
    custom_settings TEXT,  -- JSON
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Data sources
CREATE TABLE data_sources (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    base_url TEXT NOT NULL,
    auth_type TEXT NOT NULL,
    rate_limit INTEGER NOT NULL,
    rate_limit_window TEXT NOT NULL,
    enabled INTEGER NOT NULL,
    config_params TEXT  -- JSON
);

-- Export reports
CREATE TABLE export_reports (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    report_type TEXT NOT NULL,
    format TEXT NOT NULL,
    generated_at TEXT NOT NULL,
    file_path TEXT NOT NULL,
    parameters TEXT NOT NULL,  -- JSON
    file_size_bytes INTEGER NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_reports_project ON export_reports(project_id);
```

### JSON File Structure

Posts stored in `data/{project_id}/{platform}/{yyyy-MM-dd}/posts_{batch}.json`:

```json
{
  "batch_id": "twitter_2024-09-15_001",
  "posts": [
    {
      "id": "twitter_1234567890",
      "platform": "twitter",
      "external_id": "1234567890",
      "content": "Typhoon Yagi caused severe flooding...",
      "author": "anonymized_user_001",
      "published_at": "2024-09-15T10:30:00Z",
      "language": "en",
      "location_tags": ["Hanoi", "Vietnam"],
      "engagement_metrics": {
        "likes": 45,
        "shares": 12,
        "comments": 8,
        "views": 1203
      },
      "raw_metadata": {
        "hashtags": ["TyphoonYagi", "VietnamFlood"],
        "mentions": ["@disaster_relief"]
      }
    }
  ]
}
```

---

## Summary

This data model provides:

- **Clear separation of concerns**: Project management, data collection,
  analysis, configuration
- **Flexibility**: Multi-label damage classification, extensible categories,
  pluggable analyzers
- **Validation**: Enforced constraints at entity level (invariants)
- **Traceability**: All analysis results link back to source posts and projects
- **Versioning**: Category versions allow re-classification when keywords
  updated
- **Performance**: Hybrid storage (SQLite for queries, JSON for raw data)
  optimized for 100K+ posts

The model adheres to Constitution principles: encapsulation (entities hide
state), clear relationships (aggregates), determinism (explicit timezone
handling), and extensibility (configuration-driven categories).
