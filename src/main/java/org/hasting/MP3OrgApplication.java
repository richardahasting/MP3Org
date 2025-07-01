package org.hasting;

import javafx.application.Application;
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
import org.hasting.ui.ImportOrganizeView;
import org.hasting.ui.ConfigurationView;
import org.hasting.ui.LogViewerDialog;
import org.hasting.util.DatabaseManager;
import org.hasting.util.HelpSystem;
import org.hasting.util.logging.MP3OrgLoggingManager;
import org.hasting.util.logging.Logger;

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
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(MP3OrgApplication.class);

    /**
     * Starts the MP3Org application by initializing the user interface and database.
     * 
     * <p>This method performs the following initialization steps:
     * <ol>
     *   <li>Initializes the database manager and connections</li>
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
        // Initialize logging system
        MP3OrgLoggingManager.initialize();
        MP3OrgLoggingManager.logApplicationStartup("MP3Org", "1.0");
        
        logger.info("Initializing MP3Org application");
        
        // Initialize database
        logger.debug("Initializing database manager");
        DatabaseManager.initialize();
        logger.info("Database manager initialized successfully");
        
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
        metadataTab.setContent(new MetadataEditorView());
        HelpSystem.setTooltip(
            metadataTab.getGraphic() != null ? (Control)metadataTab.getGraphic() : new Label(), 
            "tab.metadata"
        );
        
        Tab importTab = new Tab("Import & Organize");
        importTab.setClosable(false);
        importTab.setContent(new ImportOrganizeView());
        HelpSystem.setTooltip(
            importTab.getGraphic() != null ? (Control)importTab.getGraphic() : new Label(), 
            "tab.import"
        );
        
        Tab configTab = new Tab("Config");
        configTab.setClosable(false);
        configTab.setContent(new ConfigurationView());
        HelpSystem.setTooltip(
            configTab.getGraphic() != null ? (Control)configTab.getGraphic() : new Label(), 
            "tab.config"
        );
        
        // Add tabs to tab pane
        tabPane.getTabs().addAll(duplicateTab, metadataTab, importTab, configTab);
        
        // Set duplicate manager as default selected tab
        tabPane.getSelectionModel().select(duplicateTab);
        
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
            logger.error("Failed to open log viewer dialog: {}", e.getMessage(), e);
            
            Alert errorDialog = new Alert(Alert.AlertType.ERROR);
            errorDialog.initOwner(owner);
            errorDialog.setTitle("Error");
            errorDialog.setHeaderText("Failed to Open Log Viewer");
            errorDialog.setContentText("An error occurred while opening the log viewer: " + e.getMessage());
            errorDialog.showAndWait();
        }
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
    @Override
    public void stop() {
        // Clean up database connection if needed
        logger.info("Shutting down application");
        DatabaseManager.shutdown();
        MP3OrgLoggingManager.shutdown();
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