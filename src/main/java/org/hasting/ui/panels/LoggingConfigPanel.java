package org.hasting.ui.panels;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.hasting.util.HelpSystem;
import org.hasting.util.logging.LogLevel;
import org.hasting.util.logging.LoggingConfiguration;
import org.hasting.util.logging.MP3OrgLoggingManager;
import org.hasting.util.logging.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration panel for comprehensive logging settings management in MP3Org.
 * 
 * <p>This panel provides a user-friendly interface for adjusting all logging parameters
 * without requiring manual file editing or technical knowledge. It integrates with the
 * existing MP3OrgLoggingManager to provide runtime configuration changes.
 * 
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Global logging level and output destination control</li>
 *   <li>Component-specific log level configuration</li>
 *   <li>File logging path and rotation settings</li>
 *   <li>Runtime configuration updates without application restart</li>
 *   <li>Test logging functionality for verification</li>
 *   <li>Reset to defaults capability</li>
 * </ul>
 * 
 * <p>The interface is organized into logical sections:
 * <ul>
 *   <li><strong>Global Settings:</strong> Default log level, console/file output toggles</li>
 *   <li><strong>File Configuration:</strong> Log file path selection and settings</li>
 *   <li><strong>Component Levels:</strong> Per-package/class log level overrides</li>
 *   <li><strong>Runtime Controls:</strong> Apply, reset, and test functionality</li>
 * </ul>
 * 
 * @see MP3OrgLoggingManager for the underlying logging framework
 * @see LoggingConfiguration for configuration data model
 * @since 1.0
 */
public class LoggingConfigPanel extends VBox {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(LoggingConfigPanel.class);
    
    // Global settings controls
    private ComboBox<LogLevel> defaultLevelComboBox;
    private CheckBox consoleLoggingCheckBox;
    private CheckBox fileLoggingCheckBox;
    private TextField logFilePathField;
    private Button browseLogFileButton;
    
    // Component-specific level controls
    private TableView<ComponentLogLevel> componentTable;
    private ObservableList<ComponentLogLevel> componentData;
    private TextField customComponentField;
    private Button addComponentButton;
    
    // Runtime control buttons
    private Button applyChangesButton;
    private Button resetDefaultsButton;
    private Button testLoggingButton;
    private Button viewLogsButton;
    
    // Status display
    private Label statusLabel;
    
    /**
     * Helper class to represent component-specific log levels in the table.
     */
    public static class ComponentLogLevel {
        private final String component;
        private LogLevel level;
        
        public ComponentLogLevel(String component, LogLevel level) {
            this.component = component;
            this.level = level;
        }
        
        public String getComponent() { return component; }
        public LogLevel getLevel() { return level; }
        public void setLevel(LogLevel level) { this.level = level; }
    }
    
    /**
     * Creates a new LoggingConfigPanel with all necessary components initialized.
     */
    public LoggingConfigPanel() {
        initializeComponents();
        layoutComponents();
        loadCurrentSettings();
    }
    
