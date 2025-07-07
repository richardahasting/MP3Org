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
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User interface for importing music files and organizing them using configurable templates.
 * 
 * <p>This view provides comprehensive file management functionality including:
 * <ul>
 * <li>Directory scanning for supported audio file types</li>
 * <li>Selective file import with preview and confirmation</li>
 * <li>Template-based file organization and copying</li>
 * <li>Progress tracking for long-running operations</li>
 * <li>Database management (scanning, organizing, clearing)</li>
 * <li>Batch processing with cancellation support</li>
 * </ul>
 * 
 * <p>Import process features:
 * <ul>
 * <li>Recursive directory scanning with file type filtering</li>
 * <li>Metadata extraction and validation during import</li>
 * <li>Duplicate detection and handling</li>
 * <li>Real-time progress feedback with detailed status messages</li>
 * <li>Error handling and recovery for corrupted or inaccessible files</li>
 * </ul>
 * 
 * <p>Organization capabilities include:
 * <ul>
 * <li>Configurable path templates for systematic file organization</li>
 * <li>Artist-based directory grouping with automatic distribution</li>
 * <li>File copying with integrity verification</li>
 * <li>Selective processing based on user-defined criteria</li>
 * <li>Preview of organization structure before execution</li>
 * </ul>
 * 
 * <p>The interface supports both single-directory and multi-directory operations,
 * with comprehensive progress tracking and user feedback throughout all processes.
 * 
 * @see MusicFileScanner for directory scanning implementation
 * @see PathTemplate for file organization templates
 * @see DatabaseManager for data persistence
 * @since 1.0
 */
public class ImportOrganizeView extends BorderPane {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(ImportOrganizeView.class);
    
    private TextArea selectedDirectoriesArea;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label statusLabel;
    private Button scanButton;
    private Button organizeButton;
    private Button clearDatabaseButton;
    private TextField organizeFolderField;
    private Button browseFolderButton;
    
    // Selective rescanning components
    private TableView<DirectoryItem> directoryTable;
    private ObservableList<DirectoryItem> directoryData;
    private CheckBox selectAllDirectoriesCheckBox;
    private Button rescanSelectedButton;
    private Button addNewDirectoryButton;
    
    // File selection components for Issue #6
    private TableView<MusicFileSelection> fileSelectionTable;
    private ObservableList<MusicFileSelection> fileSelectionData;
    private CheckBox selectAllCheckBox;
    private Button refreshSelectionButton;
    private Label selectionCountLabel;
    
    /**
     * Creates a new ImportOrganizeView with initialized components for file
     * importing and organization operations.
     * 
     * <p>The view is immediately ready for user interaction with all necessary
     * components initialized and properly laid out.
     */
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
    
    /**
     * Helper class to represent directory items for selective rescanning (Issue #28)
     */
    public static class DirectoryItem {
        private final String path;
        private javafx.beans.property.BooleanProperty selected;
        private javafx.beans.property.StringProperty status;
        private javafx.beans.property.StringProperty lastScanned;
        
        public DirectoryItem(String path) {
            this.path = path;
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
            this.status = new SimpleStringProperty("Ready");
            this.lastScanned = new SimpleStringProperty("Never");
        }
        
        public String getPath() { return path; }
        public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
        
        public javafx.beans.property.StringProperty statusProperty() { return status; }
        public String getStatus() { return status.get(); }
        public void setStatus(String status) { this.status.set(status); }
        
        public javafx.beans.property.StringProperty lastScannedProperty() { return lastScanned; }
        public String getLastScanned() { return lastScanned.get(); }
        public void setLastScanned(String lastScanned) { this.lastScanned.set(lastScanned); }
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
        
        // Initialize selective directory rescanning components for Issue #28
        initializeDirectorySelectionComponents();
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
    
    /**
     * Initializes components for selective directory rescanning functionality (Issue #28).
     */
    private void initializeDirectorySelectionComponents() {
        directoryData = FXCollections.observableArrayList();
        directoryTable = createDirectorySelectionTable();
        directoryTable.setItems(directoryData);
        
        selectAllDirectoriesCheckBox = new CheckBox("Select All");
        selectAllDirectoriesCheckBox.setOnAction(e -> toggleSelectAllDirectories());
        
        rescanSelectedButton = new Button("Rescan Selected Directories");
        rescanSelectedButton.setOnAction(e -> rescanSelectedDirectories());
        rescanSelectedButton.setDisable(true);
        
        addNewDirectoryButton = new Button("Add New Directory");
        addNewDirectoryButton.setOnAction(e -> addNewDirectory());
        
        // Update button state when selection changes
        directoryData.addListener((javafx.collections.ListChangeListener<DirectoryItem>) change -> {
            updateRescanButtonState();
        });
        
        // Load directories from previous scans
        loadPreviouslyScannedDirectories();
    }
    
