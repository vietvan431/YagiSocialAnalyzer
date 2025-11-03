# Tasks: Disaster Social Media Analysis System

**Input**: Design documents from `/specs/001-disaster-analysis/`  
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/, research.md,
quickstart.md

**Tests**: Not explicitly requested in specification - test tasks omitted. Focus
on implementation tasks organized by user story for independent delivery.

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md project structure:

- Java source: `src/main/java/com/yagi/socialanalyzer/`
- Resources: `src/main/resources/`
- Tests: `src/test/java/com/yagi/socialanalyzer/`
- Configuration: `config/`
- Data storage: `data/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic Maven structure

- [x] T001 Create Maven project structure with groupId `com.yagi.socialanalyzer`
      and artifactId `yagi-social-analyzer`
- [x] T002 Configure `pom.xml` with Java 25, JavaFX 21, Selenium 4.x, Jackson
      2.15, Apache HttpClient 5.x, SQLite JDBC, SLF4J, Logback, JUnit 5, TestFX
      dependencies
- [x] T003 [P] Create directory structure:
      `src/main/java/com/yagi/socialanalyzer/{domain,application,infrastructure,ui}`
      with subdirectories per plan.md
- [x] T004 [P] Create `src/main/resources/` with subdirectories: `fxml/`,
      `css/`, `config/`, `i18n/`
- [x] T005 [P] Setup `.gitignore` for Java/Maven (target/, .idea/, \*.iml,
      data/, logs/)
- [x] T006 [P] Configure Checkstyle, SpotBugs, and PMD plugins in `pom.xml` for
      code quality gates
- [x] T007 [P] Create `config/platforms.yaml` skeleton with platform definitions
      structure
- [x] T008 [P] Create `config/damage_categories.yaml` with 6 predefined
      categories from spec.md
- [x] T009 Create `src/main/resources/i18n/messages_en.properties` and
      `messages_vi.properties` for internationalization
- [x] T010 [P] Setup logging configuration in `src/main/resources/logback.xml`
      with console and file appenders

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can
be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Domain Foundation

- [ ] T011 [P] Create domain exceptions in
      `src/main/java/com/yagi/socialanalyzer/domain/exceptions/`:
      `DomainException`, `RepositoryException`, `AnalysisException`,
      `CollectionException`, `AuthenticationException`,
      `ConfigurationException`, `ExportException`, `SecurityException`
- [ ] T012 [P] Create value objects in
      `src/main/java/com/yagi/socialanalyzer/domain/valueobjects/`:
      `ProjectStatus` enum, `SentimentLabel` enum, `ReportType` enum,
      `ReportFormat` enum, `DataSourceType` enum, `AuthenticationType` enum
- [ ] T013 [P] Create `DateRange` value object in
      `src/main/java/com/yagi/socialanalyzer/domain/valueobjects/DateRange.java`
      with validation (start <= end, max 365 days)
- [ ] T014 [P] Create `Keyword` value object in
      `src/main/java/com/yagi/socialanalyzer/domain/valueobjects/Keyword.java`
      with validation (non-empty, max 100 chars)
- [x] T015 [P] Create `EngagementMetrics` embedded object in
      `src/main/java/com/yagi/socialanalyzer/domain/valueobjects/EngagementMetrics.java`

### Core Entities (Shared Across Multiple Stories)

- [x] T016 Create `DisasterProject` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/DisasterProject.java`
      with all attributes from data-model.md, validation rules, and state
      machine transitions
- [x] T017 Create `DataSource` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/DataSource.java`
      for platform configurations
- [x] T018 Create `DamageCategory` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/DamageCategory.java`
      with Vietnamese/English keyword lists
- [x] T019 Create `AnalysisConfiguration` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/AnalysisConfiguration.java`
      with sentiment backend selection

### Infrastructure Foundation

- [x] T020 Setup SQLite database schema in `src/main/resources/db/schema.sql`
      with all tables from data-model.md (projects, posts_metadata,
      sentiment_results, damage_categories, damage_classifications,
      analysis_configurations, data_sources, export_reports)
- [x] T021 Create `DatabaseManager` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/DatabaseManager.java`
      for connection pooling and schema initialization
- [x] T022 [P] Implement `YamlConfigurationProvider` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/config/YamlConfigurationProvider.java`
      implementing `IConfigurationProvider` to load platforms.yaml and
      damage_categories.yaml
- [x] T023 [P] Implement `EncryptedCredentialStore` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/security/EncryptedCredentialStore.java`
      implementing `ICredentialStore` with AES-256 encryption and OS keyring
      integration (Windows Credential Manager, macOS Keychain, Linux Secret
      Service)
- [x] T024 Create `JsonFileManager` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/JsonFileManager.java`
      for reading/writing post JSON files with batch operations

### Application Foundation

- [x] T025 Create application DTOs in
      `src/main/java/com/yagi/socialanalyzer/application/dto/`:
      `CollectionStatus`, `AnalysisSummary`, `SentimentSummary`, `DamageSummary`
- [x] T026 Setup JavaFX application entry point in
      `src/main/java/com/yagi/socialanalyzer/Main.java` with primary stage
      initialization
- [x] T027 Create main window FXML in `src/main/resources/fxml/MainWindow.fxml`
      with navigation menu, content area, and status bar
- [x] T028 Create main window controller in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/MainController.java`
      with scene navigation