    /**
     * Initializes all UI components for the logging configuration panel.
     */
    private void initializeComponents() {
        // Global settings
        defaultLevelComboBox = new ComboBox<>();
        defaultLevelComboBox.setItems(FXCollections.observableArrayList(LogLevel.values()));
        defaultLevelComboBox.setValue(LogLevel.INFO);
        HelpSystem.setTooltip(defaultLevelComboBox, "config.logging.defaultLevel");
        
        consoleLoggingCheckBox = new CheckBox("Enable Console Logging");
        consoleLoggingCheckBox.setSelected(true);
        HelpSystem.setTooltip(consoleLoggingCheckBox, "config.logging.console");
        
        fileLoggingCheckBox = new CheckBox("Enable File Logging");
        fileLoggingCheckBox.setSelected(false);
        HelpSystem.setTooltip(fileLoggingCheckBox, "config.logging.file");
        
        logFilePathField = new TextField();
        logFilePathField.setPromptText("Select log file path...");
        logFilePathField.setPrefWidth(300);
        logFilePathField.setDisable(true); // Enabled when file logging is checked
        
        browseLogFileButton = new Button("Browse...");
        browseLogFileButton.setOnAction(e -> selectLogFile());
        browseLogFileButton.setDisable(true); // Enabled when file logging is checked
        
        // Enable/disable file controls based on file logging checkbox
        fileLoggingCheckBox.setOnAction(e -> {
            boolean fileLoggingEnabled = fileLoggingCheckBox.isSelected();
            logFilePathField.setDisable(!fileLoggingEnabled);
            browseLogFileButton.setDisable(!fileLoggingEnabled);
        });
        
        // Component-specific level controls
        componentData = FXCollections.observableArrayList();
        componentTable = createComponentTable();
        componentTable.setItems(componentData);
        
        customComponentField = new TextField();
        customComponentField.setPromptText("Enter component name (e.g., org.hasting.util)");
        customComponentField.setPrefWidth(250);
        
        addComponentButton = new Button("Add Component");
        addComponentButton.setOnAction(e -> addCustomComponent());
        
        // Runtime controls
        applyChangesButton = new Button("Apply Changes");
        applyChangesButton.setOnAction(e -> applyChanges());
        applyChangesButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        resetDefaultsButton = new Button("Reset to Defaults");
        resetDefaultsButton.setOnAction(e -> resetToDefaults());
        resetDefaultsButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        
        testLoggingButton = new Button("Test Logging");
        testLoggingButton.setOnAction(e -> testLogging());
        
        viewLogsButton = new Button("View Logs");
        viewLogsButton.setOnAction(e -> viewLogs());
        
        // Status label
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #2196F3;");
    }
    
