package org.hasting.ui.panels;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import org.hasting.util.DatabaseManager;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.DatabaseProfileManager;
import org.hasting.util.HelpSystem;

import java.io.File;
import java.util.Optional;

/**
 * UI panel for managing database profiles in the MP3Org application.
 * Provides functionality to create, duplicate, rename, delete, and switch between profiles.
 */
public class ProfileManagementPanel extends VBox {
    
    private ComboBox<String> profileComboBox;
    private Button newProfileButton;
    private Button duplicateProfileButton;
    private Button deleteProfileButton;
    private Button renameProfileButton;
    private TextField profileNameField;
    private TextArea profileDescriptionArea;
    private Label statusLabel;
    
    // Callback interfaces for notifying parent components of changes
    private Runnable onProfileChanged;
    
    // Flag to prevent recursive profile switching
    private boolean isUpdatingProfile = false;
    
    /**
     * Creates a new ProfileManagementPanel with all necessary components.
     * 
     * @param statusLabel The shared status label for displaying operation results
     */
    public ProfileManagementPanel(Label statusLabel) {
        this.statusLabel = statusLabel;
        initializeComponents();
        layoutComponents();
        loadCurrentSettings();
    }
    
    /**
     * Sets a callback to be notified when profile changes occur.
     * 
     * @param onProfileChanged Callback to execute when profiles change
     */
    public void setOnProfileChanged(Runnable onProfileChanged) {
        this.onProfileChanged = onProfileChanged;
    }
    
    /**
     * Initializes all UI components for the profile management panel.
     */
    private void initializeComponents() {
        // Profile selection combo box
        profileComboBox = new ComboBox<>();
        profileComboBox.setPrefWidth(300);
        profileComboBox.setOnAction(e -> switchToSelectedProfile());
        HelpSystem.setTooltip(profileComboBox, "config.profile.combo");
        
        // Profile management buttons
        newProfileButton = new Button("New Profile");
        newProfileButton.setOnAction(e -> createNewProfile());
        HelpSystem.setTooltip(newProfileButton, "config.profile.new");
        
        duplicateProfileButton = new Button("Duplicate Profile");
        duplicateProfileButton.setOnAction(e -> duplicateCurrentProfile());
        HelpSystem.setTooltip(duplicateProfileButton, "config.profile.duplicate");
        
        deleteProfileButton = new Button("Delete Profile");
        deleteProfileButton.setOnAction(e -> deleteCurrentProfile());
        HelpSystem.setTooltip(deleteProfileButton, "config.profile.delete");
        
        renameProfileButton = new Button("Rename Profile");
        renameProfileButton.setOnAction(e -> renameCurrentProfile());
        HelpSystem.setTooltip(renameProfileButton, "config.profile.rename");
        
        // Profile information fields
        profileNameField = new TextField();
        profileNameField.setEditable(false);
        profileNameField.setFocusTraversable(false);
        profileNameField.setMouseTransparent(true);
        profileNameField.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
        
        profileDescriptionArea = new TextArea();
        profileDescriptionArea.setEditable(false);
        profileDescriptionArea.setPrefRowCount(5);
        profileDescriptionArea.setWrapText(true);
        profileDescriptionArea.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
    }
    
    /**
     * Arranges the components in the panel layout.
     */
    private void layoutComponents() {
        setSpacing(10);
        
        // Profile management section
        Label profileLabel = new Label("Database Profiles:");
        profileLabel.setStyle("-fx-font-weight: bold;");
        
        // Button layout
        HBox buttonBox1 = new HBox(10);
        buttonBox1.getChildren().addAll(newProfileButton, duplicateProfileButton);
        
        HBox buttonBox2 = new HBox(10);
        buttonBox2.getChildren().addAll(renameProfileButton, deleteProfileButton);
        
        // Profile info section
        Label nameLabel = new Label("Profile Name:");
        Label descLabel = new Label("Profile Description:");
        
        // Add all components
        getChildren().addAll(
            profileLabel,
            profileComboBox,
            buttonBox1,
            buttonBox2,
            nameLabel,
            profileNameField,
            descLabel,
            profileDescriptionArea
        );
    }
    
    /**
     * Loads and displays the current profile settings.
     */
    public void loadCurrentSettings() {
        refreshProfileComboBox();
        updateProfileInfo();
    }
    
