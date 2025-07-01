package org.hasting.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for profile deletion functionality.
 * 
 * <p>This test class validates all aspects of profile deletion including:
 * <ul>
 *   <li>Basic profile deletion operations</li>
 *   <li>Safety mechanisms (cannot delete last profile)</li>
 *   <li>Active profile switching when deleting current profile</li>
 *   <li>Database file deletion when profile is removed</li>
 *   <li>Configuration persistence after deletions</li>
 * </ul>
 * 
 * @since 1.0
 */
@DisplayName("Profile Deletion Functionality Tests")
public class ProfileDeletionTest {
    
    private DatabaseProfileManager profileManager;
    private String originalActiveProfile;
    private int originalProfileCount;
    
    @BeforeEach
    void setUp() {
        // Initialize profile manager and capture initial state
        profileManager = DatabaseProfileManager.getInstance();
        
        // Store original state for restoration
        DatabaseProfile activeProfile = profileManager.getActiveProfile();
        originalActiveProfile = activeProfile != null ? activeProfile.getId() : null;
        originalProfileCount = profileManager.getAllProfiles().size();
        
        System.out.println("=== Test Setup ===");
        System.out.println("Original active profile: " + originalActiveProfile);
        System.out.println("Original profile count: " + originalProfileCount);
    }
    
    @AfterEach
    void tearDown() {
        // Restore original active profile if it still exists
        if (originalActiveProfile != null) {
            try {
                DatabaseProfile profile = profileManager.getProfile(originalActiveProfile);
                if (profile != null) {
                    profileManager.setActiveProfile(originalActiveProfile);
                    System.out.println("Restored original active profile: " + originalActiveProfile);
                }
            } catch (Exception e) {
                System.out.println("Could not restore original active profile (may have been deleted): " + e.getMessage());
            }
        }
        System.out.println("=== Test Cleanup Complete ===\n");
    }
    