    /**
     * Creates the table view for directory selection and management.
     */
    private TableView<DirectoryItem> createDirectorySelectionTable() {
        TableView<DirectoryItem> table = new TableView<>();
        table.setPrefHeight(200);
        table.setEditable(true);
        
        // Selection column
        TableColumn<DirectoryItem, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(60);
        selectCol.setEditable(true);
        
        // Directory path column
        TableColumn<DirectoryItem, String> pathCol = new TableColumn<>("Directory Path");
        pathCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPath()));
        pathCol.setPrefWidth(400);
        
        // Status column
        TableColumn<DirectoryItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(100);
        
        // Last scanned column
        TableColumn<DirectoryItem, String> lastScannedCol = new TableColumn<>("Last Scanned");
        lastScannedCol.setCellValueFactory(cellData -> cellData.getValue().lastScannedProperty());
        lastScannedCol.setPrefWidth(120);
        
        table.getColumns().addAll(selectCol, pathCol, statusCol, lastScannedCol);
        
        // Update button state when selection changes
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateRescanButtonState();
        });
        
        return table;
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
            logger.error("Error loading files for selection: {}", e.getMessage(), e);
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
        
        // Directory management section for Issue #28
        VBox directorySection = new VBox(10);
        
        Label directoryTitle = new Label("Directory Management & Selective Rescanning");
        directoryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label directoryInstructions = new Label(
            "Manage previously scanned directories and selectively rescan specific directories for updated files."
        );
        directoryInstructions.setWrapText(true);
        
        HBox directoryControlsBox = new HBox(10);
        directoryControlsBox.getChildren().addAll(
            selectAllDirectoriesCheckBox, addNewDirectoryButton, rescanSelectedButton
        );
        
        directorySection.getChildren().addAll(
            directoryTitle,
            directoryInstructions,
            directoryControlsBox,
            directoryTable
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
            directorySection,
            new Separator(), 
            fileSelectionSection, 
            new Separator(), 
            organizeSection
        );
        
        setCenter(mainContent);
        setBottom(new VBox(10, progressSection, statusSection));
        
        // Make text area and tables grow
        VBox.setVgrow(selectedDirectoriesArea, Priority.ALWAYS);
        VBox.setVgrow(directoryTable, Priority.ALWAYS);
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
                
                // Stage 3: Save files to database using batch operation for better performance
                int totalFiles = allMusicFiles.size();
                
                if (!progressDialog.isCancelled() && !allMusicFiles.isEmpty()) {
                    try {
                        // Update progress dialog
                        progressDialog.addLogEntry("Saving " + totalFiles + " files to database using batch operation...");
                        
                        // Use batch insert for much better performance
                        int savedFiles = DatabaseManager.saveMusicFilesBatch(allMusicFiles);
                        
                        // Update final progress
                        progressDialog.updateSavingProgress(
                            "Batch Operation",
                            "Database Save", 
                            "Completed",
                            savedFiles,
                            totalFiles);
                            
                        progressDialog.addLogEntry("Successfully saved " + savedFiles + " files to database");
                        
                    } catch (Exception e) {
                        progressDialog.addLogEntry("Batch save error: " + e.getMessage());
                        logger.error("Batch save failed, falling back to individual saves", e);
                        
                        // Fallback to individual saves if batch fails
                        int savedFiles = 0;
                        for (MusicFile musicFile : allMusicFiles) {
                            if (progressDialog.isCancelled()) {
                                break;
                            }
                            
                            try {
                                DatabaseManager.saveMusicFile(musicFile);
                                savedFiles++;
                                
                                // Update saving progress less frequently for fallback
                                if (savedFiles % (totalFiles / 20) == 0) {  // update every 5%
                                    progressDialog.updateSavingProgress(
                                        musicFile.getArtist(),
                                        musicFile.getAlbum(), 
                                        musicFile.getTitle(),
                                        savedFiles,
                                        totalFiles);
                                }
                                
                            } catch (Exception ex) {
                                // Skip duplicates but log them
                                progressDialog.addLogEntry("Skipped (duplicate or error): " + musicFile.getTitle() + " - " + ex.getMessage());
                            }
                        }
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
                    logger.error("Import operation failed: {}", errorMessage, getException());
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
                        logger.debug("Copying file: {} -> {}", sourcePath, destinationFilePath);
                        
                        successfulCopies++;
                        
                    } catch (Exception e) {
                        logger.error("Failed to copy file: {} - {}", musicFile.getFilePath(), e.getMessage(), e);
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
                    logger.error("File organization failed: {}", getException().getMessage(), getException());
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
                    logger.error("Error clearing database: {}", e.getMessage(), e);
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
    
    // ================================================================================================
    // SELECTIVE DIRECTORY RESCANNING METHODS (Issue #28)
    // ================================================================================================
    
    /**
     * Toggles selection state for all directories in the table.
     */
    private void toggleSelectAllDirectories() {
        boolean selectAll = selectAllDirectoriesCheckBox.isSelected();
        for (DirectoryItem item : directoryData) {
            item.setSelected(selectAll);
        }
        updateRescanButtonState();
    }
    
    /**
     * Updates the rescan button state based on directory selections.
     */
    private void updateRescanButtonState() {
        boolean hasSelected = directoryData.stream().anyMatch(DirectoryItem::isSelected);
        rescanSelectedButton.setDisable(!hasSelected);
    }
    
    /**
     * Loads previously scanned directories from the selectedDirectoriesArea.
     */
    private void loadPreviouslyScannedDirectories() {
        String directoriesText = selectedDirectoriesArea.getText().trim();
        if (!directoriesText.isEmpty()) {
            String[] directories = directoriesText.split("\n");
            for (String directory : directories) {
                if (!directory.trim().isEmpty()) {
                    DirectoryItem item = new DirectoryItem(directory.trim());
                    item.setLastScanned("Previously scanned");
                    directoryData.add(item);
                }
            }
        }
    }
    
    /**
     * Adds a new directory to the scanning list.
     */
    private void addNewDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Add");
        
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory != null) {
            String directoryPath = selectedDirectory.getAbsolutePath();
            
            // Check if directory is already in the list
            boolean alreadyExists = directoryData.stream()
                    .anyMatch(item -> item.getPath().equals(directoryPath));
            
            if (!alreadyExists) {
                DirectoryItem item = new DirectoryItem(directoryPath);
                directoryData.add(item);
                
                // Also add to the legacy text area for backward compatibility
                String currentText = selectedDirectoriesArea.getText();
                if (!currentText.isEmpty()) {
                    currentText += "\n";
                }
                currentText += directoryPath;
                selectedDirectoriesArea.setText(currentText);
                
                statusLabel.setText("Added directory: " + directoryPath);
            } else {
                statusLabel.setText("Directory already in list: " + directoryPath);
            }
        }
    }
    
    /**
     * Rescans only the selected directories using the new upsert functionality.
     */
    private void rescanSelectedDirectories() {
        List<DirectoryItem> selectedItems = directoryData.stream()
                .filter(DirectoryItem::isSelected)
                .collect(Collectors.toList());
        
        if (selectedItems.isEmpty()) {
            statusLabel.setText("No directories selected for rescanning");
            return;
        }
        
        // Disable buttons during rescan
        rescanSelectedButton.setDisable(true);
        addNewDirectoryButton.setDisable(true);
        scanButton.setDisable(true);
        clearDatabaseButton.setDisable(true);
        organizeButton.setDisable(true);
        
        progressBar.setVisible(true);
        progressLabel.setText("Rescanning selected directories...");
        statusLabel.setText("Rescan in progress...");
        
        // Update status for selected items
        for (DirectoryItem item : selectedItems) {
            item.setStatus("Scanning...");
        }
        
        Task<Integer> rescanTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                MusicFileScanner scanner = new MusicFileScanner();
                int totalProcessed = 0;
                
                // Set up progress callbacks
                scanner.setDetailedProgressCallback(progress -> {
                    Platform.runLater(() -> {
                        // Calculate progress percentage from the scan progress data
                        double progressPercent = 0.0;
                        if (progress.totalDirectories > 0) {
                            progressPercent = (double) progress.directoriesProcessed / progress.totalDirectories;
                        }
                        progressBar.setProgress(progressPercent);
                        progressLabel.setText(progress.currentFile != null ? progress.currentFile : progress.currentDirectory);
                    });
                });

                DatabaseManager.initAllPathsMap(); // Load all paths for quick lookups  issue#41
                
                // First, collect all files from all selected directories
                List<MusicFile> allFoundFiles = new ArrayList<>();
                List<DirectoryItem> processedItems = new ArrayList<>();
                
                for (DirectoryItem item : selectedItems) {
                    if (isCancelled()) break;
                    
                    Platform.runLater(() -> item.setStatus("Scanning..."));
                    
                    try {
                        // Scan the directory using the correct method
                        List<MusicFile> foundFiles = scanner.findAllMusicFiles(item.getPath());
                        allFoundFiles.addAll(foundFiles);
                        processedItems.add(item);
                        
                        // Update status
                        Platform.runLater(() -> item.setStatus("Scanned"));
                        
                    } catch (Exception e) {
                        Platform.runLater(() -> item.setStatus("Scan Error"));
                        logger.error("Failed to scan directory: {}", item.getPath(), e);
                    }
                }
                
                // Now process all found files using batch operations where possible
                if (!allFoundFiles.isEmpty() && !isCancelled()) {
                    Platform.runLater(() -> {
                        for (DirectoryItem item : processedItems) {
                            if (item.getStatus().equals("Scanned")) {
                                item.setStatus("Saving...");
                            }
                        }
                    });
                    
                    try {
                        // Separate new files from existing files for optimal batch processing
                        List<MusicFile> newFiles = new ArrayList<>();
                        List<MusicFile> existingFiles = new ArrayList<>();
                        
                        for (MusicFile musicFile : allFoundFiles) {
                            if (DatabaseManager.getFileIdByPath(musicFile.getFilePath()) == null) {
                                newFiles.add(musicFile);
                            } else {
                                existingFiles.add(musicFile);
                            }
                        }
                        
                        // Batch insert new files for maximum performance
                        if (!newFiles.isEmpty()) {
                            int savedNew = DatabaseManager.saveMusicFilesBatch(newFiles);
                            totalProcessed += savedNew;
                            logger.info("Batch inserted {} new files during rescan", savedNew);
                        }
                        
                        // Handle existing files individually (upsert operations)
                        for (MusicFile musicFile : existingFiles) {
                            if (isCancelled()) break;
                            
                            try {
                                DatabaseManager.saveOrUpdateMusicFile(musicFile);
                                totalProcessed++;
                            } catch (Exception e) {
                                logger.error("Failed to update music file: {}", musicFile.getFilePath(), e);
                            }
                        }
                        
                        // Update final status and timestamp for all processed items
                        Platform.runLater(() -> {
                            for (DirectoryItem item : processedItems) {
                                if (!item.getStatus().equals("Scan Error")) {
                                    item.setStatus("Completed");
                                    item.setLastScanned(java.time.LocalDateTime.now().format(
                                        java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")));
                                }
                            }
                        });
                        
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            for (DirectoryItem item : processedItems) {
                                if (item.getStatus().equals("Saving...")) {
                                    item.setStatus("Save Error");
                                }
                            }
                        });
                        logger.error("Failed to save rescanned files", e);
                    }
                }
                
                return totalProcessed;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Re-enable buttons
                    rescanSelectedButton.setDisable(false);
                    addNewDirectoryButton.setDisable(false);
                    scanButton.setDisable(false);
                    clearDatabaseButton.setDisable(false);
                    organizeButton.setDisable(false);
                    
                    progressBar.setVisible(false);
                    progressLabel.setText("");
                    
                    int processed = getValue();
                    statusLabel.setText("Rescan completed: " + processed + " files processed");
                    
                    // Refresh file selection to show updated data
                    refreshFileSelection();
                    
                    // Update organizeButton state
                    organizeButton.setDisable(DatabaseManager.getMusicFileCount() <= 0);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    // Re-enable buttons
                    rescanSelectedButton.setDisable(false);
                    addNewDirectoryButton.setDisable(false);
                    scanButton.setDisable(false);
                    clearDatabaseButton.setDisable(false);
                    organizeButton.setDisable(false);
                    
                    progressBar.setVisible(false);
                    progressLabel.setText("");
                    
                    String errorMessage = getException().getMessage();
                    statusLabel.setText("Rescan failed: " + errorMessage);
                    
                    // Reset status for failed items
                    for (DirectoryItem item : selectedItems) {
                        if (item.getStatus().equals("Scanning...")) {
                            item.setStatus("Error");
                        }
                    }
                });
            }
        };
        
        // Run the rescan task in background
        Thread rescanThread = new Thread(rescanTask);
        rescanThread.setDaemon(true);
        rescanThread.start();
    }
}