-- YagiSocialAnalyzer SQLite Database Schema
-- Version 1.0

-- Enable foreign keys
PRAGMA foreign_keys = ON;

-- Projects table
CREATE TABLE IF NOT EXISTS projects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    disaster_name TEXT NOT NULL,
    region TEXT NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    keywords TEXT NOT NULL, -- JSON array
    data_sources TEXT NOT NULL, -- JSON array
    status TEXT NOT NULL CHECK(status IN ('DRAFT', 'COLLECTING', 'PAUSED', 'COMPLETED', 'ANALYZING', 'ARCHIVED')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    total_posts_collected INTEGER NOT NULL DEFAULT 0,
    CHECK(total_posts_collected >= 0)
);

CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_projects_created_at ON projects(created_at);

-- Data sources table
CREATE TABLE IF NOT EXISTS data_sources (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('API', 'SELENIUM')),
    base_url TEXT NOT NULL,
    authentication_type TEXT NOT NULL CHECK(authentication_type IN ('NONE', 'API_KEY', 'OAUTH2', 'SESSION_LOGIN')),
    rate_limit INTEGER NOT NULL CHECK(rate_limit > 0),
    rate_limit_window_seconds INTEGER NOT NULL CHECK(rate_limit_window_seconds > 0),
    enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0, 1)),
    config_params TEXT, -- JSON object
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_data_sources_enabled ON data_sources(enabled);

-- Damage categories table
CREATE TABLE IF NOT EXISTS damage_categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    keywords_vietnamese TEXT NOT NULL, -- JSON array
    keywords_english TEXT NOT NULL, -- JSON array
    description TEXT,
    priority INTEGER NOT NULL CHECK(priority BETWEEN 1 AND 10),
    color TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_damage_categories_priority ON damage_categories(priority);

-- Posts metadata table
CREATE TABLE IF NOT EXISTS posts_metadata (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    post_id TEXT NOT NULL,
    author TEXT NOT NULL,
    content TEXT NOT NULL,
    published_at TEXT NOT NULL,
    likes INTEGER NOT NULL DEFAULT 0,
    shares INTEGER NOT NULL DEFAULT 0,
    comments INTEGER NOT NULL DEFAULT 0,
    views INTEGER NOT NULL DEFAULT 0,
    url TEXT,
    collected_at TEXT NOT NULL,
    data_file_path TEXT NOT NULL,
    CHECK(likes >= 0 AND shares >= 0 AND comments >= 0 AND views >= 0),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    UNIQUE(project_id, platform, post_id)
);

CREATE INDEX IF NOT EXISTS idx_posts_project_id ON posts_metadata(project_id);
CREATE INDEX IF NOT EXISTS idx_posts_platform ON posts_metadata(platform);
CREATE INDEX IF NOT EXISTS idx_posts_published_at ON posts_metadata(published_at);
CREATE INDEX IF NOT EXISTS idx_posts_collected_at ON posts_metadata(collected_at);

-- Sentiment analysis results table
CREATE TABLE IF NOT EXISTS sentiment_results (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    post_id TEXT NOT NULL,
    sentiment_label TEXT NOT NULL CHECK(sentiment_label IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    confidence_score REAL NOT NULL CHECK(confidence_score BETWEEN 0.0 AND 1.0),
    analyzed_at TEXT NOT NULL,
    backend TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts_metadata(id) ON DELETE CASCADE,
    UNIQUE(post_id)
);

CREATE INDEX IF NOT EXISTS idx_sentiment_project_id ON sentiment_results(project_id);
CREATE INDEX IF NOT EXISTS idx_sentiment_label ON sentiment_results(sentiment_label);
CREATE INDEX IF NOT EXISTS idx_sentiment_analyzed_at ON sentiment_results(analyzed_at);

-- Damage classifications table
CREATE TABLE IF NOT EXISTS damage_classifications (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    post_id TEXT NOT NULL,
    category_id TEXT NOT NULL,
    matched_keywords TEXT NOT NULL, -- JSON array
    confidence_score REAL NOT NULL CHECK(confidence_score BETWEEN 0.0 AND 1.0),
    classified_at TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts_metadata(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES damage_categories(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_damage_project_id ON damage_classifications(project_id);
CREATE INDEX IF NOT EXISTS idx_damage_category_id ON damage_classifications(category_id);
CREATE INDEX IF NOT EXISTS idx_damage_post_id ON damage_classifications(post_id);
CREATE INDEX IF NOT EXISTS idx_damage_classified_at ON damage_classifications(classified_at);

-- Analysis configurations table
CREATE TABLE IF NOT EXISTS analysis_configurations (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL UNIQUE,
    sentiment_backend TEXT NOT NULL CHECK(sentiment_backend IN ('java_lexicon', 'python_api')),
    sentiment_threshold REAL NOT NULL CHECK(sentiment_threshold BETWEEN 0.0 AND 1.0),
    python_api_url TEXT,
    damage_classification_enabled INTEGER NOT NULL DEFAULT 1 CHECK(damage_classification_enabled IN (0, 1)),
    minimum_keyword_matches INTEGER NOT NULL DEFAULT 1 CHECK(minimum_keyword_matches >= 0),
    custom_settings TEXT, -- JSON object
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_analysis_config_project_id ON analysis_configurations(project_id);

-- Export reports table
CREATE TABLE IF NOT EXISTS export_reports (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    report_type TEXT NOT NULL CHECK(report_type IN ('SENTIMENT_ANALYSIS', 'DAMAGE_CATEGORIZATION', 'COMBINED')),
    report_format TEXT NOT NULL CHECK(report_format IN ('CSV', 'PDF', 'JSON')),
    file_path TEXT NOT NULL,
    generated_at TEXT NOT NULL,
    file_size_bytes INTEGER NOT NULL CHECK(file_size_bytes >= 0),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_export_project_id ON export_reports(project_id);
CREATE INDEX IF NOT EXISTS idx_export_generated_at ON export_reports(generated_at);
CREATE INDEX IF NOT EXISTS idx_export_type ON export_reports(report_type);
