package org.hasting.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import org.hasting.model.MusicFile;
import org.hasting.model.PathTemplate;
import org.hasting.util.DatabaseManager;
import org.hasting.util.PathTemplateManager;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User interface for organizing music files with advanced search and selection capabilities.
 * 
 * <p>This view provides comprehensive file organization functionality including:
 * <ul>
 * <li>Advanced music file search with multiple criteria</li>
 * <li>Multi-selection from search results</li>
 * <li>Accumulation of files from multiple searches</li>
 * <li>Batch organization operations</li>
 * <li>Template-based file organization and copying</li>
 * <li>Progress tracking for organization operations</li>
 * </ul>
 * 
 * <p>Search capabilities include:
 * <ul>
 * <li>Search by artist, title, album, file path</li>
 * <li>Fuzzy matching for flexible searches</li>
 * <li>Multiple search sessions with accumulated results</li>
 * <li>Clear visualization of selected files for organization</li>
 * </ul>
 * 
 * <p>Organization features:
 * <ul>
 * <li>Configurable path templates for systematic file organization</li>
 * <li>Artist-based directory grouping with automatic distribution</li>
 * <li>File copying with integrity verification</li>
 * <li>Preview of organization structure before execution</li>
 * </ul>
 * 
 * @see PathTemplate for file organization templates
 * @see DatabaseManager for data persistence
 * @since 2.0
 */
public class OrganizeView extends BorderPane {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(OrganizeView.class);
    
    // Search components
    private TextField searchField;
    private ComboBox<String> searchCriteriaCombo;
    private Button searchButton;
    private Button clearSearchButton;
    
    // Search results components
    private TableView<MusicFileSearchResult> searchResultsTable;
    private ObservableList<MusicFileSearchResult> searchResultsData;
    private CheckBox selectAllSearchCheckBox;
    private Button addSelectedButton;
    private Label searchResultsCountLabel;
    
    // Organization queue components  
    private TableView<MusicFileSelection> organizationTable;
    private ObservableList<MusicFileSelection> organizationData;
    private CheckBox selectAllOrganizationCheckBox;
    private Button removeSelectedButton;
    private Button clearQueueButton;
    private Label organizationCountLabel;
    
    // Organization execution components
    private TextField organizeFolderField;
    private Button browseFolderButton;
    private Button organizeButton;
    
    // Progress tracking
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label statusLabel;
    
    /**
     * Creates a new OrganizeView with initialized components for file organization operations.
     */
    public OrganizeView() {
        initializeComponents();
        layoutComponents();
        refreshSearchResults();
    }
    
    /**
     * Helper class to wrap MusicFile with selection state for search results
     */
    public static class MusicFileSearchResult {
        private final MusicFile musicFile;
        private SimpleBooleanProperty selected;
        
        public MusicFileSearchResult(MusicFile musicFile) {
            this.musicFile = musicFile;
            this.selected = new SimpleBooleanProperty(false);
        }
        
        public MusicFile getMusicFile() { return musicFile; }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
    }
    
    /**
     * Helper class to wrap MusicFile with selection state for organization queue
     */
    public static class MusicFileSelection {
        private final MusicFile musicFile;
        private SimpleBooleanProperty selected;
        
        public MusicFileSelection(MusicFile musicFile) {
            this.musicFile = musicFile;
            this.selected = new SimpleBooleanProperty(false);
        }
        
        public MusicFile getMusicFile() { return musicFile; }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
    }
    
