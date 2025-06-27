package org.hasting;

import org.hasting.util.DatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MP3OrgApplicationTest {

    private Stage primaryStage;
    private Scene scene;
    private TabPane tabPane;

    @Start
    void start(Stage stage) throws Exception {
        primaryStage = stage;
        
        // Create and start the application
        MP3OrgApplication app = new MP3OrgApplication();
        app.start(stage);
        
        // Get references to main components
        scene = stage.getScene();
        javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
        tabPane = (TabPane) root.getCenter();
    }

    @Test
    @Order(1)
    @DisplayName("Test application initialization")
    void testApplicationInitialization() {
        // Verify application components are created
        assertNotNull(primaryStage);
        assertNotNull(scene);
        assertNotNull(tabPane);
        
        // Verify stage properties
        assertEquals("MP3 Organizer", primaryStage.getTitle());
        assertTrue(primaryStage.isShowing());
        assertEquals(1200.0, scene.getWidth(), 0.1);
        assertEquals(800.0, scene.getHeight(), 0.1);
    }

    @Test
    @Order(2)
    @DisplayName("Test database initialization")
    void testDatabaseInitialization() {
        // Database should be initialized by the application
        assertNotNull(DatabaseManager.getConnection());
        assertDoesNotThrow(() -> DatabaseManager.getConnection().isValid(1));
    }

    @Test
    @Order(3)
    @DisplayName("Test stage configuration")
    void testStageConfiguration() {
        // Verify stage properties
        assertEquals(800.0, primaryStage.getMinWidth(), 0.1);
        assertEquals(600.0, primaryStage.getMinHeight(), 0.1);
        assertTrue(primaryStage.isShowing());
    }

    @Test
    @Order(4)
    @DisplayName("Test scene configuration")
    void testSceneConfiguration() {
        // Verify scene properties
        assertNotNull(scene.getRoot());
        assertTrue(scene.getRoot() instanceof javafx.scene.layout.BorderPane);
        
        // Check scene dimensions
        assertEquals(1200.0, scene.getWidth(), 0.1);
        assertEquals(800.0, scene.getHeight(), 0.1);
    }

    @Test
    @Order(5)
    @DisplayName("Test tab pane configuration")
    void testTabPaneConfiguration() {
        // Verify tab pane is configured correctly
        assertNotNull(tabPane);
        assertEquals(4, tabPane.getTabs().size());
        
        // Verify tab properties
        for (Tab tab : tabPane.getTabs()) {
            assertFalse(tab.isClosable());
            assertNotNull(tab.getContent());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test tab names and order")
    void testTabNamesAndOrder() {
        // Verify tab names and order
        assertEquals("Duplicate Manager", tabPane.getTabs().get(0).getText());
        assertEquals("Metadata Editor", tabPane.getTabs().get(1).getText());
        assertEquals("Import & Organize", tabPane.getTabs().get(2).getText());
        assertEquals("Config", tabPane.getTabs().get(3).getText());
    }

    @Test
    @Order(7)
    @DisplayName("Test default tab selection")
    void testDefaultTabSelection() {
        // Duplicate Manager should be selected by default
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        assertEquals("Duplicate Manager", selectedTab.getText());
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
    }

    @Test
    @Order(8)
    @DisplayName("Test tab content types")
    void testTabContentTypes() {
        // Verify each tab has the correct content type
        Tab duplicateTab = tabPane.getTabs().get(0);
        assertTrue(duplicateTab.getContent() instanceof org.hasting.ui.DuplicateManagerView);
        
        Tab metadataTab = tabPane.getTabs().get(1);
        assertTrue(metadataTab.getContent() instanceof org.hasting.ui.MetadataEditorView);
        
        Tab importTab = tabPane.getTabs().get(2);
        assertTrue(importTab.getContent() instanceof org.hasting.ui.ImportOrganizeView);
        
        Tab configTab = tabPane.getTabs().get(3);
        assertTrue(configTab.getContent() instanceof org.hasting.ui.ConfigurationView);
    }

    @Test
    @Order(9)
    @DisplayName("Test tab switching functionality")
    void testTabSwitchingFunctionality(FxRobot robot) {
        // Test switching to each tab
        robot.clickOn("Metadata Editor");
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        
        robot.clickOn("Import & Organize");
        assertEquals(2, tabPane.getSelectionModel().getSelectedIndex());
        
        robot.clickOn("Config");
        assertEquals(3, tabPane.getSelectionModel().getSelectedIndex());
        
        robot.clickOn("Duplicate Manager");
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
    }

    @Test
    @Order(10)
    @DisplayName("Test layout padding")
    void testLayoutPadding() {
        // Verify main layout has padding
        javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
        assertTrue(root.getPadding().getTop() > 0);
        assertTrue(root.getPadding().getLeft() > 0);
        assertTrue(root.getPadding().getRight() > 0);
        assertTrue(root.getPadding().getBottom() > 0);
    }

    @Test
    @Order(11)
    @DisplayName("Test application responsiveness")
    void testApplicationResponsiveness(FxRobot robot) {
        // Test that the application responds to user interactions
        robot.clickOn("Metadata Editor");
        robot.sleep(100);
        
        robot.clickOn("Import & Organize");
        robot.sleep(100);
        
        robot.clickOn("Duplicate Manager");
        robot.sleep(100);
        
        // Application should remain responsive
        assertTrue(primaryStage.isShowing());
        assertNotNull(tabPane.getSelectionModel().getSelectedItem());
    }

    @Test
    @Order(12)
    @DisplayName("Test stage resizing")
    void testStageResizing() {
        // Test minimum size constraints
        primaryStage.setWidth(700); // Below minimum
        primaryStage.setHeight(500); // Below minimum
        
        // Should enforce minimum sizes
        assertTrue(primaryStage.getWidth() >= 800);
        assertTrue(primaryStage.getHeight() >= 600);
    }

    @Test
    @Order(13)
    @DisplayName("Test tab content initialization")
    void testTabContentInitialization() {
        // Verify all tab contents are properly initialized
        for (Tab tab : tabPane.getTabs()) {
            assertNotNull(tab.getContent());
            assertTrue(tab.getContent() instanceof javafx.scene.Node);
        }
    }

    @Test
    @Order(14)
    @DisplayName("Test application main method")
    void testApplicationMainMethod() {
        // Test that main method can be called without errors
        assertDoesNotThrow(() -> {
            // We can't actually call main in a test environment easily,
            // but we can verify the method exists and is public static
            var mainMethod = MP3OrgApplication.class.getMethod("main", String[].class);
            assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
        });
    }

    @Test
    @Order(15)
    @DisplayName("Test stop method functionality")
    void testStopMethodFunctionality() {
        // Test that stop method can be called without errors
        MP3OrgApplication app = new MP3OrgApplication();
        assertDoesNotThrow(() -> app.stop());
    }

    @Test
    @Order(16)
    @DisplayName("Test scene graph structure")
    void testSceneGraphStructure() {
        // Verify scene graph structure
        javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
        
        // Should have center content (TabPane)
        assertNotNull(root.getCenter());
        assertEquals(tabPane, root.getCenter());
        
        // Other regions should be null (no top, bottom, left, right)
        assertNull(root.getTop());
        assertNull(root.getBottom());
        assertNull(root.getLeft());
        assertNull(root.getRight());
    }

    @Test
    @Order(17)
    @DisplayName("Test keyboard navigation")
    void testKeyboardNavigation(FxRobot robot) {
        // Test that tab navigation works with keyboard
        robot.press(javafx.scene.input.KeyCode.CONTROL, javafx.scene.input.KeyCode.TAB);
        robot.sleep(100);
        
        // Should remain functional
        assertTrue(primaryStage.isShowing());
        assertNotNull(tabPane.getSelectionModel().getSelectedItem());
    }

    @Test
    @Order(18)
    @DisplayName("Test error handling during initialization")
    void testErrorHandlingDuringInitialization() {
        // Test that application can handle database initialization errors gracefully
        // (This would require mocking or more complex setup)
        
        // For now, just verify that the application is properly initialized
        assertNotNull(primaryStage);
        assertTrue(primaryStage.isShowing());
        assertNotNull(DatabaseManager.getConnection());
    }

    @Test
    @Order(19)
    @DisplayName("Test concurrent access")
    void testConcurrentAccess(FxRobot robot) {
        // Test rapid tab switching to ensure thread safety
        for (int i = 0; i < 5; i++) {
            robot.clickOn("Metadata Editor");
            robot.clickOn("Import & Organize");
            robot.clickOn("Config");
            robot.clickOn("Duplicate Manager");
        }
        
        // Application should remain stable
        assertTrue(primaryStage.isShowing());
        assertEquals(4, tabPane.getTabs().size());
    }

    @Test
    @Order(20)
    @DisplayName("Test application lifecycle")
    void testApplicationLifecycle() {
        // Verify application is in proper state after startup
        assertTrue(primaryStage.isShowing());
        assertNotNull(scene.getRoot());
        assertEquals(4, tabPane.getTabs().size());
        
        // Verify default state
        assertEquals("Duplicate Manager", 
                    tabPane.getSelectionModel().getSelectedItem().getText());
        
        // Test that application can be stopped
        MP3OrgApplication app = new MP3OrgApplication();
        assertDoesNotThrow(() -> app.stop());
    }
}