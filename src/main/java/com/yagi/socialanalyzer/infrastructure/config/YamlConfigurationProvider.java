package com.yagi.socialanalyzer.infrastructure.config;

import com.yagi.socialanalyzer.domain.entities.DamageCategory;
import com.yagi.socialanalyzer.domain.entities.DataSource;
import com.yagi.socialanalyzer.domain.exceptions.ConfigurationException;
import com.yagi.socialanalyzer.domain.valueobjects.AuthenticationType;
import com.yagi.socialanalyzer.domain.valueobjects.DataSourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration provider that loads YAML files for platforms and damage categories.
 */
public class YamlConfigurationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlConfigurationProvider.class);
    private static final String PLATFORMS_CONFIG = "config/platforms.yaml";
    private static final String DAMAGE_CATEGORIES_CONFIG = "config/damage_categories.yaml";
    
    private final Yaml yaml;
    
    public YamlConfigurationProvider() {
        this.yaml = new Yaml();
    }
    
    /**
     * Load platform configurations from YAML.
     */
    @SuppressWarnings("unchecked")
    public List<DataSource> loadPlatforms() throws ConfigurationException {
        logger.info("Loading platform configurations from {}", PLATFORMS_CONFIG);
        
        try (InputStream is = new FileInputStream(PLATFORMS_CONFIG)) {
            Map<String, Object> config = yaml.load(is);
            Map<String, Map<String, Object>> platforms = (Map<String, Map<String, Object>>) config.get("platforms");
            
            if (platforms == null || platforms.isEmpty()) {
                throw new ConfigurationException("No platforms defined in configuration");
            }
            
            List<DataSource> dataSources = new ArrayList<>();
            
            for (Map.Entry<String, Map<String, Object>> entry : platforms.entrySet()) {
                String id = entry.getKey();
                Map<String, Object> platformData = entry.getValue();
                
                DataSource dataSource = parseDataSource(id, platformData);
                dataSources.add(dataSource);
                logger.debug("Loaded platform: {}", id);
            }
            
            logger.info("Loaded {} platform configurations", dataSources.size());
            return dataSources;
            
        } catch (IOException e) {
            logger.error("Failed to load platforms configuration", e);
            throw new ConfigurationException("Failed to load platforms: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load damage categories from YAML.
     */
    @SuppressWarnings("unchecked")
    public List<DamageCategory> loadDamageCategories() throws ConfigurationException {
        logger.info("Loading damage categories from {}", DAMAGE_CATEGORIES_CONFIG);
        
        try (InputStream is = new FileInputStream(DAMAGE_CATEGORIES_CONFIG)) {
            Map<String, Object> config = yaml.load(is);
            Map<String, Map<String, Object>> categories = (Map<String, Map<String, Object>>) config.get("categories");
            
            if (categories == null || categories.isEmpty()) {
                throw new ConfigurationException("No damage categories defined in configuration");
            }
            
            List<DamageCategory> damageCategories = new ArrayList<>();
            
            for (Map.Entry<String, Map<String, Object>> entry : categories.entrySet()) {
                String id = entry.getKey();
                Map<String, Object> categoryData = entry.getValue();
                
                DamageCategory category = parseDamageCategory(id, categoryData);
                damageCategories.add(category);
                logger.debug("Loaded damage category: {}", id);
            }
            
            logger.info("Loaded {} damage categories", damageCategories.size());
            return damageCategories;
            
        } catch (IOException e) {
            logger.error("Failed to load damage categories configuration", e);
            throw new ConfigurationException("Failed to load damage categories: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse DataSource from YAML map.
     */
    @SuppressWarnings("unchecked")
    private DataSource parseDataSource(String id, Map<String, Object> data) {
        String name = (String) data.get("name");
        String typeStr = (String) data.get("type");
        String baseUrl = (String) data.get("base_url");
        String authTypeStr = (String) data.get("authentication_type");
        
        DataSourceType type = DataSourceType.valueOf(typeStr);
        AuthenticationType authType = AuthenticationType.valueOf(authTypeStr);
        
        // Parse rate limit
        int rateLimit = 0;
        Duration rateLimitWindow = Duration.ZERO;
        
        if (data.containsKey("rate_limit")) {
            Map<String, Object> rateLimitData = (Map<String, Object>) data.get("rate_limit");
            rateLimit = (Integer) rateLimitData.get("requests");
            String windowStr = (String) rateLimitData.get("window");
            rateLimitWindow = parseRateLimitWindow(windowStr);
        }
        
        boolean enabled = data.containsKey("enabled") ? (Boolean) data.get("enabled") : true;
        
        // Parse config params
        Map<String, String> configParams = new HashMap<>();
        if (data.containsKey("endpoint")) {
            configParams.put("endpoint", (String) data.get("endpoint"));
        }
        if (data.containsKey("selector")) {
            configParams.put("selector", (String) data.get("selector"));
        }
        if (data.containsKey("quota")) {
            Map<String, Object> quotaData = (Map<String, Object>) data.get("quota");
            configParams.put("quota_amount", String.valueOf(quotaData.get("amount")));
            configParams.put("quota_period", (String) quotaData.get("period"));
        }
        
        return new DataSource(id, name, type, baseUrl, authType, rateLimit, 
                             rateLimitWindow, enabled, configParams);
    }
    
    /**
     * Parse DamageCategory from YAML map.
     */
    @SuppressWarnings("unchecked")
    private DamageCategory parseDamageCategory(String id, Map<String, Object> data) {
        String name = (String) data.get("name");
        List<String> keywordsVi = (List<String>) data.get("keywords_vietnamese");
        List<String> keywordsEn = (List<String>) data.get("keywords_english");
        String description = (String) data.get("description");
        int priority = (Integer) data.get("priority");
        String color = (String) data.get("color");
        int version = data.containsKey("version") ? (Integer) data.get("version") : 1;
        
        return new DamageCategory(id, name, keywordsVi, keywordsEn, description, priority, color, version);
    }
    
    /**
     * Parse rate limit window string to Duration.
     */
    private Duration parseRateLimitWindow(String window) {
        if (window.endsWith("min")) {
            int minutes = Integer.parseInt(window.replace("min", "").trim());
            return Duration.ofMinutes(minutes);
        } else if (window.endsWith("hour")) {
            int hours = Integer.parseInt(window.replace("hour", "").trim());
            return Duration.ofHours(hours);
        } else if (window.endsWith("day")) {
            int days = Integer.parseInt(window.replace("day", "").trim());
            return Duration.ofDays(days);
        } else {
            throw new IllegalArgumentException("Invalid rate limit window format: " + window);
        }
    }
}
