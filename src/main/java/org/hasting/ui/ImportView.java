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
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.hasting.util.MusicFileScanner;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User interface for importing music files from directories into the application database.
 * 
 * <p>This view provides comprehensive file import functionality including:
 * <ul>
 * <li>Directory scanning for supported audio file types</li>
 * <li>Recursive directory scanning with progress tracking</li>
 * <li>Database management and clearing operations</li>
 * <li>Selective rescanning of previously imported directories</li>
 * <li>Progress tracking for long-running operations</li>
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
 * @see MusicFileScanner for directory scanning implementation
 * @see DatabaseManager for data persistence
 * @since 2.0
 */
public class ImportView extends BorderPane {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(ImportView.class);
    
    private TextArea selectedDirectoriesArea;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label statusLabel;
    private Button scanButton;
    private Button clearDatabaseButton;
    
    // Selective rescanning components
    private TableView<DirectoryItem> directoryTable;
    private ObservableList<DirectoryItem> directoryData;
    private CheckBox selectAllDirectoriesCheckBox;
    private Button rescanSelectedButton;
    
    /**
     * Creates a new ImportView with initialized components for file importing operations.
     */
    public ImportView() {
        initializeComponents();
        layoutComponents();
        loadPreviouslyScannedDirectories();
    }
    
    /**
     * Helper class to represent directory items for selective rescanning
     */
    public static class DirectoryItem {
        private javafx.beans.property.StringProperty path;
        private javafx.beans.property.BooleanProperty selected;
        private javafx.beans.property.StringProperty status;
        private javafx.beans.property.StringProperty lastScanned;
        private final boolean isOriginalDirectory;
        private final String originalRootPath;
        
        public DirectoryItem(String path) {
            this.path = new SimpleStringProperty(path);
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
            this.status = new SimpleStringProperty("Ready");
            this.lastScanned = new SimpleStringProperty("Never");
            this.isOriginalDirectory = true;
            this.originalRootPath = path;
        }
        
        public DirectoryItem(String path, String originalRootPath) {
            this.path = new SimpleStringProperty(path);
            this.selected = new javafx.beans.property.SimpleBooleanProperty(true);
            this.status = new SimpleStringProperty("Subdirectory");
            this.lastScanned = new SimpleStringProperty("Never");
            this.isOriginalDirectory = false;
            this.originalRootPath = originalRootPath;
        }
        
        public String getPath() { return path.get(); }
        public javafx.beans.property.StringProperty pathProperty() { return path; }
        public void setPath(String path) { this.path.set(path); }
        public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
        
        public javafx.beans.property.StringProperty statusProperty() { return status; }
        public String getStatus() { return status.get(); }
        public void setStatus(String status) { this.status.set(status); }
        
        public javafx.beans.property.StringProperty lastScannedProperty() { return lastScanned; }
        public String getLastScanned() { return lastScanned.get(); }
        public void setLastScanned(String lastScanned) { this.lastScanned.set(lastScanned); }
        
        public boolean isOriginalDirectory() { return isOriginalDirectory; }
        public String getOriginalRootPath() { return originalRootPath; }
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
        
        clearDatabaseButton = new Button("Clear Database");
        clearDatabaseButton.setOnAction(e -> clearDatabase());
        clearDatabaseButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
        
        initializeDirectoryTable();
    }
    
    private void initializeDirectoryTable() {
        directoryData = FXCollections.observableArrayList();
        directoryTable = new TableView<>(directoryData);
        directoryTable.setPrefHeight(200);
        
        // Selection column
        TableColumn<DirectoryItem, Boolean> selectColumn = new TableColumn<>("Select");
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);
        selectColumn.setPrefWidth(60);
        
        // Directory path column
        TableColumn<DirectoryItem, String> pathColumn = new TableColumn<>("Directory Path");
        pathColumn.setCellValueFactory(cellData -> cellData.getValue().pathProperty());
        pathColumn.setPrefWidth(400);
        
        // Status column
        TableColumn<DirectoryItem, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusColumn.setPrefWidth(100);
        
        // Last scanned column
        TableColumn<DirectoryItem, String> lastScannedColumn = new TableColumn<>("Last Scanned");
        lastScannedColumn.setCellValueFactory(cellData -> cellData.getValue().lastScannedProperty());
        lastScannedColumn.setPrefWidth(120);
        
        directoryTable.getColumns().addAll(selectColumn, pathColumn, statusColumn, lastScannedColumn);
        directoryTable.setEditable(true);
        
        selectAllDirectoriesCheckBox = new CheckBox("Select All");
        selectAllDirectoriesCheckBox.setOnAction(e -> toggleSelectAllDirectories());
        
        rescanSelectedButton = new Button("Rescan Selected Directories");
        rescanSelectedButton.setOnAction(e -> rescanSelectedDirectories());
        rescanSelectedButton.setDisable(true);
        
