package org.hasting.util;

import org.hasting.model.MusicFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for initializing test data from real audio files in test resources.
 * Provides methods to populate the test database with known, predictable music file data.
 */
public class TestDataInitializer {
    private static final Logger logger = Logger.getLogger(TestDataInitializer.class.getName());
    
    private static final String TEST_AUDIO_RESOURCES = "src/test/resources/audio";
    
    /**
     * Populates the database with test data by scanning real audio files from test resources.
     * This ensures tests run with known, predictable data.
     * 
     * @return List of MusicFile objects that were saved to the database
     */
    public static List<MusicFile> populateTestData() {
        logger.info("Initializing test data from audio resources...");
        
        MusicFileScanner scanner = new MusicFileScanner();
        
        // Scan test audio resources
        List<String> testDirectories = Arrays.asList(
            TEST_AUDIO_RESOURCES + "/basic",
            TEST_AUDIO_RESOURCES + "/scanner"
        );
        
        // Filter to only existing directories
        List<String> existingDirectories = new ArrayList<>();
        for (String dir : testDirectories) {
            File directory = new File(dir);
            if (directory.exists() && directory.isDirectory()) {
                existingDirectories.add(dir);
                logger.info("Found test audio directory: " + dir);
            } else {
                logger.warning("Test audio directory not found: " + dir);
            }
        }
        
        if (existingDirectories.isEmpty()) {
            logger.warning("No test audio directories found. Tests will run with empty database.");
            return new ArrayList<>();
        }
        
        // Scan for music files
        List<MusicFile> testFiles = scanner.scanMusicFiles(existingDirectories);
        logger.info("Scanned " + testFiles.size() + " test music files");
        
        // Save all test files to database
        int savedCount = 0;
        for (MusicFile musicFile : testFiles) {
            try {
                DatabaseManager.saveMusicFile(musicFile);
                savedCount++;
                logger.fine("Saved test file: " + musicFile.getFilePath());
            } catch (Exception e) {
                logger.warning("Failed to save test file " + musicFile.getFilePath() + ": " + e.getMessage());
            }
        }
        
        logger.info("Successfully saved " + savedCount + " test music files to database");
        return testFiles;
    }
    
    /**
     * Gets a list of expected test data characteristics for verification purposes.
     * This allows tests to validate that the expected test data is present.
     * 
     * @return TestDataExpectations object with known characteristics
     */
    public static TestDataExpectations getExpectedTestData() {
        TestDataExpectations expectations = new TestDataExpectations();
        
        // Expected test files based on src/test/resources/audio structure
        expectations.expectedMinimumFiles = 10; // We created 10 test files
        expectations.expectedFileTypes = Arrays.asList("mp3", "flac", "wav");
        expectations.expectedArtists = Arrays.asList("Scanner Test Artist"); // From our test file creation
        expectations.expectedAlbums = Arrays.asList("Scanner Test Album");
        
        // Beatles file from basic collection
        expectations.expectedBeatlesFiles = 1;
        
        return expectations;
    }
    
    /**
     * Validates that the database contains the expected test data.
     * 
     * @return true if test data is valid, false otherwise
     */
    public static boolean validateTestData() {
        try {
            List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
            TestDataExpectations expected = getExpectedTestData();
            
            if (allFiles.size() < expected.expectedMinimumFiles) {
                logger.warning("Test data validation failed: Expected at least " + 
                             expected.expectedMinimumFiles + " files, found " + allFiles.size());
                return false;
            }
            
            // Check file types
            boolean hasExpectedTypes = allFiles.stream()
                .anyMatch(f -> expected.expectedFileTypes.contains(f.getFileType()));
            
            if (!hasExpectedTypes) {
                logger.warning("Test data validation failed: No expected file types found");
                return false;
            }
            
            logger.info("Test data validation passed: " + allFiles.size() + " files loaded");
            return true;
            
        } catch (Exception e) {
            logger.severe("Test data validation failed with exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Data class to hold expectations about test data for validation.
     */
    public static class TestDataExpectations {
        public int expectedMinimumFiles;
        public List<String> expectedFileTypes;
        public List<String> expectedArtists;
        public List<String> expectedAlbums;
        public int expectedBeatlesFiles;
    }
}