    /**
     * Creates the table view for component-specific log level management.
     */
    private TableView<ComponentLogLevel> createComponentTable() {
        TableView<ComponentLogLevel> table = new TableView<>();
        table.setPrefHeight(200);
        table.setEditable(true);
        
        // Component name column
        TableColumn<ComponentLogLevel, String> componentCol = new TableColumn<>("Component/Package");
        componentCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getComponent()));
        componentCol.setPrefWidth(300);
        
        // Log level column
        TableColumn<ComponentLogLevel, LogLevel> levelCol = new TableColumn<>("Log Level");
        levelCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getLevel()));
        levelCol.setCellFactory(col -> new ComboBoxTableCell<ComponentLogLevel, LogLevel>(
            FXCollections.observableArrayList(LogLevel.values())) {
            @Override
            public void updateItem(LogLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item.toString());
                }
            }
        });
        levelCol.setOnEditCommit(event -> {
            ComponentLogLevel component = event.getRowValue();
            component.setLevel(event.getNewValue());
        });
        levelCol.setPrefWidth(120);
        
        // Remove button column
        TableColumn<ComponentLogLevel, Void> removeCol = new TableColumn<>("Action");
        removeCol.setCellFactory(col -> new TableCell<ComponentLogLevel, Void>() {
            private final Button removeButton = new Button("Remove");
            
            {
                removeButton.setOnAction(e -> {
                    ComponentLogLevel component = getTableView().getItems().get(getIndex());
                    componentData.remove(component);
                });
                removeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(removeButton);
                }
            }
        });
        removeCol.setPrefWidth(80);
        
        table.getColumns().addAll(componentCol, levelCol, removeCol);
        return table;
    }
    
    /**
     * Arranges all components in the panel layout.
     */
    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(10));
        
        // Global Settings Section
        Label globalTitle = new Label("Global Logging Settings");
        globalTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        GridPane globalGrid = new GridPane();
        globalGrid.setHgap(10);
        globalGrid.setVgap(8);
        
        globalGrid.add(new Label("Default Log Level:"), 0, 0);
        globalGrid.add(defaultLevelComboBox, 1, 0);
        
        globalGrid.add(consoleLoggingCheckBox, 0, 1, 2, 1);
        globalGrid.add(fileLoggingCheckBox, 0, 2, 2, 1);
        
        globalGrid.add(new Label("Log File Path:"), 0, 3);
        HBox filePathBox = new HBox(5);
        filePathBox.getChildren().addAll(logFilePathField, browseLogFileButton);
        globalGrid.add(filePathBox, 1, 3);
        
        // Component-Specific Settings Section
        Label componentTitle = new Label("Component-Specific Log Levels");
        componentTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label componentInstructions = new Label(
            "Configure log levels for specific packages or classes. More specific configurations override general ones."
        );
        componentInstructions.setWrapText(true);
        componentInstructions.setStyle("-fx-text-fill: #666666;");
        
        HBox addComponentBox = new HBox(10);
        addComponentBox.getChildren().addAll(
            new Label("Add Component:"), 
            customComponentField, 
            addComponentButton
        );
        
        // Runtime Controls Section
        Label controlsTitle = new Label("Runtime Controls");
        controlsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        HBox controlsBox = new HBox(10);
        controlsBox.getChildren().addAll(
            applyChangesButton, 
            resetDefaultsButton, 
            testLoggingButton, 
            viewLogsButton
        );
        
        // Add all sections to main layout
        getChildren().addAll(
            globalTitle,
            globalGrid,
            new Separator(),
            componentTitle,
            componentInstructions,
            addComponentBox,
            componentTable,
            new Separator(),
            controlsTitle,
            controlsBox,
            new Separator(),
            statusLabel
        );
    }
    
    /**
     * Loads current logging configuration into the UI components.
     */
    public void loadCurrentSettings() {
        try {
            LoggingConfiguration currentConfig = MP3OrgLoggingManager.getCurrentConfiguration();
            
            // Load global settings
            defaultLevelComboBox.setValue(currentConfig.getDefaultLevel());
            consoleLoggingCheckBox.setSelected(currentConfig.isConsoleEnabled());
            fileLoggingCheckBox.setSelected(currentConfig.isFileEnabled());
            
            if (currentConfig.getFilePath() != null) {
                logFilePathField.setText(currentConfig.getFilePath());
            }
            
            // Enable/disable file controls
            boolean fileLoggingEnabled = fileLoggingCheckBox.isSelected();
            logFilePathField.setDisable(!fileLoggingEnabled);
            browseLogFileButton.setDisable(!fileLoggingEnabled);
            
            // Load component-specific settings
            loadComponentSettings(currentConfig);
            
            statusLabel.setText("Configuration loaded successfully");
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            
        } catch (Exception e) {
            logger.error("Failed to load current logging configuration: {}", e.getMessage(), e);
            statusLabel.setText("Error loading configuration: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #f44336;");
        }
    }
    
    /**
     * Loads component-specific log level settings.
     */
    private void loadComponentSettings(LoggingConfiguration config) {
        componentData.clear();
        
        // Add common MP3Org components
        Map<String, LogLevel> commonComponents = new HashMap<>();
        commonComponents.put("org.hasting", LogLevel.INFO);
        commonComponents.put("org.hasting.util.DatabaseManager", LogLevel.INFO);
        commonComponents.put("org.hasting.util.MusicFileScanner", LogLevel.INFO);
        commonComponents.put("org.hasting.ui", LogLevel.WARNING);
        commonComponents.put("org.hasting.model", LogLevel.WARNING);
        
        // Load from configuration (this would need to be implemented in LoggingConfiguration)
        // For now, use the common components as defaults
        for (Map.Entry<String, LogLevel> entry : commonComponents.entrySet()) {
            componentData.add(new ComponentLogLevel(entry.getKey(), entry.getValue()));
        }
    }
    
    /**
     * Handles log file path selection.
     */
    private void selectLogFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Log File Location");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Log Files", "*.log", "*.txt")
        );
        
        // Set initial directory to mp3org/logs if it exists
        File initialDir = new File("mp3org/logs");
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        
        File selectedFile = fileChooser.showSaveDialog(getScene().getWindow());
        if (selectedFile != null) {
            logFilePathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    /**
     * Adds a custom component to the log level table.
     */
    private void addCustomComponent() {
        String componentName = customComponentField.getText().trim();
        if (componentName.isEmpty()) {
            statusLabel.setText("Please enter a component name");
            statusLabel.setStyle("-fx-text-fill: #ff9800;");
            return;
        }
        
        // Check if component already exists
        boolean exists = componentData.stream()
                .anyMatch(comp -> comp.getComponent().equals(componentName));
        
        if (exists) {
            statusLabel.setText("Component already exists: " + componentName);
            statusLabel.setStyle("-fx-text-fill: #ff9800;");
            return;
        }
        
        // Add new component with INFO level as default
        componentData.add(new ComponentLogLevel(componentName, LogLevel.INFO));
        customComponentField.clear();
        
        statusLabel.setText("Added component: " + componentName);
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
    }
    
    /**
     * Applies the current configuration settings to the logging framework.
     */
    private void applyChanges() {
        try {
            // Create new configuration with current settings
            LoggingConfiguration newConfig = new LoggingConfiguration();
            
            // Set global settings
            newConfig.setDefaultLevel(defaultLevelComboBox.getValue());
            newConfig.setConsoleEnabled(consoleLoggingCheckBox.isSelected());
            newConfig.setFileEnabled(fileLoggingCheckBox.isSelected());
            
            if (fileLoggingCheckBox.isSelected() && !logFilePathField.getText().trim().isEmpty()) {
                newConfig.setFilePath(logFilePathField.getText().trim());
            }
            
            // Set component-specific levels
            for (ComponentLogLevel component : componentData) {
                newConfig.setLoggerLevel(component.getComponent(), component.getLevel());
            }
            
            // Apply the configuration
            MP3OrgLoggingManager.updateConfiguration(newConfig);
            
            statusLabel.setText("Configuration applied successfully");
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            
            logger.info("Logging configuration updated successfully");
            
        } catch (Exception e) {
            logger.error("Failed to apply logging configuration: {}", e.getMessage(), e);
            statusLabel.setText("Error applying configuration: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #f44336;");
        }
    }
    
    /**
     * Resets all settings to their default values.
     */
    private void resetToDefaults() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Reset to Defaults");
        confirmDialog.setHeaderText("Reset Logging Configuration");
        confirmDialog.setContentText("Are you sure you want to reset all logging settings to their default values?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Reset UI to defaults
                defaultLevelComboBox.setValue(LogLevel.INFO);
                consoleLoggingCheckBox.setSelected(true);
                fileLoggingCheckBox.setSelected(false);
                logFilePathField.setText("");
                logFilePathField.setDisable(true);
                browseLogFileButton.setDisable(true);
                
                // Reset component settings
                loadComponentSettings(LoggingConfiguration.createDefault());
                
                statusLabel.setText("Configuration reset to defaults");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                
                logger.info("Logging configuration reset to defaults");
            }
        });
    }
    
    /**
     * Tests the logging functionality by generating sample log entries.
     */
    private void testLogging() {
        try {
            logger.debug("TEST: Debug level message");
            logger.info("TEST: Info level message");
            logger.warning("TEST: Warning level message");
            logger.error("TEST: Error level message");
            logger.critical("TEST: Critical level message");
            
            statusLabel.setText("Test log entries generated - check console and log file");
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            
        } catch (Exception e) {
            statusLabel.setText("Error generating test logs: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #f44336;");
        }
    }
    
    /**
     * Opens the log viewer dialog to view current logs.
     */
    private void viewLogs() {
        try {
            // This would open the existing LogViewerDialog
            // For now, just show a status message
            statusLabel.setText("Log viewer functionality - to be integrated with existing LogViewerDialog");
            statusLabel.setStyle("-fx-text-fill: #2196F3;");
            
        } catch (Exception e) {
            statusLabel.setText("Error opening log viewer: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #f44336;");
        }
    }
}