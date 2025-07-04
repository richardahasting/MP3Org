package org.hasting.ui.panels;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.hasting.util.DatabaseManager;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.FuzzySearchConfig;
import org.hasting.util.HelpSystem;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

/**
 * UI panel for configuring fuzzy search settings for duplicate detection.
 * Provides comprehensive controls for similarity thresholds and matching options.
 */
public class FuzzySearchConfigPanel extends VBox {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(FuzzySearchConfigPanel.class);
    
    // Preset and main controls
    private ComboBox<String> fuzzyPresetComboBox;
    
    // Similarity sliders
    private Slider titleSimilaritySlider;
    private Slider artistSimilaritySlider;
    private Slider albumSimilaritySlider;
    
    // Duration controls
    private Spinner<Integer> durationToleranceSpinner;
    private Slider durationPercentSlider;
    
    // Option checkboxes
    private CheckBox ignoreCaseCheckBox;
    private CheckBox ignorePunctuationCheckBox;
    private CheckBox trackNumberMatchCheckBox;
    private CheckBox ignoreArtistPrefixesCheckBox;
    private CheckBox ignoreFeaturingCheckBox;
    private CheckBox ignoreAlbumEditionsCheckBox;
    
    // Minimum fields
    private Spinner<Integer> minFieldsSpinner;
    
    // Control buttons
    private Button applyFuzzyConfigButton;
    private Button resetFuzzyConfigButton;
    
    // Status
    private Label statusLabel;
    
    // Callback for notifying parent of changes
    private Runnable onConfigChanged;
    
    /**
     * Creates a new FuzzySearchConfigPanel with all necessary components.
     * 
     * @param statusLabel The shared status label for displaying operation results
     */
    public FuzzySearchConfigPanel(Label statusLabel) {
        this.statusLabel = statusLabel;
        initializeComponents();
        layoutComponents();
        loadCurrentSettings();
    }
    
    /**
     * Sets a callback to be notified when configuration changes occur.
     * 
     * @param onConfigChanged Callback to execute when configuration changes
     */
    public void setOnConfigChanged(Runnable onConfigChanged) {
        this.onConfigChanged = onConfigChanged;
    }
    
    /**
     * Initializes all UI components for the fuzzy search configuration panel.
     */
    private void initializeComponents() {
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
    }
    
    /**
     * Creates a slider for similarity thresholds with consistent settings.
     * 
     * @param name The name of the slider for identification
     * @return A configured Slider for similarity values
     */
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
    
    /**
     * Arranges the components in the panel layout.
     */
    private void layoutComponents() {
        setSpacing(10);
        
        // Header section
        HBox headerBox = new HBox(10);
        Label fuzzyLabel = new Label("Duplicate Detection Settings:");
        fuzzyLabel.setStyle("-fx-font-weight: bold;");
        Button helpButton = new Button("?");
        helpButton.setPrefSize(25, 25);
        helpButton.setOnAction(e -> HelpSystem.showHelpDialog("config.fuzzy.help", 
            "Fuzzy Search Configuration Help", getScene().getWindow()));
        headerBox.getChildren().addAll(fuzzyLabel, helpButton);
        
        Label description = new Label("Configure how similar songs are considered duplicates:");
        description.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        // Preset selection
        HBox presetBox = new HBox(10);
        Label presetLabel = new Label("Preset:");
        presetBox.getChildren().addAll(presetLabel, fuzzyPresetComboBox);
        
        // Similarity settings grid
        GridPane similarityGrid = createSimilarityGrid();
        
        // Options section
        VBox optionsBox = createOptionsBox();
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(applyFuzzyConfigButton, resetFuzzyConfigButton);
        
        // Add all components
        getChildren().addAll(
            headerBox,
            description,
            presetBox,
            new Separator(),
            createBoldLabel("Similarity Thresholds:"),
            similarityGrid,
            new Separator(),
            optionsBox,
            buttonBox
        );
    }
    
    /**
     * Creates the similarity settings grid.
     * 
     * @return GridPane with similarity controls
     */
    private GridPane createSimilarityGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #dddddd; -fx-border-radius: 5;");
        
        // Row 0: Title similarity
        grid.add(new Label("Title Similarity:"), 0, 0);
        grid.add(titleSimilaritySlider, 1, 0);
        Label titleValueLabel = new Label();
        titleValueLabel.textProperty().bind(titleSimilaritySlider.valueProperty().asString("%.0f%%"));
        grid.add(titleValueLabel, 2, 0);
        