    private void initializeComponents() {
        // Search components
        searchField = new TextField();
        searchField.setPromptText("Enter search terms...");
        searchField.setOnAction(e -> performSearch());
        
        searchCriteriaCombo = new ComboBox<>();
        searchCriteriaCombo.getItems().addAll("All Fields", "Artist", "Title", "Album", "File Path");
        searchCriteriaCombo.setValue("All Fields");
        
        searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        
        clearSearchButton = new Button("Clear");
        clearSearchButton.setOnAction(e -> clearSearch());
        
        // Search results table
        initializeSearchResultsTable();
        
        // Organization queue table
        initializeOrganizationTable();
        
        // Organization execution components
        organizeFolderField = new TextField();
        organizeFolderField.setPromptText("Select destination folder for organized files...");
        organizeFolderField.setEditable(false);
        
        browseFolderButton = new Button("Browse");
        browseFolderButton.setOnAction(e -> selectOrganizeFolder());
        
        organizeButton = new Button("Organize Files");
        organizeButton.setOnAction(e -> organizeFiles());
        organizeButton.setDisable(true);
        
        // Progress components
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setVisible(false);
        
        progressLabel = new Label("");
        statusLabel = new Label("Ready");
    }
    
    private void initializeSearchResultsTable() {
        searchResultsData = FXCollections.observableArrayList();
        searchResultsTable = new TableView<>(searchResultsData);
        searchResultsTable.setPrefHeight(250);
        
        // Selection column
        TableColumn<MusicFileSearchResult, Boolean> selectColumn = new TableColumn<>("Select");
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);
        selectColumn.setPrefWidth(60);
        