        // Enable/disable rescan button based on selection
        directoryData.addListener((javafx.collections.ListChangeListener<DirectoryItem>) c -> {
            boolean hasSelection = directoryData.stream().anyMatch(DirectoryItem::isSelected);
            rescanSelectedButton.setDisable(!hasSelection);
        });
    }
    
    private void layoutComponents() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        
        // Directory selection section
        VBox directionSelectionSection = UIStyleHelper.createStyledSection(
            "Directory Selection",
            "Choose directories to scan for music files"
        );
        
        HBox scanControls = new HBox(10);
        scanControls.getChildren().addAll(scanButton, clearDatabaseButton);
        
        directionSelectionSection.getChildren().addAll(
            selectedDirectoriesArea,
            scanControls
        );
        
        // Rescan directories section
        VBox rescanSection = UIStyleHelper.createStyledSection(
            "Rescan Directories",
            "Select previously scanned directories to rescan"
        );
        
        HBox rescanControls = new HBox(10);
        rescanControls.getChildren().addAll(selectAllDirectoriesCheckBox, rescanSelectedButton);
        
        rescanSection.getChildren().addAll(
            directoryTable,
            rescanControls
        );
        
        // Progress section
        VBox progressSection = new VBox(5);
        progressSection.getChildren().addAll(progressBar, progressLabel, statusLabel);
        
        mainContainer.getChildren().addAll(
            directionSelectionSection,
            UIStyleHelper.createStyledSeparator(),
            rescanSection,
            UIStyleHelper.createStyledSeparator(),
            progressSection
        );
        
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        setCenter(scrollPane);
    }
    
    private void selectDirectoriesToScan() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directories to Scan");
        
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        List<File> selectedDirectories = selectedDirectory != null ? List.of(selectedDirectory) : null;
        
        if (selectedDirectories != null && !selectedDirectories.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (File dir : selectedDirectories) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(dir.getAbsolutePath());
            }
            selectedDirectoriesArea.setText(sb.toString());
            
            // Start scanning immediately
            scanDirectories(selectedDirectories);
        }
    }
    
    private void scanDirectories(List<File> directories) {
        if (directories == null || directories.isEmpty()) {
            statusLabel.setText("No directories selected");
            return;
        }
        
        scanButton.setDisable(true);
        clearDatabaseButton.setDisable(true);
        progressBar.setVisible(true);
        statusLabel.setText("Scanning directories...");
        
        Task<Void> scanTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                MusicFileScanner scanner = new MusicFileScanner();
                
                updateMessage("Initializing scan...");
                updateProgress(0, 1);
                
                for (int i = 0; i < directories.size(); i++) {
                    File directory = directories.get(i);
                    updateMessage("Scanning: " + directory.getName());
                    
                    try {
                        List<String> directoryPaths = List.of(directory.getAbsolutePath());
                        List<MusicFile> files = scanner.scanMusicFiles(directoryPaths);
                        
                        updateMessage("Processing " + files.size() + " files from " + directory.getName());
                        
                        for (int j = 0; j < files.size(); j++) {
                            if (isCancelled()) return null;
                            
                            MusicFile file = files.get(j);
                            DatabaseManager.saveMusicFile(file);
                            
                            updateProgress((double) (i * 1000 + j) / (directories.size() * files.size()), 1);
                        }
                        
                        // Store the scanned directory
                        DatabaseManager.recordScanDirectory(directory.getAbsolutePath());
                        
                    } catch (Exception e) {
                        logger.error("Error scanning directory: " + directory.getAbsolutePath(), e);
                        updateMessage("Error scanning: " + directory.getName() + " - " + e.getMessage());
                    }
                    
                    updateProgress((double) (i + 1) / directories.size(), 1);
                }
                
                updateMessage("Scan completed successfully");
                return null;
            }
        };
        
        progressLabel.textProperty().bind(scanTask.messageProperty());
        progressBar.progressProperty().bind(scanTask.progressProperty());
        
        scanTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                scanButton.setDisable(false);
                clearDatabaseButton.setDisable(false);
                progressBar.setVisible(false);
                statusLabel.setText("Scan completed successfully");
                loadPreviouslyScannedDirectories();
            });
        });
        
        scanTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                scanButton.setDisable(false);
                clearDatabaseButton.setDisable(false);
                progressBar.setVisible(false);
                statusLabel.setText("Scan failed: " + scanTask.getException().getMessage());
                logger.error("Scan task failed", scanTask.getException());
            });
        });
        
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }
    
    private void clearDatabase() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Database");
        alert.setHeaderText("Are you sure you want to clear the database?");
        alert.setContentText("This will remove all imported music files from the database. This action cannot be undone.");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                DatabaseManager.deleteAllMusicFiles();
                statusLabel.setText("Database cleared successfully");
                loadPreviouslyScannedDirectories();
                selectedDirectoriesArea.clear();
            } catch (Exception e) {
                statusLabel.setText("Error clearing database: " + e.getMessage());
                logger.error("Error clearing database", e);
            }
        }
    }
    
    private void loadPreviouslyScannedDirectories() {
        try {
            List<String> scanDirectories = DatabaseManager.getScanDirectories();
            directoryData.clear();
            
            for (String directory : scanDirectories) {
                DirectoryItem item = new DirectoryItem(directory);
                item.setLastScanned("Previously scanned");
                directoryData.add(item);
            }
            
        } catch (Exception e) {
            logger.error("Error loading previously scanned directories", e);
            statusLabel.setText("Error loading directory list: " + e.getMessage());
        }
    }
    
    private void toggleSelectAllDirectories() {
        boolean selectAll = selectAllDirectoriesCheckBox.isSelected();
        for (DirectoryItem item : directoryData) {
            item.setSelected(selectAll);
        }
    }
    
    private void rescanSelectedDirectories() {
        List<String> selectedPaths = directoryData.stream()
            .filter(DirectoryItem::isSelected)
            .map(DirectoryItem::getPath)
            .collect(java.util.stream.Collectors.toList());
        
        if (selectedPaths.isEmpty()) {
            statusLabel.setText("No directories selected for rescanning");
            return;
        }
        
        List<File> directories = selectedPaths.stream()
            .map(File::new)
            .collect(java.util.stream.Collectors.toList());
        
        scanDirectories(directories);
    }
}