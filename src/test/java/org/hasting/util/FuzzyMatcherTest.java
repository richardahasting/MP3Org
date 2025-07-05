package org.hasting.util;

import org.hasting.model.MusicFile;
import org.hasting.test.TestDataFactory;
import org.hasting.test.spec.DuplicateSpec;
import org.hasting.test.spec.TestFileSpec;
import org.hasting.test.spec.AudioFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FuzzyMatcher functionality.
 * Updated to use TestDataFactory for more realistic and comprehensive test scenarios.
 */
public class FuzzyMatcherTest {
    
    private FuzzySearchConfig config;
    private MusicFile file1;
    private MusicFile file2;
    private MusicFile file3;
    private List<MusicFile> duplicateSet;
    
    @BeforeEach
    void setUp() throws IOException {
        config = new FuzzySearchConfig();
        
        // Create test files with explicit metadata for fuzzy matching tests
        // Using direct MusicFile creation for predictable duplicate detection results
        file1 = new MusicFile();
        file1.setTitle("Hotel California");
        file1.setArtist("Eagles");
        file1.setAlbum("Hotel California");
        file1.setFilePath("/test/path/hotel_california_1.mp3");
        file1.setFileType("mp3");
        file1.setDurationSeconds(391); // 6:31
        
        file2 = new MusicFile();
        file2.setTitle("Hotel California (Remastered)");
        file2.setArtist("Eagles ft. Someone");
        file2.setAlbum("Hotel California");
        file2.setFilePath("/test/path/hotel_california_2.mp3");
        file2.setFileType("mp3");
        file2.setDurationSeconds(395); // 6:35, close enough for duplicate detection
        
        // Create a completely different file for non-duplicate tests
        file3 = new MusicFile();
        file3.setTitle("Bohemian Rhapsody");
        file3.setArtist("Queen");
        file3.setAlbum("A Night at the Opera");
        file3.setTrackNumber(11);
        file3.setFilePath("/test/path/bohemian_rhapsody.mp3");
        file3.setFileType("mp3");
        file3.setDurationSeconds(355); // 5:55
        
        // Create a duplicate set for comprehensive testing
        duplicateSet = Arrays.asList(file1, file2);
    }

    @AfterEach
    void tearDown() {
        // Clean up generated test files
        TestDataFactory.cleanupGeneratedFiles();
    }
    
    @Test
    void testJaroWinklerSimilarity() {
        // Test exact match
        assertEquals(1.0, FuzzyMatcher.jaroWinklerSimilarity("test", "test"), 0.001);
        
        // Test similar strings
        double similarity = FuzzyMatcher.jaroWinklerSimilarity("Hotel California", "Hotel California (Remastered)");
        assertTrue(similarity > 0.8, "Similar titles should have high similarity: " + similarity);
        
        // Test different strings
        double dissimilarity = FuzzyMatcher.jaroWinklerSimilarity("Hotel California", "Bohemian Rhapsody");
        assertTrue(dissimilarity < 0.6, "Different titles should have low similarity: " + dissimilarity);
    }
    
    @Test
    void testLevenshteinDistance() {
        // Test exact match
        assertEquals(0, FuzzyMatcher.levenshteinDistance("test", "test"));
        
        // Test single character difference
        assertEquals(1, FuzzyMatcher.levenshteinDistance("test", "best"));
        
        // Test longer strings
        int distance = FuzzyMatcher.levenshteinDistance("Hotel California", "Hotel California (Remastered)");
        assertTrue(distance > 0 && distance < 20, "Expected reasonable edit distance: " + distance);
    }
    
    @Test
    void testAreDuplicates() {
        // These should be considered duplicates with default config
        assertTrue(FuzzyMatcher.areDuplicates(file1, file2, config),
                "Similar files should be detected as duplicates");
        
        // These should not be duplicates
        assertFalse(FuzzyMatcher.areDuplicates(file1, file3, config),
                "Different files should not be detected as duplicates");
    }
    
