package org.hasting.ui.panels;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import org.hasting.util.DatabaseConfig;
import org.hasting.util.DatabaseManager;
import org.hasting.util.HelpSystem;

import java.io.File;
import java.awt.Desktop;

/**
 * UI panel for managing database location settings in the MP3Org application.
 * Provides functionality to view, change, and reload database configuration.
 */
public class DatabaseLocationPanel extends VBox {
    
    private TextField currentPathField;
    private TextArea configInfoArea;
    private Button changeLocationButton;
    private Button reloadConfigButton;
    private Button openLocationButton;
    private Label statusLabel;
    
    /**
     * Creates a new DatabaseLocationPanel with all necessary components.
     * 
     * @param statusLabel The shared status label for displaying operation results
     */
    public DatabaseLocationPanel(Label statusLabel) {
        this.statusLabel = statusLabel;
        initializeComponents();
        layoutComponents();
        loadCurrentSettings();
    }
    
    /**
     * Initializes all UI components for the database location panel.
     */
    private void initializeComponents() {
        // Current database path display
        currentPathField = new TextField();
        currentPathField.setEditable(false);
        currentPathField.setFocusTraversable(false);
        currentPathField.setMouseTransparent(true);
        currentPathField.setPrefWidth(400);
        currentPathField.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
        HelpSystem.setTooltip(currentPathField, "config.current.path");
        
        // Configuration information display
        configInfoArea = new TextArea();
        configInfoArea.setEditable(false);
        configInfoArea.setPrefRowCount(10);
        configInfoArea.setWrapText(true);
        
        // Control buttons
        changeLocationButton = new Button("Change Database Location");
        changeLocationButton.setOnAction(e -> changeLocation());
        HelpSystem.setTooltip(changeLocationButton, "config.change.location");
        
        reloadConfigButton = new Button("Reload Configuration");
        reloadConfigButton.setOnAction(e -> reloadConfiguration());
        HelpSystem.setTooltip(reloadConfigButton, "config.reload");
        
        openLocationButton = new Button("Open Database Location");
        openLocationButton.setOnAction(e -> openDatabaseLocation());
        HelpSystem.setTooltip(openLocationButton, "config.open.location");
    }
    
    /**
     * Arranges the components in the panel layout.
     */
    private void layoutComponents() {
        setSpacing(10);
        
        // Database location section
        Label locationLabel = new Label("Database Location:");
        locationLabel.setStyle("-fx-font-weight: bold;");
        
        // Button layout
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(
            changeLocationButton, 
            reloadConfigButton, 
            openLocationButton
        );
        
        // Configuration info section
        Label configLabel = new Label("Configuration Information:");
        configLabel.setStyle("-fx-font-weight: bold;");
        
        // Add all components
        getChildren().addAll(
            locationLabel,
            currentPathField,
            buttonBox,
            configLabel,
            configInfoArea
        );
        
        // Make config info area grow
        VBox.setVgrow(configInfoArea, Priority.ALWAYS);
    }
    
    /**
     * Loads and displays the current database settings.
     */
    public void loadCurrentSettings() {
        updateDisplayedInfo();
    }
    
    /**
     * Updates the displayed database configuration information.
     */
    private void updateDisplayedInfo() {
        try {
            DatabaseConfig config = DatabaseManager.getConfig();
            
            // Update current path
            currentPathField.setText(config.getDatabasePath());
            
            // Update configuration info
            configInfoArea.setText(config.getConfigurationInfo());
            
            // Update status
            statusLabel.setText("Configuration loaded successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            statusLabel.setText("Error loading configuration: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }
    
    /**
     * Opens a directory chooser dialog to change the database location.
     */
    private void changeLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Database Location");
        
        // Set initial directory to current database location's parent
        try {
            File currentPath = new File(DatabaseManager.getConfig().getDatabasePath());
            File parentDir = currentPath.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                directoryChooser.setInitialDirectory(parentDir);
            }
        } catch (Exception e) {
            // Use default if there's an issue
        }
        
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory != null) {
            try {
                // Create database subdirectory name
                String newDatabasePath = new File(selectedDirectory, "mp3org").getAbsolutePath();
                
                // Show confirmation dialog
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Change Database Location");
                confirmAlert.setHeaderText("Confirm Database Location Change");
                confirmAlert.setContentText(
                    "Change database location to:\n" + newDatabasePath + "\n\n" +
                    "This will:\n" +
                    "• Close the current database connection\n" +
                    "• Create a new database at the selected location\n" +
                    "• Your existing data will remain at the old location\n\n" +
                    "Continue?"
                );
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        changeDatabaseLocation(newDatabasePath);
                    }
                });
                
            } catch (Exception e) {
                showError("Failed to change database location: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Actually changes the database location to the specified path.
     * 
     * @param newPath The new database path
     */
    private void changeDatabaseLocation(String newPath) {
        try {
            statusLabel.setText("Changing database location...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            // Change the database location
            DatabaseManager.changeDatabaseLocation(newPath);
            
            // Update the display
            updateDisplayedInfo();
            
            statusLabel.setText("Database location changed successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Show success dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Database Location Changed");
            successAlert.setHeaderText("Success");
            successAlert.setContentText("Database location has been changed to:\n" + newPath);
            successAlert.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to change database location: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reloads the database configuration from disk.
     */
    private void reloadConfiguration() {
        try {
            statusLabel.setText("Reloading configuration...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            // Reload database configuration
            DatabaseManager.reloadConfig();
            
            // Small delay to ensure database reinitialization is complete
            Thread.sleep(100);
            
            // Update the displayed information
            updateDisplayedInfo();
            
            statusLabel.setText("Configuration reloaded successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            String errorMessage = "Failed to reload configuration";
            
            // Provide more specific error information
            if (e.getMessage() != null) {
                if (e.getMessage().contains("database")) {
                    errorMessage += " (database connection issue)";
                } else if (e.getMessage().contains("config")) {
                    errorMessage += " (configuration file issue)";
                } else {
                    errorMessage += ": " + e.getMessage();
                }
            }
            
            showError(errorMessage);
            System.err.println("Configuration reload failed: " + e.getMessage());
            e.printStackTrace();
            
            // Try to at least update the display with current info
            try {
                updateDisplayedInfo();
                statusLabel.setText("Reload failed, showing current configuration");
                statusLabel.setStyle("-fx-text-fill: orange;");
            } catch (Exception displayError) {
                statusLabel.setText("Configuration reload failed");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }
    
    /**
     * Opens the database location directory in the system file manager.
     */
    private void openDatabaseLocation() {
        try {
            String databasePath = DatabaseManager.getConfig().getDatabasePath();
            File databaseDir = new File(databasePath);
            
            // If database directory doesn't exist, try to open parent directory
            if (!databaseDir.exists()) {
                databaseDir = databaseDir.getParentFile();
                if (databaseDir == null || !databaseDir.exists()) {
                    showError("Database directory does not exist: " + databasePath);
                    return;
                }
            }
            
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(databaseDir);
                statusLabel.setText("Opened database location in file manager");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                showError("Desktop operations not supported on this system");
            }
            
        } catch (Exception e) {
            showError("Failed to open database location: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Displays an error message to the user.
     * 
     * @param message The error message to display
     */
    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Configuration Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Gets the current path field for external access if needed.
     * 
     * @return The TextField displaying the current database path
     */
    public TextField getCurrentPathField() {
        return currentPathField;
    }
    
    /**
     * Gets the configuration info area for external access if needed.
     * 
     * @return The TextArea displaying configuration information
     */
    public TextArea getConfigInfoArea() {
        return configInfoArea;
    }
}