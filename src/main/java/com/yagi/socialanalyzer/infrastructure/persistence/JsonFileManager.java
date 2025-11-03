package com.yagi.socialanalyzer.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JSON file manager for batch storage of social media posts.
 * Organizes files by project/platform/date structure.
 */
public class JsonFileManager {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonFileManager.class);
    private static final String DATA_DIR = "data";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final ObjectMapper objectMapper;
    
    public JsonFileManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Save a batch of posts to JSON file.
     * File path: data/{projectId}/{platform}/{date}/{timestamp}.json
     */
    public <T> String savePosts(UUID projectId, String platform, LocalDate date, 
                               List<T> posts) throws RepositoryException {
        if (posts == null || posts.isEmpty()) {
            throw new IllegalArgumentException("Posts list cannot be null or empty");
        }
        
        try {
            // Generate file path
            String dateStr = date.format(DATE_FORMATTER);
            long timestamp = System.currentTimeMillis();
            String fileName = String.format("%d.json", timestamp);
            
            Path filePath = Paths.get(DATA_DIR, projectId.toString(), platform, dateStr, fileName);
            
            // Ensure directory exists
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.debug("Created directory: {}", parentDir);
            }
            
            // Write JSON file
            objectMapper.writeValue(filePath.toFile(), posts);
            logger.info("Saved {} posts to {}", posts.size(), filePath);
            
            return filePath.toString();
            
        } catch (IOException e) {
            logger.error("Failed to save posts", e);
            throw new RepositoryException("Failed to save posts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load posts from a JSON file.
     */
    public <T> List<T> loadPosts(String filePath, Class<T> postType) throws RepositoryException {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new RepositoryException("File not found: " + filePath);
            }
            
            T[] posts = (T[]) objectMapper.readValue(path.toFile(), 
                java.lang.reflect.Array.newInstance(postType, 0).getClass());
            
            List<T> result = new ArrayList<>();
            for (T post : posts) {
                result.add(post);
            }
            
            logger.debug("Loaded {} posts from {}", result.size(), filePath);
            return result;
            
        } catch (IOException e) {
            logger.error("Failed to load posts from {}", filePath, e);
            throw new RepositoryException("Failed to load posts: " + e.getMessage(), e);
        }
    }
    
    /**
     * List all JSON files for a project and platform.
     */
    public List<Path> listPostFiles(UUID projectId, String platform) throws RepositoryException {
        try {
            Path platformDir = Paths.get(DATA_DIR, projectId.toString(), platform);
            
            if (!Files.exists(platformDir)) {
                return new ArrayList<>();
            }
            
            List<Path> files = new ArrayList<>();
            Files.walk(platformDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(files::add);
            
            logger.debug("Found {} post files for project {} platform {}", 
                files.size(), projectId, platform);
            return files;
            
        } catch (IOException e) {
            logger.error("Failed to list post files", e);
            throw new RepositoryException("Failed to list post files: " + e.getMessage(), e);
        }
    }
    
    /**
     * List all JSON files for a project, platform, and date.
     */
    public List<Path> listPostFiles(UUID projectId, String platform, LocalDate date) 
            throws RepositoryException {
        try {
            String dateStr = date.format(DATE_FORMATTER);
            Path dateDir = Paths.get(DATA_DIR, projectId.toString(), platform, dateStr);
            
            if (!Files.exists(dateDir)) {
                return new ArrayList<>();
            }
            
            List<Path> files = new ArrayList<>();
            Files.list(dateDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(files::add);
            
            logger.debug("Found {} post files for project {} platform {} date {}", 
                files.size(), projectId, platform, dateStr);
            return files;
            
        } catch (IOException e) {
            logger.error("Failed to list post files", e);
            throw new RepositoryException("Failed to list post files: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete all post files for a project.
     */
    public void deleteProjectFiles(UUID projectId) throws RepositoryException {
        try {
            Path projectDir = Paths.get(DATA_DIR, projectId.toString());
            
            if (Files.exists(projectDir)) {
                deleteDirectory(projectDir);
                logger.info("Deleted all files for project {}", projectId);
            }
            
        } catch (IOException e) {
            logger.error("Failed to delete project files", e);
            throw new RepositoryException("Failed to delete project files: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recursively delete a directory.
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            Files.list(directory).forEach(path -> {
                try {
                    deleteDirectory(path);
                } catch (IOException e) {
                    logger.warn("Failed to delete {}", path, e);
                }
            });
        }
        Files.delete(directory);
    }
}
