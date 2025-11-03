package com.yagi.socialanalyzer.ui.controllers;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import com.yagi.socialanalyzer.domain.repositories.IProjectRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import com.yagi.socialanalyzer.application.ApplicationContext;
import com.yagi.socialanalyzer.infrastructure.security.EncryptedCredentialStore;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for project list view.
 */
public class ProjectListController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectListController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    
    @FXML private TableView<DisasterProject> projectTable;
    @FXML private TableColumn<DisasterProject, String> nameColumn;
    @FXML private TableColumn<DisasterProject, String> disasterColumn;
    @FXML private TableColumn<DisasterProject, String> regionColumn;
    @FXML private TableColumn<DisasterProject, String> dateRangeColumn;
    @FXML private TableColumn<DisasterProject, String> statusColumn;
    @FXML private TableColumn<DisasterProject, Long> postsColumn;
    @FXML private TableColumn<DisasterProject, String> createdColumn;
    
    @FXML private TextField searchField;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button startCollectionButton;
    @FXML private Button viewDataButton;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    
    private IProjectRepository projectRepository;
    private MainController mainController;
    private ObservableList<DisasterProject> projects;
    
    @FXML
    public void initialize() {
        logger.debug("Initializing project list controller");
        
        // Configure table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        disasterColumn.setCellValueFactory(new PropertyValueFactory<>("disasterName"));
        regionColumn.setCellValueFactory(new PropertyValueFactory<>("region"));
        
        dateRangeColumn.setCellValueFactory(cellData -> {
            DisasterProject project = cellData.getValue();
            String range = project.getStartDate().format(DATE_FORMATTER) + " - " +
                          project.getEndDate().format(DATE_FORMATTER);
            return new SimpleStringProperty(range);
        });
        
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus().name())
        );
        
        postsColumn.setCellValueFactory(new PropertyValueFactory<>("totalPostsCollected"));
        
        createdColumn.setCellValueFactory(cellData -> {
            String created = cellData.getValue().getCreatedAt()
                .atZone(java.time.ZoneId.systemDefault())
                .format(DATETIME_FORMATTER);
            return new SimpleStringProperty(created);
        });
        
        // Selection listener
        projectTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                boolean hasSelection = newSelection != null;
                editButton.setDisable(!hasSelection);
                deleteButton.setDisable(!hasSelection);
                startCollectionButton.setDisable(!hasSelection);
                viewDataButton.setDisable(!hasSelection);
            }
        );
        
        // Search filter
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterProjects(newVal));
        
        // Initialize empty list
        projects = FXCollections.observableArrayList();
        projectTable.setItems(projects);
    }
    
    /**
     * Set the project repository.
     */
    public void setProjectRepository(IProjectRepository repository) {
        this.projectRepository = repository;
        loadProjects();
    }
    
    /**
     * Set the main controller for navigation.
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    /**
     * Load all projects from repository.
     */
    public void loadProjects() {
        if (projectRepository == null) {
            logger.warn("Project repository not set");
            return;
        }
        
        try {
            statusLabel.setText("Loading projects...");
            List<DisasterProject> allProjects = projectRepository.findAll();
            projects.clear();
            projects.addAll(allProjects);
            
            updateCountLabel();
            statusLabel.setText("Ready");
            logger.info("Loaded {} projects", allProjects.size());
            
        } catch (RepositoryException e) {
            logger.error("Failed to load projects", e);
            showError("Failed to load projects: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleNewProject() {
        logger.info("Creating new project");
        
        try {
            // Load resource bundle for i18n
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages");
            
            // Load project config dialog
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ProjectConfigDialog.fxml"),
                bundle
            );
            DialogPane dialogPane = loader.load();
            ProjectConfigController controller = loader.getController();
            
            // Show dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("New Project");
            dialog.setDialogPane(dialogPane);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                DisasterProject project = controller.createProject();
                if (project != null) {
                    projectRepository.save(project);
                    projects.add(project);
                    updateCountLabel();
                    statusLabel.setText("Project created: " + project.getName());
                }
            }
            
        } catch (IOException | RepositoryException e) {
            logger.error("Failed to create project", e);
            showError("Failed to create project: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleEditProject() {
        DisasterProject selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        logger.info("Editing project: {}", selected.getId());
        
        try {
            // Load resource bundle for i18n
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages");
            
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ProjectConfigDialog.fxml"),
                bundle
            );
            DialogPane dialogPane = loader.load();
            ProjectConfigController controller = loader.getController();
            controller.loadProject(selected);
            
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Project");
            dialog.setDialogPane(dialogPane);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                DisasterProject updated = controller.createProject();
                if (updated != null) {
                    projectRepository.save(updated);
                    projectTable.refresh();
                    statusLabel.setText("Project updated: " + updated.getName());
                }
            }
            
        } catch (IOException | RepositoryException e) {
            logger.error("Failed to edit project", e);
            showError("Failed to edit project: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDeleteProject() {
        DisasterProject selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Project");
        confirmation.setHeaderText("Delete project: " + selected.getName() + "?");
        confirmation.setContentText("This will delete all collected posts. This action cannot be undone.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                projectRepository.delete(selected.getId());
                projects.remove(selected);
                updateCountLabel();
                statusLabel.setText("Project deleted: " + selected.getName());
                logger.info("Deleted project: {}", selected.getId());
                
            } catch (RepositoryException e) {
                logger.error("Failed to delete project", e);
                showError("Failed to delete project: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadProjects();
    }
    
    @FXML
    private void handleStartCollection() {
        DisasterProject selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null || mainController == null) {
            return;
        }
        
        logger.info("Starting collection for project: {}", selected.getName());
        mainController.loadDataCollectionView(selected);
    }

    @FXML
    private void handleManageCredentials() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages");
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/CredentialsDialog.fxml"),
                bundle
            );

            // Load the VBox content
            javafx.scene.layout.VBox content = loader.load();
            CredentialsController controller = loader.getController();

            // Inject credential store and available platforms
            ApplicationContext ctx = ApplicationContext.getInstance();
            EncryptedCredentialStore store = ctx.getCredentialStore();
            controller.setCredentialStore(store);
            controller.setPlatforms(List.copyOf(ctx.getDataSources().keySet()));

            // Create dialog with the content
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Manage Credentials");
            
            // Create a DialogPane and set the VBox as content
            DialogPane dialogPane = new DialogPane();
            dialogPane.setContent(content);
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);
            
            dialog.setDialogPane(dialogPane);
            dialog.showAndWait();

        } catch (Exception e) {
            logger.error("Failed to open credentials dialog", e);
            showError("Failed to open credentials dialog: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleViewData() {
        DisasterProject selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null || mainController == null) {
            return;
        }
        
        logger.info("Viewing data for project: {}", selected.getName());
        mainController.loadRawDataView(selected);
    }
    
    private void filterProjects(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            projectTable.setItems(projects);
            return;
        }
        
        String lower = searchText.toLowerCase();
        ObservableList<DisasterProject> filtered = projects.filtered(project ->
            project.getName().toLowerCase().contains(lower) ||
            project.getDisasterName().toLowerCase().contains(lower) ||
            project.getRegion().toLowerCase().contains(lower)
        );
        
        projectTable.setItems(filtered);
        updateCountLabel();
    }
    
    private void updateCountLabel() {
        int total = projects.size();
        int filtered = projectTable.getItems().size();
        
        if (total == filtered) {
            countLabel.setText(total + " project" + (total != 1 ? "s" : ""));
        } else {
            countLabel.setText(filtered + " of " + total + " projects");
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
