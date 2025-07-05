package org.hasting;

import org.hasting.model.MusicFile;
import org.hasting.util.BaseTest;
import org.hasting.util.DatabaseManager;
import org.hasting.util.TestHarness;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for MP3Org tests that provides isolated database testing infrastructure.
 * 
 * <p>This class now uses the new TestHarness infrastructure to provide:</p>
 * <ul>
 * <li><strong>TEST-HARNESS profile</strong> - Standardized test profile shared across all tests</li>
 * <li><strong>Consistent test data</strong> - Pre-loaded data from /Users/richard/mp3s directory</li>
 * <li><strong>Automatic cleanup</strong> - Removes temporary profiles and restores user settings</li>
 * <li><strong>Profile isolation</strong> - No interference with user's production profiles</li>
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
 *         // Test runs in TEST-HARNESS profile with consistent data
 *         ensureTestEnvironment(); // Verify isolation
 *     }
 * }
 * }</pre>
 * 
 * @since 1.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MP3OrgTestBase extends BaseTest {
    private static final Logger logger = Logger.getLogger(MP3OrgTestBase.class.getName());
    
    /**
     * Gets the test data available in the TEST-HARNESS profile.
     * 
     * @return List of music files from the test database
     */
    protected List<MusicFile> getTestData() {
        return DatabaseManager.getAllMusicFiles();
    }
    
    /**
     * Gets the number of test files available in the test database.
     * 
     * @return Number of test files loaded
     */
    protected int getExpectedTestFileCount() {
        return getTestDataCount();
    }
    
    /**
     * Utility method to verify that test data is properly loaded.
     * 
     * @return true if test data appears to be properly loaded
     */
    protected boolean isTestDataValid() {
        try {
            ensureTestEnvironment();
            List<MusicFile> dbFiles = DatabaseManager.getAllMusicFiles();
            return !dbFiles.isEmpty();
        } catch (Exception e) {
            logger.warning("Test data validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method for tests that create additional profiles during testing.
     * This ensures they get cleaned up properly.
     * 
     * @param profileId The profile ID to track for cleanup
     */
    protected void trackProfileForCleanup(String profileId) {
        TestHarness.trackProfileForCleanup(profileId);
    }
}