- [x] T029 [P] Create base CSS stylesheet in
      `src/main/resources/css/application.css` with common styles

**Checkpoint**: Foundation ready - user story implementation can now begin in
parallel

---

## Phase 3: User Story 1 - Configure and Collect Social Media Data (Priority: P1) 🎯 MVP

**Goal**: Enable disaster researchers to configure disaster projects and collect
social media posts from multiple platforms with progress tracking and error
handling.

**Independent Test**: Create a project "Typhoon Yagi" with date range Sep 1-30
2024, keywords "typhoon yagi, bão yagi", select Twitter/Reddit platforms, run
collection, verify posts stored in database and JSON files with proper metadata.

### Domain Layer for US1

- [x] T030 [P] [US1] Create `SocialMediaPost` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/SocialMediaPost.java`
      with all attributes from data-model.md (composite ID, content, author,
      timestamps, engagement metrics, file path)
- [x] T031 [P] [US1] Create `IProjectRepository` interface in
      `src/main/java/com/yagi/socialanalyzer/domain/repositories/IProjectRepository.java`
      with methods: save, findById, findByStatus, findAll, delete, existsByName
- [x] T032 [P] [US1] Create `IPostRepository` interface in
      `src/main/java/com/yagi/socialanalyzer/domain/repositories/IPostRepository.java`
      with methods: save, saveBatch, findById, findByProjectAndDateRange,
      findByProjectAndPlatform, countByProject, deleteByProject

### Infrastructure Layer for US1

- [x] T033 [US1] Implement `SQLiteProjectRepository` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/SQLiteProjectRepository.java`
      implementing `IProjectRepository` with JDBC operations for projects table
- [x] T034 [US1] Implement `SQLitePostRepository` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/SQLitePostRepository.java`
      implementing `IPostRepository` with JDBC for posts_metadata table and JSON
      file storage integration
- [x] T035 [P] [US1] Create `IPlatformDataSource` interface in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/datasources/IPlatformDataSource.java`
      with methods: getPlatformId, authenticate, searchPosts, isAvailable,
      getRemainingRateLimit
- [x] T036 [P] [US1] Implement `TwitterDataSource` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/datasources/TwitterDataSource.java`
      implementing `IPlatformDataSource` using Twitter API v2 with Apache
      HttpClient
- [x] T037 [P] [US1] Implement `RedditDataSource` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/datasources/RedditDataSource.java`
      implementing `IPlatformDataSource` using Reddit API
- [x] T038 [P] [US1] Implement `YouTubeDataSource` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/datasources/YouTubeDataSource.java`
      implementing `IPlatformDataSource` using YouTube Data API v3
- [x] T039 [P] [US1] Implement `FacebookSeleniumDataSource` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/datasources/FacebookSeleniumDataSource.java`
      implementing `IPlatformDataSource` using Selenium WebDriver with anti-bot
      detection handling
- [x] T040 [P] [US1] Implement `TikTokSeleniumDataSource` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/datasources/TikTokSeleniumDataSource.java`
      implementing `IPlatformDataSource` using Selenium WebDriver
- [x] T041 [P] [US1] Implement `GoogleSeleniumDataSource` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/datasources/GoogleSeleniumDataSource.java`
      implementing `IPlatformDataSource` for Google Search results via Selenium

### Real Platform Implementation for US1

- [ ] T041a [P] [US1] Implement real Twitter API v2 integration in
      `TwitterDataSource`: authenticate using OAuth 2.0 Bearer Token, implement
      `/tweets/search/recent` endpoint calls, parse response JSON to extract
      tweet data (id, text, created_at, author_id, public_metrics), handle
      pagination with next_token, implement rate limit headers parsing
      (x-rate-limit-remaining), add retry logic for 429 Too Many Requests with
      exponential backoff
- [x] T041b [P] [US1] Implement real Reddit API integration in
      `RedditDataSource`: authenticate using OAuth 2.0 client credentials flow,
      implement `/r/all/search` endpoint, parse JSON responses to extract
      submission data (id, title, selftext, author, created_utc, score,
      num_comments), handle pagination with after/before parameters, implement
      rate limit checking (X-Ratelimit-Remaining header), add support for both
      submissions and comments collection
- [ ] T041c [P] [US1] Implement real YouTube Data API v3 integration in
      `YouTubeDataSource`: authenticate using API key, implement `/search`
      endpoint for video search, implement `/commentThreads` endpoint for
      comment collection, parse JSON responses to extract video metadata (id,
      title, description, publishedAt, statistics), handle quota limits (10,000
      units/day), implement continuation tokens for pagination, add comment
      replies expansion if requested
- [ ] T041d [P] [US1] Implement real Facebook Graph API integration in
      `FacebookSeleniumDataSource`: replace Selenium with Graph API calls using
      OAuth 2.0 app access token, implement `/search` endpoint with type=post
      parameter, parse JSON responses to extract post data (id, message,
      created_time, from, reactions, shares, comments), handle API versioning
      (v18.0+), implement cursor-based pagination, add error handling for
      restricted posts and privacy settings, fallback to Selenium for pages
      requiring login
