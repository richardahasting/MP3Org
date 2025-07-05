package org.hasting.util;

import org.hasting.model.MusicFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Test harness utility for managing test database profiles and data.
 * 
 * <p>This class provides a standardized testing infrastructure that:
 * <ul>
 * <li>Creates a dedicated TEST-HARNESS profile for all tests</li>
 * <li>Imports known test data from /Users/richard/mp3s directory</li>
 * <li>Isolates tests from user's production profiles</li>
 * <li>Provides automatic cleanup of test artifacts</li>
 * </ul>
 * 
 * <p>Usage in test classes:
 * <pre>{@code
 * @BeforeAll
 * static void setupTestHarness() {
 *     TestHarness.setupTestingProfile();
 * }
 * 
 * @AfterAll
 * static void cleanupTestHarness() {
 *     TestHarness.cleanup();
 * }
 * }</pre>
 * 
 * <p>The test harness ensures consistent, predictable test data and prevents
 * tests from interfering with user's production database profiles.
 * 
 * @see DatabaseProfileManager
 * @see DatabaseManager
 * @since 1.0
 */
public class TestHarness {
    
    private static final String TEST_PROFILE_NAME = "TEST-HARNESS";
    private static final String TEST_PROFILE_DESCRIPTION = "Dedicated test profile with standardized test data";
    private static final String TEST_MP3_DIRECTORY = "/Users/richard/mp3s";
    
    private static String originalActiveProfileId;
    private static String testProfileId;
    private static final List<String> profilesCreatedDuringTesting = new ArrayList<>();
    
    /**
     * Sets up the testing profile and imports test data.
     * This should be called in @BeforeAll of test classes.
     */
    public static void setupTestingProfile() {
        try {
            System.out.println("Setting up TEST-HARNESS profile...");
            
            // Save the current active profile to restore later
            DatabaseProfile currentProfile = DatabaseManager.getActiveProfile();
            if (currentProfile != null) {
                originalActiveProfileId = currentProfile.getId();
                System.out.println("Saved original active profile: " + currentProfile.getName());
            }
            
            DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
            
            // Check if TEST-HARNESS profile already exists
            DatabaseProfile existingTestProfile = null;
            for (DatabaseProfile profile : profileManager.getAllProfiles()) {
                if (TEST_PROFILE_NAME.equals(profile.getName())) {
                    existingTestProfile = profile;
                    break;
                }
            }
            
            if (existingTestProfile != null) {
                // Use existing test profile
                testProfileId = existingTestProfile.getId();
                System.out.println("Using existing TEST-HARNESS profile");
            } else {
                // Create new test profile with temporary database location
                String testDatabasePath = System.getProperty("java.io.tmpdir") + File.separator + "mp3org-test-harness";
                DatabaseProfile newProfile = profileManager.createProfile(TEST_PROFILE_NAME, testDatabasePath, TEST_PROFILE_DESCRIPTION);
                testProfileId = newProfile.getId();
                System.out.println("Created new TEST-HARNESS profile at: " + testDatabasePath);
            }
            
            // Switch to test profile
            profileManager.setActiveProfile(testProfileId);
            DatabaseManager.reloadConfig();
            
            // Import test data if the database is empty
            List<MusicFile> existingFiles = DatabaseManager.getAllMusicFiles();
            if (existingFiles.isEmpty()) {
                importTestData();
            } else {
                System.out.println("TEST-HARNESS profile already contains " + existingFiles.size() + " files");
            }
            
            System.out.println("TEST-HARNESS setup complete");
            
        } catch (Exception e) {
            System.err.println("Failed to setup testing profile: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Test setup failed", e);
        }
    }
    
