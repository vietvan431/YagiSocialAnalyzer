package com.yagi.socialanalyzer.ui.controllers;

import com.yagi.socialanalyzer.domain.exceptions.SecurityException;
import com.yagi.socialanalyzer.infrastructure.security.EncryptedCredentialStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Controller for credentials dialog. Uses EncryptedCredentialStore to persist credentials.
 */
public class CredentialsController {

    private static final Logger logger = LoggerFactory.getLogger(CredentialsController.class);

    @FXML private ComboBox<String> platformComboBox;
    @FXML private TextField apiKeyField;
    @FXML private TextField apiSecretField;
    @FXML private TextField accessTokenField;
    @FXML private TextField accessSecretField;
    @FXML private Button testButton;
    @FXML private Button saveButton;

    private EncryptedCredentialStore credentialStore;

    public void initialize() {
        // basic initialization
    }

    /**
     * Provide a credential store instance (from ApplicationContext).
     */
    public void setCredentialStore(EncryptedCredentialStore store) {
        this.credentialStore = Objects.requireNonNull(store);
    }

    /**
     * Populate available platforms into combo box.
     */
    public void setPlatforms(List<String> platforms) {
        Platform.runLater(() -> {
            platformComboBox.getItems().clear();
            if (platforms != null) {
                platformComboBox.getItems().addAll(platforms);
            }
            if (!platformComboBox.getItems().isEmpty()) {
                platformComboBox.getSelectionModel().selectFirst();
                loadForSelectedPlatform();
            }
        });
    }

    @FXML
    private void handleLoad() {
        loadForSelectedPlatform();
    }

    private void loadForSelectedPlatform() {
        String platform = platformComboBox.getSelectionModel().getSelectedItem();
        if (platform == null || credentialStore == null) return;

        String k1 = platform + ".apiKey";
        String k2 = platform + ".apiSecret";
        String k3 = platform + ".accessToken";
        String k4 = platform + ".accessSecret";

        apiKeyField.setText(safeRetrieve(k1));
        apiSecretField.setText(safeRetrieve(k2));
        accessTokenField.setText(safeRetrieve(k3));
        accessSecretField.setText(safeRetrieve(k4));
    }

    private String safeRetrieve(String key) {
        try {
            String v = credentialStore.retrieve(key);
            return v == null ? "" : v;
        } catch (Exception e) {
            logger.warn("Failed to retrieve credential {}: {}", key, e.getMessage());
            return "";
        }
    }

    @FXML
    private void handleTest() {
        String platform = platformComboBox.getSelectionModel().getSelectedItem();
        if (platform == null) {
            showAlert(Alert.AlertType.WARNING, "No platform selected", "Please select a platform to test credentials.");
            return;
        }

        try {
            // store provided values temporarily (this will persist them) and then verify
            storeValuesForPlatform(platform);

            // verify round-trip (attempt to retrieve at least one value)
            credentialStore.retrieve(platform + ".apiKey");
            showAlert(Alert.AlertType.INFORMATION, "Test Success", "Credentials stored and readable for platform: " + platform);
        } catch (SecurityException e) {
            logger.error("Credential test failed", e);
            showAlert(Alert.AlertType.ERROR, "Test Failed", "Failed to store/test credentials: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during credential test", e);
            showAlert(Alert.AlertType.ERROR, "Test Failed", "Unexpected error: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        String platform = platformComboBox.getSelectionModel().getSelectedItem();
        if (platform == null) {
            showAlert(Alert.AlertType.WARNING, "No platform selected", "Please select a platform to save credentials.");
            return;
        }

        try {
            storeValuesForPlatform(platform);
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Credentials saved for platform: " + platform);
            // close dialog if possible
            closeWindow();
        } catch (SecurityException e) {
            logger.error("Failed to save credentials", e);
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Failed to save credentials: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error saving credentials", e);
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Unexpected error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void storeValuesForPlatform(String platform) throws SecurityException {
        if (credentialStore == null) throw new SecurityException("Credential store not initialized");

        String k1 = platform + ".apiKey";
        String k2 = platform + ".apiSecret";
        String k3 = platform + ".accessToken";
        String k4 = platform + ".accessSecret";

        // store values (allow empty values but keep keys consistent)
        credentialStore.store(k1, apiKeyField.getText() == null ? "" : apiKeyField.getText());
        credentialStore.store(k2, apiSecretField.getText() == null ? "" : apiSecretField.getText());
        credentialStore.store(k3, accessTokenField.getText() == null ? "" : accessTokenField.getText());
        credentialStore.store(k4, accessSecretField.getText() == null ? "" : accessSecretField.getText());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    private void closeWindow() {
        Window w = platformComboBox.getScene().getWindow();
        if (w != null) w.hide();
    }
}
