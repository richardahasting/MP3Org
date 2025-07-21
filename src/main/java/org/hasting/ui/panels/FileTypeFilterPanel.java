package org.hasting.ui.panels;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.hasting.util.DatabaseConfig;
import org.hasting.util.DatabaseManager;
import org.hasting.util.HelpSystem;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.util.HashSet;
import java.util.Set;

/**
 * UI panel for managing file type filters in the MP3Org application.
 * Allows users to select which audio file types should be processed during scanning.
 */
public class FileTypeFilterPanel extends VBox {
    
    private static final Logger logger = Log4Rich.getLogger(FileTypeFilterPanel.class);
    
    private ListView<CheckBox> fileTypesList;
    private Button selectAllButton;
    private Button selectNoneButton;
    private Button applyFiltersButton;
    private Label statusLabel;
    
    /**
     * Creates a new FileTypeFilterPanel with all necessary components.
     * 
     * @param statusLabel The shared status label for displaying operation results
     */
    public FileTypeFilterPanel(Label statusLabel) {
        this.statusLabel = statusLabel;
        initializeComponents();
        layoutComponents();
        loadCurrentSettings();
    }
    
    /**
     * Initializes all UI components for the file type filter panel.
     */
    private void initializeComponents() {
        // Create checkboxes list for each supported file type
        fileTypesList = new ListView<>();
        fileTypesList.setPrefHeight(200);
        HelpSystem.setTooltip(fileTypesList, "config.filetypes.list");
        
        // File type filter buttons
        selectAllButton = new Button("Select All");
        selectAllButton.setOnAction(e -> selectAllFileTypes());
        HelpSystem.setTooltip(selectAllButton, "config.filetypes.selectall");
        
        selectNoneButton = new Button("Select None");
        selectNoneButton.setOnAction(e -> selectNoFileTypes());
        HelpSystem.setTooltip(selectNoneButton, "config.filetypes.selectnone");
        
        applyFiltersButton = new Button("Apply File Type Filters");
        applyFiltersButton.setOnAction(e -> applyFileTypeFilters());
        HelpSystem.setTooltip(applyFiltersButton, "config.filetypes.apply");
    }
    
    /**
     * Arranges the components in the panel layout.
     */
    private void layoutComponents() {
        setSpacing(10);
        
        // File types section
        Label fileTypesLabel = new Label("Supported File Types:");
        fileTypesLabel.setStyle("-fx-font-weight: bold;");
        
        // Button layout
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(selectAllButton, selectNoneButton);
        
        // Add all components
        getChildren().addAll(
            fileTypesLabel,
            fileTypesList,
            buttonBox,
            applyFiltersButton
        );
    }
    
    /**
     * Loads and displays the current file type settings.
     */
    public void loadCurrentSettings() {
        refreshFileTypesList();
    }
    
    /**
     * Refreshes the file types list with current configuration settings.
     */
    private void refreshFileTypesList() {
        try {
            DatabaseConfig config = DatabaseManager.getConfig();
            Set<String> enabledTypes = config.getEnabledFileTypes();
            
            fileTypesList.getItems().clear();
            
            for (String fileType : DatabaseConfig.getAllSupportedTypes()) {
                CheckBox checkBox = new CheckBox(fileType.toUpperCase());
                checkBox.setSelected(enabledTypes.contains(fileType.toLowerCase()));
                checkBox.setUserData(fileType.toLowerCase());
                fileTypesList.getItems().add(checkBox);
            }
        } catch (Exception e) {
            showError("Failed to load file type settings: " + e.getMessage());
        }
    }
    
    /**
     * Selects all file type checkboxes.
     */
    private void selectAllFileTypes() {
        for (CheckBox checkBox : fileTypesList.getItems()) {
            checkBox.setSelected(true);
        }
    }
    
    /**
     * Deselects all file type checkboxes.
     */
    private void selectNoFileTypes() {
        for (CheckBox checkBox : fileTypesList.getItems()) {
            checkBox.setSelected(false);
        }
    }
    
    /**
     * Applies the selected file type filters to the application configuration.
     */
    private void applyFileTypeFilters() {
        try {
            statusLabel.setText("Applying file type filters...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            // Collect selected file types
            Set<String> selectedTypes = new HashSet<>();
            for (CheckBox checkBox : fileTypesList.getItems()) {
                if (checkBox.isSelected()) {
                    selectedTypes.add((String) checkBox.getUserData());
                }
            }
            
            if (selectedTypes.isEmpty()) {
                showError("At least one file type must be selected.");
                return;
            }
            
            // Update configuration
            DatabaseConfig config = DatabaseManager.getConfig();
            config.setEnabledFileTypes(selectedTypes);
            
            statusLabel.setText("File type filters applied successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Show success dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("File Type Filters Updated");
            successAlert.setHeaderText("Success");
            successAlert.setContentText("File type filters have been updated. " +
                "The new filters will apply to future searches and directory scans.");
            successAlert.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to apply file type filters: " + e.getMessage());
            logger.error(String.format("Error applying file type filter configuration: {}", e.getMessage()), e);
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
     * Gets the file types list view for external access if needed.
     * 
     * @return The ListView containing file type checkboxes
     */
    public ListView<CheckBox> getFileTypesList() {
        return fileTypesList;
    }
}