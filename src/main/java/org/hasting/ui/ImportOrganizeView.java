package org.hasting.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.hasting.util.MusicFileScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImportOrganizeView extends BorderPane {
    
    private TextArea selectedDirectoriesArea;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label statusLabel;
    private Button scanButton;
    private Button organizeButton;
    private Button clearDatabaseButton;
    private TextField organizeFolderField;
    private Button browseFolderButton;
    
    public ImportOrganizeView() {
        initializeComponents();
        layoutComponents();
    }
    
    private void initializeComponents() {
        selectedDirectoriesArea = new TextArea();
        selectedDirectoriesArea.setPromptText("Selected directories will appear here...");
        selectedDirectoriesArea.setPrefRowCount(4);
        selectedDirectoriesArea.setEditable(false);
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setVisible(false);
        
        progressLabel = new Label("");
        statusLabel = new Label("Ready");
        
        scanButton = new Button("Add Directories to Scan");
        scanButton.setOnAction(e -> selectDirectoriesToScan());
        
        organizeButton = new Button("Organize Music Files");
        organizeButton.setOnAction(e -> organizeFiles());
        organizeButton.setDisable(true);
        
        clearDatabaseButton = new Button("Clear Database");
        clearDatabaseButton.setOnAction(e -> clearDatabase());
        clearDatabaseButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
        
        organizeFolderField = new TextField();
        organizeFolderField.setPromptText("Select destination folder for organized files...");
        organizeFolderField.setPrefWidth(300);
        
        browseFolderButton = new Button("Browse");
        browseFolderButton.setOnAction(e -> selectOrganizeFolder());
    }
    
    private void layoutComponents() {
        setPadding(new Insets(20));
        
        // Import section
        VBox importSection = new VBox(10);
        
        Label importTitle = new Label("Import Music Files");
        importTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label importInstructions = new Label(
            "Select directories containing music files to scan and add to the database."
        );
        importInstructions.setWrapText(true);
        
        HBox importButtons = new HBox(10);
        importButtons.getChildren().addAll(scanButton, clearDatabaseButton);
        
        Label selectedLabel = new Label("Selected Directories:");
        selectedLabel.setStyle("-fx-font-weight: bold;");
        
        importSection.getChildren().addAll(
            importTitle,
            importInstructions,
            importButtons,
            selectedLabel,
            selectedDirectoriesArea
        );
        
        // Organize section
        VBox organizeSection = new VBox(10);
        
        Label organizeTitle = new Label("Organize Music Files");
        organizeTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label organizeInstructions = new Label(
            "Copy all music files from the database to a new organized folder structure.\n" +
            "Files will be organized as: Artist/Album/TrackNumber-Title.ext"
        );
        organizeInstructions.setWrapText(true);
        
        HBox organizeFolderBox = new HBox(10);
        organizeFolderBox.getChildren().addAll(
            new Label("Destination:"), organizeFolderField, browseFolderButton
        );
        
        organizeSection.getChildren().addAll(
            organizeTitle,
            organizeInstructions,
            organizeFolderBox,
            organizeButton
        );
        
        // Progress section
        VBox progressSection = new VBox(5);
        progressSection.getChildren().addAll(progressBar, progressLabel);
        
        // Status section
        HBox statusSection = new HBox();
        statusSection.getChildren().add(statusLabel);
        
        // Main layout
        VBox mainContent = new VBox(20);
        mainContent.getChildren().addAll(importSection, new Separator(), organizeSection);
        
        setCenter(mainContent);
        setBottom(new VBox(10, progressSection, statusSection));
        
        // Make text area grow
        VBox.setVgrow(selectedDirectoriesArea, Priority.ALWAYS);
    }
    
    private void selectDirectoriesToScan() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Music Directories");
        
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory != null) {
            String currentText = selectedDirectoriesArea.getText();
            if (!currentText.isEmpty()) {
                currentText += "\n";
            }
            currentText += selectedDirectory.getAbsolutePath();
            selectedDirectoriesArea.setText(currentText);
            
            // Start scanning immediately
            scanDirectories();
        }
    }
    
    private void scanDirectories() {
        String directoriesText = selectedDirectoriesArea.getText().trim();
        if (directoriesText.isEmpty()) {
            statusLabel.setText("No directories selected");
            return;
        }
        
        String[] directories = directoriesText.split("\n");
        
        // Create enhanced progress dialog
        ImportProgressDialog progressDialog = new ImportProgressDialog(getScene().getWindow());
        progressDialog.setOnCancel(() -> {
            // Handle cancellation - this will be passed to the scanner
        });
        
        // Disable buttons during scan
        scanButton.setDisable(true);
        clearDatabaseButton.setDisable(true);
        organizeButton.setDisable(true);
        
        progressBar.setVisible(true);
        progressLabel.setText("Scanning directories...");
        statusLabel.setText("Scanning in progress...");
        
        // Show progress dialog
        progressDialog.show();
        progressDialog.addLogEntry("Starting import process...");
        
        Task<List<MusicFile>> scanTask = new Task<List<MusicFile>>() {
            @Override
            protected List<MusicFile> call() throws Exception {
                // Create scanner with enhanced progress callbacks
                MusicFileScanner scanner = new MusicFileScanner();
                
                // Set up progress callbacks
                scanner.setDetailedProgressCallback(progress -> {
                    if (!progressDialog.isCancelled()) {
                        progressDialog.updateProgress(progress);
                    }
                });
                
                scanner.setFileProcessingCallback(message -> {
                    if (!progressDialog.isCancelled()) {
                        progressDialog.addLogEntry(message);
                    }
                });
                
                // Handle cancellation
                progressDialog.setOnCancel(() -> {
                    scanner.requestStop();
                });
                
                // Use enhanced scanning method
                List<String> directoryList = java.util.Arrays.asList(directories);
                List<MusicFile> allMusicFiles = scanner.findAllMusicFilesWithProgress(directoryList);
                
                if (progressDialog.isCancelled()) {
                    return new ArrayList<>(); // Return empty list if cancelled
                }
                
                // Stage 3: Save files to database with progress
                int totalFiles = allMusicFiles.size();
                int savedFiles = 0;
                
                for (MusicFile musicFile : allMusicFiles) {
                    if (progressDialog.isCancelled()) {
                        break;
                    }
                    
                    try {
                        DatabaseManager.saveMusicFile(musicFile);
                        savedFiles++;
                        
                        // Update saving progress
                        progressDialog.updateSavingProgress(
                            musicFile.getArtist(),
                            musicFile.getAlbum(), 
                            musicFile.getTitle(),
                            savedFiles,
                            totalFiles
                        );
                        
                    } catch (Exception e) {
                        // Skip duplicates but log them
                        progressDialog.addLogEntry("Skipped (duplicate or error): " + musicFile.getTitle() + " - " + e.getMessage());
                    }
                }
                
                return allMusicFiles;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<MusicFile> result = getValue();
                    
                    scanButton.setDisable(false);
                    clearDatabaseButton.setDisable(false);
                    organizeButton.setDisable(false);
                    progressBar.setVisible(false);
                    progressLabel.setText("");
                    
                    if (progressDialog.isCancelled()) {
                        statusLabel.setText("Import cancelled by user");
                        progressDialog.setCompleted(false, "Import was cancelled by user");
                    } else {
                        statusLabel.setText("Import completed: " + result.size() + " files processed");
                        progressDialog.setCompleted(true, "Successfully imported " + result.size() + " music files");
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    scanButton.setDisable(false);
                    clearDatabaseButton.setDisable(false);
                    organizeButton.setDisable(false);
                    progressBar.setVisible(false);
                    progressLabel.setText("");
                    
                    String errorMessage = getException().getMessage();
                    statusLabel.setText("Import failed: " + errorMessage);
                    progressDialog.setCompleted(false, errorMessage);
                    getException().printStackTrace();
                });
            }
        };
        
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }
    
    private void selectOrganizeFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Destination Folder");
        
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory != null) {
            organizeFolderField.setText(selectedDirectory.getAbsolutePath());
            organizeButton.setDisable(false);
        }
    }
    
    private void organizeFiles() {
        String destinationPath = organizeFolderField.getText().trim();
        if (destinationPath.isEmpty()) {
            statusLabel.setText("Please select a destination folder");
            return;
        }
        
        File destinationDir = new File(destinationPath);
        if (!destinationDir.exists() || !destinationDir.isDirectory()) {
            statusLabel.setText("Invalid destination folder");
            return;
        }
        
        // Disable buttons during organization
        scanButton.setDisable(true);
        clearDatabaseButton.setDisable(true);
        organizeButton.setDisable(true);
        
        progressBar.setVisible(true);
        progressLabel.setText("Organizing files...");
        statusLabel.setText("Organization in progress...");
        
        Task<Void> organizeTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
                int totalFiles = allFiles.size();
                int processedFiles = 0;
                int successfulCopies = 0;
                
                for (MusicFile musicFile : allFiles) {
                    try {
                        Platform.runLater(() -> 
                            progressLabel.setText("Copying: " + musicFile.getTitle())
                        );
                        
                        musicFile.copyToNewLocation(destinationPath);
                        successfulCopies++;
                        
                    } catch (Exception e) {
                        System.err.println("Failed to copy file: " + musicFile.getFilePath() + 
                                         " - " + e.getMessage());
                    }
                    
                    processedFiles++;
                    final double progress = (double) processedFiles / totalFiles;
                    final int copiedSoFar = successfulCopies;
                    Platform.runLater(() -> 
                        updateProgressBar(progress, copiedSoFar + " files copied")
                    );
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    scanButton.setDisable(false);
                    clearDatabaseButton.setDisable(false);
                    organizeButton.setDisable(false);
                    progressBar.setVisible(false);
                    progressLabel.setText("");
                    statusLabel.setText("File organization completed");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    scanButton.setDisable(false);
                    clearDatabaseButton.setDisable(false);
                    organizeButton.setDisable(false);
                    progressBar.setVisible(false);
                    progressLabel.setText("");
                    statusLabel.setText("Organization failed: " + getException().getMessage());
                    getException().printStackTrace();
                });
            }
        };
        
        Thread organizeThread = new Thread(organizeTask);
        organizeThread.setDaemon(true);
        organizeThread.start();
    }
    
    private void clearDatabase() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Database");
        alert.setHeaderText("Clear All Music Files from Database");
        alert.setContentText("This will remove all music file records from the database. " +
                           "The actual files will not be deleted. Are you sure?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DatabaseManager.deleteAllMusicFiles();
                    selectedDirectoriesArea.clear();
                    organizeFolderField.clear();
                    organizeButton.setDisable(true);
                    statusLabel.setText("Database cleared successfully");
                } catch (Exception e) {
                    statusLabel.setText("Error clearing database: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void updateProgressBar(double progress, String message) {
        progressBar.setProgress(progress);
        if (!message.isEmpty()) {
            progressLabel.setText(message);
        }
    }
}