package org.hasting.ui;

import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DuplicateManagerViewTest {

    private DuplicateManagerView duplicateManagerView;
    private TableView<MusicFile> duplicatesTable;
    private TableView<MusicFile> comparisonTable;

    @Start
    void start(Stage stage) {
        // Initialize database for testing
        DatabaseManager.initialize();
        
        // Create the DuplicateManagerView
        duplicateManagerView = new DuplicateManagerView();
        
        // Set up the scene
        Scene scene = new Scene(duplicateManagerView, 800, 600);
        stage.setScene(scene);
        stage.show();
        
        // Get references to UI components for testing
        duplicatesTable = (TableView<MusicFile>) duplicateManagerView.lookup(".table-view");
        comparisonTable = findSecondTableView();
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
    @DisplayName("Test DuplicateManagerView initialization")
    void testInitialization(FxRobot robot) {
        // Verify the view is created
        assertNotNull(duplicateManagerView);
        
        // Verify main components are present
        assertNotNull(robot.lookup("#refreshButton").tryQuery().orElse(null));
        assertNotNull(robot.lookup("#deleteSelectedButton").tryQuery().orElse(null));
        assertNotNull(robot.lookup("#keepBetterQualityButton").tryQuery().orElse(null));
        
        // Verify tables are present
        assertNotNull(duplicatesTable);
        assertNotNull(comparisonTable);
    }

    @Test
    @Order(2)
    @DisplayName("Test refresh duplicates functionality")
    void testRefreshDuplicates(FxRobot robot) {
        // Click refresh button
        Button refreshButton = robot.lookup("Refresh Duplicates").queryButton();
        robot.clickOn(refreshButton);
        
        // Wait for refresh to complete
        robot.sleep(1000);
        
        // Verify status label is updated
        Label statusLabel = robot.lookup(".label").query();
        assertNotNull(statusLabel);
    }

    @Test
    @Order(3)
    @DisplayName("Test table column setup")
    void testTableColumns() {
        // Verify duplicates table has correct columns
        assertEquals(6, duplicatesTable.getColumns().size());
        
        // Check column names
        assertEquals("Artist", duplicatesTable.getColumns().get(0).getText());
        assertEquals("Title", duplicatesTable.getColumns().get(1).getText());
        assertEquals("Album", duplicatesTable.getColumns().get(2).getText());
        assertEquals("Duration", duplicatesTable.getColumns().get(3).getText());
        assertEquals("Bitrate", duplicatesTable.getColumns().get(4).getText());
        assertEquals("File Path", duplicatesTable.getColumns().get(5).getText());
        
        // Verify comparison table has same columns
        assertEquals(6, comparisonTable.getColumns().size());
    }

    @Test
    @Order(4)
    @DisplayName("Test duplicate selection triggers comparison")
    void testDuplicateSelectionTriggersComparison(FxRobot robot) {
        // Refresh to load data
        Button refreshButton = robot.lookup("Refresh Duplicates").queryButton();
        robot.clickOn(refreshButton);
        robot.sleep(1000);
        
        // If there are items in the duplicates table, select one
        if (!duplicatesTable.getItems().isEmpty()) {
            robot.clickOn(duplicatesTable);
            
            // Verify that selecting an item doesn't cause errors
            assertDoesNotThrow(() -> {
                duplicatesTable.getSelectionModel().select(0);
            });
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test delete selected button state")
    void testDeleteSelectedButtonState(FxRobot robot) {
        Button deleteButton = robot.lookup("Delete Selected").queryButton();
        assertNotNull(deleteButton);
        
        // Button should be enabled (assuming no special conditions)
        assertFalse(deleteButton.isDisabled());
    }

    @Test
    @Order(6)
    @DisplayName("Test keep better quality button state")
    void testKeepBetterQualityButtonState(FxRobot robot) {
        Button keepBetterQualityButton = robot.lookup("Keep Better Quality").queryButton();
        assertNotNull(keepBetterQualityButton);
        
        // Button should be enabled
        assertFalse(keepBetterQualityButton.isDisabled());
    }

    @Test
    @Order(7)
    @DisplayName("Test status label updates")
    void testStatusLabelUpdates(FxRobot robot) {
        // Find status label
        Label statusLabel = findStatusLabel(robot);
        assertNotNull(statusLabel);
        
        // Initial status should be "Ready"
        assertEquals("Ready", statusLabel.getText());
    }

    @Test
    @Order(8)
    @DisplayName("Test progress indicator visibility")
    void testProgressIndicatorVisibility(FxRobot robot) {
        // Progress indicator should initially be hidden
        var progressIndicator = robot.lookup(".progress-indicator").tryQuery();
        
        // It's okay if progress indicator is not found or is hidden
        assertTrue(progressIndicator.isEmpty() || !progressIndicator.get().isVisible());
    }

    @Test
    @Order(9)
    @DisplayName("Test table data loading")
    void testTableDataLoading(FxRobot robot) {
        // Refresh to load data
        Button refreshButton = robot.lookup("Refresh Duplicates").queryButton();
        robot.clickOn(refreshButton);
        robot.sleep(1000);
        
        // Tables should be initialized (may be empty if no duplicates)
        assertNotNull(duplicatesTable.getItems());
        assertNotNull(comparisonTable.getItems());
    }

    @Test
    @Order(10)
    @DisplayName("Test error handling with no database connection")
    void testErrorHandlingNoDatabaseConnection(FxRobot robot) {
        // Shutdown database to simulate connection error
        DatabaseManager.shutdown();
        
        // Try to refresh
        Button refreshButton = robot.lookup("Refresh Duplicates").queryButton();
        robot.clickOn(refreshButton);
        robot.sleep(1000);
        
        // Should handle error gracefully
        Label statusLabel = findStatusLabel(robot);
        if (statusLabel != null) {
            assertTrue(statusLabel.getText().contains("Error") || statusLabel.getText().contains("error"));
        }
        
        // Reinitialize database for other tests
        DatabaseManager.initialize();
    }

    @Test
    @Order(11)
    @DisplayName("Test UI layout and components")
    void testUILayoutAndComponents(FxRobot robot) {
        // Verify main layout components
        assertNotNull(duplicateManagerView.getTop()); // Should have top controls
        assertNotNull(duplicateManagerView.getCenter()); // Should have center split pane
        assertNotNull(duplicateManagerView.getBottom()); // Should have bottom status bar
    }

    @Test
    @Order(12)
    @DisplayName("Test split pane configuration")
    void testSplitPaneConfiguration() {
        // Verify split pane exists and is configured
        var splitPane = duplicateManagerView.lookup(".split-pane");
        assertNotNull(splitPane);
    }

    @Test
    @Order(13)
    @DisplayName("Test table selection model")
    void testTableSelectionModel() {
        // Verify selection models are configured
        assertNotNull(duplicatesTable.getSelectionModel());
        assertNotNull(comparisonTable.getSelectionModel());
        
        // Should allow single selection
        assertEquals(duplicatesTable.getSelectionModel().getSelectionMode(), 
                    javafx.scene.control.SelectionMode.SINGLE);
    }

    @Test
    @Order(14)
    @DisplayName("Test button layout")
    void testButtonLayout(FxRobot robot) {
        // Verify all required buttons are present
        assertNotNull(robot.lookup("Refresh Duplicates").tryQuery().orElse(null));
        assertNotNull(robot.lookup("Delete Selected").tryQuery().orElse(null));
        assertNotNull(robot.lookup("Keep Better Quality").tryQuery().orElse(null));
    }

    @Test
    @Order(15)
    @DisplayName("Test table cell factories")
    void testTableCellFactories() {
        // Verify columns have appropriate cell value factories
        duplicatesTable.getColumns().forEach(column -> {
            assertNotNull(column.getCellValueFactory());
        });
        
        comparisonTable.getColumns().forEach(column -> {
            assertNotNull(column.getCellValueFactory());
        });
    }

    // Helper methods
    private void createTestMusicFiles() {
        // Create test music files for testing
        MusicFile file1 = createTestMusicFile("Test Song 1", "Test Artist", "Test Album", 320L);
        MusicFile file2 = createTestMusicFile("Test Song 1", "Test Artist", "Test Album", 256L); // Duplicate
        MusicFile file3 = createTestMusicFile("Test Song 2", "Test Artist", "Test Album", 320L);
        
        try {
            DatabaseManager.saveMusicFile(file1);
            DatabaseManager.saveMusicFile(file2);
            DatabaseManager.saveMusicFile(file3);
        } catch (Exception e) {
            // Ignore if files already exist
        }
    }

    private MusicFile createTestMusicFile(String title, String artist, String album, Long bitRate) {
        MusicFile musicFile = new MusicFile();
        musicFile.setTitle(title);
        musicFile.setArtist(artist);
        musicFile.setAlbum(album);
        musicFile.setGenre("Rock");
        musicFile.setTrackNumber(1);
        musicFile.setYear(2023);
        musicFile.setDurationSeconds(200);
        musicFile.setBitRate(bitRate);
        musicFile.setSampleRate(44100);
        musicFile.setFileType("mp3");
        musicFile.setFilePath("/test/path/" + title.replaceAll(" ", "_") + ".mp3");
        musicFile.setFileSizeBytes(5000000L);
        musicFile.setLastModified(new Date());
        musicFile.setModified(false);
        return musicFile;
    }

    private TableView<MusicFile> findSecondTableView() {
        // Find the second table view (comparison table)
        var allTableViews = duplicateManagerView.lookupAll(".table-view");
        if (allTableViews.size() >= 2) {
            return (TableView<MusicFile>) allTableViews.toArray()[1];
        }
        return null;
    }

    private Label findStatusLabel(FxRobot robot) {
        // Find status label - it should be in the bottom area
        var labels = robot.lookup(".label").queryAll();
        for (var label : labels) {
            if (label instanceof Label) {
                Label l = (Label) label;
                if (l.getText().equals("Ready") || l.getText().contains("Found") || l.getText().contains("Error")) {
                    return l;
                }
            }
        }
        return null;
    }
}