- [ ] T041e [P] [US1] Implement real TikTok scraping in
      `TikTokSeleniumDataSource`: enhance Selenium implementation with TikTok
      search page automation, implement scroll-to-load mechanism for infinite
      scroll, extract video metadata from DOM elements (video ID, description,
      author, stats), parse timestamp from data attributes, implement
      anti-detection measures (random delays, user agent rotation, browser
      fingerprint randomization), add CAPTCHA detection and manual intervention
      prompt, handle rate limiting with exponential backoff and session rotation
- [ ] T041f [P] [US1] Implement real Google Search scraping in
      `GoogleSeleniumDataSource`: enhance Selenium implementation with Google
      Custom Search JSON API as primary method, authenticate using API key,
      implement `/customsearch/v1` endpoint, parse search results JSON
      extracting (title, link, snippet, displayLink, pagemap), handle quota
      limits (100 queries/day free tier), fallback to Selenium scraping when
      quota exceeded, implement CAPTCHA detection and retry logic for scraping,
      add respect for robots.txt and crawl delays

### Application Layer for US1

- [x] T042 [US1] Create `IDataCollectionService` interface in
      `src/main/java/com/yagi/socialanalyzer/application/services/IDataCollectionService.java`
      with methods: startCollection, pauseCollection, resumeCollection,
      getStatus
- [x] T043 [US1] Implement `DataCollectionService` in
      `src/main/java/com/yagi/socialanalyzer/application/services/DataCollectionService.java`
      implementing `IDataCollectionService` with platform orchestration, rate
      limit handling, and progress callbacks
- [x] T044 [US1] Implement rate limit handler with exponential backoff in
      `DataCollectionService` for graceful retry on API errors
- [x] T045 [US1] Add batch post saving optimization in `DataCollectionService`
      to save 100 posts at a time for performance

### UI Layer for US1

- [x] T046 [US1] Create project configuration FXML in
      `src/main/resources/fxml/ProjectConfigDialog.fxml` with fields for name,
      disaster name, region, date range (DatePicker), keywords (TextArea), data
      sources (CheckBox list)
- [x] T047 [US1] Create `ProjectConfigController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/ProjectConfigController.java`
      with form validation (date range <= 365 days, keywords not empty, at least
      one data source selected)
- [x] T048 [US1] Create project list view FXML in
      `src/main/resources/fxml/ProjectListView.fxml` with TableView showing
      project name, disaster name, status, date range, total posts collected
- [x] T049 [US1] Create `ProjectListController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/ProjectListController.java`
      with actions: create project, edit project, delete project, view details
- [x] T050 [US1] Create data collection view FXML in
      `src/main/resources/fxml/DataCollectionView.fxml` with platform progress
      indicators (ProgressBar per platform), current status label,
      pause/resume/stop buttons
- [x] T051 [US1] Create `DataCollectionController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/DataCollectionController.java`
      extending JavaFX Task for background collection with progress updates
- [x] T052 [US1] Implement `DataCollectionTask` extending `Task<Integer>` in
      `src/main/java/com/yagi/socialanalyzer/ui/tasks/DataCollectionTask.java`
      wrapping `DataCollectionService.startCollection` with
      updateProgress/updateMessage calls
- [x] T053 [US1] Create raw data view FXML in
      `src/main/resources/fxml/RawDataView.fxml` with TableView showing post ID,
      platform, timestamp, content preview, engagement metrics, with
      search/filter TextFields
- [x] T054 [US1] Create `RawDataController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/RawDataController.java`
      loading posts from repository with pagination (100 posts per page)
- [x] T055 [US1] Create platform credentials dialog FXML in
      `src/main/resources/fxml/CredentialsDialog.fxml` with platform dropdown,
      credential fields (API key, secret, tokens), save/test buttons
- [x] T056 [US1] Create `CredentialsController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/CredentialsController.java`
      integrating with `EncryptedCredentialStore` to save/retrieve credentials
      securely

### Integration for US1

- [x] T057 [US1] Wire up main window navigation to project list view as default
      screen
- [x] T058 [US1] Add "New Project" button handler in `ProjectListController`
      opening `ProjectConfigDialog`
- [x] T059 [US1] Add "Start Collection" button handler in
      `ProjectListController` opening `DataCollectionView` with selected project
- [x] T060 [US1] Add error handling in `DataCollectionController` to display
      alert dialogs on API errors, rate limits, authentication failures
- [x] T061 [US1] Update project status (DRAFT → COLLECTING → COMPLETED) in
      database during collection lifecycle
- [x] T062 [US1] Update `totalPostsCollected` counter in DisasterProject entity
      after each batch save

**Checkpoint**: At this point, User Story 1 should be fully functional - users
can create projects, configure platforms, collect data, view raw posts, and
manage credentials independently.

---

## Phase 4: User Story 2 - Analyze Sentiment Trends Over Time (Priority: P2)

**Goal**: Enable researchers to view sentiment analysis results
(positive/negative/neutral) with time-series visualization showing how public
emotion changed throughout the disaster period.

**Independent Test**: Using collected posts from US1, run sentiment analysis,
verify all posts classified with sentiment labels and confidence scores, view
timeline chart showing daily sentiment distribution with drill-down to sample
posts.

### Domain Layer for US2

