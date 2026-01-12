package org.hasting.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.hasting.ui.panels.*;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

/**
 * Main configuration view that orchestrates all configuration panels.
 * This view has been refactored to use specialized panels for better maintainability.
 */
public class ConfigurationView extends BorderPane {
    
    private static final Logger logger = Log4Rich.getLogger(ConfigurationView.class);
    
    // Configuration panels
    private DatabaseLocationPanel databaseLocationPanel;
    private FileTypeFilterPanel fileTypeFilterPanel;
    private ProfileManagementPanel profileManagementPanel;
    private FuzzySearchConfigPanel fuzzySearchConfigPanel;
    private PathTemplateConfigPanel pathTemplateConfigPanel;
    private LoggingConfigPanel loggingConfigPanel;
    
    // Shared status label
    private Label statusLabel;
    
    // Tab container for organizing panels
    private TabPane configTabPane;
    
    /**
     * Creates a new ConfigurationView with all configuration panels.
     */
    public ConfigurationView() {
        initializeComponents();
        layoutComponents();
        setupCallbacks();
        loadInitialSettings();
    }
    
    /**
     * Initializes all configuration panels and shared components.
     */
    private void initializeComponents() {
        // Shared status label
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: green;");
        
        // Initialize all configuration panels
        databaseLocationPanel = new DatabaseLocationPanel(statusLabel);
        fileTypeFilterPanel = new FileTypeFilterPanel(statusLabel);
        profileManagementPanel = new ProfileManagementPanel(statusLabel);
        fuzzySearchConfigPanel = new FuzzySearchConfigPanel(statusLabel);
        pathTemplateConfigPanel = new PathTemplateConfigPanel(statusLabel);
        loggingConfigPanel = new LoggingConfigPanel();
        
        // Create tab container for better organization
        configTabPane = new TabPane();
        configTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Add tab selection listener to refresh data when switching tabs
        configTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                refreshCurrentTab();
            }
        });
    }
    
    /**
     * Arranges all components in the main layout.
     */
    private void layoutComponents() {
        // Create tabs for each configuration area
        Tab profilesTab = createTab("Profiles", profileManagementPanel, 
            "Manage database profiles");
        Tab databaseTab = createTab("Database", databaseLocationPanel, 
            "Configure database location and settings");
        Tab fileTypesTab = createTab("File Types", fileTypeFilterPanel, 
            "Select supported audio file types");
        Tab organizationTab = createTab("File Organization", pathTemplateConfigPanel, 
            "Configure file organization templates");
        Tab duplicatesTab = createTab("Duplicate Detection", fuzzySearchConfigPanel, 
            "Configure fuzzy matching for duplicates");
        Tab loggingTab = createTab("Logging Configuration", loggingConfigPanel, 
            "Configure logging levels and output settings");
        
        // Add tabs to tab pane in optimized user workflow order:
        // Profiles → Database → File Types → File Organization → Duplicate Detection → Logging
        configTabPane.getTabs().addAll(
            profilesTab,
            databaseTab,
            fileTypesTab,
            organizationTab,
            duplicatesTab,
            loggingTab
        );
        
        // Create title section
        VBox titleSection = createTitleSection();
        
        // Create status section
        HBox statusSection = createStatusSection();
        
        // Main layout
        setTop(titleSection);
        setCenter(configTabPane);
        setBottom(statusSection);
        
        // Add padding to main content
        setPadding(new Insets(20));
    }
    
    /**
     * Creates a tab with the specified title, content, and tooltip.
     * 
     * @param title The tab title
     * @param content The tab content panel
     * @param tooltip The tooltip text for the tab
     * @return A configured Tab
     */
    private Tab createTab(String title, Region content, String tooltip) {
        Tab tab = new Tab(title);
        
        // Wrap content in scroll pane for better handling of large panels
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        tab.setContent(scrollPane);
        tab.setTooltip(new Tooltip(tooltip));
        
        return tab;
    }
    
    /**
     * Creates the title section with application name and version.
     * 
     * @return VBox containing title elements
     */
    private VBox createTitleSection() {
        VBox titleSection = new VBox(5);
        titleSection.setPadding(new Insets(0, 0, 20, 0));
        
        Label titleLabel = new Label("MP3Org Configuration");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label subtitleLabel = new Label("Configure application settings and preferences");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);
        return titleSection;
    }
    
    /**
     * Creates the status section with shared status label.
     * 
     * @return HBox containing status elements
     */
    private HBox createStatusSection() {
        HBox statusSection = new HBox(10);
        statusSection.setPadding(new Insets(20, 0, 0, 0));
        statusSection.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        
        Label statusLabelText = new Label("Status:");
        statusLabelText.setStyle("-fx-font-weight: bold;");
        
        statusSection.getChildren().addAll(statusLabelText, statusLabel);
        return statusSection;
    }
    
    /**
     * Sets up callbacks between panels to handle inter-panel dependencies.
     */
    private void setupCallbacks() {
        // When profile changes, refresh other panels
        profileManagementPanel.setOnProfileChanged(() -> {
            refreshAllPanels();
        });
        
        // When database location changes, refresh profile and file type panels
        // Note: DatabaseLocationPanel doesn't have callbacks yet, but could be added
        
        // When fuzzy config changes, update status
        fuzzySearchConfigPanel.setOnConfigChanged(() -> {
            statusLabel.setText("Fuzzy search configuration updated");
            statusLabel.setStyle("-fx-text-fill: green;");
        });
        
        // When path template changes, update status
        pathTemplateConfigPanel.setOnTemplateChanged(() -> {
            statusLabel.setText("Path template configuration updated");
            statusLabel.setStyle("-fx-text-fill: green;");
        });
    }
    
    /**
     * Loads initial settings for all panels.
     */
    private void loadInitialSettings() {
        try {
            // Load settings for all panels
            databaseLocationPanel.loadCurrentSettings();
            fileTypeFilterPanel.loadCurrentSettings();
            profileManagementPanel.loadCurrentSettings();
            fuzzySearchConfigPanel.loadCurrentSettings();
            pathTemplateConfigPanel.loadCurrentSettings();
            loggingConfigPanel.loadCurrentSettings();
            
            statusLabel.setText("Configuration loaded successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            statusLabel.setText("Error loading configuration: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            logger.error(String.format("Error loading configuration tab content: {}", e.getMessage()), e);
        }
    }
    
    /**
     * Refreshes all configuration panels with current data.
     */
    private void refreshAllPanels() {
        try {
            databaseLocationPanel.loadCurrentSettings();
            fileTypeFilterPanel.loadCurrentSettings();
            fuzzySearchConfigPanel.loadCurrentSettings();
            pathTemplateConfigPanel.loadCurrentSettings();
            profileManagementPanel.loadCurrentSettings();
            loggingConfigPanel.loadCurrentSettings();
            
            statusLabel.setText("All panels refreshed successfully");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (Exception e) {
            statusLabel.setText("Error refreshing panels: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            logger.error(String.format("Error loading configuration tab content: {}", e.getMessage()), e);
        }
    }
    
    /**
     * Refreshes the currently selected tab's content.
     */
    private void refreshCurrentTab() {
        Tab selectedTab = configTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return;
        
        try {
            String tabText = selectedTab.getText();
            switch (tabText) {
                case "Database":
                    databaseLocationPanel.loadCurrentSettings();
                    break;
                case "Profiles":
                    profileManagementPanel.loadCurrentSettings();
                    break;
                case "File Types":
                    fileTypeFilterPanel.loadCurrentSettings();
                    break;
                case "Duplicate Detection":
                    fuzzySearchConfigPanel.loadCurrentSettings();
                    break;
                case "File Organization":
                    pathTemplateConfigPanel.loadCurrentSettings();
                    break;
                case "Logging Configuration":
                    loggingConfigPanel.loadCurrentSettings();
                    break;
            }
        } catch (Exception e) {
            logger.error(String.format("Error refreshing current tab: {}", e.getMessage()), e);
        }
    }
    
    /**
     * Gets the current status label for external access.
     * 
     * @return The shared status label
     */
    public Label getStatusLabel() {
        return statusLabel;
    }
    
    /**
     * Gets the database location panel for external access if needed.
     * 
     * @return The DatabaseLocationPanel
     */
    public DatabaseLocationPanel getDatabaseLocationPanel() {
        return databaseLocationPanel;
    }
    
    /**
     * Gets the file type filter panel for external access if needed.
     * 
     * @return The FileTypeFilterPanel
     */
    public FileTypeFilterPanel getFileTypeFilterPanel() {
        return fileTypeFilterPanel;
    }
    
    /**
     * Gets the profile management panel for external access if needed.
     * 
     * @return The ProfileManagementPanel
     */
    public ProfileManagementPanel getProfileManagementPanel() {
        return profileManagementPanel;
    }
    
    /**
     * Gets the fuzzy search configuration panel for external access if needed.
     * 
     * @return The FuzzySearchConfigPanel
     */
    public FuzzySearchConfigPanel getFuzzySearchConfigPanel() {
        return fuzzySearchConfigPanel;
    }
    
    /**
     * Gets the path template configuration panel for external access if needed.
     * 
     * @return The PathTemplateConfigPanel
     */
    public PathTemplateConfigPanel getPathTemplateConfigPanel() {
        return pathTemplateConfigPanel;
    }
    
    /**
     * Gets the logging configuration panel for external access if needed.
     * 
     * @return The LoggingConfigPanel
     */
    public LoggingConfigPanel getLoggingConfigPanel() {
        return loggingConfigPanel;
    }
    
    /**
     * Switches to a specific configuration tab by name.
     * 
     * @param tabName The name of the tab to switch to
     */
    public void switchToTab(String tabName) {
        for (Tab tab : configTabPane.getTabs()) {
            if (tab.getText().equals(tabName)) {
                configTabPane.getSelectionModel().select(tab);
                break;
            }
        }
    }
    
    /**
     * Updates the displayed configuration information.
     * This method maintains compatibility with external code that might call it.
     */
    public void updateDisplayedInfo() {
        refreshAllPanels();
    }
}