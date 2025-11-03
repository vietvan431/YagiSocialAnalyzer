package com.yagi.socialanalyzer.ui.controllers;

import com.yagi.socialanalyzer.application.ApplicationContext;
import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.ui.tasks.DataCollectionTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for data collection view.
 */
public class DataCollectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataCollectionController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    @FXML private Label projectNameLabel;
    @FXML private Label projectInfoLabel;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label overallStatusLabel;
    @FXML private VBox platformProgressContainer;
    @FXML private Label totalPostsLabel;
    @FXML private Label elapsedTimeLabel;
    @FXML private Label collectionRateLabel;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resumeButton;
    @FXML private Button stopButton;
    
    private DisasterProject project;
    private DataCollectionTask collectionTask;
    private Map<String, ProgressBar> platformProgressBars;
    private Map<String, Label> platformStatusLabels;
    private Instant startTime;
    private long totalPostsCollected;
    
    @FXML
    public void initialize() {
        logger.debug("Initializing data collection controller");
        platformProgressBars = new HashMap<>();
        platformStatusLabels = new HashMap<>();
        totalPostsCollected = 0;
    }
    
    /**
     * Set the project for collection.
     */
    public void setProject(DisasterProject project) {
        this.project = project;
        
        // Update header
        projectNameLabel.setText(project.getName());
        String info = String.format("%s | %s | %s - %s",
            project.getDisasterName(),
            project.getRegion(),
            project.getStartDate().format(DATE_FORMATTER),
            project.getEndDate().format(DATE_FORMATTER)
        );
        projectInfoLabel.setText(info);
        
        // Create progress bars for each platform
        platformProgressContainer.getChildren().clear();
        for (String platformId : project.getDataSources()) {
            VBox platformBox = createPlatformProgressBox(platformId);
            platformProgressContainer.getChildren().add(platformBox);
        }
        
        // Update stats
        totalPostsLabel.setText(String.valueOf(project.getTotalPostsCollected()));
    }
    
    /**
     * Set the collection task to use for data collection.
     */
    public void setCollectionTask(DataCollectionTask task) {
        this.collectionTask = task;
    }
    
    @FXML
    private void handleStart() {
        if (project == null) {
            showError("No project selected");
            return;
        }
        
        logger.info("Starting data collection for project: {}", project.getId());
        
        // Create and configure task
        collectionTask = new DataCollectionTask(project.getId());
        
        // Inject collection service from ApplicationContext
        ApplicationContext ctx = ApplicationContext.getInstance();
        collectionTask.setCollectionService(ctx.getDataCollectionService());
        
        // Bind progress
        overallProgressBar.progressProperty().bind(collectionTask.progressProperty());
        overallStatusLabel.textProperty().bind(collectionTask.messageProperty());
        
        // Handle task completion
        collectionTask.setOnSucceeded(event -> {
            Integer collected = collectionTask.getValue();
            logger.info("Collection completed: {} posts", collected);
            
            // Unbind before setting text manually
            overallStatusLabel.textProperty().unbind();
            overallProgressBar.progressProperty().unbind();
            
            overallStatusLabel.setText("Collection completed: " + collected + " posts");
            overallProgressBar.setProgress(1.0);
            totalPostsCollected = collected;
            totalPostsLabel.setText(String.valueOf(collected));
            refreshProjectData(); // Refresh to get updated status and count
            resetButtons();
        });
        
        collectionTask.setOnFailed(event -> {
            Throwable error = collectionTask.getException();
            logger.error("Collection failed", error);
            
            // Unbind before setting text manually
            overallStatusLabel.textProperty().unbind();
            overallProgressBar.progressProperty().unbind();
            
            // T060: Better error handling with specific error types
            String errorMessage = "Collection failed: ";
            String errorDetails = error.getMessage();
            
            if (errorDetails != null) {
                if (errorDetails.contains("rate limit") || errorDetails.contains("429")) {
                    errorMessage = "Rate limit exceeded. Please wait before retrying.";
                    showErrorWithRetry("Rate Limit Error", errorMessage, 
                        "The platform has rate-limited your requests. Wait a few minutes and try again.");
                } else if (errorDetails.contains("authentication") || errorDetails.contains("401") || 
                          errorDetails.contains("403")) {
                    errorMessage = "Authentication failed. Please check your credentials.";
                    showErrorWithAction("Authentication Error", errorMessage,
                        "Check your platform credentials in Settings → Manage Credentials");
                } else if (errorDetails.contains("network") || errorDetails.contains("connection")) {
                    errorMessage = "Network error. Please check your internet connection.";
                    showErrorWithRetry("Network Error", errorMessage, errorDetails);
                } else {
                    showError(errorMessage + errorDetails);
                }
            } else {
                showError(errorMessage + "Unknown error occurred");
            }
            
            resetButtons();
        });
        
        collectionTask.setOnCancelled(event -> {
            logger.info("Collection cancelled");
            overallStatusLabel.textProperty().unbind();
            overallProgressBar.progressProperty().unbind();
            overallStatusLabel.setText("Collection cancelled");
            refreshProjectData(); // Refresh to get updated status
            resetButtons();
        });
        
        // Update button states
        startButton.setDisable(true);
        pauseButton.setDisable(false);
        resumeButton.setDisable(true);
        stopButton.setDisable(false);
        
        // Start task
        startTime = Instant.now();
        Thread thread = new Thread(collectionTask);
        thread.setDaemon(true);
        thread.start();
        
        // Start timer for elapsed time
        startElapsedTimer();
    }
    
    @FXML
    private void handlePause() {
        if (collectionTask != null) {
            logger.info("Pausing collection");
            collectionTask.pause();
            
            pauseButton.setDisable(true);
            resumeButton.setDisable(false);
            overallStatusLabel.setText("Collection paused");
        }
    }
    
    @FXML
    private void handleResume() {
        if (collectionTask != null) {
            logger.info("Resuming collection");
            collectionTask.resume();
            
            pauseButton.setDisable(false);
            resumeButton.setDisable(true);
            overallStatusLabel.setText("Collection resumed");
        }
    }
    
    @FXML
    private void handleStop() {
        if (collectionTask != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Stop Collection");
            confirmation.setHeaderText("Stop data collection?");
            confirmation.setContentText("Progress will be saved but collection will stop.");
            
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    logger.info("Stopping collection");
                    collectionTask.cancel();
                    resetButtons();
                }
            });
        }
    }
    
    private VBox createPlatformProgressBox(String platformId) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));
        box.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 3; -fx-background-radius: 3;");
        
        Label nameLabel = new Label(platformId.toUpperCase());
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        
        Label statusLabel = new Label("Waiting...");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        
        box.getChildren().addAll(nameLabel, progressBar, statusLabel);
        
        platformProgressBars.put(platformId, progressBar);
        platformStatusLabels.put(platformId, statusLabel);
        
        return box;
    }
    
    private void startElapsedTimer() {
        Thread timerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && 
                   collectionTask != null && 
                   !collectionTask.isDone()) {
                try {
                    Thread.sleep(1000);
                    
                    Platform.runLater(() -> {
                        if (startTime != null) {
                            Duration elapsed = Duration.between(startTime, Instant.now());
                            long hours = elapsed.toHours();
                            long minutes = elapsed.toMinutesPart();
                            long seconds = elapsed.toSecondsPart();
                            
                            elapsedTimeLabel.setText(
                                String.format("%02d:%02d:%02d", hours, minutes, seconds)
                            );
                            
                            // Calculate collection rate
                            long totalSeconds = elapsed.getSeconds();
                            if (totalSeconds > 0) {
                                double rate = (totalPostsCollected * 60.0) / totalSeconds;
                                collectionRateLabel.setText(
                                    String.format("%.1f posts/min", rate)
                                );
                            }
                            
                            // T062: Refresh project data every 5 seconds to update post count
                            if (totalSeconds % 5 == 0) {
                                refreshProjectData();
                            }
                        }
                    });
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }
    
    private void resetButtons() {
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        resumeButton.setDisable(true);
        stopButton.setDisable(true);
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show error with retry option (T060).
     */
    private void showErrorWithRetry(String title, String message, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(details);
        
        ButtonType retryButton = new ButtonType("Retry");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(retryButton, closeButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == retryButton) {
                // Retry collection
                handleStart();
            }
        });
    }
    
    /**
     * Show error with action suggestion (T060).
     */
    private void showErrorWithAction(String title, String message, String action) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(action);
        alert.showAndWait();
    }
    
    /**
     * Refresh project data from repository (T062).
     */
    private void refreshProjectData() {
        if (project != null) {
            try {
                ApplicationContext ctx = ApplicationContext.getInstance();
                ctx.getProjectRepository().findById(project.getId()).ifPresent(updatedProject -> {
                    this.project = updatedProject;
                    totalPostsLabel.setText(String.valueOf(updatedProject.getTotalPostsCollected()));
                    logger.debug("Refreshed project data: {} posts collected", 
                        updatedProject.getTotalPostsCollected());
                });
            } catch (Exception e) {
                logger.warn("Failed to refresh project data", e);
            }
        }
    }
    
    /**
     * Update platform progress (called from task).
     */
    public void updatePlatformProgress(String platformId, double progress, String status) {
        Platform.runLater(() -> {
            ProgressBar progressBar = platformProgressBars.get(platformId);
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
            
            Label statusLabel = platformStatusLabels.get(platformId);
            if (statusLabel != null) {
                statusLabel.setText(status);
            }
        });
    }
}
