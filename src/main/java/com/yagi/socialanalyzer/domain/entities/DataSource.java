package com.yagi.socialanalyzer.domain.entities;

import com.yagi.socialanalyzer.domain.valueobjects.AuthenticationType;
import com.yagi.socialanalyzer.domain.valueobjects.DataSourceType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data Source entity representing a social media platform configuration.
 */
public class DataSource {
    
    private final String id;
    private String name;
    private DataSourceType type;
    private String baseUrl;
    private AuthenticationType authenticationType;
    private int rateLimit;
    private Duration rateLimitWindow;
    private boolean enabled;
    private Map<String, String> configParams;
    
    /**
     * Create a new data source.
     */
    public DataSource(String id, String name, DataSourceType type, String baseUrl,
                     AuthenticationType authenticationType, int rateLimit,
                     Duration rateLimitWindow, boolean enabled, Map<String, String> configParams) {
        this.id = Objects.requireNonNull(id, "Data source ID cannot be null");
        this.configParams = new HashMap<>();
        
        setName(name);
        setType(type);
        setBaseUrl(baseUrl);
        setAuthenticationType(authenticationType);
        setRateLimit(rateLimit, rateLimitWindow);
        this.enabled = enabled;
        setConfigParams(configParams);
    }
    
    // Setters with validation
    
    public void setName(String name) {
        Objects.requireNonNull(name, "Data source name cannot be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Data source name cannot be empty");
        }
        this.name = trimmed;
    }
    
    public void setType(DataSourceType type) {
        this.type = Objects.requireNonNull(type, "Data source type cannot be null");
    }
    
    public void setBaseUrl(String baseUrl) {
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be empty");
        }
        this.baseUrl = trimmed;
    }
    
    public void setAuthenticationType(AuthenticationType authenticationType) {
        this.authenticationType = Objects.requireNonNull(authenticationType, 
            "Authentication type cannot be null");
    }
    
    public void setRateLimit(int rateLimit, Duration rateLimitWindow) {
        if (rateLimit <= 0) {
            throw new IllegalArgumentException("Rate limit must be positive");
        }
        Objects.requireNonNull(rateLimitWindow, "Rate limit window cannot be null");
        if (rateLimitWindow.isNegative() || rateLimitWindow.isZero()) {
            throw new IllegalArgumentException("Rate limit window must be positive");
        }
        this.rateLimit = rateLimit;
        this.rateLimitWindow = rateLimitWindow;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setConfigParams(Map<String, String> configParams) {
        Objects.requireNonNull(configParams, "Config params cannot be null");
        this.configParams = new HashMap<>(configParams);
    }
    
    public void setConfigParam(String key, String value) {
        Objects.requireNonNull(key, "Config param key cannot be null");
        Objects.requireNonNull(value, "Config param value cannot be null");
        this.configParams.put(key, value);
    }
    
    public void removeConfigParam(String key) {
        this.configParams.remove(key);
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public DataSourceType getType() {
        return type;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }
    
    public int getRateLimit() {
        return rateLimit;
    }
    
    public Duration getRateLimitWindow() {
        return rateLimitWindow;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Map<String, String> getConfigParams() {
        return new HashMap<>(configParams);
    }
    
    public String getConfigParam(String key) {
        return configParams.get(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSource that = (DataSource) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "DataSource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", enabled=" + enabled +
                '}';
    }
}
