package org.hasting.ui.panels;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.awt.Desktop;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel responsible for searching music files and displaying results in a table.
 * Provides search functionality with different search types and result management.
 */
public class SearchPanel extends VBox {
    
    private static final Logger logger = Log4Rich.getLogger(SearchPanel.class);
    
    private TextField searchField;
    private ComboBox<String> searchTypeCombo;
    private TableView<MusicFile> resultsTable;
    private ObservableList<MusicFile> resultsData;
    private Label statusLabel;
    
    // Callbacks for interaction with parent components
    private Consumer<MusicFile> onFileSelected;
    private Consumer<List<MusicFile>> onSelectionChanged;
    
    /**
     * Creates a new SearchPanel with search functionality and results table.
     */
    public SearchPanel() {
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }
    
    /**
     * Sets the callback to be invoked when a file is selected.
     * 
     * @param callback The callback function that receives the selected MusicFile
     */
    public void setOnFileSelected(Consumer<MusicFile> callback) {
        this.onFileSelected = callback;
    }
    
    /**
     * Sets the callback to be invoked when the selection changes.
     * 
     * @param callback The callback function that receives the list of selected files
     */
    public void setOnSelectionChanged(Consumer<List<MusicFile>> callback) {
        this.onSelectionChanged = callback;
    }
    
    /**
     * Sets the status label for displaying search status and messages.
     * 
     * @param statusLabel The label to use for status messages
     */
    public void setStatusLabel(Label statusLabel) {
        this.statusLabel = statusLabel;
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
    }
    
