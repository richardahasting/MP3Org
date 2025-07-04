package org.hasting.ui.panels;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.hasting.util.HelpSystem;
import org.hasting.util.logging.LogBackupManager;
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
    
    // Backup and rotation controls
    private CheckBox backupEnabledCheckBox;
    private Spinner<Integer> backupMaxSizeSpinner;
    private Spinner<Integer> backupCountSpinner;
    private CheckBox compressionEnabledCheckBox;
    private Slider compressionLevelSlider;
    private Label compressionLevelLabel;
    private TextField backupDirectoryField;
    private Button browseBackupDirButton;
    private Button backupNowButton;
    private Label backupStatusLabel;
    
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
        
        // Backup and rotation controls
        initializeBackupControls();
        
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
     * Initializes backup and rotation control components.
     */
    private void initializeBackupControls() {
        // Backup enabled checkbox
        backupEnabledCheckBox = new CheckBox("Enable Automatic Backup");
        backupEnabledCheckBox.setSelected(true);
        HelpSystem.setTooltip(backupEnabledCheckBox, "config.logging.backup.enabled");
        
        // Backup max size spinner
        backupMaxSizeSpinner = new Spinner<>(1, 1000, 10); // 1MB to 1000MB, default 10MB
        backupMaxSizeSpinner.setEditable(true);
        backupMaxSizeSpinner.setPrefWidth(80);
        HelpSystem.setTooltip(backupMaxSizeSpinner, "config.logging.backup.maxSize");
        
        // Backup count spinner
        backupCountSpinner = new Spinner<>(1, 50, 5); // 1 to 50 backups, default 5
        backupCountSpinner.setEditable(true);
        backupCountSpinner.setPrefWidth(80);
        HelpSystem.setTooltip(backupCountSpinner, "config.logging.backup.count");
        
        // Compression enabled checkbox
        compressionEnabledCheckBox = new CheckBox("Enable Compression");
        compressionEnabledCheckBox.setSelected(true);
        HelpSystem.setTooltip(compressionEnabledCheckBox, "config.logging.backup.compression");
        
        // Compression level slider
        compressionLevelSlider = new Slider(1, 9, 6);
        compressionLevelSlider.setMajorTickUnit(1);
        compressionLevelSlider.setMinorTickCount(0);
        compressionLevelSlider.setSnapToTicks(true);
        compressionLevelSlider.setShowTickLabels(true);
        compressionLevelSlider.setPrefWidth(200);
        
        compressionLevelLabel = new Label("Level: 6");
        compressionLevelSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            compressionLevelLabel.setText("Level: " + newVal.intValue());
        });
        
        // Enable/disable compression level based on compression checkbox
        compressionEnabledCheckBox.setOnAction(e -> {
            boolean compressionEnabled = compressionEnabledCheckBox.isSelected();
            compressionLevelSlider.setDisable(!compressionEnabled);
            compressionLevelLabel.setDisable(!compressionEnabled);
        });
        
        // Backup directory controls
        backupDirectoryField = new TextField();
        backupDirectoryField.setPromptText("backup");
        backupDirectoryField.setPrefWidth(200);
        backupDirectoryField.setText("backup");
        HelpSystem.setTooltip(backupDirectoryField, "config.logging.backup.directory");
        
        browseBackupDirButton = new Button("Browse...");
        browseBackupDirButton.setOnAction(e -> selectBackupDirectory());
        
        // Backup now button
        backupNowButton = new Button("Backup Now");
        backupNowButton.setOnAction(e -> performManualBackup());
        backupNowButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        
        // Backup status label
        backupStatusLabel = new Label("No recent backup activity");
        backupStatusLabel.setStyle("-fx-text-fill: #666666;");
        
        // Enable/disable backup controls based on backup enabled checkbox
        backupEnabledCheckBox.setOnAction(e -> updateBackupControlsState());
    }
    
    /**
     * Updates the state of backup controls based on whether backup is enabled.
     */
    private void updateBackupControlsState() {
        boolean backupEnabled = backupEnabledCheckBox.isSelected();
        backupMaxSizeSpinner.setDisable(!backupEnabled);
        backupCountSpinner.setDisable(!backupEnabled);
        compressionEnabledCheckBox.setDisable(!backupEnabled);
        
        boolean compressionEnabled = compressionEnabledCheckBox.isSelected() && backupEnabled;
        compressionLevelSlider.setDisable(!compressionEnabled);
        compressionLevelLabel.setDisable(!compressionEnabled);
        
        backupDirectoryField.setDisable(!backupEnabled);
        browseBackupDirButton.setDisable(!backupEnabled);
        backupNowButton.setDisable(!backupEnabled);
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
        
        // Backup and Rotation Section
        Label backupTitle = new Label("Backup and Rotation Settings");
        backupTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label backupInstructions = new Label(
            "Configure automatic backup and compression settings for log files. " +
            "Backups are created when log files exceed the specified size threshold."
        );
        backupInstructions.setWrapText(true);
        backupInstructions.setStyle("-fx-text-fill: #666666;");
        
        GridPane backupGrid = new GridPane();
        backupGrid.setHgap(10);
        backupGrid.setVgap(8);
        
        // Row 0: Backup enabled
        backupGrid.add(backupEnabledCheckBox, 0, 0, 2, 1);
        
        // Row 1: Max size and backup count
        backupGrid.add(new Label("Max Log Size (MB):"), 0, 1);
        backupGrid.add(backupMaxSizeSpinner, 1, 1);
        backupGrid.add(new Label("Keep Backups:"), 2, 1);
        backupGrid.add(backupCountSpinner, 3, 1);
        
        // Row 2: Compression settings
        backupGrid.add(compressionEnabledCheckBox, 0, 2, 2, 1);
        HBox compressionBox = new HBox(10);
        compressionBox.getChildren().addAll(compressionLevelSlider, compressionLevelLabel);
        backupGrid.add(new Label("Compression Level:"), 2, 2);
        backupGrid.add(compressionBox, 3, 2);
        
        // Row 3: Backup directory
        backupGrid.add(new Label("Backup Directory:"), 0, 3);
        HBox backupDirBox = new HBox(5);
        backupDirBox.getChildren().addAll(backupDirectoryField, browseBackupDirButton);
        backupGrid.add(backupDirBox, 1, 3, 3, 1);
        
        // Backup controls box
        HBox backupControlsBox = new HBox(10);
        backupControlsBox.getChildren().addAll(backupNowButton, backupStatusLabel);
        
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
            backupTitle,
            backupInstructions,
            backupGrid,
            backupControlsBox,
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
            
            // Load backup settings
            backupEnabledCheckBox.setSelected(currentConfig.isBackupEnabled());
            backupMaxSizeSpinner.getValueFactory().setValue(currentConfig.getBackupMaxSizeMB());
            backupCountSpinner.getValueFactory().setValue(currentConfig.getBackupCount());
            compressionEnabledCheckBox.setSelected(currentConfig.isCompressionEnabled());
            compressionLevelSlider.setValue(currentConfig.getCompressionLevel());
            compressionLevelLabel.setText("Level: " + currentConfig.getCompressionLevel());
            backupDirectoryField.setText(currentConfig.getBackupDirectory());
            
            // Update backup control states
            updateBackupControlsState();
            
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
            
            // Set backup settings
            updateConfigurationFromUI(newConfig);
            
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
            // Get the parent stage for proper dialog ownership
            javafx.stage.Stage parentStage = (javafx.stage.Stage) getScene().getWindow();
            
            // Create and show the log viewer dialog
            org.hasting.ui.LogViewerDialog logViewer = new org.hasting.ui.LogViewerDialog(parentStage);
            logViewer.show();
            
            statusLabel.setText("Log viewer opened successfully");
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            
            logger.info("Log viewer dialog opened from logging configuration panel");
            
        } catch (Exception e) {
            statusLabel.setText("Error opening log viewer: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            logger.error("Failed to open log viewer dialog: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles backup directory selection.
     */
    private void selectBackupDirectory() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Select Backup Directory");
        
        // Set initial directory to current backup directory or log directory
        try {
            LoggingConfiguration currentConfig = MP3OrgLoggingManager.getCurrentConfiguration();
            String currentBackupDir = currentConfig.getFullBackupDirectoryPath();
            java.io.File initialDir = new java.io.File(currentBackupDir);
            if (initialDir.exists()) {
                directoryChooser.setInitialDirectory(initialDir);
            } else {
                // Fall back to log directory
                String logFilePath = currentConfig.getFilePath();
                if (logFilePath != null) {
                    java.io.File logFile = new java.io.File(logFilePath);
                    java.io.File logDir = logFile.getParentFile();
                    if (logDir != null && logDir.exists()) {
                        directoryChooser.setInitialDirectory(logDir);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not set initial directory for backup directory chooser: {}", e.getMessage());
        }
        
        java.io.File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory != null) {
            backupDirectoryField.setText(selectedDirectory.getName()); // Use relative name
        }
    }
    
    /**
     * Performs a manual backup of the current log file.
     */
    private void performManualBackup() {
        try {
            backupStatusLabel.setText("Creating backup...");
            backupStatusLabel.setStyle("-fx-text-fill: #2196F3;");
            
            // Get current configuration and apply current UI settings
            LoggingConfiguration config = MP3OrgLoggingManager.getCurrentConfiguration();
            updateConfigurationFromUI(config);
            
            // Perform the backup
            boolean success = LogBackupManager.forceBackup(config);
            
            if (success) {
                backupStatusLabel.setText("Backup created successfully");
                backupStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                logger.info("Manual backup completed successfully");
            } else {
                backupStatusLabel.setText("Backup failed - check log for details");
                backupStatusLabel.setStyle("-fx-text-fill: #f44336;");
            }
            
        } catch (Exception e) {
            backupStatusLabel.setText("Error during backup: " + e.getMessage());
            backupStatusLabel.setStyle("-fx-text-fill: #f44336;");
            logger.error("Manual backup failed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Updates a LoggingConfiguration object with current UI settings.
     */
    private void updateConfigurationFromUI(LoggingConfiguration config) {
        // Update backup settings from UI
        config.setBackupEnabled(backupEnabledCheckBox.isSelected());
        config.setBackupMaxSizeMB(backupMaxSizeSpinner.getValue());
        config.setBackupCount(backupCountSpinner.getValue());
        config.setCompressionEnabled(compressionEnabledCheckBox.isSelected());
        config.setCompressionLevel((int) compressionLevelSlider.getValue());
        config.setBackupDirectory(backupDirectoryField.getText().trim());
    }
}