    @Test
    void testFindFuzzyDuplicates() {
        List<MusicFile> files = Arrays.asList(file1, file2, file3);
        List<MusicFile> duplicates = FuzzyMatcher.findFuzzyDuplicates(files, config);
        
        // Should find file1 and file2 as duplicates
        assertEquals(2, duplicates.size(), "Should find exactly 2 duplicate files");
        assertTrue(duplicates.contains(file1), "Should include first duplicate");
        assertTrue(duplicates.contains(file2), "Should include second duplicate");
        assertFalse(duplicates.contains(file3), "Should not include non-duplicate");
    }
    
    @Test
    void testGroupDuplicates() {
        List<MusicFile> files = Arrays.asList(file1, file2, file3);
        List<List<MusicFile>> groups = FuzzyMatcher.groupDuplicates(files, config);
        
        // Should find one group with 2 files
        assertEquals(1, groups.size(), "Should find exactly 1 duplicate group");
        assertEquals(2, groups.get(0).size(), "Group should contain exactly 2 files");
    }
    
    @Test
    void testSimilarityBreakdown() {
        String breakdown = FuzzyMatcher.getSimilarityBreakdown(file1, file2, config);
        
        assertNotNull(breakdown, "Breakdown should not be null");
        assertTrue(breakdown.contains("Title:"), "Should include title similarity");
        assertTrue(breakdown.contains("Artist:"), "Should include artist similarity");
        assertTrue(breakdown.contains("Album:"), "Should include album similarity");
        assertTrue(breakdown.contains("Duration:"), "Should include duration check");
        assertTrue(breakdown.contains("Track:"), "Should include track check");
    }
    
    @Test
    void testConfigurableThresholds() {
        // Create stricter config - title similarity is 91.9%, so requiring 95% threshold should fail
        FuzzySearchConfig strictConfig = new FuzzySearchConfig();
        strictConfig.setTitleSimilarityThreshold(95.0);  // Title has 91.9%, so this should fail
        strictConfig.setArtistSimilarityThreshold(98.0);
        strictConfig.setAlbumSimilarityThreshold(98.0);
        strictConfig.setMinimumFieldsToMatch(4);  // Require title, artist, album, AND duration to match
        
        // Should not be duplicates with strict config
        assertFalse(FuzzyMatcher.areDuplicates(file1, file2, strictConfig),
                "Files should not be duplicates with strict thresholds");
        
        // Create lenient config
        FuzzySearchConfig lenientConfig = new FuzzySearchConfig();
        lenientConfig.setTitleSimilarityThreshold(50.0);
        lenientConfig.setArtistSimilarityThreshold(50.0);
        lenientConfig.setAlbumSimilarityThreshold(50.0);
        lenientConfig.setMinimumFieldsToMatch(1);
        
        // Should be duplicates with lenient config
        assertTrue(FuzzyMatcher.areDuplicates(file1, file2, lenientConfig),
                "Files should be duplicates with lenient thresholds");
    }
    
    @Test
    void testDurationTolerance() throws IOException {
        // Test duration matching within tolerance using explicit duration values
        config.setDurationToleranceSeconds(5);
        config.setMinimumFieldsToMatch(4); // Require all fields including duration
        
        MusicFile shortFile = new MusicFile();
        shortFile.setTitle("Test Song");
        shortFile.setArtist("Test Artist");
        shortFile.setAlbum("Test Album");
        shortFile.setDurationSeconds(180); // 3:00
        shortFile.setFilePath("/test/path/short.mp3");
        shortFile.setFileType("mp3");
        
        MusicFile longFile = new MusicFile();
        longFile.setTitle("Test Song");
        longFile.setArtist("Test Artist");
        longFile.setAlbum("Test Album");
        longFile.setDurationSeconds(183); // 3:03 - within 5 second tolerance
        longFile.setFilePath("/test/path/long.mp3");
        longFile.setFileType("mp3");
        
        assertTrue(FuzzyMatcher.areDuplicates(shortFile, longFile, config),
                "Files with identical metadata and similar duration should be duplicates");
        
        // Create a file with different title to test minimum fields requirement
        MusicFile differentFile = new MusicFile();
        differentFile.setTitle("Different Song");
        differentFile.setArtist("Test Artist");
        differentFile.setAlbum("Test Album");
        differentFile.setDurationSeconds(181); // Similar duration but different title
        differentFile.setFilePath("/test/path/different.mp3");
        differentFile.setFileType("mp3");
        
        assertFalse(FuzzyMatcher.areDuplicates(shortFile, differentFile, config),
                "Files with different titles should not meet minimum fields requirement");
    }

