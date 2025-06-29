package org.hasting.ui.panels;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;

import java.awt.Desktop;
import java.io.File;
import java.util.function.Consumer;

/**
 * Panel responsible for file actions such as save, revert, delete, and file operations.
 * Provides keyboard shortcuts and handles file system operations.
 */
public class FileActionPanel extends VBox {
    
    private MusicFile currentFile;
    private Label statusLabel;
    
    // Action buttons
    private Button saveButton;
    private Button revertButton;
    private Button deleteButton;
    private Button openLocationButton;
    private Button refreshButton;
    
    // Callbacks for actions
    private Runnable onSaveChanges;
    private Runnable onRevertChanges;
    private Runnable onDeleteFile;
    private Runnable onRefreshData;
    private Consumer<String> onStatusUpdate;
    private Consumer<MusicFile> onFileSelected;
    
    /**
     * Creates a new FileActionPanel with file operation buttons and shortcuts.
     */
    public FileActionPanel() {
        initializeComponents();
        layoutComponents();
        setupKeyboardShortcuts();
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
     * Sets the callback to be invoked when refresh data is requested.
     * 
     * @param callback The callback to invoke on refresh
     */
    public void setOnRefreshData(Runnable callback) {
        this.onRefreshData = callback;
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
     * Sets the callback to be invoked when a file is selected.
     * 
     * @param callback The callback that receives the selected file
     */
    public void setOnFileSelected(Consumer<MusicFile> callback) {
        this.onFileSelected = callback;
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
        saveButton = new Button("Save Changes");
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        saveButton.setDisable(true);
        saveButton.setOnAction(e -> saveChanges());
        
        revertButton = new Button("Revert Changes");
        revertButton.setDisable(true);
        revertButton.setOnAction(e -> revertChanges());
        
        deleteButton = new Button("Delete File");
        deleteButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
        deleteButton.setDisable(true);
        deleteButton.setOnAction(e -> deleteFile());
        
        openLocationButton = new Button("Open File Location");
        openLocationButton.setDisable(true);
        openLocationButton.setOnAction(e -> openFileLocation());
        
        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshData());
        
        // Add tooltips
        saveButton.setTooltip(new Tooltip("Save changes to database (Ctrl+S)"));
        revertButton.setTooltip(new Tooltip("Revert to original values (Ctrl+R)"));
        deleteButton.setTooltip(new Tooltip("Delete file from disk and database (Delete)"));
        openLocationButton.setTooltip(new Tooltip("Open file location in system file manager"));
        refreshButton.setTooltip(new Tooltip("Refresh data from database (F5)"));
    }
    
    private void layoutComponents() {
        Label titleLabel = new Label("File Actions");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Primary actions
        HBox primaryActions = new HBox(10);
        primaryActions.getChildren().addAll(saveButton, revertButton);
        
        // Secondary actions
        HBox secondaryActions = new HBox(10);
        secondaryActions.getChildren().addAll(openLocationButton, refreshButton);
        
        // Dangerous actions
        HBox dangerousActions = new HBox(10);
        dangerousActions.getChildren().add(deleteButton);
        
        this.getChildren().addAll(
            titleLabel,
            primaryActions,
            secondaryActions,
            new Separator(),
            dangerousActions
        );
        
        this.setPadding(new Insets(10));
        this.setSpacing(10);
    }
    
    private void setupKeyboardShortcuts() {
        // Note: These would typically be set on the parent scene
        // For now, we'll document the intended shortcuts
        
        // Ctrl+S for save
        saveButton.getScene().getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
            () -> {
                if (!saveButton.isDisabled()) {
                    saveChanges();
                }
            }
        );
        
        // Ctrl+R for revert
        revertButton.getScene().getAccelerators().put(
            new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN),
            () -> {
                if (!revertButton.isDisabled()) {
                    revertChanges();
                }
            }
        );
        
        // Delete key for delete file
        deleteButton.getScene().getAccelerators().put(
            new KeyCodeCombination(KeyCode.DELETE),
            () -> {
                if (!deleteButton.isDisabled()) {
                    deleteFile();
                }
            }
        );
        