    /**
     * Imports test data from the configured test MP3 directory.
     */
    private static void importTestData() {
        try {
            File testDir = new File(TEST_MP3_DIRECTORY);
            if (!testDir.exists() || !testDir.isDirectory()) {
                System.out.println("Test MP3 directory not found at: " + TEST_MP3_DIRECTORY);
                System.out.println("Continuing with empty test database");
                return;
            }
            
            System.out.println("Importing test data from: " + TEST_MP3_DIRECTORY);
            
            // Use MusicFileScanner to scan and import the test directory
            MusicFileScanner scanner = new MusicFileScanner();
            List<MusicFile> musicFiles = scanner.findAllMusicFiles(TEST_MP3_DIRECTORY);
            
            if (!musicFiles.isEmpty()) {
                // Save all files to the test database
                for (MusicFile musicFile : musicFiles) {
                    DatabaseManager.saveMusicFile(musicFile);
                }
                System.out.println("Imported " + musicFiles.size() + " test files into TEST-HARNESS");
            } else {
                System.out.println("No music files found in test directory");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to import test data: " + e.getMessage());
            // Don't fail the test setup if import fails - continue with empty database
        }
    }
    
    /**
     * Tracks a profile ID that was created during testing for cleanup.
     * 
     * @param profileId The profile ID to track for cleanup
     */
    public static void trackProfileForCleanup(String profileId) {
        if (profileId != null && !profilesCreatedDuringTesting.contains(profileId)) {
            profilesCreatedDuringTesting.add(profileId);
        }
    }
    
    /**
     * Gets the test profile ID for use in tests.
     * 
     * @return The TEST-HARNESS profile ID
     */
    public static String getTestProfileId() {
        return testProfileId;
    }
    
    /**
     * Gets the test profile name.
     * 
     * @return The TEST-HARNESS profile name
     */
    public static String getTestProfileName() {
        return TEST_PROFILE_NAME;
    }
    
    /**
     * Performs cleanup after tests complete.
     * Removes ALL test profiles except TEST-HARNESS and user's production profiles.
     * This should be called in @AfterAll of test classes.
     */
    public static void cleanup() {
        try {
            System.out.println("Starting test harness cleanup...");
            
            DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
            
            // Remove ALL additional profiles created during testing (but keep TEST-HARNESS)
            for (String profileId : profilesCreatedDuringTesting) {
                if (!profileId.equals(testProfileId)) {
                    try {
                        profileManager.removeProfile(profileId);
                        System.out.println("Removed test profile: " + profileId);
                    } catch (Exception e) {
                        System.err.println("Failed to remove profile " + profileId + ": " + e.getMessage());
                    }
                }
            }
            profilesCreatedDuringTesting.clear();
            
            // Restore original active profile if it still exists
            if (originalActiveProfileId != null) {
                try {
                    boolean restored = profileManager.setActiveProfile(originalActiveProfileId);
                    if (restored) {
                        DatabaseManager.reloadConfig();
                        System.out.println("Restored original active profile: " + originalActiveProfileId);
                    } else {
                        System.out.println("Original profile no longer exists, keeping TEST-HARNESS active");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to restore original profile: " + e.getMessage());
                }
            }
            
            System.out.println("Test harness cleanup complete");
            
        } catch (Exception e) {
            System.err.println("Error during test cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Performs comprehensive cleanup of ALL test profiles except TEST-HARNESS.
     * This method scans all profiles and removes any that appear to be test-related
     * while preserving user's production profiles and the TEST-HARNESS.
     * 
     * Use this for thorough cleanup after test runs that may have created
     * many temporary profiles.
     */
    public static void cleanupAllTestProfiles() {
        try {
            System.out.println("Starting comprehensive test profile cleanup...");
            
            DatabaseProfileManager profileManager = DatabaseManager.getProfileManager();
            List<DatabaseProfile> allProfiles = profileManager.getAllProfiles();
            
            int removedCount = 0;
            for (DatabaseProfile profile : allProfiles) {
                // Skip TEST-HARNESS profile
                if (TEST_PROFILE_NAME.equals(profile.getName())) {
                    continue;
                }
                
                // Identify test profiles by common patterns
                String name = profile.getName();
                String description = profile.getDescription();
                String path = profile.getDatabasePath();
                
                boolean isTestProfile = 
                    name.startsWith("Test ") ||
                    name.contains("test-") ||
                    name.contains("Test Profile") ||
                    name.contains("Alternative Profile") ||
                    name.contains("Availability Test") ||
                    name.contains("Temporary") ||
                    (description != null && (
                        description.contains("test") ||
                        description.contains("testing") ||
                        description.contains("fallback") ||
                        description.contains("Isolated")
                    )) ||
                    (path != null && (
                        path.contains("/tmp/") ||
                        path.contains("test-") ||
                        path.contains("temp-")
                    ));
                
                if (isTestProfile) {
                    try {
                        profileManager.removeProfile(profile.getId());
                        System.out.println("Removed test profile: " + name + " (" + profile.getId() + ")");
                        removedCount++;
                    } catch (Exception e) {
                        System.err.println("Failed to remove test profile " + name + ": " + e.getMessage());
                    }
                }
            }
            
            System.out.println("Comprehensive cleanup complete - removed " + removedCount + " test profiles");
            System.out.println("Preserved TEST-HARNESS and user production profiles");
            
        } catch (Exception e) {
            System.err.println("Error during comprehensive cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ensures the TEST-HARNESS profile is active. Useful for tests that need to verify
     * they're running in the correct test environment.
     */
    public static void ensureTestProfileActive() {
        DatabaseProfile activeProfile = DatabaseManager.getActiveProfile();
        if (activeProfile == null || !TEST_PROFILE_NAME.equals(activeProfile.getName())) {
            throw new IllegalStateException("TEST-HARNESS profile is not active. Current profile: " + 
                (activeProfile != null ? activeProfile.getName() : "None"));
        }
    }
    
    /**
     * Gets the count of music files in the test database.
     * 
     * @return Number of music files in the test database
     */
    public static int getTestDataCount() {
        return DatabaseManager.getAllMusicFiles().size();
    }
}