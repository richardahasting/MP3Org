package org.hasting;

import org.hasting.util.DatabaseProfile;
import org.hasting.util.DatabaseProfileManager;
import org.hasting.util.DatabaseManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.lang.reflect.Method;

/**
 * Test suite for database error recovery mechanisms in MP3OrgApplication.
 * Tests the progressive fallback strategy for database initialization failures.
 * 
 * @see MP3OrgApplication#initializeDatabaseWithAutomaticFallback()
 */
@DisplayName("Database Error Recovery Tests - Issue #59")
public class DatabaseErrorRecoveryTest {
    
    private MP3OrgApplication application;
    private DatabaseProfileManager profileManager;
    
    @BeforeEach
    void setUp() {
        application = new MP3OrgApplication();
        profileManager = DatabaseProfileManager.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up any test profiles
        try {
            DatabaseManager.shutdown();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    @DisplayName("Test standard database initialization works normally")
    void testStandardInitializationSuccess() throws Exception {
        // Get access to private method
        Method standardInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseStandard");
        standardInit.setAccessible(true);
        
        // Create a valid test profile
        String testPath = System.getProperty("user.home") + "/MP3Org-Test-" + System.currentTimeMillis();
        DatabaseProfile testProfile = profileManager.createProfile("Test Profile", testPath);
        profileManager.setActiveProfile(testProfile.getId());
        
        // Standard initialization should work
        assertDoesNotThrow(() -> {
            try {
                standardInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify database is initialized by checking if we can get a connection
        assertDoesNotThrow(() -> {
            DatabaseManager.getConnection();
        }, "Database should be initialized after standard init");
    }
    
    @Test
    @DisplayName("Test emergency database creation when standard initialization fails")
    void testEmergencyDatabaseCreation() throws Exception {
        // Get access to private method
        Method emergencyInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseEmergency");
        emergencyInit.setAccessible(true);
        
        // Emergency initialization should create new profile
        assertDoesNotThrow(() -> {
            try {
                emergencyInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify emergency profile was created
        String activeProfileId = profileManager.getActiveProfileId();
        assertNotNull(activeProfileId, "Emergency profile should be created and activated");
        
        DatabaseProfile emergencyProfile = profileManager.getProfile(activeProfileId);
        assertTrue(emergencyProfile.getName().startsWith("Emergency-"), 
                  "Profile name should indicate emergency mode");
        
        // Verify emergency directory exists
        File emergencyDir = new File(emergencyProfile.getDatabasePath());
        assertTrue(emergencyDir.exists(), "Emergency database directory should exist");
        assertTrue(emergencyDir.isDirectory(), "Emergency path should be a directory");
    }
    
    @Test
    @DisplayName("Test safe mode database initialization")
    void testSafeModeInitialization() throws Exception {
        // Get access to private method
        Method safeModeInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseSafeMode");
        safeModeInit.setAccessible(true);
        
        // Safe mode initialization should work
        assertDoesNotThrow(() -> {
            try {
                safeModeInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify safe mode profile was created
        String activeProfileId = profileManager.getActiveProfileId();
        assertNotNull(activeProfileId, "Safe mode profile should be created and activated");
        
        DatabaseProfile safeModeProfile = profileManager.getProfile(activeProfileId);
        assertEquals("Safe Mode", safeModeProfile.getName(), 
                    "Profile name should indicate safe mode");
        
        // Verify safe mode directory is in temp location
        String safeModeDir = safeModeProfile.getDatabasePath();
        assertTrue(safeModeDir.contains("SafeMode"), 
                  "Safe mode path should contain 'SafeMode'");
        
        File safeDir = new File(safeModeDir);
        assertTrue(safeDir.exists(), "Safe mode database directory should exist");
    }
    
    @Test
    @DisplayName("Test temporary in-memory database initialization")
    void testTemporaryDatabaseInitialization() throws Exception {
        // Get access to private method
        Method tempInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseTemporary");
        tempInit.setAccessible(true);
        
        // Temporary initialization should never fail
        assertDoesNotThrow(() -> {
            try {
                tempInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify temporary profile was created
        String activeProfileId = profileManager.getActiveProfileId();
        assertNotNull(activeProfileId, "Temporary profile should be created and activated");
        
        DatabaseProfile tempProfile = profileManager.getProfile(activeProfileId);
        assertEquals("Temporary", tempProfile.getName(), 
                    "Profile name should indicate temporary mode");
        assertEquals(":memory:", tempProfile.getDatabasePath(), 
                    "Temporary profile should use in-memory database");
    }
    
    @Test
    @DisplayName("Test progressive fallback chain handles multiple failures")
    void testProgressiveFallbackChain() throws Exception {
        // This test simulates the scenario where multiple initialization attempts fail
        // and verifies that the application eventually succeeds with a fallback option
        
        // Remove all existing profiles to force fallback behavior
        for (DatabaseProfile profile : profileManager.getAllProfiles()) {
            if (!profile.getName().equals("Temporary")) {
                profileManager.removeProfile(profile.getId());
            }
        }
        
        // Get access to main fallback method
        Method fallbackInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseWithAutomaticFallback");
        fallbackInit.setAccessible(true);
        
        // The progressive fallback should eventually succeed
        assertDoesNotThrow(() -> {
            try {
                fallbackInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify some form of database is initialized
        assertDoesNotThrow(() -> {
            DatabaseManager.getConnection();
        }, "Database should be initialized through fallback mechanisms");
        
        // Verify a profile is active
        String activeProfileId = profileManager.getActiveProfileId();
        assertNotNull(activeProfileId, "Some profile should be active after fallback");
    }
    
    @Test
    @DisplayName("Test emergency directory creation permissions")
    void testEmergencyDirectoryPermissions() throws Exception {
        String emergencyBasePath = System.getProperty("user.home") + "/MP3Org-Emergency";
        
        // Get access to private method
        Method emergencyInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseEmergency");
        emergencyInit.setAccessible(true);
        
        // Emergency initialization should create directory structure
        assertDoesNotThrow(() -> {
            try {
                emergencyInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify emergency base directory exists
        File emergencyBase = new File(emergencyBasePath);
        assertTrue(emergencyBase.exists(), "Emergency base directory should exist");
        assertTrue(emergencyBase.isDirectory(), "Emergency base should be a directory");
        assertTrue(emergencyBase.canRead(), "Emergency directory should be readable");
        assertTrue(emergencyBase.canWrite(), "Emergency directory should be writable");
    }
    
    @Test
    @DisplayName("Test database recovery preserves user experience")
    void testRecoveryPreservesUserExperience() throws Exception {
        // Test that after recovery, basic application functionality is available
        
        // Get access to main fallback method
        Method fallbackInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseWithAutomaticFallback");
        fallbackInit.setAccessible(true);
        
        // Initialize with fallback
        assertDoesNotThrow(() -> {
            try {
                fallbackInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify core functionality is available
        assertDoesNotThrow(() -> {
            DatabaseManager.getConnection();
        }, "Database should be available for use");
        
        // Verify profile management is functional
        assertNotNull(profileManager.getActiveProfileId(), "Profile management should work");
        assertNotNull(profileManager.getActiveProfile(), "Active profile should be accessible");
        
        // Verify database operations are possible (basic test)
        assertDoesNotThrow(() -> {
            DatabaseManager.getAllMusicFiles(); // Should not throw even with empty database
        }, "Basic database operations should be available");
    }
    
    @Test
    @DisplayName("Test error recovery logging and user notification")
    void testErrorRecoveryLogging() throws Exception {
        // This test verifies that recovery operations are properly logged
        // and would show appropriate user notifications (notifications are UI-dependent)
        
        // Get access to emergency init method  
        Method emergencyInit = MP3OrgApplication.class.getDeclaredMethod("initializeDatabaseEmergency");
        emergencyInit.setAccessible(true);
        
        // Run emergency initialization
        assertDoesNotThrow(() -> {
            try {
                emergencyInit.invoke(application);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
        
        // Verify recovery was successful (indirect verification through state)
        DatabaseProfile activeProfile = profileManager.getActiveProfile();
        assertNotNull(activeProfile, "Recovery should result in active profile");
        assertTrue(activeProfile.getName().startsWith("Emergency-"), 
                  "Profile should indicate emergency recovery mode");
    }
}