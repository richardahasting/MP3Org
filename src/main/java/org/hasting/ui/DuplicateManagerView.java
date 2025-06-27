package org.hasting.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.hasting.util.HelpSystem;

import java.awt.Desktop;
import java.io.File;

import java.util.List;

public class DuplicateManagerView extends BorderPane {
    
    private TableView<MusicFile> duplicatesTable;
    private TableView<MusicFile> comparisonTable;
    private ObservableList<MusicFile> duplicatesData;
    private ObservableList<MusicFile> comparisonData;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    
    public DuplicateManagerView() {
        initializeComponents();
        layoutComponents();
        loadDuplicates();
    }
    
    private void initializeComponents() {
        // Initialize data lists
        duplicatesData = FXCollections.observableArrayList();
        comparisonData = FXCollections.observableArrayList();
        
        // Create main duplicates table
        duplicatesTable = createMusicFileTable();
        duplicatesTable.setItems(duplicatesData);
        
        // Create comparison table
        comparisonTable = createMusicFileTable();
        comparisonTable.setItems(comparisonData);
        
        // Add selection listener to load similar files
        duplicatesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadSimilarFiles(newSelection);
                }
            }
        );
        
        // Status label and progress indicator
        statusLabel = new Label("Ready");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
    }
    
    private TableView<MusicFile> createMusicFileTable() {
        TableView<MusicFile> table = new TableView<>();
        
        // Artist column
        TableColumn<MusicFile, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArtist()));
        artistCol.setPrefWidth(150);
        
        // Title column
        TableColumn<MusicFile, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        titleCol.setPrefWidth(200);
        
        // Album column
        TableColumn<MusicFile, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAlbum()));
        albumCol.setPrefWidth(150);
        
        // Duration column
        TableColumn<MusicFile, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> {
            Integer duration = data.getValue().getDurationSeconds();
            if (duration != null) {
                int minutes = duration / 60;
                int seconds = duration % 60;
                return new SimpleStringProperty(String.format("%d:%02d", minutes, seconds));
            }
            return new SimpleStringProperty("");
        });
        durationCol.setPrefWidth(80);
        
        // Bitrate column
        TableColumn<MusicFile, String> bitrateCol = new TableColumn<>("Bitrate");
        bitrateCol.setCellValueFactory(data -> {
            Long bitrate = data.getValue().getBitRate();
            return new SimpleStringProperty(bitrate != null ? bitrate + " kbps" : "");
        });
        bitrateCol.setPrefWidth(80);
        
        // File path column
        TableColumn<MusicFile, String> pathCol = new TableColumn<>("File Path");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFilePath()));
        pathCol.setPrefWidth(300);
        
        table.getColumns().addAll(artistCol, titleCol, albumCol, durationCol, bitrateCol, pathCol);
        
        // Add context menu
        setupTableContextMenu(table);
        
        return table;
    }
    
    private void layoutComponents() {
        // Create top controls
        HBox topControls = new HBox(10);
        topControls.setPadding(new Insets(5));
        
        Button refreshButton = new Button("Refresh Duplicates");
        refreshButton.setOnAction(e -> loadDuplicates());
        HelpSystem.setTooltip(refreshButton, "duplicates.refresh");
        
        Button deleteSelectedButton = new Button("Delete Selected");
        deleteSelectedButton.setOnAction(e -> deleteSelectedFile());
        HelpSystem.setTooltip(deleteSelectedButton, "duplicates.delete.selected");
        
        Button keepBetterQualityButton = new Button("Keep Better Quality");
        keepBetterQualityButton.setOnAction(e -> keepBetterQuality());
        HelpSystem.setTooltip(keepBetterQualityButton, "duplicates.keep.better");
        
        Button helpButton = new Button("?");
        helpButton.setPrefSize(25, 25);
        helpButton.setOnAction(e -> HelpSystem.showHelpDialog("duplicates.help", "Duplicate Management Help", getScene().getWindow()));
        
        topControls.getChildren().addAll(refreshButton, deleteSelectedButton, keepBetterQualityButton, helpButton);
        
        // Create main content area with split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        
        // Left side - potential duplicates
        VBox leftSide = new VBox(5);
        leftSide.setPadding(new Insets(5));
        Label duplicatesLabel = new Label("Potential Duplicates:");
        duplicatesLabel.setStyle("-fx-font-weight: bold;");
        HelpSystem.setTooltip(duplicatesTable, "duplicates.potential.table");
        leftSide.getChildren().addAll(duplicatesLabel, duplicatesTable);
        
        // Right side - similar files
        VBox rightSide = new VBox(5);
        rightSide.setPadding(new Insets(5));
        Label similarLabel = new Label("Similar Files:");
        similarLabel.setStyle("-fx-font-weight: bold;");
        HelpSystem.setTooltip(comparisonTable, "duplicates.similar.table");
        rightSide.getChildren().addAll(similarLabel, comparisonTable);
        
        splitPane.getItems().addAll(leftSide, rightSide);
        splitPane.setDividerPositions(0.5);
        
        // Bottom status bar
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.getChildren().addAll(statusLabel, progressIndicator);
        
        // Layout main components
        setTop(topControls);
        setCenter(splitPane);
        setBottom(statusBar);
        
        // Make tables grow to fill space
        VBox.setVgrow(duplicatesTable, Priority.ALWAYS);
        VBox.setVgrow(comparisonTable, Priority.ALWAYS);
    }
    
    private void loadDuplicates() {
        statusLabel.setText("Loading potential duplicates...");
        progressIndicator.setVisible(true);
        
        // TODO: This should be done on a background thread
        try {
            List<MusicFile> potentialDuplicates = DatabaseManager.findPotentialDuplicates();
            
            duplicatesData.setAll(potentialDuplicates);
            statusLabel.setText("Found " + potentialDuplicates.size() + " potential duplicates");
        } catch (Exception e) {
            statusLabel.setText("Error loading duplicates: " + e.getMessage());
            e.printStackTrace();
        } finally {
            progressIndicator.setVisible(false);
        }
    }
    
    private void loadSimilarFiles(MusicFile selectedFile) {
        try {
            List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
            List<MusicFile> similarFiles = selectedFile.findMostSimilarFiles(allFiles, 10);
            comparisonData.setAll(similarFiles);
        } catch (Exception e) {
            statusLabel.setText("Error loading similar files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void deleteSelectedFile() {
        MusicFile selected = duplicatesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setHeaderText("Delete Music File");
            alert.setContentText("Are you sure you want to delete: " + selected.getTitle() + " by " + selected.getArtist() + "?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        if (selected.deleteFile()) {
                            DatabaseManager.deleteMusicFile(selected);
                            duplicatesData.remove(selected);
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
    }
    
    private void keepBetterQuality() {
        MusicFile duplicate = duplicatesTable.getSelectionModel().getSelectedItem();
        MusicFile similar = comparisonTable.getSelectionModel().getSelectedItem();
        
        if (duplicate != null && similar != null) {
            // Determine which has better quality (higher bitrate)
            Long duplicateBitrate = duplicate.getBitRate() != null ? duplicate.getBitRate() : 0L;
            Long similarBitrate = similar.getBitRate() != null ? similar.getBitRate() : 0L;
            
            MusicFile toDelete = duplicateBitrate > similarBitrate ? similar : duplicate;
            MusicFile toKeep = duplicateBitrate > similarBitrate ? duplicate : similar;
            
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Keep Better Quality");
            alert.setHeaderText("Delete Lower Quality File");
            alert.setContentText("Keep: " + toKeep.getTitle() + " (" + toKeep.getBitRate() + " kbps)\n" +
                               "Delete: " + toDelete.getTitle() + " (" + toDelete.getBitRate() + " kbps)");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        if (toDelete.deleteFile()) {
                            DatabaseManager.deleteMusicFile(toDelete);
                            duplicatesData.remove(toDelete);
                            comparisonData.remove(toDelete);
                            statusLabel.setText("Lower quality file deleted");
                        } else {
                            statusLabel.setText("Failed to delete file");
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Error deleting file: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } else {
            statusLabel.setText("Please select files from both tables");
        }
    }
    
    /**
     * Sets up context menu for a music file table.
     */
    private void setupTableContextMenu(TableView<MusicFile> table) {
        ContextMenu contextMenu = new ContextMenu();
        
        // Delete File menu item
        MenuItem deleteItem = new MenuItem("🗑️ Delete File");
        deleteItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                deleteFile(selectedFile, table);
            }
        });
        
        // Keep Better Quality menu item (only for duplicate context)
        MenuItem keepBetterItem = new MenuItem("✨ Keep Better Quality");
        keepBetterItem.setOnAction(e -> {
            if (table == duplicatesTable) {
                keepBetterQuality();
            } else {
                // For comparison table, compare with selected duplicate
                MusicFile duplicate = duplicatesTable.getSelectionModel().getSelectedItem();
                MusicFile similar = table.getSelectionModel().getSelectedItem();
                if (duplicate != null && similar != null) {
                    keepBetterQualityBetween(duplicate, similar);
                }
            }
        });
        
        // Edit Metadata menu item
        MenuItem editMetadataItem = new MenuItem("✏️ Edit Metadata");
        editMetadataItem.setOnAction(e -> {
            MusicFile selectedFile = table.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                openMetadataEditor(selectedFile);
            }
        });
        
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
        
        // Add separator
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
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
            deleteItem,
            keepBetterItem,
            editMetadataItem,
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
                    // Update menu item availability based on context
                    keepBetterItem.setDisable(table == comparisonTable && duplicatesTable.getSelectionModel().isEmpty());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }
    
    /**
     * Deletes a specific file with confirmation.
     */
    private void deleteFile(MusicFile file, TableView<MusicFile> sourceTable) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Music File");
        alert.setContentText("Are you sure you want to delete: " + file.getTitle() + " by " + file.getArtist() + "?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (file.deleteFile()) {
                        DatabaseManager.deleteMusicFile(file);
                        duplicatesData.remove(file);
                        comparisonData.remove(file);
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
    
    /**
     * Compares quality between two specific files and deletes the lower quality one.
     */
    private void keepBetterQualityBetween(MusicFile file1, MusicFile file2) {
        Long bitrate1 = file1.getBitRate() != null ? file1.getBitRate() : 0L;
        Long bitrate2 = file2.getBitRate() != null ? file2.getBitRate() : 0L;
        
        MusicFile toDelete = bitrate1 > bitrate2 ? file2 : file1;
        MusicFile toKeep = bitrate1 > bitrate2 ? file1 : file2;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Keep Better Quality");
        alert.setHeaderText("Delete Lower Quality File");
        alert.setContentText("Keep: " + toKeep.getTitle() + " (" + toKeep.getBitRate() + " kbps)\n" +
                           "Delete: " + toDelete.getTitle() + " (" + toDelete.getBitRate() + " kbps)");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteFile(toDelete, null);
            }
        });
    }
    
    /**
     * Opens the metadata editor for the selected file.
     * Note: This would ideally switch to the metadata tab and populate the form.
     */
    private void openMetadataEditor(MusicFile file) {
        // For now, show file info. In a full implementation, this would:
        // 1. Switch to the Metadata Editor tab
        // 2. Search for this file
        // 3. Select it for editing
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Edit Metadata");
        info.setHeaderText("Open Metadata Editor");
        info.setContentText("To edit metadata for '" + file.getTitle() + "':\n\n" +
                          "1. Go to the Metadata Editor tab\n" +
                          "2. Search for: " + file.getTitle() + "\n" +
                          "3. Select the file and edit its information\n\n" +
                          "File path: " + file.getFilePath());
        info.showAndWait();
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
            e.printStackTrace();
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
}