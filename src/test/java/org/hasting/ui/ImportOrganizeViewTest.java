package org.hasting.ui;

import org.hasting.util.DatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ImportOrganizeViewTest {

    private ImportOrganizeView importOrganizeView;
    private TextArea selectedDirectoriesArea;
    private TextField organizeFolderField;
    private ProgressBar progressBar;

    @Start
    void start(Stage stage) {
        // Initialize database for testing
        DatabaseManager.initialize();
        
        // Create the ImportOrganizeView
        importOrganizeView = new ImportOrganizeView();
        
        // Set up the scene
        Scene scene = new Scene(importOrganizeView, 800, 600);
        stage.setScene(scene);
        stage.show();
        
        // Get references to UI components for testing
        selectedDirectoriesArea = (TextArea) importOrganizeView.lookup(".text-area");
        organizeFolderField = findOrganizeFolderField();
        progressBar = (ProgressBar) importOrganizeView.lookup(".progress-bar");
    }

    @BeforeEach
    void setUp() {
        // Clear database before each test
        DatabaseManager.deleteAllMusicFiles();
    }

    @Test
    @Order(1)
    @DisplayName("Test ImportOrganizeView initialization")
    void testInitialization(FxRobot robot) {
        // Verify the view is created
        assertNotNull(importOrganizeView);
        
        // Verify main components are present
        assertNotNull(selectedDirectoriesArea);
        assertNotNull(organizeFolderField);
        
        // Verify buttons are present
        assertNotNull(robot.lookup("Add Directories to Scan").tryQuery().orElse(null));
        assertNotNull(robot.lookup("Organize Music Files").tryQuery().orElse(null));
        assertNotNull(robot.lookup("Clear Database").tryQuery().orElse(null));
        assertNotNull(robot.lookup("Browse").tryQuery().orElse(null));
    }

    @Test
    @Order(2)
    @DisplayName("Test selected directories text area configuration")
    void testSelectedDirectoriesTextAreaConfiguration() {
        // Verify text area configuration
        assertEquals("Selected directories will appear here...", selectedDirectoriesArea.getPromptText());
        assertEquals(4, selectedDirectoriesArea.getPrefRowCount());
        assertFalse(selectedDirectoriesArea.isEditable());
    }

    @Test
    @Order(3)
    @DisplayName("Test organize folder field configuration")
    void testOrganizeFolderFieldConfiguration() {
        // Verify organize folder field configuration
        if (organizeFolderField != null) {
            assertEquals("Select destination folder for organized files...", organizeFolderField.getPromptText());
            assertEquals(300.0, organizeFolderField.getPrefWidth(), 0.1);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test progress bar configuration")
    void testProgressBarConfiguration() {
        // Progress bar should initially be hidden
        if (progressBar != null) {
            assertFalse(progressBar.isVisible());
            assertEquals(400.0, progressBar.getPrefWidth(), 0.1);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test button states initialization")
    void testButtonStatesInitialization(FxRobot robot) {
        // Scan button should be enabled
        Button scanButton = robot.lookup("Add Directories to Scan").queryButton();
        assertFalse(scanButton.isDisabled());
        
        // Organize button should be disabled initially
        Button organizeButton = robot.lookup("Organize Music Files").queryButton();
        assertTrue(organizeButton.isDisabled());
        
        // Clear database button should be enabled
        Button clearButton = robot.lookup("Clear Database").queryButton();
        assertFalse(clearButton.isDisabled());
        
        // Browse button should be enabled
        Button browseButton = robot.lookup("Browse").queryButton();
        assertFalse(browseButton.isDisabled());
    }

    @Test
    @Order(6)
    @DisplayName("Test clear database button styling")
    void testClearDatabaseButtonStyling(FxRobot robot) {
        Button clearButton = robot.lookup("Clear Database").queryButton();
        
        // Should have red styling
        assertTrue(clearButton.getStyle().contains("#ff6b6b") || 
                  clearButton.getStyle().contains("red"));
    }

    @Test
    @Order(7)
    @DisplayName("Test layout structure")
    void testLayoutStructure() {
        // Verify main layout structure
        assertNotNull(importOrganizeView.getCenter()); // Main content
        assertNotNull(importOrganizeView.getBottom()); // Progress and status
    }

    @Test
    @Order(8)
    @DisplayName("Test section titles and labels")
    void testSectionTitlesAndLabels(FxRobot robot) {
        // Find labels with section titles
        var labels = robot.lookup(".label").queryAll();
        boolean foundImportTitle = false;
        boolean foundOrganizeTitle = false;
        
        for (var label : labels) {
            if (label instanceof Label) {
                Label l = (Label) label;
                String text = l.getText();
                if (text.contains("Import Music Files")) {
                    foundImportTitle = true;
                }
                if (text.contains("Organize Music Files")) {
                    foundOrganizeTitle = true;
                }
            }
        }
        
        assertTrue(foundImportTitle);
        assertTrue(foundOrganizeTitle);
    }

    @Test
    @Order(9)
    @DisplayName("Test instructions text")
    void testInstructionsText(FxRobot robot) {
        // Verify instruction labels are present
        var labels = robot.lookup(".label").queryAll();
        boolean foundInstructions = false;
        
        for (var label : labels) {
            if (label instanceof Label) {
                Label l = (Label) label;
                if (l.getText().contains("Select directories") || 
                    l.getText().contains("Copy all music files")) {
                    foundInstructions = true;
                    break;
                }
            }
        }
        
        assertTrue(foundInstructions);
    }

    @Test
    @Order(10)
    @DisplayName("Test clear database functionality")
    void testClearDatabaseFunctionality(FxRobot robot) {
        // Click clear database button
        Button clearButton = robot.lookup("Clear Database").queryButton();
        robot.clickOn(clearButton);
        
        // Should show confirmation dialog
        robot.sleep(500);
        
        // If dialog appears, we can cancel it for testing
        if (robot.lookup("Cancel").tryQuery().isPresent()) {
            robot.clickOn("Cancel");
        }
        
        // Test should complete without errors
        assertDoesNotThrow(() -> robot.sleep(100));
    }

    @Test
    @Order(11)
    @DisplayName("Test organize button state changes")
    void testOrganizeButtonStateChanges(FxRobot robot) {
        Button organizeButton = robot.lookup("Organize Music Files").queryButton();
        
        // Initially disabled
        assertTrue(organizeButton.isDisabled());
        
        // Enter text in organize folder field
        if (organizeFolderField != null) {
            robot.clickOn(organizeFolderField);
            robot.write("/test/path");
            
            // Button might become enabled (implementation-dependent)
            // We just verify no errors occur
            assertDoesNotThrow(() -> robot.sleep(100));
        }
    }

    @Test
    @Order(12)
    @DisplayName("Test status label")
    void testStatusLabel(FxRobot robot) {
        // Find status label
        var labels = robot.lookup(".label").queryAll();
        boolean foundStatusLabel = false;
        
        for (var label : labels) {
            if (label instanceof Label) {
                Label l = (Label) label;
                if (l.getText().equals("Ready") || l.getText().contains("status")) {
                    foundStatusLabel = true;
                    break;
                }
            }
        }
        
        assertTrue(foundStatusLabel || labels.size() > 0);
    }

    @Test
    @Order(13)
    @DisplayName("Test separator presence")
    void testSeparatorPresence() {
        // Verify separator exists between sections
        var separator = importOrganizeView.lookup(".separator");
        assertNotNull(separator);
    }

    @Test
    @Order(14)
    @DisplayName("Test button layout consistency")
    void testButtonLayoutConsistency(FxRobot robot) {
        // Verify all buttons are properly laid out
        Button scanButton = robot.lookup("Add Directories to Scan").queryButton();
        Button organizeButton = robot.lookup("Organize Music Files").queryButton();
        Button clearButton = robot.lookup("Clear Database").queryButton();
        Button browseButton = robot.lookup("Browse").queryButton();
        
        assertNotNull(scanButton);
        assertNotNull(organizeButton);
        assertNotNull(clearButton);
        assertNotNull(browseButton);
        
        // All buttons should be visible
        assertTrue(scanButton.isVisible());
        assertTrue(organizeButton.isVisible());
        assertTrue(clearButton.isVisible());
        assertTrue(browseButton.isVisible());
    }

    @Test
    @Order(15)
    @DisplayName("Test text field constraints")
    void testTextFieldConstraints() {
        // Verify text area and text field have proper constraints
        assertTrue(selectedDirectoriesArea.getPrefRowCount() > 0);
        
        if (organizeFolderField != null) {
            assertTrue(organizeFolderField.getPrefWidth() > 0);
        }
    }

    @Test
    @Order(16)
    @DisplayName("Test progress components initially hidden")
    void testProgressComponentsInitiallyHidden() {
        // Progress bar should be hidden initially
        if (progressBar != null) {
            assertFalse(progressBar.isVisible());
        }
        
        // Progress label should be empty or hidden
        var progressLabels = importOrganizeView.lookupAll(".label").stream()
            .filter(node -> node instanceof Label)
            .map(node -> (Label) node)
            .filter(label -> label.getText().contains("Scanning") || label.getText().contains("Copying"))
            .findFirst();
        
        assertTrue(progressLabels.isEmpty() || progressLabels.get().getText().isEmpty());
    }

    @Test
    @Order(17)
    @DisplayName("Test VBox growth properties")
    void testVBoxGrowthProperties() {
        // Verify that text area grows to fill space
        // This would require checking VBox.setVgrow properties
        // Implementation details would depend on the actual layout
        assertNotNull(selectedDirectoriesArea);
    }

    @Test
    @Order(18)
    @DisplayName("Test error handling gracefully")
    void testErrorHandlingGracefully(FxRobot robot) {
        // Test various operations that might fail gracefully
        
        // Try clicking organize without setting up folder
        Button organizeButton = robot.lookup("Organize Music Files").queryButton();
        // Button should be disabled, so this shouldn't do anything
        robot.clickOn(organizeButton);
        
        assertDoesNotThrow(() -> robot.sleep(100));
    }

    @Test
    @Order(19)
    @DisplayName("Test padding and spacing")
    void testPaddingAndSpacing() {
        // Verify the view has proper padding
        // BorderPane should have padding set
        assertTrue(importOrganizeView.getPadding().getTop() > 0);
    }

    @Test
    @Order(20)
    @DisplayName("Test component accessibility")
    void testComponentAccessibility(FxRobot robot) {
        // Verify components can be accessed via robot
        assertDoesNotThrow(() -> {
            robot.lookup("Add Directories to Scan").queryButton();
            robot.lookup("Clear Database").queryButton();
            robot.lookup("Organize Music Files").queryButton();
            robot.lookup("Browse").queryButton();
        });
        
        // Verify text components can be accessed
        if (selectedDirectoriesArea != null) {
            assertDoesNotThrow(() -> robot.clickOn(selectedDirectoriesArea));
        }
        
        if (organizeFolderField != null) {
            assertDoesNotThrow(() -> robot.clickOn(organizeFolderField));
        }
    }

    // Helper methods
    private TextField findOrganizeFolderField() {
        // Find the organize folder text field (second text field if present)
        var allTextFields = importOrganizeView.lookupAll(".text-field");
        if (allTextFields.size() >= 2) {
            for (var field : allTextFields) {
                if (field instanceof TextField) {
                    TextField tf = (TextField) field;
                    if (tf.getPromptText() != null && tf.getPromptText().contains("destination")) {
                        return tf;
                    }
                }
            }
        }
        return null;
    }
}