package org.hasting.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.hasting.util.DatabaseConfig;
import org.hasting.util.DatabaseManager;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.DatabaseProfileManager;
import org.hasting.util.FuzzySearchConfig;
import org.hasting.util.HelpSystem;
import org.hasting.util.PathTemplateManager;
import org.hasting.model.PathTemplate;

import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;

/**
 * UI component for configuring application settings including database location and file type filters.
 */
public class ConfigurationView extends BorderPane {
    
    private TextField currentPathField;
    private TextArea configInfoArea;
    private Button changeLocationButton;
    private Button reloadConfigButton;
    private Button openLocationButton;
    private Label statusLabel;
    
    // File type filter components
    private ListView<CheckBox> fileTypesList;
    private Button selectAllButton;
    private Button selectNoneButton;
    private Button applyFiltersButton;
    
    // Database profile components
    private ComboBox<String> profileComboBox;
    private Button newProfileButton;
    private Button duplicateProfileButton;
    private Button deleteProfileButton;
    private Button renameProfileButton;
    private TextField profileNameField;
    private TextArea profileDescriptionArea;
    
    // Fuzzy search configuration components
    private ComboBox<String> fuzzyPresetComboBox;
    private Slider titleSimilaritySlider;
    private Slider artistSimilaritySlider;
    private Slider albumSimilaritySlider;
    private Spinner<Integer> durationToleranceSpinner;
    private Slider durationPercentSlider;
    private CheckBox ignoreCaseCheckBox;
    private CheckBox ignorePunctuationCheckBox;
    private CheckBox trackNumberMatchCheckBox;
    private CheckBox ignoreArtistPrefixesCheckBox;
    private CheckBox ignoreFeaturingCheckBox;
    private CheckBox ignoreAlbumEditionsCheckBox;
    private Spinner<Integer> minFieldsSpinner;
    private Button applyFuzzyConfigButton;
    private Button resetFuzzyConfigButton;
    
    // Path template configuration components
    private ComboBox<String> templateComboBox;
    private TextField customTemplateField;
    private ComboBox<PathTemplate.TextFormat> textFormatComboBox;
    private CheckBox useSubdirectoriesCheckBox;
    private Spinner<Integer> subdirectoryLevelsSpinner;
    private Label templatePreviewLabel;
    private Button applyTemplateButton;
    private Button resetTemplateButton;
    
    public ConfigurationView() {
        initializeComponents();
        layoutComponents();
        updateDisplayedInfo();
    }
    
    private void initializeComponents() {
        // Current database path display
        currentPathField = new TextField();
        currentPathField.setEditable(false);
        currentPathField.setFocusTraversable(false);  // Prevent focus
        currentPathField.setMouseTransparent(true);   // Prevent mouse interaction
        currentPathField.setPrefWidth(400);
        currentPathField.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc;"); // Visual indication it's read-only
        HelpSystem.setTooltip(currentPathField, "config.current.path");
        
        // Configuration information display
        configInfoArea = new TextArea();
        configInfoArea.setEditable(false);
        configInfoArea.setPrefRowCount(10);
        configInfoArea.setWrapText(true);
        
        // Buttons
        changeLocationButton = new Button("Change Database Location");
        changeLocationButton.setOnAction(e -> changeLocation());
        HelpSystem.setTooltip(changeLocationButton, "config.change.location");
        
        reloadConfigButton = new Button("Reload Configuration");
        reloadConfigButton.setOnAction(e -> reloadConfiguration());
        HelpSystem.setTooltip(reloadConfigButton, "config.reload.config");
        
        openLocationButton = new Button("Open Database Folder");
        openLocationButton.setOnAction(e -> openDatabaseLocation());
        HelpSystem.setTooltip(openLocationButton, "config.open.location");
        
        // Status label
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: green;");
        
        // File type filter components
        initializeFileTypeComponents();
        
        // Profile management components
        initializeProfileComponents();
        
        // Fuzzy search components
        initializeFuzzySearchComponents();
        
        // Path template components
        initializePathTemplateComponents();
    }
    
    private void initializeFileTypeComponents() {
        // Create checkboxes for each supported file type
        fileTypesList = new ListView<>();
        fileTypesList.setPrefHeight(200);
        HelpSystem.setTooltip(fileTypesList, "config.filetypes.list");
        
        // Initialize checkboxes for all supported types
        refreshFileTypesList();
        
        // File type filter buttons
        selectAllButton = new Button("Select All");
        selectAllButton.setOnAction(e -> selectAllFileTypes());
        HelpSystem.setTooltip(selectAllButton, "config.filetypes.selectall");
        
        selectNoneButton = new Button("Select None");
        selectNoneButton.setOnAction(e -> selectNoFileTypes());
        HelpSystem.setTooltip(selectNoneButton, "config.filetypes.selectnone");
        
        applyFiltersButton = new Button("Apply File Type Filters");
        applyFiltersButton.setOnAction(e -> applyFileTypeFilters());
        HelpSystem.setTooltip(applyFiltersButton, "config.filetypes.apply");
    }
    
