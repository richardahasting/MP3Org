package org.hasting.util;

import org.hasting.model.MusicFile;
import org.hasting.test.TestDataFactory;
import org.hasting.test.spec.TestFileSpec;
import org.hasting.test.spec.AudioFormat;
import org.hasting.test.spec.TestDataSetSpec;
import org.hasting.test.TestDataSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.hasting.util.DatabaseManager;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DatabaseManager functionality.
 * Updated to use TestDataFactory for more realistic test data and better coverage.
 */
public class DatabaseManagerTest {

    @BeforeAll
    public static void setUp() {
        DatabaseManager.initialize();
    }

    @AfterAll
    public static void tearDown() {
        DatabaseManager.shutdown();
    }

    @AfterEach
    public void cleanupTestFiles() {
        // Clean up generated test files after each test
        TestDataFactory.cleanupGeneratedFiles();
    }

    @Test
    public void testDatabaseConnection() {
        assertNotNull(DatabaseManager.getConnection(), "Connection should not be null");
    }

    @Test
    public void testSaveAndRetrieveMusicFile() throws IOException {
        // Use TestDataFactory to create realistic test data with actual audio file
        MusicFile musicFile = TestDataFactory.createCustomFile(
                TestFileSpec.builder()
                        .title("Test Song")
                        .artist("Test Artist")
                        .album("Test Album")
                        .genre("Test Genre")
                        .trackNumber(1)
                        .year(2023)
                        .format(AudioFormat.MP3)
                        .build()
        );

        // Save the music file
        DatabaseManager.saveMusicFile(musicFile);

        // Retrieve the music file by ID
        MusicFile retrievedMusicFile = DatabaseManager.getMusicFileById(musicFile.getId());

        assertNotNull(retrievedMusicFile, "Retrieved music file should not be null");
        assertEquals(musicFile.getFilePath(), retrievedMusicFile.getFilePath(), "File paths should match");
        assertEquals(musicFile.getTitle(), retrievedMusicFile.getTitle(), "Titles should match");
        assertEquals(musicFile.getArtist(), retrievedMusicFile.getArtist(), "Artists should match");
        assertEquals(musicFile.getAlbum(), retrievedMusicFile.getAlbum(), "Albums should match");
        assertEquals(musicFile.getGenre(), retrievedMusicFile.getGenre(), "Genres should match");
        assertEquals(musicFile.getTrackNumber(), retrievedMusicFile.getTrackNumber(), "Track numbers should match");
        assertEquals(musicFile.getYear(), retrievedMusicFile.getYear(), "Years should match");
        // Note: Duration, file size, bitrate, etc. come from actual audio file via TestDataFactory
        assertNotNull(retrievedMusicFile.getDurationSeconds(), "Duration should be extracted from audio file");
        assertNotNull(retrievedMusicFile.getFileSizeBytes(), "File size should be extracted from audio file");
        assertNotNull(retrievedMusicFile.getFileType(), "File type should be extracted from audio file");
    }

    /**
     * Test bulk operations with TestDataFactory generated datasets.
     */
    @Test
    public void testBulkOperations() throws IOException {
        // Generate a small test dataset for bulk operations
        TestDataSet testDataSet = TestDataFactory.createTestDataSet(
                TestDataSetSpec.builder()
                        .fileCount(10)
                        .randomizeMetadata(true)
                        .formatDistribution(
                                AudioFormat.MP3, 70,
                                AudioFormat.FLAC, 30
                        )
                        .build()
        );

        List<MusicFile> testFiles = testDataSet.getFiles();

        // Save all files
        for (MusicFile file : testFiles) {
            DatabaseManager.saveMusicFile(file);
        }

        // Verify all files were saved
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        assertTrue(allFiles.size() >= testFiles.size(), 
                "Database should contain at least " + testFiles.size() + " files");

        // Test search functionality
        if (!testFiles.isEmpty()) {
            MusicFile firstFile = testFiles.get(0);
            if (firstFile.getTitle() != null) {
                List<MusicFile> searchResults = DatabaseManager.searchMusicFiles(firstFile.getTitle());
                assertTrue(searchResults.size() > 0, "Search should find the saved file");
            }
        }
    }

    /**
     * Test database operations with edge cases using TestDataFactory.
     */
    @Test
    public void testDatabaseEdgeCases() throws IOException {
        // Test with Unicode characters
        MusicFile unicodeFile = new MusicFile();
        unicodeFile.setTitle("日本語のタイトル");
        unicodeFile.setArtist("アーティスト名");
        unicodeFile.setAlbum("アルバム名");
        unicodeFile.setGenre("J-Pop");
        unicodeFile.setFilePath("/test/path/unicode_test_" + System.currentTimeMillis() + ".mp3");
        unicodeFile.setFileType("mp3");

        // Should handle Unicode without issues
        assertDoesNotThrow(() -> {
            DatabaseManager.saveMusicFile(unicodeFile);
        }, "Database should handle Unicode characters");

        MusicFile retrieved = DatabaseManager.getMusicFileById(unicodeFile.getId());
        assertNotNull(retrieved, "Should retrieve Unicode file successfully");
        assertEquals(unicodeFile.getTitle(), retrieved.getTitle(), "Unicode title should be preserved");

        // Test with special characters
        MusicFile specialCharsFile = new MusicFile();
        specialCharsFile.setTitle("Test's \"Song\" & More");
        specialCharsFile.setArtist("Artist/Band");
        specialCharsFile.setAlbum("Album: The Collection");
        specialCharsFile.setFilePath("/test/path/special_chars_test_" + System.currentTimeMillis() + ".mp3");
        specialCharsFile.setFileType("mp3");

        assertDoesNotThrow(() -> {
            DatabaseManager.saveMusicFile(specialCharsFile);
        }, "Database should handle special characters");
    }

    /**
     * Test upsert functionality with TestDataFactory generated duplicates.
     */
    @Test
    public void testUpsertFunctionality() throws IOException {
        // Create a file with explicit metadata for database testing
        MusicFile originalFile = new MusicFile();
        originalFile.setTitle("Original Title");
        originalFile.setArtist("Original Artist");
        originalFile.setAlbum("Original Album");
        originalFile.setFilePath("/test/path/upsert_test_" + System.currentTimeMillis() + ".mp3");
        originalFile.setFileType("mp3");
        originalFile.setDurationSeconds(180);
        originalFile.setBitRate(320L);

        // Save the original file
        DatabaseManager.saveMusicFile(originalFile);
        String originalFilePath = originalFile.getFilePath();

        // Create another file with the same path but different metadata (simulating updated metadata)
        MusicFile updatedFile = new MusicFile();
        updatedFile.setTitle("Updated Title");
        updatedFile.setArtist("Updated Artist");
        updatedFile.setAlbum("Updated Album");
        updatedFile.setFilePath(originalFilePath); // Same path for upsert test
        updatedFile.setFileType("mp3");
        updatedFile.setDurationSeconds(185);
        updatedFile.setBitRate(256L);

        // Use saveOrUpdateMusicFile to test upsert functionality
        DatabaseManager.saveOrUpdateMusicFile(updatedFile);

        // Verify that the file was updated, not duplicated
        MusicFile retrievedFile = DatabaseManager.findByPath(originalFilePath);
        assertNotNull(retrievedFile, "Should find the file by path");
        assertEquals("Updated Title", retrievedFile.getTitle(), "Title should be updated");
        assertEquals("Updated Artist", retrievedFile.getArtist(), "Artist should be updated");
    }
}