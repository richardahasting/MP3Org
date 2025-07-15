package org.hasting.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import org.hasting.model.MusicFile;
import org.hasting.ui.TabSwitchCallback;
import org.hasting.util.DatabaseManager;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.ProfileChangeListener;
import org.hasting.util.ProfileChangeNotifier;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

/**
 * User interface for searching, viewing, and editing music file metadata in the MP3Org application.
 * 
 * <p>This view provides comprehensive metadata management functionality including:
 * <ul>
 * <li>Advanced search capabilities with multiple criteria (title, artist, album, genre)</li>
 * <li>Real-time search results display in sortable, filterable tables</li>
 * <li>Individual file metadata editing with validation</li>
 * <li>Bulk editing operations for multiple selected files</li>
 * <li>File system integration (open files, show in folder)</li>
 * <li>Database synchronization and persistence</li>
 * </ul>
 * 
 * <p>Search functionality supports:
 * <ul>
 * <li>Partial text matching across all metadata fields</li>
 * <li>Case-insensitive search operations</li>
 * <li>Real-time filtering as the user types</li>
 * <li>Multiple search criteria combinations</li>
 * </ul>
 * 
 * <p>Editing capabilities include:
 * <ul>
 * <li>Single file editing with immediate validation feedback</li>
 * <li>Bulk editing for applying changes to multiple files simultaneously</li>
 * <li>Automatic database updates with rollback on errors</li>
 * <li>Change tracking and user confirmation for destructive operations</li>
 * </ul>
 * 
 * <p>The interface is profile-aware and automatically refreshes when the active
 * database profile changes, ensuring users always see current data.
 * 
 * @see MusicFile for the underlying data model
 * @see DatabaseManager#searchMusicFiles(String) for search implementation
 * @see DatabaseManager#updateMusicFile(MusicFile) for persistence
 * @since 1.0
 */
public class MetadataEditorView extends BorderPane implements ProfileChangeListener {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(MetadataEditorView.class);
    
    private TextField searchField;
    private ComboBox<String> searchTypeCombo;
    private TableView<MusicFile> resultsTable;
    private ObservableList<MusicFile> resultsData;
    
    // Edit form fields
    private TextField titleField;
    private TextField artistField;
    private TextField albumField;
    private TextField genreField;
    private TextField trackNumberField;
    private TextField yearField;
    private Label filePathLabel;
    private Label fileSizeLabel;
    private Label bitrateLabel;
    private Label durationLabel;
    
    private MusicFile currentFile;
    private Label statusLabel;
    private TabSwitchCallback tabSwitchCallback;
    
    // Bulk editing components
    private CheckBox bulkEditModeCheckBox;
    private VBox bulkEditSection;
    private TextField bulkArtistField;
    private TextField bulkAlbumField;
    private TextField bulkGenreField;
    private Label bulkSelectionLabel;
    
    public MetadataEditorView() {
        this(null);
    }
    
    public MetadataEditorView(TabSwitchCallback tabSwitchCallback) {
        this.tabSwitchCallback = tabSwitchCallback;
        initializeComponents();
        layoutComponents();
        
        // Register for profile change notifications
        ProfileChangeNotifier.getInstance().addListener(this);
    }
    
