package com.yagi.socialanalyzer.domain.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Analysis Configuration entity for project-specific analysis settings.
 */
public class AnalysisConfiguration {
    
    private final UUID id;
    private final UUID projectId;
    private String sentimentBackend;
    private double sentimentThreshold;
    private String pythonApiUrl;
    private boolean damageClassificationEnabled;
    private int minimumKeywordMatches;
    private Map<String, Object> customSettings;
    
    /**
     * Create a new analysis configuration.
     */
    public AnalysisConfiguration(UUID id, UUID projectId, String sentimentBackend,
                                double sentimentThreshold, String pythonApiUrl,
                                boolean damageClassificationEnabled, int minimumKeywordMatches,
                                Map<String, Object> customSettings) {
        this.id = Objects.requireNonNull(id, "Configuration ID cannot be null");
        this.projectId = Objects.requireNonNull(projectId, "Project ID cannot be null");
        this.customSettings = new HashMap<>();
        
        setSentimentBackend(sentimentBackend);
        setSentimentThreshold(sentimentThreshold);
        setPythonApiUrl(pythonApiUrl);
        this.damageClassificationEnabled = damageClassificationEnabled;
        setMinimumKeywordMatches(minimumKeywordMatches);
        setCustomSettings(customSettings);
    }
    
    // Setters with validation
    
    public void setSentimentBackend(String sentimentBackend) {
        Objects.requireNonNull(sentimentBackend, "Sentiment backend cannot be null");
        if (!sentimentBackend.equals("java_lexicon") && !sentimentBackend.equals("python_api")) {
            throw new IllegalArgumentException(
                "Sentiment backend must be 'java_lexicon' or 'python_api'");
        }
        this.sentimentBackend = sentimentBackend;
    }
    
    public void setSentimentThreshold(double sentimentThreshold) {
        if (sentimentThreshold < 0.0 || sentimentThreshold > 1.0) {
            throw new IllegalArgumentException("Sentiment threshold must be between 0.0 and 1.0");
        }
        this.sentimentThreshold = sentimentThreshold;
    }
    
    public void setPythonApiUrl(String pythonApiUrl) {
        // Can be null if using java_lexicon backend
        this.pythonApiUrl = pythonApiUrl;
    }
    
    public void setDamageClassificationEnabled(boolean damageClassificationEnabled) {
        this.damageClassificationEnabled = damageClassificationEnabled;
    }
    
    public void setMinimumKeywordMatches(int minimumKeywordMatches) {
        if (minimumKeywordMatches < 0) {
            throw new IllegalArgumentException("Minimum keyword matches cannot be negative");
        }
        this.minimumKeywordMatches = minimumKeywordMatches;
    }
    
    public void setCustomSettings(Map<String, Object> customSettings) {
        Objects.requireNonNull(customSettings, "Custom settings cannot be null");
        this.customSettings = new HashMap<>(customSettings);
    }
    
    public void setCustomSetting(String key, Object value) {
        Objects.requireNonNull(key, "Setting key cannot be null");
        Objects.requireNonNull(value, "Setting value cannot be null");
        this.customSettings.put(key, value);
    }
    
    public void removeCustomSetting(String key) {
        this.customSettings.remove(key);
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getProjectId() {
        return projectId;
    }
    
    public String getSentimentBackend() {
        return sentimentBackend;
    }
    
    public double getSentimentThreshold() {
        return sentimentThreshold;
    }
    
    public String getPythonApiUrl() {
        return pythonApiUrl;
    }
    
    public boolean isDamageClassificationEnabled() {
        return damageClassificationEnabled;
    }
    
    public int getMinimumKeywordMatches() {
        return minimumKeywordMatches;
    }
    
    public Map<String, Object> getCustomSettings() {
        return new HashMap<>(customSettings);
    }
    
    public Object getCustomSetting(String key) {
        return customSettings.get(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisConfiguration that = (AnalysisConfiguration) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "AnalysisConfiguration{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", sentimentBackend='" + sentimentBackend + '\'' +
                ", damageClassificationEnabled=" + damageClassificationEnabled +
                '}';
    }
}