- [ ] T063 [P] [US2] Create `SentimentResult` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/SentimentResult.java`
      with attributes: id, postId, projectId, sentimentLabel, confidence,
      positiveScore, negativeScore, neutralScore, analyzedAt, analyzerType,
      explanation
- [ ] T064 [P] [US2] Create `ISentimentRepository` interface in
      `src/main/java/com/yagi/socialanalyzer/domain/repositories/ISentimentRepository.java`
      with methods: save, findByPostId, findByProjectId, countByProjectAndLabel,
      findByProjectAndDateRange
- [ ] T065 [P] [US2] Create `ISentimentAnalyzer` domain service interface in
      `src/main/java/com/yagi/socialanalyzer/domain/services/ISentimentAnalyzer.java`
      with methods: analyze, analyzeBatch, getAnalyzerType, supportsLanguage

### Infrastructure Layer for US2

- [ ] T066 [US2] Implement `SQLiteSentimentRepository` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/SQLiteSentimentRepository.java`
      implementing `ISentimentRepository` with JDBC operations for
      sentiment_results table
- [ ] T067 [P] [US2] Implement `JavaLexiconSentimentAnalyzer` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/analysis/JavaLexiconSentimentAnalyzer.java`
      implementing `ISentimentAnalyzer` with keyword-based sentiment detection
      for Vietnamese and English (research.md: 70-75% accuracy, 10K posts/sec)
- [ ] T068 [P] [US2] Create sentiment lexicon files:
      `src/main/resources/lexicons/sentiment_vi.txt` (Vietnamese
      positive/negative keywords) and `sentiment_en.txt` (English
      positive/negative keywords)
- [ ] T069 [US2] Implement lexicon loading and TF-IDF scoring in
      `JavaLexiconSentimentAnalyzer` with confidence calculation based on
      keyword match strength
- [ ] T070 [P] [US2] Implement `PythonApiSentimentAnalyzer` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/analysis/PythonApiSentimentAnalyzer.java`
      implementing `ISentimentAnalyzer` calling Python API endpoints from
      contracts/python-api.md (optional, for 85-90% accuracy)
- [ ] T071 [US2] Implement health check and fallback logic in
      `PythonApiSentimentAnalyzer`: if Python API unavailable, throw clear
      exception suggesting Java lexicon backend

### Application Layer for US2

- [ ] T072 [US2] Create `IAnalysisService` interface in
      `src/main/java/com/yagi/socialanalyzer/application/services/IAnalysisService.java`
      with methods: runAnalysis, runSentimentAnalysis, runDamageAnalysis
- [ ] T073 [US2] Implement `AnalysisService` in
      `src/main/java/com/yagi/socialanalyzer/application/services/AnalysisService.java`
      implementing `IAnalysisService` with sentiment analysis orchestration,
      batch processing (100 posts per batch), and progress callbacks
- [ ] T074 [US2] Add analyzer selection logic in `AnalysisService` reading
      `AnalysisConfiguration.sentimentBackend` to choose between Java lexicon
      and Python API
- [ ] T075 [US2] Implement sentiment statistics calculation in `AnalysisService`
      returning `SentimentSummary` DTO with positive/negative/neutral counts and
      average confidence

### UI Layer for US2

- [ ] T076 [US2] Create sentiment analysis view FXML in
      `src/main/resources/fxml/SentimentAnalysisView.fxml` with overall
      distribution PieChart, timeline LineChart, confidence histogram, sample
      posts TableView
- [ ] T077 [US2] Create `SentimentAnalysisController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/SentimentAnalysisController.java`
      loading sentiment results from repository and populating charts
- [ ] T078 [US2] Implement timeline chart in `SentimentAnalysisController`
      grouping sentiment results by date (daily buckets) with three series:
      positive, negative, neutral counts
- [ ] T079 [US2] Implement drill-down feature: when user clicks date point on
      timeline, load and display sample posts from that date in TableView with
      sentiment labels and confidence scores
- [ ] T080 [US2] Create `SentimentAnalysisTask` extending
      `Task<AnalysisSummary>` in
      `src/main/java/com/yagi/socialanalyzer/ui/tasks/SentimentAnalysisTask.java`
      wrapping `AnalysisService.runSentimentAnalysis` with progress updates
- [ ] T081 [US2] Create analysis configuration dialog FXML in
      `src/main/resources/fxml/AnalysisConfigDialog.fxml` with sentiment backend
      selection (ComboBox: Java Lexicon / Python API), Python API URL field,
      sentiment threshold slider
- [ ] T082 [US2] Create `AnalysisConfigController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/AnalysisConfigController.java`
      with "Test Connection" button for Python API validation

### Integration for US2

- [ ] T083 [US2] Add "Analyze Sentiment" button in project list view opening
      sentiment analysis view
- [ ] T084 [US2] Update project status to ANALYZING during sentiment analysis,
      back to COMPLETED when done
- [ ] T085 [US2] Add error handling for analysis failures (posts without
      language detected, Python API unreachable, lexicon files missing)
- [ ] T086 [US2] Implement re-analysis capability: if analysis already exists,
      ask user "Re-run analysis? Existing results will be overwritten"

**Checkpoint**: At this point, User Stories 1 AND 2 should both work
independently - sentiment analysis can be run on collected data with timeline
visualization.

