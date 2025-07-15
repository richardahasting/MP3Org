package org.hasting.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.FuzzyMatcher;
import org.hasting.util.HelpSystem;
import org.hasting.util.ProfileChangeListener;
import org.hasting.util.ProfileChangeNotifier;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User interface for managing duplicate music files in the MP3Org application.
 * 
 * <p>This view provides functionality to:
 * <ul>
 * <li>Automatically detect duplicate music files using fuzzy matching algorithms</li>
 * <li>Display duplicates in a searchable, sortable table format</li>
 * <li>Compare potential duplicates side-by-side for manual review</li>
 * <li>Delete unwanted duplicate files with confirmation dialogs</li>
 * <li>Open files in external applications for detailed examination</li>
 * <li>Handle asynchronous duplicate detection with progress feedback</li>
 * </ul>
 * 
 * <p>The duplicate detection process uses sophisticated fuzzy matching based on:
 * <ul>
 * <li>Artist and title similarity (configurable threshold, default >90%)</li>
 * <li>Duration comparison (within 5% tolerance)</li>
 * <li>Bitrate analysis (prefers higher quality files)</li>
 * <li>File path uniqueness validation</li>
 * </ul>
 * 
 * <p>The interface is designed for efficient duplicate management workflows:
 * <ul>
 * <li>Background processing with cancellation support</li>
 * <li>Progressive loading with user feedback</li>
 * <li>Responsive UI that remains interactive during long operations</li>
 * <li>Profile-aware duplicate detection (responds to database profile changes)</li>
 * </ul>
 * 
 * @see MusicFile#isSimilarTo(MusicFile) for duplicate detection algorithm
 * @see DatabaseManager#searchMusicFiles(String) for data retrieval
 * @since 1.0
 */
public class DuplicateManagerView extends BorderPane implements ProfileChangeListener {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(DuplicateManagerView.class);
    
    /**
     * Enumeration for different display modes in the duplicate manager.
     */
    public enum DisplayMode {
        ALL_FILES("All Files"),
        DUPLICATES_ONLY("Duplicates Only");
        
        private final String displayName;
        
        DisplayMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private TableView<MusicFile> duplicatesTable;
    private TableView<MusicFile> comparisonTable;
    private ObservableList<MusicFile> duplicatesData;
    private ObservableList<MusicFile> comparisonData;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private ProgressBar progressBar;
    private Button cancelButton;
    private ChoiceBox<DisplayMode> displayModeChoice;
    private DisplayMode currentDisplayMode;
    private Label leftPaneLabel;
    private ExecutorService executorService;
    private Task<List<MusicFile>> currentDuplicateTask;
    private Task<List<MusicFile>> currentSimilarFilesTask;
    
    /**
     * Creates a new DuplicateManagerView with initialized components and starts
     * asynchronous duplicate detection.
     * 
     * <p>The view is immediately displayed while duplicate detection runs in the
     * background. Progress feedback is provided through progress indicators and
     * status messages.
     */
    public DuplicateManagerView() {
        // Set default display mode first, before initializing components
        currentDisplayMode = DisplayMode.ALL_FILES;
        
        initializeComponents();
        layoutComponents();
        
        // Register for profile change notifications
        ProfileChangeNotifier.getInstance().addListener(this);
        
        // Load all files initially
        loadFilesForCurrentMode();
    }
    
