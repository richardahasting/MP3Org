package org.hasting.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.hasting.util.logging.LogLevel;
import org.hasting.util.logging.MP3OrgLoggingManager;
import org.hasting.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * JavaFX dialog for viewing and analyzing MP3Org log files.
 * 
 * <p>This dialog provides comprehensive log file viewing capabilities including:
 * <ul>
 *   <li>Real-time log file viewing with auto-refresh</li>
 *   <li>Log level filtering (DEBUG, INFO, WARNING, ERROR, CRITICAL)</li>
 *   <li>Search functionality with regex support</li>
 *   <li>Line number display and navigation</li>
 *   <li>Export filtered logs to file</li>
 *   <li>Tail mode for following active logs</li>
 * </ul>
 * 
 * @since 1.0
 */
public class LogViewerDialog extends Stage {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(LogViewerDialog.class);
    
    // UI Components
    private TextArea logTextArea;
    private ComboBox<LogLevel> levelFilterCombo;
    private TextField searchField;
    private CheckBox regexCheckBox;
    private CheckBox tailModeCheckBox;
    private Label statusLabel;
    private Button refreshButton;
    private Button exportButton;
    private Button clearButton;
    
    // Data and state
    private File currentLogFile;
    private ExecutorService backgroundExecutor;
    private boolean tailModeActive = false;
    private long lastFileSize = 0;
    
    // Filtering and search
    private LogLevel currentLevelFilter = null;
    private String currentSearchTerm = "";
    private boolean useRegexSearch = false;
    
