package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FuzzyMatcher functionality.
 */
public class FuzzyMatcherTest {
    
    private FuzzySearchConfig config;
    private MusicFile file1;
    private MusicFile file2;
    private MusicFile file3;
    
    @BeforeEach
    void setUp() {
        config = new FuzzySearchConfig();
        
        // Create test music files
        file1 = new MusicFile();
        file1.setTitle("Hotel California");
        file1.setArtist("Eagles");
        file1.setAlbum("Hotel California");
        file1.setDurationSeconds(391);
        file1.setTrackNumber(1);
        
        file2 = new MusicFile();
        file2.setTitle("Hotel California (Remastered)");
        file2.setArtist("The Eagles");
        file2.setAlbum("Hotel California - Deluxe Edition");
        file2.setDurationSeconds(390);
        file2.setTrackNumber(1);
        
        file3 = new MusicFile();
        file3.setTitle("Bohemian Rhapsody");
        file3.setArtist("Queen");
        file3.setAlbum("A Night at the Opera");
        file3.setDurationSeconds(355);
        file3.setTrackNumber(11);
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
    void testDurationTolerance() {
        // Test duration matching within tolerance
        config.setDurationToleranceSeconds(5);
        config.setMinimumFieldsToMatch(4); // Require all fields including duration
        
        MusicFile shortFile = new MusicFile();
        shortFile.setTitle("Test Song");
        shortFile.setArtist("Test Artist");
        shortFile.setAlbum("Test Album");
        shortFile.setDurationSeconds(180);
        
        MusicFile longFile = new MusicFile();
        longFile.setTitle("Test Song");
        longFile.setArtist("Test Artist");
        longFile.setAlbum("Test Album");
        longFile.setDurationSeconds(183); // 3 seconds difference
        
        assertTrue(FuzzyMatcher.areDuplicates(shortFile, longFile, config),
                "Files with similar duration should be duplicates");
        
        // Test beyond tolerance - duration fails, so need other fields to fail too for minimum fields requirement
        longFile.setDurationSeconds(190); // 10 seconds difference - fails duration check
        longFile.setTitle("Different Song");  // Make title different too - fails title check
        // Artist and album still match, but only 2/4 fields match now
        assertFalse(FuzzyMatcher.areDuplicates(shortFile, longFile, config),
                "Files with only 2/4 matching fields should not be duplicates");
    }
}