---

## Phase 5: User Story 3 - Categorize and Analyze Damage Types (Priority: P3)

**Goal**: Enable disaster coordinators to see which damage types (affected
people, economic disruption, damaged buildings, lost property, damaged
infrastructure, other) are most frequently mentioned with category distribution
visualization.

**Independent Test**: Using collected posts from US1, run damage categorization,
verify posts classified into damage categories (supporting multi-label), view
distribution chart showing category counts, drill down to posts in specific
category.

### Domain Layer for US3

- [ ] T087 [P] [US3] Create `DamageClassification` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/DamageClassification.java`
      with attributes: id, postId, projectId, categoryId, confidence,
      matchedKeywords, analyzedAt, categoryVersion
- [ ] T088 [P] [US3] Create `IDamageClassificationRepository` interface in
      `src/main/java/com/yagi/socialanalyzer/domain/repositories/IDamageClassificationRepository.java`
      with methods: save, saveBatch, findByPostId, findByProjectId,
      countByProjectAndCategory, deleteByProject
- [ ] T089 [P] [US3] Create `IDamageCategoryRepository` interface in
      `src/main/java/com/yagi/socialanalyzer/domain/repositories/IDamageCategoryRepository.java`
      with methods: save, findAll, findById, delete
- [ ] T090 [P] [US3] Create `IDamageClassifier` domain service interface in
      `src/main/java/com/yagi/socialanalyzer/domain/services/IDamageClassifier.java`
      with methods: classify (single post), classifyBatch

### Infrastructure Layer for US3

- [ ] T091 [US3] Implement `SQLiteDamageClassificationRepository` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/SQLiteDamageClassificationRepository.java`
      implementing `IDamageClassificationRepository` with JDBC for
      damage_classifications table
- [ ] T092 [US3] Implement `SQLiteDamageCategoryRepository` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/SQLiteDamageCategoryRepository.java`
      implementing `IDamageCategoryRepository` with JDBC for damage_categories
      table, loading from YAML on first run
- [ ] T093 [US3] Implement `KeywordBasedDamageClassifier` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/analysis/KeywordBasedDamageClassifier.java`
      implementing `IDamageClassifier` with multi-label keyword matching, TF-IDF
      weighting per research.md
- [ ] T094 [US3] Implement keyword matching algorithm in
      `KeywordBasedDamageClassifier`: tokenize post content, match against
      category keywords (case-insensitive, Vietnamese/English), calculate
      confidence as min(1.0, matchCount / 3.0)
- [ ] T095 [US3] Add multi-label support in `KeywordBasedDamageClassifier`:
      single post can match multiple categories if minimum keyword threshold met
      (configurable via `AnalysisConfiguration.minimumKeywordMatches`)

### Application Layer for US3

- [ ] T096 [US3] Extend `AnalysisService.runDamageAnalysis` method to
      orchestrate damage classification with batch processing (100 posts per
      batch), progress callbacks, and return `DamageSummary` DTO
- [ ] T097 [US3] Implement damage statistics calculation in `AnalysisService`
      returning category distribution map (categoryId → count), total posts
      classified, posts with multiple categories count
- [ ] T098 [US3] Add re-classification logic in `AnalysisService`: when category
      definitions change (keywords updated), increment categoryVersion and
      re-run classification

### UI Layer for US3

- [ ] T099 [US3] Create damage analysis view FXML in
      `src/main/resources/fxml/DamageAnalysisView.fxml` with category
      distribution BarChart, category breakdown TableView, multi-label
      indicator, sample posts for selected category
- [ ] T100 [US3] Create `DamageAnalysisController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/DamageAnalysisController.java`
      loading damage classifications and populating charts
- [ ] T101 [US3] Implement category selection in `DamageAnalysisController`:
      when user clicks bar in chart or row in table, load posts for that
      category with highlighted matched keywords
- [ ] T102 [US3] Create `DamageAnalysisTask` extending `Task<DamageSummary>` in
      `src/main/java/com/yagi/socialanalyzer/ui/tasks/DamageAnalysisTask.java`
      wrapping `AnalysisService.runDamageAnalysis`
- [ ] T103 [US3] Create damage category editor FXML in
      `src/main/resources/fxml/DamageCategoryEditor.fxml` with category list,
      add/edit/delete buttons, keyword TextArea (Vietnamese and English tabs),
      color picker, priority spinner
- [ ] T104 [US3] Create `DamageCategoryEditorController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/DamageCategoryEditorController.java`
      with CRUD operations on `IDamageCategoryRepository`, saving changes to
      damage_categories.yaml

### Integration for US3

- [ ] T105 [US3] Add "Analyze Damage" button in project list view opening damage
      analysis view
- [ ] T106 [US3] Add "Manage Categories" menu item in settings menu opening
      damage category editor
- [ ] T107 [US3] Implement combined analysis: "Analyze All" button runs both
      sentiment and damage analysis sequentially with combined progress bar
- [ ] T108 [US3] Add validation: if no damage categories defined, show warning
      "Please define at least one damage category in Settings"

**Checkpoint**: All core analysis user stories (US1, US2, US3) should now be
independently functional.

---

## Phase 6: User Story 4 - Manage Data Sources and Keywords Flexibly (Priority: P4)

