package org.hasting.ui.panels;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.hasting.model.MusicFile;

import java.text.DecimalFormat;
import java.util.function.Consumer;

/**
 * Panel responsible for editing metadata of individual music files.
 * Provides form fields for all editable metadata and file information display.
 */
public class EditFormPanel extends VBox {
    
    // Editable form fields
    private TextField titleField;
    private TextField artistField;
    private TextField albumField;
    private TextField genreField;
    private TextField trackNumberField;
    private TextField yearField;
    
    // Read-only file information labels
    private Label filePathLabel;
    private Label fileSizeLabel;
    private Label bitrateLabel;
    private Label durationLabel;
    private Label lastModifiedLabel;
    
    private MusicFile currentFile;
    private Label statusLabel;
    
    // Callbacks for actions
    private Runnable onSaveChanges;
    private Runnable onRevertChanges;
    private Runnable onDeleteFile;
    private Consumer<String> onStatusUpdate;
    
    /**
     * Creates a new EditFormPanel for editing music file metadata.
     */
    public EditFormPanel() {
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }
    
    /**
     * Sets the callback to be invoked when save changes is requested.
     * 
     * @param callback The callback to invoke on save
     */
    public void setOnSaveChanges(Runnable callback) {
        this.onSaveChanges = callback;
    }
    
    /**
     * Sets the callback to be invoked when revert changes is requested.
     * 
     * @param callback The callback to invoke on revert
     */
    public void setOnRevertChanges(Runnable callback) {
        this.onRevertChanges = callback;
    }
    
    /**
     * Sets the callback to be invoked when delete file is requested.
     * 
     * @param callback The callback to invoke on delete
     */
    public void setOnDeleteFile(Runnable callback) {
        this.onDeleteFile = callback;
    }
    
    /**
     * Sets the callback to be invoked for status updates.
     * 
     * @param callback The callback that receives status messages
     */
    public void setOnStatusUpdate(Consumer<String> callback) {
        this.onStatusUpdate = callback;
    }
    
    /**
     * Sets the status label for displaying messages.
     * 
     * @param statusLabel The label to use for status messages
     */
    public void setStatusLabel(Label statusLabel) {
        this.statusLabel = statusLabel;
    }
    
    private void initializeComponents() {
        // Editable fields
        titleField = new TextField();
        artistField = new TextField();
        albumField = new TextField();
        genreField = new TextField();
        trackNumberField = new TextField();
        yearField = new TextField();
        
        // Read-only information labels
        filePathLabel = new Label();
        filePathLabel.setWrapText(true);
        filePathLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 10px;");
        
        fileSizeLabel = new Label();
        bitrateLabel = new Label();
        durationLabel = new Label();
        lastModifiedLabel = new Label();
        
        // Initially disable all fields
        setFieldsEnabled(false);
    }
    
    private void layoutComponents() {
        // Edit form section
        Label editLabel = new Label("Edit Metadata");
        editLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        GridPane editGrid = createEditGrid();
        
        // File information section
        Label infoLabel = new Label("File Information");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        GridPane infoGrid = createInfoGrid();
        
        // Action buttons
        HBox buttonBox = createButtonBox();
        
        this.getChildren().addAll(
            editLabel,
            editGrid,
            new Separator(),
            infoLabel,
            infoGrid,
            new Separator(),
            buttonBox
        );
        
        this.setPadding(new Insets(10));
        this.setSpacing(10);
    }
    
    private GridPane createEditGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Add form fields
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        
        grid.add(new Label("Artist:"), 0, 1);
        grid.add(artistField, 1, 1);
        
        grid.add(new Label("Album:"), 0, 2);
        grid.add(albumField, 1, 2);
        
        grid.add(new Label("Genre:"), 0, 3);
        grid.add(genreField, 1, 3);
        
        grid.add(new Label("Track #:"), 0, 4);
        grid.add(trackNumberField, 1, 4);
        
        grid.add(new Label("Year:"), 0, 5);
        grid.add(yearField, 1, 5);
        
        // Make text fields expand horizontally
        GridPane.setHgrow(titleField, Priority.ALWAYS);
        GridPane.setHgrow(artistField, Priority.ALWAYS);
        GridPane.setHgrow(albumField, Priority.ALWAYS);
        GridPane.setHgrow(genreField, Priority.ALWAYS);
        
