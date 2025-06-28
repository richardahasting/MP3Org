package org.hasting.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.hasting.model.MusicFile;
import org.hasting.model.PathTemplate;
import org.hasting.util.DatabaseManager;
import org.hasting.util.MusicFileScanner;
import org.hasting.util.PathTemplateManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    
    // File selection components for Issue #6
    private TableView<MusicFileSelection> fileSelectionTable;
    private ObservableList<MusicFileSelection> fileSelectionData;
    private CheckBox selectAllCheckBox;
    private Button refreshSelectionButton;
    private Label selectionCountLabel;
    
    public ImportOrganizeView() {
        initializeComponents();
        layoutComponents();
    }
    
    /**
     * Helper class to wrap MusicFile with selection state for Issue #6
     */
    public static class MusicFileSelection {
        private final MusicFile musicFile;
        private javafx.beans.property.BooleanProperty selected;
        
        public MusicFileSelection(MusicFile musicFile) {
            this.musicFile = musicFile;
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
        }
        
        public MusicFile getMusicFile() { return musicFile; }
        public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
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
        
        // Initialize file selection components for Issue #6
        initializeFileSelectionComponents();
    }
    
    private void initializeFileSelectionComponents() {
        fileSelectionData = FXCollections.observableArrayList();
        fileSelectionTable = createFileSelectionTable();
        fileSelectionTable.setItems(fileSelectionData);
        
        selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setOnAction(e -> toggleSelectAll());
        
        refreshSelectionButton = new Button("Refresh File List");
        refreshSelectionButton.setOnAction(e -> refreshFileSelection());
        
        selectionCountLabel = new Label("0 files selected");
        
        // Load initial file list
        refreshFileSelection();
    }
    
    private TableView<MusicFileSelection> createFileSelectionTable() {
        TableView<MusicFileSelection> table = new TableView<>();
        table.setPrefHeight(300);
        
        // Selection column
        TableColumn<MusicFileSelection, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(60);
        selectCol.setEditable(true);
        
        // Artist column
        TableColumn<MusicFileSelection, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMusicFile().getArtist()));
        artistCol.setPrefWidth(150);
        
        // Title column
        TableColumn<MusicFileSelection, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMusicFile().getTitle()));
        titleCol.setPrefWidth(200);
        
        // Album column
        TableColumn<MusicFileSelection, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMusicFile().getAlbum()));
        albumCol.setPrefWidth(150);
        
        // Duration column
        TableColumn<MusicFileSelection, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> {
            Integer duration = data.getValue().getMusicFile().getDurationSeconds();
            if (duration != null) {
                int minutes = duration / 60;
                int seconds = duration % 60;
                return new SimpleStringProperty(String.format("%d:%02d", minutes, seconds));
            }
            return new SimpleStringProperty("");
        });
        durationCol.setPrefWidth(80);
        
        // File path column
        TableColumn<MusicFileSelection, String> pathCol = new TableColumn<>("File Path");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMusicFile().getFilePath()));
        pathCol.setPrefWidth(400);
        
        table.getColumns().addAll(selectCol, artistCol, titleCol, albumCol, durationCol, pathCol);
        table.setEditable(true);
        
        return table;
    }
    
    private void toggleSelectAll() {
        boolean selectAll = selectAllCheckBox.isSelected();
        for (MusicFileSelection selection : fileSelectionData) {
            selection.setSelected(selectAll);
        }
        updateSelectionCount();
    }
    
    private void refreshFileSelection() {
        try {
            List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
            fileSelectionData.clear();
            
            for (MusicFile file : allFiles) {
                MusicFileSelection selection = new MusicFileSelection(file);
                // Add listener to each selection to update count when changed
                selection.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectionCount());
                fileSelectionData.add(selection);
            }
            
            updateSelectionCount();
            statusLabel.setText("Loaded " + allFiles.size() + " files for selection");
        } catch (Exception e) {
            statusLabel.setText("Error loading files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateSelectionCount() {
        long selectedCount = fileSelectionData.stream()
            .mapToLong(selection -> selection.isSelected() ? 1 : 0)
            .sum();
        selectionCountLabel.setText(selectedCount + " of " + fileSelectionData.size() + " files selected");
        
        // Enable/disable organize button based on selection
        organizeButton.setDisable(selectedCount == 0 || organizeFolderField.getText().trim().isEmpty());
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
        
        // File selection section for Issue #6
        VBox fileSelectionSection = new VBox(10);
        
        Label selectionTitle = new Label("Select Files to Organize");
        selectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label selectionInstructions = new Label(
            "Choose which music files to organize. You can select individual files or use 'Select All'."
        );
        selectionInstructions.setWrapText(true);
        
        HBox selectionControlsBox = new HBox(10);
        selectionControlsBox.getChildren().addAll(
            selectAllCheckBox, refreshSelectionButton, selectionCountLabel
        );
        
        fileSelectionSection.getChildren().addAll(
            selectionTitle,
            selectionInstructions,
            selectionControlsBox,
            fileSelectionTable
        );
        
        // Organize section
        VBox organizeSection = new VBox(10);
        
        Label organizeTitle = new Label("Organize Selected Files");
        organizeTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label organizeInstructions = new Label(
            "Copy selected music files to a new organized folder structure.\n" +
            "Files will be organized using the template configured in Settings."
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
        mainContent.getChildren().addAll(
            importSection, 
            new Separator(), 
            fileSelectionSection, 
            new Separator(), 
            organizeSection
        );
        
        setCenter(mainContent);
        setBottom(new VBox(10, progressSection, statusSection));
        
        // Make text area and file selection table grow
        VBox.setVgrow(selectedDirectoriesArea, Priority.ALWAYS);
        VBox.setVgrow(fileSelectionTable, Priority.ALWAYS);
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
                        refreshFileSelection(); // Refresh file selection after import
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
            updateSelectionCount(); // This will update organize button state based on file selection
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
        
        // Get selected files for Issue #6
        List<MusicFile> selectedFiles = fileSelectionData.stream()
            .filter(MusicFileSelection::isSelected)
            .map(MusicFileSelection::getMusicFile)
            .collect(Collectors.toList());
        
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("Please select files to organize");
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
                // Use selected files instead of all files
                int totalFiles = selectedFiles.size();
                int processedFiles = 0;
                int successfulCopies = 0;
                
                // Get current path template
                PathTemplate currentTemplate = PathTemplateManager.getInstance().getCurrentTemplate();
                
                for (MusicFile musicFile : selectedFiles) {
                    try {
                        Platform.runLater(() -> 
                            progressLabel.setText("Copying: " + musicFile.getTitle())
                        );
                        
                        // Use the configured template for organization
                        String newPath = musicFile.newFileNameAndLocation(destinationPath, currentTemplate);
                        java.nio.file.Path sourcePath = java.nio.file.Paths.get(musicFile.getFilePath());
                        java.nio.file.Path destinationFilePath = java.nio.file.Paths.get(newPath);
                        
                        // Create directories if they do not exist
                        java.nio.file.Files.createDirectories(destinationFilePath.getParent());
                        
                        // Copy the file to the new location
                        java.nio.file.Files.copy(sourcePath, destinationFilePath);
                        System.out.println("Copying file..." + sourcePath + " -> " + destinationFilePath);
                        
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
                    refreshFileSelection(); // Refresh file selection after clearing database
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