package com.yagi.socialanalyzer.infrastructure.persistence;

import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Database manager for SQLite connection pooling and schema initialization.
 */
public class DatabaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DEFAULT_DB_PATH = "data/socialanalyzer.db";
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    
    private final String databasePath;
    private Connection connection;
    
    /**
     * Create database manager with default path.
     */
    public DatabaseManager() {
        this(DEFAULT_DB_PATH);
    }
    
    /**
     * Create database manager with custom path.
     */
    public DatabaseManager(String databasePath) {
        this.databasePath = databasePath;
    }
    
    /**
     * Get or create database connection.
     */
    public Connection getConnection() throws RepositoryException {
        try {
            if (connection == null || connection.isClosed()) {
                ensureDatabaseDirectory();
                String url = "jdbc:sqlite:" + databasePath;
                connection = DriverManager.getConnection(url);
                connection.setAutoCommit(true);
                
                // Enable foreign keys
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
                
                logger.info("Database connection established: {}", databasePath);
            }
            return connection;
        } catch (SQLException e) {
            logger.error("Failed to establish database connection", e);
            throw new RepositoryException("Failed to connect to database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize database schema if not exists.
     */
    public void initializeSchema() throws RepositoryException {
        logger.info("Initializing database schema...");

        try {
            String schema = loadSchemaFromResource();
            Connection conn = getConnection();

            // Ensure PRAGMA applies to this connection/session
            try (Statement pragma = conn.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }

            var statements = splitSql(schema); // robust split

            // ---- PASS 1: CREATE TABLE ... only (guarantee tables exist first)
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                for (String s : statements) {
                    String head = s.length() >= 12 ? s.substring(0, 12).toUpperCase() : s.toUpperCase();
                    if (head.startsWith("CREATE TABLE")) {
                        logger.debug("SQL[TABLE]: {}", s);
                        st.execute(s);
                    }
                }
            }
            conn.commit();

            // ---- PASS 2: everything else (indexes, pragma, etc.)
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                for (String s : statements) {
                    String head = s.length() >= 12 ? s.substring(0, 12).toUpperCase() : s.toUpperCase();
                    if (!head.startsWith("CREATE TABLE")) {
                        logger.debug("SQL[OTHER]: {}", s);
                        st.execute(s);
                    }
                }
            }
            conn.commit();

            logger.info("Database schema initialized successfully");
        } catch (SQLException | IOException e) {
            try { if (connection != null) connection.rollback(); } catch (Exception ignore) {}
            logger.error("Failed to initialize database schema", e);
            throw new RepositoryException("Failed to initialize schema: " + e.getMessage(), e);
        } finally {
            try { if (connection != null) connection.setAutoCommit(true); } catch (Exception ignore) {}
        }
    }

    /** Split SQL script into individual statements safely (handles CRLF, inline -- comments, trailing semicolons). */
    private java.util.List<String> splitSql(String sql) {
        var out = new java.util.ArrayList<String>();
        // Normalize newlines and strip BOM if any
        String s = sql.replace("\r", "").replace("\uFEFF", "");

        // Remove full-line comments and trim inline "-- ..." on each line
        var cleaned = new StringBuilder();
        for (String line : s.split("\n")) {
            String ln = line;
            int i = ln.indexOf("--");
            if (i >= 0) ln = ln.substring(0, i);
            if (!ln.trim().isEmpty()) cleaned.append(ln).append('\n');
        }

        // Split on semicolon that ends a statement (semicolon followed by newline or end)
        for (String part : cleaned.toString().split(";(\\n|\\s*$)")) {
            String stmt = part.strip();
            if (!stmt.isEmpty()) out.add(stmt);
        }
        return out;
    }

    
    /**
     * Remove inline SQL comments.
     */
    private String removeInlineComments(String sql) {
        StringBuilder result = new StringBuilder();
        String[] lines = sql.split("\n");
        
        for (String line : lines) {
            // Remove inline comments (-- style)
            int commentIndex = line.indexOf("--");
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex);
            }
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                result.append(line).append("\n");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Close database connection.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.warn("Error closing database connection", e);
            }
        }
    }
    
    /**
     * Check if database exists.
     */
    public boolean databaseExists() {
        return Files.exists(Paths.get(databasePath));
    }
    
    /**
     * Delete database file (for testing).
     */
    public void deleteDatabase() throws RepositoryException {
        close();
        try {
            Path path = Paths.get(databasePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Database deleted: {}", databasePath);
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to delete database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ensure database directory exists.
     */
    private void ensureDatabaseDirectory() throws RepositoryException {
        try {
            Path dbPath = Paths.get(databasePath);
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.info("Created database directory: {}", parentDir);
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to create database directory: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load schema SQL from resources.
     */
    private String loadSchemaFromResource() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new IOException("Schema resource not found: " + SCHEMA_RESOURCE);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
    
    /**
     * Get database file path.
     */
    public String getDatabasePath() {
        return databasePath;
    }
}