    /**
     * Initializes all UI components including tables, progress indicators, and buttons.
     */
    private void initializeComponents() {
        // Initialize data lists
        duplicatesData = FXCollections.observableArrayList();
        comparisonData = FXCollections.observableArrayList();
        
        // Create main duplicates table
        duplicatesTable = createMusicFileTable();
        duplicatesTable.setItems(duplicatesData);
        
        // Create comparison table
        comparisonTable = createMusicFileTable();
        comparisonTable.setItems(comparisonData);
        
        // Add selection listener to load similar files
        duplicatesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadSimilarFiles(newSelection);
                }
            }
        );
        
        // Status label and progress components
        statusLabel = new Label("Initializing duplicate detection...");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
        
        // Progress bar for background operations
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);
        
        // Cancel button for long operations
        cancelButton = new Button("Cancel");
        cancelButton.setVisible(false);
        cancelButton.setOnAction(e -> cancelCurrentOperation());
        
        // Display mode choice box
        displayModeChoice = new ChoiceBox<>();
        displayModeChoice.getItems().addAll(DisplayMode.values());
        displayModeChoice.setValue(DisplayMode.ALL_FILES);
        displayModeChoice.setOnAction(e -> {
            DisplayMode newMode = displayModeChoice.getValue();
            if (newMode != currentDisplayMode) {
                currentDisplayMode = newMode;
                loadFilesForCurrentMode();
            }
        });
        HelpSystem.setTooltip(displayModeChoice, "duplicates.display.mode");
        
        // Initialize thread pool for background operations
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DuplicateDetection");
            t.setDaemon(true);
            return t;
        });
    }
    
    private TableView<MusicFile> createMusicFileTable() {
        TableView<MusicFile> table = new TableView<>();
        
        // Artist column
        TableColumn<MusicFile, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArtist()));
        artistCol.setPrefWidth(150);
        
        // Title column
        TableColumn<MusicFile, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        titleCol.setPrefWidth(200);
        
        // Album column
        TableColumn<MusicFile, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAlbum()));
        albumCol.setPrefWidth(150);
        
        // Track Number column
        TableColumn<MusicFile, String> trackCol = new TableColumn<>("Track #");
        trackCol.setCellValueFactory(data -> {
            Integer trackNumber = data.getValue().getTrackNumber();
            return new SimpleStringProperty(trackNumber != null ? trackNumber.toString() : "");
        });
        trackCol.setPrefWidth(70);
        
        // Duration column
        TableColumn<MusicFile, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> {
            Integer duration = data.getValue().getDurationSeconds();
            if (duration != null) {
                int minutes = duration / 60;
                int seconds = duration % 60;
                return new SimpleStringProperty(String.format("%d:%02d", minutes, seconds));
            }
            return new SimpleStringProperty("");
        });
        durationCol.setPrefWidth(80);
        
        // Bitrate column
        TableColumn<MusicFile, String> bitrateCol = new TableColumn<>("Bitrate");
        bitrateCol.setCellValueFactory(data -> {
            Long bitrate = data.getValue().getBitRate();
            return new SimpleStringProperty(bitrate != null ? bitrate + " kbps" : "");
        });
        bitrateCol.setPrefWidth(80);
        
        // File path column
        TableColumn<MusicFile, String> pathCol = new TableColumn<>("File Path");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFilePath()));
        pathCol.setPrefWidth(300);
        
        table.getColumns().addAll(artistCol, titleCol, albumCol, trackCol, durationCol, bitrateCol, pathCol);
        
        // Add context menu
        setupTableContextMenu(table);
        
        return table;
    }
    
    private void layoutComponents() {
        // Create top controls
        HBox topControls = new HBox(10);
        topControls.setPadding(new Insets(5));
        
        // Display mode label and choice box
        Label displayModeLabel = new Label("Display:");
        displayModeLabel.setStyle("-fx-font-weight: bold;");
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadFilesForCurrentMode());
        HelpSystem.setTooltip(refreshButton, "duplicates.refresh");
        
        Button deleteSelectedButton = new Button("Delete Selected");
        deleteSelectedButton.setOnAction(e -> deleteSelectedFile());
        HelpSystem.setTooltip(deleteSelectedButton, "duplicates.delete.selected");
        
        Button keepBetterQualityButton = new Button("Keep Better Quality");
        keepBetterQualityButton.setOnAction(e -> keepBetterQuality());
        HelpSystem.setTooltip(keepBetterQualityButton, "duplicates.keep.better");
        
        Button helpButton = new Button("?");
        helpButton.setPrefSize(25, 25);
        helpButton.setOnAction(e -> HelpSystem.showHelpDialog("duplicates.help", "Duplicate Management Help", getScene().getWindow()));
        
        topControls.getChildren().addAll(displayModeLabel, displayModeChoice, new Separator(Orientation.VERTICAL), 
                                          refreshButton, deleteSelectedButton, keepBetterQualityButton, helpButton);
        
        // Create main content area with split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        
        // Left side - dynamic label based on display mode
        VBox leftSide = new VBox(5);
        leftSide.setPadding(new Insets(5));
        leftPaneLabel = new Label(getLeftPaneLabelText());
        leftPaneLabel.setStyle("-fx-font-weight: bold;");
        HelpSystem.setTooltip(duplicatesTable, "duplicates.potential.table");
        leftSide.getChildren().addAll(leftPaneLabel, duplicatesTable);
        
        // Right side - similar files
        VBox rightSide = new VBox(5);
        rightSide.setPadding(new Insets(5));
        Label similarLabel = new Label("Similar Files:");
        similarLabel.setStyle("-fx-font-weight: bold;");
        HelpSystem.setTooltip(comparisonTable, "duplicates.similar.table");
        rightSide.getChildren().addAll(similarLabel, comparisonTable);
        
        splitPane.getItems().addAll(leftSide, rightSide);
        splitPane.setDividerPositions(0.5);
        
        // Bottom status bar with enhanced progress indicators
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        statusBar.getChildren().addAll(statusLabel, progressIndicator, progressBar, cancelButton);
        
        // Layout main components
        setTop(topControls);
        setCenter(splitPane);
        setBottom(statusBar);
        
        // Make tables grow to fill space
        VBox.setVgrow(duplicatesTable, Priority.ALWAYS);
        VBox.setVgrow(comparisonTable, Priority.ALWAYS);
    }
    
    /**
     * Loads duplicates asynchronously to prevent UI blocking.
     * This method returns immediately, allowing the UI to remain responsive.
     */
    private void loadDuplicatesAsync() {
        // Cancel any existing operation
        if (currentDuplicateTask != null && !currentDuplicateTask.isDone()) {
            currentDuplicateTask.cancel(true);
        }
        
        // Clear existing data and show loading state
        duplicatesData.clear();
        comparisonData.clear();
        
        // Update UI to show loading state
        statusLabel.setText("Analyzing music collection for duplicates...");
        progressBar.setProgress(-1); // Indeterminate progress
        progressBar.setVisible(true);
        cancelButton.setVisible(true);
        progressIndicator.setVisible(true);
        
        // Create background task for duplicate detection using parallel processing
        currentDuplicateTask = new Task<List<MusicFile>>() {
            private final Set<MusicFile> foundDuplicates = Collections.synchronizedSet(new HashSet<>());
            
            @Override
            protected List<MusicFile> call() throws Exception {
                // Get all music files for duplicate detection
                List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
                int totalFiles = allFiles.size();
                
                // Clear any existing similar files cache before starting new detection
                updateMessage("Preparing files for duplicate analysis...");
                for (MusicFile file : allFiles) {
                    file.clearSimilarFiles();
                }
                updateMessage("Loading " + totalFiles + " music files...");
                
                if (isCancelled()) return null;
                
                updateMessage("Detecting potential duplicates using parallel processing...");
                updateProgress(0, 100);
                
                // Create callback for streaming results
                FuzzyMatcher.DuplicateCallback callback = new FuzzyMatcher.DuplicateCallback() {
                    @Override
                    public void onDuplicateFound(MusicFile file1, MusicFile file2) {
                        if (!isCancelled()) {
                            foundDuplicates.add(file1);
                            foundDuplicates.add(file2);
                            
                            // Cache similar files in the MusicFile objects for efficient access
                            file1.addSimilarFile(file2);
                            file2.addSimilarFile(file1);
                            
                            // Update UI immediately with new duplicates
                            Platform.runLater(() -> {
                                if (!duplicatesData.contains(file1)) {
                                    duplicatesData.add(file1);
                                }
                                if (!duplicatesData.contains(file2)) {
                                    duplicatesData.add(file2);
                                }
                            });
                            
                            // Update message from task thread (not JavaFX thread)
                            updateMessage("Found " + foundDuplicates.size() + " potential duplicates so far...");
                        }
                    }
                    
                    @Override
                    public void onProgressUpdate(int completed, int total) {
                        if (!isCancelled()) {
                            double progress = (double) completed / total * 100.0;
                            updateProgress(progress, 100);
                            updateMessage(String.format("Analyzed %,d of %,d comparisons (%.1f%%), found %d duplicates", 
                                completed, total, progress, foundDuplicates.size()));
                        }
                    }
                    
                    @Override
                    public boolean isCancelled() {
                        return currentDuplicateTask.isCancelled();
                    }
                };
                
                // Run parallel duplicate detection
                DatabaseManager.findPotentialDuplicatesParallel(callback);
                
                return new ArrayList<>(foundDuplicates);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    updateUIAfterLoad(foundDuplicates.size(), false);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
                    statusLabel.setText("Error loading duplicates: " + errorMsg);
                    updateUIAfterLoad(0, true);
                    logger.error("Failed to detect duplicates: {}", errorMsg, exception);
                });
            }
            
            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    statusLabel.setText("Duplicate detection cancelled");
                    updateUIAfterLoad(duplicatesData.size(), true);
                });
            }
        };
        
        // Bind progress indicators to task
        progressBar.progressProperty().bind(currentDuplicateTask.progressProperty());
        statusLabel.textProperty().bind(currentDuplicateTask.messageProperty());
        
        // Execute task on background thread
        executorService.submit(currentDuplicateTask);
    }
    
    /**
     * Updates UI elements after duplicate loading completes, fails, or is cancelled.
     */
    private void updateUIAfterLoad(int duplicateCount, boolean isError) {
        progressBar.setVisible(false);
        cancelButton.setVisible(false);
        progressIndicator.setVisible(false);
        
        // Unbind properties
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        
        if (!isError) {
            statusLabel.setText("Found " + duplicateCount + " potential duplicates");
        }
    }
    
    /**
     * Cancels the current duplicate detection operation.
     */
    private void cancelCurrentOperation() {
        if (currentDuplicateTask != null && !currentDuplicateTask.isDone()) {
            currentDuplicateTask.cancel(true);
        }
        if (currentSimilarFilesTask != null && !currentSimilarFilesTask.isDone()) {
            currentSimilarFilesTask.cancel(true);
        }
    }
    
    /**
     * Loads files based on the current display mode.
     * - ALL_FILES: Shows complete database (fast load)
     * - DUPLICATES_ONLY: Shows only potential duplicates (parallel processing)
     */
    private void loadFilesForCurrentMode() {
        updateLeftPaneLabel();
        
        switch (currentDisplayMode) {
            case ALL_FILES:
                loadAllFiles();
                break;
            case DUPLICATES_ONLY:
                loadDuplicatesAsync();
                break;
        }
    }
    
    /**
     * Public method to refresh the content when this tab becomes active.
     * This ensures that database-dependent content is always up-to-date.
     */
    public void refreshContent() {
        loadFilesForCurrentMode();
    }
    
    /**
     * Loads all files from the database quickly and displays them.
     * This provides immediate access to the complete music collection.
     */
    private void loadAllFiles() {
        // Cancel any existing operation
        if (currentDuplicateTask != null && !currentDuplicateTask.isDone()) {
            currentDuplicateTask.cancel(true);
        }
        
        // Clear existing data
        duplicatesData.clear();
        comparisonData.clear();
        
        // Show loading state briefly
        statusLabel.setText("Loading all music files...");
        progressIndicator.setVisible(true);
        
        // Create quick loading task
        currentDuplicateTask = new Task<List<MusicFile>>() {
            @Override
            protected List<MusicFile> call() throws Exception {
                return DatabaseManager.getAllMusicFiles();
            }
            
            @Override
            protected void succeeded() {
                List<MusicFile> allFiles = getValue();
                if (allFiles != null && !isCancelled()) {
                    Platform.runLater(() -> {
                        duplicatesData.setAll(allFiles);
                        progressIndicator.setVisible(false);
                        statusLabel.setText("Showing all " + allFiles.size() + " music files - select 'Duplicates Only' to find potential duplicates");
                    });
                }
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    Throwable exception = getException();
                    String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
                    statusLabel.setText("Error loading files: " + errorMsg);
                    logger.error("Failed to load all music files: {}", errorMsg, exception);
                });
            }
            
            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("File loading cancelled");
                });
            }
        };
        
        // Execute task on background thread
        executorService.submit(currentDuplicateTask);
    }
    
    /**
     * Gets the appropriate label text for the left pane based on current display mode.
     */
    private String getLeftPaneLabelText() {
        switch (currentDisplayMode) {
            case ALL_FILES:
                return "All Music Files:";
            case DUPLICATES_ONLY:
                return "Potential Duplicates:";
            default:
                return "Music Files:";
        }
    }
    
    /**
     * Updates the left pane label to match the current display mode.
     */
    private void updateLeftPaneLabel() {
        if (leftPaneLabel != null) {
            leftPaneLabel.setText(getLeftPaneLabelText());
        }
    }
    
    /**
     * Legacy synchronous method - kept for compatibility but now calls async version.
     * @deprecated Use loadDuplicatesAsync() instead for better performance.
     */
    @Deprecated
    private void loadDuplicates() {
        loadDuplicatesAsync();
    }
    
    private void loadSimilarFiles(MusicFile selectedFile) {
        // Cancel any existing similar files task
        if (currentSimilarFilesTask != null && !currentSimilarFilesTask.isDone()) {
            currentSimilarFilesTask.cancel(true);
        }
        
        // Clear existing comparison data and show loading state
        comparisonData.clear();
        
        // Create background task for finding similar files
        currentSimilarFilesTask = new Task<List<MusicFile>>() {
            @Override
            protected List<MusicFile> call() throws Exception {
                updateMessage("Loading similar files for: " + selectedFile.getArtist() + " - " + selectedFile.getTitle());
                updateProgress(0, 100);
                
                if (isCancelled()) return null;
                
                // First, try to use cached similar files from duplicate detection
                List<MusicFile> similarFiles = selectedFile.getSimilarFilesList();
                
                if (!similarFiles.isEmpty()) {
                    // Use cached results from previous duplicate detection
                    updateProgress(100, 100);
                    updateMessage("Using cached results - found " + similarFiles.size() + " similar files");
                    return new ArrayList<>(similarFiles);
                } else {
                    // Fall back to calculating similar files if cache is empty
                    updateMessage("Cache empty, calculating similarities...");
                    updateProgress(25, 100);
                    
                    List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
                    
                    if (isCancelled()) return null;
                    
                    // Find most similar files using traditional method
                    updateMessage("Analyzing similarities...");
                    updateProgress(50, 100);
                    similarFiles = selectedFile.findMostSimilarFiles(allFiles, 10);
                    
                    updateProgress(100, 100);
                    updateMessage("Found " + similarFiles.size() + " similar files");
                    
                    return similarFiles;
                }
            }
            
            @Override
            protected void succeeded() {
                List<MusicFile> similarFiles = getValue();
                if (similarFiles != null && !isCancelled()) {
                    Platform.runLater(() -> {
                        comparisonData.setAll(similarFiles);
                        statusLabel.setText("Loaded " + similarFiles.size() + " similar files");
                    });
                }
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
                    statusLabel.setText("Error loading similar files: " + errorMsg);
                    logger.error("Failed to load similar files: {}", errorMsg, exception);
                });
            }
            
            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    statusLabel.setText("Similar files loading cancelled");
                });
            }
        };
        
        // Show brief status message (no progress indicators for this quick operation)
        statusLabel.setText("Finding similar files...");
        
        // Execute task on background thread
        executorService.submit(currentSimilarFilesTask);
    }
    
    private void deleteSelectedFile() {
        MusicFile selected = duplicatesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setHeaderText("Delete Music File");
            alert.setContentText("Are you sure you want to delete: " + selected.getTitle() + " by " + selected.getArtist() + "?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // Use DatabaseManager to handle both database and file deletion
                        if (DatabaseManager.deleteMusicFile(selected)) {
                            duplicatesData.remove(selected);
                            statusLabel.setText("File deleted successfully");
                        } else {
                            statusLabel.setText("Failed to delete file");
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Error deleting file: " + e.getMessage());
                        logger.error("Failed to delete selected file: {}", e.getMessage(), e);
                    }
                }
            });
        }
    }
    
    private void keepBetterQuality() {
        MusicFile duplicate = duplicatesTable.getSelectionModel().getSelectedItem();
        MusicFile similar = comparisonTable.getSelectionModel().getSelectedItem();
        
        if (duplicate != null && similar != null) {
            // Determine which has better quality (higher bitrate)
            Long duplicateBitrate = duplicate.getBitRate() != null ? duplicate.getBitRate() : 0L;
            Long similarBitrate = similar.getBitRate() != null ? similar.getBitRate() : 0L;
            
            MusicFile toDelete = duplicateBitrate > similarBitrate ? similar : duplicate;
            MusicFile toKeep = duplicateBitrate > similarBitrate ? duplicate : similar;
            
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Keep Better Quality");
            alert.setHeaderText("Delete Lower Quality File");
            alert.setContentText("Keep: " + toKeep.getTitle() + " (" + toKeep.getBitRate() + " kbps)\n" +
                               "Delete: " + toDelete.getTitle() + " (" + toDelete.getBitRate() + " kbps)");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // Use DatabaseManager to handle both database and file deletion
                        if (DatabaseManager.deleteMusicFile(toDelete)) {
                            duplicatesData.remove(toDelete);
                            comparisonData.remove(toDelete);
                            statusLabel.setText("Lower quality file deleted");
                        } else {
                            statusLabel.setText("Failed to delete file");
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Error deleting file: " + e.getMessage());
                        logger.error("Failed to delete file during keep better quality operation: {}", e.getMessage(), e);
                    }
                }
            });
        } else {
            statusLabel.setText("Please select files from both tables");
        }
    }
    
    /**
     * Sets up context menu for a music file table.
     */
    private void setupTableContextMenu(TableView<MusicFile> table) {
        ContextMenu contextMenu = new ContextMenu();
        
        // Delete File menu item
        MenuItem deleteItem = new MenuItem("🗑️ Delete File");
        deleteItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                deleteFile(selectedFile, table);
            }
        });
        
        // Keep Better Quality menu item (only for duplicate context)
        MenuItem keepBetterItem = new MenuItem("✨ Keep Better Quality");
        keepBetterItem.setOnAction(e -> {
            if (table == duplicatesTable) {
                keepBetterQuality();
            } else {
                // For comparison table, compare with selected duplicate
                MusicFile duplicate = duplicatesTable.getSelectionModel().getSelectedItem();
                MusicFile similar = table.getSelectionModel().getSelectedItem();
                if (duplicate != null && similar != null) {
                    keepBetterQualityBetween(duplicate, similar);
                }
            }
        });
        
        // Edit Metadata menu item
        MenuItem editMetadataItem = new MenuItem("✏️ Edit Metadata");
        editMetadataItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                openMetadataEditor(selectedFile);
            }
        });
        
        // Open File Location menu item
        MenuItem openLocationItem = new MenuItem("📂 Open File Location");
        openLocationItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                openFileLocation(selectedFile);
            }
        });
        
        // Copy File Path menu item
        MenuItem copyPathItem = new MenuItem("📋 Copy File Path");
        copyPathItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                copyFilePathToClipboard(selectedFile);
            }
        });
        
        // Add separator
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
        // Show File Info menu item
        MenuItem fileInfoItem = new MenuItem("ℹ️ Show File Info");
        fileInfoItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                showFileInfo(selectedFile);
            }
        });
        
        // Add all items to context menu
        contextMenu.getItems().addAll(
            deleteItem,
            keepBetterItem,
            editMetadataItem,
            separator1,
            openLocationItem,
            copyPathItem,
            fileInfoItem
        );
        
        // Show context menu only when right-clicking on a row with data
        table.setRowFactory(tv -> {
            TableRow<MusicFile> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    // Update menu item availability based on context
                    keepBetterItem.setDisable(table == comparisonTable && duplicatesTable.getSelectionModel().isEmpty());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }
    
    /**
     * Deletes a specific file with confirmation.
     */
    private void deleteFile(MusicFile file, TableView<MusicFile> sourceTable) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Music File");
        alert.setContentText("Are you sure you want to delete: " + file.getTitle() + " by " + file.getArtist() + "?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Use DatabaseManager to handle both database and file deletion
                    if (DatabaseManager.deleteMusicFile(file)) {
                        duplicatesData.remove(file);
                        comparisonData.remove(file);
                        statusLabel.setText("File deleted successfully");
                    } else {
                        statusLabel.setText("Failed to delete file");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error deleting file: " + e.getMessage());
                    logger.error("Failed to delete file: {}", e.getMessage(), e);
                }
            }
        });
    }
    
    /**
     * Compares quality between two specific files and deletes the lower quality one.
     */
    private void keepBetterQualityBetween(MusicFile file1, MusicFile file2) {
        Long bitrate1 = file1.getBitRate() != null ? file1.getBitRate() : 0L;
        Long bitrate2 = file2.getBitRate() != null ? file2.getBitRate() : 0L;
        
        MusicFile toDelete = bitrate1 > bitrate2 ? file2 : file1;
        MusicFile toKeep = bitrate1 > bitrate2 ? file1 : file2;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Keep Better Quality");
        alert.setHeaderText("Delete Lower Quality File");
        alert.setContentText("Keep: " + toKeep.getTitle() + " (" + toKeep.getBitRate() + " kbps)\n" +
                           "Delete: " + toDelete.getTitle() + " (" + toDelete.getBitRate() + " kbps)");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteFile(toDelete, null);
            }
        });
    }
    
    /**
     * Opens the metadata editor for the selected file.
     * Note: This would ideally switch to the metadata tab and populate the form.
     */
    private void openMetadataEditor(MusicFile file) {
        // For now, show file info. In a full implementation, this would:
        // 1. Switch to the Metadata Editor tab
        // 2. Search for this file
        // 3. Select it for editing
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Edit Metadata");
        info.setHeaderText("Open Metadata Editor");
        info.setContentText("To edit metadata for '" + file.getTitle() + "':\n\n" +
                          "1. Go to the Metadata Editor tab\n" +
                          "2. Search for: " + file.getTitle() + "\n" +
                          "3. Select the file and edit its information\n\n" +
                          "File path: " + file.getFilePath());
        info.showAndWait();
    }
    
    /**
     * Opens the file location in the system file manager.
     */
    private void openFileLocation(MusicFile file) {
        try {
            File fileLocation = new File(file.getFilePath());
            if (fileLocation.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(fileLocation.getParentFile());
                    statusLabel.setText("Opened file location");
                } else {
                    statusLabel.setText("Desktop operations not supported");
                }
            } else {
                statusLabel.setText("File does not exist: " + file.getFilePath());
            }
        } catch (Exception e) {
            statusLabel.setText("Error opening file location: " + e.getMessage());
            logger.error("Failed to open file location: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Copies the file path to the system clipboard.
     */
    private void copyFilePathToClipboard(MusicFile file) {
        try {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(file.getFilePath());
            clipboard.setContent(content);
            statusLabel.setText("File path copied to clipboard");
        } catch (Exception e) {
            statusLabel.setText("Error copying to clipboard: " + e.getMessage());
        }
    }
    
    /**
     * Shows detailed information about the selected file.
     */
    private void showFileInfo(MusicFile file) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("File Information");
        info.setHeaderText(file.getTitle() + " by " + file.getArtist());
        
        StringBuilder details = new StringBuilder();
        details.append("Album: ").append(file.getAlbum()).append("\n");
        details.append("Genre: ").append(file.getGenre()).append("\n");
        details.append("Year: ").append(file.getYear()).append("\n");
        details.append("Track: ").append(file.getTrackNumber()).append("\n");
        details.append("Duration: ");
        if (file.getDurationSeconds() != null) {
            int duration = file.getDurationSeconds();
            details.append(String.format("%d:%02d", duration / 60, duration % 60));
        }
        details.append("\n");
        details.append("Bitrate: ").append(file.getBitRate()).append(" kbps\n");
        details.append("Sample Rate: ").append(file.getSampleRate()).append(" Hz\n");
        details.append("File Size: ");
        if (file.getFileSizeBytes() != null) {
            long size = file.getFileSizeBytes();
            if (size > 1024 * 1024) {
                details.append(String.format("%.1f MB", size / (1024.0 * 1024.0)));
            } else {
                details.append(String.format("%.1f KB", size / 1024.0));
            }
        }
        details.append("\n");
        details.append("File Type: ").append(file.getFileType()).append("\n");
        details.append("File Path: ").append(file.getFilePath());
        
        info.setContentText(details.toString());
        info.showAndWait();
    }
    
    /**
     * Cleanup method to properly shutdown background threads.
     * Should be called when the view is being disposed.
     */
    public void cleanup() {
        if (currentDuplicateTask != null && !currentDuplicateTask.isDone()) {
            currentDuplicateTask.cancel(true);
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // Unregister from profile change notifications
        ProfileChangeNotifier.getInstance().removeListener(this);
    }
    
    // ProfileChangeListener implementation
    @Override
    public void onProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile) {
        Platform.runLater(() -> {
            statusLabel.setText("Profile changed to: " + newProfile.getName() + " - Reloading duplicates...");
            
            // Cancel current duplicate detection if running
            if (currentDuplicateTask != null && !currentDuplicateTask.isDone()) {
                currentDuplicateTask.cancel(true);
            }
        });
    }
    
    @Override
    public void onDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase) {
        Platform.runLater(() -> {
            // Reset all data
            resetAllData();
            
            if (isNewDatabase) {
                statusLabel.setText("New database detected - no duplicates to show yet");
                showEmptyDatabaseMessage();
            } else {
                statusLabel.setText("Database changed - reloading duplicates...");
                // Start loading duplicates from the new database
                loadDuplicatesAsync();
            }
        });
    }
    
    /**
     * Resets all data in the duplicate manager.
     */
    private void resetAllData() {
        // Cancel any running tasks
        if (currentDuplicateTask != null && !currentDuplicateTask.isDone()) {
            currentDuplicateTask.cancel(true);
        }
        if (currentSimilarFilesTask != null && !currentSimilarFilesTask.isDone()) {
            currentSimilarFilesTask.cancel(true);
        }
        
        // Clear data lists
        duplicatesData.clear();
        comparisonData.clear();
        
        // Hide progress indicators
        progressIndicator.setVisible(false);
        progressBar.setVisible(false);
        cancelButton.setVisible(false);
        
        logger.info("DuplicateManagerView: All data reset due to database change");
    }
    
    /**
     * Shows a message when the database is empty.
     */
    private void showEmptyDatabaseMessage() {
        Platform.runLater(() -> {
            statusLabel.setText("No music files in database yet - import files to detect duplicates");
        });
    }
}