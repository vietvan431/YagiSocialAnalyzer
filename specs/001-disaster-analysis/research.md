# Research: Disaster Social Media Analysis System

**Feature**: Disaster Social Media Analysis  
**Date**: 2025-10-31  
**Phase**: 0 - Technology Research and Architecture Decisions

## Overview

This document consolidates research findings for technical decisions needed to
implement the disaster social media analysis system. All "NEEDS CLARIFICATION"
items from Technical Context have been resolved through research.

## Research Tasks & Findings

### 1. Social Media Platform Data Access Strategies

**Task**: Research best approaches for collecting data from Facebook, Twitter,
Reddit, YouTube, TikTok, and Google given current API availability and
restrictions.

**Decision**: Hybrid approach using official APIs where available, Selenium
WebDriver for platforms without accessible APIs.

**Rationale**:

- **Twitter/X API**: Official API v2 available with tiered pricing (Free tier:
  1,500 posts/month; Basic $100/month: 10,000 posts/month). Use
  `TwitterApiCollector` with OAuth 2.0.
- **Reddit API**: Free official API with OAuth 2.0, rate limit 60
  requests/minute. Use `RedditApiCollector` with PRAW-like HTTP client.
- **YouTube Data API**: Google official API, free tier 10,000 units/day (search
  ~100 queries/day). Use `YouTubeApiCollector` with API key.
- **Facebook Graph API**: Severely restricted for public data collection
  (requires page admin access). Use Selenium as fallback
  (`FacebookSeleniumCollector`) with user-provided login.
- **TikTok**: No official public data API for research. Use Selenium
  (`TikTokSeleniumCollector`) with scroll automation.
- **Google News/Search**: No official API for web search results. Use Selenium
  (`GoogleSeleniumCollector`) to scrape search results page.

**Alternatives Considered**:

- Pure API approach: Rejected because Facebook, TikTok, Google don't provide
  accessible public data APIs
- Pure web scraping: Rejected because it's slower and more fragile than APIs;
  use APIs where available
- Third-party aggregators (e.g., Brandwatch, Hootsuite): Rejected due to cost
  ($500-5000/month) and vendor lock-in

**Implementation Notes**:

- Selenium collectors must handle JavaScript rendering, pagination, anti-bot
  detection (random delays, user-agent rotation)
- API collectors must handle rate limits with exponential backoff (initial delay
  1 sec, max 5 min)
- All collectors implement common `DataCollector` interface with methods:
  `configure()`, `collect()`, `pause()`, `resume()`, `getProgress()`

---

### 2. Sentiment Analysis Approach for Vietnamese and English

**Task**: Research sentiment analysis techniques suitable for disaster-related
content in Vietnamese and English languages.

**Decision**: Hybrid approach with configurable backend:

1. **Java-based lexicon approach** (default): Dictionary-based sentiment scoring
   using Vietnamese and English disaster lexicons
2. **Python ML model API** (optional): Pre-trained transformer models (PhoBERT
   for Vietnamese, RoBERTa for English) exposed via FastAPI

**Rationale**:

- Disaster content has unique vocabulary (e.g., "casualties", "rescue", "loss"
  are negative but contextually important)
- Vietnamese sentiment analysis: Limited pre-trained models; PhoBERT (Facebook
  Research) achieves 91% accuracy on Vietnamese sentiment
- English sentiment analysis: RoBERTa or DistilBERT fine-tuned on disaster
  tweets (available on HuggingFace)
- Lexicon approach is deterministic, fast (10K posts/sec), requires no ML
  dependencies; sufficient for initial MVP
- ML approach is more accurate (85-90% vs 70-75%) but requires Python runtime
  and larger resource footprint

**Alternatives Considered**:

- Cloud APIs (Google Natural Language, AWS Comprehend): Rejected due to cost
  ($1-2 per 1000 docs) and data privacy concerns
- Rule-based only: Rejected as insufficient accuracy for nuanced disaster
  sentiment
- Pure ML in Java (DL4J): Rejected due to complexity and limited Vietnamese
  model availability

**Implementation Notes**:

- Java lexicon implementation: `JavaSentimentAnalyzer` uses
  `HashMap<String, Double>` for word scores, handles negation ("not good" →
  negative)
- Python API: `PythonApiSentimentAnalyzer` sends HTTP POST to
  `http://localhost:8000/analyze/sentiment` with JSON `{text, language}`
- Configuration flag in `analysis_config.yaml`:
  `sentiment_backend: java | python_api`

---

### 3. Damage Categorization Approach

**Task**: Research methods to classify social media posts into six damage
categories (Affected people, Economic disruption, Damaged buildings, Lost
property, Damaged infrastructure, Other).

**Decision**: Keyword-based multi-label classification with configurable keyword
dictionaries stored in YAML.