        return grid;
    }
    
    private GridPane createInfoGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));
        
        grid.add(new Label("File Path:"), 0, 0);
        grid.add(filePathLabel, 1, 0);
        
        grid.add(new Label("File Size:"), 0, 1);
        grid.add(fileSizeLabel, 1, 1);
        
        grid.add(new Label("Bitrate:"), 0, 2);
        grid.add(bitrateLabel, 1, 2);
        
        grid.add(new Label("Duration:"), 0, 3);
        grid.add(durationLabel, 1, 3);
        
        grid.add(new Label("Last Modified:"), 0, 4);
        grid.add(lastModifiedLabel, 1, 4);
        
        // Make labels expand horizontally
        GridPane.setHgrow(filePathLabel, Priority.ALWAYS);
        GridPane.setHgrow(fileSizeLabel, Priority.ALWAYS);
        GridPane.setHgrow(bitrateLabel, Priority.ALWAYS);
        GridPane.setHgrow(durationLabel, Priority.ALWAYS);
        GridPane.setHgrow(lastModifiedLabel, Priority.ALWAYS);
        
        return grid;
    }
    
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        
        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(e -> {
            if (onSaveChanges != null) {
                onSaveChanges.run();
            }
        });
        
        Button revertButton = new Button("Revert Changes");
        revertButton.setOnAction(e -> {
            if (onRevertChanges != null) {
                onRevertChanges.run();
            }
        });
        
        Button deleteButton = new Button("Delete File");
        deleteButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> {
            if (onDeleteFile != null) {
                // Show confirmation dialog
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Deletion");
                alert.setHeaderText("Delete File");
                alert.setContentText("Are you sure you want to delete this file? This action cannot be undone.");
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        onDeleteFile.run();
                    }
                });
            }
        });
        
        buttonBox.getChildren().addAll(saveButton, revertButton, deleteButton);
        return buttonBox;
    }
    
    private void setupEventHandlers() {
        // Add change listeners to detect modifications
        titleField.textProperty().addListener((obs, oldText, newText) -> markAsModified());
        artistField.textProperty().addListener((obs, oldText, newText) -> markAsModified());
        albumField.textProperty().addListener((obs, oldText, newText) -> markAsModified());
        genreField.textProperty().addListener((obs, oldText, newText) -> markAsModified());
        trackNumberField.textProperty().addListener((obs, oldText, newText) -> markAsModified());
        yearField.textProperty().addListener((obs, oldText, newText) -> markAsModified());
    }
    
    private void markAsModified() {
        if (currentFile != null) {
            updateStatus("File has been modified (unsaved changes)");
        }
    }
    
    /**
     * Loads a music file for editing in the form.
     * 
     * @param file The MusicFile to load for editing
     */
    public void loadFile(MusicFile file) {
        this.currentFile = file;
        
        if (file == null) {
            clearForm();
            setFieldsEnabled(false);
            updateStatus("No file selected");
            return;
        }
        
        // Populate editable fields
        titleField.setText(file.getTitle() != null ? file.getTitle() : "");
        artistField.setText(file.getArtist() != null ? file.getArtist() : "");
        albumField.setText(file.getAlbum() != null ? file.getAlbum() : "");
        genreField.setText(file.getGenre() != null ? file.getGenre() : "");
        trackNumberField.setText(file.getTrackNumber() != null ? file.getTrackNumber().toString() : "");
        yearField.setText(file.getYear() != null ? file.getYear().toString() : "");
        
        // Populate file information
        filePathLabel.setText(file.getFilePath() != null ? file.getFilePath() : "Unknown");
        
        // Format file size
        if (file.getFileSizeBytes() != null && file.getFileSizeBytes() > 0) {
            long size = file.getFileSizeBytes();
            DecimalFormat df = new DecimalFormat("#.##");
            if (size < 1024) {
                fileSizeLabel.setText(size + " bytes");
            } else if (size < 1024 * 1024) {
                fileSizeLabel.setText(df.format(size / 1024.0) + " KB");
            } else {
                fileSizeLabel.setText(df.format(size / (1024.0 * 1024.0)) + " MB");
            }
        } else {
            fileSizeLabel.setText("Unknown");
        }
        
        // Format bitrate
        if (file.getBitRate() != null && file.getBitRate() > 0) {
            bitrateLabel.setText(file.getBitRate() + " kbps");
        } else {
            bitrateLabel.setText("Unknown");
        }
        
        // Format duration
        if (file.getDurationSeconds() != null && file.getDurationSeconds() > 0) {
            int duration = file.getDurationSeconds();
            int minutes = duration / 60;
            int seconds = duration % 60;
            durationLabel.setText(String.format("%d:%02d", minutes, seconds));
        } else {
            durationLabel.setText("Unknown");
        }
        
        // Format last modified
        if (file.getLastModified() != null) {
            lastModifiedLabel.setText(file.getLastModified().toString());
        } else {
            lastModifiedLabel.setText("Unknown");
        }
        
        setFieldsEnabled(true);
        updateStatus("File loaded for editing: " + file.getTitle());
    }
    
    /**
     * Applies the current form values to the loaded MusicFile object.
     * Does not save to database - just updates the object.
     */
    public void applyChangesToFile() {
        if (currentFile == null) {
            return;
        }
        
        currentFile.setTitle(titleField.getText().trim());
        currentFile.setArtist(artistField.getText().trim());
        currentFile.setAlbum(albumField.getText().trim());
        currentFile.setGenre(genreField.getText().trim());
        
        // Handle numeric fields
        try {
            String trackText = trackNumberField.getText().trim();
            if (!trackText.isEmpty()) {
                currentFile.setTrackNumber(Integer.parseInt(trackText));
            } else {
                currentFile.setTrackNumber(null);
            }
        } catch (NumberFormatException e) {
            updateStatus("Invalid track number format");
        }
        
        try {
            String yearText = yearField.getText().trim();
            if (!yearText.isEmpty()) {
                currentFile.setYear(Integer.parseInt(yearText));
            } else {
                currentFile.setYear(null);
            }
        } catch (NumberFormatException e) {
            updateStatus("Invalid year format");
        }
    }
    
    /**
     * Reverts the form fields to the current file's values.
     */
    public void revertFormToFile() {
        if (currentFile != null) {
            loadFile(currentFile);
            updateStatus("Changes reverted");
        }
    }
    
    /**
     * Clears all form fields and file information.
     */
    public void clearForm() {
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
        lastModifiedLabel.setText("");
        
        currentFile = null;
    }
    
    /**
     * Gets the currently loaded file.
     * 
     * @return The currently loaded MusicFile, or null if none loaded
     */
    public MusicFile getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Checks if the form has been modified since the file was loaded.
     * 
     * @return true if the form has unsaved changes
     */
    public boolean hasUnsavedChanges() {
        if (currentFile == null) {
            return false;
        }
        
        // Compare form values with current file values
        return !equals(titleField.getText().trim(), currentFile.getTitle()) ||
               !equals(artistField.getText().trim(), currentFile.getArtist()) ||
               !equals(albumField.getText().trim(), currentFile.getAlbum()) ||
               !equals(genreField.getText().trim(), currentFile.getGenre()) ||
               !equals(trackNumberField.getText().trim(), 
                      currentFile.getTrackNumber() != null ? currentFile.getTrackNumber().toString() : "") ||
               !equals(yearField.getText().trim(), 
                      currentFile.getYear() != null ? currentFile.getYear().toString() : "");
    }
    
    private boolean equals(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }
    
    private void setFieldsEnabled(boolean enabled) {
        titleField.setDisable(!enabled);
        artistField.setDisable(!enabled);
        albumField.setDisable(!enabled);
        genreField.setDisable(!enabled);
        trackNumberField.setDisable(!enabled);
        yearField.setDisable(!enabled);
    }
    
    private void updateStatus(String message) {
        if (onStatusUpdate != null) {
            onStatusUpdate.accept(message);
        }
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    /**
     * Gets access to the title field for validation or other purposes.
     * 
     * @return The title TextField
     */
    public TextField getTitleField() {
        return titleField;
    }
    
    /**
     * Gets access to the artist field for validation or other purposes.
     * 
     * @return The artist TextField
     */
    public TextField getArtistField() {
        return artistField;
    }
}