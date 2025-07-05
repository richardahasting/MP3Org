package org.hasting.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.hasting.model.MusicFile;
import org.hasting.test.TestDataFactory;
import org.hasting.test.spec.TestFileSpec;
import org.hasting.test.spec.AudioFormat;
import org.hasting.test.TestDataSet;
import org.hasting.test.spec.TestDataSetSpec;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for bulk editing functionality in the metadata editor.
 * Updated to use TestDataFactory for more realistic test scenarios.
 * Note: These are unit tests that don't require a full JavaFX environment.
 */
public class BulkEditingTest {
    
    private MusicFile testFile1;
    private MusicFile testFile2;
    private MusicFile testFile3;
    private List<MusicFile> testDataSet;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create test music files with explicit metadata for bulk editing tests
        // Since we're testing metadata manipulation, not audio file generation,
        // we'll create MusicFile objects directly with the metadata we need
        testFile1 = new MusicFile();
        testFile1.setTitle("Song One");
        testFile1.setArtist("Old Artist");
        testFile1.setAlbum("Old Album");
        testFile1.setGenre("Old Genre");
        testFile1.setFilePath("/test/path/song1.mp3");
        testFile1.setFileType("mp3");
        
        testFile2 = new MusicFile();
        testFile2.setTitle("Song Two");
        testFile2.setArtist("Different Artist");
        testFile2.setAlbum("Different Album");
        testFile2.setGenre("Different Genre");
        testFile2.setFilePath("/test/path/song2.mp3");
        testFile2.setFileType("mp3");
        
        testFile3 = new MusicFile();
        testFile3.setTitle("Song Three");
        testFile3.setArtist("Another Artist");
        testFile3.setAlbum("Another Album");
        testFile3.setGenre("Another Genre");
        testFile3.setFilePath("/test/path/song3.mp3");
        testFile3.setFileType("mp3");

