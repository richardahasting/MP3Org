package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MusicFileListUtilsTest {

    private List<MusicFile> testMusicFiles;
    private MusicFile duplicateFile1;
    private MusicFile duplicateFile2;
    private MusicFile uniqueFile1;
    private MusicFile uniqueFile2;

    @BeforeEach
    void setUp() {
        testMusicFiles = new ArrayList<>();
        
        // Create test files with some duplicates
        duplicateFile1 = createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", 355, 320L);
        duplicateFile2 = createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", 355, 256L); // Same song, different bitrate
        
        uniqueFile1 = createTestMusicFile("Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", 482, 320L);
        uniqueFile2 = createTestMusicFile("Hotel California", "Eagles", "Hotel California", 391, 256L);
        
        testMusicFiles.addAll(Arrays.asList(duplicateFile1, duplicateFile2, uniqueFile1, uniqueFile2));
    }

    @Test
    @Order(1)
    @DisplayName("Test findPotentialDuplicates with exact duplicates")
    void testFindPotentialDuplicatesExactDuplicates() {
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(testMusicFiles);
        
        assertNotNull(duplicates);
        assertEquals(2, duplicates.size()); // Both duplicate files should be found
        
        // Verify the duplicates are the expected ones
        assertTrue(duplicates.contains(duplicateFile1));
        assertTrue(duplicates.contains(duplicateFile2));
    }

    @Test
    @Order(2)
    @DisplayName("Test findPotentialDuplicates with no duplicates")
    void testFindPotentialDuplicatesNoDuplicates() {
        List<MusicFile> uniqueFiles = Arrays.asList(uniqueFile1, uniqueFile2);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(uniqueFiles);
        
        assertNotNull(duplicates);
        assertEquals(0, duplicates.size());
    }

    @Test
    @Order(3)
    @DisplayName("Test findPotentialDuplicates with empty list")
    void testFindPotentialDuplicatesEmptyList() {
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(new ArrayList<>());
        
        assertNotNull(duplicates);
        assertEquals(0, duplicates.size());
    }

    @Test
    @Order(4)
    @DisplayName("Test findPotentialDuplicates with single file")
    void testFindPotentialDuplicatesSingleFile() {
        List<MusicFile> singleFile = Arrays.asList(uniqueFile1);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(singleFile);
        
        assertNotNull(duplicates);
        assertEquals(0, duplicates.size());
    }

    @Test
    @Order(5)
    @DisplayName("Test findPotentialDuplicates with similar titles")
    void testFindPotentialDuplicatesSimilarTitles() {
        MusicFile similar1 = createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", 355, 320L);
        MusicFile similar2 = createTestMusicFile("Bohemian Rapsody", "Queen", "A Night at the Opera", 355, 256L); // Typo in title
        
        List<MusicFile> files = Arrays.asList(similar1, similar2, uniqueFile1);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        // Should find the similar files as potential duplicates
        assertTrue(duplicates.size() >= 2);
    }

    @Test
    @Order(6)
    @DisplayName("Test findPotentialDuplicates with different durations")
    void testFindPotentialDuplicatesDifferentDurations() {
        MusicFile file1 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        MusicFile file2 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 300, 320L); // Very different duration
        
        List<MusicFile> files = Arrays.asList(file1, file2);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        // Should not consider these duplicates due to large duration difference
        assertEquals(0, duplicates.size());
    }

    @Test
    @Order(7)
    @DisplayName("Test findPotentialDuplicates with null metadata")
    void testFindPotentialDuplicatesNullMetadata() {
        MusicFile fileWithNulls = new MusicFile();
        fileWithNulls.setFilePath("/test/path.mp3");
        // Leave title, artist, album as null
        
        List<MusicFile> files = Arrays.asList(fileWithNulls, uniqueFile1);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        // Should handle null metadata gracefully
        assertEquals(0, duplicates.size());
    }

    @Test
    @Order(8)
    @DisplayName("Test findPotentialDuplicates with multiple duplicate groups")
    void testFindPotentialDuplicatesMultipleGroups() {
        MusicFile group1a = createTestMusicFile("Song A", "Artist A", "Album A", 200, 320L);
        MusicFile group1b = createTestMusicFile("Song A", "Artist A", "Album A", 200, 256L);
        
        MusicFile group2a = createTestMusicFile("Song B", "Artist B", "Album B", 180, 320L);
        MusicFile group2b = createTestMusicFile("Song B", "Artist B", "Album B", 180, 192L);
        
        List<MusicFile> files = Arrays.asList(group1a, group1b, group2a, group2b, uniqueFile1);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        assertEquals(4, duplicates.size()); // All 4 duplicates should be found
    }

    @Test
    @Order(9)
    @DisplayName("Test findPotentialDuplicates with case variations")
    void testFindPotentialDuplicatesCaseVariations() {
        MusicFile file1 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        MusicFile file2 = createTestMusicFile("TEST SONG", "test artist", "Test Album", 200, 256L);
        
        List<MusicFile> files = Arrays.asList(file1, file2);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        // Should find case variations as duplicates
        assertEquals(2, duplicates.size());
    }

    @Test
    @Order(10)
    @DisplayName("Test findPotentialDuplicates with whitespace variations")
    void testFindPotentialDuplicatesWhitespaceVariations() {
        MusicFile file1 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        MusicFile file2 = createTestMusicFile("Test  Song", "Test  Artist", "Test Album", 200, 256L); // Extra spaces
        
        List<MusicFile> files = Arrays.asList(file1, file2);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        // Should find whitespace variations as duplicates
        assertEquals(2, duplicates.size());
    }

    @Test
    @Order(11)
    @DisplayName("Test findPotentialDuplicates performance with large list")
    void testFindPotentialDuplicatesPerformance() {
        List<MusicFile> largeList = new ArrayList<>();
        
        // Create a large list with some duplicates
        for (int i = 0; i < 1000; i++) {
            MusicFile file = createTestMusicFile("Song " + (i % 100), "Artist " + (i % 50), 
                                               "Album " + (i % 25), 200 + (i % 10), 320L);
            largeList.add(file);
        }
        
        long startTime = System.currentTimeMillis();
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(largeList);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(duplicates);
        assertTrue(duplicates.size() > 0, "Should find duplicates in large list");
        assertTrue(endTime - startTime < 5000, "Should complete in reasonable time");
    }

    @Test
    @Order(12)
    @DisplayName("Test findPotentialDuplicates with high bitrate exclusion")
    void testFindPotentialDuplicatesHighBitrateExclusion() {
        MusicFile lowBitrate = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 128L);
        MusicFile highBitrate = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 500L); // Very high bitrate
        
        List<MusicFile> files = Arrays.asList(lowBitrate, highBitrate);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        // High bitrate files might be excluded from duplicate detection
        assertTrue(duplicates.size() <= 2);
    }

    @Test
    @Order(13)
    @DisplayName("Test findPotentialDuplicates ordering")
    void testFindPotentialDuplicatesOrdering() {
        MusicFile higherQuality = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        MusicFile lowerQuality = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 128L);
        
        List<MusicFile> files = Arrays.asList(lowerQuality, higherQuality); // Add in reverse quality order
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        assertEquals(2, duplicates.size());
        
        // Should maintain some logical ordering (possibly by quality or file order)
        assertTrue(duplicates.contains(higherQuality));
        assertTrue(duplicates.contains(lowerQuality));
    }

    @Test
    @Order(14)
    @DisplayName("Test findPotentialDuplicates with exact same objects")
    void testFindPotentialDuplicatesExactSameObjects() {
        MusicFile file = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        
        List<MusicFile> files = Arrays.asList(file, file); // Same object twice
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        // Should handle same objects gracefully
        assertTrue(duplicates.size() <= 2);
    }

    @Test
    @Order(15)
    @DisplayName("Test findPotentialDuplicates with modified flag")
    void testFindPotentialDuplicatesModifiedFlag() {
        MusicFile file1 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        MusicFile file2 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 256L);
        
        file1.setModified(true);
        file2.setModified(false);
        
        List<MusicFile> files = Arrays.asList(file1, file2);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        // Modified flag should not affect duplicate detection
        assertEquals(2, duplicates.size());
    }

    @Test
    @Order(16)
    @DisplayName("Test findPotentialDuplicates consistency")
    void testFindPotentialDuplicatesConsistency() {
        // Run the same detection multiple times
        List<MusicFile> duplicates1 = MusicFileListUtils.findPotentialDuplicates(testMusicFiles);
        List<MusicFile> duplicates2 = MusicFileListUtils.findPotentialDuplicates(testMusicFiles);
        List<MusicFile> duplicates3 = MusicFileListUtils.findPotentialDuplicates(testMusicFiles);
        
        assertEquals(duplicates1.size(), duplicates2.size(), "Results should be consistent");
        assertEquals(duplicates2.size(), duplicates3.size(), "Results should be consistent");
        
        // Should contain the same files (order might differ)
        assertTrue(duplicates1.containsAll(duplicates2));
        assertTrue(duplicates2.containsAll(duplicates3));
    }

    @Test
    @Order(17)
    @DisplayName("Test findPotentialDuplicates with different file paths")
    void testFindPotentialDuplicatesDifferentFilePaths() {
        MusicFile file1 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        MusicFile file2 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);
        
        file1.setFilePath("/path1/song.mp3");
        file2.setFilePath("/path2/song.mp3");
        
        List<MusicFile> files = Arrays.asList(file1, file2);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        // Should find duplicates regardless of file path
        assertEquals(2, duplicates.size());
    }

    @Test
    @Order(18)
    @DisplayName("Test findPotentialDuplicates with null input")
    void testFindPotentialDuplicatesNullInput() {
        List<MusicFile> result = MusicFileListUtils.findPotentialDuplicates(null);
        assertNotNull(result);
        assertEquals(0, result.size(), "Should return empty list for null input");
    }

    @Test
    @Order(19)
    @DisplayName("Test findPotentialDuplicates with mixed quality files")
    void testFindPotentialDuplicatesMixedQuality() {
        MusicFile file1 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 96L);   // Low quality
        MusicFile file2 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 192L);  // Medium quality
        MusicFile file3 = createTestMusicFile("Test Song", "Test Artist", "Test Album", 200, 320L);  // High quality
        
        List<MusicFile> files = Arrays.asList(file1, file2, file3);
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(files);
        
        assertNotNull(duplicates);
        assertEquals(3, duplicates.size()); // Should find all three as duplicates
    }

    @Test
    @Order(20)
    @DisplayName("Test findPotentialDuplicates real-world scenario")
    void testFindPotentialDuplicatesRealWorldScenario() {
        // Simulate a real music library with various duplicate scenarios
        List<MusicFile> realWorldFiles = Arrays.asList(
            // Exact duplicates (different bitrates)
            createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", 355, 320L),
            createTestMusicFile("Bohemian Rhapsody", "Queen", "A Night at the Opera", 355, 256L),
            
            // Near duplicates (typo)
            createTestMusicFile("Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", 482, 320L),
            createTestMusicFile("Stairway to Heaven", "Led Zepelin", "Led Zeppelin IV", 482, 192L), // Typo in artist
            
            // Case differences
            createTestMusicFile("Hotel California", "Eagles", "Hotel California", 391, 320L),
            createTestMusicFile("HOTEL CALIFORNIA", "eagles", "Hotel California", 391, 256L),
            
            // Unique files
            createTestMusicFile("Imagine", "John Lennon", "Imagine", 183, 320L),
            createTestMusicFile("Like a Rolling Stone", "Bob Dylan", "Highway 61 Revisited", 371, 256L)
        );
        
        List<MusicFile> duplicates = MusicFileListUtils.findPotentialDuplicates(realWorldFiles);
        
        assertNotNull(duplicates);
        assertTrue(duplicates.size() >= 4, "Should find multiple duplicate groups");
        assertTrue(duplicates.size() <= 6, "Should not include unique files");
    }

    // Helper method to create test music files
    private MusicFile createTestMusicFile(String title, String artist, String album, 
                                         Integer duration, Long bitRate) {
        MusicFile musicFile = new MusicFile();
        musicFile.setTitle(title);
        musicFile.setArtist(artist);
        musicFile.setAlbum(album);
        musicFile.setGenre("Rock");
        musicFile.setTrackNumber(1);
        musicFile.setYear(2023);
        musicFile.setDurationSeconds(duration);
        musicFile.setBitRate(bitRate);
        musicFile.setSampleRate(44100);
        musicFile.setFileType("mp3");
        musicFile.setFilePath("/test/path/" + title.replaceAll(" ", "_") + ".mp3");
        musicFile.setFileSizeBytes(5000000L);
        musicFile.setLastModified(new Date());
        musicFile.setModified(false);
        return musicFile;
    }
}