**Goal**: Enable administrators to add new social media platforms, modify search
keywords, and update damage categories without code changes through
configuration interfaces.

**Independent Test**: Add new platform "Instagram" via configuration UI, define
API endpoint pattern and auth method, verify it appears in collection options;
modify keyword list for existing project and verify changes applied in next
collection; create new damage category "Health impacts" and verify it's
available in analysis.

### UI Layer for US4

- [ ] T109 [US4] Create data source manager FXML in
      `src/main/resources/fxml/DataSourceManager.fxml` with platform list
      TableView, add/edit/disable buttons, platform details form (name, type,
      base URL, auth type, rate limit, config params JSON editor)
- [ ] T110 [US4] Create `DataSourceManagerController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/DataSourceManagerController.java`
      with CRUD operations loading/saving to platforms.yaml via
      `YamlConfigurationProvider`
- [ ] T111 [US4] Add platform validation in `DataSourceManagerController`: base
      URL must be valid HTTPS, rate limit > 0, auth type matches available
      credential fields
- [ ] T112 [US4] Implement hot reload in `DataSourceManagerController`: after
      saving platform changes, call `YamlConfigurationProvider.reload()` to
      update in-memory platform list without restart

### Application Layer for US4

- [ ] T113 [US4] Extend `YamlConfigurationProvider` with `savePlatforms` and
      `saveDamageCategories` methods to write changes back to YAML files
- [ ] T114 [US4] Add validation in `YamlConfigurationProvider.savePlatforms`:
      prevent duplicate platform IDs, validate required fields (id, name, type,
      baseUrl, authType, rateLimit)
- [ ] T115 [US4] Add validation in
      `YamlConfigurationProvider.saveDamageCategories`: prevent duplicate
      category IDs, require at least one keyword list (Vietnamese or English)

### Integration for US4

- [ ] T116 [US4] Add "Manage Data Sources" menu item in settings menu opening
      data source manager
- [ ] T117 [US4] Update project configuration dialog to dynamically load
      platform list from `YamlConfigurationProvider` instead of hardcoded list
- [ ] T118 [US4] Implement keyword editing in project configuration: allow users
      to add/remove keywords after project creation, save to database
- [ ] T119 [US4] Add "Re-analyze" button in sentiment/damage views: when
      categories or thresholds change, allow re-running analysis without
      re-collecting data

**Checkpoint**: All user stories complete - system is fully functional with
flexible configuration.

---

## Phase 7: Export and Reporting

**Purpose**: Generate reports for analysis results (addresses spec requirement
FR-030)

### Domain Layer for Export

- [ ] T120 [P] Create `ExportReport` entity in
      `src/main/java/com/yagi/socialanalyzer/domain/entities/ExportReport.java`
      with attributes: id, projectId, reportType, format, generatedAt, filePath,
      parameters, fileSizeBytes
- [ ] T121 [P] Create `IExportReportRepository` interface in
      `src/main/java/com/yagi/socialanalyzer/domain/repositories/IExportReportRepository.java`
      with methods: save, findByProjectId, delete

### Application Layer for Export

- [ ] T122 Create `IExportService` interface in
      `src/main/java/com/yagi/socialanalyzer/application/services/IExportService.java`
      with methods: generateReport, getReportPath
- [ ] T123 Implement `ExportService` in
      `src/main/java/com/yagi/socialanalyzer/application/services/ExportService.java`
      implementing `IExportService` with CSV, PDF, JSON export formats
- [ ] T124 Implement CSV export in `ExportService`: sentiment results with
      columns (post_id, timestamp, content_preview, sentiment_label,
      confidence), damage results with columns (post_id, categories,
      matched_keywords)
- [ ] T125 Implement JSON export in `ExportService`: full analysis results in
      structured JSON with metadata, sentiment timeline data, damage
      distribution, sample posts
- [ ] T126 [P] Implement PDF export in `ExportService` using Apache PDFBox or
      iText: title page, executive summary, sentiment timeline chart (embedded
      image), damage distribution chart, top posts per category

### Infrastructure Layer for Export

- [ ] T127 Implement `SQLiteExportReportRepository` in
      `src/main/java/com/yagi/socialanalyzer/infrastructure/persistence/SQLiteExportReportRepository.java`
      implementing `IExportReportRepository` with JDBC for export_reports table
- [ ] T128 Add PDF generation dependencies to `pom.xml`: Apache PDFBox or iText
      library

### UI Layer for Export

- [ ] T129 Create export dialog FXML in
      `src/main/resources/fxml/ExportDialog.fxml` with report type radio buttons
      (Sentiment Analysis / Damage Categorization / Combined), format radio
      buttons (CSV / PDF / JSON), date range filter, platform filter, output
      directory chooser
- [ ] T130 Create `ExportController` in
      `src/main/java/com/yagi/socialanalyzer/ui/controllers/ExportController.java`
      with export generation, progress indicator, "Open Report" button after
      completion
- [ ] T131 Create `ExportTask` extending `Task<ExportReport>` in
      `src/main/java/com/yagi/socialanalyzer/ui/tasks/ExportTask.java` wrapping
      `ExportService.generateReport`

### Integration for Export

- [ ] T132 Add "Export Report" button in sentiment analysis view and damage
      analysis view opening export dialog
