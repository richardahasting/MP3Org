package org.hasting.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;

import java.util.List;

public class MetadataEditorView extends BorderPane {
    
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
    
    public MetadataEditorView() {
        initializeComponents();
        layoutComponents();
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
        
        // Add selection listener
        resultsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadFileForEditing(newSelection);
                }
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
        
        // Add search functionality
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        
        // Enter key search
        searchField.setOnAction(e -> performSearch());
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
        
        return table;
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
        resultsSection.getChildren().addAll(resultsLabel, resultsTable);
        
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
            e.printStackTrace();
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
            e.printStackTrace();
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
                    if (currentFile.deleteFile()) {
                        DatabaseManager.deleteMusicFile(currentFile);
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
                    e.printStackTrace();
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
}