    private void initializeProfileComponents() {
        // Profile selection combo box
        profileComboBox = new ComboBox<>();
        profileComboBox.setPrefWidth(300);
        profileComboBox.setOnAction(e -> switchToSelectedProfile());
        HelpSystem.setTooltip(profileComboBox, "config.profile.combo");
        
        // Profile management buttons
        newProfileButton = new Button("New Profile");
        newProfileButton.setOnAction(e -> createNewProfile());
        HelpSystem.setTooltip(newProfileButton, "config.profile.new");
        
        duplicateProfileButton = new Button("Duplicate");
        duplicateProfileButton.setOnAction(e -> duplicateCurrentProfile());
        HelpSystem.setTooltip(duplicateProfileButton, "config.profile.duplicate");
        
        deleteProfileButton = new Button("Delete");
        deleteProfileButton.setOnAction(e -> deleteCurrentProfile());
        HelpSystem.setTooltip(deleteProfileButton, "config.profile.delete");
        
        renameProfileButton = new Button("Rename");
        renameProfileButton.setOnAction(e -> renameCurrentProfile());
        HelpSystem.setTooltip(renameProfileButton, "config.profile.rename");
        
        // Profile details
        profileNameField = new TextField();
        profileNameField.setPromptText("Profile name...");
        profileNameField.setPrefWidth(250);
        HelpSystem.setTooltip(profileNameField, "config.profile.name");
        
        profileDescriptionArea = new TextArea();
        profileDescriptionArea.setPromptText("Profile description (optional)...");
        profileDescriptionArea.setPrefRowCount(3);
        profileDescriptionArea.setWrapText(true);
        HelpSystem.setTooltip(profileDescriptionArea, "config.profile.description");
        
        // Load profile data
        refreshProfileComboBox();
    }
    
    private void initializeFuzzySearchComponents() {
        // Preset selector
        fuzzyPresetComboBox = new ComboBox<>();
        fuzzyPresetComboBox.getItems().addAll("Strict", "Balanced", "Lenient", "Custom");
        fuzzyPresetComboBox.setValue("Balanced");
        fuzzyPresetComboBox.setOnAction(e -> loadFuzzyPreset());
        HelpSystem.setTooltip(fuzzyPresetComboBox, "config.fuzzy.preset");
        
        // Similarity sliders
        titleSimilaritySlider = createSimilaritySlider("Title Similarity");
        HelpSystem.setTooltip(titleSimilaritySlider, "config.fuzzy.title.similarity");
        artistSimilaritySlider = createSimilaritySlider("Artist Similarity");
        HelpSystem.setTooltip(artistSimilaritySlider, "config.fuzzy.artist.similarity");
        albumSimilaritySlider = createSimilaritySlider("Album Similarity");
        HelpSystem.setTooltip(albumSimilaritySlider, "config.fuzzy.album.similarity");
        
        // Duration tolerance
        durationToleranceSpinner = new Spinner<>(0, 120, 10);
        durationToleranceSpinner.setEditable(true);
        durationToleranceSpinner.setPrefWidth(80);
        HelpSystem.setTooltip(durationToleranceSpinner, "config.fuzzy.duration.tolerance");
        
        durationPercentSlider = new Slider(0, 20, 5);
        durationPercentSlider.setShowTickLabels(true);
        durationPercentSlider.setShowTickMarks(true);
        durationPercentSlider.setMajorTickUnit(5);
        durationPercentSlider.setPrefWidth(200);
        HelpSystem.setTooltip(durationPercentSlider, "config.fuzzy.duration.percent");
        
        // Checkboxes
        ignoreCaseCheckBox = new CheckBox("Ignore case differences");
        HelpSystem.setTooltip(ignoreCaseCheckBox, "config.fuzzy.ignore.case");
        ignorePunctuationCheckBox = new CheckBox("Ignore punctuation");
        HelpSystem.setTooltip(ignorePunctuationCheckBox, "config.fuzzy.ignore.punctuation");
        trackNumberMatchCheckBox = new CheckBox("Track numbers must match");
        HelpSystem.setTooltip(trackNumberMatchCheckBox, "config.fuzzy.track.match");
        ignoreArtistPrefixesCheckBox = new CheckBox("Ignore artist prefixes (The, A, An)");
        HelpSystem.setTooltip(ignoreArtistPrefixesCheckBox, "config.fuzzy.ignore.prefixes");
        ignoreFeaturingCheckBox = new CheckBox("Ignore featuring artists");
        HelpSystem.setTooltip(ignoreFeaturingCheckBox, "config.fuzzy.ignore.featuring");
        ignoreAlbumEditionsCheckBox = new CheckBox("Ignore album editions (Deluxe, Remastered)");
        HelpSystem.setTooltip(ignoreAlbumEditionsCheckBox, "config.fuzzy.ignore.editions");
        
        // Minimum fields spinner
        minFieldsSpinner = new Spinner<>(1, 4, 2);
        minFieldsSpinner.setEditable(true);
        minFieldsSpinner.setPrefWidth(60);
        HelpSystem.setTooltip(minFieldsSpinner, "config.fuzzy.min.fields");
        
        // Buttons
        applyFuzzyConfigButton = new Button("Apply Fuzzy Settings");
        applyFuzzyConfigButton.setOnAction(e -> applyFuzzySearchConfig());
        HelpSystem.setTooltip(applyFuzzyConfigButton, "config.fuzzy.apply");
        
        resetFuzzyConfigButton = new Button("Reset to Defaults");
        resetFuzzyConfigButton.setOnAction(e -> resetFuzzySearchConfig());
        HelpSystem.setTooltip(resetFuzzyConfigButton, "config.fuzzy.reset");
        
        // Load current configuration
        loadCurrentFuzzyConfig();
    }
    
