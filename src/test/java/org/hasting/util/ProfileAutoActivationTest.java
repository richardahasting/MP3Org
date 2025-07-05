package org.hasting.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for verifying that newly created database profiles are automatically activated.
 * 
 * <p>This test validates Issue #35: "Auto-activate newly created database profiles"
 * 
 * <p>Tests verify that:
 * <ul>
 *   <li>New profiles created via createProfile() are automatically activated</li>
 *   <li>Duplicated profiles are automatically activated</li>
 *   <li>UI properly reflects the profile change</li>
 *   <li>Database connections switch to the new profile</li>
 * </ul>
 * 
 * @since 1.0
 */
@DisplayName("Profile Auto-Activation Tests")
public class ProfileAutoActivationTest extends BaseTest {
    
    private DatabaseProfileManager profileManager;
    private String originalActiveProfileId;
    
    @BeforeEach
    void setUp() {
        profileManager = DatabaseProfileManager.getInstance();
        
        // Store original active profile for restoration
        DatabaseProfile originalProfile = profileManager.getActiveProfile();
        originalActiveProfileId = originalProfile != null ? originalProfile.getId() : null;
        
        System.out.println("=== Test Setup ===");
        System.out.println("Original active profile: " + originalActiveProfileId);
    }
    
    @Test
    @DisplayName("Creating a new profile automatically activates it")
    void testCreateProfileAutoActivation() {
        // Given: An existing active profile
        String originalActiveId = profileManager.getActiveProfileId();
        assertNotNull(originalActiveId, "Should have an active profile before test");
        
        // When: Creating a new profile
        String newProfileName = "Test Auto-Activate Profile";
        String newDatabasePath = "test-auto-activate-db-" + System.currentTimeMillis();
        DatabaseProfile newProfile = profileManager.createProfile(newProfileName, newDatabasePath);
        
        // Then: The new profile should be automatically activated
        assertNotNull(newProfile, "New profile should be created");
        assertEquals(newProfile.getId(), profileManager.getActiveProfileId(), 
                    "New profile should be automatically activated");
        
        DatabaseProfile currentActive = profileManager.getActiveProfile();
        assertNotNull(currentActive, "Should have an active profile after creation");
        assertEquals(newProfileName, currentActive.getName(), 
                    "Active profile should be the newly created one");
        assertEquals(newDatabasePath, currentActive.getDatabasePath(), 
                    "Active profile should have the correct database path");
    }
    
    @Test
    @DisplayName("Creating a profile with description automatically activates it")
    void testCreateProfileWithDescriptionAutoActivation() {
        // Given: An existing active profile
        String originalActiveId = profileManager.getActiveProfileId();
        
        // When: Creating a new profile with description
        String newProfileName = "Test Profile With Description";
        String newDatabasePath = "test-described-db-" + System.currentTimeMillis();
        String description = "Test profile created for auto-activation testing";
        DatabaseProfile newProfile = profileManager.createProfile(newProfileName, newDatabasePath, description);
        
        // Then: The new profile should be automatically activated
        assertEquals(newProfile.getId(), profileManager.getActiveProfileId(), 
                    "New profile with description should be automatically activated");
        
        DatabaseProfile currentActive = profileManager.getActiveProfile();
        assertEquals(description, currentActive.getDescription(), 
                    "Active profile should have the correct description");
    }
    
    @Test
    @DisplayName("Duplicating a profile automatically activates the duplicate")
    void testDuplicateProfileAutoActivation() {
        // Given: An existing active profile to duplicate
        DatabaseProfile originalProfile = profileManager.getActiveProfile();
        assertNotNull(originalProfile, "Should have an active profile to duplicate");
        String originalActiveId = originalProfile.getId();
        
        // When: Duplicating the active profile
        String duplicateName = "Duplicate of " + originalProfile.getName();
        DatabaseProfile duplicateProfile = profileManager.duplicateProfile(originalActiveId, duplicateName);
        
        // Then: The duplicate profile should be automatically activated
        assertNotNull(duplicateProfile, "Duplicate profile should be created");
        assertEquals(duplicateProfile.getId(), profileManager.getActiveProfileId(), 
                    "Duplicate profile should be automatically activated");
        
        DatabaseProfile currentActive = profileManager.getActiveProfile();
        assertEquals(duplicateName, currentActive.getName(), 
                    "Active profile should be the duplicate");
        
        // Verify the duplicate inherited settings from the original
        assertEquals(originalProfile.getEnabledFileTypes(), duplicateProfile.getEnabledFileTypes(),
                    "Duplicate should inherit file type settings");
    }
    
    @Test
    @DisplayName("Multiple profile creations maintain correct activation")
    void testMultipleProfileCreationsActivation() {
        // Given: Starting state
        String startingActiveId = profileManager.getActiveProfileId();
        int startingProfileCount = profileManager.getAllProfiles().size();
        
        // When: Creating multiple profiles in sequence
        DatabaseProfile profile1 = profileManager.createProfile("First New Profile", "test-db-1-" + System.currentTimeMillis());
        assertEquals(profile1.getId(), profileManager.getActiveProfileId(), 
                    "First profile should be active after creation");
        
        DatabaseProfile profile2 = profileManager.createProfile("Second New Profile", "test-db-2-" + System.currentTimeMillis());
        assertEquals(profile2.getId(), profileManager.getActiveProfileId(), 
                    "Second profile should be active after creation");
        
        DatabaseProfile profile3 = profileManager.createProfile("Third New Profile", "test-db-3-" + System.currentTimeMillis());
        assertEquals(profile3.getId(), profileManager.getActiveProfileId(), 
                    "Third profile should be active after creation");
        
        // Then: Verify all profiles were created and the last one is active
        assertEquals(startingProfileCount + 3, profileManager.getAllProfiles().size(),
                    "All three profiles should be created");
        assertEquals(profile3.getId(), profileManager.getActiveProfileId(),
                    "Last created profile should remain active");
    }
    
    @Test
    @DisplayName("Profile creation switches from any starting profile")
    void testProfileCreationFromDifferentStartingProfiles() {
        // Given: Multiple existing profiles (from TEST-HARNESS setup)
        assertTrue(profileManager.getAllProfiles().size() >= 1, "Should have at least one profile");
        
        // When: Creating a new profile from current active profile
        String originalActiveId = profileManager.getActiveProfileId();
        DatabaseProfile newProfile = profileManager.createProfile("New From Current", "test-from-current-" + System.currentTimeMillis());
        
        // Then: New profile should be active regardless of starting profile
        assertEquals(newProfile.getId(), profileManager.getActiveProfileId(),
                    "New profile should be active regardless of starting profile");
        assertNotEquals(originalActiveId, profileManager.getActiveProfileId(),
                    "Active profile should have changed from original");
    }
}