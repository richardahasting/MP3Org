package org.hasting;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.hasting.ui.DuplicateManagerView;
import org.hasting.ui.MetadataEditorView;
import org.hasting.ui.ImportView;
import org.hasting.ui.OrganizeView;
import org.hasting.ui.ConfigurationView;
import org.hasting.ui.LogViewerDialog;
import org.hasting.ui.TabSwitchCallback;
import org.hasting.util.DatabaseManager;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.DatabaseProfileManager;
import org.hasting.util.HelpSystem;
import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;

/**
 * Main application class for MP3Org - Music Collection Manager.
 * 
 * <p>MP3Org is a comprehensive music collection management application that provides:
 * <ul>
 *   <li>Duplicate song detection and removal</li>
 *   <li>Metadata editing and correction</li>
 *   <li>File organization and directory structuring</li>
 *   <li>Import and scanning of music collections</li>
 *   <li>Configuration management with database profiles</li>
 * </ul>
 * 
 * <p>The application uses a tabbed interface to organize functionality into distinct modules:
 * <ul>
 *   <li><strong>Duplicate Manager</strong> - Find and manage duplicate songs</li>
 *   <li><strong>Metadata Editor</strong> - Edit song information and metadata</li>
 *   <li><strong>Import & Organize</strong> - Scan directories and organize files</li>
 *   <li><strong>Configuration</strong> - Manage settings and database profiles</li>
 * </ul>
 * 
 * <p>Supported audio formats include: MP3, FLAC, M4A, WAV, OGG, WMA, AIFF, APE, OPUS
 * 
 * @author MP3Org Development Team
 * @version 1.0
 * @since 1.0
 */
public class MP3OrgApplication extends Application {
    
    private static final Logger logger = Log4Rich.getLogger(MP3OrgApplication.class);

    /**
     * Starts the MP3Org application by initializing the user interface and database.
     * 
     * <p>This method performs the following initialization steps:
     * <ol>
     *   <li>Initializes the database manager and connections with automatic fallback</li>
     *   <li>Creates the main window layout with menu bar and tab pane</li>
     *   <li>Sets up the four main functional tabs (Duplicate Manager, Metadata Editor, Import & Organize, Configuration)</li>
     *   <li>Configures keyboard shortcuts and tooltips</li>
     *   <li>Displays the main application window</li>
     * </ol>
     * 
     * @param primaryStage the primary stage for this application, onto which the application scene can be set
     * @throws RuntimeException if database initialization fails or UI components cannot be created
     */
    @Override
    public void start(Stage primaryStage) {
        // Initialize log4Rich - configuration is loaded automatically from log4Rich.config
        logger.info("Initializing MP3Org application with log4Rich");
        
        try {
            // Initialize database with comprehensive recovery mechanisms
            logger.debug("Initializing database manager with comprehensive recovery");
            initializeDatabaseWithAutomaticFallback();
            logger.info("Database manager initialized successfully");
        } catch (Exception e) {
            // This should never happen with our comprehensive recovery, but handle it gracefully
            logger.fatal(String.format("Unexpected failure after comprehensive database recovery: {}", e.getMessage()), e);
            handleStartupFailure(e, primaryStage);
            return;
        }
        
        // Create main layout
        BorderPane root = new BorderPane();
        
        // Create menu bar
        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);
        
        // Create tab pane for navigation
        TabPane tabPane = new TabPane();
        
        // Create tabs for main functions
        Tab duplicateTab = new Tab("Duplicate Manager");
        duplicateTab.setClosable(false);
        duplicateTab.setContent(new DuplicateManagerView());
        HelpSystem.setTooltip(
            duplicateTab.getGraphic() != null ? (Control)duplicateTab.getGraphic() : new Label(), 
            "tab.duplicates"
        );
        
        Tab metadataTab = new Tab("Metadata Editor");
        metadataTab.setClosable(false);
        metadataTab.setContent(new MetadataEditorView(createTabSwitchCallback(tabPane)));
        HelpSystem.setTooltip(
            metadataTab.getGraphic() != null ? (Control)metadataTab.getGraphic() : new Label(), 
            "tab.metadata"
        );
        
