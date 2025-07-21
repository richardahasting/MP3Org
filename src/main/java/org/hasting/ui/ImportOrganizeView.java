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
import javafx.scene.control.ScrollPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.hasting.model.MusicFile;
import org.hasting.model.PathTemplate;
import org.hasting.util.DatabaseManager;
import org.hasting.util.MusicFileScanner;
import org.hasting.util.PathTemplateManager;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

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
    
    private static final Logger logger = Log4Rich.getLogger(ImportOrganizeView.class);
    
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
        private javafx.beans.property.StringProperty path;
        private javafx.beans.property.BooleanProperty selected;
        private javafx.beans.property.StringProperty status;
        private javafx.beans.property.StringProperty lastScanned;
        private final boolean isOriginalDirectory;
        private final String originalRootPath; // For subdirectories, stores the original root
        
        // Constructor for original directories
        public DirectoryItem(String path) {
            this.path = new SimpleStringProperty(path);
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
            this.status = new SimpleStringProperty("Ready");
            this.lastScanned = new SimpleStringProperty("Never");
            this.isOriginalDirectory = true;
            this.originalRootPath = path;
        }
        
        // Constructor for subdirectories
        public DirectoryItem(String path, String originalRootPath) {
            this.path = new SimpleStringProperty(path);
            this.selected = new javafx.beans.property.SimpleBooleanProperty(true); // Auto-select subdirectories
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
        
        // Disable row selection to avoid confusion with checkbox selection
        table.setRowFactory(tv -> {
            TableRow<DirectoryItem> row = new TableRow<>();
            // Disable row selection highlighting
            row.setOnMouseClicked(event -> {
                // Prevent row selection, only allow checkbox interaction
                event.consume();
            });
            return row;
        });
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
        table.getSelectionModel().clearSelection();
        
        // Selection column
        TableColumn<DirectoryItem, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(60);
        selectCol.setEditable(true);
        
        // Directory path column with visual hierarchy
        TableColumn<DirectoryItem, String> pathCol = new TableColumn<>("Directory Path");
        pathCol.setCellValueFactory(cellData -> cellData.getValue().pathProperty());
        pathCol.setCellFactory(col -> new TableCell<DirectoryItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    DirectoryItem directoryItem = getTableView().getItems().get(getIndex());
                    if (directoryItem.isOriginalDirectory()) {
                        setText(item);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setText("  → " + item); // Indentation with arrow for subdirectories
                        setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 20;");
                    }
                }
            }
        });
        pathCol.setPrefWidth(400);
        
        // Status column
        TableColumn<DirectoryItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(100);
        
        // Last scanned column
        TableColumn<DirectoryItem, String> lastScannedCol = new TableColumn<>("Last Scanned");
        lastScannedCol.setCellValueFactory(cellData -> cellData.getValue().lastScannedProperty());
        lastScannedCol.setPrefWidth(120);
        
        // Browse/Remove column - shows different buttons based on directory type
        TableColumn<DirectoryItem, Void> actionCol = new TableColumn<>("Browse");
        actionCol.setCellFactory(col -> new TableCell<DirectoryItem, Void>() {
            private final Button browseButton = new Button("...");
            private final Button removeButton = new Button("X");
            
            {
                browseButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6;");
                browseButton.setTooltip(new Tooltip("Select subdirectory within this path"));
                
                removeButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6; -fx-background-color: #ff6b6b; -fx-text-fill: white;");
                removeButton.setTooltip(new Tooltip("Remove this subdirectory"));
                
                browseButton.setOnAction(event -> {
                    DirectoryItem item = getTableView().getItems().get(getIndex());
                    selectSubdirectory(item);
                });
                
                removeButton.setOnAction(event -> {
                    DirectoryItem item = getTableView().getItems().get(getIndex());
                    removeSubdirectory(item);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DirectoryItem directoryItem = getTableView().getItems().get(getIndex());
                    if (directoryItem.isOriginalDirectory()) {
                        setGraphic(browseButton);
                    } else {
                        setGraphic(removeButton);
                    }
                }
            }
        });
        actionCol.setPrefWidth(60);
        actionCol.setResizable(false);
        
        table.getColumns().addAll(selectCol, pathCol, statusCol, lastScannedCol, actionCol);
        
        return table;
    }
    
    /**
     * Opens a directory chooser to allow user to select a subdirectory within the given DirectoryItem's path.
     * Creates a new subdirectory entry below the original directory instead of replacing it.
     * 
     * @param directoryItem the original DirectoryItem to browse from (must be original directory)
     * @since 1.0
     */
    private void selectSubdirectory(DirectoryItem directoryItem) {
        // Only allow browsing from original directories
        if (!directoryItem.isOriginalDirectory()) {
            statusLabel.setText("Cannot browse: Can only browse from original directories");
            return;
        }
        
        String currentPath = directoryItem.getPath();
        File currentDirectory = new File(currentPath);
        
        // Check if current directory exists
        if (!currentDirectory.exists() || !currentDirectory.isDirectory()) {
            statusLabel.setText("Cannot browse: Directory does not exist - " + currentPath);
            return;
        }
        
        // Create and configure directory chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Subdirectory to Rescan");
        directoryChooser.setInitialDirectory(currentDirectory);
        
        // Show directory chooser
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        
        if (selectedDirectory != null) {
            String selectedPath = selectedDirectory.getAbsolutePath();
            String originalPath = currentPath;
            
            // Validate that selected directory is within the original path
            if (selectedPath.startsWith(originalPath)) {
                // Check if this subdirectory already exists in the list
                boolean alreadyExists = directoryData.stream()
                    .anyMatch(item -> item.getPath().equals(selectedPath));
                
                if (!alreadyExists) {
                    // Add new subdirectory entry below the original directory
                    addSubdirectoryEntry(directoryItem, selectedPath);
                    statusLabel.setText("Added subdirectory: " + selectedPath);
                } else {
                    statusLabel.setText("Subdirectory already exists: " + selectedPath);
                }
            } else {
                // Selected directory is outside the original path
                statusLabel.setText("Selected directory must be within: " + originalPath);
            }
        }
    }
    
    /**
     * Adds a new subdirectory entry below the given original directory.
     * The subdirectory is automatically selected for immediate rescanning.
     * 
     * @param originalDirectoryItem the original directory item (parent)
     * @param subdirectoryPath the path of the subdirectory to add
     * @since 1.0
     */
    private void addSubdirectoryEntry(DirectoryItem originalDirectoryItem, String subdirectoryPath) {
        // Create new subdirectory item (auto-selected)
        DirectoryItem subdirectoryItem = new DirectoryItem(subdirectoryPath, originalDirectoryItem.getPath());
        
        // Add listener for selection changes
        subdirectoryItem.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            updateRescanButtonState();
        });
        
        // Find the position to insert the subdirectory (right after the original directory)
        int originalIndex = directoryData.indexOf(originalDirectoryItem);
        if (originalIndex != -1) {
            // Insert after the original directory and any existing subdirectories
            int insertIndex = originalIndex + 1;
            
            // Find the correct position (after all existing subdirectories of this original directory)
            while (insertIndex < directoryData.size()) {
                DirectoryItem nextItem = directoryData.get(insertIndex);
                if (nextItem.isOriginalDirectory() || 
                    !nextItem.getOriginalRootPath().equals(originalDirectoryItem.getPath())) {
                    break; // Found next original directory or subdirectory of different parent
                }
                insertIndex++;
            }
            
            directoryData.add(insertIndex, subdirectoryItem);
        } else {
            // Fallback: add at the end
            directoryData.add(subdirectoryItem);
        }
        
        // Update button state since we have a new selected item
        updateRescanButtonState();
        
        logger.info(String.format("Added subdirectory '{}' under original directory '{}'", subdirectoryPath, originalDirectoryItem.getPath()));
    }
    
    /**
     * Removes a subdirectory entry from the list.
     * 
     * @param subdirectoryItem the subdirectory item to remove (must not be original directory)
     * @since 1.0
     */
    private void removeSubdirectory(DirectoryItem subdirectoryItem) {
        if (subdirectoryItem.isOriginalDirectory()) {
            statusLabel.setText("Cannot remove: This is an original directory");
            return;
        }
        
        directoryData.remove(subdirectoryItem);
        updateRescanButtonState();
        statusLabel.setText("Removed subdirectory: " + subdirectoryItem.getPath());
        
        logger.info(String.format("Removed subdirectory '{}'", subdirectoryItem.getPath()));
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
            logger.error(String.format("Error loading files for selection: {}", e.getMessage()), e);
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
        setPadding(new Insets(25, 30, 25, 30)); // Increased padding for better spacing
        
        // Import section with enhanced styling
        VBox importSection = createStyledSection();
        
        Label importTitle = createSectionTitle("Import Music Files");
        
        Label importInstructions = createInstructionLabel(
            "Select directories containing music files to scan and add to the database."
        );
        
        HBox importButtons = createButtonContainer();
        importButtons.getChildren().addAll(scanButton, clearDatabaseButton);
        
        Label selectedLabel = createFieldLabel("Selected Directories:");
        
        // Enhanced directory area styling
        selectedDirectoriesArea.setStyle(
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 5; " +
            "-fx-padding: 10; " +
            "-fx-background-color: #fafafa;"
        );
        
        importSection.getChildren().addAll(
            importTitle,
            createVerticalSpacer(8),
            importInstructions,
            createVerticalSpacer(12),
            importButtons,
            createVerticalSpacer(15),
            selectedLabel,
            createVerticalSpacer(5),
            selectedDirectoriesArea
        );
        
        // Directory management section with enhanced styling
        VBox directorySection = createStyledSection();
        
        Label directoryTitle = createSectionTitle("Rescan Directories");
        
        Label directoryInstructions = createInstructionLabel(
            "Manage previously scanned directories and selectively rescan specific directories for updated files."
        );
        
        HBox directoryControlsBox = createButtonContainer();
        directoryControlsBox.getChildren().addAll(
            selectAllDirectoriesCheckBox, rescanSelectedButton
        );
        
        // Enhanced table styling
        directoryTable.setStyle(
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 5; " +
            "-fx-background-color: white;"
        );
        
        directorySection.getChildren().addAll(
            directoryTitle,
            createVerticalSpacer(8),
            directoryInstructions,
            createVerticalSpacer(12),
            directoryControlsBox,
            createVerticalSpacer(15),
            directoryTable
        );
        
        // File selection section with enhanced styling
        VBox fileSelectionSection = createStyledSection();
        
        Label selectionTitle = createSectionTitle("Select Files to Organize");
        
        Label selectionInstructions = createInstructionLabel(
            "Choose which music files to organize. You can select individual files or use 'Select All'."
        );
        
        HBox selectionControlsBox = createButtonContainer();
        // Add selection count with better styling
        selectionCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        selectionControlsBox.getChildren().addAll(
            selectAllCheckBox, refreshSelectionButton, 
            createHorizontalSpacer(), selectionCountLabel
        );
        
        // Enhanced table styling
        fileSelectionTable.setStyle(
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 5; " +
            "-fx-background-color: white;"
        );
        
        fileSelectionSection.getChildren().addAll(
            selectionTitle,
            createVerticalSpacer(8),
            selectionInstructions,
            createVerticalSpacer(12),
            selectionControlsBox,
            createVerticalSpacer(15),
            fileSelectionTable
        );
        
        // Organize section with enhanced styling
        VBox organizeSection = createStyledSection();
        
        Label organizeTitle = createSectionTitle("Organize Selected Files");
        
        Label organizeInstructions = createInstructionLabel(
            "Copy selected music files to a new organized folder structure.\n" +
            "Files will be organized using the template configured in Settings."
        );
        
        HBox organizeFolderBox = createButtonContainer();
        Label destinationLabel = createFieldLabel("Destination:");
        destinationLabel.setMinWidth(80);
        
        // Enhanced folder field styling
        organizeFolderField.setStyle(
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 3; " +
            "-fx-padding: 8; " +
            "-fx-background-color: white;"
        );
        HBox.setHgrow(organizeFolderField, Priority.ALWAYS);
        
        organizeFolderBox.getChildren().addAll(
            destinationLabel, organizeFolderField, browseFolderButton
        );
        
        // Enhanced organize button styling
        organizeButton.setStyle(
            "-fx-background-color: #4CAF50; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12 24; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5;"
        );
        
        organizeSection.getChildren().addAll(
            organizeTitle,
            createVerticalSpacer(8),
            organizeInstructions,
            createVerticalSpacer(12),
            organizeFolderBox,
            createVerticalSpacer(15),
            organizeButton
        );
        
        // Progress section - use plain VBox to avoid visible styling when hidden
        VBox progressSection = new VBox(8);
        progressSection.setPadding(new Insets(10, 25, 10, 25));
        
        // Enhanced progress bar styling
        progressBar.setStyle(
            "-fx-accent: #2196F3; " +
            "-fx-background-color: #f0f0f0; " +
            "-fx-border-radius: 3; " +
            "-fx-background-radius: 3;"
        );
        
        // Enhanced progress label styling
        progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        progressSection.getChildren().addAll(progressBar, progressLabel);
        
        // Bind progress section visibility to progress bar visibility
        progressSection.visibleProperty().bind(progressBar.visibleProperty());
        progressSection.managedProperty().bind(progressBar.visibleProperty());
        
        // Status section with enhanced styling
        HBox statusSection = new HBox();
        statusSection.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #333;"
        );
        statusSection.getChildren().add(statusLabel);
        
        // Main layout with improved spacing and separators
        VBox mainContent = new VBox();
        mainContent.setSpacing(25); // Increased spacing between sections
        
        mainContent.getChildren().addAll(
            importSection, 
            createStyledSeparator(), 
            directorySection,
            createStyledSeparator(), 
            fileSelectionSection, 
            createStyledSeparator(), 
            organizeSection
        );
        
        // Enhanced scroll pane for main content
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        setCenter(scrollPane);
        setBottom(new VBox(15, progressSection, statusSection));
        
        // Enhanced growth behavior for responsive design
        VBox.setVgrow(selectedDirectoriesArea, Priority.ALWAYS);
        VBox.setVgrow(directoryTable, Priority.ALWAYS);
        VBox.setVgrow(fileSelectionTable, Priority.ALWAYS);
        
        // Set minimum heights for better UX
        selectedDirectoriesArea.setMinHeight(100);
        directoryTable.setMinHeight(150);
        fileSelectionTable.setMinHeight(200);
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
        
        // Record the original scan directories for rescanning functionality
        for (String directory : directories) {
            if (!directory.trim().isEmpty()) {
                DatabaseManager.recordScanDirectory(directory.trim());
            }
        }
        
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
                        logger.error("Batch save failed, falling back to individual saves");
                        
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
                        refreshDirectoryTable(); // Refresh directory rescanning table after import
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
                    logger.error(String.format("Import operation failed: {}", errorMessage), getException());
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
                        logger.debug(String.format("Copying file: {} -> {}", sourcePath, destinationFilePath));
                        
                        successfulCopies++;
                        
                    } catch (Exception e) {
                        logger.error(String.format("Failed to copy file: %s - %s", musicFile.getFilePath(), e.getMessage()), e);
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
                    logger.error(String.format("File organization failed: %s", getException().getMessage()), getException());
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
                    logger.error(String.format("Error clearing database: {}", e.getMessage()), e);
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
     * Loads previously scanned directories from the database.
     * 
     * <p>This method populates the directory rescanning table with directories
     * that actually contain music files in the database, providing users with
     * a meaningful list of directories they can selectively rescan.
     */
    private void loadPreviouslyScannedDirectories() {
        try {
            // Get original scan directories from the database
            List<String> directories = DatabaseManager.getScanDirectories();
            
            // Clear existing data
            directoryData.clear();
            
            // Add directories to the table
            for (String directory : directories) {
                DirectoryItem item = new DirectoryItem(directory);
                
                // Set status based on file existence
                java.io.File dir = new java.io.File(directory);
                if (dir.exists() && dir.isDirectory()) {
                    item.setStatus("Ready");
                    item.setLastScanned("Previously scanned");
                } else {
                    item.setStatus("Directory not found");
                    item.setLastScanned("Directory missing");
                }
                
                // Add listener to update button state when checkbox selection changes
                item.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    updateRescanButtonState();
                });
                
                directoryData.add(item);
            }
            
            logger.info(String.format("Loaded {} directories for rescanning from database", directories.size()));
            
        } catch (Exception e) {
            logger.error("Failed to load directories from database");
            statusLabel.setText("Error loading directories from database: " + e.getMessage());
        }
    }
    
    /**
     * Refreshes the directory rescanning table with current data from the database.
     * 
     * <p>This method should be called after scanning operations to ensure the
     * directory table reflects the current state of the database.
     */
    public void refreshDirectoryTable() {
        loadPreviouslyScannedDirectories();
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
                        
                        // Update status and rescan timestamp
                        Platform.runLater(() -> item.setStatus("Scanned"));
                        DatabaseManager.updateScanDirectoryRescanTime(item.getPath());
                        
                    } catch (Exception e) {
                        Platform.runLater(() -> item.setStatus("Scan Error"));
                        logger.error(String.format("Failed to scan directory: {}", item.getPath()), e);
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
                            logger.info(String.format("Batch inserted {} new files during rescan", savedNew));
                        }
                        
                        // Handle existing files individually (upsert operations)
                        for (MusicFile musicFile : existingFiles) {
                            if (isCancelled()) break;
                            
                            try {
                                DatabaseManager.saveOrUpdateMusicFile(musicFile);
                                totalProcessed++;
                            } catch (Exception e) {
                                logger.error(String.format("Failed to update music file: {}", musicFile.getFilePath()), e);
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
                        logger.error("Failed to save rescanned files");
                    }
                }
                
                return totalProcessed;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Re-enable buttons
                    rescanSelectedButton.setDisable(false);
                    scanButton.setDisable(false);
                    clearDatabaseButton.setDisable(false);
                    organizeButton.setDisable(false);
                    
                    progressBar.setVisible(false);
                    progressLabel.setText("");
                    
                    int processed = getValue();
                    statusLabel.setText("Rescan completed: " + processed + " files processed");
                    
                    // Refresh file selection to show updated data
                    refreshFileSelection();
                    
                    // Refresh directory rescanning table after rescan
                    refreshDirectoryTable();
                    
                    // Update organizeButton state
                    organizeButton.setDisable(DatabaseManager.getMusicFileCount() <= 0);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    // Re-enable buttons
                    rescanSelectedButton.setDisable(false);
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
    
    // ================================================================================================
    // UI STYLING AND LAYOUT HELPER METHODS (Issue #45)
    // ================================================================================================
    
    /**
     * Creates a styled section container with consistent padding and styling.
     * 
     * @return a VBox configured with standard section styling
     */
    private VBox createStyledSection() {
        VBox section = new VBox();
        section.setSpacing(10);
        section.setPadding(new Insets(20, 25, 20, 25));
        section.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #e0e0e0; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);"
        );
        return section;
    }
    
    /**
     * Creates a styled section title with consistent formatting.
     * 
     * @param text the title text
     * @return a Label with title styling
     */
    private Label createSectionTitle(String text) {
        Label title = new Label(text);
        title.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1976D2; " +
            "-fx-padding: 0 0 5 0;"
        );
        return title;
    }
    
    /**
     * Creates a styled instruction label with consistent formatting.
     * 
     * @param text the instruction text
     * @return a Label with instruction styling
     */
    private Label createInstructionLabel(String text) {
        Label instruction = new Label(text);
        instruction.setWrapText(true);
        instruction.setStyle(
            "-fx-font-size: 13px; " +
            "-fx-text-fill: #666; " +
            "-fx-line-spacing: 2px;"
        );
        return instruction;
    }
    
    /**
     * Creates a styled field label with consistent formatting.
     * 
     * @param text the label text
     * @return a Label with field label styling
     */
    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #333;"
        );
        return label;
    }
    
    /**
     * Creates a styled button container with consistent spacing.
     * 
     * @return an HBox configured for button layout
     */
    private HBox createButtonContainer() {
        HBox container = new HBox();
        container.setSpacing(12);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return container;
    }
    
    /**
     * Creates a vertical spacer for consistent spacing between components.
     * 
     * @param height the height of the spacer in pixels
     * @return a Region configured as a vertical spacer
     */
    private Region createVerticalSpacer(double height) {
        Region spacer = new Region();
        spacer.setPrefHeight(height);
        spacer.setMaxHeight(height);
        spacer.setMinHeight(height);
        return spacer;
    }
    
    /**
     * Creates a horizontal spacer that grows to fill available space.
     * 
     * @return a Region configured as a horizontal spacer
     */
    private Region createHorizontalSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    
    /**
     * Creates a styled separator line for visual section separation.
     * 
     * @return a Separator with enhanced styling
     */
    private Separator createStyledSeparator() {
        Separator separator = new Separator();
        separator.setStyle(
            "-fx-background-color: #e0e0e0; " +
            "-fx-border-color: #e0e0e0; " +
            "-fx-opacity: 0.8;"
        );
        separator.setPadding(new Insets(10, 0, 10, 0));
        return separator;
    }
}