    /**
     * Refreshes the profile combo box with current profiles.
     * Temporarily disables action events to prevent recursive calls.
     */
    private void refreshProfileComboBox() {
        try {
            DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
            if (profileManager != null) {
                // Temporarily disable action events to prevent recursion
                isUpdatingProfile = true;
                
                ObservableList<String> profileNames = FXCollections.observableArrayList(profileManager.getProfileNames());
                profileComboBox.setItems(profileNames);
                
                // Select current active profile without triggering action event
                DatabaseProfile activeProfile = profileManager.getActiveProfile();
                if (activeProfile != null) {
                    profileComboBox.setValue(activeProfile.getName());
                }
                
                // Update button states
                boolean hasProfiles = profileManager.getAllProfiles().size() > 0;
                boolean canDelete = profileManager.getAllProfiles().size() > 1;
                
                duplicateProfileButton.setDisable(!hasProfiles);
                deleteProfileButton.setDisable(!canDelete);
                renameProfileButton.setDisable(!hasProfiles);
                
                isUpdatingProfile = false;
            }
        } catch (Exception e) {
            isUpdatingProfile = false;
            showError("Failed to load profiles: " + e.getMessage());
        }
    }
    
    /**
     * Updates the profile information display.
     */
    private void updateProfileInfo() {
        try {
            DatabaseProfile activeProfile = DatabaseManager.getActiveProfile();
            if (activeProfile != null) {
                profileNameField.setText(activeProfile.getName());
                
                // Get music file count from database with formatted display
                int musicFileCount = DatabaseManager.getMusicFileCount();
                String countDisplay;
                if (musicFileCount >= 0) {
                    String formattedCount = String.format("%,d", musicFileCount);
                    countDisplay = formattedCount + (musicFileCount == 1 ? " file" : " files");
                } else {
                    countDisplay = "Unknown (database error)";
                }
                
                profileDescriptionArea.setText(
                    "Database Path: " + activeProfile.getDatabasePath() + "\n" +
                    "Created: " + activeProfile.getCreatedDate() + "\n" +
                    "Last Used: " + activeProfile.getLastUsedDate() + "\n" +
                    "Music Files: " + countDisplay
                );
            } else {
                profileNameField.setText("No active profile");
                profileDescriptionArea.setText("No profile information available");
            }
        } catch (Exception e) {
            profileNameField.setText("Error loading profile");
            profileDescriptionArea.setText("Error: " + e.getMessage());
        }
    }
    
    /**
     * Switches to the selected profile from the combo box.
     * Includes protection against recursive calls to prevent StackOverflowError.
     */
    private void switchToSelectedProfile() {
        // Prevent recursive calls that can cause StackOverflowError
        if (isUpdatingProfile) {
            return;
        }
        
        try {
            String selectedProfileName = profileComboBox.getValue();
            if (selectedProfileName != null) {
                // Check if this is actually a different profile
                DatabaseProfile currentActive = DatabaseManager.getActiveProfile();
                if (currentActive != null && selectedProfileName.equals(currentActive.getName())) {
                    // Same profile selected, no need to switch
                    return;
                }
                
                isUpdatingProfile = true;
                
                statusLabel.setText("Switching to profile: " + selectedProfileName);
                statusLabel.setStyle("-fx-text-fill: orange;");
                
                boolean success = DatabaseManager.switchToProfileByName(selectedProfileName);
                
                if (success) {
                    updateProfileInfo();
                    statusLabel.setText("Switched to profile: " + selectedProfileName);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    
                    // Notify parent of profile change
                    if (onProfileChanged != null) {
                        onProfileChanged.run();
                    }
                    
                    // Show success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Profile Switched");
                    alert.setHeaderText("Success");
                    alert.setContentText("Successfully switched to profile: " + selectedProfileName);
                    alert.showAndWait();
                } else {
                    statusLabel.setText("Failed to switch to profile: " + selectedProfileName);
                    statusLabel.setStyle("-fx-text-fill: red;");
                    refreshProfileComboBox(); // Reset selection
                }
            }
        } catch (Exception e) {
            showError("Failed to switch profile: " + e.getMessage());
            refreshProfileComboBox(); // Reset selection
        } finally {
            isUpdatingProfile = false;
        }
    }
    
    /**
     * Creates a new database profile.
     */
    private void createNewProfile() {
        try {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Database Profile");
            dialog.setHeaderText("Create New Profile");
            dialog.setContentText("Profile name:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String profileName = result.get().trim();
                if (profileName.isEmpty()) {
                    showError("Profile name cannot be empty.");
                    return;
                }
                
                DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
                if (profileManager.isProfileNameExists(profileName)) {
                    showError("A profile with that name already exists.");
                    return;
                }
                
                // Get database path
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Database Location for New Profile");
                File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
                
                if (selectedDirectory != null) {
                    String databasePath = new File(selectedDirectory, "mp3org").getAbsolutePath();
                    profileManager.createProfile(profileName, databasePath);
                    
                    refreshProfileComboBox();
                    updateProfileInfo();
                    statusLabel.setText("Created new profile: " + profileName);
                    statusLabel.setStyle("-fx-text-fill: green;");
                }
            }
        } catch (Exception e) {
            showError("Failed to create new profile: " + e.getMessage());
        }
    }
    