    /**
     * Creates a new log viewer dialog.
     * 
     * @param owner the parent stage
     */
    public LogViewerDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.NONE); // Allow interaction with parent window
        setTitle("MP3Org Log Viewer");
        
        backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LogViewer-Background");
            t.setDaemon(true);
            return t;
        });
        
        initializeUI();
        setupEventHandlers();
        loadDefaultLogFile();
        
        // Set window size and position
        setWidth(1000);
        setHeight(700);
        setMinWidth(600);
        setMinHeight(400);
        
        // Center on parent if available
        if (owner != null) {
            setX(owner.getX() + (owner.getWidth() - getWidth()) / 2);
            setY(owner.getY() + (owner.getHeight() - getHeight()) / 2);
        }
        
        // Handle window closing
        setOnCloseRequest(e -> cleanup());
        
        logger.info("Log viewer dialog initialized");
    }
    
    /**
     * Initializes the user interface components.
     */
    private void initializeUI() {
        BorderPane root = new BorderPane();
        
        // Create top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Create main content area
        VBox mainContent = createMainContent();
        root.setCenter(mainContent);
        
        // Create bottom status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        Scene scene = new Scene(root);
        setScene(scene);
    }
    
    /**
     * Creates the toolbar with controls for file operations and filtering.
     */
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0;");
        
        // File operations
        Button openButton = new Button("Open Log File...");
        openButton.setOnAction(e -> openLogFile());
        
        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshLogContent());
        
        exportButton = new Button("Export...");
        exportButton.setOnAction(e -> exportFilteredLogs());
        
        clearButton = new Button("Clear");
        clearButton.setOnAction(e -> clearLogDisplay());
        
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-orientation: vertical;");
        
        // Filtering controls
        Label levelLabel = new Label("Level:");
        levelFilterCombo = new ComboBox<>();
        levelFilterCombo.getItems().addAll(LogLevel.values());
        levelFilterCombo.getItems().add(0, null); // Add "All" option
        levelFilterCombo.setConverter(new StringConverter<LogLevel>() {
            @Override
            public String toString(LogLevel level) {
                return level == null ? "All Levels" : level.toString();
            }
            
            @Override
            public LogLevel fromString(String string) {
                return string.equals("All Levels") ? null : LogLevel.valueOf(string);
            }
        });
        levelFilterCombo.setValue(null);
        
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-orientation: vertical;");
        
        // Search controls
        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Enter search term...");
        searchField.setPrefWidth(200);
        
        regexCheckBox = new CheckBox("Regex");
        
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        
        Separator sep3 = new Separator();
        sep3.setStyle("-fx-orientation: vertical;");
        
        // Auto-refresh controls
        tailModeCheckBox = new CheckBox("Tail Mode");
        tailModeCheckBox.setOnAction(e -> toggleTailMode());
        
        toolbar.getChildren().addAll(
            openButton, refreshButton, exportButton, clearButton, sep1,
            levelLabel, levelFilterCombo, sep2,
            searchLabel, searchField, regexCheckBox, searchButton, sep3,
            tailModeCheckBox
        );
        
        return toolbar;
    }
    
    /**
     * Creates the main content area with the log text display.
     */
    private VBox createMainContent() {
        VBox content = new VBox(5);
        content.setPadding(new Insets(10));
        
        // Log text area
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        logTextArea.setWrapText(true);
        
        VBox.setVgrow(logTextArea, Priority.ALWAYS);
        content.getChildren().add(logTextArea);
        
        return content;
    }
    
    /**
     * Creates the bottom status bar.
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #c0c0c0; -fx-border-width: 1 0 0 0;");
        
        statusLabel = new Label("Ready");
        statusBar.getChildren().add(statusLabel);
        
        return statusBar;
    }
    
    /**
     * Sets up event handlers for UI components.
     */
    private void setupEventHandlers() {
        // Level filter change
        levelFilterCombo.setOnAction(e -> {
            currentLevelFilter = levelFilterCombo.getValue();
            applyFilters();
        });
        
        // Search field enter key
        searchField.setOnAction(e -> performSearch());
        
        // Regex checkbox change
        regexCheckBox.setOnAction(e -> {
            useRegexSearch = regexCheckBox.isSelected();
            if (!searchField.getText().trim().isEmpty()) {
                performSearch();
            }
        });
    }
    
    /**
     * Loads the default log file from the logging configuration.
     */
    private void loadDefaultLogFile() {
        try {
            String defaultLogPath = MP3OrgLoggingManager.getCurrentConfiguration().getFilePath();
            if (defaultLogPath != null && !defaultLogPath.isEmpty()) {
                File defaultFile = new File(defaultLogPath);
                if (defaultFile.exists()) {
                    loadLogFile(defaultFile);
                    return;
                }
            }
            
            // Try common log file locations
            String[] commonPaths = {
                "mp3org/logs/mp3org.log",
                "mp3org/logs/mp3org-dev.log",
                "logs/mp3org.log",
                "mp3org.log"
            };
            
            for (String path : commonPaths) {
                File file = new File(path);
                if (file.exists()) {
                    loadLogFile(file);
                    return;
                }
            }
            
            updateStatus("No default log file found. Use 'Open Log File...' to select a file.");
            
        } catch (Exception e) {
            logger.error("Error loading default log file: {}", e.getMessage(), e);
            updateStatus("Error loading default log file: " + e.getMessage());
        }
    }
    
    /**
     * Opens a file chooser to select a log file.
     */
    private void openLogFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Log File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Log Files", "*.log"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Set initial directory to log directory if it exists
        File logDir = new File("mp3org/logs");
        if (logDir.exists() && logDir.isDirectory()) {
            fileChooser.setInitialDirectory(logDir);
        }
        
        File selectedFile = fileChooser.showOpenDialog(this);
        if (selectedFile != null) {
            loadLogFile(selectedFile);
        }
    }
    
    /**
     * Loads content from the specified log file.
     * 
     * @param file the log file to load
     */
    private void loadLogFile(File file) {
        currentLogFile = file;
        refreshLogContent();
        setTitle("MP3Org Log Viewer - " + file.getName());
        logger.info("Loaded log file: {}", file.getAbsolutePath());
    }
    
    /**
     * Refreshes the log content from the current file.
     */
    private void refreshLogContent() {
        if (currentLogFile == null) {
            return;
        }
        
        backgroundExecutor.submit(() -> {
            try {
                List<String> lines = Files.readAllLines(currentLogFile.toPath());
                Platform.runLater(() -> {
                    displayLogLines(lines);
                    lastFileSize = currentLogFile.length();
                    updateStatus(String.format("Loaded %d lines from %s", 
                                              lines.size(), currentLogFile.getName()));
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    logger.error("Error reading log file: {}", e.getMessage(), e);
                    updateStatus("Error reading log file: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * Displays the log lines in the text area with current filters applied.
     * 
     * @param lines the log lines to display
     */
    private void displayLogLines(List<String> lines) {
        StringBuilder filteredContent = new StringBuilder();
        int lineNumber = 1;
        int displayedLines = 0;
        
        for (String line : lines) {
            if (shouldDisplayLine(line)) {
                // Add line number prefix
                filteredContent.append(String.format("%5d: %s%n", lineNumber, line));
                displayedLines++;
            }
            lineNumber++;
        }
        
        logTextArea.setText(filteredContent.toString());
        
        // Scroll to bottom if in tail mode
        if (tailModeActive) {
            logTextArea.setScrollTop(Double.MAX_VALUE);
        }
        
        updateStatus(String.format("Displaying %d of %d lines from %s", 
                                  displayedLines, lines.size(), currentLogFile.getName()));
    }
    
    /**
     * Determines if a log line should be displayed based on current filters.
     * 
     * @param line the log line to check
     * @return true if the line should be displayed
     */
    private boolean shouldDisplayLine(String line) {
        // Apply level filter
        if (currentLevelFilter != null) {
            if (!line.contains("[" + currentLevelFilter.toString() + "]")) {
                return false;
            }
        }
        
        // Apply search filter
        if (!currentSearchTerm.isEmpty()) {
            if (useRegexSearch) {
                try {
                    Pattern pattern = Pattern.compile(currentSearchTerm, Pattern.CASE_INSENSITIVE);
                    if (!pattern.matcher(line).find()) {
                        return false;
                    }
                } catch (Exception e) {
                    // Invalid regex, fall back to simple contains
                    if (!line.toLowerCase().contains(currentSearchTerm.toLowerCase())) {
                        return false;
                    }
                }
            } else {
                if (!line.toLowerCase().contains(currentSearchTerm.toLowerCase())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Applies current filters to the displayed log content.
     */
    private void applyFilters() {
        refreshLogContent();
    }
    
    /**
     * Performs a search with the current search term.
     */
    private void performSearch() {
        currentSearchTerm = searchField.getText().trim();
        useRegexSearch = regexCheckBox.isSelected();
        applyFilters();
        
        if (!currentSearchTerm.isEmpty()) {
            logger.debug("Performed log search: '{}' (regex: {})", currentSearchTerm, useRegexSearch);
        }
    }
    
    /**
     * Toggles tail mode for following log file changes.
     */
    private void toggleTailMode() {
        tailModeActive = tailModeCheckBox.isSelected();
        
        if (tailModeActive) {
            startTailMode();
            updateStatus("Tail mode enabled - watching for file changes");
        } else {
            updateStatus("Tail mode disabled");
        }
        
        logger.info("Tail mode {}", tailModeActive ? "enabled" : "disabled");
    }
    
    /**
     * Starts tail mode monitoring for file changes.
     */
    private void startTailMode() {
        if (currentLogFile == null) {
            return;
        }
        
        backgroundExecutor.submit(() -> {
            while (tailModeActive && currentLogFile != null) {
                try {
                    long currentSize = currentLogFile.length();
                    if (currentSize > lastFileSize) {
                        Platform.runLater(this::refreshLogContent);
                    }
                    
                    Thread.sleep(1000); // Check every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in tail mode: {}", e.getMessage(), e);
                    Platform.runLater(() -> {
                        tailModeCheckBox.setSelected(false);
                        tailModeActive = false;
                        updateStatus("Tail mode error: " + e.getMessage());
                    });
                    break;
                }
            }
        });
    }
    
    /**
     * Exports the currently filtered log content to a file.
     */
    private void exportFilteredLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Filtered Logs");
        fileChooser.setInitialFileName("mp3org-filtered-" + 
                                      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + 
                                      ".log");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Log Files", "*.log"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        File exportFile = fileChooser.showSaveDialog(this);
        if (exportFile != null) {
            backgroundExecutor.submit(() -> {
                try {
                    Files.write(exportFile.toPath(), logTextArea.getText().getBytes());
                    Platform.runLater(() -> {
                        updateStatus("Exported filtered logs to: " + exportFile.getName());
                        logger.info("Exported filtered logs to: {}", exportFile.getAbsolutePath());
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        updateStatus("Export failed: " + e.getMessage());
                        logger.error("Failed to export logs: {}", e.getMessage(), e);
                    });
                }
            });
        }
    }
    
    /**
     * Clears the log display.
     */
    private void clearLogDisplay() {
        logTextArea.clear();
        updateStatus("Log display cleared");
    }
    
    /**
     * Updates the status bar message.
     * 
     * @param message the status message
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    /**
     * Cleans up resources when the dialog is closed.
     */
    private void cleanup() {
        tailModeActive = false;
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        logger.info("Log viewer dialog closed");
    }
}