package com.yagi.socialanalyzer.ui.controllers;

import com.yagi.socialanalyzer.application.ApplicationContext;
import com.yagi.socialanalyzer.ui.tasks.DataCollectionTask;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Main controller for the primary application window.
 * Manages navigation and loads different views into the content area.
 */
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    @FXML private StackPane contentArea;
    @FXML private Label statusLabel;
    @FXML private Label connectionLabel;
    
    private ApplicationContext appContext;
    private ResourceBundle bundle;
    
    @FXML
    public void initialize() {
        logger.info("Main controller initialized");
        appContext = ApplicationContext.getInstance();
        
        // Load resource bundle for i18n
        bundle = ResourceBundle.getBundle("i18n.messages");
        
        // Load project list view as default screen
        handleViewProjects();
        
        updateStatus("Ready");
    }
    
    /**
     * Load a view into the content area.
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath), bundle);
            Parent view = loader.load();
            
            // Set repository dependencies if the controller supports it
            Object controller = loader.getController();
            if (controller instanceof ProjectListController) {
                ((ProjectListController) controller).setProjectRepository(
                    appContext.getProjectRepository()
                );
                ((ProjectListController) controller).setMainController(this);
            } else if (controller instanceof RawDataController) {
                ((RawDataController) controller).setRepository(
                    appContext.getPostRepository()
                );
            }
            
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
            logger.error("Failed to load view: " + fxmlPath, e);
            showError("Failed to load view", e.getMessage());
        }
    }
    
    /**
     * Load data collection view for a specific project.
     */
    public void loadDataCollectionView(com.yagi.socialanalyzer.domain.entities.DisasterProject project) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/DataCollectionView.fxml"), 
                bundle
            );
            Parent view = loader.load();
            
            DataCollectionController controller = loader.getController();
            controller.setProject(project);
            
            // Inject dependencies
            DataCollectionTask task = new DataCollectionTask(project.getId());
            task.setCollectionService(appContext.getDataCollectionService());
            controller.setCollectionTask(task);
            
            contentArea.getChildren().setAll(view);
            updateStatus("Data collection view loaded");
            
        } catch (Exception e) {
            logger.error("Failed to load data collection view", e);
            showError("Failed to load data collection view", e.getMessage());
        }
    }
    
    /**
     * Load raw data view for a specific project.
     */
    public void loadRawDataView(com.yagi.socialanalyzer.domain.entities.DisasterProject project) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/RawDataView.fxml"), 
                bundle
            );
            Parent view = loader.load();
            
            RawDataController controller = loader.getController();
            controller.setRepository(appContext.getPostRepository());
            controller.setProject(project);
            controller.loadPosts();
            
            contentArea.getChildren().setAll(view);
            updateStatus("Raw data view loaded");
            
        } catch (Exception e) {
            logger.error("Failed to load raw data view", e);
            showError("Failed to load raw data view", e.getMessage());
        }
    }
    
    // File menu handlers
    
    @FXML
    private void handleNewProject() {
        logger.info("New project requested");
        // This will be handled by ProjectListController
        handleViewProjects();
    }
    
    @FXML
    private void handleOpenProject() {
        logger.info("Open project requested");
        updateStatus("Opening project...");
        // To be implemented in Phase 3
    }
    
    @FXML
    private void handleSaveProject() {
        logger.info("Save project requested");
        updateStatus("Saving project...");
        // To be implemented in Phase 3
    }
    
    @FXML
    private void handleImport() {
        logger.info("Import requested");
        updateStatus("Importing data...");
        // To be implemented
    }
    
    @FXML
    private void handleExport() {
        logger.info("Export requested");
        updateStatus("Exporting data...");
        // To be implemented in Phase 7
    }
    
    @FXML
    private void handleExit() {
        logger.info("Exit requested");
        System.exit(0);
    }
    
    // Edit menu handlers
    
    @FXML
    private void handlePreferences() {
        logger.info("Preferences requested");
        updateStatus("Opening preferences...");
        // To be implemented
    }
    
    // View menu handlers
    
    @FXML
    private void handleViewProjects() {
        logger.info("View projects requested");
        updateStatus("Loading projects...");
        loadView("/fxml/ProjectListView.fxml");
    }
    
    @FXML
    private void handleViewCollection() {
        logger.info("View collection requested");
        updateStatus("Loading collection view...");
        // To be implemented in Phase 3
    }
    
    @FXML
    private void handleViewAnalysis() {
        logger.info("View analysis requested");
        updateStatus("Loading analysis view...");
        // To be implemented in Phase 4+
    }
    
    // Settings menu handlers
    
    @FXML
    private void handlePlatformSettings() {
        logger.info("Platform settings requested");
        updateStatus("Opening platform settings...");
        // To be implemented
    }
    
    @FXML
    private void handleCredentialSettings() {
        logger.info("Credential settings requested");
        updateStatus("Opening credential settings...");
        // To be implemented
    }
    
    // Help menu handlers
    
    @FXML
    private void handleDocumentation() {
        logger.info("Documentation requested");
        updateStatus("Opening documentation...");
        // To be implemented
    }
    
    @FXML
    private void handleAbout() {
        logger.info("About requested");
        updateStatus("Showing about dialog...");
        // To be implemented
    }
    
    // Utility methods
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    public void setConnectionStatus(String status) {
        if (connectionLabel != null) {
            connectionLabel.setText(status);
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
