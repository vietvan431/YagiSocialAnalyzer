package com.yagi.socialanalyzer.ui.controllers;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.valueobjects.ProjectStatus;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Controller for project configuration dialog.
 */
public class ProjectConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectConfigController.class);
    private static final int MAX_DATE_RANGE_DAYS = 365;
    
    @FXML private TextField projectNameField;
    @FXML private TextField disasterNameField;
    @FXML private TextField regionField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea keywordsArea;
    @FXML private CheckBox twitterCheckBox;
    @FXML private CheckBox redditCheckBox;
    @FXML private CheckBox youtubeCheckBox;
    @FXML private CheckBox facebookCheckBox;
    @FXML private CheckBox tiktokCheckBox;
    @FXML private CheckBox googleCheckBox;
    
    @FXML private Label nameErrorLabel;
    @FXML private Label disasterErrorLabel;
    @FXML private Label regionErrorLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Label keywordsErrorLabel;
    @FXML private Label sourcesErrorLabel;
    
    private DisasterProject existingProject;
    private DialogPane dialogPane;
    
    @FXML
    public void initialize() {
        logger.debug("Initializing project config controller");
        
        // Get the dialog pane (will be set after FXML loads)
        javafx.application.Platform.runLater(() -> {
            dialogPane = (DialogPane) projectNameField.getScene().getRoot();
            if (dialogPane != null) {
                // Disable OK button initially and on invalid input
                Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
                if (okButton != null) {
                    okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                        if (!validateAll()) {
                            event.consume(); // Prevent dialog from closing
                        }
                    });
                }
            }
        });
        
        // Set default date range (last 30 days)
        LocalDate today = LocalDate.now();
        endDatePicker.setValue(today);
        startDatePicker.setValue(today.minusDays(30));
        
        // Add validation listeners
        projectNameField.textProperty().addListener((obs, old, val) -> validateProjectName());
        disasterNameField.textProperty().addListener((obs, old, val) -> validateDisasterName());
        regionField.textProperty().addListener((obs, old, val) -> validateRegion());
        startDatePicker.valueProperty().addListener((obs, old, val) -> validateDateRange());
        endDatePicker.valueProperty().addListener((obs, old, val) -> validateDateRange());
        keywordsArea.textProperty().addListener((obs, old, val) -> validateKeywords());
    }
    
    /**
     * Load existing project for editing.
     */
    public void loadProject(DisasterProject project) {
        this.existingProject = project;
        
        projectNameField.setText(project.getName());
        disasterNameField.setText(project.getDisasterName());
        regionField.setText(project.getRegion());
        startDatePicker.setValue(project.getStartDate());
        endDatePicker.setValue(project.getEndDate());
        
        // Load keywords
        String keywords = String.join("\n", project.getKeywords());
        keywordsArea.setText(keywords);
        
        // Load data sources
        Set<String> sources = project.getDataSources();
        twitterCheckBox.setSelected(sources.contains("twitter"));
        redditCheckBox.setSelected(sources.contains("reddit"));
        youtubeCheckBox.setSelected(sources.contains("youtube"));
        facebookCheckBox.setSelected(sources.contains("facebook"));
        tiktokCheckBox.setSelected(sources.contains("tiktok"));
        googleCheckBox.setSelected(sources.contains("google"));
    }
    
    /**
     * Validate all fields and return true if valid.
     */
    public boolean validateAll() {
        boolean valid = true;
        
        valid &= validateProjectName();
        valid &= validateDisasterName();
        valid &= validateRegion();
        valid &= validateDateRange();
        valid &= validateKeywords();
        valid &= validateDataSources();
        
        return valid;
    }
    
    /**
     * Create project from form data.
     */
    public DisasterProject createProject() {
        if (!validateAll()) {
            return null;
        }
        
        UUID id = existingProject != null ? existingProject.getId() : UUID.randomUUID();
        String name = projectNameField.getText().trim();
        String disasterName = disasterNameField.getText().trim();
        String region = regionField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        // Parse keywords
        Set<String> keywords = parseKeywords();
        
        // Get selected data sources
        Set<String> dataSources = getSelectedDataSources();
        
        if (existingProject != null) {
            // Update existing project
            existingProject.setName(name);
            existingProject.setDisasterName(disasterName);
            existingProject.setRegion(region);
            existingProject.setDateRange(startDate, endDate);
            existingProject.setKeywords(keywords);
            existingProject.setDataSources(dataSources);
            return existingProject;
        } else {
            // Create new project
            return new DisasterProject(id, name, disasterName, region, startDate, endDate,
                keywords, dataSources);
        }
    }
    
    private boolean validateProjectName() {
        String name = projectNameField.getText().trim();
        
        if (name.isEmpty()) {
            showError(nameErrorLabel, "Project name is required");
            return false;
        }
        
        if (name.length() > 200) {
            showError(nameErrorLabel, "Project name cannot exceed 200 characters");
            return false;
        }
        
        hideError(nameErrorLabel);
        return true;
    }
    
    private boolean validateDisasterName() {
        String name = disasterNameField.getText().trim();
        
        if (name.isEmpty()) {
            showError(disasterErrorLabel, "Disaster name is required");
            return false;
        }
        
        if (name.length() > 200) {
            showError(disasterErrorLabel, "Disaster name cannot exceed 200 characters");
            return false;
        }
        
        hideError(disasterErrorLabel);
        return true;
    }
    
    private boolean validateRegion() {
        String region = regionField.getText().trim();
        
        if (region.isEmpty()) {
            showError(regionErrorLabel, "Region is required");
            return false;
        }
        
        if (region.length() > 100) {
            showError(regionErrorLabel, "Region cannot exceed 100 characters");
            return false;
        }
        
        hideError(regionErrorLabel);
        return true;
    }
    
    private boolean validateDateRange() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showError(dateErrorLabel, "Both start and end dates are required");
            return false;
        }
        
        if (startDate.isAfter(endDate)) {
            showError(dateErrorLabel, "Start date must be before or equal to end date");
            return false;
        }
        
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > MAX_DATE_RANGE_DAYS) {
            showError(dateErrorLabel, "Date range cannot exceed 365 days");
            return false;
        }
        
        hideError(dateErrorLabel);
        return true;
    }
    
    private boolean validateKeywords() {
        Set<String> keywords = parseKeywords();
        
        if (keywords.isEmpty()) {
            showError(keywordsErrorLabel, "At least one keyword is required");
            return false;
        }
        
        if (keywords.size() > 50) {
            showError(keywordsErrorLabel, "Cannot exceed 50 keywords");
            return false;
        }
        
        // Check individual keyword length
        for (String keyword : keywords) {
            if (keyword.length() > 100) {
                showError(keywordsErrorLabel, "Keyword too long: " + keyword.substring(0, 20) + "...");
                return false;
            }
        }
        
        hideError(keywordsErrorLabel);
        return true;
    }
    
    private boolean validateDataSources() {
        if (getSelectedDataSources().isEmpty()) {
            showError(sourcesErrorLabel, "At least one data source must be selected");
            return false;
        }
        
        hideError(sourcesErrorLabel);
        return true;
    }
    
    private Set<String> parseKeywords() {
        String text = keywordsArea.getText();
        if (text == null || text.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> keywords = new HashSet<>();
        String[] lines = text.split("[,\n]");
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                keywords.add(trimmed);
            }
        }
        
        return keywords;
    }
    
    private Set<String> getSelectedDataSources() {
        Set<String> sources = new HashSet<>();
        
        if (twitterCheckBox.isSelected()) sources.add("twitter");
        if (redditCheckBox.isSelected()) sources.add("reddit");
        if (youtubeCheckBox.isSelected()) sources.add("youtube");
        if (facebookCheckBox.isSelected()) sources.add("facebook");
        if (tiktokCheckBox.isSelected()) sources.add("tiktok");
        if (googleCheckBox.isSelected()) sources.add("google");
        
        return sources;
    }
    
    private void showError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }
    
    private void hideError(Label label) {
        label.setManaged(false);
        label.setVisible(false);
    }
}
