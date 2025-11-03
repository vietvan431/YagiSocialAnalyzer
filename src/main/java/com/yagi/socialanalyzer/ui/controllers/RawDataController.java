package com.yagi.socialanalyzer.ui.controllers;

import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import com.yagi.socialanalyzer.domain.repositories.IPostRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for raw data view.
 */
public class RawDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(RawDataController.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    
    @FXML private TableView<SocialMediaPost> postsTable;
    @FXML private TableColumn<SocialMediaPost, String> platformColumn;
    @FXML private TableColumn<SocialMediaPost, String> postIdColumn;
    @FXML private TableColumn<SocialMediaPost, String> authorColumn;
    @FXML private TableColumn<SocialMediaPost, String> contentColumn;
    @FXML private TableColumn<SocialMediaPost, String> publishedColumn;
    @FXML private TableColumn<SocialMediaPost, Integer> likesColumn;
    @FXML private TableColumn<SocialMediaPost, Integer> sharesColumn;
    @FXML private TableColumn<SocialMediaPost, Integer> commentsColumn;
    @FXML private TableColumn<SocialMediaPost, Long> viewsColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> platformFilter;
    @FXML private DatePicker dateFilter;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    
    private IPostRepository postRepository;
    private UUID projectId;
    private ObservableList<SocialMediaPost> allPosts;
    private ObservableList<SocialMediaPost> filteredPosts;
    
    @FXML
    public void initialize() {
        logger.debug("Initializing raw data controller");
        
        // Configure table columns
        platformColumn.setCellValueFactory(new PropertyValueFactory<>("platform"));
        postIdColumn.setCellValueFactory(new PropertyValueFactory<>("postId"));
        authorColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().author())
        );
        
        contentColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getContentPreview())
        );
        
        publishedColumn.setCellValueFactory(cellData -> {
            String published = cellData.getValue().getPublishedAt()
                .atZone(java.time.ZoneId.systemDefault())
                .format(DATETIME_FORMATTER);
            return new SimpleStringProperty(published);
        });
        
        likesColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getEngagementMetrics().likes()
            ).asObject()
        );
        
        sharesColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getEngagementMetrics().shares()
            ).asObject()
        );
        
        commentsColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getEngagementMetrics().comments()
            ).asObject()
        );
        
        viewsColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleLongProperty(
                cellData.getValue().getEngagementMetrics().views()
            ).asObject()
        );
        
        // Initialize lists
        allPosts = FXCollections.observableArrayList();
        filteredPosts = FXCollections.observableArrayList();
        postsTable.setItems(filteredPosts);
        
        // Filter listeners
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        platformFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
    }
    
    /**
     * Set post repository and project ID.
     */
    public void setRepository(IPostRepository repository, UUID projectId) {
        this.postRepository = repository;
        this.projectId = projectId;
        loadPosts();
    }
    
    /**
     * Set post repository (project ID must be set separately).
     */
    public void setRepository(IPostRepository repository) {
        this.postRepository = repository;
    }
    
    /**
     * Set the project to load data for.
     */
    public void setProject(com.yagi.socialanalyzer.domain.entities.DisasterProject project) {
        this.projectId = project.getId();
    }
    
    /**
     * Load all posts for the project.
     */
    public void loadPosts() {
        if (postRepository == null || projectId == null) {
            logger.warn("Repository or project ID not set");
            return;
        }
        
        try {
            statusLabel.setText("Loading posts...");
            
            // Load all posts for the project
            // Note: This is simplified - in production, you'd want pagination
            List<SocialMediaPost> posts = postRepository.findByProjectAndDateRange(
                projectId,
                LocalDate.now().minusYears(1),
                LocalDate.now().plusDays(1)
            );
            
            allPosts.clear();
            allPosts.addAll(posts);
            
            // Populate platform filter
            List<String> platforms = posts.stream()
                .map(SocialMediaPost::getPlatform)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            platformFilter.getItems().clear();
            platformFilter.getItems().add("All Platforms");
            platformFilter.getItems().addAll(platforms);
            platformFilter.setValue("All Platforms");
            
            applyFilters();
            statusLabel.setText("Ready");
            logger.info("Loaded {} posts", posts.size());
            
        } catch (RepositoryException e) {
            logger.error("Failed to load posts", e);
            showError("Failed to load posts: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        platformFilter.setValue("All Platforms");
        dateFilter.setValue(null);
        applyFilters();
    }
    
    @FXML
    private void handleExport() {
        // TODO: Implement export to CSV
        logger.info("Export requested");
        showInfo("Export functionality will be implemented in Phase 7");
    }
    
    private void applyFilters() {
        filteredPosts.clear();
        
        String searchText = searchField.getText();
        String selectedPlatform = platformFilter.getValue();
        LocalDate selectedDate = dateFilter.getValue();
        
        List<SocialMediaPost> filtered = allPosts.stream()
            .filter(post -> {
                // Search filter
                if (searchText != null && !searchText.trim().isEmpty()) {
                    String lower = searchText.toLowerCase();
                    if (!post.getContent().toLowerCase().contains(lower) &&
                        !post.author().toLowerCase().contains(lower)) {
                        return false;
                    }
                }
                
                // Platform filter
                if (selectedPlatform != null && !selectedPlatform.equals("All Platforms")) {
                    if (!post.getPlatform().equals(selectedPlatform)) {
                        return false;
                    }
                }
                
                // Date filter
                if (selectedDate != null) {
                    LocalDate postDate = post.getPublishedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                    if (!postDate.equals(selectedDate)) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        filteredPosts.addAll(filtered);
        updateCountLabel();
    }
    
    private void updateCountLabel() {
        int total = allPosts.size();
        int filtered = filteredPosts.size();
        
        if (total == filtered) {
            countLabel.setText(total + " post" + (total != 1 ? "s" : ""));
        } else {
            countLabel.setText(filtered + " of " + total + " posts");
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
