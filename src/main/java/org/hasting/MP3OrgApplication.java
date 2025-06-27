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
import org.hasting.util.DatabaseManager;
import org.hasting.util.HelpSystem;

public class MP3OrgApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize database
        DatabaseManager.initialize();
        
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
        HelpSystem.setTooltip(duplicateTab.getGraphic() != null ? (Control)duplicateTab.getGraphic() : new Label(), "tab.duplicates");
        
        Tab metadataTab = new Tab("Metadata Editor");
        metadataTab.setClosable(false);
        metadataTab.setContent(new MetadataEditorView());
        HelpSystem.setTooltip(metadataTab.getGraphic() != null ? (Control)metadataTab.getGraphic() : new Label(), "tab.metadata");
        
        Tab importTab = new Tab("Import & Organize");
        importTab.setClosable(false);
        importTab.setContent(new ImportOrganizeView());
        HelpSystem.setTooltip(importTab.getGraphic() != null ? (Control)importTab.getGraphic() : new Label(), "tab.import");
        
        Tab configTab = new Tab("Config");
        configTab.setClosable(false);
        configTab.setContent(new ConfigurationView());
        HelpSystem.setTooltip(configTab.getGraphic() != null ? (Control)configTab.getGraphic() : new Label(), "tab.config");
        
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
        
        MenuItem aboutItem = new MenuItem("About MP3Org");
        aboutItem.setOnAction(e -> showAboutDialog(primaryStage));
        
        helpMenu.getItems().addAll(gettingStartedItem, generalHelpItem, new SeparatorMenuItem(), aboutItem);
        
        menuBar.getMenus().add(helpMenu);
        return menuBar;
    }
    
    private void setupKeyboardShortcuts(Scene scene, Stage primaryStage) {
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.F1),
            () -> HelpSystem.showGeneralHelp(primaryStage)
        );
        
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN),
            () -> HelpSystem.showGettingStarted(primaryStage)
        );
    }
    
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

    @Override
    public void stop() {
        // Clean up database connection if needed
        DatabaseManager.shutdown();
        System.out.println("Application shutting down...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}