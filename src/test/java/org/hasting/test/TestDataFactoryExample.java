package org.hasting.test;

import org.hasting.model.MusicFile;
import org.hasting.test.spec.*;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

import java.io.IOException;
import java.util.List;

/**
 * Example usage of TestDataFactory for generating test data.
 * This class demonstrates various use cases for the factory.
 * 
 * @since 1.0
 */
public class TestDataFactoryExample {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(TestDataFactoryExample.class);
    
    /**
     * Example 1: Generate duplicate test set
     */
    public static void testDuplicateGeneration() throws IOException {
        logger.info("=== Duplicate Generation Example ===");
        
        // Generate variations of the same song
        DuplicateSpec spec = DuplicateSpec.builder()
                .baseTitle("Yesterday")
                .baseArtist("The Beatles")
                .baseAlbum("Help!")
                .addVariation(DuplicateSpec.DuplicateVariation.TITLE_TYPO)
                .addVariation(DuplicateSpec.DuplicateVariation.ARTIST_FEATURING)
                .addVariation(DuplicateSpec.DuplicateVariation.BITRATE_DIFFERENT)
                .addVariation(DuplicateSpec.DuplicateVariation.CASE_DIFFERENT)
                .build();
        
        List<MusicFile> duplicates = TestDataFactory.createDuplicateSet(spec);
        
        logger.info("Generated {} duplicate files:", duplicates.size());
        for (MusicFile file : duplicates) {
            logger.info("  - {} by {} ({})", 
                    file.getTitle(), 
                    file.getArtist(), 
                    file.getFilePath());
        }
    }
    
    /**
     * Example 2: Generate edge case files
     */
    public static void testEdgeCaseGeneration() throws IOException {
        logger.info("=== Edge Case Generation Example ===");
        
        EdgeCaseSpec spec = EdgeCaseSpec.builder()
                .addType(EdgeCaseSpec.EdgeCaseType.UNICODE_TITLE)
                .addType(EdgeCaseSpec.EdgeCaseType.UNICODE_ARTIST)
                .addType(EdgeCaseSpec.EdgeCaseType.LONG_STRINGS)
                .addType(EdgeCaseSpec.EdgeCaseType.SPECIAL_CHARACTERS)
                .preferredFormat(AudioFormat.MP3)
                .build();
        
        List<MusicFile> edgeCases = TestDataFactory.createEdgeCaseSet(spec);
        
        logger.info("Generated {} edge case files:", edgeCases.size());
        for (MusicFile file : edgeCases) {
            logger.info("  - Title: '{}', Artist: '{}'", 
                    file.getTitle(), 
                    file.getArtist());
        }
    }
    
    /**
     * Example 3: Generate format test set
     */
    public static void testFormatGeneration() throws IOException {
        logger.info("=== Format Test Generation Example ===");
        
        FormatSpec spec = FormatSpec.builder()
                .baseTitle("Format Test Song")
                .baseArtist("Test Artist")
                .baseAlbum("Format Test Album")
                .addAllFormats()
                .build();
        
        List<MusicFile> formatFiles = TestDataFactory.createFormatTestSet(spec);
        
        logger.info("Generated {} format test files:", formatFiles.size());
        for (MusicFile file : formatFiles) {
            String extension = file.getFilePath().substring(file.getFilePath().lastIndexOf('.') + 1);
            logger.info("  - {} format: {}", extension.toUpperCase(), file.getFilePath());
        }
    }
    
    /**
     * Example 4: Generate custom single file
     */
    public static void testCustomFileGeneration() throws IOException {
        logger.info("=== Custom File Generation Example ===");
        
        TestFileSpec spec = TestFileSpec.builder()
                .title("日本語タイトル")
                .artist("Björk")
                .album("Homogenic")
                .genre("Electronic")
                .trackNumber(5)
                .year(1997)
                .format(AudioFormat.MP3)
                .build();
        
        MusicFile customFile = TestDataFactory.createCustomFile(spec);
        
        logger.info("Generated custom file:");
        logger.info("  Title: {}", customFile.getTitle());
        logger.info("  Artist: {}", customFile.getArtist());
        logger.info("  Album: {}", customFile.getAlbum());
        logger.info("  Path: {}", customFile.getFilePath());
    }
    
    /**
     * Example 5: Generate large test data set
     */
    public static void testLargeDataSetGeneration() throws IOException {
        logger.info("=== Large Data Set Generation Example ===");
        
        TestDataSetSpec spec = TestDataSetSpec.builder()
                .fileCount(100)
                .randomizeMetadata(true)
                .formatDistribution(
                        AudioFormat.MP3, 70,    // 70% MP3
                        AudioFormat.FLAC, 20,   // 20% FLAC
                        AudioFormat.WAV, 5,     // 5% WAV
                        AudioFormat.OGG, 5      // 5% OGG
                )
                .includeDuplicates(true, 10) // 10% duplicates
                .build();
        
        TestDataSet dataSet = TestDataFactory.createTestDataSet(spec);
        
        logger.info("Generated test data set:");
        logger.info(dataSet.getStatistics());
    }
    
    /**
     * Main method to run all examples.
     */
    public static void main(String[] args) {
        try {
            // Run all examples
            testDuplicateGeneration();
            logger.info("");
            
            testEdgeCaseGeneration();
            logger.info("");
            
            testFormatGeneration();
            logger.info("");
            
            testCustomFileGeneration();
            logger.info("");
            
            testLargeDataSetGeneration();
            logger.info("");
            
            // Cleanup
            logger.info("Cleaning up generated files...");
            TestDataFactory.cleanupGeneratedFiles();
            logger.info("Cleanup complete!");
            
        } catch (Exception e) {
            logger.error("Error in example: {}", e.getMessage(), e);
        }
    }
}