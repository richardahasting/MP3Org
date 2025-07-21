package org.hasting.ui.panels;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.hasting.model.PathTemplate;
import org.hasting.util.HelpSystem;
import org.hasting.util.PathTemplateManager;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

/**
 * UI panel for configuring path templates for file organization.
 * Provides controls for selecting predefined templates or creating custom ones.
 */
public class PathTemplateConfigPanel extends VBox {
    
    private static final Logger logger = Log4Rich.getLogger(PathTemplateConfigPanel.class);
    
    // Template selection
    private ComboBox<String> templateComboBox;
    private TextField customTemplateField;
    private VBox customTemplateBox;
    
    // Format options
    private ComboBox<PathTemplate.TextFormat> textFormatComboBox;
    private CheckBox useSubdirectoriesCheckBox;
    private Spinner<Integer> subdirectoryLevelsSpinner;
    
    // Preview and reference
    private Label templatePreviewLabel;
    private TextArea fieldsReferenceArea;
    
    // Control buttons
    private Button applyTemplateButton;
    private Button resetTemplateButton;
    
    // Status
    private Label statusLabel;
    
    // Callback for notifying parent of changes
    private Runnable onTemplateChanged;
    
    /**
     * Creates a new PathTemplateConfigPanel with all necessary components.
     * 
     * @param statusLabel The shared status label for displaying operation results
     */
    public PathTemplateConfigPanel(Label statusLabel) {
        this.statusLabel = statusLabel;
        initializeComponents();
        layoutComponents();
        loadCurrentSettings();
    }
    
    /**
     * Sets a callback to be notified when template changes occur.
     * 
     * @param onTemplateChanged Callback to execute when template changes
     */
    public void setOnTemplateChanged(Runnable onTemplateChanged) {
        this.onTemplateChanged = onTemplateChanged;
    }
    
    /**
     * Initializes all UI components for the path template configuration panel.
     */
    private void initializeComponents() {
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
        customTemplateField.setPromptText(
            "Enter custom template like: {artist}/{album}/{track_number:02d}-{title}.{file_type}"
        );
        customTemplateField.setPrefWidth(500);
        customTemplateField.textProperty().addListener((obs, oldVal, newVal) -> updateTemplatePreview());
        HelpSystem.setTooltip(customTemplateField, "config.template.custom");
        
        // Custom template container (initially hidden)
        customTemplateBox = new VBox(5);
        customTemplateBox.setVisible(false);
        Label customTemplateLabel = new Label("Custom Template:");
        customTemplateLabel.setStyle("-fx-font-weight: bold;");
        customTemplateBox.getChildren().addAll(customTemplateLabel, customTemplateField);
        
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
        
        // Fields reference
        createFieldsReference();
        
        // Control buttons
        applyTemplateButton = new Button("Apply Path Template");
        applyTemplateButton.setOnAction(e -> applyPathTemplate());
        HelpSystem.setTooltip(applyTemplateButton, "config.template.apply");
        
        resetTemplateButton = new Button("Reset to Default");
        resetTemplateButton.setOnAction(e -> resetPathTemplate());
        HelpSystem.setTooltip(resetTemplateButton, "config.template.reset");
    }
    
    /**
     * Creates the fields reference text area.
     */
    private void createFieldsReference() {
        String fieldsList = "Standard fields:\n" +
                          "  {artist}, {album}, {title}, {genre}, {year}, {track_number}, {bit_rate}, {sample_rate}, {file_type}\n\n" +
                          "Special fields:\n" +
                          "  {subdirectory} - creates alphabetical folders based on artist distribution\n\n" +
                          "Number formatting:\n" +
                          "  {track_number:02d} - zero-padded track numbers (01, 02, 03...)\n\n" +
                          "Subdirectory groups distribute artists across folders (e.g., A-F/, G-M/, N-Z/ with 3 groups)";
        
        fieldsReferenceArea = new TextArea(fieldsList);
        fieldsReferenceArea.setEditable(false);
        fieldsReferenceArea.setPrefRowCount(7);
        fieldsReferenceArea.setWrapText(true);
        fieldsReferenceArea.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-background-color: #f8f8f8;");
    }
    
    /**
     * Arranges the components in the panel layout.
     */
    private void layoutComponents() {
        setSpacing(10);
        
        // Section header
        Label pathTemplateLabel = createBoldLabel("File Organization Templates:");
        Label pathTemplateDescription = new Label("Configure how music files are organized when copied to a new location:");
        pathTemplateDescription.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        // Template selection section
        VBox templateSelectionBox = new VBox(5);
        Label templateSelectorLabel = new Label("Template:");
        templateSelectorLabel.setStyle("-fx-font-weight: bold;");
        templateSelectionBox.getChildren().addAll(templateSelectorLabel, templateComboBox, customTemplateBox);
        
        // Format options grid
        GridPane formatGrid = createFormatGrid();
        
        // Template preview section
        VBox previewBox = new VBox(5);
        Label previewLabel = createBoldLabel("Preview:");
        previewBox.getChildren().addAll(previewLabel, templatePreviewLabel);
        
        // Available fields reference section
        VBox fieldsBox = new VBox(5);
        Label fieldsLabel = createBoldLabel("Available Template Fields:");
        fieldsBox.getChildren().addAll(fieldsLabel, fieldsReferenceArea);
        
        // Control buttons
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(applyTemplateButton, resetTemplateButton);
        
        // Add all components
        getChildren().addAll(
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
            buttonBox
        );
    }
    
