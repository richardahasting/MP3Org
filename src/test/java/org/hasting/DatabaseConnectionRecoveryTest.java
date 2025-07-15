package org.hasting;

import org.hasting.util.DatabaseManager;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.DatabaseProfileManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Test suite for database connection recovery mechanisms in DatabaseManager.
 * Tests the ensureConnection() method and null pointer exception prevention.
 * 
 * @see DatabaseManager#ensureConnection()
 */
@DisplayName("Database Connection Recovery Tests")
public class DatabaseConnectionRecoveryTest {
    
    private DatabaseProfileManager profileManager;
    
    @BeforeEach
    void setUp() {
        profileManager = DatabaseProfileManager.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        try {
            DatabaseManager.shutdown();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    @DisplayName("Test ensureConnection returns valid connection")
    void testEnsureConnectionReturnsValidConnection() throws SQLException {
        // Create a test profile first
        String testPath = System.getProperty("user.home") + "/MP3Org-ConnectionTest-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("Connection Test", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // ensureConnection should return a valid connection
        Connection conn = assertDoesNotThrow(() -> DatabaseManager.ensureConnection());
        
        assertNotNull(conn, "ensureConnection should return non-null connection");
        assertFalse(conn.isClosed(), "Connection should be open and valid");
    }
    
    @Test
    @DisplayName("Test ensureConnection recovers from null connection")
    void testEnsureConnectionRecoversFromNull() throws SQLException {
        // Create a test profile
        String testPath = System.getProperty("user.home") + "/MP3Org-RecoveryTest-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("Recovery Test", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // Initialize database first
        DatabaseManager.initialize();
        
        // Simulate connection loss by shutting down
        DatabaseManager.shutdown();
        
        // ensureConnection should recover automatically
        Connection conn = assertDoesNotThrow(() -> DatabaseManager.ensureConnection());
        
        assertNotNull(conn, "ensureConnection should recover from null connection");
        assertFalse(conn.isClosed(), "Recovered connection should be valid");
    }
    
    @Test
    @DisplayName("Test recordScanDirectory works with connection recovery")
    void testRecordScanDirectoryWithRecovery() {
        // Create a test profile
        String testPath = System.getProperty("user.home") + "/MP3Org-ScanTest-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("Scan Test", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // Initialize database
        DatabaseManager.initialize();
        
        // Simulate profile switching scenario where connection might be null
        DatabaseManager.shutdown();
        
        // recordScanDirectory should work despite connection being null initially
        String testDirectory = "/test/music/directory";
        assertDoesNotThrow(() -> {
            DatabaseManager.recordScanDirectory(testDirectory);
        }, "recordScanDirectory should handle null connection gracefully");
        
        // Verify the directory was recorded
        assertDoesNotThrow(() -> {
            var directories = DatabaseManager.getScanDirectories();
            assertTrue(directories.contains(testDirectory), "Directory should be recorded despite connection recovery");
        });
    }
    
    @Test
    @DisplayName("Test getScanDirectories works with connection recovery")
    void testGetScanDirectoriesWithRecovery() {
        // Create a test profile
        String testPath = System.getProperty("user.home") + "/MP3Org-GetScanTest-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("GetScan Test", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // Initialize and record a directory
        DatabaseManager.initialize();
        DatabaseManager.recordScanDirectory("/test/directory");
        
        // Simulate connection loss
        DatabaseManager.shutdown();
        
        // getScanDirectories should recover and return results
        assertDoesNotThrow(() -> {
            var directories = DatabaseManager.getScanDirectories();
            assertNotNull(directories, "getScanDirectories should return valid list");
            assertTrue(directories.contains("/test/directory"), "Should find previously recorded directory");
        }, "getScanDirectories should handle connection recovery");
    }
    
    @Test
    @DisplayName("Test getMusicFileCount works with connection recovery")
    void testGetMusicFileCountWithRecovery() {
        // Create a test profile
        String testPath = System.getProperty("user.home") + "/MP3Org-CountTest-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("Count Test", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // Initialize database
        DatabaseManager.initialize();
        
        // Simulate connection loss
        DatabaseManager.shutdown();
        
        // getMusicFileCount should recover and return valid count
        assertDoesNotThrow(() -> {
            int count = DatabaseManager.getMusicFileCount();
            assertTrue(count >= 0, "Count should be non-negative");
        }, "getMusicFileCount should handle connection recovery");
    }
    
    @Test
    @DisplayName("Test updateScanDirectoryRescanTime works with connection recovery")
    void testUpdateScanDirectoryRescanTimeWithRecovery() {
        // Create a test profile
        String testPath = System.getProperty("user.home") + "/MP3Org-UpdateTest-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("Update Test", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // Initialize and record a directory
        DatabaseManager.initialize();
        String testDirectory = "/test/update/directory";
        DatabaseManager.recordScanDirectory(testDirectory);
        
        // Simulate connection loss
        DatabaseManager.shutdown();
        
        // updateScanDirectoryRescanTime should recover and work
        assertDoesNotThrow(() -> {
            DatabaseManager.updateScanDirectoryRescanTime(testDirectory);
        }, "updateScanDirectoryRescanTime should handle connection recovery");
    }
    
    @Test
    @DisplayName("Test ensureConnection handles invalid profile gracefully")
    void testEnsureConnectionHandlesInvalidProfile() {
        // Set an invalid profile (non-existent path with no permissions)
        String invalidPath = "/invalid/path/with/no/permissions";
        DatabaseProfile invalidProfile = profileManager.createProfile("Invalid Test", invalidPath);
        profileManager.setActiveProfile(invalidProfile.getId());
        
        // ensureConnection should either work with fallback or throw meaningful exception
        assertDoesNotThrow(() -> {
            try {
                Connection conn = DatabaseManager.ensureConnection();
                assertNotNull(conn, "Should get valid connection through fallback");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("database connection"), 
                          "Exception should have meaningful database connection message");
            }
        }, "ensureConnection should handle invalid profiles gracefully");
    }
    
    @Test
    @DisplayName("Test connection recovery preserves database operations")
    void testConnectionRecoveryPreservesOperations() {
        // Create a test profile
        String testPath = System.getProperty("user.home") + "/MP3Org-PreserveTest-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("Preserve Test", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // Initialize and perform operations
        DatabaseManager.initialize();
        String directory1 = "/test/preserve/dir1";
        String directory2 = "/test/preserve/dir2";
        
        DatabaseManager.recordScanDirectory(directory1);
        
        // Simulate connection loss and recovery
        DatabaseManager.shutdown();
        
        // Operations after recovery should work and preserve previous data
        assertDoesNotThrow(() -> {
            DatabaseManager.recordScanDirectory(directory2);
            
            var directories = DatabaseManager.getScanDirectories();
            assertTrue(directories.contains(directory1), "Should preserve directory1 from before connection loss");
            assertTrue(directories.contains(directory2), "Should include directory2 added after recovery");
        }, "Connection recovery should preserve database operations");
    }
}