    private Slider createSimilaritySlider(String name) {
        Slider slider = new Slider(0, 100, 85);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(4);
        slider.setPrefWidth(200);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Update preset to "Custom" when user changes values manually
            if (!fuzzyPresetComboBox.getValue().equals("Custom")) {
                fuzzyPresetComboBox.setValue("Custom");
            }
        });
        return slider;
    }
    
    private Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }
    
    private void initializePathTemplateComponents() {
        PathTemplateManager templateManager = PathTemplateManager.getInstance();
        
        // Template selector
        templateComboBox = new ComboBox<>();
        String[] descriptions = templateManager.getPredefinedTemplateDescriptions();
        templateComboBox.getItems().addAll(descriptions);
        templateComboBox.getItems().add("Custom Template");
        templateComboBox.setValue(descriptions[0]); // Default to first template
        templateComboBox.setOnAction(e -> updateTemplateFromCombo());
        templateComboBox.setPrefWidth(400);
        HelpSystem.setTooltip(templateComboBox, "config.template.combo");
        
        // Custom template field
        customTemplateField = new TextField();
        customTemplateField.setPromptText("Enter custom template like: {artist}/{album}/{track_number:02d}-{title}.{file_type}");
        customTemplateField.setPrefWidth(500);
        customTemplateField.textProperty().addListener((obs, oldVal, newVal) -> updateTemplatePreview());
        HelpSystem.setTooltip(customTemplateField, "config.template.custom");
        
        // Text format selector
        textFormatComboBox = new ComboBox<>();
        for (PathTemplate.TextFormat format : PathTemplate.TextFormat.values()) {
            textFormatComboBox.getItems().add(format);
        }
        textFormatComboBox.setValue(PathTemplate.TextFormat.UNDERSCORE);
        textFormatComboBox.setOnAction(e -> updateTemplatePreview());
        HelpSystem.setTooltip(textFormatComboBox, "config.template.format");
        
        // Subdirectory options
        useSubdirectoriesCheckBox = new CheckBox("Group files by alphabetical subdirectories");
        useSubdirectoriesCheckBox.setSelected(true);
        useSubdirectoriesCheckBox.setOnAction(e -> {
            subdirectoryLevelsSpinner.setDisable(!useSubdirectoriesCheckBox.isSelected());
            updateTemplatePreview();
        });
        HelpSystem.setTooltip(useSubdirectoriesCheckBox, "config.template.subdirs");
        
        subdirectoryLevelsSpinner = new Spinner<>(0, 10, 7);
        subdirectoryLevelsSpinner.setPrefWidth(80);
        subdirectoryLevelsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTemplatePreview());
        HelpSystem.setTooltip(subdirectoryLevelsSpinner, "config.template.levels");
        
        // Template preview
        templatePreviewLabel = new Label();
        templatePreviewLabel.setStyle("-fx-font-family: monospace; -fx-background-color: #f8f8f8; -fx-padding: 5; -fx-border-color: #cccccc;");
        templatePreviewLabel.setWrapText(true);
        templatePreviewLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Control buttons
        applyTemplateButton = new Button("Apply Path Template");
        applyTemplateButton.setOnAction(e -> applyPathTemplate());
        HelpSystem.setTooltip(applyTemplateButton, "config.template.apply");
        
        resetTemplateButton = new Button("Reset to Default");
        resetTemplateButton.setOnAction(e -> resetPathTemplate());
        HelpSystem.setTooltip(resetTemplateButton, "config.template.reset");
        
        // Load current template
        loadCurrentPathTemplate();
        updateTemplatePreview();
    }
    
    private void layoutComponents() {
        // Create main content container
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));
        
        // Title
        Label titleLabel = new Label("Configuration");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Current location section
        VBox currentLocationSection = new VBox(10);
        Label currentLocationLabel = new Label("Current Database Location:");
        currentLocationLabel.setStyle("-fx-font-weight: bold;");
        
        HBox pathBox = new HBox(10);
        pathBox.getChildren().addAll(currentPathField, changeLocationButton, openLocationButton);
        
        currentLocationSection.getChildren().addAll(currentLocationLabel, pathBox);
        
        // Profile management section
        VBox profileSection = new VBox(10);
        HBox profileHeaderBox = new HBox(10);
        Label profileLabel = new Label("Database Profiles:");
        profileLabel.setStyle("-fx-font-weight: bold;");
        Button profileHelpButton = new Button("?");
        profileHelpButton.setPrefSize(25, 25);
        profileHelpButton.setOnAction(e -> HelpSystem.showHelpDialog("profiles.help", "Database Profiles Help", getScene().getWindow()));
        profileHeaderBox.getChildren().addAll(profileLabel, profileHelpButton);
        
        Label profileDescription = new Label("Manage multiple database configurations:");
        profileDescription.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        HBox profileSelectorBox = new HBox(10);
        Label currentProfileLabel = new Label("Active Profile:");
        profileSelectorBox.getChildren().addAll(currentProfileLabel, profileComboBox);
        
        HBox profileButtonBox = new HBox(10);
        profileButtonBox.getChildren().addAll(newProfileButton, duplicateProfileButton, renameProfileButton, deleteProfileButton);
        
        profileSection.getChildren().addAll(profileHeaderBox, profileDescription, profileSelectorBox, profileButtonBox);
        
        // Configuration information section
        VBox configInfoSection = new VBox(10);
        Label configInfoLabel = new Label("Configuration Information:");
        configInfoLabel.setStyle("-fx-font-weight: bold;");
        
        configInfoSection.getChildren().addAll(configInfoLabel, configInfoArea);
        
        // Actions section
        VBox actionsSection = new VBox(10);
        Label actionsLabel = new Label("Actions:");
        actionsLabel.setStyle("-fx-font-weight: bold;");
        
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(reloadConfigButton);
        
        actionsSection.getChildren().addAll(actionsLabel, buttonBox);
        
        // Instructions section
        VBox instructionsSection = new VBox(10);
        Label instructionsLabel = new Label("Configuration Methods:");
        instructionsLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea instructionsArea = new TextArea();
        instructionsArea.setEditable(false);
        instructionsArea.setPrefRowCount(8);
        instructionsArea.setWrapText(true);
        instructionsArea.setText(
            "You can configure the database location using any of these methods (in order of precedence):\n\n" +
            "1. SYSTEM PROPERTY:\n" +
            "   java -Dmp3org.database.path=/path/to/database -jar mp3org.jar\n\n" +
            "2. ENVIRONMENT VARIABLE:\n" +
            "   export MP3ORG_DATABASE_PATH=/path/to/database\n\n" +
            "3. CONFIGURATION FILE (mp3org.properties):\n" +
            "   database.path=/path/to/database\n\n" +
            "4. DEFAULT LOCATION:\n" +
            "   ./mp3org (in current working directory)\n\n" +
            "The configuration file can be placed in the current directory or your home directory."
        );
        
        instructionsSection.getChildren().addAll(instructionsLabel, instructionsArea);
        
        // File type filter section
        VBox fileTypeSection = new VBox(10);
        Label fileTypeLabel = new Label("File Type Filters:");
        fileTypeLabel.setStyle("-fx-font-weight: bold;");
        
        Label fileTypeDescription = new Label("Select which music file types to include in searches and directory scans:");
        fileTypeDescription.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        HBox filterButtonBox = new HBox(10);
        filterButtonBox.getChildren().addAll(selectAllButton, selectNoneButton, applyFiltersButton);
        
        fileTypeSection.getChildren().addAll(fileTypeLabel, fileTypeDescription, fileTypesList, filterButtonBox);
        
        // Fuzzy search configuration section
        VBox fuzzySearchSection = createFuzzySearchSection();
        
        // Path template configuration section
        VBox pathTemplateSection = createPathTemplateSection();
        
        // Add all sections to main content
        mainContent.getChildren().addAll(
            titleLabel,
            new Separator(),
            profileSection,
            new Separator(),
            currentLocationSection,
            new Separator(),
            fileTypeSection,
            new Separator(),
            pathTemplateSection,
            new Separator(),
            fuzzySearchSection,
            new Separator(),
            configInfoSection,
            new Separator(),
            actionsSection,
            new Separator(),
            instructionsSection
        );
        
        // Create scroll pane for main content
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: white;");
        
        // Set minimum height for scroll pane to ensure it takes available space
        scrollPane.setMinHeight(400);
        
        // Add status section to bottom (always visible)
        HBox statusSection = new HBox();
        statusSection.setPadding(new Insets(10));
        statusSection.getChildren().add(statusLabel);
        statusSection.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        
        // Layout in BorderPane
        setCenter(scrollPane);
        setBottom(statusSection);
        
        // Make text areas grow within their containers
        VBox.setVgrow(configInfoArea, Priority.ALWAYS);
        VBox.setVgrow(instructionsArea, Priority.NEVER);
    }
    
    private void updateDisplayedInfo() {
        try {
            DatabaseConfig config = DatabaseManager.getConfig();
            
            // Update current path
            currentPathField.setText(config.getDatabasePath());
            
            // Update configuration info
            configInfoArea.setText(config.getConfigurationInfo());
            
            // Update status
            statusLabel.setText("Configuration loaded successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            statusLabel.setText("Error loading configuration: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }
    
    private void changeLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Database Location");
        
        // Set initial directory to current database location's parent
        try {
            File currentPath = new File(DatabaseManager.getConfig().getDatabasePath());
            File parentDir = currentPath.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                directoryChooser.setInitialDirectory(parentDir);
            }
        } catch (Exception e) {
            // Use default if there's an issue
        }
        
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory != null) {
            try {
                // Create database subdirectory name
                String newDatabasePath = new File(selectedDirectory, "mp3org").getAbsolutePath();
                
                // Show confirmation dialog
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Change Database Location");
                confirmAlert.setHeaderText("Confirm Database Location Change");
                confirmAlert.setContentText(
                    "Change database location to:\n" + newDatabasePath + "\n\n" +
                    "This will:\n" +
                    "• Close the current database connection\n" +
                    "• Create a new database at the selected location\n" +
                    "• Your existing data will remain at the old location\n\n" +
                    "Continue?"
                );
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        changeDatabaseLocation(newDatabasePath);
                    }
                });
                
            } catch (Exception e) {
                showError("Failed to change database location: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void changeDatabaseLocation(String newPath) {
        try {
            statusLabel.setText("Changing database location...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            // Change the database location
            DatabaseManager.changeDatabaseLocation(newPath);
            
            // Update the display
            updateDisplayedInfo();
            
            statusLabel.setText("Database location changed successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Show success dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Database Location Changed");
            successAlert.setHeaderText("Success");
            successAlert.setContentText("Database location has been changed to:\n" + newPath);
            successAlert.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to change database location: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void reloadConfiguration() {
        try {
            statusLabel.setText("Reloading configuration...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            DatabaseManager.reloadConfig();
            updateDisplayedInfo();
            
            statusLabel.setText("Configuration reloaded successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            showError("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openDatabaseLocation() {
        try {
            String databasePath = DatabaseManager.getConfig().getDatabasePath();
            File databaseDir = new File(databasePath);
            
            // If database directory doesn't exist, try to open parent directory
            if (!databaseDir.exists()) {
                File parentDir = databaseDir.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    databaseDir = parentDir;
                }
            }
            
            if (databaseDir.exists()) {
                // Try to open the directory in the system file manager
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(databaseDir);
                    statusLabel.setText("Opened database location in file manager");
                    statusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    showError("Desktop operations not supported on this system");
                }
            } else {
                showError("Database directory does not exist: " + databasePath);
            }
            
        } catch (Exception e) {
            showError("Failed to open database location: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Error");
        errorAlert.setHeaderText("Operation Failed");
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }
    
    /**
     * Refreshes the displayed information (useful when called from external components).
     */
    public void refresh() {
        updateDisplayedInfo();
        refreshFileTypesList();
        refreshProfileComboBox();
        loadCurrentFuzzyConfig();
    }
    
    private void refreshFileTypesList() {
        try {
            DatabaseConfig config = DatabaseManager.getConfig();
            Set<String> enabledTypes = config.getEnabledFileTypes();
            
            fileTypesList.getItems().clear();
            
            for (String fileType : DatabaseConfig.getAllSupportedTypes()) {
                CheckBox checkBox = new CheckBox(fileType.toUpperCase());
                checkBox.setSelected(enabledTypes.contains(fileType.toLowerCase()));
                checkBox.setUserData(fileType.toLowerCase());
                fileTypesList.getItems().add(checkBox);
            }
        } catch (Exception e) {
            showError("Failed to load file type settings: " + e.getMessage());
        }
    }
    
    private void selectAllFileTypes() {
        for (CheckBox checkBox : fileTypesList.getItems()) {
            checkBox.setSelected(true);
        }
    }
    
    private void selectNoFileTypes() {
        for (CheckBox checkBox : fileTypesList.getItems()) {
            checkBox.setSelected(false);
        }
    }
    
    private void applyFileTypeFilters() {
        try {
            statusLabel.setText("Applying file type filters...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            // Collect selected file types
            Set<String> selectedTypes = new HashSet<>();
            for (CheckBox checkBox : fileTypesList.getItems()) {
                if (checkBox.isSelected()) {
                    selectedTypes.add((String) checkBox.getUserData());
                }
            }
            
            if (selectedTypes.isEmpty()) {
                showError("At least one file type must be selected.");
                return;
            }
            
            // Update configuration
            DatabaseConfig config = DatabaseManager.getConfig();
            config.setEnabledFileTypes(selectedTypes);
            
            // Update display
            updateDisplayedInfo();
            
            statusLabel.setText("File type filters applied successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Show success dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("File Type Filters Updated");
            successAlert.setHeaderText("Success");
            successAlert.setContentText("File type filters have been updated. " +
                "The new filters will apply to future searches and directory scans.");
            successAlert.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to apply file type filters: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Profile management methods
    
    private void refreshProfileComboBox() {
        try {
            DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
            if (profileManager != null) {
                ObservableList<String> profileNames = FXCollections.observableArrayList(profileManager.getProfileNames());
                profileComboBox.setItems(profileNames);
                
                // Select current active profile
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
            }
        } catch (Exception e) {
            showError("Failed to load profiles: " + e.getMessage());
        }
    }
    
    private void switchToSelectedProfile() {
        try {
            String selectedProfileName = profileComboBox.getValue();
            if (selectedProfileName != null) {
                statusLabel.setText("Switching to profile: " + selectedProfileName);
                statusLabel.setStyle("-fx-text-fill: orange;");
                
                boolean success = DatabaseManager.switchToProfileByName(selectedProfileName);
                
                if (success) {
                    updateDisplayedInfo();
                    refreshFileTypesList();
                    loadCurrentFuzzyConfig();
                    statusLabel.setText("Switched to profile: " + selectedProfileName);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    
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
        }
    }
    
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
                    statusLabel.setText("Created new profile: " + profileName);
                    statusLabel.setStyle("-fx-text-fill: green;");
                }
            }
        } catch (Exception e) {
            showError("Failed to create new profile: " + e.getMessage());
        }
    }
    
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
                
                statusLabel.setText("Duplicated profile: " + newName);
                statusLabel.setStyle("-fx-text-fill: green;");
            }
        } catch (Exception e) {
            showError("Failed to duplicate profile: " + e.getMessage());
        }
    }
    
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
                    statusLabel.setText("Renamed profile to: " + newName);
                    statusLabel.setStyle("-fx-text-fill: green;");
                }
            }
        } catch (Exception e) {
            showError("Failed to rename profile: " + e.getMessage());
        }
    }
    
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
            
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Delete Profile");
            confirmAlert.setHeaderText("Delete Profile: " + activeProfile.getName());
            confirmAlert.setContentText("Are you sure you want to delete this profile?\n\n" +
                    "This will not delete the actual database files, only the profile configuration.");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String profileName = activeProfile.getName();
                boolean success = profileManager.removeProfile(activeProfile.getId());
                
                if (success) {
                    // Refresh UI to reflect new active profile
                    updateDisplayedInfo();
                    refreshFileTypesList();
                    refreshProfileComboBox();
                    
                    statusLabel.setText("Deleted profile: " + profileName);
                    statusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    showError("Failed to delete profile: " + profileName);
                }
            }
        } catch (Exception e) {
            showError("Failed to delete profile: " + e.getMessage());
        }
    }
    
    // Fuzzy search configuration methods
    
    private VBox createFuzzySearchSection() {
        VBox fuzzySearchSection = new VBox(10);
        HBox fuzzyHeaderBox = new HBox(10);
        Label fuzzyLabel = new Label("Duplicate Detection Settings:");
        fuzzyLabel.setStyle("-fx-font-weight: bold;");
        Button fuzzyHelpButton = new Button("?");
        fuzzyHelpButton.setPrefSize(25, 25);
        fuzzyHelpButton.setOnAction(e -> HelpSystem.showHelpDialog("config.fuzzy.help", "Fuzzy Search Configuration Help", getScene().getWindow()));
        fuzzyHeaderBox.getChildren().addAll(fuzzyLabel, fuzzyHelpButton);
        
        Label fuzzyDescription = new Label("Configure how similar songs are considered duplicates:");
        fuzzyDescription.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        // Preset selection
        HBox presetBox = new HBox(10);
        Label presetLabel = new Label("Preset:");
        presetBox.getChildren().addAll(presetLabel, fuzzyPresetComboBox);
        
        // Similarity settings
        GridPane similarityGrid = new GridPane();
        similarityGrid.setHgap(10);
        similarityGrid.setVgap(8);
        similarityGrid.setPadding(new Insets(10));
        similarityGrid.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #dddddd; -fx-border-radius: 5;");
        
        // Row 0: Title similarity
        similarityGrid.add(new Label("Title Similarity:"), 0, 0);
        similarityGrid.add(titleSimilaritySlider, 1, 0);
        Label titleValueLabel = new Label();
        titleValueLabel.textProperty().bind(titleSimilaritySlider.valueProperty().asString("%.0f%%"));
        similarityGrid.add(titleValueLabel, 2, 0);
        
        // Row 1: Artist similarity
        similarityGrid.add(new Label("Artist Similarity:"), 0, 1);
        similarityGrid.add(artistSimilaritySlider, 1, 1);
        Label artistValueLabel = new Label();
        artistValueLabel.textProperty().bind(artistSimilaritySlider.valueProperty().asString("%.0f%%"));
        similarityGrid.add(artistValueLabel, 2, 1);
        
        // Row 2: Album similarity
        similarityGrid.add(new Label("Album Similarity:"), 0, 2);
        similarityGrid.add(albumSimilaritySlider, 1, 2);
        Label albumValueLabel = new Label();
        albumValueLabel.textProperty().bind(albumSimilaritySlider.valueProperty().asString("%.0f%%"));
        similarityGrid.add(albumValueLabel, 2, 2);
        
        // Row 3: Duration tolerance
        similarityGrid.add(new Label("Duration Tolerance:"), 0, 3);
        HBox durationBox = new HBox(5);
        durationBox.getChildren().addAll(durationToleranceSpinner, new Label("seconds OR"));
        similarityGrid.add(durationBox, 1, 3);
        
        // Row 4: Duration percentage
        HBox percentBox = new HBox(5);
        Label percentValueLabel = new Label();
        percentValueLabel.textProperty().bind(durationPercentSlider.valueProperty().asString("±%.1f%%"));
        percentBox.getChildren().addAll(durationPercentSlider, percentValueLabel);
        similarityGrid.add(percentBox, 1, 4);
        
        // Row 5: Minimum fields
        similarityGrid.add(new Label("Min Fields Match:"), 0, 5);
        HBox minFieldsBox = new HBox(5);
        minFieldsBox.getChildren().addAll(minFieldsSpinner, new Label("out of 4"));
        similarityGrid.add(minFieldsBox, 1, 5);
        
        // Options checkboxes
        VBox optionsBox = new VBox(5);
        optionsBox.setPadding(new Insets(10));
        optionsBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #dddddd; -fx-border-radius: 5;");
        
        Label optionsLabel = new Label("Options:");
        optionsLabel.setStyle("-fx-font-weight: bold;");
        
        // Organize checkboxes in two columns for better space utilization
        GridPane checkBoxGrid = new GridPane();
        checkBoxGrid.setHgap(15);
        checkBoxGrid.setVgap(5);
        
        checkBoxGrid.add(ignoreCaseCheckBox, 0, 0);
        checkBoxGrid.add(ignorePunctuationCheckBox, 1, 0);
        checkBoxGrid.add(trackNumberMatchCheckBox, 0, 1);
        checkBoxGrid.add(ignoreArtistPrefixesCheckBox, 1, 1);
        checkBoxGrid.add(ignoreFeaturingCheckBox, 0, 2);
        checkBoxGrid.add(ignoreAlbumEditionsCheckBox, 1, 2);
        
        optionsBox.getChildren().addAll(optionsLabel, checkBoxGrid);
        
        // Buttons
        HBox fuzzyButtonBox = new HBox(10);
        fuzzyButtonBox.getChildren().addAll(applyFuzzyConfigButton, resetFuzzyConfigButton);
        
        fuzzySearchSection.getChildren().addAll(
            fuzzyHeaderBox,
            fuzzyDescription,
            presetBox,
            new Separator(),
            createBoldLabel("Similarity Thresholds:"),
            similarityGrid,
            new Separator(),
            optionsBox,
            fuzzyButtonBox
        );
        
        return fuzzySearchSection;
    }
    
    private void loadCurrentFuzzyConfig() {
        try {
            DatabaseProfile activeProfile = DatabaseManager.getActiveProfile();
            if (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) {
                FuzzySearchConfig config = activeProfile.getFuzzySearchConfig();
                updateFuzzySearchUI(config);
            }
        } catch (Exception e) {
            showError("Failed to load fuzzy search configuration: " + e.getMessage());
        }
    }
    
    private void updateFuzzySearchUI(FuzzySearchConfig config) {
        titleSimilaritySlider.setValue(config.getTitleSimilarityThreshold());
        artistSimilaritySlider.setValue(config.getArtistSimilarityThreshold());
        albumSimilaritySlider.setValue(config.getAlbumSimilarityThreshold());
        durationToleranceSpinner.getValueFactory().setValue(config.getDurationToleranceSeconds());
        durationPercentSlider.setValue(config.getDurationTolerancePercent());
        
        ignoreCaseCheckBox.setSelected(config.isIgnoreCaseDifferences());
        ignorePunctuationCheckBox.setSelected(config.isIgnorePunctuation());
        trackNumberMatchCheckBox.setSelected(config.isTrackNumberMustMatch());
        ignoreArtistPrefixesCheckBox.setSelected(config.isIgnoreArtistPrefixes());
        ignoreFeaturingCheckBox.setSelected(config.isIgnoreFeaturing());
        ignoreAlbumEditionsCheckBox.setSelected(config.isIgnoreAlbumEditions());
        
        minFieldsSpinner.getValueFactory().setValue(config.getMinimumFieldsToMatch());
        
        // Determine preset
        String preset = determinePreset(config);
        fuzzyPresetComboBox.setValue(preset);
    }
    
    private String determinePreset(FuzzySearchConfig config) {
        // Check if it matches any preset
        FuzzySearchConfig strict = FuzzySearchConfig.createStrictConfig();
        FuzzySearchConfig balanced = FuzzySearchConfig.createBalancedConfig();
        FuzzySearchConfig lenient = FuzzySearchConfig.createLenientConfig();
        
        if (configsMatch(config, strict)) return "Strict";
        if (configsMatch(config, balanced)) return "Balanced";
        if (configsMatch(config, lenient)) return "Lenient";
        return "Custom";
    }
    
    private boolean configsMatch(FuzzySearchConfig config1, FuzzySearchConfig config2) {
        return Math.abs(config1.getTitleSimilarityThreshold() - config2.getTitleSimilarityThreshold()) < 0.1 &&
               Math.abs(config1.getArtistSimilarityThreshold() - config2.getArtistSimilarityThreshold()) < 0.1 &&
               Math.abs(config1.getAlbumSimilarityThreshold() - config2.getAlbumSimilarityThreshold()) < 0.1 &&
               config1.getDurationToleranceSeconds() == config2.getDurationToleranceSeconds() &&
               Math.abs(config1.getDurationTolerancePercent() - config2.getDurationTolerancePercent()) < 0.1 &&
               config1.isTrackNumberMustMatch() == config2.isTrackNumberMustMatch() &&
               config1.getMinimumFieldsToMatch() == config2.getMinimumFieldsToMatch();
    }
    
    private void loadFuzzyPreset() {
        String selectedPreset = fuzzyPresetComboBox.getValue();
        if (selectedPreset == null || selectedPreset.equals("Custom")) return;
        
        FuzzySearchConfig config;
        switch (selectedPreset) {
            case "Strict":
                config = FuzzySearchConfig.createStrictConfig();
                break;
            case "Lenient":
                config = FuzzySearchConfig.createLenientConfig();
                break;
            case "Balanced":
            default:
                config = FuzzySearchConfig.createBalancedConfig();
                break;
        }
        
        updateFuzzySearchUI(config);
    }
    
    private void applyFuzzySearchConfig() {
        try {
            DatabaseProfile activeProfile = DatabaseManager.getActiveProfile();
            if (activeProfile == null) {
                showError("No active profile to update.");
                return;
            }
            
            statusLabel.setText("Applying fuzzy search configuration...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            // Create configuration from UI values
            FuzzySearchConfig config = new FuzzySearchConfig("Custom");
            config.setTitleSimilarityThreshold(titleSimilaritySlider.getValue());
            config.setArtistSimilarityThreshold(artistSimilaritySlider.getValue());
            config.setAlbumSimilarityThreshold(albumSimilaritySlider.getValue());
            config.setDurationToleranceSeconds(durationToleranceSpinner.getValue());
            config.setDurationTolerancePercent(durationPercentSlider.getValue());
            
            config.setIgnoreCaseDifferences(ignoreCaseCheckBox.isSelected());
            config.setIgnorePunctuation(ignorePunctuationCheckBox.isSelected());
            config.setTrackNumberMustMatch(trackNumberMatchCheckBox.isSelected());
            config.setIgnoreArtistPrefixes(ignoreArtistPrefixesCheckBox.isSelected());
            config.setIgnoreFeaturing(ignoreFeaturingCheckBox.isSelected());
            config.setIgnoreAlbumEditions(ignoreAlbumEditionsCheckBox.isSelected());
            
            config.setMinimumFieldsToMatch(minFieldsSpinner.getValue());
            
            // Update the profile
            activeProfile.setFuzzySearchConfig(config);
            DatabaseManager.getProfileManager().updateProfile(activeProfile);
            
            statusLabel.setText("Fuzzy search configuration updated successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Update the configuration info display
            updateDisplayedInfo();
            
            // Show success dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Fuzzy Search Updated");
            successAlert.setHeaderText("Success");
            successAlert.setContentText("Fuzzy search configuration has been updated.\n" +
                "These settings will be used for duplicate detection.");
            successAlert.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to apply fuzzy search configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void resetFuzzySearchConfig() {
        try {
            FuzzySearchConfig defaultConfig = new FuzzySearchConfig();
            updateFuzzySearchUI(defaultConfig);
            fuzzyPresetComboBox.setValue("Balanced");
            
            statusLabel.setText("Reset fuzzy search configuration to defaults");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            showError("Failed to reset fuzzy search configuration: " + e.getMessage());
        }
    }
    
    private VBox createPathTemplateSection() {
        VBox pathTemplateSection = new VBox(10);
        
        // Section header
        Label pathTemplateLabel = createBoldLabel("File Organization Templates:");
        Label pathTemplateDescription = new Label("Configure how music files are organized when copied to a new location:");
        pathTemplateDescription.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        // Template selection
        VBox templateSelectionBox = new VBox(5);
        Label templateSelectorLabel = new Label("Template:");
        templateSelectorLabel.setStyle("-fx-font-weight: bold;");
        
        VBox customTemplateBox = new VBox(5);
        customTemplateBox.setVisible(false); // Initially hidden
        Label customTemplateLabel = new Label("Custom Template:");
        customTemplateLabel.setStyle("-fx-font-weight: bold;");
        customTemplateBox.getChildren().addAll(customTemplateLabel, customTemplateField);
        
        templateSelectionBox.getChildren().addAll(templateSelectorLabel, templateComboBox, customTemplateBox);
        
        // Formatting options
        GridPane formatGrid = new GridPane();
        formatGrid.setHgap(15);
        formatGrid.setVgap(5);
        
        formatGrid.add(new Label("Text Format:"), 0, 0);
        formatGrid.add(textFormatComboBox, 1, 0);
        
        formatGrid.add(useSubdirectoriesCheckBox, 0, 1);
        formatGrid.add(new HBox(5, new Label("Subdirectory Groups:"), subdirectoryLevelsSpinner), 1, 1);
        
        // Template preview
        VBox previewBox = new VBox(5);
        Label previewLabel = createBoldLabel("Preview:");
        previewBox.getChildren().addAll(previewLabel, templatePreviewLabel);
        
        // Available fields reference
        VBox fieldsBox = new VBox(5);
        Label fieldsLabel = createBoldLabel("Available Template Fields:");
        String fieldsList = "Standard fields:\n" +
                          "  {artist}, {album}, {title}, {genre}, {year}, {track_number}, {bit_rate}, {sample_rate}, {file_type}\n\n" +
                          "Special fields:\n" +
                          "  {subdirectory} - creates alphabetical folders based on artist distribution\n\n" +
                          "Number formatting:\n" +
                          "  {track_number:02d} - zero-padded track numbers (01, 02, 03...)\n\n" +
                          "Subdirectory groups distribute artists across folders (e.g., A-F/, G-M/, N-Z/ with 3 groups)";
        TextArea fieldsArea = new TextArea(fieldsList);
        fieldsArea.setEditable(false);
        fieldsArea.setPrefRowCount(7);
        fieldsArea.setWrapText(true);
        fieldsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-background-color: #f8f8f8;");
        
        fieldsBox.getChildren().addAll(fieldsLabel, fieldsArea);
        
        // Control buttons
        HBox templateButtonBox = new HBox(10);
        templateButtonBox.getChildren().addAll(applyTemplateButton, resetTemplateButton);
        
        pathTemplateSection.getChildren().addAll(
            pathTemplateLabel,
            pathTemplateDescription,
            templateSelectionBox,
            new Separator(),
            createBoldLabel("Format Options:"),
            formatGrid,
            new Separator(),
            previewBox,
            new Separator(),
            fieldsBox,
            templateButtonBox
        );
        
        return pathTemplateSection;
    }
    
    private void updateTemplateFromCombo() {
        String selected = templateComboBox.getValue();
        VBox customTemplateBox = (VBox) ((VBox) templateComboBox.getParent()).getChildren().get(2);
        
        if ("Custom Template".equals(selected)) {
            customTemplateBox.setVisible(true);
            customTemplateField.setText("");
        } else {
            customTemplateBox.setVisible(false);
            // Get the selected predefined template
            PathTemplateManager manager = PathTemplateManager.getInstance();
            String[] descriptions = manager.getPredefinedTemplateDescriptions();
            for (int i = 0; i < descriptions.length; i++) {
                if (descriptions[i].equals(selected)) {
                    PathTemplate template = manager.getPredefinedTemplate(i);
                    customTemplateField.setText(template.getTemplate());
                    textFormatComboBox.setValue(template.getTextFormat());
                    useSubdirectoriesCheckBox.setSelected(template.isUseSubdirectoryGrouping());
                    subdirectoryLevelsSpinner.getValueFactory().setValue(template.getSubdirectoryLevels());
                    break;
                }
            }
        }
        updateTemplatePreview();
    }
    
    private void updateTemplatePreview() {
        try {
            String templateStr = customTemplateField.getText();
            if (templateStr == null || templateStr.trim().isEmpty()) {
                templatePreviewLabel.setText("Enter a template to see preview");
                return;
            }
            
            PathTemplate template = new PathTemplate(
                templateStr,
                textFormatComboBox.getValue(),
                useSubdirectoriesCheckBox.isSelected(),
                subdirectoryLevelsSpinner.getValue()
            );
            
            String preview = PathTemplateManager.getInstance().getTemplatePreview(template);
            templatePreviewLabel.setText("Preview: " + preview);
            
        } catch (Exception e) {
            templatePreviewLabel.setText("Error in template: " + e.getMessage());
        }
    }
    
    private void loadCurrentPathTemplate() {
        try {
            PathTemplateManager manager = PathTemplateManager.getInstance();
            PathTemplate current = manager.getCurrentTemplate();
            
            customTemplateField.setText(current.getTemplate());
            textFormatComboBox.setValue(current.getTextFormat());
            useSubdirectoriesCheckBox.setSelected(current.isUseSubdirectoryGrouping());
            subdirectoryLevelsSpinner.getValueFactory().setValue(current.getSubdirectoryLevels());
            
            // Determine if it matches a predefined template
            String[] descriptions = manager.getPredefinedTemplateDescriptions();
            boolean foundMatch = false;
            for (int i = 0; i < descriptions.length; i++) {
                PathTemplate predefined = manager.getPredefinedTemplate(i);
                if (templatesMatch(current, predefined)) {
                    templateComboBox.setValue(descriptions[i]);
                    foundMatch = true;
                    break;
                }
            }
            
            if (!foundMatch) {
                templateComboBox.setValue("Custom Template");
                VBox customTemplateBox = (VBox) ((VBox) templateComboBox.getParent()).getChildren().get(2);
                customTemplateBox.setVisible(true);
            }
            
        } catch (Exception e) {
            showError("Failed to load current path template: " + e.getMessage());
        }
    }
    
    private boolean templatesMatch(PathTemplate t1, PathTemplate t2) {
        return t1.getTemplate().equals(t2.getTemplate()) &&
               t1.getTextFormat() == t2.getTextFormat() &&
               t1.isUseSubdirectoryGrouping() == t2.isUseSubdirectoryGrouping() &&
               t1.getSubdirectoryLevels() == t2.getSubdirectoryLevels();
    }
    
    private void applyPathTemplate() {
        try {
            String templateStr = customTemplateField.getText();
            if (templateStr == null || templateStr.trim().isEmpty()) {
                showError("Please enter a template");
                return;
            }
            
            PathTemplate template = new PathTemplate(
                templateStr,
                textFormatComboBox.getValue(),
                useSubdirectoriesCheckBox.isSelected(),
                subdirectoryLevelsSpinner.getValue()
            );
            
            PathTemplateManager.getInstance().setCurrentTemplate(template);
            
            statusLabel.setText("Path template applied successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            showError("Failed to apply path template: " + e.getMessage());
        }
    }
    
    private void resetPathTemplate() {
        try {
            PathTemplateManager.getInstance().resetToDefault();
            loadCurrentPathTemplate();
            updateTemplatePreview();
            
            statusLabel.setText("Path template reset to default");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            showError("Failed to reset path template: " + e.getMessage());
        }
    }
}