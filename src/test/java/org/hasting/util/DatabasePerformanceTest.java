package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test suite specifically for Issue #41 - Database insert performance optimization.
 * 
 * <p>This test validates that the file path cache improves the performance of
 * saveOrUpdateMusicFile operations by avoiding database queries for duplicate checks.
 * 
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Database Performance Tests - Issue #41")
public class DatabasePerformanceTest {
    
    private static final int TEST_FILE_COUNT = 1000;
    private static final int UPDATE_FILE_COUNT = 100;
    
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
    @DisplayName("Test cache initialization performance")
    void testCacheInitializationPerformance() {
        // First, insert test data
        List<MusicFile> testFiles = createTestMusicFiles(TEST_FILE_COUNT);
        
        long insertStartTime = System.currentTimeMillis();
        for (MusicFile file : testFiles) {
            DatabaseManager.saveMusicFile(file);
        }
        long insertTime = System.currentTimeMillis() - insertStartTime;
        
        System.out.println("Inserted " + TEST_FILE_COUNT + " files in " + insertTime + " ms");
        
        // Now test cache initialization
        long cacheStartTime = System.currentTimeMillis();
        DatabaseManager.initAllPathsMap();
        long cacheTime = System.currentTimeMillis() - cacheStartTime;
        
        System.out.println("Cache initialized with " + DatabaseManager.getFilePathCacheSize() + 
                          " entries in " + cacheTime + " ms");
        
        // Cache initialization should be much faster than inserts
        assertTrue(cacheTime < insertTime / 10, 
                  "Cache initialization should be at least 10x faster than inserts");
        
        // Verify cache size
        assertEquals(TEST_FILE_COUNT, DatabaseManager.getFilePathCacheSize(), 
                    "Cache should contain all inserted files");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test saveOrUpdateMusicFile performance with cache")
    void testSaveOrUpdatePerformanceWithCache() {
        // Ensure cache is populated from previous test
        assertTrue(DatabaseManager.getFilePathCacheSize() > 0, "Cache should be populated");
        
        // Create files - some new, some existing
        List<MusicFile> updateFiles = new ArrayList<>();
        
        // Add existing files (should be updates)
        for (int i = 0; i < UPDATE_FILE_COUNT / 2; i++) {
            MusicFile file = new MusicFile();
            file.setFilePath("/test/file" + i + ".mp3");
            file.setTitle("Updated Title " + i);
            file.setArtist("Updated Artist " + i);
            updateFiles.add(file);
        }
        
        // Add new files
        for (int i = TEST_FILE_COUNT; i < TEST_FILE_COUNT + (UPDATE_FILE_COUNT / 2); i++) {
            MusicFile file = new MusicFile();
            file.setFilePath("/test/file" + i + ".mp3");
            file.setTitle("New Title " + i);
            file.setArtist("New Artist " + i);
            updateFiles.add(file);
        }
        
        // Measure performance with cache
        long startTime = System.currentTimeMillis();
        int updateCount = 0;
        int insertCount = 0;
        
        for (MusicFile file : updateFiles) {
            Long oldId = file.getId();
            DatabaseManager.saveOrUpdateMusicFile(file);
            
            if (oldId == null && file.getId() != null) {
                insertCount++;
            } else if (file.getId() != null) {
                updateCount++;
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        double avgTime = (double) totalTime / UPDATE_FILE_COUNT;
        
        System.out.println("SaveOrUpdate " + UPDATE_FILE_COUNT + " files in " + totalTime + 
                          " ms (avg: " + String.format("%.2f", avgTime) + " ms/file)");
        System.out.println("Updates: " + updateCount + ", Inserts: " + insertCount);
        
        // With cache, average time should be very low
        assertTrue(avgTime < 5.0, "Average time per file should be less than 5ms with cache");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test performance without cache (comparison)")
    void testSaveOrUpdatePerformanceWithoutCache() {
        // Clear the cache to simulate no-cache scenario
        DatabaseManager.clearFilePathCache();
        assertEquals(0, DatabaseManager.getFilePathCacheSize(), "Cache should be empty");
        
        // Create test files (same as cache test but different paths)
        List<MusicFile> testFiles = new ArrayList<>();
        for (int i = 0; i < UPDATE_FILE_COUNT; i++) {
            MusicFile file = new MusicFile();
            file.setFilePath("/test/nocache/file" + i + ".mp3");
            file.setTitle("No Cache Title " + i);
            file.setArtist("No Cache Artist " + i);
            testFiles.add(file);
        }
        
        // Measure performance without cache (will populate cache as we go)
        long startTime = System.currentTimeMillis();
        for (MusicFile file : testFiles) {
            DatabaseManager.saveOrUpdateMusicFile(file);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        double avgTime = (double) totalTime / UPDATE_FILE_COUNT;
        
        System.out.println("SaveOrUpdate " + UPDATE_FILE_COUNT + 
                          " files without pre-populated cache in " + totalTime + 
                          " ms (avg: " + String.format("%.2f", avgTime) + " ms/file)");
        
        // This establishes a baseline - actual improvement depends on database size
        assertTrue(totalTime < 30000, "Should complete within 30 seconds even without cache");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test cache consistency with deletions")
    void testCacheConsistencyWithDeletions() {
        // Ensure we have data
        assertTrue(DatabaseManager.getFilePathCacheSize() > 0, "Cache should have entries");
        
        // Get a file to delete
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        assertTrue(allFiles.size() > 10, "Should have files to test with");
        
        MusicFile fileToDelete = allFiles.get(0);
        String filePath = fileToDelete.getFilePath();
        int initialCacheSize = DatabaseManager.getFilePathCacheSize();
        
        // Delete the file
        boolean deleted = DatabaseManager.deleteMusicFile(fileToDelete);
        assertTrue(deleted || fileToDelete.getId() == null, "File should be deleted");
        
        // Verify cache was updated
        assertEquals(initialCacheSize - 1, DatabaseManager.getFilePathCacheSize(), 
                    "Cache size should decrease by 1");
        
        // Verify we can now insert a file with the same path
        MusicFile newFile = new MusicFile();
        newFile.setFilePath(filePath);
        newFile.setTitle("Replacement File");
        newFile.setArtist("Replacement Artist");
        
        assertDoesNotThrow(() -> DatabaseManager.saveOrUpdateMusicFile(newFile));
        assertNotNull(newFile.getId(), "New file should get an ID");
        
        // Cache should be back to original size
        assertEquals(initialCacheSize, DatabaseManager.getFilePathCacheSize(), 
                    "Cache size should be restored");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test bulk delete cache consistency")
    void testBulkDeleteCacheConsistency() {
        int fileCount = DatabaseManager.getAllMusicFiles().size();
        assertTrue(fileCount > 0, "Should have files in database");
        assertEquals(fileCount, DatabaseManager.getFilePathCacheSize(), 
                    "Cache should match database count");
        
        // Delete all files
        DatabaseManager.deleteAllMusicFiles();
        
        // Verify cache was cleared
        assertEquals(0, DatabaseManager.getFilePathCacheSize(), 
                    "Cache should be empty after deleteAll");
        
        // Verify database is empty
        assertEquals(0, DatabaseManager.getAllMusicFiles().size(), 
                    "Database should be empty");
    }
    
    private List<MusicFile> createTestMusicFiles(int count) {
        List<MusicFile> files = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            MusicFile file = new MusicFile();
            file.setFilePath("/test/file" + i + ".mp3");
            file.setTitle("Test Song " + i);
            file.setArtist("Test Artist " + (i % 100)); // Some artist duplication
            file.setAlbum("Test Album " + (i % 50));    // Some album duplication
            file.setGenre(random.nextBoolean() ? "Rock" : "Pop");
            file.setTrackNumber(i % 20 + 1);
            file.setYear(2020 + (i % 5));
            file.setDurationSeconds(180 + random.nextInt(120));
            file.setBitRate((long)(128 + random.nextInt(192)));
            file.setSampleRate(44100);
            file.setFileType("mp3");
            files.add(file);
        }
        
        return files;
    }
}