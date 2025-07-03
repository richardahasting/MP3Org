package org.hasting.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.hasting.util.MusicFileScanner;

/**
 * Enhanced progress dialog for music file import operations with detailed feedback.
 */
public class ImportProgressDialog {
    
    private Stage dialog;
    private ProgressBar overallProgressBar;
    private ProgressBar currentStageProgressBar;
    private Label stageLabel;
    private Label currentFileLabel;
    private Label statisticsLabel;
    private Label detailsLabel;
    private Button cancelButton;
    private TextArea logArea;
    
    // Progress tracking
    private int totalDirectories = 0;
    private int directoriesProcessed = 0;
    private int totalFilesFound = 0;
    private int totalFilesProcessed = 0;
    private int totalFilesSaved = 0;
    private boolean cancelled = false;
    private Runnable onCancelCallback;
    
    // Progress update throttling
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 100; // Update UI at most every 100ms
    private int pendingLogEntries = 0;
    private static final int LOG_BATCH_SIZE = 10; // Only add log entries every 10 updates
    
    public ImportProgressDialog(Window owner) {
        createDialog(owner);
    }
    
    private void createDialog(Window owner) {
        dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Import Progress");
        dialog.setResizable(true);
        dialog.setMinWidth(500);
        dialog.setMinHeight(400);
        
        // Create UI components
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        
        // Title
        Label titleLabel = new Label("Importing Music Files");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Stage information
        stageLabel = new Label("Initializing...");
        stageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Overall progress
        Label overallLabel = new Label("Overall Progress:");
        overallLabel.setStyle("-fx-font-weight: bold;");
        overallProgressBar = new ProgressBar(0);
        overallProgressBar.setPrefWidth(400);
        overallProgressBar.setPrefHeight(20);
        
        // Current stage progress
        Label stageProgressLabel = new Label("Current Stage:");
        stageProgressLabel.setStyle("-fx-font-weight: bold;");
        currentStageProgressBar = new ProgressBar(0);
        currentStageProgressBar.setPrefWidth(400);
        currentStageProgressBar.setPrefHeight(20);
        
        // Current file being processed
        currentFileLabel = new Label("");
        currentFileLabel.setStyle("-fx-font-style: italic;");
        currentFileLabel.setWrapText(true);
        
        // Statistics
        statisticsLabel = new Label("Files found: 0 | Processed: 0 | Saved: 0");
        statisticsLabel.setStyle("-fx-font-family: monospace;");
        
        // Details/status
        detailsLabel = new Label("");
        detailsLabel.setWrapText(true);
        
        // Progress details in a collapsible section
        TitledPane logPane = new TitledPane();
        logPane.setText("Progress Log");
        logPane.setExpanded(false);
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 10px;");
        
        logPane.setContent(logArea);
        
        // Control buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center-right;");
        
        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> handleCancel());
        buttonBox.getChildren().add(cancelButton);
        
        // Layout components
        VBox progressSection = new VBox(5);
        progressSection.getChildren().addAll(
            overallLabel, overallProgressBar,
            stageProgressLabel, currentStageProgressBar
        );
        
        mainLayout.getChildren().addAll(
            titleLabel,
            new Separator(),
            stageLabel,
            progressSection,
            currentFileLabel,
            statisticsLabel,
            detailsLabel,
            logPane,
            buttonBox
        );
        
        // Create scene and show
        Scene scene = new Scene(mainLayout);
        dialog.setScene(scene);
        
        // Handle window close
        dialog.setOnCloseRequest(e -> {
            e.consume(); // Prevent default close
            handleCancel();
        });
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
    
