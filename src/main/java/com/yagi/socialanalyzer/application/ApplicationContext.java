package com.yagi.socialanalyzer.application;

import com.yagi.socialanalyzer.application.services.IDataCollectionService;
import com.yagi.socialanalyzer.application.services.DataCollectionService;
import com.yagi.socialanalyzer.domain.repositories.IProjectRepository;
import com.yagi.socialanalyzer.domain.repositories.IPostRepository;
import com.yagi.socialanalyzer.infrastructure.persistence.DatabaseManager;
import com.yagi.socialanalyzer.infrastructure.persistence.SQLiteProjectRepository;
import com.yagi.socialanalyzer.infrastructure.persistence.SQLitePostRepository;
import com.yagi.socialanalyzer.infrastructure.datasources.*;
import com.yagi.socialanalyzer.infrastructure.config.YamlConfigurationProvider;
import com.yagi.socialanalyzer.infrastructure.security.EncryptedCredentialStore;
import com.yagi.socialanalyzer.infrastructure.persistence.JsonFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Application context for dependency injection.
 * Implements a simple singleton pattern to manage application-wide services and repositories.
 */
public class ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);
    private static ApplicationContext instance;
    
    // Infrastructure
    private final DatabaseManager databaseManager;
    private final YamlConfigurationProvider configProvider;
    private final EncryptedCredentialStore credentialStore;
    private final JsonFileManager jsonFileManager;
    
    // Repositories
    private final IProjectRepository projectRepository;
    private final IPostRepository postRepository;
    
    // Data sources
    private final Map<String, IPlatformDataSource> dataSources;
    
    // Services
    private final IDataCollectionService dataCollectionService;
    
    private ApplicationContext() {
        logger.info("Initializing ApplicationContext...");
        
        try {
            // Initialize infrastructure
            this.databaseManager = new DatabaseManager("data/yagi_analyzer.db");
            this.databaseManager.initializeSchema();
            
            this.configProvider = new YamlConfigurationProvider();
            
            this.credentialStore = new EncryptedCredentialStore();
            
            this.jsonFileManager = new JsonFileManager();
            
            // Initialize repositories
            this.projectRepository = new SQLiteProjectRepository(databaseManager);
            this.postRepository = new SQLitePostRepository(databaseManager, jsonFileManager);
            
            // Initialize data sources (no credentials needed for stub implementations)
            this.dataSources = new HashMap<>();
            dataSources.put("twitter", new TwitterDataSource());
            dataSources.put("reddit", new RedditDataSource());
            dataSources.put("youtube", new YouTubeDataSource());
            dataSources.put("facebook", new FacebookSeleniumDataSource());
            dataSources.put("tiktok", new TikTokSeleniumDataSource());
            dataSources.put("google", new GoogleSeleniumDataSource());
            
            // Initialize services
            this.dataCollectionService = new DataCollectionService(
                projectRepository,
                postRepository,
                jsonFileManager,
                dataSources
            );
            
            logger.info("ApplicationContext initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize ApplicationContext", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }
    
    /**
     * Get the singleton instance of ApplicationContext.
     */
    public static synchronized ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }
    
    /**
     * Shutdown the application context and release resources.
     */
    public void shutdown() {
        logger.info("Shutting down ApplicationContext...");
        
        // Close data sources
        for (IPlatformDataSource dataSource : dataSources.values()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                logger.warn("Error closing data source: " + dataSource.getPlatformId(), e);
            }
        }
        
        // Close database
        try {
            databaseManager.close();
        } catch (Exception e) {
            logger.error("Error closing database", e);
        }
        
        logger.info("ApplicationContext shutdown complete");
    }
    
    // Getters for dependency injection
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public YamlConfigurationProvider getConfigProvider() {
        return configProvider;
    }
    
    public EncryptedCredentialStore getCredentialStore() {
        return credentialStore;
    }
    
    public JsonFileManager getJsonFileManager() {
        return jsonFileManager;
    }
    
    public IProjectRepository getProjectRepository() {
        return projectRepository;
    }
    
    public IPostRepository getPostRepository() {
        return postRepository;
    }
    
    public Map<String, IPlatformDataSource> getDataSources() {
        return dataSources;
    }
    
    public IDataCollectionService getDataCollectionService() {
        return dataCollectionService;
    }
}
