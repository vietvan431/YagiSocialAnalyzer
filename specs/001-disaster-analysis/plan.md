# Implementation Plan: Disaster Social Media Analysis System

**Branch**: `001-disaster-analysis` | **Date**: 2025-10-31 | **Spec**:
[spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-disaster-analysis/spec.md`

## Summary

Build a Java desktop application for disaster researchers to collect, analyze,
and visualize social media data about disasters in Vietnam. The system will:

1. **Collect data** from multiple social media platforms (Facebook, Twitter,
   Reddit, YouTube, TikTok, Google) using platform APIs where available, or
   Selenium web scraping as fallback
2. **Analyze sentiment trends** over time (positive/negative/neutral
   classification) to track public psychological response
3. **Categorize damage types** into six categories (Affected people, Economic
   disruption, Damaged buildings, Lost property, Infrastructure, Other)
4. **Provide flexible configuration** for adding new platforms, modifying
   keywords, and updating damage categories without code changes

Technical approach: JavaFX desktop UI with Maven build, modular architecture
separating data collection (Java), preprocessing (Java), and analysis (Python
API or Java), with local file-based storage for collected data and
configuration.

## Technical Context

**Language/Version**: Java 25 (latest version with modern features and
performance improvements)  
**Primary Dependencies**:

- JavaFX 21+ (Desktop UI framework)
- Maven 3.9+ (Build tool)
- Selenium WebDriver 4.x (Web scraping fallback for platforms without API)
- Jackson 2.15+ (JSON parsing for API responses and configuration)
- Apache HttpClient 5.x (HTTP client for API calls)
- SLF4J + Logback (Logging)
- JUnit 5 + TestFX (Testing)
- (Optional) Python 3.11+ with FastAPI (if sentiment/damage analysis implemented
  in Python)

**Storage**:

- Local file system for collected posts (JSON files organized by
  project/platform/date)
- SQLite embedded database for metadata, project configurations, and analysis
  results
- Configuration files (YAML/JSON) for platform definitions, keywords, damage
  categories

**Testing**:

- JUnit 5 for unit tests
- TestFX for JavaFX UI tests
- Mockito for mocking external dependencies (API clients, file I/O)
- Test coverage target: 80%+ on core domain logic

**Target Platform**:

- Desktop: Windows 10/11, macOS 10.15+, Linux (Ubuntu 20.04+)
- Java Runtime: JRE 17+ required
- Minimum specs: 8GB RAM, dual-core processor, 1920x1080 display, 10GB free disk
  space

**Project Type**: Single desktop application (JavaFX)

**Performance Goals**:

- Data collection: Handle 10,000+ posts per platform without UI freeze
- Sentiment analysis: Process 10,000 posts in <5 minutes
- UI responsiveness: <100ms for user interactions, background threads for long
  operations
- Export: Generate reports <2 minutes for 10,000 posts

**Constraints**:

- UI thread must never block (use JavaFX Task/Service for background work)
- All API credentials stored encrypted
- Support offline analysis of previously collected data
- Graceful handling of API rate limits (exponential backoff, resume capability)
- Multi-language support: Vietnamese and English

**Scale/Scope**:

- Support 6+ social media platforms initially, extensible to 20+
- Handle disaster projects spanning 1-365 days
- Store 100,000+ posts per project
- 5-10 concurrent data collection tasks
- ~50 UI screens/dialogs (main window, project config, data collection, analysis
  views, settings)

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

### I. Object-Oriented Design Discipline ✅ PASS

- **Layered architecture**: UI (JavaFX controllers) → Application Services →
  Domain → Infrastructure (data access, external APIs)
- **SOLID principles**: Strategy pattern for platform adapters, Dependency
  Injection for service wiring
- **Encapsulation**: Domain entities hide state, expose behavior through methods
- **Interfaces for collaborations**: `DataCollector`, `SentimentAnalyzer`,
  `DamageClassifier`, `DataRepository`
- **No global state**: Configuration injected, thread-safe service instances

### II. Code Quality & Maintainability ✅ PASS

- **Unit tests**: JUnit 5 with Mockito, target 80%+ coverage on domain/service
  layers
- **Static analysis**: Maven plugins for Checkstyle, SpotBugs, PMD configured in
  pom.xml
- **Complexity limits**: Checkstyle cyclomatic complexity < 10 per method
- **Peer review**: Required per constitution before merge

### III. User Experience & Accessibility ✅ PASS

- **Non-blocking UI**: All long operations (data collection, analysis) use
  JavaFX `Task` on background threads
- **Progress indicators**: Collection/analysis progress shown with cancel
  buttons
- **Keyboard navigation**: JavaFX focus traversal configured
- **Error handling**: User-friendly alerts with actionable messages (e.g.,
  "Twitter API rate limit reached. Retry in 15 minutes?")
- **Persistence**: Project state auto-saved, resume on restart

### IV. Data Accuracy & Determinism ✅ PASS

- **Timezone handling**: All timestamps normalized to UTC+7 (Vietnam ICT)
- **Input validation**: Date ranges, keyword patterns, API credentials validated
  before use
- **Reproducibility**: Analysis results deterministic for same input data;
  random seeds fixed in tests
- **Language detection**: Explicit language tags stored with posts
- **Precision**: Sentiment confidence scores stored as `double` (IEEE 754)

### V. Performance, Stability & Observability ✅ PASS

- **UI responsiveness**: <100ms target for button clicks (background threads for
  heavy work)
- **Progress/cancellation**: All long operations expose progress and support
  cancellation
- **Logging**: SLF4J/Logback structured logs (INFO in production, DEBUG in dev)
- **Error recovery**: API failures logged, retried with exponential backoff
- **Data format versioning**: JSON schemas versioned; migration path for format
  changes

### Additional Constraints ✅ PASS

- **Java 25**: Documented in Technical Context
- **Maven build**: Configured with reproducible builds, dependency lock via
  `maven-enforcer-plugin`
- **Package structure**: Clear layering (ui/, domain/, application/,
  infrastructure/)
- **Security**: API credentials encrypted using AES-256, stored in OS keyring
  (jdk.security.jgss)
- **Javadoc**: Public APIs documented with `@param`, `@return`, `@throws`

**Gate Result**: ✅ **PASS** - All constitution principles satisfied. No
violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── yagi/
│   │           └── socialanalyzer/
│   │               ├── YagiSocialAnalyzerApp.java          # JavaFX main application entry
│   │               ├── domain/                              # Core business entities
│   │               │   ├── model/
│   │               │   │   ├── DisasterProject.java
│   │               │   │   ├── SocialMediaPost.java
│   │               │   │   ├── DataSource.java
│   │               │   │   ├── SentimentResult.java
│   │               │   │   ├── DamageCategory.java
│   │               │   │   └── AnalysisConfiguration.java
│   │               │   ├── service/                         # Domain services (interfaces)
│   │               │   │   ├── DataCollector.java
│   │               │   │   ├── SentimentAnalyzer.java
│   │               │   │   └── DamageClassifier.java
│   │               │   └── repository/                      # Repository interfaces
│   │               │       ├── ProjectRepository.java
│   │               │       ├── PostRepository.java
│   │               │       └── ConfigurationRepository.java
│   │               ├── application/                         # Application services
│   │               │   ├── collector/
│   │               │   │   ├── CollectorOrchestrator.java
│   │               │   │   ├── PlatformCollectorFactory.java
│   │               │   │   └── CollectionProgressTracker.java
│   │               │   ├── analysis/
│   │               │   │   ├── SentimentAnalysisService.java
│   │               │   │   ├── DamageCategorizationService.java
│   │               │   │   └── TimeSeriesAggregator.java
│   │               │   ├── export/
│   │               │   │   ├── ReportGenerator.java
│   │               │   │   ├── CsvExporter.java
│   │               │   │   └── PdfReportBuilder.java
│   │               │   └── config/
│   │               │       ├── ConfigurationManager.java
│   │               │       └── CredentialManager.java
│   │               ├── infrastructure/                      # External integrations
│   │               │   ├── collector/
│   │               │   │   ├── api/                         # API-based collectors
│   │               │   │   │   ├── TwitterApiCollector.java
│   │               │   │   │   ├── RedditApiCollector.java
│   │               │   │   │   └── YouTubeApiCollector.java
│   │               │   │   └── selenium/                    # Selenium-based collectors
│   │               │   │       ├── FacebookSeleniumCollector.java
│   │               │   │       ├── TikTokSeleniumCollector.java
│   │               │   │       └── GoogleSeleniumCollector.java
│   │               │   ├── analysis/
│   │               │   │   ├── JavaSentimentAnalyzer.java   # Java-based sentiment (lexicon/model)
│   │               │   │   ├── PythonApiSentimentAnalyzer.java  # Python API client (optional)
│   │               │   │   └── KeywordDamageClassifier.java
│   │               │   ├── persistence/
│   │               │   │   ├── sqlite/
│   │               │   │   │   ├── SqliteProjectRepository.java
│   │               │   │   │   └── SqliteConnectionManager.java
│   │               │   │   └── file/
│   │               │   │       ├── JsonPostRepository.java
│   │               │   │       └── YamlConfigRepository.java
│   │               │   └── util/
│   │               │       ├── HttpClientFactory.java
│   │               │       └── EncryptionUtil.java
│   │               └── ui/                                  # JavaFX UI layer
│   │                   ├── MainController.java
│   │                   ├── project/
│   │                   │   ├── ProjectListController.java
│   │                   │   └── ProjectConfigController.java
│   │                   ├── collection/
│   │                   │   ├── DataCollectionController.java
│   │                   │   └── CollectionProgressView.java
│   │                   ├── analysis/
│   │                   │   ├── SentimentAnalysisController.java
│   │                   │   ├── DamageAnalysisController.java
│   │                   │   └── ChartViewer.java
│   │                   ├── config/
│   │                   │   ├── DataSourceConfigController.java
│   │                   │   └── CategoryEditorController.java
│   │                   └── common/
│   │                       ├── AlertHelper.java
│   │                       └── ProgressDialogController.java
│   └── resources/
│       ├── fxml/                                            # JavaFX FXML files
│       │   ├── main.fxml
│       │   ├── project_list.fxml
│       │   ├── data_collection.fxml
│       │   └── sentiment_analysis.fxml
│       ├── css/
│       │   └── application.css
│       ├── config/
│       │   ├── platforms.yaml                               # Platform definitions
│       │   ├── damage_categories.yaml                       # Damage category keywords
│       │   └── logback.xml                                  # Logging config
│       └── i18n/
│           ├── messages_en.properties                       # English strings
│           └── messages_vi.properties                       # Vietnamese strings

tests/
├── unit/
│   └── java/
│       └── com/
│           └── yagi/
│               └── socialanalyzer/
│                   ├── domain/                              # Domain logic tests
│                   ├── application/                         # Service tests
│                   └── infrastructure/                      # Integration tests (mocked externals)
└── integration/
    └── java/
        └── com/
            └── yagi/
                └── socialanalyzer/
                    ├── ui/                                  # TestFX UI tests
                    └── collector/                           # Live API tests (optional)

python-analysis-api/                                         # Optional Python analysis service
├── requirements.txt
├── main.py                                                  # FastAPI server
├── sentiment/
│   └── analyzer.py                                          # Sentiment model
└── damage/
    └── classifier.py                                        # Damage categorization

pom.xml                                                      # Maven build configuration
README.md
LICENSE
.gitignore
```

**Structure Decision**: Single desktop application using Maven standard layout.
Java 25 with JavaFX 21 for UI. Modular package structure follows DDD layering:

- **domain**: Core entities and interfaces (no external dependencies)
- **application**: Use case orchestration, business services
- **infrastructure**: External integrations (APIs, Selenium, SQLite, file I/O)
- **ui**: JavaFX controllers and views (depends on application layer)

Optional Python service (`python-analysis-api/`) provides REST API for advanced
sentiment/damage classification models. If used, Java calls it via HTTP; if not,
pure Java implementations in `infrastructure/analysis/` are used.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. Constitution Check passed all gates.