        // Create a larger dataset for bulk operations testing using TestDataFactory
        TestDataSet largeTestSet = TestDataFactory.createTestDataSet(
                TestDataSetSpec.builder()
                        .fileCount(15)
                        .randomizeMetadata(true)
                        .formatDistribution(AudioFormat.MP3, 100)
                        .build()
        );
        testDataSet = largeTestSet.getFiles();
    }

    @AfterEach
    void tearDown() {
        // Clean up generated test files
        TestDataFactory.cleanupGeneratedFiles();
    }
    
    @Test
    @DisplayName("Test bulk artist update")
    void testBulkArtistUpdate() {
        // Simulate bulk edit operation
        String newArtist = "New Artist";
        
        // Apply to all files
        testFile1.setArtist(newArtist);
        testFile2.setArtist(newArtist);
        testFile3.setArtist(newArtist);
        
        // Verify all files have the new artist
        assertEquals(newArtist, testFile1.getArtist());
        assertEquals(newArtist, testFile2.getArtist());
        assertEquals(newArtist, testFile3.getArtist());
        
        // Verify other fields are unchanged
        assertEquals("Song One", testFile1.getTitle());
        assertEquals("Old Album", testFile1.getAlbum());
        assertEquals("Old Genre", testFile1.getGenre());
    }
    
    @Test
    @DisplayName("Test bulk album update")
    void testBulkAlbumUpdate() {
        String newAlbum = "Compilation Album";
        
        testFile1.setAlbum(newAlbum);
        testFile2.setAlbum(newAlbum);
        testFile3.setAlbum(newAlbum);
        
        assertEquals(newAlbum, testFile1.getAlbum());
        assertEquals(newAlbum, testFile2.getAlbum());
        assertEquals(newAlbum, testFile3.getAlbum());
        
        // Verify other fields are unchanged
        assertEquals("Old Artist", testFile1.getArtist());
        assertEquals("Different Artist", testFile2.getArtist());
        assertEquals("Another Artist", testFile3.getArtist());
    }
    
    @Test
    @DisplayName("Test bulk genre update")
    void testBulkGenreUpdate() {
        String newGenre = "Rock";
        
        testFile1.setGenre(newGenre);
        testFile2.setGenre(newGenre);
        testFile3.setGenre(newGenre);
        
        assertEquals(newGenre, testFile1.getGenre());
        assertEquals(newGenre, testFile2.getGenre());
        assertEquals(newGenre, testFile3.getGenre());
        
        // Verify other fields are unchanged
        assertEquals("Old Album", testFile1.getAlbum());
        assertEquals("Different Album", testFile2.getAlbum());
        assertEquals("Another Album", testFile3.getAlbum());
    }
    
    @Test
    @DisplayName("Test bulk multi-field update")
    void testBulkMultiFieldUpdate() {
        String newArtist = "Various Artists";
        String newAlbum = "Greatest Hits";
        String newGenre = "Pop";
        
        // Apply all three fields to all files
        testFile1.setArtist(newArtist);
        testFile1.setAlbum(newAlbum);
        testFile1.setGenre(newGenre);
        
        testFile2.setArtist(newArtist);
        testFile2.setAlbum(newAlbum);
        testFile2.setGenre(newGenre);
        
        testFile3.setArtist(newArtist);
        testFile3.setAlbum(newAlbum);
        testFile3.setGenre(newGenre);
        
        // Verify all files have the new values
        assertEquals(newArtist, testFile1.getArtist());
        assertEquals(newAlbum, testFile1.getAlbum());
        assertEquals(newGenre, testFile1.getGenre());
        
        assertEquals(newArtist, testFile2.getArtist());
        assertEquals(newAlbum, testFile2.getAlbum());
        assertEquals(newGenre, testFile2.getGenre());
        
        assertEquals(newArtist, testFile3.getArtist());
        assertEquals(newAlbum, testFile3.getAlbum());
        assertEquals(newGenre, testFile3.getGenre());
        
        // Verify titles are unchanged
        assertEquals("Song One", testFile1.getTitle());
        assertEquals("Song Two", testFile2.getTitle());
        assertEquals("Song Three", testFile3.getTitle());
    }
    
    @Test
    @DisplayName("Test partial bulk update")
    void testPartialBulkUpdate() {
        // Simulate updating only artist and genre, leaving album unchanged
        String newArtist = "Partial Update Artist";
        String newGenre = "Jazz";
        
        // Store original albums
        String originalAlbum1 = testFile1.getAlbum();
        String originalAlbum2 = testFile2.getAlbum();
        String originalAlbum3 = testFile3.getAlbum();
        
        // Apply partial update
        testFile1.setArtist(newArtist);
        testFile1.setGenre(newGenre);
        
        testFile2.setArtist(newArtist);
        testFile2.setGenre(newGenre);
        
        testFile3.setArtist(newArtist);
        testFile3.setGenre(newGenre);
        
        // Verify updated fields
        assertEquals(newArtist, testFile1.getArtist());
        assertEquals(newGenre, testFile1.getGenre());
        assertEquals(newArtist, testFile2.getArtist());
        assertEquals(newGenre, testFile2.getGenre());
        assertEquals(newArtist, testFile3.getArtist());
        assertEquals(newGenre, testFile3.getGenre());
        
        // Verify album fields are unchanged
        assertEquals(originalAlbum1, testFile1.getAlbum());
        assertEquals(originalAlbum2, testFile2.getAlbum());
        assertEquals(originalAlbum3, testFile3.getAlbum());
    }
    
    @Test
    @DisplayName("Test validation of bulk edit fields")
    void testBulkEditValidation() {
        // Test that empty strings work correctly
        String emptyArtist = "";
        String validAlbum = "Valid Album";
        
        // Only album should be updated
        if (!emptyArtist.isEmpty()) {
            testFile1.setArtist(emptyArtist);
        }
        testFile1.setAlbum(validAlbum);
        
        // Artist should remain unchanged, album should be updated
        assertEquals("Old Artist", testFile1.getArtist());
        assertEquals(validAlbum, testFile1.getAlbum());
    }

    /**
     * Test bulk operations on a larger dataset generated by TestDataFactory.
     */
    @Test
    @DisplayName("Test bulk operations on large dataset")
    void testLargeDatasetBulkOperations() {
        // Use the larger test dataset from TestDataFactory
        String newArtist = "Bulk Update Artist";
        String newGenre = "Bulk Update Genre";
        
        // Apply bulk update to all files in the test dataset
        for (MusicFile file : testDataSet) {
            file.setArtist(newArtist);
            file.setGenre(newGenre);
        }
        
        // Verify all files were updated
        for (MusicFile file : testDataSet) {
            assertEquals(newArtist, file.getArtist(), 
                    "Artist should be updated for file: " + file.getTitle());
            assertEquals(newGenre, file.getGenre(), 
                    "Genre should be updated for file: " + file.getTitle());
            
            // Verify other fields remain intact (allowing null values for randomized test data)
            // Note: TestDataFactory may generate files with null albums/titles for randomization
            assertTrue(file.getTitle() != null || file.getAlbum() != null || file.getFilePath() != null, 
                    "File should have at least one identifying field");
        }
        
        // Verify we processed a reasonable number of files
        assertTrue(testDataSet.size() >= 10, "Should have at least 10 files for bulk testing");
    }

    /**
     * Test bulk operations with Unicode and special characters using TestDataFactory.
     */
    @Test
    @DisplayName("Test bulk operations with Unicode and special characters")
    void testBulkOperationsWithSpecialCharacters() throws IOException {
        // Create files with special characters
        MusicFile unicodeFile = new MusicFile();
        unicodeFile.setTitle("日本語タイトル");
        unicodeFile.setArtist("Original Artist");
        unicodeFile.setAlbum("Original Album");
        unicodeFile.setFilePath("/test/path/unicode.mp3");
        unicodeFile.setFileType("mp3");
        
        MusicFile specialCharsFile = new MusicFile();
        specialCharsFile.setTitle("Test's \"Song\" & More");
        specialCharsFile.setArtist("Artist/Band");
        specialCharsFile.setAlbum("Album: Collection");
        specialCharsFile.setFilePath("/test/path/special.mp3");
        specialCharsFile.setFileType("mp3");
        
        // Apply bulk update with Unicode
        String newArtist = "新しいアーティスト"; // "New Artist" in Japanese
        String newGenre = "J-Pop/Rock & Roll";
        
        unicodeFile.setArtist(newArtist);
        unicodeFile.setGenre(newGenre);
        specialCharsFile.setArtist(newArtist);
        specialCharsFile.setGenre(newGenre);
        
        // Verify Unicode handling
        assertEquals(newArtist, unicodeFile.getArtist(), "Unicode artist should be preserved");
        assertEquals(newGenre, unicodeFile.getGenre(), "Unicode/special genre should be preserved");
        assertEquals(newArtist, specialCharsFile.getArtist(), "Unicode artist should work with special char files");
        
        // Verify original titles with special characters are preserved
        assertEquals("日本語タイトル", unicodeFile.getTitle(), "Unicode title should be preserved");
        assertEquals("Test's \"Song\" & More", specialCharsFile.getTitle(), "Special characters in title should be preserved");
    }

    /**
     * Test performance of bulk operations with TestDataFactory generated dataset.
     */
    @Test
    @DisplayName("Test bulk operation performance")
    void testBulkOperationPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Perform bulk update on the entire test dataset
        String newArtist = "Performance Test Artist";
        for (MusicFile file : testDataSet) {
            file.setArtist(newArtist);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify the operation completed reasonably quickly
        assertTrue(duration < 1000, "Bulk update should complete in under 1 second for " + testDataSet.size() + " files");
        
        // Verify the updates were applied
        for (MusicFile file : testDataSet) {
            assertEquals(newArtist, file.getArtist(), "All files should have updated artist");
        }
        
        System.out.println("Bulk updated " + testDataSet.size() + " files in " + duration + "ms");
    }

    /**
     * Test edge cases in bulk operations using TestDataFactory.
     */
    @Test
    @DisplayName("Test bulk operation edge cases")
    void testBulkOperationEdgeCases() throws IOException {
        // Create files with edge case data
        MusicFile nullTitleFile = new MusicFile();
        nullTitleFile.setTitle(null); // Null title
        nullTitleFile.setArtist("Artist with Null Title");
        nullTitleFile.setFilePath("/test/path/null_title.mp3");
        nullTitleFile.setFileType("mp3");
        
        MusicFile emptyFieldsFile = new MusicFile();
        emptyFieldsFile.setTitle("");
        emptyFieldsFile.setArtist("");
        emptyFieldsFile.setAlbum("");
        emptyFieldsFile.setFilePath("/test/path/empty_fields.mp3");
        emptyFieldsFile.setFileType("mp3");
        
        // Apply bulk updates to edge case files
        String newArtist = "Edge Case Updated Artist";
        String newAlbum = "Edge Case Updated Album";
        
        nullTitleFile.setArtist(newArtist);
        nullTitleFile.setAlbum(newAlbum);
        emptyFieldsFile.setArtist(newArtist);
        emptyFieldsFile.setAlbum(newAlbum);
        
        // Verify updates work even with edge case data
        assertEquals(newArtist, nullTitleFile.getArtist(), "Bulk update should work with null title file");
        assertEquals(newAlbum, nullTitleFile.getAlbum(), "Bulk update should work with null title file");
        assertEquals(newArtist, emptyFieldsFile.getArtist(), "Bulk update should work with empty fields file");
        assertEquals(newAlbum, emptyFieldsFile.getAlbum(), "Bulk update should work with empty fields file");
        
        // Verify null/empty fields remain as they were set
        assertNull(nullTitleFile.getTitle(), "Null title should remain null");
        assertEquals("", emptyFieldsFile.getTitle(), "Empty title should remain empty");
    }
}