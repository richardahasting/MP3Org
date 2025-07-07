package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Test suite for the batch insert functionality in DatabaseManager.
 * Tests the new saveMusicFilesBatch method for performance and correctness.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Database Batch Insert Tests")
public class DatabaseBatchInsertTest {
    
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
    @DisplayName("Test batch insert performance compared to individual inserts")
    void testBatchInsertPerformance() {
        int testFileCount = 100;
        
        // Create test data
        List<MusicFile> testFiles = createTestMusicFiles(testFileCount);
        
        // Test batch insert
        long batchStartTime = System.currentTimeMillis();
        int insertedCount = DatabaseManager.saveMusicFilesBatch(testFiles);
        long batchTime = System.currentTimeMillis() - batchStartTime;
        
        assertEquals(testFileCount, insertedCount, "Should insert all test files");
        assertTrue(batchTime < 1000, "Batch insert should complete within 1 second for 100 files");
        
        System.out.println("Batch insert: " + testFileCount + " files in " + batchTime + "ms (avg: " + 
                          (batchTime / (double) testFileCount) + "ms/file)");
        
        // Verify files were actually inserted
        assertEquals(testFileCount, DatabaseManager.getAllMusicFiles().size(), 
                    "Database should contain all inserted files");
        
        // Ensure cache is synchronized before checking
        DatabaseManager.initAllPathsMap();
        
        // Verify cache was updated 
        assertEquals(testFileCount, DatabaseManager.getFilePathCacheSize(), 
                    "Cache should contain all inserted files");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test batch insert handles duplicates correctly")
    void testBatchInsertDuplicates() {
        // Ensure cache is synchronized before this test
        DatabaseManager.initAllPathsMap();
        
        // Create some new files and some duplicates
        List<MusicFile> mixedFiles = new ArrayList<>();
        
        // Add some duplicate files (should be skipped) - use same paths from previous test
        for (int i = 0; i < 10; i++) {
            MusicFile duplicate = new MusicFile();
            duplicate.setFilePath("/test/batch/file" + i + ".mp3");
            duplicate.setTitle("Existing Title " + i);
            duplicate.setArtist("Existing Artist " + i);
            mixedFiles.add(duplicate);
        }
        
        // Add some new files (should be inserted)
        for (int i = 100; i < 120; i++) {
            MusicFile newFile = new MusicFile();
            newFile.setFilePath("/test/batch/newfile" + i + ".mp3");
            newFile.setTitle("New Title " + i);
            newFile.setArtist("New Artist " + i);
            mixedFiles.add(newFile);
        }
        
        int originalCount = DatabaseManager.getAllMusicFiles().size();
        int insertedCount = DatabaseManager.saveMusicFilesBatch(mixedFiles);
        
        assertEquals(20, insertedCount, "Should insert only the 20 new files");
        assertEquals(originalCount + 20, DatabaseManager.getAllMusicFiles().size(), 
                    "Database should have 20 more files");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test batch insert with empty collection")
    void testBatchInsertEmpty() {
        List<MusicFile> emptyList = new ArrayList<>();
        int insertedCount = DatabaseManager.saveMusicFilesBatch(emptyList);
        
        assertEquals(0, insertedCount, "Should insert 0 files from empty collection");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test batch insert with null collection")
    void testBatchInsertNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseManager.saveMusicFilesBatch(null);
        }, "Should throw IllegalArgumentException for null collection");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test batch insert with null/invalid entries")
    void testBatchInsertNullEntries() {
        List<MusicFile> mixedList = new ArrayList<>();
        
        // Add null entry
        mixedList.add(null);
        
        // Add invalid entry (no file path)
        MusicFile invalidFile = new MusicFile();
        mixedList.add(invalidFile);
        
        // Add valid entry
        MusicFile validFile = new MusicFile();
        validFile.setFilePath("/test/batch/validfile.mp3");
        validFile.setTitle("Valid Title");
        validFile.setArtist("Valid Artist");
        mixedList.add(validFile);
        
        int insertedCount = DatabaseManager.saveMusicFilesBatch(mixedList);
        
        assertEquals(1, insertedCount, "Should insert only the 1 valid file");
    }
    
    private List<MusicFile> createTestMusicFiles(int count) {
        List<MusicFile> files = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            MusicFile file = new MusicFile();
            file.setFilePath("/test/batch/file" + i + ".mp3");
            file.setTitle("Test Title " + i);
            file.setArtist("Test Artist " + i);
            file.setAlbum("Test Album " + (i / 10)); // Group files into albums
            file.setGenre("Test Genre");
            file.setTrackNumber(i % 10 + 1);
            file.setYear(2020 + (i % 5));
            file.setDurationSeconds(180 + (i % 120)); // 3-5 minutes
            file.setFileSizeBytes(3000000L + (i * 1000)); // ~3MB files
            file.setBitRate(320L);
            file.setSampleRate(44100);
            file.setFileType("mp3");
            files.add(file);
        }
        
        return files;
    }
}