    @Test
    @DisplayName("Should create test profile successfully")
    void testCreateTestProfile() {
        System.out.println("\n=== Testing Profile Creation ===");
        
        String testProfileName = "Test Profile for Deletion " + System.currentTimeMillis();
        String testDescription = "Temporary profile created for deletion testing";
        String testDatabasePath = "test-deletion-db-" + System.currentTimeMillis();
        
        try {
            // Create test profile
            DatabaseProfile testProfile = profileManager.createProfile(
                testProfileName, testDatabasePath, testDescription
            );
            
            // Verify profile was created
            assertNotNull(testProfile, "Test profile should be created successfully");
            assertNotNull(testProfile.getId(), "Profile should have a generated ID");
            assertEquals(testProfileName, testProfile.getName(), "Profile name should match");
            assertEquals(testDescription, testProfile.getDescription(), "Profile description should match");
            
            // Verify profile appears in profile list
            List<DatabaseProfile> allProfiles = profileManager.getAllProfiles();
            assertTrue(allProfiles.stream().anyMatch(p -> p.getId().equals(testProfile.getId())), 
                      "Test profile should appear in profile list");
            
            System.out.println("✓ Test profile created successfully: " + testProfile.getId());
            System.out.println("✓ Current profile count: " + allProfiles.size());
            
        } catch (Exception e) {
            fail("Failed to create test profile: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Should delete non-active profile successfully")
    void testDeleteNonActiveProfile() {
        System.out.println("\n=== Testing Non-Active Profile Deletion ===");
        
        // Ensure we have enough profiles to safely delete one
        List<DatabaseProfile> profilesBefore = profileManager.getAllProfiles();
        if (profilesBefore.size() <= 1) {
            // Create additional profile to ensure we can safely delete
            createTemporaryProfile("temp-for-deletion");
            profilesBefore = profileManager.getAllProfiles();
        }
        
        // Find a profile that is not currently active
        DatabaseProfile activeProfile = profileManager.getActiveProfile();
        DatabaseProfile profileToDelete = profilesBefore.stream()
            .filter(p -> !p.getId().equals(activeProfile.getId()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(profileToDelete, "Should have at least one non-active profile to delete");
        
        String profileIdToDelete = profileToDelete.getId();
        String activeProfileId = activeProfile.getId();
        
        System.out.println("Active profile: " + activeProfileId);
        System.out.println("Profile to delete: " + profileIdToDelete);
        System.out.println("Profiles before deletion: " + profilesBefore.size());
        
        // Store database path for later verification
        String databasePath = profileToDelete.getDatabasePath();
        File databaseFile = new File(databasePath);
        File parentFile = databaseFile.getParentFile();
        boolean databaseExistedBefore = databaseFile.exists() || (parentFile != null && parentFile.exists());
        
        try {
            // Delete the profile
            boolean deleteResult = profileManager.removeProfile(profileIdToDelete);
            assertTrue(deleteResult, "Profile deletion should succeed");
            
            // Verify profile was removed from list
            List<DatabaseProfile> profilesAfter = profileManager.getAllProfiles();
            assertEquals(profilesBefore.size() - 1, profilesAfter.size(), 
                        "Profile count should decrease by 1");
            
            assertFalse(profilesAfter.stream().anyMatch(p -> p.getId().equals(profileIdToDelete)),
                       "Deleted profile should not appear in profile list");
            
            // Verify active profile remains unchanged
            DatabaseProfile currentActiveProfile = profileManager.getActiveProfile();
            assertNotNull(currentActiveProfile, "Should still have an active profile");
            assertEquals(activeProfileId, currentActiveProfile.getId(), 
                        "Active profile should remain unchanged when deleting non-active profile");
            
            // Verify database files are deleted
            if (databaseExistedBefore) {
                // Database files should now be deleted with the new implementation
                boolean databaseStillExists = databaseFile.exists() || (parentFile != null && parentFile.exists());
                assertFalse(databaseStillExists, "Database directory should be deleted when profile is removed");
                System.out.println("✓ Database files successfully deleted at: " + databasePath);
            }
            
            System.out.println("✓ Non-active profile deleted successfully");
            System.out.println("✓ Active profile preserved: " + currentActiveProfile.getId());
            System.out.println("✓ Profile count after deletion: " + profilesAfter.size());
            
        } catch (Exception e) {
            fail("Failed to delete non-active profile: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Should delete active profile and switch to another profile")
    void testDeleteActiveProfile() {
        System.out.println("\n=== Testing Active Profile Deletion ===");
        
        // Ensure we have enough profiles to safely delete the active one
        List<DatabaseProfile> profilesBefore = profileManager.getAllProfiles();
        if (profilesBefore.size() <= 1) {
            // Create additional profiles to ensure we can safely delete active one
            createTemporaryProfile("temp-1");
            createTemporaryProfile("temp-2");
            profilesBefore = profileManager.getAllProfiles();
        }
        
        DatabaseProfile originalActiveProfile = profileManager.getActiveProfile();
        assertNotNull(originalActiveProfile, "Should have an active profile");
        
        String activeProfileId = originalActiveProfile.getId();
        System.out.println("Active profile to delete: " + activeProfileId);
        System.out.println("Profiles before deletion: " + profilesBefore.size());
        
        try {
            // Delete the active profile
            boolean deleteResult = profileManager.removeProfile(activeProfileId);
            assertTrue(deleteResult, "Active profile deletion should succeed");
            
            // Verify profile was removed from list
            List<DatabaseProfile> profilesAfter = profileManager.getAllProfiles();
            assertEquals(profilesBefore.size() - 1, profilesAfter.size(), 
                        "Profile count should decrease by 1");
            
            assertFalse(profilesAfter.stream().anyMatch(p -> p.getId().equals(activeProfileId)),
                       "Deleted profile should not appear in profile list");
            
            // Verify a different profile is now active
            DatabaseProfile newActiveProfile = profileManager.getActiveProfile();
            assertNotNull(newActiveProfile, "Should automatically switch to another profile");
            assertNotEquals(activeProfileId, newActiveProfile.getId(), 
                           "Active profile should be different from deleted profile");
            
            System.out.println("✓ Active profile deleted successfully");
            System.out.println("✓ Automatically switched to profile: " + newActiveProfile.getId());
            System.out.println("✓ Profile count after deletion: " + profilesAfter.size());
            
        } catch (Exception e) {
            fail("Failed to delete active profile: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Should prevent deletion of last profile")
    void testCannotDeleteLastProfile() {
        System.out.println("\n=== Testing Last Profile Protection ===");
        
        // First, reduce to only one profile by deleting others (if possible)
        List<DatabaseProfile> allProfiles = profileManager.getAllProfiles();
        System.out.println("Starting profiles: " + allProfiles.size());
        
        // Keep deleting profiles until we have only one left
        while (allProfiles.size() > 1) {
            DatabaseProfile profileToDelete = allProfiles.get(allProfiles.size() - 1);
            try {
                boolean deleted = profileManager.removeProfile(profileToDelete.getId());
                if (deleted) {
                    System.out.println("Deleted profile to reach single profile state: " + profileToDelete.getId());
                }
            } catch (Exception e) {
                System.out.println("Could not delete profile: " + e.getMessage());
                break;
            }
            allProfiles = profileManager.getAllProfiles();
        }
        
        assertEquals(1, allProfiles.size(), "Should have exactly one profile for this test");
        
        DatabaseProfile lastProfile = allProfiles.get(0);
        String lastProfileId = lastProfile.getId();
        
        System.out.println("Last remaining profile: " + lastProfileId);
        
        try {
            // Attempt to delete the last profile - this should fail
            boolean deleteResult = profileManager.removeProfile(lastProfileId);
            assertFalse(deleteResult, "Should not be able to delete the last profile");
            
            // Verify profile still exists
            List<DatabaseProfile> profilesAfter = profileManager.getAllProfiles();
            assertEquals(1, profilesAfter.size(), "Should still have exactly one profile");
            assertEquals(lastProfileId, profilesAfter.get(0).getId(), 
                        "Last profile should still exist");
            
            // Verify it's still the active profile
            DatabaseProfile activeProfile = profileManager.getActiveProfile();
            assertNotNull(activeProfile, "Should still have an active profile");
            assertEquals(lastProfileId, activeProfile.getId(), 
                        "Last profile should remain active");
            
            System.out.println("✓ Last profile protection working correctly");
            System.out.println("✓ Last profile preserved: " + lastProfileId);
            
        } catch (Exception e) {
            // This is actually expected behavior - the method might throw an exception
            System.out.println("✓ Last profile deletion prevented with exception: " + e.getMessage());
            
            // Verify profile still exists after exception
            List<DatabaseProfile> profilesAfter = profileManager.getAllProfiles();
            assertEquals(1, profilesAfter.size(), "Should still have exactly one profile after exception");
        }
    }
    
    @Test
    @DisplayName("Should handle profile not found gracefully")
    void testDeleteNonExistentProfile() {
        System.out.println("\n=== Testing Non-Existent Profile Deletion ===");
        
        String nonExistentProfileId = "non-existent-profile-" + System.currentTimeMillis();
        
        List<DatabaseProfile> profilesBefore = profileManager.getAllProfiles();
        System.out.println("Profiles before: " + profilesBefore.size());
        
        try {
            // Attempt to delete non-existent profile
            boolean deleteResult = profileManager.removeProfile(nonExistentProfileId);
            assertFalse(deleteResult, "Should return false for non-existent profile");
            
            // Verify no profiles were affected
            List<DatabaseProfile> profilesAfter = profileManager.getAllProfiles();
            assertEquals(profilesBefore.size(), profilesAfter.size(), 
                        "Profile count should remain unchanged");
            
            System.out.println("✓ Non-existent profile deletion handled gracefully");
            
        } catch (Exception e) {
            // This is also acceptable behavior - method might throw exception
            System.out.println("✓ Non-existent profile deletion handled with exception: " + e.getMessage());
            
            // Verify no profiles were affected
            List<DatabaseProfile> profilesAfter = profileManager.getAllProfiles();
            assertEquals(profilesBefore.size(), profilesAfter.size(), 
                        "Profile count should remain unchanged after exception");
        }
    }
    
    /**
     * Helper method to create a temporary profile for testing.
     */
    private void createTemporaryProfile(String suffix) {
        try {
            String profileName = "Temporary Test Profile " + suffix + " " + System.currentTimeMillis();
            String description = "Temporary profile for testing";
            String databasePath = "temp-test-db-" + suffix + "-" + System.currentTimeMillis();
            
            DatabaseProfile profile = profileManager.createProfile(profileName, databasePath, description);
            System.out.println("Created temporary profile: " + profile.getId());
            
        } catch (Exception e) {
            System.err.println("Failed to create temporary profile: " + e.getMessage());
        }
    }
}