package org.hasting.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Tests for DatabaseConnectionManager lock detection functionality.
 *
 * <p>This test class validates the self-teaching lock detection methods that form
 * the foundation of the database fallback system. Following the development philosophy
 * of clear, self-documenting code, these tests demonstrate how lock detection works
 * and validate the Derby-specific error handling.
 *
 * <p>Note: These tests require exclusive database access and will be skipped
 * if the MP3Org application is running.
 *
 * @author MP3Org Development Team
 */
public class DatabaseConnectionManagerTest {

    private static final String TEST_DB_BASE_PATH = "test-connection-manager";
    private Connection lockingConnection;

    @BeforeEach
    void setUp() {
        // Skip tests if app is running - they conflict on database resources
        BaseTest.assumeAppNotRunning();
        // Clean up any existing test databases
        cleanupTestDatabases();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up connections and test databases
        if (lockingConnection != null) {
            try {
                lockingConnection.close();
            } catch (SQLException e) {
                // Ignore cleanup errors
            }
        }
        cleanupTestDatabases();
    }
    
    @Test
    @DisplayName("Lock detection correctly identifies available databases")
    void testLockDetectionCorrectlyIdentifiesAvailableDatabases() {
        String testDbPath = createUniqueTestPath("available");
        
        // New database path should not be locked
        boolean isLocked = DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(testDbPath);
        
        assertFalse(isLocked, "Fresh database path should not be locked");
    }
    
    @Test
    @DisplayName("Lock detection validates input parameters properly")
    void testLockDetectionValidatesInputParametersProperly() {
        // Test null parameter
        IllegalArgumentException nullException = assertThrows(
            IllegalArgumentException.class,
            () -> DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(null),
            "Should reject null database path"
        );
        assertTrue(nullException.getMessage().contains("cannot be null"));
        
        // Test empty parameter
        IllegalArgumentException emptyException = assertThrows(
            IllegalArgumentException.class,
            () -> DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(""),
            "Should reject empty database path"
        );
        assertTrue(emptyException.getMessage().contains("cannot be null or empty"));
        
        // Test whitespace-only parameter
        IllegalArgumentException whitespaceException = assertThrows(
            IllegalArgumentException.class,
            () -> DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess("   "),
            "Should reject whitespace-only database path"
        );
        assertTrue(whitespaceException.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    @DisplayName("Profile availability checking works with valid profiles")
    void testProfileAvailabilityCheckingWorksWithValidProfiles() {
        String testDbPath = createUniqueTestPath("profile-test");
        
        // Create a test profile
        DatabaseProfile testProfile = new DatabaseProfile("test-profile", "Test Profile", testDbPath);
        
        // Should be available since no one is using it
        boolean isAvailable = DatabaseConnectionManager.isDatabaseAvailableForProfile(testProfile);
        
        assertTrue(isAvailable, "New test profile should be available");
    }
    
    @Test
    @DisplayName("Profile availability checking validates input parameters")
    void testProfileAvailabilityCheckingValidatesInputParameters() {
        // Test null profile
        IllegalArgumentException nullException = assertThrows(
            IllegalArgumentException.class,
            () -> DatabaseConnectionManager.isDatabaseAvailableForProfile(null),
            "Should reject null profile"
        );
        assertTrue(nullException.getMessage().contains("cannot be null"));
        
        // Test profile with null database path
        DatabaseProfile invalidProfile = new DatabaseProfile("invalid", "Invalid", null);
        IllegalArgumentException pathException = assertThrows(
            IllegalArgumentException.class,
            () -> DatabaseConnectionManager.isDatabaseAvailableForProfile(invalidProfile),
            "Should reject profile with null database path"
        );
        assertTrue(pathException.getMessage().contains("invalid database path"));
    }
    
    @Test
    @DisplayName("Connection failure explanation provides helpful messages")
    void testConnectionFailureExplanationProvidesHelpfulMessages() {
        String testDbPath = createUniqueTestPath("explanation-test");
        
        // Create a mock SQLException that might occur
        SQLException testException = new SQLException("Test connection failure", "08001");
        
        String explanation = DatabaseConnectionManager.explainConnectionFailure(testException, testDbPath);
        
        assertNotNull(explanation, "Should provide explanation for connection failure");
        assertTrue(explanation.contains(testDbPath), "Explanation should mention the database path");
        assertTrue(explanation.contains("Test connection failure"), "Explanation should include original error message");
    }
    
    @Test
    @DisplayName("Lock explanation recognizes Derby lock scenarios")
    void testLockExplanationRecognizesDerbyLockScenarios() {
        String testDbPath = createUniqueTestPath("lock-explanation-test");
        
        // Test with a Derby lock exception
        SQLException lockException = new SQLException("Database is already in use by another process", "XJ041");
        
        String explanation = DatabaseConnectionManager.explainConnectionFailure(lockException, testDbPath);
        
        assertNotNull(explanation, "Should provide explanation for lock failure");
        assertTrue(explanation.contains("another MP3Org instance"), "Should mention other instance");
        assertTrue(explanation.contains("automatically use an alternative"), "Should mention automatic fallback");
    }
    
    @Test
    @DisplayName("Quick test connection method handles various scenarios")
    void testQuickTestConnectionMethodHandlesVariousScenarios() {
        // This test validates that the private createQuickTestConnection method
        // (tested indirectly through public methods) handles various scenarios properly
        
        String testDbPath = createUniqueTestPath("quick-test");
        
        // Should not be locked initially
        assertFalse(DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(testDbPath));
        
        // Try with a completely invalid path (should not crash)
        String invalidPath = "/invalid/path/that/does/not/exist";
        // This should return false (not locked, just inaccessible)
        assertFalse(DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(invalidPath));
    }
    
    @Test
    @DisplayName("Multiple rapid lock checks do not interfere with each other")
    void testMultipleRapidLockChecksDoNotInterfereWithEachOther() {
        String testDbPath1 = createUniqueTestPath("rapid-1");
        String testDbPath2 = createUniqueTestPath("rapid-2");
        String testDbPath3 = createUniqueTestPath("rapid-3");
        
        // Perform multiple rapid checks
        boolean result1 = DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(testDbPath1);
        boolean result2 = DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(testDbPath2);
        boolean result3 = DatabaseConnectionManager.isDerbyDatabaseLockedByAnotherProcess(testDbPath3);
        
        // All should be available (not locked)
        assertFalse(result1, "First database should not be locked");
        assertFalse(result2, "Second database should not be locked");
        assertFalse(result3, "Third database should not be locked");
    }
    
    /**
     * Creates a unique test database path to avoid conflicts between tests.
     */
    private String createUniqueTestPath(String identifier) {
        long timestamp = System.currentTimeMillis();
        return TEST_DB_BASE_PATH + "-" + identifier + "-" + timestamp;
    }
    
    /**
     * Cleans up test databases to avoid interference between tests.
     */
    private void cleanupTestDatabases() {
        try {
            Path currentDir = Paths.get(".");
            Files.list(currentDir)
                .filter(path -> path.getFileName().toString().startsWith(TEST_DB_BASE_PATH))
                .forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            deleteDirectoryRecursively(path);
                        } else {
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException e) {
                        // Ignore cleanup errors - they shouldn't fail the test
                    }
                });
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Helper method to recursively delete directories.
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore individual file deletion errors
                    }
                });
        }
    }
}