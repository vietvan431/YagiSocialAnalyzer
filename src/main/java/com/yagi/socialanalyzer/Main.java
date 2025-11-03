package com.yagi.socialanalyzer;

import com.yagi.socialanalyzer.application.ApplicationContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Main JavaFX application entry point for YagiSocialAnalyzer.
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String MAIN_WINDOW_FXML = "/fxml/MainWindow.fxml";
    private static final String STYLESHEET = "/css/application.css";
    private static final String APP_TITLE = "Yagi Social Media Analyzer";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    
    private ApplicationContext appContext;
    
    @Override
    public void init() {
        logger.info("Initializing application...");
        // Initialize ApplicationContext in init() phase (before JavaFX start)
        appContext = ApplicationContext.getInstance();
    }
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting YagiSocialAnalyzer application...");
            
            // Load resource bundle for internationalization
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", Locale.getDefault());
            
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_WINDOW_FXML), bundle);
            Parent root = loader.load();
            
            // Create scene
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
            
            // Configure stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(600);
            
            // Show window
            primaryStage.show();
            logger.info("Application started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Application shutting down...");
        if (appContext != null) {
            appContext.shutdown();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