    /**
     * Test comprehensive duplicate detection using TestDataFactory generated duplicates.
     */
    @Test
    void testComprehensiveDuplicateDetection() {
        // Test with the complete duplicate set plus a non-duplicate file
        List<MusicFile> allTestFiles = Arrays.asList(file1, file2, file3);
        
        List<List<MusicFile>> groups = FuzzyMatcher.groupDuplicates(allTestFiles, config);
        
        // Should find one group with 2 duplicates (file1 and file2) and file3 should be separate
        assertEquals(1, groups.size(), "Should find exactly 1 duplicate group");
        assertEquals(2, groups.get(0).size(), "Group should contain exactly 2 duplicate files");
        
        // Verify that file3 (Queen song) is not in the duplicate group
        List<MusicFile> duplicateGroup = groups.get(0);
        assertFalse(duplicateGroup.contains(file3), "Non-duplicate file should not be in duplicate group");
        assertTrue(duplicateGroup.contains(file1), "Should include first duplicate");
        assertTrue(duplicateGroup.contains(file2), "Should include second duplicate");
    }

    /**
     * Test edge cases using TestDataFactory edge case generation.
     */
    @Test
    void testEdgeCaseHandling() throws IOException {
        // Create files with special characters and Unicode
        MusicFile unicodeFile = new MusicFile();
        unicodeFile.setTitle("日本語タイトル");
        unicodeFile.setArtist("Björk");
        unicodeFile.setAlbum("Ñoño");
        unicodeFile.setFilePath("/test/path/unicode.mp3");
        unicodeFile.setFileType("mp3");
        
        MusicFile specialCharsFile = new MusicFile();
        specialCharsFile.setTitle("Test / Song \\ With : Special * Characters");
        specialCharsFile.setArtist("Artist & Band | Feat. Someone");
        specialCharsFile.setFilePath("/test/path/special_chars.mp3");
        specialCharsFile.setFileType("mp3");
        
        // These should not crash the fuzzy matching
        assertDoesNotThrow(() -> {
            FuzzyMatcher.areDuplicates(unicodeFile, specialCharsFile, config);
        }, "Unicode and special characters should not cause exceptions");
        
        assertDoesNotThrow(() -> {
            FuzzyMatcher.getSimilarityBreakdown(unicodeFile, specialCharsFile, config);
        }, "Similarity breakdown should handle special characters gracefully");
    }

    /**
     * Test multiple format support using TestDataFactory.
     */
    @Test
    void testMultipleFormatSupport() throws IOException {
        // Create identical metadata across different formats
        MusicFile mp3File = new MusicFile();
        mp3File.setTitle("Format Test Song");
        mp3File.setArtist("Format Test Artist");
        mp3File.setAlbum("Format Test Album");
        mp3File.setFilePath("/test/path/format_test.mp3");
        mp3File.setFileType("mp3");
        
        assertNotNull(mp3File.getTitle(), "MP3 file should have metadata");
        assertNotNull(mp3File.getArtist(), "MP3 file should have artist");
        
        // Test that format differences don't affect duplicate detection when metadata matches
        MusicFile flacFile = new MusicFile();
        flacFile.setTitle("Format Test Song");
        flacFile.setArtist("Format Test Artist");
        flacFile.setAlbum("Format Test Album");
        flacFile.setFilePath("/test/path/format_test.flac");
        flacFile.setFileType("flac");
        
        assertTrue(FuzzyMatcher.areDuplicates(mp3File, flacFile, config),
                "Files with identical metadata should be duplicates regardless of format");
    }
}