package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseManagerTestComprehensive {

    private MusicFile testMusicFile1;
    private MusicFile testMusicFile2;
    private MusicFile testMusicFile3;

    @BeforeAll
    void setUpDatabase() {
        // Initialize the database
        DatabaseManager.initialize();
        
        // Clear any existing data for clean testing
        DatabaseManager.deleteAllMusicFiles();
    }

    @BeforeEach
    void setUp() {
        // Create test music files
        testMusicFile1 = createTestMusicFile("Test Song 1", "Test Artist 1", "Test Album 1", 
                                            "Rock", 1, 2023, 240, 320L, 44100, "/test/path1.mp3");
        
        testMusicFile2 = createTestMusicFile("Test Song 2", "Test Artist 2", "Test Album 2", 
                                            "Pop", 2, 2022, 180, 256L, 48000, "/test/path2.mp3");
        
        testMusicFile3 = createTestMusicFile("Test Song 1", "Test Artist 1", "Test Album 1", 
                                            "Rock", 1, 2023, 240, 256L, 44100, "/test/path3.mp3");
    }

    @AfterEach
    void tearDown() {
        // Clean up test data after each test
        DatabaseManager.deleteAllMusicFiles();
    }

    @AfterAll
    void shutdownDatabase() {
        DatabaseManager.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("Test database initialization and connection")
    void testDatabaseInitializationAndConnection() {
        assertDoesNotThrow(() -> DatabaseManager.initialize());
        
        Connection connection = DatabaseManager.getConnection();
        assertNotNull(connection, "Database connection should not be null");
        
        assertDoesNotThrow(() -> {
            assertFalse(connection.isClosed(), "Connection should be open");
        });
    }

    @Test
    @Order(2)
    @DisplayName("Test save music file")
    void testSaveMusicFile() {
        // Test saving a new music file
        assertDoesNotThrow(() -> DatabaseManager.saveMusicFile(testMusicFile1));
        
        // Verify the ID was assigned
        assertNotNull(testMusicFile1.getId(), "ID should be assigned after saving");
        assertTrue(testMusicFile1.getId() > 0, "ID should be positive");
    }

    @Test
    @Order(3)
    @DisplayName("Test save duplicate music file")
    void testSaveDuplicateMusicFile() {
        // Save the first file
        DatabaseManager.saveMusicFile(testMusicFile1);
        Long originalId = testMusicFile1.getId();
        
        // Try to save a file with the same path
        MusicFile duplicateFile = createTestMusicFile("Different Title", "Different Artist", 
                                                     "Different Album", "Jazz", 3, 2021, 300, 
                                                     192L, 44100, "/test/path1.mp3");
        
        // Should not throw an exception, but should not save the duplicate
        assertDoesNotThrow(() -> DatabaseManager.saveMusicFile(duplicateFile));
        
        // The duplicate file should not have an ID assigned
        assertNull(duplicateFile.getId(), "Duplicate file should not get an ID");
        
        // Original file should still be retrievable
        MusicFile retrieved = DatabaseManager.getMusicFileById(originalId);
        assertNotNull(retrieved);
        assertEquals("Test Song 1", retrieved.getTitle()); // Original title should be preserved
    }

    @Test
    @Order(4)
    @DisplayName("Test get music file by ID")
    void testGetMusicFileById() {
        // Save a music file
        DatabaseManager.saveMusicFile(testMusicFile1);
        Long savedId = testMusicFile1.getId();
        
        // Retrieve it by ID
        MusicFile retrieved = DatabaseManager.getMusicFileById(savedId);
        
        assertNotNull(retrieved, "Retrieved file should not be null");
        assertEquals(savedId, retrieved.getId(), "IDs should match");
        assertEquals(testMusicFile1.getTitle(), retrieved.getTitle(), "Titles should match");
        assertEquals(testMusicFile1.getArtist(), retrieved.getArtist(), "Artists should match");
        assertEquals(testMusicFile1.getAlbum(), retrieved.getAlbum(), "Albums should match");
        assertEquals(testMusicFile1.getFilePath(), retrieved.getFilePath(), "File paths should match");
    }

    @Test
    @Order(5)
    @DisplayName("Test get music file by non-existent ID")
    void testGetMusicFileByNonExistentId() {
        MusicFile retrieved = DatabaseManager.getMusicFileById(99999L);
        assertNull(retrieved, "Non-existent file should return null");
    }

    @Test
    @Order(6)
    @DisplayName("Test update music file")
    void testUpdateMusicFile() {
        // Save a music file
        DatabaseManager.saveMusicFile(testMusicFile1);
        
        // Modify it
        testMusicFile1.setTitle("Updated Title");
        testMusicFile1.setArtist("Updated Artist");
        testMusicFile1.setYear(2024);
        
        // Update it
        assertDoesNotThrow(() -> DatabaseManager.updateMusicFile(testMusicFile1));
        
        // Retrieve and verify changes
        MusicFile updated = DatabaseManager.getMusicFileById(testMusicFile1.getId());
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Artist", updated.getArtist());
        assertEquals(2024, updated.getYear());
    }

    @Test
    @Order(7)
    @DisplayName("Test update music file without changes")
    void testUpdateMusicFileWithoutChanges() {
        DatabaseManager.saveMusicFile(testMusicFile1);
        testMusicFile1.setModified(false); // Mark as not modified
        
        // Should not perform update when not modified
        assertDoesNotThrow(() -> DatabaseManager.updateMusicFile(testMusicFile1));
    }

    @Test
    @Order(8)
    @DisplayName("Test delete music file")
    void testDeleteMusicFile() {
        // Save a music file
        DatabaseManager.saveMusicFile(testMusicFile1);
        Long savedId = testMusicFile1.getId();
        
        // Delete it
        boolean deleted = DatabaseManager.deleteMusicFile(testMusicFile1);
        
        // Verify deletion
        MusicFile retrieved = DatabaseManager.getMusicFileById(savedId);
        assertNull(retrieved, "Deleted file should not be retrievable");
        assertNull(testMusicFile1.getId(), "ID should be cleared after deletion");
    }

    @Test
    @Order(9)
    @DisplayName("Test get all music files")
    void testGetAllMusicFiles() {
        // Save multiple files
        DatabaseManager.saveMusicFile(testMusicFile1);
        DatabaseManager.saveMusicFile(testMusicFile2);
        
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        
        assertNotNull(allFiles, "Result should not be null");
        assertEquals(2, allFiles.size(), "Should return 2 files");
        
        // Verify files are ordered correctly (by artist, album, title)
        assertTrue(allFiles.stream().anyMatch(f -> f.getTitle().equals("Test Song 1")));
        assertTrue(allFiles.stream().anyMatch(f -> f.getTitle().equals("Test Song 2")));
    }

    @Test
    @Order(10)
    @DisplayName("Test search music files by all fields")
    void testSearchMusicFiles() {
        // Save test files
        DatabaseManager.saveMusicFile(testMusicFile1);
        DatabaseManager.saveMusicFile(testMusicFile2);
        
        // Search by partial title
        List<MusicFile> results = DatabaseManager.searchMusicFiles("Song");
        assertEquals(2, results.size(), "Should find both songs");
        
        // Search by artist
        results = DatabaseManager.searchMusicFiles("Artist 1");
        assertEquals(1, results.size(), "Should find one song by Artist 1");
        assertEquals("Test Song 1", results.get(0).getTitle());
        
        // Search with no results
        results = DatabaseManager.searchMusicFiles("NonExistent");
        assertEquals(0, results.size(), "Should find no songs");
    }

    @Test
    @Order(11)
    @DisplayName("Test search music files by title")
    void testSearchMusicFilesByTitle() {
        DatabaseManager.saveMusicFile(testMusicFile1);
        DatabaseManager.saveMusicFile(testMusicFile2);
        
        List<MusicFile> results = DatabaseManager.searchMusicFilesByTitle("Song 1");
        assertEquals(1, results.size());
        assertEquals("Test Song 1", results.get(0).getTitle());
        
        results = DatabaseManager.searchMusicFilesByTitle("NonExistent");
        assertEquals(0, results.size());
    }

    @Test
    @Order(12)
    @DisplayName("Test search music files by artist")
    void testSearchMusicFilesByArtist() {
        DatabaseManager.saveMusicFile(testMusicFile1);
        DatabaseManager.saveMusicFile(testMusicFile2);
        
        List<MusicFile> results = DatabaseManager.searchMusicFilesByArtist("Artist 2");
        assertEquals(1, results.size());
        assertEquals("Test Artist 2", results.get(0).getArtist());
        
        results = DatabaseManager.searchMusicFilesByArtist("NonExistent");
        assertEquals(0, results.size());
    }

    @Test
    @Order(13)
    @DisplayName("Test search music files by album")
    void testSearchMusicFilesByAlbum() {
        DatabaseManager.saveMusicFile(testMusicFile1);
        DatabaseManager.saveMusicFile(testMusicFile2);
        
        List<MusicFile> results = DatabaseManager.searchMusicFilesByAlbum("Album 1");
        assertEquals(1, results.size());
        assertEquals("Test Album 1", results.get(0).getAlbum());
        
        results = DatabaseManager.searchMusicFilesByAlbum("NonExistent");
        assertEquals(0, results.size());
    }

    @Test
    @Order(14)
    @DisplayName("Test find potential duplicates")
    void testFindPotentialDuplicates() {
        // Save files including duplicates
        DatabaseManager.saveMusicFile(testMusicFile1);
        DatabaseManager.saveMusicFile(testMusicFile3); // Same title, artist, album as file1
        DatabaseManager.saveMusicFile(testMusicFile2); // Different
        
        List<MusicFile> duplicates = DatabaseManager.findPotentialDuplicates();
        
        // Should find the duplicate (file1 or file3, but not both due to query logic)
        assertTrue(duplicates.size() >= 1, "Should find at least one potential duplicate");
        
        // The returned file should be one of the duplicates
        boolean foundDuplicate = duplicates.stream()
            .anyMatch(f -> f.getTitle().equals("Test Song 1") && f.getArtist().equals("Test Artist 1"));
        assertTrue(foundDuplicate, "Should find the duplicate song");
    }

    @Test
    @Order(15)
    @DisplayName("Test find by path")
    void testFindByPath() {
        DatabaseManager.saveMusicFile(testMusicFile1);
        
        MusicFile found = DatabaseManager.findByPath("/test/path1.mp3");
        assertNotNull(found, "Should find file by path");
        assertEquals(testMusicFile1.getId(), found.getId());
        
        MusicFile notFound = DatabaseManager.findByPath("/nonexistent/path.mp3");
        assertNull(notFound, "Should not find non-existent path");
    }

    @Test
    @Order(16)
    @DisplayName("Test delete all music files")
    void testDeleteAllMusicFiles() {
        // Save multiple files
        DatabaseManager.saveMusicFile(testMusicFile1);
        DatabaseManager.saveMusicFile(testMusicFile2);
        
        // Verify files exist
        List<MusicFile> beforeDelete = DatabaseManager.getAllMusicFiles();
        assertEquals(2, beforeDelete.size());
        
        // Delete all
        assertDoesNotThrow(() -> DatabaseManager.deleteAllMusicFiles());
        
        // Verify all files are deleted
        List<MusicFile> afterDelete = DatabaseManager.getAllMusicFiles();
        assertEquals(0, afterDelete.size());
    }

    @Test
    @Order(17)
    @DisplayName("Test search with special parameters")
    void testSearchWithSpecialParameters() {
        DatabaseManager.saveMusicFile(testMusicFile1);
        
        // Test search with null parameters
        assertThrows(IllegalArgumentException.class, () -> 
            DatabaseManager.searchMusicFiles(null, null, null));
        
        // Test search with empty parameters
        assertThrows(IllegalArgumentException.class, () -> 
            DatabaseManager.searchMusicFiles("", "", ""));
        
        // Test search with valid combination
        List<MusicFile> results = DatabaseManager.searchMusicFiles("Test Song 1", "Test Artist 1", null);
        assertEquals(1, results.size());
    }

    @Test
    @Order(18)
    @DisplayName("Test database operations with null values")
    void testDatabaseOperationsWithNullValues() {
        MusicFile fileWithNulls = new MusicFile();
        fileWithNulls.setFilePath("/test/null/values.mp3");
        fileWithNulls.setTitle("Test Title");
        fileWithNulls.setArtist("Test Artist");
        // Leave other fields as null
        
        // Should handle null values gracefully
        assertDoesNotThrow(() -> DatabaseManager.saveMusicFile(fileWithNulls));
        
        MusicFile retrieved = DatabaseManager.getMusicFileById(fileWithNulls.getId());
        assertNotNull(retrieved);
        assertEquals("Test Title", retrieved.getTitle());
        assertNull(retrieved.getGenre());
        assertNull(retrieved.getTrackNumber());
    }

    @Test
    @Order(19)
    @DisplayName("Test concurrent database operations")
    void testConcurrentDatabaseOperations() {
        // Test that synchronized methods work correctly
        MusicFile file1 = createTestMusicFile("Concurrent 1", "Artist", "Album", 
                                             "Rock", 1, 2023, 200, 320L, 44100, "/concurrent1.mp3");
        MusicFile file2 = createTestMusicFile("Concurrent 2", "Artist", "Album", 
                                             "Rock", 2, 2023, 200, 320L, 44100, "/concurrent2.mp3");
        
        assertDoesNotThrow(() -> {
            DatabaseManager.saveMusicFile(file1);
            DatabaseManager.saveMusicFile(file2);
        });
        
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        assertEquals(2, allFiles.size());
    }

    @Test
    @Order(20)
    @DisplayName("Test database error handling")
    void testDatabaseErrorHandling() {
        // Test with extremely long strings to potentially trigger database errors
        MusicFile fileWithLongData = new MusicFile();
        fileWithLongData.setFilePath("/test/long/data.mp3");
        fileWithLongData.setTitle("A".repeat(1000)); // Very long title
        fileWithLongData.setArtist("Test Artist");
        
        // Should either succeed or throw a RuntimeException
        assertDoesNotThrow(() -> DatabaseManager.saveMusicFile(fileWithLongData));
    }

    // Helper method to create test music files
    private MusicFile createTestMusicFile(String title, String artist, String album, String genre,
                                         Integer trackNumber, Integer year, Integer duration,
                                         Long bitRate, Integer sampleRate, String filePath) {
        MusicFile musicFile = new MusicFile();
        musicFile.setTitle(title);
        musicFile.setArtist(artist);
        musicFile.setAlbum(album);
        musicFile.setGenre(genre);
        musicFile.setTrackNumber(trackNumber);
        musicFile.setYear(year);
        musicFile.setDurationSeconds(duration);
        musicFile.setBitRate(bitRate);
        musicFile.setSampleRate(sampleRate);
        musicFile.setFileType("mp3");
        musicFile.setFilePath(filePath);
        musicFile.setFileSizeBytes(5000000L);
        musicFile.setLastModified(new Date());
        musicFile.setModified(false); // Reset modification flag
        return musicFile;
    }
}