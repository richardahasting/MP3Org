package org.hasting.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.hasting.model.MusicFile;
import org.hasting.ui.panels.BulkEditPanel;
import org.hasting.ui.panels.EditFormPanel;
import org.hasting.ui.panels.FileActionPanel;
import org.hasting.ui.panels.SearchPanel;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.ProfileChangeListener;
import org.hasting.util.ProfileChangeNotifier;

import java.util.List;

/**
 * Refactored MetadataEditorView that uses extracted panel components for better maintainability.
 * Provides music file searching, editing, and bulk operations through specialized panels.
 */
public class MetadataEditorViewRefactored extends BorderPane implements ProfileChangeListener {
    
    // Main panels
    private SearchPanel searchPanel;
    private EditFormPanel editFormPanel;
    private BulkEditPanel bulkEditPanel;
    private FileActionPanel fileActionPanel;
    
    // UI components
    private Label statusLabel;
    private SplitPane mainSplitPane;
    private ScrollPane rightScrollPane;
    
    // Current state
    private MusicFile currentFile;
    
    /**
     * Creates a new MetadataEditorViewRefactored with all editing functionality.
     */
    public MetadataEditorViewRefactored() {
        initializePanels();
        layoutPanels();
        setupInteractions();
        setupKeyboardShortcuts();
        
        // Register for profile change notifications
        ProfileChangeNotifier.getInstance().addListener(this);
    }
    
    private void initializePanels() {
        // Create panels
        searchPanel = new SearchPanel();
        editFormPanel = new EditFormPanel();
        bulkEditPanel = new BulkEditPanel();
        fileActionPanel = new FileActionPanel();
        
        // Create status label
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 5px;");
        
        // Set status label on all panels
        searchPanel.setStatusLabel(statusLabel);
        editFormPanel.setStatusLabel(statusLabel);
        bulkEditPanel.setStatusLabel(statusLabel);
        fileActionPanel.setStatusLabel(statusLabel);
    }
    
    private void layoutPanels() {
        // Create right side panel with edit forms
        VBox rightPanel = new VBox();
        rightPanel.setSpacing(10);
        rightPanel.setPadding(new Insets(10));
        
        rightPanel.getChildren().addAll(
            editFormPanel,
            bulkEditPanel,
            fileActionPanel
        );
        
        // Wrap right panel in scroll pane
        rightScrollPane = new ScrollPane(rightPanel);
        rightScrollPane.setFitToWidth(true);
        rightScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Create main split pane
        mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(searchPanel, rightScrollPane);
        mainSplitPane.setDividerPositions(0.6); // Search panel takes 60% of width
        
        // Layout main components
        this.setCenter(mainSplitPane);
        this.setBottom(statusLabel);
    }
    
    private void setupInteractions() {
        // Search panel interactions
        searchPanel.setOnFileSelected(this::loadFileForEditing);
        searchPanel.setOnSelectionChanged(this::updateSelectionUI);
        
        // Edit form panel interactions
        editFormPanel.setOnSaveChanges(this::saveChanges);
        editFormPanel.setOnRevertChanges(this::revertChanges);
        editFormPanel.setOnDeleteFile(this::deleteCurrentFile);
        editFormPanel.setOnStatusUpdate(this::updateStatus);
        
        // Bulk edit panel interactions
        bulkEditPanel.setOnStatusUpdate(this::updateStatus);
        bulkEditPanel.setOnBulkChangesApplied(this::refreshAfterBulkChanges);
        
        // File action panel interactions
        fileActionPanel.setOnSaveChanges(this::saveChanges);
        fileActionPanel.setOnRevertChanges(this::revertChanges);
        fileActionPanel.setOnDeleteFile(this::deleteCurrentFile);
        fileActionPanel.setOnRefreshData(this::refreshData);
        fileActionPanel.setOnStatusUpdate(this::updateStatus);
    }
    