        // F5 for refresh
        refreshButton.getScene().getAccelerators().put(
            new KeyCodeCombination(KeyCode.F5),
            () -> refreshData()
        );
    }
    
    /**
     * Sets the current file and updates button states accordingly.
     * 
     * @param file The current MusicFile being worked with
     */
    public void setCurrentFile(MusicFile file) {
        this.currentFile = file;
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean hasFile = (currentFile != null);
        
        saveButton.setDisable(!hasFile);
        revertButton.setDisable(!hasFile);
        deleteButton.setDisable(!hasFile);
        openLocationButton.setDisable(!hasFile || currentFile.getFilePath() == null);
    }
    
    private void saveChanges() {
        if (currentFile == null) {
            updateStatus("No file selected to save");
            return;
        }
        
        updateStatus("Saving changes...");
        
        try {
            DatabaseManager.updateMusicFile(currentFile);
            updateStatus("Changes saved successfully");
            
            if (onSaveChanges != null) {
                onSaveChanges.run();
            }
            
        } catch (Exception e) {
            updateStatus("Error saving changes: " + e.getMessage());
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to save changes");
            alert.setContentText("An error occurred while saving: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void revertChanges() {
        if (currentFile == null) {
            updateStatus("No file selected to revert");
            return;
        }
        
        // Show confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Revert");
        alert.setHeaderText("Revert Changes");
        alert.setContentText("Are you sure you want to revert all changes? This will discard any unsaved modifications.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (onRevertChanges != null) {
                    onRevertChanges.run();
                }
                updateStatus("Changes reverted");
            }
        });
    }
    
    private void deleteFile() {
        if (currentFile == null) {
            updateStatus("No file selected to delete");
            return;
        }
        
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete File");
        alert.setContentText(
            "Are you sure you want to delete this file?\n\n" +
            "File: " + currentFile.getTitle() + "\n" +
            "Path: " + currentFile.getFilePath() + "\n\n" +
            "This action cannot be undone!"
        );
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performDeleteFile();
            }
        });
    }
    
    private void performDeleteFile() {
        updateStatus("Deleting file...");
        
        try {
            // Delete file from disk
            File file = new File(currentFile.getFilePath());
            boolean fileDeleted = false;
            
            if (file.exists()) {
                fileDeleted = file.delete();
                if (!fileDeleted) {
                    throw new RuntimeException("Failed to delete file from disk");
                }
            }
            
            // Delete from database
            DatabaseManager.deleteMusicFile(currentFile);
            
            updateStatus("File deleted successfully");
            
            // Clear current file and notify parent
            setCurrentFile(null);
            if (onDeleteFile != null) {
                onDeleteFile.run();
            }
            
        } catch (Exception e) {
            updateStatus("Error deleting file: " + e.getMessage());
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Delete Error");
            alert.setHeaderText("Failed to delete file");
            alert.setContentText("An error occurred while deleting: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void openFileLocation() {
        if (currentFile == null || currentFile.getFilePath() == null) {
            updateStatus("No file path available");
            return;
        }
        
        try {
            File file = new File(currentFile.getFilePath());
            if (file.exists()) {
                Desktop.getDesktop().open(file.getParentFile());
                updateStatus("Opened file location");
            } else {
                updateStatus("File not found: " + currentFile.getFilePath());
                
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("File Not Found");
                alert.setHeaderText("Cannot open file location");
                alert.setContentText("The file no longer exists at the specified location:\n" + 
                                   currentFile.getFilePath());
                alert.showAndWait();
            }
        } catch (Exception e) {
            updateStatus("Error opening file location: " + e.getMessage());
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Open Location Error");
            alert.setHeaderText("Failed to open file location");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void refreshData() {
        updateStatus("Refreshing data...");
        
        if (onRefreshData != null) {
            onRefreshData.run();
        }
        
        updateStatus("Data refreshed");
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
     * Enables or disables the save button.
     * 
     * @param enabled true to enable the save button
     */
    public void setSaveEnabled(boolean enabled) {
        saveButton.setDisable(!enabled || currentFile == null);
    }
    
    /**
     * Enables or disables the revert button.
     * 
     * @param enabled true to enable the revert button
     */
    public void setRevertEnabled(boolean enabled) {
        revertButton.setDisable(!enabled || currentFile == null);
    }
    
    /**
     * Gets the save button for external access.
     * 
     * @return The save Button
     */
    public Button getSaveButton() {
        return saveButton;
    }
    
    /**
     * Gets the delete button for external access.
     * 
     * @return The delete Button
     */
    public Button getDeleteButton() {
        return deleteButton;
    }
}