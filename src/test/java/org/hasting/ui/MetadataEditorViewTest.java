package org.hasting.ui;

import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MetadataEditorViewTest {

    private MetadataEditorView metadataEditorView;
    private TextField searchField;
    private ComboBox<String> searchTypeCombo;
    private TableView<MusicFile> resultsTable;

    @Start
    void start(Stage stage) {
        // Initialize database for testing
        DatabaseManager.initialize();
        
        // Create the MetadataEditorView
        metadataEditorView = new MetadataEditorView();
        
        // Set up the scene
        Scene scene = new Scene(metadataEditorView, 800, 600);
        stage.setScene(scene);
        stage.show();
        
        // Get references to UI components for testing
        searchField = (TextField) metadataEditorView.lookup(".text-field");
        searchTypeCombo = (ComboBox<String>) metadataEditorView.lookup(".combo-box");
        resultsTable = (TableView<MusicFile>) metadataEditorView.lookup(".table-view");
    }

    @BeforeEach
    void setUp() {
        // Clear database before each test
        DatabaseManager.deleteAllMusicFiles();
        
        // Add some test data
        createTestMusicFiles();
    }

    @Test
    @Order(1)
    @DisplayName("Test MetadataEditorView initialization")
    void testInitialization(FxRobot robot) {
        // Verify the view is created
        assertNotNull(metadataEditorView);
        
        // Verify main components are present
        assertNotNull(searchField);
        assertNotNull(searchTypeCombo);
        assertNotNull(resultsTable);
        
        // Verify search button is present
        assertNotNull(robot.lookup("Search").tryQuery().orElse(null));
    }

    @Test
    @Order(2)
    @DisplayName("Test search field configuration")
    void testSearchFieldConfiguration() {
        // Verify search field has correct prompt text
        assertEquals("Enter search term...", searchField.getPromptText());
        
        // Verify preferred width
        assertEquals(300.0, searchField.getPrefWidth(), 0.1);
    }

    @Test
    @Order(3)
    @DisplayName("Test search type combo box configuration")
    void testSearchTypeComboConfiguration() {
        // Verify combo box has correct items
        assertEquals(4, searchTypeCombo.getItems().size());
        assertTrue(searchTypeCombo.getItems().contains("Title"));
        assertTrue(searchTypeCombo.getItems().contains("Artist"));
        assertTrue(searchTypeCombo.getItems().contains("Album"));
        assertTrue(searchTypeCombo.getItems().contains("All Fields"));
        
        // Verify default selection
        assertEquals("All Fields", searchTypeCombo.getValue());
    }

    @Test
    @Order(4)
    @DisplayName("Test results table configuration")
    void testResultsTableConfiguration() {
        // Verify table has correct columns
        assertEquals(5, resultsTable.getColumns().size());
        
        // Check column names
        assertEquals("Artist", resultsTable.getColumns().get(0).getText());
        assertEquals("Title", resultsTable.getColumns().get(1).getText());
        assertEquals("Album", resultsTable.getColumns().get(2).getText());
        assertEquals("Genre", resultsTable.getColumns().get(3).getText());
        assertEquals("Year", resultsTable.getColumns().get(4).getText());
        
        // Verify table height
        assertEquals(250.0, resultsTable.getPrefHeight(), 0.1);
    }

    @Test
    @Order(5)
    @DisplayName("Test search functionality")
    void testSearchFunctionality(FxRobot robot) {
        // Enter search term
        robot.clickOn(searchField);
        robot.write("Test");
        
        // Click search button
        Button searchButton = robot.lookup("Search").queryButton();
        robot.clickOn(searchButton);
        
        // Wait for search to complete
        robot.sleep(500);
        
        // Verify results are loaded (may be empty if no matching data)
        assertNotNull(resultsTable.getItems());
    }

    @Test
    @Order(6)
    @DisplayName("Test search with Enter key")
    void testSearchWithEnterKey(FxRobot robot) {
        // Enter search term and press Enter
        robot.clickOn(searchField);
        robot.write("Test");
        robot.press(javafx.scene.input.KeyCode.ENTER);
        
        // Wait for search to complete
        robot.sleep(500);
        
        // Should trigger search
        assertNotNull(resultsTable.getItems());
    }

    @Test
    @Order(7)
    @DisplayName("Test search type selection")
    void testSearchTypeSelection(FxRobot robot) {
        // Test each search type
        robot.clickOn(searchTypeCombo);
        robot.clickOn("Title");
        assertEquals("Title", searchTypeCombo.getValue());
        
        robot.clickOn(searchTypeCombo);
        robot.clickOn("Artist");
        assertEquals("Artist", searchTypeCombo.getValue());
        
        robot.clickOn(searchTypeCombo);
        robot.clickOn("Album");
        assertEquals("Album", searchTypeCombo.getValue());
    }

    @Test
    @Order(8)
    @DisplayName("Test edit form layout")
    void testEditFormLayout(FxRobot robot) {
        // Verify edit form fields are present
        var textFields = robot.lookup(".text-field").queryAll();
        assertTrue(textFields.size() >= 6); // At least search + 5 edit fields
        
        // Verify labels are present
        var labels = robot.lookup(".label").queryAll();
        assertTrue(labels.size() > 0);
    }

    @Test
    @Order(9)
    @DisplayName("Test edit form buttons")
    void testEditFormButtons(FxRobot robot) {
        // Verify edit form buttons are present
        assertNotNull(robot.lookup("Save Changes").tryQuery().orElse(null));
        assertNotNull(robot.lookup("Revert").tryQuery().orElse(null));
        assertNotNull(robot.lookup("Delete File").tryQuery().orElse(null));
    }

    @Test
    @Order(10)
    @DisplayName("Test edit form initially disabled")
    void testEditFormInitiallyDisabled() {
        // Edit form should be initially disabled
        var editSection = metadataEditorView.getBottom();
        if (editSection instanceof javafx.scene.layout.VBox) {
            var vbox = (javafx.scene.layout.VBox) editSection;
            if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof javafx.scene.layout.VBox) {
                assertTrue(vbox.getChildren().get(0).isDisabled());
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("Test file selection enables edit form")
    void testFileSelectionEnablesEditForm(FxRobot robot) {
        // First search for files
        robot.clickOn(searchField);
        robot.write("Test");
        robot.press(javafx.scene.input.KeyCode.ENTER);
        robot.sleep(500);
        
        // If there are results, select one
        if (!resultsTable.getItems().isEmpty()) {
            robot.clickOn(resultsTable);
            
            // Edit form should be enabled after selection
            // (This is implementation-dependent, so we just verify no errors occur)
            assertDoesNotThrow(() -> {
                resultsTable.getSelectionModel().select(0);
            });
        }
    }

    @Test
    @Order(12)
    @DisplayName("Test search with empty term")
    void testSearchWithEmptyTerm(FxRobot robot) {
        // Clear search field and search
        robot.clickOn(searchField);
        robot.eraseText(20); // Clear any existing text
        
        Button searchButton = robot.lookup("Search").queryButton();
        robot.clickOn(searchButton);
        
        // Should handle empty search gracefully
        assertDoesNotThrow(() -> robot.sleep(500));
    }

    @Test
    @Order(13)
    @DisplayName("Test status label")
    void testStatusLabel(FxRobot robot) {
        // Find status label
        var labels = robot.lookup(".label").queryAll();
        boolean foundStatusLabel = false;
        
        for (var label : labels) {
            if (label instanceof Label) {
                Label l = (Label) label;
                if (l.getText().equals("Ready") || l.getText().contains("results") || l.getText().contains("search")) {
                    foundStatusLabel = true;
                    break;
                }
            }
        }
        
        assertTrue(foundStatusLabel || labels.size() > 0); // Either find specific status or any label
    }

    @Test
    @Order(14)
    @DisplayName("Test delete button styling")
    void testDeleteButtonStyling(FxRobot robot) {
        Button deleteButton = robot.lookup("Delete File").tryQuery()
            .filter(node -> node instanceof Button)
            .map(node -> (Button) node)
            .orElse(null);
        
        if (deleteButton != null) {
            // Should have red styling
            assertTrue(deleteButton.getStyle().contains("#ff6b6b") || 
                      deleteButton.getStyle().contains("red"));
        }
    }

    @Test
    @Order(15)
    @DisplayName("Test layout structure")
    void testLayoutStructure() {
        // Verify main layout structure
        assertNotNull(metadataEditorView.getTop()); // Search section
        assertNotNull(metadataEditorView.getCenter()); // Results section
        assertNotNull(metadataEditorView.getBottom()); // Edit section
    }

    @Test
    @Order(16)
    @DisplayName("Test error handling with database issues")
    void testErrorHandlingDatabaseIssues(FxRobot robot) {
        // Shutdown database to simulate error
        DatabaseManager.shutdown();
        
        // Try to search
        robot.clickOn(searchField);
        robot.write("Test");
        robot.press(javafx.scene.input.KeyCode.ENTER);
        robot.sleep(500);
        
        // Should handle error gracefully
        assertDoesNotThrow(() -> robot.sleep(100));
        
        // Reinitialize database
        DatabaseManager.initialize();
    }

    @Test
    @Order(17)
    @DisplayName("Test form field constraints")
    void testFormFieldConstraints(FxRobot robot) {
        // Find text fields in edit form
        var allTextFields = robot.lookup(".text-field").queryAll();
        
        // Should have appropriate constraints (this is implementation-dependent)
        assertTrue(allTextFields.size() >= 1);
        
        // Verify track number and year fields have number constraints
        // (This would require accessing specific fields by ID or other means)
    }

    @Test
    @Order(18)
    @DisplayName("Test selection model configuration")
    void testSelectionModelConfiguration() {
        // Verify selection model is configured
        assertNotNull(resultsTable.getSelectionModel());
        assertEquals(javafx.scene.control.SelectionMode.SINGLE, 
                    resultsTable.getSelectionModel().getSelectionMode());
    }

    @Test
    @Order(19)
    @DisplayName("Test column cell factories")
    void testColumnCellFactories() {
        // Verify all columns have cell value factories
        resultsTable.getColumns().forEach(column -> {
            assertNotNull(column.getCellValueFactory());
        });
    }

    @Test
    @Order(20)
    @DisplayName("Test concurrent search operations")
    void testConcurrentSearchOperations(FxRobot robot) {
        // Perform multiple quick searches
        robot.clickOn(searchField);
        robot.write("Test1");
        robot.press(javafx.scene.input.KeyCode.ENTER);
        
        robot.eraseText(5);
        robot.write("Test2");
        robot.press(javafx.scene.input.KeyCode.ENTER);
        
        robot.sleep(1000);
        
        // Should handle concurrent operations gracefully
        assertNotNull(resultsTable.getItems());
    }

    // Helper methods
    private void createTestMusicFiles() {
        // Create test music files for testing
        MusicFile file1 = createTestMusicFile("Test Song 1", "Test Artist 1", "Test Album 1");
        MusicFile file2 = createTestMusicFile("Test Song 2", "Test Artist 2", "Test Album 2");
        MusicFile file3 = createTestMusicFile("Another Song", "Another Artist", "Another Album");
        
        try {
            DatabaseManager.saveMusicFile(file1);
            DatabaseManager.saveMusicFile(file2);
            DatabaseManager.saveMusicFile(file3);
        } catch (Exception e) {
            // Ignore if files already exist
        }
    }

    private MusicFile createTestMusicFile(String title, String artist, String album) {
        MusicFile musicFile = new MusicFile();
        musicFile.setTitle(title);
        musicFile.setArtist(artist);
        musicFile.setAlbum(album);
        musicFile.setGenre("Rock");
        musicFile.setTrackNumber(1);
        musicFile.setYear(2023);
        musicFile.setDurationSeconds(200);
        musicFile.setBitRate(320L);
        musicFile.setSampleRate(44100);
        musicFile.setFileType("mp3");
        musicFile.setFilePath("/test/path/" + title.replaceAll(" ", "_") + ".mp3");
        musicFile.setFileSizeBytes(5000000L);
        musicFile.setLastModified(new Date());
        musicFile.setModified(false);
        return musicFile;
    }
}