    private TableView<MusicFile> createResultsTable() {
        TableView<MusicFile> table = new TableView<>();
        
        // Title column
        TableColumn<MusicFile, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTitle()));
        titleCol.setPrefWidth(200);
        
        // Artist column
        TableColumn<MusicFile, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getArtist()));
        artistCol.setPrefWidth(150);
        
        // Album column
        TableColumn<MusicFile, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAlbum()));
        albumCol.setPrefWidth(150);
        
        // Genre column
        TableColumn<MusicFile, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getGenre()));
        genreCol.setPrefWidth(100);
        
        // Year column
        TableColumn<MusicFile, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(cellData -> {
            Integer year = cellData.getValue().getYear();
            return new SimpleStringProperty(year != null ? year.toString() : "");
        });
        yearCol.setPrefWidth(60);
        
        // Duration column
        TableColumn<MusicFile, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(cellData -> {
            Integer duration = cellData.getValue().getDurationSeconds();
            if (duration != null && duration > 0) {
                int minutes = duration / 60;
                int seconds = duration % 60;
                return new SimpleStringProperty(String.format("%d:%02d", minutes, seconds));
            }
            return new SimpleStringProperty("");
        });
        durationCol.setPrefWidth(80);
        
        // File size column
        TableColumn<MusicFile, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(cellData -> {
            Long size = cellData.getValue().getFileSizeBytes();
            if (size != null && size > 0) {
                DecimalFormat df = new DecimalFormat("#.##");
                if (size < 1024) {
                    return new SimpleStringProperty(size + " B");
                } else if (size < 1024 * 1024) {
                    return new SimpleStringProperty(df.format(size / 1024.0) + " KB");
                } else {
                    return new SimpleStringProperty(df.format(size / (1024.0 * 1024.0)) + " MB");
                }
            }
            return new SimpleStringProperty("");
        });
        sizeCol.setPrefWidth(80);
        
        table.getColumns().addAll(titleCol, artistCol, albumCol, genreCol, yearCol, durationCol, sizeCol);
        
        return table;
    }
    
    private void layoutComponents() {
        // Search section
        Label searchLabel = new Label("Search:");
        HBox searchBox = new HBox(10);
        searchBox.getChildren().addAll(searchLabel, searchField, searchTypeCombo);
        searchBox.setPadding(new Insets(10));
        
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        searchBox.getChildren().add(searchButton);
        
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> clearSearch());
        searchBox.getChildren().add(clearButton);
        
        // Results section
        Label resultsLabel = new Label("Search Results:");
        
        this.getChildren().addAll(searchBox, resultsLabel, resultsTable);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        this.setPadding(new Insets(10));
    }
    
    private void setupEventHandlers() {
        // Search on Enter key
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                performSearch();
            }
        });
        
        // Selection change listener
        resultsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (onFileSelected != null && newSelection != null) {
                    onFileSelected.accept(newSelection);
                }
                if (onSelectionChanged != null) {
                    onSelectionChanged.accept(getSelectedFiles());
                }
            }
        );
        
        // Double-click to open file location
        resultsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                MusicFile selected = resultsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openFileLocation(selected);
                }
            }
        });
        
        // Context menu
        setupTableContextMenu();
    }
    
    private void setupTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem openLocationItem = new MenuItem("Open File Location");
        openLocationItem.setOnAction(e -> {
            MusicFile selected = resultsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openFileLocation(selected);
            }
        });
        
        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(e -> resultsTable.getSelectionModel().selectAll());
        
        MenuItem clearSelectionItem = new MenuItem("Clear Selection");
        clearSelectionItem.setOnAction(e -> resultsTable.getSelectionModel().clearSelection());
        
        contextMenu.getItems().addAll(openLocationItem, new SeparatorMenuItem(), 
                                     selectAllItem, clearSelectionItem);
        
        resultsTable.setContextMenu(contextMenu);
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        
        if (searchTerm.isEmpty()) {
            updateStatus("Please enter a search term");
            return;
        }
        
        updateStatus("Searching...");
        
        // Perform search in background thread
        Platform.runLater(() -> {
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
                    default: // "All Fields"
                        results = DatabaseManager.searchMusicFiles(searchTerm);
                        break;
                }
                
                Platform.runLater(() -> {
                    resultsData.clear();
                    resultsData.addAll(results);
                    updateStatus("Found " + results.size() + " results");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Search error: " + e.getMessage());
                    logger.error(String.format("Error performing search operation: {}", e.getMessage()), e);
                });
            }
        });
    }
    
    private void clearSearch() {
        searchField.clear();
        resultsData.clear();
        resultsTable.getSelectionModel().clearSelection();
        updateStatus("Search cleared");
    }
    
    private void openFileLocation(MusicFile file) {
        try {
            File f = new File(file.getFilePath());
            if (f.exists()) {
                Desktop.getDesktop().open(f.getParentFile());
            } else {
                updateStatus("File not found: " + file.getFilePath());
            }
        } catch (Exception e) {
            updateStatus("Error opening file location: " + e.getMessage());
        }
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    /**
     * Gets the currently selected file in the results table.
     * 
     * @return The selected MusicFile, or null if none selected
     */
    public MusicFile getSelectedFile() {
        return resultsTable.getSelectionModel().getSelectedItem();
    }
    
    /**
     * Gets all currently selected files in the results table.
     * 
     * @return List of selected MusicFile objects
     */
    public List<MusicFile> getSelectedFiles() {
        return resultsTable.getSelectionModel().getSelectedItems();
    }
    
    /**
     * Gets the results table for direct access if needed.
     * 
     * @return The TableView containing search results
     */
    public TableView<MusicFile> getResultsTable() {
        return resultsTable;
    }
    
    /**
     * Gets the search field for direct access if needed.
     * 
     * @return The search TextField
     */
    public TextField getSearchField() {
        return searchField;
    }
    
    /**
     * Sets the search term in the search field.
     * 
     * @param searchTerm The search term to set
     */
    public void setSearchTerm(String searchTerm) {
        searchField.setText(searchTerm);
    }
    
    /**
     * Triggers a search with the current search field content.
     */
    public void search() {
        performSearch();
    }
    
    /**
     * Clears the search and results.
     */
    public void clear() {
        clearSearch();
    }
}