    public void hide() {
        if (dialog != null) {
            dialog.hide();
        }
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setOnCancel(Runnable callback) {
        this.onCancelCallback = callback;
    }
    
    private void handleCancel() {
        cancelled = true;
        cancelButton.setText("Cancelling...");
        cancelButton.setDisable(true);
        
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }
    
    /**
     * Updates progress based on MusicFileScanner.ScanProgress data.
     */
    public void updateProgress(MusicFileScanner.ScanProgress progress) {
        long currentTime = System.currentTimeMillis();
        
        // Throttle UI updates to improve performance
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateTime = currentTime;
        
        Platform.runLater(() -> {
            // Update stage information
            String stageText = switch (progress.stage) {
                case "scanning" -> "Scanning Directories";
                case "reading_tags" -> "Reading Music Tags";
                case "saving" -> "Saving to Database";
                default -> "Processing";
            };
            stageLabel.setText(stageText + " (" + (progress.directoriesProcessed + 1) + "/" + progress.totalDirectories + ")");
            
            // Update overall progress
            double overallProgress = progress.totalDirectories > 0 ? 
                (double) progress.directoriesProcessed / progress.totalDirectories : 0;
            overallProgressBar.setProgress(overallProgress);
            
            // Update current stage progress
            double stageProgress = progress.filesFound > 0 ? 
                (double) progress.filesProcessed / progress.filesFound : 0;
            currentStageProgressBar.setProgress(stageProgress);
            
            // Update current file information
            if (!progress.currentFile.isEmpty()) {
                currentFileLabel.setText("Processing: " + progress.currentFile);
            } else if (!progress.currentDirectory.isEmpty()) {
                currentFileLabel.setText("Directory: " + progress.currentDirectory);
            }
            
            // Update statistics
            totalDirectories = progress.totalDirectories;
            directoriesProcessed = progress.directoriesProcessed;
            totalFilesFound = progress.filesFound;
            totalFilesProcessed = progress.filesProcessed;
            
            updateStatistics();
            
            // Only add log entries periodically to reduce overhead
            pendingLogEntries++;
            if (pendingLogEntries >= LOG_BATCH_SIZE) {
                String logEntry = String.format("[%s] Dir: %d/%d, Files: %d/%d - %s", 
                    progress.stage.toUpperCase(),
                    progress.directoriesProcessed + 1,
                    progress.totalDirectories,
                    progress.filesProcessed,
                    progress.filesFound,
                    progress.currentFile.isEmpty() ? progress.currentDirectory : progress.currentFile
                );
                addLogEntry(logEntry);
                pendingLogEntries = 0;
            }
        });
    }
    
    /**
     * Updates the dialog for database saving stage.
     */
    public void updateSavingProgress(String artist, String album, String title, int saved, int total) {
        Platform.runLater(() -> {
            stageLabel.setText("Saving to Database");
            
            // Update progress
            double progress = total > 0 ? (double) saved / total : 0;
            overallProgressBar.setProgress(progress);
            currentStageProgressBar.setProgress(progress);
            
            // Update current file info
            String fileInfo = String.format("%s - %s - %s", 
                artist != null ? artist : "Unknown Artist",
                album != null ? album : "Unknown Album", 
                title != null ? title : "Unknown Title"
            );
            currentFileLabel.setText("Saving: " + fileInfo);
            
            totalFilesSaved = saved;
            updateStatistics();
            
            // Add to log
            addLogEntry(String.format("SAVED (%d/%d): %s", saved, total, fileInfo));
        });
    }
    
    /**
     * Updates the statistics display.
     */
    private void updateStatistics() {
        String stats = String.format("Directories: %d/%d | Files found: %d | Processed: %d | Saved: %d",
            directoriesProcessed, totalDirectories, totalFilesFound, totalFilesProcessed, totalFilesSaved);
        statisticsLabel.setText(stats);
    }
    
    /**
     * Adds an entry to the progress log.
     */
    public void addLogEntry(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            );
            String logLine = String.format("[%s] %s\n", timestamp, message);
            logArea.appendText(logLine);
            
            // Auto-scroll to bottom
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    /**
     * Marks the import as completed.
     */
    public void setCompleted(boolean success, String message) {
        Platform.runLater(() -> {
            if (success) {
                stageLabel.setText("Import Completed Successfully");
                overallProgressBar.setProgress(1.0);
                currentStageProgressBar.setProgress(1.0);
                currentFileLabel.setText(message);
                detailsLabel.setText("All files have been imported successfully.");
                
                addLogEntry("COMPLETED: Import finished successfully");
                
                // Auto-close after 2 seconds for successful imports
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(2),
                        e -> hide()
                    )
                );
                timeline.play();
                
            } else {
                stageLabel.setText("Import Failed");
                detailsLabel.setText("Error: " + message);
                detailsLabel.setStyle("-fx-text-fill: red;");
                
                // For failed imports, show close button but don't auto-close
                cancelButton.setText("Close");
                cancelButton.setDisable(false);
                cancelButton.setOnAction(e -> hide());
                
                addLogEntry("FAILED: " + message);
            }
        });
    }
}