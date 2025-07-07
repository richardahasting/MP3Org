package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * Test suite for the scan directories functionality in DatabaseManager.
 * Tests the new scan_directories table and related methods for Issue #49.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Scan Directories Tests - Issue #49")
public class ScanDirectoriesTest {
    
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
    
    @BeforeEach
    void clearScanDirectories() {
        // Clear scan directories before each test
        try {
            DatabaseManager.getConnection().createStatement()
                .executeUpdate("DELETE FROM scan_directories");
        } catch (Exception e) {
            // Ignore if table doesn't exist yet
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Test recording scan directories")
    void testRecordScanDirectory() {
        // Test recording a new scan directory
        DatabaseManager.recordScanDirectory("/music/test1");
        
        List<String> scanDirs = DatabaseManager.getScanDirectories();
        assertEquals(1, scanDirs.size(), "Should have 1 scan directory");
        assertTrue(scanDirs.contains("/music/test1"), "Should contain recorded directory");
        
        // Test recording another directory
        DatabaseManager.recordScanDirectory("/music/test2");
        scanDirs = DatabaseManager.getScanDirectories();
        assertEquals(2, scanDirs.size(), "Should have 2 scan directories");
        assertTrue(scanDirs.contains("/music/test2"), "Should contain second directory");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test duplicate scan directory handling")
    void testDuplicateScanDirectory() {
        // Record the same directory twice
        DatabaseManager.recordScanDirectory("/music/duplicate");
        DatabaseManager.recordScanDirectory("/music/duplicate");
        
        List<String> scanDirs = DatabaseManager.getScanDirectories();
        assertEquals(1, scanDirs.size(), "Should only have 1 directory (no duplicates)");
        assertEquals("/music/duplicate", scanDirs.get(0), "Should contain the directory");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test scan directory ordering")
    void testScanDirectoryOrdering() {
        // Record directories in non-alphabetical order
        DatabaseManager.recordScanDirectory("/music/zzz");
        DatabaseManager.recordScanDirectory("/music/aaa");
        DatabaseManager.recordScanDirectory("/music/mmm");
        
        List<String> scanDirs = DatabaseManager.getScanDirectories();
        assertEquals(3, scanDirs.size(), "Should have 3 directories");
        
        // Verify alphabetical ordering
        assertEquals("/music/aaa", scanDirs.get(0), "First should be alphabetically first");
        assertEquals("/music/mmm", scanDirs.get(1), "Second should be alphabetically middle");
        assertEquals("/music/zzz", scanDirs.get(2), "Third should be alphabetically last");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test empty and null directory handling")
    void testEmptyAndNullDirectories() {
        int initialCount = DatabaseManager.getScanDirectories().size();
        
        // Test null and empty strings
        DatabaseManager.recordScanDirectory(null);
        DatabaseManager.recordScanDirectory("");
        DatabaseManager.recordScanDirectory("   ");
        
        List<String> scanDirs = DatabaseManager.getScanDirectories();
        assertEquals(initialCount, scanDirs.size(), "Should not record null/empty directories");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test rescan timestamp update")
    void testRescanTimestampUpdate() {
        // Record a directory
        DatabaseManager.recordScanDirectory("/music/rescan-test");
        
        // Update rescan timestamp
        assertDoesNotThrow(() -> {
            DatabaseManager.updateScanDirectoryRescanTime("/music/rescan-test");
        }, "Should not throw exception when updating existing directory");
        
        // Try to update non-existent directory (should not throw)
        assertDoesNotThrow(() -> {
            DatabaseManager.updateScanDirectoryRescanTime("/music/non-existent");
        }, "Should not throw exception for non-existent directory");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test scan directories vs distinct directories difference")
    void testScanDirectoriesVsDistinctDirectories() {
        // Clear everything first
        DatabaseManager.deleteAllMusicFiles();
        
        // Record scan directories
        DatabaseManager.recordScanDirectory("/music/root1");
        DatabaseManager.recordScanDirectory("/music/root2");
        
        // Add music files in subdirectories
        MusicFile file1 = new MusicFile();
        file1.setFilePath("/music/root1/artist1/album1/song1.mp3");
        file1.setTitle("Song 1");
        file1.setArtist("Artist 1");
        DatabaseManager.saveMusicFile(file1);
        
        MusicFile file2 = new MusicFile();
        file2.setFilePath("/music/root1/artist2/album2/song2.mp3");
        file2.setTitle("Song 2");
        file2.setArtist("Artist 2");
        DatabaseManager.saveMusicFile(file2);
        
        MusicFile file3 = new MusicFile();
        file3.setFilePath("/music/root2/artist3/album3/song3.mp3");
        file3.setTitle("Song 3");
        file3.setArtist("Artist 3");
        DatabaseManager.saveMusicFile(file3);
        
        // Get scan directories (should be original root directories)
        List<String> scanDirs = DatabaseManager.getScanDirectories();
        assertEquals(2, scanDirs.size(), "Should have 2 scan directories");
        assertTrue(scanDirs.contains("/music/root1"), "Should contain root1");
        assertTrue(scanDirs.contains("/music/root2"), "Should contain root2");
        
        // Get distinct directories (should be all file parent directories)
        List<String> distinctDirs = DatabaseManager.getDistinctDirectories();
        assertEquals(3, distinctDirs.size(), "Should have 3 distinct file directories");
        assertTrue(distinctDirs.contains("/music/root1/artist1/album1"), "Should contain file directory");
        assertTrue(distinctDirs.contains("/music/root1/artist2/album2"), "Should contain file directory");
        assertTrue(distinctDirs.contains("/music/root2/artist3/album3"), "Should contain file directory");
        
        // Verify the difference - scan directories should be much cleaner
        assertTrue(scanDirs.size() < distinctDirs.size(), 
                  "Scan directories should be fewer than distinct file directories");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test getScanDirectories with empty table")
    void testGetScanDirectoriesEmpty() {
        List<String> scanDirs = DatabaseManager.getScanDirectories();
        assertNotNull(scanDirs, "Should return non-null list");
        assertTrue(scanDirs.isEmpty(), "Should return empty list when no directories recorded");
    }
}