    /**
     * Creates the format options grid.
     * 
     * @return GridPane with format controls
     */
    private GridPane createFormatGrid() {
        GridPane formatGrid = new GridPane();
        formatGrid.setHgap(15);
        formatGrid.setVgap(5);
        
        formatGrid.add(new Label("Text Format:"), 0, 0);
        formatGrid.add(textFormatComboBox, 1, 0);
        
        formatGrid.add(useSubdirectoriesCheckBox, 0, 1);
        formatGrid.add(new HBox(5, new Label("Subdirectory Groups:"), subdirectoryLevelsSpinner), 1, 1);
        
        return formatGrid;
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
     * Loads and displays the current path template settings.
     */
    public void loadCurrentSettings() {
        loadCurrentPathTemplate();
        updateTemplatePreview();
    }
    
    /**
     * Loads the current path template from the template manager.
     */
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
                    customTemplateBox.setVisible(false);
                    foundMatch = true;
                    break;
                }
            }
            
            if (!foundMatch) {
                templateComboBox.setValue("Custom Template");
                customTemplateBox.setVisible(true);
            }
            
        } catch (Exception e) {
            showError("Failed to load current path template: " + e.getMessage());
        }
    }
    
    /**
     * Updates the template fields when a combo box selection changes.
     */
    private void updateTemplateFromCombo() {
        String selected = templateComboBox.getValue();
        
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
    
    /**
     * Updates the template preview display.
     */
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
    
    /**
     * Applies the current template settings.
     */
    private void applyPathTemplate() {
        try {
            statusLabel.setText("Applying path template...");
            statusLabel.setStyle("-fx-text-fill: orange;");
            
            String templateStr = customTemplateField.getText();
            if (templateStr == null || templateStr.trim().isEmpty()) {
                showError("Template cannot be empty.");
                return;
            }
            
            PathTemplate template = new PathTemplate(
                templateStr,
                textFormatComboBox.getValue(),
                useSubdirectoriesCheckBox.isSelected(),
                subdirectoryLevelsSpinner.getValue()
            );
            
            // Validate template
            try {
                PathTemplateManager.getInstance().getTemplatePreview(template);
            } catch (Exception e) {
                showError("Invalid template: " + e.getMessage());
                return;
            }
            
            // Save template
            PathTemplateManager.getInstance().setCurrentTemplate(template);
            
            statusLabel.setText("Path template applied successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Notify parent of changes
            if (onTemplateChanged != null) {
                onTemplateChanged.run();
            }
            
            // Show success dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Path Template Updated");
            successAlert.setHeaderText("Success");
            successAlert.setContentText("Path template has been updated.\n" +
                "This template will be used for organizing files.");
            successAlert.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to apply path template: " + e.getMessage());
            logger.error(String.format("Error applying template configuration changes: {}", e.getMessage()), e);
        }
    }
    
    /**
     * Resets the template to default settings.
     */
    private void resetPathTemplate() {
        try {
            PathTemplate defaultTemplate = new PathTemplate();
            
            customTemplateField.setText(defaultTemplate.getTemplate());
            textFormatComboBox.setValue(defaultTemplate.getTextFormat());
            useSubdirectoriesCheckBox.setSelected(defaultTemplate.isUseSubdirectoryGrouping());
            subdirectoryLevelsSpinner.getValueFactory().setValue(defaultTemplate.getSubdirectoryLevels());
            
            // Set to default predefined template
            templateComboBox.setValue(PathTemplateManager.getInstance().getPredefinedTemplateDescriptions()[0]);
            customTemplateBox.setVisible(false);
            
            updateTemplatePreview();
            
            statusLabel.setText("Reset path template to default");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            showError("Failed to reset path template: " + e.getMessage());
        }
    }
    
    /**
     * Checks if two templates match in all important aspects.
     * 
     * @param template1 First template
     * @param template2 Second template
     * @return true if templates match
     */
    private boolean templatesMatch(PathTemplate template1, PathTemplate template2) {
        return template1.getTemplate().equals(template2.getTemplate()) &&
               template1.getTextFormat() == template2.getTextFormat() &&
               template1.isUseSubdirectoryGrouping() == template2.isUseSubdirectoryGrouping() &&
               template1.getSubdirectoryLevels() == template2.getSubdirectoryLevels();
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
        alert.setTitle("Path Template Configuration Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Gets the template combo box for external access if needed.
     * 
     * @return The ComboBox for template selection
     */
    public ComboBox<String> getTemplateComboBox() {
        return templateComboBox;
    }
    
    /**
     * Gets the current template string from the UI.
     * 
     * @return The current template string
     */
    public String getCurrentTemplateString() {
        return customTemplateField.getText();
    }
}