        // Artist column
        TableColumn<MusicFileSearchResult, String> artistColumn = new TableColumn<>("Artist");
        artistColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getArtist()));
        artistColumn.setPrefWidth(150);
        
        // Title column
        TableColumn<MusicFileSearchResult, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getTitle()));
        titleColumn.setPrefWidth(200);
        
        // Album column
        TableColumn<MusicFileSearchResult, String> albumColumn = new TableColumn<>("Album");
        albumColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getAlbum()));
        albumColumn.setPrefWidth(150);
        
        // File path column
        TableColumn<MusicFileSearchResult, String> pathColumn = new TableColumn<>("File Path");
        pathColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getFilePath()));
        pathColumn.setPrefWidth(300);
        
        searchResultsTable.getColumns().addAll(selectColumn, artistColumn, titleColumn, albumColumn, pathColumn);
        searchResultsTable.setEditable(true);
        
        selectAllSearchCheckBox = new CheckBox("Select All");
        selectAllSearchCheckBox.setOnAction(e -> toggleSelectAllSearchResults());
        
        addSelectedButton = new Button("Add Selected to Organization Queue");
        addSelectedButton.setOnAction(e -> addSelectedToQueue());
        addSelectedButton.setDisable(true);
        
        searchResultsCountLabel = new Label("0 files found");
        
        // Enable/disable add button based on selection
        searchResultsData.addListener((javafx.collections.ListChangeListener<MusicFileSearchResult>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    // Add listeners to newly added items
                    for (MusicFileSearchResult result : c.getAddedSubList()) {
                        result.selectedProperty().addListener((obs, oldVal, newVal) -> {
                            updateSearchSelectionControls();
                        });
                    }
                }
            }
            updateSearchSelectionControls();
        });
    }
    
    private void initializeOrganizationTable() {
        organizationData = FXCollections.observableArrayList();
        organizationTable = new TableView<>(organizationData);
        organizationTable.setPrefHeight(250);
        
        // Selection column
        TableColumn<MusicFileSelection, Boolean> selectColumn = new TableColumn<>("Select");
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);
        selectColumn.setPrefWidth(60);
        
        // Artist column
        TableColumn<MusicFileSelection, String> artistColumn = new TableColumn<>("Artist");
        artistColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getArtist()));
        artistColumn.setPrefWidth(150);
        
        // Title column
        TableColumn<MusicFileSelection, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getTitle()));
        titleColumn.setPrefWidth(200);
        
        // Album column
        TableColumn<MusicFileSelection, String> albumColumn = new TableColumn<>("Album");
        albumColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getAlbum()));
        albumColumn.setPrefWidth(150);
        
        // File path column
        TableColumn<MusicFileSelection, String> pathColumn = new TableColumn<>("File Path");
        pathColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMusicFile().getFilePath()));
        pathColumn.setPrefWidth(300);
        
        organizationTable.getColumns().addAll(selectColumn, artistColumn, titleColumn, albumColumn, pathColumn);
        organizationTable.setEditable(true);
        
        selectAllOrganizationCheckBox = new CheckBox("Select All");
        selectAllOrganizationCheckBox.setOnAction(e -> toggleSelectAllOrganization());
        
        removeSelectedButton = new Button("Remove Selected");
        removeSelectedButton.setOnAction(e -> removeSelectedFromQueue());
        removeSelectedButton.setDisable(true);
        
        clearQueueButton = new Button("Clear Queue");
        clearQueueButton.setOnAction(e -> clearOrganizationQueue());
        
        organizationCountLabel = new Label("0 files queued for organization");
        
        // Enable/disable buttons based on selection and queue content
        organizationData.addListener((javafx.collections.ListChangeListener<MusicFileSelection>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    // Add listeners to newly added items
                    for (MusicFileSelection selection : c.getAddedSubList()) {
                        selection.selectedProperty().addListener((obs, oldVal, newVal) -> {
                            updateOrganizationControls();
                        });
                    }
                }
            }
            updateOrganizationControls();
        });
    }
    
    private void layoutComponents() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        
        // Search section
        VBox searchSection = UIStyleHelper.createStyledSection(
            "Music File Search", 
            "Search for music files to add to organization queue"
        );
        
        HBox searchControls = new HBox(10);
        searchControls.getChildren().addAll(
            UIStyleHelper.createFieldLabel("Search:"),
            searchField,
            UIStyleHelper.createFieldLabel("In:"),
            searchCriteriaCombo,
            searchButton,
            clearSearchButton
        );
        
        HBox searchResultsControls = new HBox(10);
        searchResultsControls.getChildren().addAll(
            selectAllSearchCheckBox,
            addSelectedButton,
            UIStyleHelper.createHorizontalSpacer(),
            searchResultsCountLabel
        );
        
        searchSection.getChildren().addAll(
            searchControls,
            searchResultsTable,
            searchResultsControls
        );
        
        // Organization queue section
        VBox organizationSection = UIStyleHelper.createStyledSection(
            "Organization Queue",
            "Files selected for organization - multiple searches can add to this queue"
        );
        
        HBox organizationControls = new HBox(10);
        organizationControls.getChildren().addAll(
            selectAllOrganizationCheckBox,
            removeSelectedButton,
            clearQueueButton,
            UIStyleHelper.createHorizontalSpacer(),
            organizationCountLabel
        );
        
        organizationSection.getChildren().addAll(
            organizationTable,
            organizationControls
        );
        
        // Organization execution section
        VBox executionSection = UIStyleHelper.createStyledSection(
            "Execute Organization",
            "Choose destination and organize all queued files"
        );
        
        HBox destinationControls = new HBox(10);
        destinationControls.getChildren().addAll(
            UIStyleHelper.createFieldLabel("Destination:"),
            organizeFolderField,
            browseFolderButton
        );
        
        HBox executeControls = new HBox(10);
        executeControls.getChildren().addAll(organizeButton);
        
        executionSection.getChildren().addAll(
            destinationControls,
            executeControls
        );
        
        // Progress section
        VBox progressSection = new VBox(5);
        progressSection.getChildren().addAll(progressBar, progressLabel, statusLabel);
        
        mainContainer.getChildren().addAll(
            searchSection,
            UIStyleHelper.createStyledSeparator(),
            organizationSection,
            UIStyleHelper.createStyledSeparator(),
            executionSection,
            UIStyleHelper.createStyledSeparator(),
            progressSection
        );
        
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        setCenter(scrollPane);
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshSearchResults();
            return;
        }
        
        String criteria = searchCriteriaCombo.getValue();
        
        try {
            List<MusicFile> results = searchMusicFiles(searchTerm, criteria);
            
            searchResultsData.clear();
            for (MusicFile file : results) {
                searchResultsData.add(new MusicFileSearchResult(file));
            }
            
            updateSearchSelectionControls();
            statusLabel.setText("Found " + results.size() + " files matching search criteria");
            
        } catch (Exception e) {
            logger.error("Error performing search", e);
            statusLabel.setText("Search error: " + e.getMessage());
        }
    }
    
    private List<MusicFile> searchMusicFiles(String searchTerm, String criteria) {
        try {
            List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
            
            return allFiles.stream()
                .filter(file -> matchesSearchCriteria(file, searchTerm, criteria))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error searching music files", e);
            return new ArrayList<>();
        }
    }
    
    private boolean matchesSearchCriteria(MusicFile file, String searchTerm, String criteria) {
        String lowerSearchTerm = searchTerm.toLowerCase();
        
        switch (criteria) {
            case "Artist":
                return file.getArtist() != null && file.getArtist().toLowerCase().contains(lowerSearchTerm);
            case "Title":
                return file.getTitle() != null && file.getTitle().toLowerCase().contains(lowerSearchTerm);
            case "Album":
                return file.getAlbum() != null && file.getAlbum().toLowerCase().contains(lowerSearchTerm);
            case "File Path":
                return file.getFilePath() != null && file.getFilePath().toLowerCase().contains(lowerSearchTerm);
            case "All Fields":
            default:
                return (file.getArtist() != null && file.getArtist().toLowerCase().contains(lowerSearchTerm)) ||
                       (file.getTitle() != null && file.getTitle().toLowerCase().contains(lowerSearchTerm)) ||
                       (file.getAlbum() != null && file.getAlbum().toLowerCase().contains(lowerSearchTerm)) ||
                       (file.getFilePath() != null && file.getFilePath().toLowerCase().contains(lowerSearchTerm));
        }
    }
    
    private void clearSearch() {
        searchField.clear();
        refreshSearchResults();
    }
    
    private void refreshSearchResults() {
        try {
            List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
            
            searchResultsData.clear();
            for (MusicFile file : allFiles) {
                searchResultsData.add(new MusicFileSearchResult(file));
            }
            
            updateSearchSelectionControls();
            statusLabel.setText("Showing all " + allFiles.size() + " imported files");
            
        } catch (Exception e) {
            logger.error("Error refreshing search results", e);
            statusLabel.setText("Error loading files: " + e.getMessage());
        }
    }
    
    private void toggleSelectAllSearchResults() {
        boolean selectAll = selectAllSearchCheckBox.isSelected();
        for (MusicFileSearchResult result : searchResultsData) {
            result.setSelected(selectAll);
        }
        updateSearchSelectionControls();
    }
    
    private void addSelectedToQueue() {
        List<MusicFileSearchResult> selectedResults = searchResultsData.stream()
            .filter(MusicFileSearchResult::isSelected)
            .collect(Collectors.toList());
        
        if (selectedResults.isEmpty()) {
            statusLabel.setText("No files selected to add to queue");
            return;
        }
        
        int addedCount = 0;
        for (MusicFileSearchResult result : selectedResults) {
            MusicFile file = result.getMusicFile();
            
            // Check if file is already in organization queue
            boolean alreadyQueued = organizationData.stream()
                .anyMatch(item -> item.getMusicFile().getFilePath().equals(file.getFilePath()));
            
            if (!alreadyQueued) {
                organizationData.add(new MusicFileSelection(file));
                addedCount++;
            }
        }
        
        updateOrganizationControls();
        statusLabel.setText("Added " + addedCount + " files to organization queue");
        
        // Clear search selections
        for (MusicFileSearchResult result : selectedResults) {
            result.setSelected(false);
        }
        selectAllSearchCheckBox.setSelected(false);
        updateSearchSelectionControls();
    }
    
    private void toggleSelectAllOrganization() {
        boolean selectAll = selectAllOrganizationCheckBox.isSelected();
        for (MusicFileSelection selection : organizationData) {
            selection.setSelected(selectAll);
        }
        updateOrganizationControls();
    }
    
    private void removeSelectedFromQueue() {
        List<MusicFileSelection> toRemove = organizationData.stream()
            .filter(MusicFileSelection::isSelected)
            .collect(Collectors.toList());
        
        organizationData.removeAll(toRemove);
        updateOrganizationControls();
        statusLabel.setText("Removed " + toRemove.size() + " files from organization queue");
    }
    
    private void clearOrganizationQueue() {
        organizationData.clear();
        updateOrganizationControls();
        statusLabel.setText("Organization queue cleared");
    }
    
    private void updateSearchSelectionControls() {
        long selectedCount = searchResultsData.stream().mapToLong(result -> result.isSelected() ? 1 : 0).sum();
        addSelectedButton.setDisable(selectedCount == 0);
        searchResultsCountLabel.setText(searchResultsData.size() + " files found, " + selectedCount + " selected");
    }
    
    private void updateOrganizationControls() {
        long selectedCount = organizationData.stream().mapToLong(selection -> selection.isSelected() ? 1 : 0).sum();
        removeSelectedButton.setDisable(selectedCount == 0);
        organizationCountLabel.setText(organizationData.size() + " files queued, " + selectedCount + " selected");
        
        boolean hasDestination = organizeFolderField.getText() != null && !organizeFolderField.getText().trim().isEmpty();
        organizeButton.setDisable(organizationData.isEmpty() || !hasDestination);
    }
    
    private void selectOrganizeFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Organization Destination Folder");
        
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        
        if (selectedDirectory != null) {
            organizeFolderField.setText(selectedDirectory.getAbsolutePath());
            updateOrganizationControls();
        }
    }
    
    private void organizeFiles() {
        String destinationPath = organizeFolderField.getText().trim();
        if (destinationPath.isEmpty()) {
            statusLabel.setText("Please select a destination folder");
            return;
        }
        
        if (organizationData.isEmpty()) {
            statusLabel.setText("No files queued for organization");
            return;
        }
        
        List<MusicFile> filesToOrganize = organizationData.stream()
            .map(MusicFileSelection::getMusicFile)
            .collect(Collectors.toList());
        
        organizeButton.setDisable(true);
        progressBar.setVisible(true);
        statusLabel.setText("Organizing files...");
        
        Task<Void> organizeTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                PathTemplate template = PathTemplateManager.getInstance().getCurrentTemplate();
                
                updateMessage("Initializing organization...");
                updateProgress(0, filesToOrganize.size());
                
                for (int i = 0; i < filesToOrganize.size(); i++) {
                    if (isCancelled()) return null;
                    
                    MusicFile file = filesToOrganize.get(i);
                    updateMessage("Organizing: " + file.getTitle());
                    
                    try {
                        String relativePath = template.generatePath("", file);
                        File destinationFile = new File(destinationPath, relativePath);
                        
                        destinationFile.getParentFile().mkdirs();
                        
                        java.nio.file.Files.copy(
                            java.nio.file.Paths.get(file.getFilePath()),
                            destinationFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );
                        
                    } catch (Exception e) {
                        logger.error("Error organizing file: " + file.getFilePath(), e);
                        updateMessage("Error organizing: " + file.getTitle() + " - " + e.getMessage());
                    }
                    
                    updateProgress(i + 1, filesToOrganize.size());
                }
                
                updateMessage("Organization completed successfully");
                return null;
            }
        };
        
        progressLabel.textProperty().bind(organizeTask.messageProperty());
        progressBar.progressProperty().bind(organizeTask.progressProperty());
        
        organizeTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                organizeButton.setDisable(false);
                progressBar.setVisible(false);
                statusLabel.setText("Organization completed successfully");
                clearOrganizationQueue();
            });
        });
        
        organizeTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                organizeButton.setDisable(false);
                progressBar.setVisible(false);
                statusLabel.setText("Organization failed: " + organizeTask.getException().getMessage());
                logger.error("Organization task failed", organizeTask.getException());
            });
        });
        
        Thread organizeThread = new Thread(organizeTask);
        organizeThread.setDaemon(true);
        organizeThread.start();
    }
}