        Tab importTab = new Tab("Import");
        importTab.setClosable(false);
        importTab.setContent(new ImportView());
        HelpSystem.setTooltip(
            importTab.getGraphic() != null ? (Control)importTab.getGraphic() : new Label(), 
            "tab.import"
        );
        
        Tab organizeTab = new Tab("Organize");
        organizeTab.setClosable(false);
        organizeTab.setContent(new OrganizeView());
        HelpSystem.setTooltip(
            organizeTab.getGraphic() != null ? (Control)organizeTab.getGraphic() : new Label(), 
            "tab.organize"
        );
        
        Tab configTab = new Tab("Config");
        configTab.setClosable(false);
        configTab.setContent(new ConfigurationView());
        HelpSystem.setTooltip(
            configTab.getGraphic() != null ? (Control)configTab.getGraphic() : new Label(), 
            "tab.config"
        );
        
        // Add tabs to tab pane
        tabPane.getTabs().addAll(duplicateTab, metadataTab, importTab, organizeTab, configTab);
        
        // Set duplicate manager as default selected tab
        tabPane.getSelectionModel().select(duplicateTab);
        
        // Add tab selection listener to refresh content when switching tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                refreshTabContent(newTab);
            }
        });
        
        // Add tab pane to main layout
        root.setCenter(tabPane);
        root.setPadding(new Insets(10));
        
        // Create scene and configure stage
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("MP3 Organizer");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Set up keyboard shortcuts
        setupKeyboardShortcuts(scene, primaryStage);
        
        primaryStage.show();
    }
    
    /**
     * Creates and configures the main menu bar for the application.
     * 
     * <p>Currently provides a Help menu with the following items:
     * <ul>
     *   <li>Getting Started (Ctrl+H) - Opens the getting started guide</li>
     *   <li>MP3Org Help (F1) - Opens the general help system</li>
     *   <li>About MP3Org - Shows application information dialog</li>
     * </ul>
     * 
     * @param primaryStage the main application stage, used as parent for dialogs
     * @return a configured MenuBar with help menu and keyboard shortcuts
     */
    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        
        // Help menu
        Menu helpMenu = new Menu("Help");
        
        MenuItem gettingStartedItem = new MenuItem("Getting Started...");
        gettingStartedItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
        gettingStartedItem.setOnAction(e -> HelpSystem.showGettingStarted(primaryStage));
        
        MenuItem generalHelpItem = new MenuItem("MP3Org Help");
        generalHelpItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        generalHelpItem.setOnAction(e -> HelpSystem.showGeneralHelp(primaryStage));
        
        MenuItem logViewerItem = new MenuItem("View Logs...");
        logViewerItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        logViewerItem.setOnAction(e -> showLogViewer(primaryStage));
        
        MenuItem aboutItem = new MenuItem("About MP3Org");
        aboutItem.setOnAction(e -> showAboutDialog(primaryStage));
        
        helpMenu.getItems().addAll(gettingStartedItem, generalHelpItem, new SeparatorMenuItem(), logViewerItem, aboutItem);
        
        menuBar.getMenus().add(helpMenu);
        return menuBar;
    }
    
    /**
     * Refreshes the content of the specified tab when it becomes active.
     * This ensures that database-dependent content is always up-to-date.
     * 
     * @param tab the tab that was just selected
     */
    private void refreshTabContent(Tab tab) {
        try {
            String tabText = tab.getText();
            switch (tabText) {
                case "Duplicate Manager":
                    // Refresh duplicate manager content
                    if (tab.getContent() instanceof DuplicateManagerView) {
                        DuplicateManagerView duplicateView = (DuplicateManagerView) tab.getContent();
                        duplicateView.refreshContent();
                    }
                    break;
                case "Metadata Editor":
                    // Metadata editor typically doesn't need refreshing - it loads on demand
                    break;
                case "Import":
                    // Import view typically doesn't need refreshing - it's file-based
                    break;
                case "Organize":
                    // Organize view may need to refresh file list when switching to it
                    if (tab.getContent() instanceof OrganizeView) {
                        // OrganizeView will automatically refresh on tab switch
                    }
                    break;
                case "Config":
                    // Configuration view refreshes automatically when switching internal tabs
                    if (tab.getContent() instanceof ConfigurationView) {
                        ConfigurationView configView = (ConfigurationView) tab.getContent();
                        configView.updateDisplayedInfo();
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error(String.format("Error refreshing tab content for tab '%s': %s", tab.getText(), e.getMessage()), e);
        }
    }
    
    /**
     * Configures global keyboard shortcuts for the application.
     * 
     * <p>Sets up the following keyboard shortcuts:
     * <ul>
     *   <li><strong>F1</strong> - Opens general help</li>
     *   <li><strong>Ctrl+H</strong> - Opens getting started guide</li>
     *   <li><strong>Ctrl+Shift+L</strong> - Opens log viewer</li>
     * </ul>
     * 
     * @param scene the main application scene to attach shortcuts to
     * @param primaryStage the main stage, used as parent for help dialogs
     */
    private void setupKeyboardShortcuts(Scene scene, Stage primaryStage) {
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.F1),
            () -> HelpSystem.showGeneralHelp(primaryStage)
        );
        
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN),
            () -> HelpSystem.showGettingStarted(primaryStage)
        );
        
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
            () -> showLogViewer(primaryStage)
        );
    }
    
    /**
     * Displays the About dialog with application information.
     * 
     * <p>The dialog includes:
     * <ul>
     *   <li>Application version and description</li>
     *   <li>List of main features and capabilities</li>
     *   <li>Supported audio file formats</li>
     *   <li>Help system access information</li>
     * </ul>
     * 
     * @param owner the parent stage for the dialog (used for proper dialog positioning and modality)
     */
    private void showAboutDialog(Stage owner) {
        Alert aboutDialog = new Alert(Alert.AlertType.INFORMATION);
        aboutDialog.initOwner(owner);
        aboutDialog.setTitle("About MP3Org");
        aboutDialog.setHeaderText("MP3Org - Music Collection Manager");
        aboutDialog.setContentText(
            "Version: 1.0\n\n" +
            "MP3Org helps you organize and manage large music collections by:\n" +
            "• Finding and removing duplicate songs\n" +
            "• Organizing files into structured folders\n" +
            "• Editing song metadata for better organization\n" +
            "• Managing multiple music databases with profiles\n\n" +
            "Supported formats: MP3, FLAC, M4A, WAV, OGG, WMA, AIFF, APE, OPUS\n\n" +
            "For help, press F1 or use the Help menu."
        );
        aboutDialog.showAndWait();
    }
    
    /**
     * Shows the log viewer dialog for viewing application logs.
     * 
     * <p>The log viewer provides functionality for:
     * <ul>
     *   <li>Viewing log files with syntax highlighting</li>
     *   <li>Filtering logs by level (DEBUG, INFO, WARNING, ERROR, CRITICAL)</li>
     *   <li>Searching log content with regex support</li>
     *   <li>Real-time tail mode for active log monitoring</li>
     *   <li>Exporting filtered log content</li>
     * </ul>
     * 
     * @param owner the parent stage for the dialog
     */
    private void showLogViewer(Stage owner) {
        try {
            LogViewerDialog logViewer = new LogViewerDialog(owner);
            logViewer.show();
            logger.info("Log viewer dialog opened");
        } catch (Exception e) {
            logger.error(String.format("Failed to open log viewer dialog: {}", e.getMessage()), e);
            
            Alert errorDialog = new Alert(Alert.AlertType.ERROR);
            errorDialog.initOwner(owner);
            errorDialog.setTitle("Error");
            errorDialog.setHeaderText("Failed to Open Log Viewer");
            errorDialog.setContentText("An error occurred while opening the log viewer: " + e.getMessage());
            errorDialog.showAndWait();
        }
    }

    // ================================================================================================
    // DATABASE INITIALIZATION WITH FALLBACK
    // Following self-documenting code philosophy: method names teach the startup strategy
    // ================================================================================================
    
    /**
     * Initializes database connection with automatic fallback to alternative profiles
     * if the preferred database is locked. This ensures the application always starts
     * successfully, even when multiple instances are running.
     * 
     * <p>This method implements the core startup strategy that embodies the principle
     * of self-documenting code - the method name clearly communicates the automatic
     * fallback behavior, and the implementation teaches the pattern through clear
     * delegation to specialized methods.
     * 
     * <p><strong>Progressive Recovery Strategy:</strong>
     * <ol>
     *   <li><strong>Standard Initialization</strong>: Try existing fallback mechanisms</li>
     *   <li><strong>Emergency Mode</strong>: Create fresh database profile in user's home</li>
     *   <li><strong>Safe Mode</strong>: Minimal functionality with temporary database</li>
     *   <li><strong>Temporary Mode</strong>: In-memory database as final fallback</li>
     * </ol>
     * 
     * <p>This progressive strategy ensures the application never fails to start,
     * providing users with access to recovery tools and configuration options
     * even when primary database initialization fails.
     * 
     * @throws RuntimeException only if all recovery mechanisms fail (should never happen)
     */
    private void initializeDatabaseWithAutomaticFallback() {
        try {
            // Primary attempt: Standard initialization with existing fallback mechanisms
            initializeDatabaseStandard();
            
        } catch (Exception primaryError) {
            logger.warn(String.format("Standard database initialization failed, attempting recovery: %s", primaryError.getMessage()));
            
            try {
                // Secondary attempt: Emergency database creation
                initializeDatabaseEmergency();
                
            } catch (Exception emergencyError) {
                logger.warn(String.format("Emergency database creation failed, enabling safe mode: %s", emergencyError.getMessage()));
                
                try {
                    // Tertiary attempt: Safe mode with minimal database
                    initializeDatabaseSafeMode();
                    
                } catch (Exception safeModeError) {
                    logger.error(String.format("Safe mode initialization failed, using in-memory fallback: %s", safeModeError.getMessage()));
                    
                    // Final fallback: In-memory temporary database
                    initializeDatabaseTemporary();
                }
            }
        }
    }
    
    /**
     * Standard database initialization using existing fallback mechanisms.
     * 
     * @throws RuntimeException if standard initialization fails
     */
    private void initializeDatabaseStandard() {
        DatabaseProfileManager profileManager = DatabaseProfileManager.getInstance();
        String preferredProfileId = profileManager.getActiveProfileId();
        
        if (preferredProfileId == null) {
            throw new RuntimeException("No database profile configuration found");
        }
        
        DatabaseProfile resolvedProfile = DatabaseManager.initializeWithAutomaticFallback(preferredProfileId);
        
        logger.info(String.format("Database initialized with profile: {}", resolvedProfile.getName()));
        if (!resolvedProfile.getId().equals(preferredProfileId)) {
            logger.warn(String.format("Switched from preferred profile due to database lock - now using profile: {}", resolvedProfile.getName()));
        }
    }
    
    /**
     * Emergency database initialization by creating a fresh database profile.
     * 
     * <p>This method creates a new emergency profile with a clean database
     * when all existing profiles fail to initialize.
     * 
     * @throws RuntimeException if emergency database creation fails
     */
    private void initializeDatabaseEmergency() {
        logger.info("Creating emergency database profile for recovery");
        
        DatabaseProfileManager profileManager = DatabaseProfileManager.getInstance();
        
        // Create emergency profile with timestamp
        String emergencyName = "Emergency-" + System.currentTimeMillis();
        String emergencyPath = System.getProperty("user.home") + "/MP3Org-Emergency/" + emergencyName;
        
        // Ensure emergency directory exists
        java.io.File emergencyDir = new java.io.File(emergencyPath);
        if (!emergencyDir.mkdirs() && !emergencyDir.exists()) {
            throw new RuntimeException("Cannot create emergency database directory: " + emergencyPath);
        }
        
        // Create and activate emergency profile
        DatabaseProfile emergencyProfile = profileManager.createProfile(emergencyName, emergencyPath);
        profileManager.setActiveProfile(emergencyProfile.getId());
        
        // Initialize database with emergency profile
        DatabaseManager.initialize();
        
        logger.info(String.format("Emergency database profile created and initialized: {}", emergencyProfile.getName()));
        
        // Show user notification about emergency mode
        showEmergencyModeNotification(emergencyProfile);
    }
    
    /**
     * Safe mode database initialization with minimal functionality.
     * 
     * <p>Creates a temporary database with basic structure for essential
     * application functionality when normal database creation fails.
     * 
     * @throws RuntimeException if safe mode initialization fails
     */
    private void initializeDatabaseSafeMode() {
        logger.info("Initializing database in safe mode with minimal functionality");
        
        // Use system temp directory for safe mode database
        String tempDir = System.getProperty("java.io.tmpdir");
        String safeModeDir = tempDir + "/MP3Org-SafeMode-" + System.currentTimeMillis();
        
        java.io.File safeDir = new java.io.File(safeModeDir);
        if (!safeDir.mkdirs()) {
            throw new RuntimeException("Cannot create safe mode database directory: " + safeModeDir);
        }
        
        // Create minimal profile for safe mode
        DatabaseProfileManager profileManager = DatabaseProfileManager.getInstance();
        DatabaseProfile safeModeProfile = profileManager.createProfile("Safe Mode", safeModeDir);
        profileManager.setActiveProfile(safeModeProfile.getId());
        
        // Initialize minimal database
        DatabaseManager.initialize();
        
        logger.info(String.format("Safe mode database initialized at: {}", safeModeDir));
        
        // Show user notification about safe mode
        showSafeModeNotification(safeModeProfile);
    }
    
    /**
     * Temporary in-memory database initialization as final fallback.
     * 
     * <p>This method should never fail and provides absolute minimum
     * functionality to allow the application to start.
     */
    private void initializeDatabaseTemporary() {
        logger.info("Initializing temporary in-memory database as final fallback");
        
        try {
            // Create in-memory database profile
            DatabaseProfileManager profileManager = DatabaseProfileManager.getInstance();
            DatabaseProfile tempProfile = profileManager.createProfile("Temporary", ":memory:");
            profileManager.setActiveProfile(tempProfile.getId());
            
            // Initialize in-memory database
            DatabaseManager.initialize();
            
            logger.info("Temporary in-memory database initialized successfully");
            
            // Show user notification about temporary mode
            showTemporaryModeNotification();
            
        } catch (Exception e) {
            // This should never happen, but provide absolute final fallback
            logger.fatal(String.format("Even temporary database initialization failed - this should never happen: {}", e.getMessage()), e);
            throw new RuntimeException("Complete database initialization failure - application cannot start", e);
        }
    }
    
    /**
     * Shows user notification when emergency database mode is activated.
     */
    private void showEmergencyModeNotification(DatabaseProfile emergencyProfile) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Emergency Database Mode");
            alert.setHeaderText("MP3Org Started in Emergency Mode");
            alert.setContentText(
                "A database error occurred during startup. MP3Org has created an emergency database to allow the application to run.\n\n" +
                "Emergency Database: " + emergencyProfile.getName() + "\n" +
                "Location: " + emergencyProfile.getDatabasePath() + "\n\n" +
                "You can:\n" +
                "• Continue using this emergency database\n" +
                "• Switch to a different profile in Configuration > Database\n" +
                "• Import your music files again if needed\n\n" +
                "Your original database files have not been modified."
            );
            alert.showAndWait();
        });
    }
    
    /**
     * Shows user notification when safe mode is activated.
     */
    private void showSafeModeNotification(DatabaseProfile safeModeProfile) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Safe Mode");
            alert.setHeaderText("MP3Org Started in Safe Mode");
            alert.setContentText(
                "Multiple database errors occurred during startup. MP3Org is running in safe mode with limited functionality.\n\n" +
                "Safe Mode Database: " + safeModeProfile.getName() + "\n" +
                "Location: " + safeModeProfile.getDatabasePath() + "\n\n" +
                "In safe mode, you can:\n" +
                "• Access configuration settings\n" +
                "• Create new database profiles\n" +
                "• Repair or recover existing databases\n\n" +
                "Some features may be limited until a full database is available."
            );
            alert.showAndWait();
        });
    }
    
    /**
     * Shows user notification when temporary in-memory mode is activated.
     */
    private void showTemporaryModeNotification() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Temporary Mode");
            alert.setHeaderText("MP3Org Started in Temporary Mode");
            alert.setContentText(
                "Severe database errors prevented normal startup. MP3Org is running with a temporary in-memory database.\n\n" +
                "WARNING: All data will be lost when the application closes.\n\n" +
                "Recommendations:\n" +
                "• Check file system permissions\n" +
                "• Verify available disk space\n" +
                "• Create a new database profile\n" +
                "• Contact support if problems persist\n\n" +
                "Use Configuration > Database to set up a permanent database."
            );
            alert.showAndWait();
        });
    }
    
    /**
     * Handles startup failures by providing clear error messages to the user
     * and gracefully shutting down the application.
     * 
     * <p>This method provides user-friendly error handling for database initialization
     * problems. It shows informative error dialogs and ensures the application
     * shuts down cleanly even when startup fails.
     * 
     * @param e the exception that caused startup to fail
     * @param primaryStage the primary stage (may be used for error dialog positioning)
     */
    private void handleStartupFailure(Exception e, Stage primaryStage) {
        logger.fatal(String.format("Failed to start MP3Org application: {}", e.getMessage()), e);
        
        // Show user-friendly error dialog
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("MP3Org Startup Error");
        errorAlert.setHeaderText("Application Failed to Start");
        errorAlert.setContentText(
            "MP3Org could not initialize properly:\n\n" +
            e.getMessage() + "\n\n" +
            "Please check that:\n" +
            "• No other MP3Org instances are running\n" +
            "• Database files are not corrupted\n" +
            "• You have write permissions to the database directory"
        );
        
        try {
            errorAlert.showAndWait();
        } catch (Exception dialogException) {
            // If we can't even show the error dialog, just log and exit
            logger.fatal(String.format("Could not display error dialog: %s", dialogException.getMessage()), dialogException);
        }
        
        // Graceful shutdown
        System.exit(1);
    }
    
    /**
     * Performs cleanup operations when the application is shutting down.
     * 
     * <p>This method is automatically called by the JavaFX framework when the application
     * is closing. It ensures proper cleanup of resources including:
     * <ul>
     *   <li>Closing database connections</li>
     *   <li>Releasing any held resources</li>
     *   <li>Logging shutdown status</li>
     * </ul>
     * 
     * @throws RuntimeException if cleanup operations fail (though application will still terminate)
     */
    /**
     * Creates a callback for switching tabs in the main application.
     * 
     * <p>This callback allows child views to request tab switches without having
     * direct access to the main TabPane, maintaining proper separation of concerns.
     * 
     * @param tabPane the main application TabPane to control
     * @return a TabSwitchCallback that can switch to named tabs
     */
    private TabSwitchCallback createTabSwitchCallback(TabPane tabPane) {
        return (tabName) -> {
            for (int i = 0; i < tabPane.getTabs().size(); i++) {
                Tab tab = tabPane.getTabs().get(i);
                if (tab.getText().equals(tabName)) {
                    tabPane.getSelectionModel().select(i);
                    logger.info(String.format("Switched to tab: {}", tabName));
                    return true;
                }
            }
            logger.warn(String.format("Tab not found: {}", tabName));
            return false;
        };
    }

    @Override
    public void stop() {
        // Clean up database connection if needed
        logger.info("Shutting down application");
        DatabaseManager.shutdown();
        // MP3OrgLoggingManager.shutdown(); // Replaced by log4Rich
    }

    /**
     * Main entry point for the MP3Org application.
     * 
     * <p>Launches the JavaFX application by calling the inherited {@code launch} method.
     * This method will create an instance of {@code MP3OrgApplication} and call its
     * {@code start} method on the JavaFX Application Thread.
     * 
     * @param args command line arguments passed to the application (currently unused)
     */
    public static void main(String[] args) {
        launch(args);
    }
}