        // Row 1: Artist similarity
        grid.add(new Label("Artist Similarity:"), 0, 1);
        grid.add(artistSimilaritySlider, 1, 1);
        Label artistValueLabel = new Label();
        artistValueLabel.textProperty().bind(artistSimilaritySlider.valueProperty().asString("%.0f%%"));
        grid.add(artistValueLabel, 2, 1);
        
        // Row 2: Album similarity
        grid.add(new Label("Album Similarity:"), 0, 2);
        grid.add(albumSimilaritySlider, 1, 2);
        Label albumValueLabel = new Label();
        albumValueLabel.textProperty().bind(albumSimilaritySlider.valueProperty().asString("%.0f%%"));
        grid.add(albumValueLabel, 2, 2);
        
        // Row 3: Duration tolerance
        grid.add(new Label("Duration Tolerance:"), 0, 3);
        HBox durationBox = new HBox(5);
        durationBox.getChildren().addAll(durationToleranceSpinner, new Label("seconds OR"));
        grid.add(durationBox, 1, 3);
        
        // Row 4: Duration percentage
        HBox percentBox = new HBox(5);
        Label percentValueLabel = new Label();
        percentValueLabel.textProperty().bind(durationPercentSlider.valueProperty().asString("±%.1f%%"));
        percentBox.getChildren().addAll(durationPercentSlider, percentValueLabel);
        grid.add(percentBox, 1, 4);
        
        // Row 5: Minimum fields
        grid.add(new Label("Min Fields Match:"), 0, 5);
        HBox minFieldsBox = new HBox(5);
        minFieldsBox.getChildren().addAll(minFieldsSpinner, new Label("out of 4"));
        grid.add(minFieldsBox, 1, 5);
        
        return grid;
    }
    
    /**
     * Creates the options checkbox section.
     * 
     * @return VBox with option checkboxes
     */
    private VBox createOptionsBox() {
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
        return optionsBox;
    }
    
    /**
     * Creates a bold label for section headers.
     * 
     * @param text The label text
     * @return A Label with bold styling
     */
    private Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }
    
    /**
     * Loads and displays the current fuzzy search settings.
     */
    public void loadCurrentSettings() {
        loadCurrentFuzzyConfig();
    }
    
    /**
     * Loads the current fuzzy search configuration from the active profile.
     */
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
    
    /**
     * Updates the UI components with values from a FuzzySearchConfig.
     * 
     * @param config The configuration to load into the UI
     */
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
    
    /**
     * Determines which preset matches the given configuration.
     * 
     * @param config The configuration to match
     * @return The preset name or "Custom" if no match
     */
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
    
    /**
     * Checks if two configurations match within tolerance.
     * 
     * @param config1 First configuration
     * @param config2 Second configuration
     * @return true if configurations match
     */
    private boolean configsMatch(FuzzySearchConfig config1, FuzzySearchConfig config2) {
        return Math.abs(config1.getTitleSimilarityThreshold() - config2.getTitleSimilarityThreshold()) < 0.1 &&
               Math.abs(config1.getArtistSimilarityThreshold() - config2.getArtistSimilarityThreshold()) < 0.1 &&
               Math.abs(config1.getAlbumSimilarityThreshold() - config2.getAlbumSimilarityThreshold()) < 0.1 &&
               config1.getDurationToleranceSeconds() == config2.getDurationToleranceSeconds() &&
               Math.abs(config1.getDurationTolerancePercent() - config2.getDurationTolerancePercent()) < 0.1 &&
               config1.isTrackNumberMustMatch() == config2.isTrackNumberMustMatch() &&
               config1.getMinimumFieldsToMatch() == config2.getMinimumFieldsToMatch();
    }
    
    /**
     * Loads a preset configuration into the UI.
     */
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
    
    /**
     * Applies the current UI settings to the active profile.
     */
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
            
            // Notify parent of changes
            if (onConfigChanged != null) {
                onConfigChanged.run();
            }
            
            // Show success dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Fuzzy Search Updated");
            successAlert.setHeaderText("Success");
            successAlert.setContentText("Fuzzy search configuration has been updated.\n" +
                "These settings will be used for duplicate detection.");
            successAlert.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to apply fuzzy search configuration: " + e.getMessage());
            logger.error("Error applying fuzzy search configuration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Resets the configuration to default values.
     */
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
    
    /**
     * Displays an error message to the user.
     * 
     * @param message The error message to display
     */
    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fuzzy Search Configuration Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Gets the preset combo box for external access if needed.
     * 
     * @return The ComboBox for preset selection
     */
    public ComboBox<String> getPresetComboBox() {
        return fuzzyPresetComboBox;
    }
}