    private void setupKeyboardShortcuts() {
        // Set up keyboard shortcuts at the scene level
        this.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F && event.isControlDown()) {
                // Ctrl+F to focus search field
                searchPanel.getSearchField().requestFocus();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                // Escape to clear selection
                searchPanel.getResultsTable().getSelectionModel().clearSelection();
                event.consume();
            }
        });
    }
    
    private void loadFileForEditing(MusicFile file) {
        this.currentFile = file;
        editFormPanel.loadFile(file);
        fileActionPanel.setCurrentFile(file);
        updateStatus("Loaded file: " + (file != null ? file.getTitle() : "None"));
    }
    
    private void updateSelectionUI(List<MusicFile> selectedFiles) {
        // Update bulk edit panel with selection
        bulkEditPanel.updateSelection(selectedFiles);
        
        // Update status
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            updateStatus("No files selected");
        } else if (selectedFiles.size() == 1) {
            updateStatus("1 file selected");
        } else {
            updateStatus(selectedFiles.size() + " files selected");
        }
    }
    
    private void saveChanges() {
        if (currentFile == null) {
            updateStatus("No file to save");
            return;
        }
        
        try {
            // Apply form changes to the file object
            editFormPanel.applyChangesToFile();
            
            // The FileActionPanel will handle the actual database save
            updateStatus("Changes saved successfully");
            
            // Refresh search results to show updated data
            refreshSearchResults();
            
        } catch (Exception e) {
            updateStatus("Error saving changes: " + e.getMessage());
        }
    }
    
    private void revertChanges() {
        if (currentFile == null) {
            updateStatus("No file to revert");
            return;
        }
        
        editFormPanel.revertFormToFile();
        updateStatus("Changes reverted");
    }
    
    private void deleteCurrentFile() {
        if (currentFile == null) {
            updateStatus("No file to delete");
            return;
        }
        
        // Clear the current file
        this.currentFile = null;
        editFormPanel.clearForm();
        fileActionPanel.setCurrentFile(null);
        
        // Refresh search results
        refreshSearchResults();
        
        updateStatus("File deleted");
    }
    
    private void refreshAfterBulkChanges() {
        // Refresh search results after bulk changes
        refreshSearchResults();
        updateStatus("Bulk changes applied successfully");
    }
    
    private void refreshData() {
        refreshSearchResults();
        updateStatus("Data refreshed");
    }
    
    private void refreshSearchResults() {
        // Trigger a new search with the current search term
        searchPanel.search();
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    // ProfileChangeListener implementation
    @Override
    public void onProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile) {
        // Clear current state when profile changes
        clearAllData();
        updateStatus("Profile changed to: " + (newProfile != null ? newProfile.getName() : "Unknown"));
    }
    
    @Override
    public void onDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase) {
        // Clear current state when database changes
        clearAllData();
        updateStatus("Database changed" + (isNewDatabase ? " (new database)" : ""));
    }
    
    private void clearAllData() {
        // Clear all panels
        searchPanel.clear();
        editFormPanel.clearForm();
        bulkEditPanel.updateSelection(null);
        fileActionPanel.setCurrentFile(null);
        
        // Clear current state
        this.currentFile = null;
    }
    
    /**
     * Cleanup method to remove listeners and release resources.
     */
    public void cleanup() {
        ProfileChangeNotifier.getInstance().removeListener(this);
    }
    
    // Public accessors for testing and external integration
    
    /**
     * Gets the search panel for direct access.
     * 
     * @return The SearchPanel instance
     */
    public SearchPanel getSearchPanel() {
        return searchPanel;
    }
    
    /**
     * Gets the edit form panel for direct access.
     * 
     * @return The EditFormPanel instance
     */
    public EditFormPanel getEditFormPanel() {
        return editFormPanel;
    }
    
    /**
     * Gets the bulk edit panel for direct access.
     * 
     * @return The BulkEditPanel instance
     */
    public BulkEditPanel getBulkEditPanel() {
        return bulkEditPanel;
    }
    
    /**
     * Gets the file action panel for direct access.
     * 
     * @return The FileActionPanel instance
     */
    public FileActionPanel getFileActionPanel() {
        return fileActionPanel;
    }
    
    /**
     * Gets the status label for direct access.
     * 
     * @return The status Label
     */
    public Label getStatusLabel() {
        return statusLabel;
    }
    
    /**
     * Gets the currently loaded file.
     * 
     * @return The current MusicFile, or null if none loaded
     */
    public MusicFile getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Performs a search with the given term.
     * 
     * @param searchTerm The term to search for
     */
    public void performSearch(String searchTerm) {
        searchPanel.setSearchTerm(searchTerm);
        searchPanel.search();
    }
    
    /**
     * Sets focus to the search field.
     */
    public void focusSearchField() {
        searchPanel.getSearchField().requestFocus();
    }
}