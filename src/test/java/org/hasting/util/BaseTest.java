package org.hasting.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Base test class that provides standardized test harness setup and cleanup.
 * 
 * <p>All test classes should extend this class to ensure:
 * <ul>
 * <li>Tests run in isolation using the TEST-HARNESS profile</li>
 * <li>Consistent test data is available across all tests</li>
 * <li>Proper cleanup of test artifacts after test completion</li>
 * <li>No interference with user's production database profiles</li>
 * </ul>
 * 
 * <p>Test classes extending this base class automatically get:
 * <ul>
 * <li>A dedicated test database with known test data</li>
 * <li>Automatic profile switching and restoration</li>
 * <li>Cleanup of temporary test profiles</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * public class MyTest extends BaseTest {
 *     
 *     @Test
 *     void testSomething() {
 *         // Test code here - automatically uses TEST-HARNESS profile
 *         List<MusicFile> files = DatabaseManager.getAllMusicFiles();
 *         assertThat(files).isNotEmpty(); // Test data is pre-loaded
 *     }
 * }
 * }</pre>
 * 
 * @see TestHarness
 * @since 1.0
 */
public abstract class BaseTest {
    
    /**
     * Sets up the test harness before any tests in the class run.
     * This creates the TEST-HARNESS profile and imports test data.
     *
     * <p>Note: Tests will be skipped if the MP3Org application is running
     * to avoid database conflicts.
     */
    @BeforeAll
    static void setupTestHarness() {
        assumeAppNotRunning();
        TestHarness.setupTestingProfile();
    }
    
    /**
     * Cleans up test artifacts after all tests in the class complete.
     * This removes temporary profiles and restores the original active profile.
     */
    @AfterAll
    static void cleanupTestHarness() {
        TestHarness.cleanup();
    }
    
    /**
     * Helper method to ensure tests are running in the correct environment.
     * Can be called by test methods to verify test isolation.
     */
    protected void ensureTestEnvironment() {
        TestHarness.ensureTestProfileActive();
    }
    
    /**
     * Gets the number of music files available in the test data.
     * Useful for tests that need to know the expected data size.
     *
     * @return Number of music files in the test database
     */
    protected int getTestDataCount() {
        return TestHarness.getTestDataCount();
    }

    /**
     * Checks if the MP3Org application is currently running on port 9090.
     *
     * @return true if the app is running, false otherwise
     */
    public static boolean isAppRunning() {
        return isPortInUse(9090);
    }

    /**
     * Checks if a specific port is in use.
     *
     * @param port the port to check
     * @return true if the port is in use, false otherwise
     */
    public static boolean isPortInUse(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Skips the current test if the MP3Org application is running.
     * Use this for tests that cannot run concurrently with the application
     * due to database locking or shared resource conflicts.
     *
     * <p>Example usage:
     * <pre>{@code
     * @Test
     * void testDatabaseOperation() {
     *     BaseTest.assumeAppNotRunning();
     *     // Test code that requires exclusive database access
     * }
     * }</pre>
     */
    public static void assumeAppNotRunning() {
        assumeFalse(isAppRunning(),
            "Test skipped: MP3Org application is running on port 9090. " +
            "Stop the application to run database isolation tests.");
    }
}