- [ ] T133 Add "View Reports" menu item showing list of previously generated
      reports with options to open or delete

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and finalize
production readiness

### Documentation

- [ ] T134 [P] Create comprehensive user guide in `docs/user-guide.md` covering
      project creation, data collection, analysis, report generation,
      troubleshooting
- [ ] T135 [P] Create architecture documentation in `docs/architecture.md`
      explaining layered design, entity relationships, extension points
- [ ] T136 [P] Update README.md with project overview, features, installation
      instructions, usage examples, screenshots

### Code Quality

- [ ] T137 Run Checkstyle validation across entire codebase, fix all violations
      (ensure 0 warnings)
- [ ] T138 Run SpotBugs analysis, fix all high/medium priority bugs
- [ ] T139 Run PMD analysis, fix code quality issues (unused variables, complex
      methods, duplicate code)
- [ ] T140 [P] Add JavaDoc comments to all public classes and methods with
      proper @param, @return, @throws tags

### Performance Optimization

- [ ] T141 Implement connection pooling in `DatabaseManager` using HikariCP for
      improved SQLite performance
- [ ] T142 Add database indices: CREATE INDEX idx_posts_published ON
      posts_metadata(published_at), CREATE INDEX idx_sentiment_project ON
      sentiment_results(project_id)
- [ ] T143 Optimize JSON file reading: implement streaming parser for large post
      files instead of loading entire file to memory
- [ ] T144 Add result caching in `AnalysisService`: cache sentiment/damage
      summaries, invalidate on re-analysis

### Error Handling & Logging

- [ ] T145 Review all exception handling: ensure meaningful error messages,
      proper exception types, no swallowed exceptions
- [ ] T146 Add structured logging with MDC (Mapped Diagnostic Context) including
      projectId, userId for log correlation
- [ ] T147 Implement global exception handler in JavaFX application showing
      user-friendly error dialogs with option to view stack trace

### UI/UX Improvements

- [ ] T148 [P] Create application icon in `src/main/resources/icons/` and
      configure in JavaFX stage
- [ ] T149 Implement keyboard shortcuts: Ctrl+N (new project), Ctrl+S (save),
      Ctrl+E (export), F5 (refresh)
- [ ] T150 Add tooltips to all form fields explaining expected input format and
      constraints
- [ ] T151 Implement dark mode support with CSS theme switching in
      `src/main/resources/css/dark-theme.css`
- [ ] T152 Add confirmation dialogs for destructive actions: delete project,
      delete posts, overwrite analysis results

### Internationalization

- [ ] T153 Complete Vietnamese translations in `messages_vi.properties` for all
      UI labels, error messages, tooltips
- [ ] T154 Add language switcher in settings menu allowing users to change UI
      language (en/vi) with immediate effect

### Security

- [ ] T155 Implement credential encryption key rotation: allow users to change
      master password, re-encrypt all stored credentials
- [ ] T156 Add audit logging for sensitive operations: credential access,
      project deletion, data export to `logs/audit.log`

### Validation & Testing

- [ ] T157 Validate quickstart.md by following all setup steps on clean Windows
      10, macOS, Linux machines
- [ ] T158 Perform end-to-end testing: create project → collect data → analyze
      sentiment → analyze damage → export report on all 3 OS platforms
- [ ] T159 Load testing: verify system handles 100,000 posts without crashes,
      measure analysis performance (target <5 min for 10K posts)
- [ ] T160 UI responsiveness testing: verify all long operations run in
      background threads, UI never freezes >100ms

### Deployment

- [ ] T161 Create Windows installer using jpackage with JRE bundled
- [ ] T162 Create macOS .dmg installer using jpackage
- [ ] T163 Create Linux .deb package for Ubuntu/Debian
- [ ] T164 [P] Write installation guide in `docs/installation.md` for each
      platform

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user
  stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - **US1 (Phase 3)**: Can start after Phase 2 - No dependencies on other
    stories ✅ **MVP**
  - **US2 (Phase 4)**: Can start after Phase 2 - Uses posts collected by US1 but
    doesn't block US1
  - **US3 (Phase 5)**: Can start after Phase 2 - Uses posts collected by US1 but
    doesn't block US1/US2
  - **US4 (Phase 6)**: Can start after Phase 2 - Enhances configuration but
    doesn't block analysis
- **Export (Phase 7)**: Depends on US2 and US3 completion (needs analysis
  results to export)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Independence

- **US1**: Fully independent - data collection works standalone
- **US2**: Depends on posts existing (from US1 collection) but US1 doesn't need
  US2
- **US3**: Depends on posts existing (from US1 collection) but US1/US2 don't
  need US3
- **US4**: Enhances all stories with flexible config but all stories work
  without it

### Within Each User Story

- Domain entities before repositories
- Repositories before services
- Services before UI controllers
- UI FXML before controllers
- Controllers before integration tasks

### Parallel Opportunities

- **Phase 1 (Setup)**: Tasks T003-T010 can run in parallel (different files)
- **Phase 2 (Foundational)**:
  - T011-T015 value objects can run in parallel
  - T016-T019 entities can run in parallel
  - T020-T024 infrastructure can run in parallel with entities