**Rationale**:

- Multi-label classification needed (one post can mention multiple damage types)
- Keyword approach is transparent, easily adjustable by researchers without
  retraining models
- Vietnamese disaster terminology is domain-specific; pretrained models lack
  disaster-specific vocabulary
- Accuracy target (70%) achievable with well-curated keyword lists

**Keyword Strategy**:

Each category has:

- **Vietnamese keywords**: ["người bị nạn", "thương vong", "mất tích"] for
  "Affected people"
- **English keywords**: ["casualties", "injured", "missing persons"]
- **Phrases**: Multi-word patterns like "nhà bị sập" (collapsed house)
- **Confidence scoring**: More keyword matches → higher confidence (0.0-1.0
  scale)

**Alternatives Considered**:

- Supervised ML classifier: Rejected due to lack of labeled Vietnamese disaster
  dataset (would require manual labeling of 5000+ posts)
- Zero-shot classification (GPT/BERT): Rejected due to API costs and latency
  (2-3 sec per post)
- Rule-based NLP (dependency parsing): Rejected as over-complex for MVP; keyword
  matching sufficient

**Implementation Notes**:

- `KeywordDamageClassifier` loads keywords from
  `resources/config/damage_categories.yaml`
- YAML structure:
  ```yaml
  categories:
    - name: "Affected people"
      keywords_vi: ["người bị nạn", "thương vong", ...]
      keywords_en: ["casualties", "injured", ...]
  ```
- Classification algorithm: TF-IDF weighted keyword matching, normalize to
  confidence scores

---

### 4. Data Storage Strategy

**Task**: Research optimal storage approach for 100,000+ social media posts with
metadata, supporting fast queries and offline access.

**Decision**: Hybrid storage:

- **SQLite** for structured metadata (projects, configurations, analysis
  results, search indices)
- **JSON files** for raw post data (organized by
  `data/{project_id}/{platform}/{date}/posts.json`)

**Rationale**:

- SQLite is embedded (no server), cross-platform, supports full-text search
  (FTS5), handles 100K+ rows efficiently
- JSON files preserve original post structure, support schema evolution, easily
  inspected/backed up
