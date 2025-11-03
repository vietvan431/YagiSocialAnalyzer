package com.yagi.socialanalyzer.domain.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Damage Category entity representing a type of disaster damage with multilingual keywords.
 */
public class DamageCategory {
    
    private final String id;
    private String name;
    private List<String> keywordsVietnamese;
    private List<String> keywordsEnglish;
    private String description;
    private int priority;
    private String color;
    private int version;
    
    /**
     * Create a new damage category.
     */
    public DamageCategory(String id, String name, List<String> keywordsVietnamese,
                         List<String> keywordsEnglish, String description,
                         int priority, String color, int version) {
        this.id = Objects.requireNonNull(id, "Category ID cannot be null");
        this.version = version;
        
        setName(name);
        setKeywordsVietnamese(keywordsVietnamese);
        setKeywordsEnglish(keywordsEnglish);
        setDescription(description);
        setPriority(priority);
        setColor(color);
    }
    
    // Setters with validation
    
    public void setName(String name) {
        Objects.requireNonNull(name, "Category name cannot be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        this.name = trimmed;
    }
    
    public void setKeywordsVietnamese(List<String> keywords) {
        Objects.requireNonNull(keywords, "Vietnamese keywords cannot be null");
        this.keywordsVietnamese = new ArrayList<>(keywords);
    }
    
    public void setKeywordsEnglish(List<String> keywords) {
        Objects.requireNonNull(keywords, "English keywords cannot be null");
        this.keywordsEnglish = new ArrayList<>(keywords);
    }
    
    public void setDescription(String description) {
        this.description = description; // Can be null or empty
    }
    
    public void setPriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 1 and 10");
        }
        this.priority = priority;
    }
    
    public void setColor(String color) {
        Objects.requireNonNull(color, "Color cannot be null");
        String trimmed = color.trim();
        if (!trimmed.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Color must be a valid hex color (e.g., #FF5733)");
        }
        this.color = trimmed.toUpperCase();
    }
    
    public void incrementVersion() {
        this.version++;
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getKeywordsVietnamese() {
        return new ArrayList<>(keywordsVietnamese);
    }
    
    public List<String> getKeywordsEnglish() {
        return new ArrayList<>(keywordsEnglish);
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public String getColor() {
        return color;
    }
    
    public int getVersion() {
        return version;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DamageCategory that = (DamageCategory) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "DamageCategory{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", version=" + version +
                '}';
    }
}
