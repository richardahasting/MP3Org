package org.hasting;

import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseConfig;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.DatabaseManager;
import org.hasting.util.TestDatabaseProfileManager;
import org.hasting.util.TestDataInitializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for MP3Org tests that provides isolated database testing infrastructure.
 * 
 * <p>This class provides:</p>
 * <ul>
 * <li><strong>Isolated test database</strong> - Each test run gets a unique database profile</li>
 * <li><strong>Automatic test data population</strong> - Real audio files from test resources are scanned and loaded</li>
 * <li><strong>Clean slate for each test</strong> - Database is cleared and repopulated before each test method</li>
 * <li><strong>Proper cleanup</strong> - Test database files are removed after test completion</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>{@code
 * public class MyServiceTest extends MP3OrgTestBase {
 *     
 *     @Test
 *     void testSomething() {
 *         // Test data is automatically available
 *         List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
 *         assertFalse(allFiles.isEmpty());
 *         
 *         // Use getTestData() to access the original test data for assertions
 *         List<MusicFile> expectedFiles = getTestData();
 *         assertEquals(expectedFiles.size(), allFiles.size());
 *     }
 * }
 * }</pre>
 * 
 * @since 1.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MP3OrgTestBase {
    private static final Logger logger = Logger.getLogger(MP3OrgTestBase.class.getName());
    
    protected DatabaseProfile testProfile;
    protected List<MusicFile> testData;
    protected DatabaseProfile originalProfile;
    
    /**
     * Sets up the test environment with an isolated database profile and test data.
     * This method runs once per test class.
     */
    @BeforeAll
    void setUpTestEnvironment() {
        logger.info("Setting up test environment for " + this.getClass().getSimpleName());
        
        try {
            // Save the original profile to restore later
            originalProfile = DatabaseConfig.getInstance().getActiveProfile();
            logger.info("Saved original profile: " + 
                       (originalProfile != null ? originalProfile.getId() : "none"));
            
            // Create and activate isolated test profile
            testProfile = TestDatabaseProfileManager.createIsolatedTestProfile();
            TestDatabaseProfileManager.activateTestProfile(testProfile);
            
            // Initialize database
            DatabaseManager.initialize();
            logger.info("Database initialized for test profile: " + testProfile.getId());
            
            // Populate with test data from real audio files
            testData = TestDataInitializer.populateTestData();
            
            // Validate test data was loaded correctly
            if (!TestDataInitializer.validateTestData()) {
                logger.warning("Test data validation failed, but continuing with tests");
            }
            
            logger.info("Test environment initialized successfully with " + 
                       testData.size() + " test music files");
            
        } catch (Exception e) {
            logger.severe("Failed to set up test environment: " + e.getMessage());
            throw new RuntimeException("Test environment setup failed", e);
        }
    }
    
    /**
     * Tears down the test environment and cleans up resources.
     * This method runs once per test class.
     */
    @AfterAll
    void tearDownTestEnvironment() {
        logger.info("Tearing down test environment for " + this.getClass().getSimpleName());
        
        try {
            // Clean up test profile and database
            if (testProfile != null) {
                TestDatabaseProfileManager.cleanupTestProfile(testProfile);
            }
            
            // Restore original profile
            if (originalProfile != null) {
                try {
                    boolean success = DatabaseConfig.getInstance().switchToProfile(originalProfile.getId());
                    if (success) {
                        DatabaseManager.initialize(); // Re-initialize with original profile
                        logger.info("Restored original profile: " + originalProfile.getId());
                    } else {
                        logger.warning("Failed to restore original profile: " + originalProfile.getId());
                    }
                } catch (Exception e) {
                    logger.warning("Could not restore original profile: " + e.getMessage());
                }
            }
            
            logger.info("Test environment teardown completed");
            
        } catch (Exception e) {
            logger.warning("Error during test environment teardown: " + e.getMessage());
            // Don't throw exception during teardown
        }
    }
    
    /**
     * Resets the test data before each test method.
     * This ensures each test starts with a clean, predictable database state.
     */
    @BeforeEach
    void resetTestData() {
        logger.fine("Resetting test data for test method");
        
        try {
            // Clear all existing data
            DatabaseManager.deleteAllMusicFiles();
            
            // Re-populate with fresh test data
            if (testData != null) {
                for (MusicFile musicFile : testData) {
                    MusicFile copy = createCopyForDatabase(musicFile);
                    DatabaseManager.saveMusicFile(copy);
                }
                logger.fine("Reset database with " + testData.size() + " test files");
            }
            
        } catch (Exception e) {
            logger.severe("Failed to reset test data: " + e.getMessage());
            throw new RuntimeException("Test data reset failed", e);
        }
    }
    
    /**
     * Creates a copy of a MusicFile suitable for database insertion.
     * This resets the ID to allow re-insertion and creates a fresh instance.
     * 
     * @param original The original MusicFile to copy
     * @return A copy suitable for database insertion
     */
    private MusicFile createCopyForDatabase(MusicFile original) {
        MusicFile copy = new MusicFile();
        
        // Copy all properties except ID
        copy.setFilePath(original.getFilePath());
        copy.setTitle(original.getTitle());
        copy.setArtist(original.getArtist());
        copy.setAlbum(original.getAlbum());
        copy.setGenre(original.getGenre());
        copy.setYear(original.getYear());
        copy.setTrackNumber(original.getTrackNumber());
        copy.setDurationSeconds(original.getDurationSeconds());
        copy.setFileType(original.getFileType());
        copy.setFileSizeBytes(original.getFileSizeBytes());
        copy.setBitRate(original.getBitRate());
        copy.setSampleRate(original.getSampleRate());
        copy.setLastModified(original.getLastModified());
        copy.setDateAdded(original.getDateAdded());
        
        return copy;
    }
    
    /**
     * Provides access to the original test data for test assertions.
     * This returns the test data as it was initially loaded, useful for
     * verifying expected results.
     * 
     * @return A copy of the original test data list
     */
    protected List<MusicFile> getTestData() {
        return testData != null ? new ArrayList<>(testData) : new ArrayList<>();
    }
    
    /**
     * Gets the current test database profile for advanced test scenarios.
     * 
     * @return The active test database profile
     */
    protected DatabaseProfile getTestProfile() {
        return testProfile;
    }
    
    /**
     * Utility method to get the number of expected test files.
     * This can be used in assertions to verify test data integrity.
     * 
     * @return The number of test files that should be available
     */
    protected int getExpectedTestFileCount() {
        return testData != null ? testData.size() : 0;
    }
    
    /**
     * Utility method to verify that test data is properly loaded.
     * Call this in test methods if you want to ensure test data integrity.
     * 
     * @return true if test data appears to be properly loaded
     */
    protected boolean isTestDataValid() {
        try {
            List<MusicFile> dbFiles = DatabaseManager.getAllMusicFiles();
            return dbFiles.size() == getExpectedTestFileCount() && 
                   TestDataInitializer.validateTestData();
        } catch (Exception e) {
            logger.warning("Test data validation failed: " + e.getMessage());
            return false;
        }
    }
}