- **US1 (Phase 3)**:
  - T030-T032 domain interfaces in parallel
  - T036-T041 platform data sources in parallel (different platforms)
  - T046-T056 UI views in parallel (different FXML files)
- **US2 (Phase 4)**:
  - T063-T065 domain layer in parallel
  - T067-T068 lexicon analyzer in parallel with T070 Python analyzer
- **US3 (Phase 5)**:
  - T087-T090 domain layer in parallel
- **US4 (Phase 6)**: Mostly sequential due to configuration dependencies
- **Export (Phase 7)**:
  - T120-T121 domain in parallel
  - T124-T126 export formats in parallel
- **Polish (Phase 8)**:
  - T134-T136 documentation in parallel
  - T161-T164 platform installers in parallel

---

## Parallel Example: User Story 1 (Data Collection)

```bash
# After Phase 2 Foundational is complete, these US1 tasks can start in parallel:

# Developer A: Domain entities and repositories
- T030 Create SocialMediaPost entity
- T031 Create IProjectRepository interface
- T032 Create IPostRepository interface

# Developer B: Infrastructure - API data sources
- T036 Implement TwitterDataSource
- T037 Implement RedditDataSource
- T038 Implement YouTubeDataSource

# Developer C: Infrastructure - Selenium data sources
- T039 Implement FacebookSeleniumDataSource
- T040 Implement TikTokSeleniumDataSource
- T041 Implement GoogleSeleniumDataSource

# Developer D: UI views
- T046 Create ProjectConfigDialog FXML
- T048 Create ProjectListView FXML
- T050 Create DataCollectionView FXML
- T053 Create RawDataView FXML
- T055 Create CredentialsDialog FXML

# After entities/interfaces complete (T030-T032), sequential tasks:
- T033 Implement SQLiteProjectRepository (needs IProjectRepository)
- T034 Implement SQLitePostRepository (needs IPostRepository)
- T042 Create IDataCollectionService interface
- T043 Implement DataCollectionService (needs repositories + data sources)

# After FXML complete (T046-T055), controllers:
- T047 Create ProjectConfigController
- T049 Create ProjectListController
- T051 Create DataCollectionController
- T054 Create RawDataController
- T056 Create CredentialsController

# Finally, integration tasks (T057-T062) wire everything together
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (~2-3 hours)
2. Complete Phase 2: Foundational (~1-2 days) ⚠️ CRITICAL BLOCKER
3. Complete Phase 3: User Story 1 (~3-5 days)
4. **STOP and VALIDATE**: Test data collection end-to-end on all platforms
5. Deploy MVP - immediate value to researchers (automated data collection)

**Estimated MVP Time**: 1-2 weeks for single developer

### Incremental Delivery

1. **Week 1-2**: Setup + Foundational + US1 → **Deploy MVP** ✅
2. **Week 3**: Add US2 (Sentiment Analysis) → Test independently → **Deploy
   v1.1**
3. **Week 4**: Add US3 (Damage Categorization) → Test independently → **Deploy
   v1.2**
4. **Week 5**: Add US4 (Flexible Config) + Export → Test → **Deploy v2.0**
5. **Week 6**: Polish, documentation, installers → **Deploy v2.1 Production**

### Parallel Team Strategy

With 3-4 developers after Foundational phase:

- **Developer 1**: US1 (Data Collection) - highest priority
- **Developer 2**: US2 (Sentiment Analysis) - can work in parallel using sample
  data
- **Developer 3**: US3 (Damage Categorization) - can work in parallel using
  sample data
- **Developer 4**: Infrastructure (Python API, installers, docs)

Stories integrate at end without blocking each other.

---

## Task Summary

**Total Tasks**: 170

**Task Count per Phase**:

- Phase 1 (Setup): 10 tasks
- Phase 2 (Foundational): 19 tasks
- Phase 3 (US1 - Data Collection): 39 tasks (33 original + 6 real platform
  implementations)
- Phase 4 (US2 - Sentiment Analysis): 24 tasks
- Phase 5 (US3 - Damage Categorization): 22 tasks
- Phase 6 (US4 - Flexible Config): 11 tasks
- Phase 7 (Export): 14 tasks
- Phase 8 (Polish): 31 tasks

**Parallel Opportunities**: 51+ tasks marked [P] can run in parallel

**Independent Test Criteria**:

- US1: Create project, collect from 2+ platforms, verify posts in DB and JSON
  files
- US2: Run sentiment analysis on collected posts, view timeline chart, drill
  down to samples
- US3: Run damage categorization, view distribution chart, filter by category
- US4: Add new platform via UI, modify keywords, create new damage category

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1 only) = 68 tasks (62
stubs + 6 real implementations), delivers automated data collection with real
platform integration

---

## Notes

- All tasks follow strict checklist format:
  `- [ ] [ID] [P?] [Story?] Description with file path`
- Tasks organized by user story for independent implementation and testing
- No test tasks included (not explicitly requested in spec.md)
- Each user story can be deployed independently after completion
- Foundational phase (Phase 2) is critical blocker - MUST complete before any
  story work
- Parallel tasks marked [P] touch different files with no dependencies
- Story labels [US1]-[US4] map to priority levels P1-P4 from spec.md
- File paths follow Maven standard layout from plan.md
- Tasks are immediately executable by LLM - no additional context needed