    private void initializeComponents() {
        // Search components
        searchField = new TextField();
        searchField.setPromptText("Enter search term...");
        searchField.setPrefWidth(300);
        
        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("Title", "Artist", "Album", "All Fields");
        searchTypeCombo.setValue("All Fields");
        
        // Results table
        resultsData = FXCollections.observableArrayList();
        resultsTable = createResultsTable();
        resultsTable.setItems(resultsData);
        
        // Enable multiple selection
        resultsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Add selection listener
        resultsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                updateSelectionUI();
            }
        );
        
        // Edit form fields
        titleField = new TextField();
        artistField = new TextField();
        albumField = new TextField();
        genreField = new TextField();
        trackNumberField = new TextField();
        yearField = new TextField();
        
        filePathLabel = new Label();
        fileSizeLabel = new Label();
        bitrateLabel = new Label();
        durationLabel = new Label();
        
        statusLabel = new Label("Ready");
        
        // Initialize bulk editing components
        initializeBulkEditingComponents();
        
        // Add search functionality
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        
        // Enter key search
        searchField.setOnAction(e -> performSearch());
        
        // Add keyboard shortcuts
        setupKeyboardShortcuts();
    }
    
    private TableView<MusicFile> createResultsTable() {
        TableView<MusicFile> table = new TableView<>();
        
        TableColumn<MusicFile, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArtist()));
        artistCol.setPrefWidth(120);
        
        TableColumn<MusicFile, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        titleCol.setPrefWidth(180);
        
        TableColumn<MusicFile, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAlbum()));
        albumCol.setPrefWidth(120);
        
        TableColumn<MusicFile, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenre()));
        genreCol.setPrefWidth(80);
        
        TableColumn<MusicFile, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(data -> {
            Integer year = data.getValue().getYear();
            return new SimpleStringProperty(year != null ? year.toString() : "");
        });
        yearCol.setPrefWidth(60);
        
        table.getColumns().addAll(artistCol, titleCol, albumCol, genreCol, yearCol);
        table.setPrefHeight(250);
        
        // Add context menu
        setupTableContextMenu(table);
        
        return table;
    }
    
    private void initializeBulkEditingComponents() {
        bulkEditModeCheckBox = new CheckBox("Bulk Edit Mode");
        bulkEditModeCheckBox.setOnAction(e -> toggleBulkEditMode());
        
        Tooltip bulkEditTooltip = new Tooltip(
            "Enable bulk editing to change artist, album, or genre for multiple selected files at once.\n" +
            "Keyboard shortcuts: Ctrl+A (Select All), Ctrl+B (Toggle Bulk Mode), Esc (Clear Selection)"
        );
        bulkEditModeCheckBox.setTooltip(bulkEditTooltip);
        
        bulkSelectionLabel = new Label("No files selected");
        bulkSelectionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666666;");
        
        bulkArtistField = new TextField();
        bulkArtistField.setPromptText("Leave empty to keep current values");
        
        bulkAlbumField = new TextField();
        bulkAlbumField.setPromptText("Leave empty to keep current values");
        
        bulkGenreField = new TextField();
        bulkGenreField.setPromptText("Leave empty to keep current values");
    }
    
    private void updateSelectionUI() {
        ObservableList<MusicFile> selectedFiles = resultsTable.getSelectionModel().getSelectedItems();
        int selectionCount = selectedFiles.size();
        
        if (bulkEditModeCheckBox.isSelected()) {
            // Bulk edit mode
            if (selectionCount == 0) {
                bulkSelectionLabel.setText("No files selected");
                if (bulkEditSection != null) {
                    bulkEditSection.setDisable(true);
                }
            } else {
                bulkSelectionLabel.setText(selectionCount + " file(s) selected for bulk editing");
                if (bulkEditSection != null) {
                    bulkEditSection.setDisable(false);
                }
            }
        } else {
            // Single edit mode
            if (selectionCount == 1) {
                loadFileForEditing(selectedFiles.get(0));
            } else if (selectionCount == 0) {
                currentFile = null;
                ((VBox) getBottom()).getChildren().get(0).setDisable(true);
                clearForm();
            } else {
                // Multiple files selected but not in bulk mode
                statusLabel.setText("Multiple files selected. Enable Bulk Edit Mode to edit them together.");
                currentFile = null;
                ((VBox) getBottom()).getChildren().get(0).setDisable(true);
                clearForm();
            }
        }
    }
    
    private void toggleBulkEditMode() {
        updateSelectionUI();
        
        // Force layout update
        if (getBottom() instanceof VBox) {
            VBox bottomContainer = (VBox) getBottom();
            // The edit section should be rebuilt to show appropriate UI
            bottomContainer.getChildren().set(0, bulkEditModeCheckBox.isSelected() ? 
                createBulkEditSection() : createEditSection());
        }
    }
    
    private void layoutComponents() {
        // Top search section
        HBox searchBox = new HBox(10);
        searchBox.setPadding(new Insets(10));
        
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        
        searchBox.getChildren().addAll(
            new Label("Search:"), searchField,
            new Label("in"), searchTypeCombo,
            searchButton
        );
        
        // Middle results section
        VBox resultsSection = new VBox(5);
        resultsSection.setPadding(new Insets(0, 10, 10, 10));
        
        Label resultsLabel = new Label("Search Results:");
        resultsLabel.setStyle("-fx-font-weight: bold;");
        
        HBox tableHeaderBox = new HBox(10);
        tableHeaderBox.setStyle("-fx-alignment: center-left;");
        
        // Add selection utilities
        Button selectAllButton = new Button("Select All");
        selectAllButton.setOnAction(e -> resultsTable.getSelectionModel().selectAll());
        selectAllButton.setStyle("-fx-font-size: 10px;");
        
        Button selectNoneButton = new Button("Clear Selection");
        selectNoneButton.setOnAction(e -> resultsTable.getSelectionModel().clearSelection());
        selectNoneButton.setStyle("-fx-font-size: 10px;");
        
        HBox selectionButtons = new HBox(5);
        selectionButtons.getChildren().addAll(selectAllButton, selectNoneButton);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        tableHeaderBox.getChildren().addAll(resultsLabel, spacer, selectionButtons, bulkEditModeCheckBox);
        
        resultsSection.getChildren().addAll(tableHeaderBox, resultsTable);
        
        // Bottom edit section
        VBox editSection = createEditSection();
        
        // Status bar
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.getChildren().add(statusLabel);
        
        setTop(searchBox);
        setCenter(resultsSection);
        setBottom(new VBox(editSection, statusBar));
        
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
    }
    
    private VBox createEditSection() {
        VBox editSection = new VBox(10);
        editSection.setPadding(new Insets(10));
        
        Label editLabel = new Label("Edit Metadata:");
        editLabel.setStyle("-fx-font-weight: bold;");
        
        // Create form grid
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        
        // Editable fields
        form.add(new Label("Title:"), 0, 0);
        form.add(titleField, 1, 0);
        
        form.add(new Label("Artist:"), 0, 1);
        form.add(artistField, 1, 1);
        
        form.add(new Label("Album:"), 0, 2);
        form.add(albumField, 1, 2);
        
        form.add(new Label("Genre:"), 0, 3);
        form.add(genreField, 1, 3);
        
        form.add(new Label("Track #:"), 2, 0);
        form.add(trackNumberField, 3, 0);
        trackNumberField.setPrefWidth(80);
        
        form.add(new Label("Year:"), 2, 1);
        form.add(yearField, 3, 1);
        yearField.setPrefWidth(80);
        
        // Read-only info fields
        form.add(new Label("File Path:"), 0, 4);
        form.add(filePathLabel, 1, 4, 3, 1);
        filePathLabel.setStyle("-fx-text-fill: gray;");
        
        form.add(new Label("File Size:"), 0, 5);
        form.add(fileSizeLabel, 1, 5);
        fileSizeLabel.setStyle("-fx-text-fill: gray;");
        
        form.add(new Label("Bitrate:"), 2, 5);
        form.add(bitrateLabel, 3, 5);
        bitrateLabel.setStyle("-fx-text-fill: gray;");
        
        form.add(new Label("Duration:"), 0, 6);
        form.add(durationLabel, 1, 6);
        durationLabel.setStyle("-fx-text-fill: gray;");
        
        // Make text fields grow
        titleField.setPrefWidth(200);
        artistField.setPrefWidth(200);
        albumField.setPrefWidth(200);
        genreField.setPrefWidth(200);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(e -> saveChanges());
        
        Button revertButton = new Button("Revert");
        revertButton.setOnAction(e -> revertChanges());
        
        Button deleteButton = new Button("Delete File");
        deleteButton.setOnAction(e -> deleteCurrentFile());
        deleteButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
        
        buttonBox.getChildren().addAll(saveButton, revertButton, deleteButton);
        
        editSection.getChildren().addAll(editLabel, form, buttonBox);
        
        // Initially disable edit section
        editSection.setDisable(true);
        
        return editSection;
    }
    
    private VBox createBulkEditSection() {
        bulkEditSection = new VBox(10);
        bulkEditSection.setPadding(new Insets(10));
        
        Label editLabel = new Label("Bulk Edit Metadata:");
        editLabel.setStyle("-fx-font-weight: bold;");
        
        // Selection info
        bulkEditSection.getChildren().add(bulkSelectionLabel);
        
        // Create form grid for bulk editing
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        
        // Only show the editable fields specified in the issue: artist, album, genre
        form.add(new Label("Artist:"), 0, 0);
        form.add(bulkArtistField, 1, 0);
        
        form.add(new Label("Album:"), 0, 1);
        form.add(bulkAlbumField, 1, 1);
        
        form.add(new Label("Genre:"), 0, 2);
        form.add(bulkGenreField, 1, 2);
        
        // Make text fields grow
        bulkArtistField.setPrefWidth(300);
        bulkAlbumField.setPrefWidth(300);
        bulkGenreField.setPrefWidth(300);
        
        // Instructions
        Label instructionsLabel = new Label("Instructions:");
        instructionsLabel.setStyle("-fx-font-weight: bold;");
        Label instructions = new Label(
            "• Only fill in fields you want to change for ALL selected files\n" +
            "• Leave fields empty to keep each file's current value\n" +
            "• Changes will be applied to all selected files"
        );
        instructions.setStyle("-fx-text-fill: #666666;");
        
        VBox instructionsBox = new VBox(5);
        instructionsBox.getChildren().addAll(instructionsLabel, instructions);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        Button bulkSaveButton = new Button("Apply to Selected Files");
        bulkSaveButton.setOnAction(e -> saveBulkChanges());
        bulkSaveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Button clearBulkButton = new Button("Clear Fields");
        clearBulkButton.setOnAction(e -> clearBulkFields());
        
        buttonBox.getChildren().addAll(bulkSaveButton, clearBulkButton);
        
        bulkEditSection.getChildren().addAll(editLabel, form, instructionsBox, buttonBox);
        
        // Initially disable if no selection
        bulkEditSection.setDisable(true);
        
        return bulkEditSection;
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            statusLabel.setText("Please enter a search term");
            return;
        }
        
        try {
            String searchType = searchTypeCombo.getValue();
            List<MusicFile> results;
            
            switch (searchType) {
                case "Title":
                    results = DatabaseManager.searchMusicFilesByTitle(searchTerm);
                    break;
                case "Artist":
                    results = DatabaseManager.searchMusicFilesByArtist(searchTerm);
                    break;
                case "Album":
                    results = DatabaseManager.searchMusicFilesByAlbum(searchTerm);
                    break;
                default:
                    results = DatabaseManager.searchMusicFiles(searchTerm);
                    break;
            }
            
            resultsData.setAll(results);
            statusLabel.setText("Found " + results.size() + " results");
            
        } catch (Exception e) {
            statusLabel.setText("Search error: " + e.getMessage());
            logger.error("Error performing search with term '{}' and type '{}'", searchTerm, searchTypeCombo.getValue(), e);
        }
    }
    
    private void loadFileForEditing(MusicFile file) {
        currentFile = file;
        
        // Populate form fields
        titleField.setText(file.getTitle() != null ? file.getTitle() : "");
        artistField.setText(file.getArtist() != null ? file.getArtist() : "");
        albumField.setText(file.getAlbum() != null ? file.getAlbum() : "");
        genreField.setText(file.getGenre() != null ? file.getGenre() : "");
        trackNumberField.setText(file.getTrackNumber() != null ? file.getTrackNumber().toString() : "");
        yearField.setText(file.getYear() != null ? file.getYear().toString() : "");
        
        // Populate read-only fields
        filePathLabel.setText(file.getFilePath());
        
        if (file.getFileSizeBytes() != null) {
            double sizeMB = file.getFileSizeBytes() / 1024.0 / 1024.0;
            fileSizeLabel.setText(String.format("%.2f MB", sizeMB));
        } else {
            fileSizeLabel.setText("Unknown");
        }
        
        bitrateLabel.setText(file.getBitRate() != null ? file.getBitRate() + " kbps" : "Unknown");
        
        if (file.getDurationSeconds() != null) {
            int minutes = file.getDurationSeconds() / 60;
            int seconds = file.getDurationSeconds() % 60;
            durationLabel.setText(String.format("%d:%02d", minutes, seconds));
        } else {
            durationLabel.setText("Unknown");
        }
        
        // Enable edit section
        ((VBox) getBottom()).getChildren().get(0).setDisable(false);
        
        statusLabel.setText("Loaded: " + file.getTitle());
    }
    
    private void saveChanges() {
        if (currentFile == null) return;
        
        try {
            // Update file with form values
            currentFile.setTitle(titleField.getText());
            currentFile.setArtist(artistField.getText());
            currentFile.setAlbum(albumField.getText());
            currentFile.setGenre(genreField.getText());
            
            // Handle numeric fields
            String trackText = trackNumberField.getText().trim();
            if (!trackText.isEmpty()) {
                try {
                    currentFile.setTrackNumber(Integer.parseInt(trackText));
                } catch (NumberFormatException e) {
                    currentFile.setTrackNumber(null);
                }
            } else {
                currentFile.setTrackNumber(null);
            }
            
            String yearText = yearField.getText().trim();
            if (!yearText.isEmpty()) {
                try {
                    currentFile.setYear(Integer.parseInt(yearText));
                } catch (NumberFormatException e) {
                    currentFile.setYear(null);
                }
            } else {
                currentFile.setYear(null);
            }
            
            // Save to database
            DatabaseManager.updateMusicFile(currentFile);
            
            // Refresh table
            resultsTable.refresh();
            
            statusLabel.setText("Changes saved successfully");
            
        } catch (Exception e) {
            statusLabel.setText("Error saving changes: " + e.getMessage());
            logger.error("Error saving metadata changes for file: {}", currentFile != null ? currentFile.getFilePath() : "unknown", e);
        }
    }
    
    private void revertChanges() {
        if (currentFile != null) {
            loadFileForEditing(currentFile);
            statusLabel.setText("Changes reverted");
        }
    }
    
    private void deleteCurrentFile() {
        if (currentFile == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Music File");
        alert.setContentText("Are you sure you want to delete: " + currentFile.getTitle() + " by " + currentFile.getArtist() + "?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Use DatabaseManager to handle both database and file deletion
                    if (DatabaseManager.deleteMusicFile(currentFile)) {
                        resultsData.remove(currentFile);
                        
                        // Clear form
                        currentFile = null;
                        ((VBox) getBottom()).getChildren().get(0).setDisable(true);
                        clearForm();
                        
                        statusLabel.setText("File deleted successfully");
                    } else {
                        statusLabel.setText("Failed to delete file");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error deleting file: " + e.getMessage());
                    logger.error("Error deleting current file: {}", currentFile != null ? currentFile.getFilePath() : "unknown", e);
                }
            }
        });
    }
    
    private void clearForm() {
        titleField.clear();
        artistField.clear();
        albumField.clear();
        genreField.clear();
        trackNumberField.clear();
        yearField.clear();
        filePathLabel.setText("");
        fileSizeLabel.setText("");
        bitrateLabel.setText("");
        durationLabel.setText("");
    }
    
    private void saveBulkChanges() {
        ObservableList<MusicFile> selectedFiles = resultsTable.getSelectionModel().getSelectedItems();
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("No files selected for bulk editing");
            return;
        }
        
        // Get values from bulk edit fields
        String newArtist = bulkArtistField.getText().trim();
        String newAlbum = bulkAlbumField.getText().trim();
        String newGenre = bulkGenreField.getText().trim();
        
        // Check if at least one field has a value
        if (newArtist.isEmpty() && newAlbum.isEmpty() && newGenre.isEmpty()) {
            statusLabel.setText("Please enter at least one value to update");
            return;
        }
        
        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Bulk Edit");
        confirmAlert.setHeaderText("Apply Changes to Multiple Files");
        
        StringBuilder message = new StringBuilder();
        message.append("You are about to apply the following changes to ")
               .append(selectedFiles.size()).append(" file(s):\n\n");
        
        if (!newArtist.isEmpty()) {
            message.append("• Artist: ").append(newArtist).append("\n");
        }
        if (!newAlbum.isEmpty()) {
            message.append("• Album: ").append(newAlbum).append("\n");
        }
        if (!newGenre.isEmpty()) {
            message.append("• Genre: ").append(newGenre).append("\n");
        }
        
        message.append("\nThis action cannot be easily undone. Continue?");
        confirmAlert.setContentText(message.toString());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performBulkUpdate(selectedFiles, newArtist, newAlbum, newGenre);
            }
        });
    }
    
    private void performBulkUpdate(ObservableList<MusicFile> files, String newArtist, String newAlbum, String newGenre) {
        int successCount = 0;
        int errorCount = 0;
        
        for (MusicFile file : files) {
            try {
                // Only update fields that have values
                boolean hasChanges = false;
                
                if (!newArtist.isEmpty()) {
                    file.setArtist(newArtist);
                    hasChanges = true;
                }
                if (!newAlbum.isEmpty()) {
                    file.setAlbum(newAlbum);
                    hasChanges = true;
                }
                if (!newGenre.isEmpty()) {
                    file.setGenre(newGenre);
                    hasChanges = true;
                }
                
                if (hasChanges) {
                    DatabaseManager.updateMusicFile(file);
                    successCount++;
                }
                
            } catch (Exception e) {
                errorCount++;
                logger.error("Error updating file: {} - {}", file.getTitle(), e.getMessage(), e);
            }
        }
        
        // Refresh table to show changes
        resultsTable.refresh();
        
        // Update status
        if (errorCount == 0) {
            statusLabel.setText("Successfully updated " + successCount + " file(s)");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Clear bulk fields after successful update
            clearBulkFields();
        } else {
            statusLabel.setText("Updated " + successCount + " file(s), " + errorCount + " failed");
            statusLabel.setStyle("-fx-text-fill: orange;");
        }
    }
    
    private void clearBulkFields() {
        bulkArtistField.clear();
        bulkAlbumField.clear();
        bulkGenreField.clear();
    }
    
    private void setupKeyboardShortcuts() {
        // Ctrl+A - Select All
        KeyCodeCombination selectAllShortcut = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);
        this.setOnKeyPressed(e -> {
            if (selectAllShortcut.match(e)) {
                resultsTable.getSelectionModel().selectAll();
                e.consume();
            }
        });
        
        // Ctrl+B - Toggle Bulk Edit Mode
        KeyCodeCombination bulkEditShortcut = new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN);
        this.setOnKeyPressed(e -> {
            if (bulkEditShortcut.match(e)) {
                bulkEditModeCheckBox.setSelected(!bulkEditModeCheckBox.isSelected());
                toggleBulkEditMode();
                e.consume();
            }
        });
        
        // Escape - Clear Selection
        this.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                resultsTable.getSelectionModel().clearSelection();
                e.consume();
            }
        });
        
        // Make this node focusable to receive key events
        this.setFocusTraversable(true);
    }
    
    /**
     * Sets up context menu for the results table.
     */
    private void setupTableContextMenu(TableView<MusicFile> table) {
        ContextMenu contextMenu = new ContextMenu();
        
        // Edit Metadata menu item (loads the file for editing or enables bulk mode)
        MenuItem editMetadataItem = new MenuItem("✏️ Edit Metadata");
        editMetadataItem.setOnAction(e -> {
            ObservableList<MusicFile> selectedFiles = table.getSelectionModel().getSelectedItems();
            if (selectedFiles.size() == 1) {
                // Single file - load for editing
                bulkEditModeCheckBox.setSelected(false);
                toggleBulkEditMode();
                loadFileForEditing(selectedFiles.get(0));
            } else if (selectedFiles.size() > 1) {
                // Multiple files - enable bulk edit mode
                bulkEditModeCheckBox.setSelected(true);
                toggleBulkEditMode();
                statusLabel.setText("Bulk edit mode enabled for " + selectedFiles.size() + " files");
            }
        });
        
        // Bulk Edit menu item
        MenuItem bulkEditItem = new MenuItem("📝 Bulk Edit Selected");
        bulkEditItem.setOnAction(e -> {
            ObservableList<MusicFile> selectedFiles = table.getSelectionModel().getSelectedItems();
            if (selectedFiles.size() > 1) {
                bulkEditModeCheckBox.setSelected(true);
                toggleBulkEditMode();
                statusLabel.setText("Bulk edit mode enabled for " + selectedFiles.size() + " files");
            } else {
                statusLabel.setText("Select multiple files to use bulk edit");
            }
        });
        
        // Delete File menu item
        MenuItem deleteItem = new MenuItem("🗑️ Delete File");
        deleteItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                deleteFile(selectedFile);
            }
        });
        
        // Add separator
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
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
            editMetadataItem,
            bulkEditItem,
            deleteItem,
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
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }
    
    /**
     * Deletes a specific file with confirmation.
     */
    private void deleteFile(MusicFile file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Music File");
        alert.setContentText("Are you sure you want to delete: " + file.getTitle() + " by " + file.getArtist() + "?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Use DatabaseManager to handle both database and file deletion
                    if (DatabaseManager.deleteMusicFile(file)) {
                        resultsData.remove(file);
                        
                        // Clear form if this was the selected file
                        if (currentFile != null && currentFile.equals(file)) {
                            currentFile = null;
                            ((VBox) getBottom()).getChildren().get(0).setDisable(true);
                            clearForm();
                        }
                        
                        statusLabel.setText("File deleted successfully");
                    } else {
                        statusLabel.setText("Failed to delete file");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error deleting file: " + e.getMessage());
                    logger.error("Error deleting file: {}", file.getFilePath(), e);
                }
            }
        });
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
            logger.error("Error opening file location for file: {}", file.getFilePath(), e);
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
    
    // ProfileChangeListener implementation
    @Override
    public void onProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile) {
        Platform.runLater(() -> {
            statusLabel.setText("Profile changed to: " + newProfile.getName());
            statusLabel.setStyle("-fx-text-fill: blue;");
            
            // Reset UI state but don't clear search results yet - 
            // wait for database change notification
        });
    }
    
    @Override
    public void onDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase) {
        Platform.runLater(() -> {
            // Clear all cached data
            resetAllData();
            
            if (isNewDatabase) {
                statusLabel.setText("New database detected - search for music files to get started");
                statusLabel.setStyle("-fx-text-fill: orange;");
                
                // Optionally notify user about new database
                showNewDatabasePrompt();
            } else {
                statusLabel.setText("Database changed - data cleared");
                statusLabel.setStyle("-fx-text-fill: blue;");
            }
        });
    }
    
    /**
     * Resets all data in the metadata editor.
     */
    private void resetAllData() {
        // Clear search results
        resultsData.clear();
        
        // Clear form data
        currentFile = null;
        clearForm();
        
        // Clear bulk edit fields
        clearBulkFields();
        
        // Reset UI state
        bulkEditModeCheckBox.setSelected(false);
        toggleBulkEditMode();
        
        // Disable edit sections
        if (getBottom() instanceof VBox) {
            VBox bottomContainer = (VBox) getBottom();
            bottomContainer.getChildren().get(0).setDisable(true);
        }
        
        // Clear search field
        searchField.clear();
        
        logger.info("MetadataEditorView: All data reset due to database change");
    }
    
    /**
     * Shows a prompt for new databases suggesting to import music files.
     */
    private void showNewDatabasePrompt() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Database");
        alert.setHeaderText("Empty Database Detected");
        alert.setContentText(
            "This database appears to be empty or new.\n\n" +
            "To get started:\n" +
            "1. Go to the 'Import & Organize' tab\n" +
            "2. Select directories containing your music files\n" +
            "3. The application will scan and import your music collection"
        );
        
        ButtonType goToImportButton = new ButtonType("Go to Import Tab");
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(goToImportButton, okButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == goToImportButton) {
                // Try to switch to import tab - this would need to be handled by the main application
                notifyRequestTabSwitch();
            }
        });
    }
    
    /**
     * Requests that the main application switch to the import tab.
     * Uses the provided TabSwitchCallback to perform the actual tab switch.
     */
    private void notifyRequestTabSwitch() {
        if (tabSwitchCallback != null) {
            boolean success = tabSwitchCallback.switchToTab("Import & Organize");
            if (success) {
                statusLabel.setText("Switched to Import & Organize tab");
                statusLabel.setStyle("-fx-text-fill: green;");
                logger.info("Successfully switched to Import & Organize tab via callback");
            } else {
                statusLabel.setText("Failed to switch to Import & Organize tab");
                statusLabel.setStyle("-fx-text-fill: red;");
                logger.warning("Failed to switch to Import & Organize tab - tab not found");
            }
        } else {
            // Fallback behavior when no callback is available
            statusLabel.setText("Please switch to 'Import & Organize' tab to add music files");
            statusLabel.setStyle("-fx-text-fill: orange;");
            logger.warning("No tab switch callback available - user must manually switch tabs");
        }
    }
    
    /**
     * Cleanup method to unregister from notifications.
     * Should be called when this view is being destroyed.
     */
    public void cleanup() {
        ProfileChangeNotifier.getInstance().removeListener(this);
    }
}