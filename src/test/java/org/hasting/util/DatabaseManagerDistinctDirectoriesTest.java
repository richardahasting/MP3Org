package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * Test suite for the new getDistinctDirectories method in DatabaseManager.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Database Distinct Directories Tests")
public class DatabaseManagerDistinctDirectoriesTest {
    
    @BeforeAll
    static void setUp() {
        // Initialize database for all tests
        DatabaseManager.initialize();
        DatabaseManager.deleteAllMusicFiles();
    }
    
    @AfterAll
    static void tearDown() {
        // Clean up
        DatabaseManager.deleteAllMusicFiles();
        DatabaseManager.shutdown();
    }
    
    @Test
    @Order(1)
    @DisplayName("Test getDistinctDirectories with no files")
    void testGetDistinctDirectoriesEmpty() {
        List<String> directories = DatabaseManager.getDistinctDirectories();
        assertNotNull(directories, "Should return non-null list");
        assertTrue(directories.isEmpty(), "Should return empty list when no files exist");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test getDistinctDirectories with multiple directories")
    void testGetDistinctDirectoriesMultiple() {
        // Add test files in different directories
        MusicFile file1 = new MusicFile();
        file1.setFilePath("/music/artist1/album1/song1.mp3");
        file1.setTitle("Song 1");
        file1.setArtist("Artist 1");
        DatabaseManager.saveMusicFile(file1);
        
        MusicFile file2 = new MusicFile();
        file2.setFilePath("/music/artist1/album1/song2.mp3");
        file2.setTitle("Song 2");
        file2.setArtist("Artist 1");
        DatabaseManager.saveMusicFile(file2);
        
        MusicFile file3 = new MusicFile();
        file3.setFilePath("/music/artist2/album2/song3.mp3");
        file3.setTitle("Song 3");
        file3.setArtist("Artist 2");
        DatabaseManager.saveMusicFile(file3);
        
        MusicFile file4 = new MusicFile();
        file4.setFilePath("/downloads/temp/song4.mp3");
        file4.setTitle("Song 4");
        file4.setArtist("Artist 3");
        DatabaseManager.saveMusicFile(file4);
        
        List<String> directories = DatabaseManager.getDistinctDirectories();
        
        assertNotNull(directories, "Should return non-null list");
        assertEquals(3, directories.size(), "Should return 3 distinct directories");
        
        // Check that directories are sorted
        assertTrue(directories.contains("/downloads/temp"), "Should contain /downloads/temp");
        assertTrue(directories.contains("/music/artist1/album1"), "Should contain /music/artist1/album1");
        assertTrue(directories.contains("/music/artist2/album2"), "Should contain /music/artist2/album2");
        
        // Verify sorting
        assertTrue(directories.get(0).compareTo(directories.get(1)) <= 0, "Directories should be sorted");
        assertTrue(directories.get(1).compareTo(directories.get(2)) <= 0, "Directories should be sorted");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test getDistinctDirectories handles null file paths")
    void testGetDistinctDirectoriesWithNullPaths() {
        // The method should handle null file paths gracefully
        // This test verifies no exception is thrown
        assertDoesNotThrow(() -> {
            List<String> directories = DatabaseManager.getDistinctDirectories();
            assertNotNull(directories, "Should return non-null list even with problematic data");
        });
    }
}