- Combined approach: SQLite for queries ("find all negative sentiment posts from
  Twitter"), JSON for raw content
- Estimated storage: ~1KB per post → 100MB for 100K posts (well within disk
  constraints)

**Schema Design**:

SQLite tables:

- `projects`: project metadata (id, name, disaster_name, date_range, keywords)
- `posts_metadata`: lightweight refs (post_id, project_id, platform, timestamp,
  language, file_path)
- `sentiment_results`: (post_id, sentiment_label, confidence, analysis_date)
- `damage_classifications`: (post_id, category_id, confidence) - supports
  multi-label

JSON file structure:

```json
{
  "posts": [
    {
      "id": "twitter_12345",
      "platform": "Twitter",
      "timestamp": "2024-09-15T10:30:00Z",
      "text": "Typhoon Yagi destroyed...",
      "author": "anonymized_user_001",
      "engagement": {"likes": 45, "shares": 12},
      "metadata": {...}
    }
  ]
}
```

**Alternatives Considered**:

- Pure SQLite (store post text in DB): Rejected due to BLOB size limits and
  slower full-text search on large texts
- Pure JSON files: Rejected due to slow queries (must scan all files) and no
  indexing
- MongoDB/PostgreSQL: Rejected as overkill (requires separate server process)

**Implementation Notes**:

- Use JDBC with HikariCP connection pooling for SQLite
- `JsonPostRepository` uses Jackson `ObjectMapper` for serialization
- File organization: `data/project_001/twitter/2024-09-15/posts_001.json` (split
  files at 1000 posts each)

---

### 5. JavaFX UI Architecture and Responsiveness

**Task**: Research JavaFX best practices for non-blocking UI with long-running
background tasks (data collection, analysis).

**Decision**: Use JavaFX `Task` and `Service` for all background operations,
with `ProgressIndicator` and cancellation support.

**Rationale**:

- JavaFX `Task` is built-in abstraction for background work with progress
  tracking
- `Service` wraps `Task` for restartable operations (e.g., data collection can
  be paused/resumed)
- UI thread must remain responsive per Constitution principle III
- `Task.updateProgress()` and `Task.updateMessage()` safely update UI from
  background threads

**Pattern**:

```java
public class DataCollectionTask extends Task<CollectionResult> {
    @Override
    protected CollectionResult call() throws Exception {
        updateProgress(0, totalPosts);
        for (int i = 0; i < totalPosts; i++) {
            if (isCancelled()) break;
            // Collect post...
            updateProgress(i+1, totalPosts);
            updateMessage("Collected " + (i+1) + " posts");
        }
        return result;
    }
}

// In controller:
DataCollectionTask task = new DataCollectionTask();
progressBar.progressProperty().bind(task.progressProperty());
statusLabel.textProperty().bind(task.messageProperty());
new Thread(task).start();
```

**Alternatives Considered**:

- `Platform.runLater()` for manual thread management: Rejected as error-prone
  and verbose
- Reactive frameworks (RxJava, Project Reactor): Rejected as over-complex for
  desktop UI
- Kotlin Coroutines: Rejected as requires Kotlin dependency

**Implementation Notes**:

- All controllers exposing long operations return `Task<T>` to be run on
  executor
- Global `ExecutorService` (cached thread pool) managed by
  `YagiSocialAnalyzerApp`
- Cancel buttons call `task.cancel(true)` and cleanup resources

---

### 6. Configuration Management and Extensibility

**Task**: Research approach for storing platform definitions, keywords, and
damage categories in editable files without code changes.

**Decision**: YAML configuration files loaded at runtime with hot-reload
support.

**Rationale**:

- YAML is human-readable, supports comments, easier to edit than JSON for
  non-developers
- Platform definitions (`platforms.yaml`) define connection params, rate limits,
  authentication types
- Keyword dictionaries (`damage_categories.yaml`) allow researchers to refine
  categories
- Changes take effect on next analysis run (no app restart needed)

**Configuration Structure**:

`platforms.yaml`:

```yaml
platforms:
  - id: "twitter"
    name: "Twitter/X"
    type: "api"
    base_url: "https://api.twitter.com/2"
    auth_type: "oauth2"
    rate_limit: 450 # requests per 15 min
    enabled: true

  - id: "facebook"
    name: "Facebook"
    type: "selenium"
    base_url: "https://www.facebook.com/search"
    enabled: true
```

`damage_categories.yaml`:

```yaml
version: "1.0"
categories:
  - id: "affected_people"
    name: "Affected people"
    keywords_vi: ["người bị nạn", "thương vong", "mất tích", "tử vong"]
    keywords_en: ["casualties", "injured", "missing", "deaths", "victims"]
    priority: 1
```

**Alternatives Considered**:

- Database storage: Rejected as harder to version control and bulk edit
- JSON: Rejected as less readable (no comments, more verbose syntax)
- Properties files: Rejected as insufficient for nested structures

**Implementation Notes**:

- `ConfigurationManager` uses SnakeYAML library to parse YAML into Java POJOs
- `@PostConstruct` method loads config; `FileWatcher` detects changes and
  triggers reload
- Validation: JSON Schema-like validation ensures required fields present

---

### 7. Credential Storage and Security

**Task**: Research secure storage for API credentials (Twitter tokens, Reddit
keys, etc.) per Constitution security requirements.

**Decision**: Encrypt credentials using AES-256, store in OS-specific secure
storage (Windows Credential Manager, macOS Keychain, Linux Secret Service).

**Rationale**:

- Storing plaintext credentials in config files violates Constitution principle
  (Security & Privacy)
- OS keyring integration provides hardware-backed encryption on supporting
  platforms
- Fallback to encrypted file if OS keyring unavailable

**Implementation**:

```java
public class CredentialManager {
    public void storeCredential(String service, String username, String password) {
        // Try OS keyring first
        if (SystemUtils.IS_OS_WINDOWS) {
            windowsCredentialStore.save(service, username, password);
        } else if (SystemUtils.IS_OS_MAC) {
            macKeychainStore.save(service, username, password);
        } else {
            // Fallback: AES-256 encrypted file
            encryptedFileStore.save(service, username, password);
        }
    }
}
```

**Alternatives Considered**:

- Plaintext config: Rejected due to security risks
- Environment variables: Rejected as not persistent across sessions
- Database encryption: Rejected as over-complex for desktop app

**Implementation Notes**:

- Use `java.security.KeyStore` for AES key management
- Master password derived from user-provided password using PBKDF2 (100,000
  iterations)
- Library: Apache Commons Crypto for AES encryption

---

### 8. Multi-language Support (i18n)

**Task**: Research internationalization approach for Vietnamese and English UI
strings.

**Decision**: Java `ResourceBundle` with properties files for each language.

**Rationale**:

- Built-in Java mechanism, well-supported by JavaFX
- Allows runtime language switching without app restart
- Easy for translators to edit `.properties` files

**Structure**:

`messages_en.properties`:

```properties
app.title=Disaster Social Media Analyzer
project.create=Create New Project
collection.start=Start Collection
```

`messages_vi.properties`:

```properties
app.title=Công cụ Phân tích Mạng xã hội Thảm họa
project.create=Tạo Dự án Mới
collection.start=Bắt đầu Thu thập
```

**Usage**:

```java
ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag("vi"));
String title = bundle.getString("app.title");
```

**Alternatives Considered**:

- Hardcoded strings: Rejected as not extensible
- Custom i18n framework: Rejected as reinventing the wheel

---

### 9. Chart Visualization Library

**Task**: Research charting library for sentiment timeline and damage
distribution visualizations.

**Decision**: JavaFX built-in charts (`LineChart`, `BarChart`, `PieChart`) for
standard visualizations; JFreeChart for advanced custom charts.

**Rationale**:

- JavaFX charts are native, integrate seamlessly with FXML, support CSS styling
- Sufficient for time-series line charts (sentiment over time) and bar/pie
  charts (damage distribution)
- JFreeChart provides more control for custom aggregations if needed (e.g.,
  stacked area charts)

**Alternatives Considered**:

- D3.js via WebView: Rejected as adds JavaScript dependency and complexity
- Apache ECharts: Rejected as requires embedding browser

---

### 10. Testing Strategy

**Task**: Research testing approach for JavaFX UI and asynchronous operations.

**Decision**:

- **Unit tests**: JUnit 5 + Mockito for domain/application layer (80%+ coverage)
- **UI tests**: TestFX for JavaFX integration tests (smoke tests for critical
  flows)
- **Contract tests**: Mock external APIs, verify correct HTTP requests

**Rationale**:

- TestFX provides headless mode for CI/CD (no display required)
- Mockito mocks `DataCollector` implementations to test services in isolation
- Contract tests ensure API adapters conform to platform expectations

**Implementation Notes**:

- TestFX example:

  ```java
  @Test
  public void testCreateProject(FxRobot robot) {
      robot.clickOn("#createProjectButton");
      robot.write("Typhoon Yagi");
      robot.clickOn("#saveButton");
      verifyThat("#projectList", hasItems(1));
  }
  ```

---

## Technology Stack Summary

| Component            | Technology                 | Version      | Justification                                                      |
| -------------------- | -------------------------- | ------------ | ------------------------------------------------------------------ |
| Language             | Java                       | 17 LTS       | Long-term support, mature ecosystem, OOP-friendly                  |
| UI Framework         | JavaFX                     | 21+          | Modern desktop UI, FXML separation, CSS styling                    |
| Build Tool           | Maven                      | 3.9+         | Industry standard, reproducible builds, extensive plugin ecosystem |
| Web Scraping         | Selenium WebDriver         | 4.x          | Handles JavaScript rendering, mature API, multi-browser support    |
| HTTP Client          | Apache HttpClient          | 5.x          | Robust, supports connection pooling, handles redirects             |
| JSON Library         | Jackson                    | 2.15+        | Fast, feature-rich, annotation-based                               |
| Database             | SQLite                     | 3.40+        | Embedded, zero-config, full-text search (FTS5)                     |
| Logging              | SLF4J + Logback            | 2.0+ / 1.4+  | Flexible, structured logging, performance                          |
| Testing              | JUnit 5 + TestFX           | 5.10+ / 4.0+ | Modern assertions, parameterized tests, UI testing                 |
| Configuration        | SnakeYAML                  | 2.0+         | YAML parsing, type-safe                                            |
| Encryption           | Apache Commons Crypto      | 1.2+         | AES-256, native acceleration                                       |
| Optional: Python API | FastAPI + uvicorn          | 0.104+       | Async, auto-generated docs, easy deployment                        |
| Optional: ML Models  | Transformers (HuggingFace) | 4.35+        | Pre-trained PhoBERT, RoBERTa                                       |

---

## Risk Assessment

| Risk                                        | Likelihood | Impact | Mitigation                                                          |
| ------------------------------------------- | ---------- | ------ | ------------------------------------------------------------------- |
| Social media API changes/deprecation        | High       | High   | Adapter pattern isolates API changes; Selenium fallback available   |
| Rate limit throttling during collection     | High       | Medium | Exponential backoff, pause/resume, multi-day collection support     |
| Vietnamese NLP model accuracy insufficient  | Medium     | Medium | Start with lexicon approach (deterministic); add ML later if needed |
| Selenium anti-bot detection                 | Medium     | Medium | User-agent rotation, random delays, manual fallback option          |
| Large dataset memory issues (>100K posts)   | Low        | Medium | Streaming JSON processing, pagination, lazy loading in UI           |
| Cross-platform JavaFX rendering differences | Low        | Low    | Test on all three platforms (Win/Mac/Linux), use standard controls  |

---

## Next Steps (Phase 1)

With all research completed:

1. Generate `data-model.md` defining entities and relationships
2. Create `contracts/` for internal service contracts and Python API spec (if
   used)
3. Write `quickstart.md` with setup instructions (Java 25, Maven, optional
   Python)
4. Update agent context files with selected tech stack
