package org.hasting.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MusicFileTestComprehensive {

    @TempDir
    Path tempDir;
    
    private MusicFile musicFile1;
    private MusicFile musicFile2;
    private MusicFile musicFile3;
    private List<MusicFile> testMusicFiles;

    @BeforeEach
    void setUp() {
        // Create test music files with different metadata
        musicFile1 = createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", "Rock", 1, 1975, 355, 320L, 44100);
        musicFile2 = createTestMusicFile("Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", "Rock", 4, 1971, 482, 256L, 44100);
        musicFile3 = createTestMusicFile("Hotel California", "Eagles", "Hotel California", "Rock", 1, 1976, 391, 320L, 44100);
        
        testMusicFiles = Arrays.asList(musicFile1, musicFile2, musicFile3);
    }

    @Test
    @Order(1)
    @DisplayName("Test MusicFile default constructor")
    void testDefaultConstructor() {
        MusicFile musicFile = new MusicFile();
        
        assertNotNull(musicFile.getDateAdded());
        assertNull(musicFile.getId());
        assertNull(musicFile.getTitle());
        assertNull(musicFile.getArtist());
        assertFalse(musicFile.isModified());
    }

    @Test
    @Order(2)
    @DisplayName("Test MusicFile constructor with actual file")
    void testFileConstructor() throws IOException {
        // Create a temporary MP3 file
        Path testFile = tempDir.resolve("test.mp3");
        Files.write(testFile, "fake mp3 content".getBytes());
        
        File file = testFile.toFile();
        MusicFile musicFile = new MusicFile(file);
        
        assertNotNull(musicFile.getFilePath());
        assertEquals(testFile.toString(), musicFile.getFilePath());
        assertEquals("mp3", musicFile.getFileType());
        assertNotNull(musicFile.getFileSizeBytes());
        assertTrue(musicFile.getFileSizeBytes() > 0);
    }

    @Test
    @Order(3)
    @DisplayName("Test getters and setters")
    void testGettersAndSetters() {
        MusicFile musicFile = new MusicFile();
        
        // Test string fields
        musicFile.setTitle("Test Title");
        assertEquals("Test Title", musicFile.getTitle());
        assertTrue(musicFile.isModified());
        
        musicFile.setArtist("Test Artist");
        assertEquals("Test Artist", musicFile.getArtist());
        
        musicFile.setAlbum("Test Album");
        assertEquals("Test Album", musicFile.getAlbum());
        
        musicFile.setGenre("Test Genre");
        assertEquals("Test Genre", musicFile.getGenre());
        
        musicFile.setFilePath("/test/path");
        assertEquals("/test/path", musicFile.getFilePath());
        
        // Test numeric fields
        musicFile.setTrackNumber(5);
        assertEquals(5, musicFile.getTrackNumber());
        
        musicFile.setYear(2023);
        assertEquals(2023, musicFile.getYear());
        
        musicFile.setDurationSeconds(240);
        assertEquals(240, musicFile.getDurationSeconds());
        
        musicFile.setBitRate(320L);
        assertEquals(320L, musicFile.getBitRate());
        
        musicFile.setSampleRate(44100);
        assertEquals(44100, musicFile.getSampleRate());
        
        musicFile.setFileSizeBytes(5000000L);
        assertEquals(5000000L, musicFile.getFileSizeBytes());
        
        // Test dates
        Date testDate = new Date();
        musicFile.setLastModified(testDate);
        assertEquals(testDate, musicFile.getLastModified());
        
        musicFile.setDateAdded(testDate);
        assertEquals(testDate, musicFile.getDateAdded());
        
        // Test ID
        musicFile.setId(123L);
        assertEquals(123L, musicFile.getId());
    }

    @Test
    @Order(4)
    @DisplayName("Test fuzzy comparator")
    void testFuzzyComparator() {
        MusicFile target = createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", "Rock", 1, 1975, 355, 320L, 44100);
        
        List<MusicFile> similarFiles = Arrays.asList(
            createTestMusicFile("Bohemian Rhapsody", "Queen", "Greatest Hits", "Rock", 1, 1975, 355, 256L, 44100),
            createTestMusicFile("Bohemian Rapsody", "Queen", "A Night at the Opera", "Rock", 1, 1975, 355, 320L, 44100), // Typo
            createTestMusicFile("Radio Ga Ga", "Queen", "The Works", "Rock", 2, 1984, 348, 320L, 44100)
        );
        
        similarFiles.sort(MusicFile.fuzzyComparator(target));
        
        // The exact match should be first, followed by the typo, then the different song
        assertEquals("Bohemian Rhapsody", similarFiles.get(0).getTitle());
        assertEquals("Greatest Hits", similarFiles.get(0).getAlbum());
    }

    @Test
    @Order(5)
    @DisplayName("Test exact match comparator")
    void testExactMatchComparator() {
        MusicFile target = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 1, 2023, 200, 320L, 44100);
        
        List<MusicFile> testFiles = Arrays.asList(
            createTestMusicFile("Test Song", "Different Artist", "Different Album", "Pop", 2, 2022, 180, 256L, 44100),
            createTestMusicFile("Different Song", "Test Artist", "Different Album", "Jazz", 3, 2021, 220, 320L, 44100),
            createTestMusicFile("Another Song", "Another Artist", "Another Album", "Classical", 4, 2020, 300, 192L, 44100)
        );
        
        // Test title matching
        testFiles.sort(MusicFile.exactMatchComparator(target, true, false));
        assertEquals("Test Song", testFiles.get(0).getTitle());
        
        // Test artist matching
        testFiles.sort(MusicFile.exactMatchComparator(target, false, true));
        assertEquals("Test Artist", testFiles.get(0).getArtist());
    }

    @Test
    @Order(6)
    @DisplayName("Test findMostSimilarFiles static method")
    void testFindMostSimilarFilesStatic() {
        MusicFile target = createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", "Rock", 1, 1975, 355, 320L, 44100);
        
        List<MusicFile> allFiles = new ArrayList<>(testMusicFiles);
        allFiles.add(createTestMusicFile("Bohemian Rhapsody", "Queen", "Greatest Hits", "Rock", 1, 1975, 355, 256L, 44100));
        
        List<MusicFile> similarFiles = MusicFile.findMostSimilarFiles(allFiles, target, 2);
        
        assertEquals(2, similarFiles.size());
        // Should not include the target itself
        assertFalse(similarFiles.contains(target));
    }

    @Test
    @Order(7)
    @DisplayName("Test findMostSimilarFiles instance methods")
    void testFindMostSimilarFilesInstance() {
        List<MusicFile> similarFiles = musicFile1.findMostSimilarFiles(testMusicFiles, 2);
        assertEquals(2, similarFiles.size());
        
        similarFiles = musicFile1.findMostSimilarFiles(testMusicFiles);
        assertTrue(similarFiles.size() <= 10); // Default limit
    }

    @Test
    @Order(8)
    @DisplayName("Test isItLikelyTheSameSong method")
    void testIsItLikelyTheSameSong() {
        // Test exact match
        MusicFile file1 = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 1, 2023, 200, 320L, 44100);
        MusicFile file2 = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 1, 2023, 200, 256L, 44100);
        
        assertTrue(file1.isItLikelyTheSameSong(file2));
        
        // Test with duration difference
        MusicFile file3 = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 1, 2023, 220, 256L, 44100);
        assertFalse(file1.isItLikelyTheSameSong(file3)); // Duration difference too large
        
        // Test with null values
        assertFalse(file1.isItLikelyTheSameSong(null));
        
        // Test with missing metadata
        MusicFile fileWithNulls = new MusicFile();
        assertFalse(file1.isItLikelyTheSameSong(fileWithNulls));
        
        // Test high bitrate exclusion
        MusicFile highBitrateFile = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 1, 2023, 200, 320L, 44100);
        highBitrateFile.setBitRate(500L); // High bitrate
        assertFalse(highBitrateFile.isItLikelyTheSameSong(file2));
    }

    @Test
    @Order(9)
    @DisplayName("Test deleteFile method")
    void testDeleteFile() throws IOException {
        // Create a temporary file
        Path testFile = tempDir.resolve("test_delete.mp3");
        Files.write(testFile, "test content".getBytes());
        
        MusicFile musicFile = new MusicFile();
        musicFile.setFilePath(testFile.toString());
        musicFile.setTitle("Test Song");
        musicFile.setArtist("Test Artist");
        musicFile.setFileType("mp3");
        // Don't set ID (simulate file not in database)
        
        boolean deleted = musicFile.deleteFile();
        
        assertTrue(deleted);
        assertEquals("****deleted****", musicFile.getTitle());
        assertEquals("****deleted****", musicFile.getArtist());
        assertFalse(Files.exists(testFile));
    }

    @Test
    @Order(10)
    @DisplayName("Test deleteFile method with ID set")
    void testDeleteFileWithId() throws IOException {
        Path testFile = tempDir.resolve("test_delete_with_id.mp3");
        Files.write(testFile, "test content".getBytes());
        
        MusicFile musicFile = new MusicFile();
        musicFile.setFilePath(testFile.toString());
        musicFile.setId(123L); // Simulate file in database
        
        boolean deleted = musicFile.deleteFile();
        
        assertFalse(deleted); // Should not delete when ID is set
        assertTrue(Files.exists(testFile)); // File should still exist
    }

    @Test
    @Order(11)
    @DisplayName("Test newFileNameAndLocation method")
    void testNewFileNameAndLocation() {
        MusicFile musicFile = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 5, 2023, 200, 320L, 44100);
        musicFile.setFileType("mp3");
        
        String newPath = musicFile.newFileNameAndLocation("/music");
        
        assertNotNull(newPath);
        assertTrue(newPath.startsWith("/music/"));
        assertTrue(newPath.contains("Test_Artist"));
        assertTrue(newPath.contains("Test_Album"));
        assertTrue(newPath.contains("05-Test_Song.mp3"));
    }

    @Test
    @Order(12)
    @DisplayName("Test copyToNewLocation method")
    void testCopyToNewLocation() throws IOException {
        // Create source file
        Path sourceFile = tempDir.resolve("source.mp3");
        Files.write(sourceFile, "test content".getBytes());
        
        MusicFile musicFile = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 1, 2023, 200, 320L, 44100);
        musicFile.setFilePath(sourceFile.toString());
        musicFile.setFileType("mp3");
        
        Path destinationDir = tempDir.resolve("organized");
        Files.createDirectories(destinationDir);
        
        // This should work without throwing an exception
        assertDoesNotThrow(() -> musicFile.copyToNewLocation(destinationDir.toString()));
    }

    @Test
    @Order(13)
    @DisplayName("Test toString method")
    void testToString() {
        // Test with track number
        MusicFile musicFile = createTestMusicFile("Test Song", "Test Artist", "Test Album", "Rock", 5, 2023, 200, 320L, 44100);
        String result = musicFile.toString();
        assertTrue(result.contains("Test Artist"));
        assertTrue(result.contains("Test Song"));
        assertTrue(result.contains("Test Album"));
        assertTrue(result.contains("5"));
        
        // Test without track number
        musicFile.setTrackNumber(null);
        result = musicFile.toString();
        assertTrue(result.contains("Test Artist"));
        assertTrue(result.contains("Test Song"));
        assertTrue(result.contains("Test Album"));
        assertFalse(result.contains(" - 5"));
        
        // Test without album
        musicFile.setAlbum(null);
        result = musicFile.toString();
        assertTrue(result.contains("Test Artist"));
        assertTrue(result.contains("Test Song"));
        assertFalse(result.contains("Test Album"));
    }

    @Test
    @Order(14)
    @DisplayName("Test edge cases and null handling")
    void testEdgeCasesAndNullHandling() {
        MusicFile musicFile = new MusicFile();
        
        // Test null-safe operations
        assertDoesNotThrow(() -> musicFile.toString());
        assertDoesNotThrow(() -> musicFile.newFileNameAndLocation("/test"));
        
        // Test with empty strings
        musicFile.setTitle("");
        musicFile.setArtist("");
        musicFile.setAlbum("");
        
        assertDoesNotThrow(() -> musicFile.toString());
        assertDoesNotThrow(() -> musicFile.newFileNameAndLocation("/test"));
    }

    @Test
    @Order(15)
    @DisplayName("Test modification tracking")
    void testModificationTracking() {
        MusicFile musicFile = new MusicFile();
        assertFalse(musicFile.isModified());
        
        musicFile.setTitle("Test");
        assertTrue(musicFile.isModified());
        
        musicFile.setModified(false);
        assertFalse(musicFile.isModified());
        
        musicFile.setArtist("Test Artist");
        assertTrue(musicFile.isModified());
    }

    // Helper method to create test music files
    private MusicFile createTestMusicFile(String title, String artist, String album, String genre, 
                                         Integer trackNumber, Integer year, Integer duration, 
                                         Long bitRate, Integer sampleRate) {
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
        musicFile.setFilePath("/test/path/" + title.replaceAll(" ", "_") + ".mp3");
        musicFile.setFileSizeBytes(5000000L);
        musicFile.setLastModified(new Date());
        musicFile.setModified(false); // Reset modification flag after setup
        return musicFile;
    }
}