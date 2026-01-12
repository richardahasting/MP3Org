package org.hasting.ui.panels;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.util.List;
import java.util.function.Consumer;

/**
 * Panel responsible for bulk editing functionality.
 * Allows users to apply changes to multiple selected music files simultaneously.
 */
public class BulkEditPanel extends VBox {
    
    private static final Logger logger = Log4Rich.getLogger(BulkEditPanel.class);
    
    private CheckBox bulkEditModeCheckBox;
    private VBox bulkEditSection;
    private TextField bulkArtistField;
    private TextField bulkAlbumField;
    private TextField bulkGenreField;
    private Label bulkSelectionLabel;
    private Label statusLabel;
    
    private List<MusicFile> selectedFiles;
    private Consumer<String> onStatusUpdate;
    private Runnable onBulkChangesApplied;
    
    /**
     * Creates a new BulkEditPanel for editing multiple files at once.
     */
    public BulkEditPanel() {
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        
        // Initially hide bulk edit section
        setBulkEditVisible(false);
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
     * Sets the callback to be invoked when bulk changes are applied.
     * 
     * @param callback The callback to invoke after bulk changes
     */
    public void setOnBulkChangesApplied(Runnable callback) {
        this.onBulkChangesApplied = callback;
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
        bulkEditModeCheckBox = new CheckBox("Bulk Edit Mode");
        bulkEditModeCheckBox.setStyle("-fx-font-weight: bold;");
        
        bulkArtistField = new TextField();
        bulkArtistField.setPromptText("Leave empty to keep current values");
        
        bulkAlbumField = new TextField();
        bulkAlbumField.setPromptText("Leave empty to keep current values");
        
        bulkGenreField = new TextField();
        bulkGenreField.setPromptText("Leave empty to keep current values");
        
        bulkSelectionLabel = new Label("No files selected");
        bulkSelectionLabel.setStyle("-fx-font-style: italic;");
    }
    
    private void layoutComponents() {
        // Bulk edit toggle
        HBox toggleBox = new HBox();
        toggleBox.getChildren().add(bulkEditModeCheckBox);
        toggleBox.setPadding(new Insets(10));
        
        // Create bulk edit section
        bulkEditSection = createBulkEditSection();
        
        this.getChildren().addAll(toggleBox, bulkEditSection);
        this.setPadding(new Insets(0));
    }
    
    private VBox createBulkEditSection() {
        VBox section = new VBox();
        section.setSpacing(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-background-color: #f9f9f9;");
        
        Label titleLabel = new Label("Bulk Edit Selected Files");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Selection info
        section.getChildren().add(bulkSelectionLabel);
        
        // Bulk edit form
        GridPane bulkGrid = new GridPane();
        bulkGrid.setHgap(10);
        bulkGrid.setVgap(10);
        bulkGrid.setPadding(new Insets(10));
        
        bulkGrid.add(new Label("Artist:"), 0, 0);
        bulkGrid.add(bulkArtistField, 1, 0);
        
        bulkGrid.add(new Label("Album:"), 0, 1);
        bulkGrid.add(bulkAlbumField, 1, 1);
        
        bulkGrid.add(new Label("Genre:"), 0, 2);
        bulkGrid.add(bulkGenreField, 1, 2);
        
        // Make text fields expand horizontally
        GridPane.setHgrow(bulkArtistField, Priority.ALWAYS);
        GridPane.setHgrow(bulkAlbumField, Priority.ALWAYS);
        GridPane.setHgrow(bulkGenreField, Priority.ALWAYS);
        
        // Bulk action buttons
        HBox bulkButtonBox = new HBox(10);
        bulkButtonBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button applyBulkButton = new Button("Apply to Selected Files");
        applyBulkButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        applyBulkButton.setOnAction(e -> applyBulkChanges());
        
        Button clearBulkButton = new Button("Clear Fields");
        clearBulkButton.setOnAction(e -> clearBulkFields());
        
        bulkButtonBox.getChildren().addAll(applyBulkButton, clearBulkButton);
        
        // Help text
        Label helpLabel = new Label("Tip: Leave fields empty to keep existing values for each file");
        helpLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");
        
        section.getChildren().addAll(titleLabel, bulkGrid, bulkButtonBox, helpLabel);
        return section;
    }
    
    private void setupEventHandlers() {
        bulkEditModeCheckBox.setOnAction(e -> toggleBulkEditMode());
    }
    
    private void toggleBulkEditMode() {
        boolean bulkMode = bulkEditModeCheckBox.isSelected();
        setBulkEditVisible(bulkMode);
        
        if (bulkMode) {
            updateStatus("Bulk edit mode enabled - select multiple files to edit");
        } else {
            updateStatus("Bulk edit mode disabled");
            clearBulkFields();
        }
    }
    
    private void setBulkEditVisible(boolean visible) {
        bulkEditSection.setVisible(visible);
        bulkEditSection.setManaged(visible);
    }
    
    /**
     * Updates the selection of files available for bulk editing.
     * 
     * @param selectedFiles The list of currently selected files
     */
    public void updateSelection(List<MusicFile> selectedFiles) {
        this.selectedFiles = selectedFiles;
        
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            bulkSelectionLabel.setText("No files selected");
            bulkSelectionLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666666;");
        } else {
            bulkSelectionLabel.setText(selectedFiles.size() + " files selected for bulk editing");
            bulkSelectionLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #333333;");
        }
    }
    
    private void applyBulkChanges() {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            updateStatus("No files selected for bulk editing");
            return;
        }
        
        String newArtist = bulkArtistField.getText().trim();
        String newAlbum = bulkAlbumField.getText().trim();
        String newGenre = bulkGenreField.getText().trim();
        
        if (newArtist.isEmpty() && newAlbum.isEmpty() && newGenre.isEmpty()) {
            updateStatus("No changes specified for bulk edit");
            return;
        }
        
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Bulk Edit");
        alert.setHeaderText("Apply Bulk Changes");
        
        StringBuilder message = new StringBuilder();
        message.append("Apply the following changes to ").append(selectedFiles.size()).append(" files?\n\n");
        
        if (!newArtist.isEmpty()) {
            message.append("Artist: ").append(newArtist).append("\n");
        }
        if (!newAlbum.isEmpty()) {
            message.append("Album: ").append(newAlbum).append("\n");
        }
        if (!newGenre.isEmpty()) {
            message.append("Genre: ").append(newGenre).append("\n");
        }
        
        message.append("\nEmpty fields will keep existing values.");
        alert.setContentText(message.toString());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performBulkUpdate(selectedFiles, newArtist, newAlbum, newGenre);
            }
        });
    }
    
    private void performBulkUpdate(List<MusicFile> files, String newArtist, String newAlbum, String newGenre) {
        updateStatus("Applying bulk changes to " + files.size() + " files...");
        
        int successCount = 0;
        int errorCount = 0;
        
        try {
            for (MusicFile file : files) {
                try {
                    // Apply changes only to non-empty fields
                    if (!newArtist.isEmpty()) {
                        file.setArtist(newArtist);
                    }
                    if (!newAlbum.isEmpty()) {
                        file.setAlbum(newAlbum);
                    }
                    if (!newGenre.isEmpty()) {
                        file.setGenre(newGenre);
                    }
                    
                    // Update in database
                    DatabaseManager.updateMusicFile(file);
                    successCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                    logger.error(String.format("Error updating file %s: %s", file.getTitle(), e.getMessage()), e);
                }
            }
            
            // Update status
            if (errorCount == 0) {
                updateStatus("Successfully updated " + successCount + " files");
            } else {
                updateStatus("Updated " + successCount + " files, " + errorCount + " errors occurred");
            }
            
            // Clear bulk fields after successful operation
            if (successCount > 0) {
                clearBulkFields();
                
                // Notify parent that changes were applied
                if (onBulkChangesApplied != null) {
                    onBulkChangesApplied.run();
                }
            }
            
        } catch (Exception e) {
            updateStatus("Bulk update failed: " + e.getMessage());
            logger.error(String.format("Bulk update failed: {}", e.getMessage()), e);
        }
    }
    
    private void clearBulkFields() {
        bulkArtistField.clear();
        bulkAlbumField.clear();
        bulkGenreField.clear();
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
     * Checks if bulk edit mode is currently enabled.
     * 
     * @return true if bulk edit mode is enabled
     */
    public boolean isBulkEditModeEnabled() {
        return bulkEditModeCheckBox.isSelected();
    }
    
    /**
     * Sets the bulk edit mode state.
     * 
     * @param enabled true to enable bulk edit mode
     */
    public void setBulkEditMode(boolean enabled) {
        bulkEditModeCheckBox.setSelected(enabled);
        setBulkEditVisible(enabled);
    }
    
    /**
     * Gets the current number of selected files.
     * 
     * @return The number of files selected for bulk editing
     */
    public int getSelectedFileCount() {
        return selectedFiles != null ? selectedFiles.size() : 0;
    }
    
    /**
     * Gets access to the bulk artist field.
     * 
     * @return The bulk artist TextField
     */
    public TextField getBulkArtistField() {
        return bulkArtistField;
    }
    
    /**
     * Gets access to the bulk album field.
     * 
     * @return The bulk album TextField
     */
    public TextField getBulkAlbumField() {
        return bulkAlbumField;
    }
    
    /**
     * Gets access to the bulk genre field.
     * 
     * @return The bulk genre TextField
     */
    public TextField getBulkGenreField() {
        return bulkGenreField;
    }
}