    /**
     * Duplicates the current active profile.
     */
    private void duplicateCurrentProfile() {
        try {
            DatabaseProfile activeProfile = DatabaseManager.getActiveProfile();
            if (activeProfile == null) {
                showError("No active profile to duplicate.");
                return;
            }
            
            DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
            String baseName = activeProfile.getName() + " Copy";
            String uniqueName = profileManager.generateUniqueProfileName(baseName);
            
            TextInputDialog dialog = new TextInputDialog(uniqueName);
            dialog.setTitle("Duplicate Profile");
            dialog.setHeaderText("Duplicate Profile: " + activeProfile.getName());
            dialog.setContentText("New profile name:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String newName = result.get().trim();
                if (newName.isEmpty()) {
                    showError("Profile name cannot be empty.");
                    return;
                }
                
                if (profileManager.isProfileNameExists(newName)) {
                    showError("A profile with that name already exists.");
                    return;
                }
                
                profileManager.duplicateProfile(activeProfile.getId(), newName);
                refreshProfileComboBox();
                updateProfileInfo();
                
                statusLabel.setText("Duplicated profile: " + newName);
                statusLabel.setStyle("-fx-text-fill: green;");
            }
        } catch (Exception e) {
            showError("Failed to duplicate profile: " + e.getMessage());
        }
    }
    
    /**
     * Renames the current active profile.
     */
    private void renameCurrentProfile() {
        try {
            DatabaseProfile activeProfile = DatabaseManager.getActiveProfile();
            if (activeProfile == null) {
                showError("No active profile to rename.");
                return;
            }
            
            TextInputDialog dialog = new TextInputDialog(activeProfile.getName());
            dialog.setTitle("Rename Profile");
            dialog.setHeaderText("Rename Profile");
            dialog.setContentText("New name:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String newName = result.get().trim();
                if (newName.isEmpty()) {
                    showError("Profile name cannot be empty.");
                    return;
                }
                
                if (!newName.equals(activeProfile.getName())) {
                    DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
                    if (profileManager.isProfileNameExists(newName)) {
                        showError("A profile with that name already exists.");
                        return;
                    }
                    
                    activeProfile.setName(newName);
                    profileManager.updateProfile(activeProfile);
                    
                    refreshProfileComboBox();
                    updateProfileInfo();
                    statusLabel.setText("Renamed profile to: " + newName);
                    statusLabel.setStyle("-fx-text-fill: green;");
                }
            }
        } catch (Exception e) {
            showError("Failed to rename profile: " + e.getMessage());
        }
    }
    
    /**
     * Deletes the current active profile.
     */
    private void deleteCurrentProfile() {
        try {
            DatabaseProfile activeProfile = DatabaseManager.getActiveProfile();
            if (activeProfile == null) {
                showError("No active profile to delete.");
                return;
            }
            
            DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
            if (profileManager.getAllProfiles().size() <= 1) {
                showError("Cannot delete the last profile.");
                return;
            }
            
            // Confirmation dialog
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Delete Profile");
            confirmAlert.setHeaderText("Delete Profile: " + activeProfile.getName());
            confirmAlert.setContentText(
                "Are you sure you want to delete this profile?\n\n" +
                "This will permanently remove the profile configuration.\n" +
                "The database files will be deleted from disk.\n\n" +
                "This action cannot be undone."
            );
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        profileManager.removeProfile(activeProfile.getId());
                        refreshProfileComboBox();
                        updateProfileInfo();
                        
                        statusLabel.setText("Deleted profile: " + activeProfile.getName());
                        statusLabel.setStyle("-fx-text-fill: green;");
                        
                        // Notify parent of profile change
                        if (onProfileChanged != null) {
                            onProfileChanged.run();
                        }
                    } catch (Exception e) {
                        showError("Failed to delete profile: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            showError("Failed to delete profile: " + e.getMessage());
        }
    }
    
    /**
     * Displays an error message to the user.
     * 
     * @param message The error message to display
     */
    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Profile Management Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Gets the profile combo box for external access if needed.
     * 
     * @return The ComboBox for profile selection
     */
    public ComboBox<String> getProfileComboBox() {
        return profileComboBox;
    }
}