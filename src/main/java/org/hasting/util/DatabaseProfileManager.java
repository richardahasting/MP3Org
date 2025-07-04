package org.hasting.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

/**
 * Manages multiple database profiles and handles profile switching.
 */
public class DatabaseProfileManager {
    
    private static Logger logger;
    private static final String PROFILES_CONFIG_FILE = "mp3org-profiles.properties";
    
    /**
     * Gets the logger instance, initializing it lazily if needed.
     * Since logging is now initialized early, this should always work.
     */
    private static Logger getLogger() {
        if (logger == null) {
            logger = MP3OrgLoggingManager.getLogger(DatabaseProfileManager.class);
        }
        return logger;
    }
    
    private static final String ACTIVE_PROFILE_KEY = "active.profile.id";
    private static final String PROFILE_PREFIX = "profile.";
    
    private static DatabaseProfileManager instance;
    private Map<String, DatabaseProfile> profiles;
    private String activeProfileId;
    private Properties profilesProperties;
    
    private DatabaseProfileManager() {
        profiles = new LinkedHashMap<>();
        profilesProperties = new Properties();
        loadProfiles();
    }
    
    /**
     * Gets the singleton instance of DatabaseProfileManager.
     */
    public static synchronized DatabaseProfileManager getInstance() {
        if (instance == null) {
            instance = new DatabaseProfileManager();
        }
        return instance;
    }
    
    /**
     * Loads all profiles from the configuration file.
     */
    private void loadProfiles() {
        loadProfilesFromFile();
        
        // Ensure we have at least a default profile
        if (profiles.isEmpty()) {
            DatabaseProfile defaultProfile = DatabaseProfile.createDefault();
            profiles.put(defaultProfile.getId(), defaultProfile);
            activeProfileId = defaultProfile.getId();
            saveProfiles();
        } else if (activeProfileId == null || !profiles.containsKey(activeProfileId)) {
            // Set first profile as active if no valid active profile
            activeProfileId = profiles.keySet().iterator().next();
        }
    }
    
    /**
     * Loads profiles from the configuration file.
     */
    private void loadProfilesFromFile() {
        Path configPath = Paths.get(PROFILES_CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return;
        }
        
        try (InputStream input = Files.newInputStream(configPath)) {
            profilesProperties.load(input);
            
            // Load active profile ID
            activeProfileId = profilesProperties.getProperty(ACTIVE_PROFILE_KEY);
            
            // Group properties by profile ID
            Map<String, Properties> profilePropsMap = new HashMap<>();
            
            for (String key : profilesProperties.stringPropertyNames()) {
                if (key.startsWith(PROFILE_PREFIX)) {
                    String remainder = key.substring(PROFILE_PREFIX.length());
                    int dotIndex = remainder.indexOf('.');
                    if (dotIndex > 0) {
                        String profileId = remainder.substring(0, dotIndex);
                        String propKey = remainder.substring(dotIndex + 1);
                        
                        profilePropsMap.computeIfAbsent(profileId, k -> new Properties())
                                .setProperty(propKey, profilesProperties.getProperty(key));
                    }
                }
            }
            
            // Create profile objects
            for (Map.Entry<String, Properties> entry : profilePropsMap.entrySet()) {
                try {
                    DatabaseProfile profile = DatabaseProfile.fromProperties(entry.getValue());
                    profiles.put(profile.getId(), profile);
                } catch (Exception e) {
                    getLogger().warning("Could not load profile {}: {}", entry.getKey(), e.getMessage());
                }
            }
            
            getLogger().info("Loaded {} database profiles", profiles.size());
            
        } catch (IOException e) {
            getLogger().warning("Could not load profiles configuration: {}", e.getMessage());
        }
    }
    
    /**
     * Saves all profiles to the configuration file.
     */
    public synchronized void saveProfiles() {
        try {
            profilesProperties.clear();
            
            // Save active profile ID
            if (activeProfileId != null) {
                profilesProperties.setProperty(ACTIVE_PROFILE_KEY, activeProfileId);
            }
            
            // Save each profile
            for (DatabaseProfile profile : profiles.values()) {
                Properties profileProps = profile.toProperties();
                for (String key : profileProps.stringPropertyNames()) {
                    String fullKey = PROFILE_PREFIX + profile.getId() + "." + key;
                    profilesProperties.setProperty(fullKey, profileProps.getProperty(key));
                }
            }
            
            // Write to file
            Path configPath = Paths.get(PROFILES_CONFIG_FILE);
            try (OutputStream output = Files.newOutputStream(configPath)) {
                profilesProperties.store(output, "MP3Org Database Profiles Configuration");
                getLogger().info("Saved {} profiles to: {}", profiles.size(), configPath.toAbsolutePath());
            }
            
        } catch (IOException e) {
            getLogger().warning("Could not save profiles configuration: {}", e.getMessage());
        }
    }
    
    /**
     * Gets all available profiles.
     */
    public List<DatabaseProfile> getAllProfiles() {
        return new ArrayList<>(profiles.values());
    }
    
    /**
     * Gets all profile names.
     */
    public List<String> getProfileNames() {
        return profiles.values().stream()
                .map(DatabaseProfile::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a profile by ID.
     */
    public DatabaseProfile getProfile(String profileId) {
        return profiles.get(profileId);
    }
    
    /**
     * Gets a profile by name.
     */
    public DatabaseProfile getProfileByName(String name) {
        return profiles.values().stream()
                .filter(profile -> profile.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the currently active profile.
     */
    public DatabaseProfile getActiveProfile() {
        return profiles.get(activeProfileId);
    }
    
    /**
     * Gets the active profile ID.
     */
    public String getActiveProfileId() {
        return activeProfileId;
    }
    
    /**
     * Adds a new profile.
     */
    public synchronized void addProfile(DatabaseProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        
        // Ensure unique ID
        String originalId = profile.getId();
        String uniqueId = originalId;
        int counter = 1;
        while (profiles.containsKey(uniqueId)) {
            uniqueId = originalId + "_" + counter++;
        }
        
        if (!uniqueId.equals(originalId)) {
            profile.setId(uniqueId);
        }
        
        profiles.put(profile.getId(), profile);
        saveProfiles();
        
        getLogger().info("Added profile: {} (ID: {})", profile.getName(), profile.getId());
    }
    
    /**
     * Updates an existing profile.
     */
    public synchronized void updateProfile(DatabaseProfile profile) {
        if (profile == null || !profiles.containsKey(profile.getId())) {
            throw new IllegalArgumentException("Profile does not exist");
        }
        
        profiles.put(profile.getId(), profile);
        saveProfiles();
        
        getLogger().info("Updated profile: {}", profile.getName());
    }
    
    /**
     * Removes a profile and its associated database files.
     */
    public synchronized boolean removeProfile(String profileId) {
        if (!profiles.containsKey(profileId)) {
            return false;
        }
        
        // Cannot remove the last profile
        if (profiles.size() <= 1) {
            throw new IllegalStateException("Cannot remove the last profile");
        }
        
        DatabaseProfile profileToRemove = profiles.get(profileId);
        String databasePath = profileToRemove.getDatabasePath();
        
        // If removing active profile, switch to another one first
        if (profileId.equals(activeProfileId)) {
            String newActiveId = profiles.keySet().stream()
                    .filter(id -> !id.equals(profileId))
                    .findFirst()
                    .orElse(null);
            
            if (newActiveId != null) {
                setActiveProfile(newActiveId);
            }
        }
        
        // Close any existing database connections to this database
        try {
            DatabaseManager.shutdown();
            getLogger().debug("Closed database connection before profile deletion");
        } catch (Exception e) {
            getLogger().warning("Could not close database connection before profile deletion: {}", e.getMessage());
        }
        
        // Remove profile from registry
        DatabaseProfile removed = profiles.remove(profileId);
        saveProfiles();
        
        // Delete the database directory and all its files
        boolean databaseDeleted = deleteDatabaseFiles(databasePath);
        
        getLogger().info("Removed profile: {}", removed != null ? removed.getName() : profileId);
        if (databaseDeleted) {
            getLogger().info("Successfully deleted database files at: {}", databasePath);
        } else {
            getLogger().warning("Could not delete database files at: {}", databasePath);
        }
        
        return true;
    }
    
    /**
     * Deletes the database directory and all its files.
     * Apache Derby databases are stored as directories with multiple files.
     */
    private boolean deleteDatabaseFiles(String databasePath) {
        if (databasePath == null || databasePath.trim().isEmpty()) {
            getLogger().warning("Cannot delete database files: path is null or empty");
            return false;
        }
        
        java.io.File databaseDir = new java.io.File(databasePath);
        if (!databaseDir.exists()) {
            // Database directory doesn't exist, consider this success
            getLogger().debug("Database directory does not exist: {}", databasePath);
            return true;
        }
        
        if (!databaseDir.isDirectory()) {
            // Path exists but is not a directory, try to delete as single file
            getLogger().debug("Database path is a file, not directory: {}", databasePath);
            boolean deleted = databaseDir.delete();
            if (deleted) {
                getLogger().debug("Successfully deleted database file: {}", databasePath);
            } else {
                getLogger().warning("Failed to delete database file: {}", databasePath);
            }
            return deleted;
        }
        
        // Recursively delete the database directory and all its contents
        getLogger().debug("Recursively deleting database directory: {}", databasePath);
        return deleteDirectoryRecursively(databaseDir);
    }
    
    /**
     * Recursively deletes a directory and all its contents.
     */
    private boolean deleteDirectoryRecursively(java.io.File directory) {
        if (directory == null || !directory.exists()) {
            return true;
        }
        
        if (directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (!deleteDirectoryRecursively(file)) {
                        getLogger().error("Failed to delete file: {}", file.getAbsolutePath());
                        return false;
                    }
                }
            }
        }
        
        boolean deleted = directory.delete();
        if (deleted) {
            getLogger().debug("Successfully deleted: {}", directory.getAbsolutePath());
        } else {
            getLogger().error("Failed to delete directory: {}", directory.getAbsolutePath());
        }
        return deleted;
    }
    
    /**
     * Sets the active profile.
     */
    public synchronized boolean setActiveProfile(String profileId) {
        if (!profiles.containsKey(profileId)) {
            return false;
        }
        
        String oldActiveId = activeProfileId;
        DatabaseProfile oldProfile = oldActiveId != null ? profiles.get(oldActiveId) : null;
        String oldDatabasePath = oldProfile != null ? oldProfile.getDatabasePath() : null;
        
        activeProfileId = profileId;
        
        // Update last used date
        DatabaseProfile activeProfile = profiles.get(profileId);
        activeProfile.updateLastUsedDate();
        
        saveProfiles();
        
        getLogger().info("Switched from profile {} to {}", oldActiveId, profileId);
        
        // Notify listeners of profile change
        ProfileChangeNotifier.getInstance().notifyProfileChanged(oldActiveId, profileId, activeProfile);
        
        // Check if database path changed and notify accordingly
        String newDatabasePath = activeProfile.getDatabasePath();
        if (oldDatabasePath == null || !oldDatabasePath.equals(newDatabasePath)) {
            // Check if the new database is empty/new
            boolean isNewDatabase = isDatabaseEmpty(newDatabasePath);
            ProfileChangeNotifier.getInstance().notifyDatabaseChanged(oldDatabasePath, newDatabasePath, isNewDatabase);
        }
        
        return true;
    }
    
    /**
     * Checks if a database is empty or new.
     */
    private boolean isDatabaseEmpty(String databasePath) {
        try {
            // Temporarily switch to check the database
            List<org.hasting.model.MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
            return allFiles == null || allFiles.isEmpty();
        } catch (Exception e) {
            // If we can't read the database, assume it's new
            return true;
        }
    }
    
    /**
     * Sets the active profile by name.
     */
    public synchronized boolean setActiveProfileByName(String profileName) {
        DatabaseProfile profile = getProfileByName(profileName);
        if (profile != null) {
            return setActiveProfile(profile.getId());
        }
        return false;
    }
    
    /**
     * Creates a new profile with the given name and database path.
     */
    public DatabaseProfile createProfile(String name, String databasePath) {
        return createProfile(name, databasePath, "");
    }
    
    /**
     * Creates a new profile with the given name, database path, and description.
     */
    public DatabaseProfile createProfile(String name, String databasePath, String description) {
        DatabaseProfile profile = new DatabaseProfile(null, name, databasePath);
        profile.setDescription(description);
        addProfile(profile);
        return profile;
    }
    
    /**
     * Duplicates an existing profile with a new name.
     */
    public DatabaseProfile duplicateProfile(String sourceProfileId, String newName) {
        DatabaseProfile sourceProfile = profiles.get(sourceProfileId);
        if (sourceProfile == null) {
            throw new IllegalArgumentException("Source profile does not exist");
        }
        
        DatabaseProfile newProfile = sourceProfile.copy(newName);
        addProfile(newProfile);
        return newProfile;
    }
    
    /**
     * Gets profile statistics.
     */
    public String getProfilesInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Database Profiles (").append(profiles.size()).append("):\n");
        info.append("  Active Profile: ").append(activeProfileId).append("\n\n");
        
        for (DatabaseProfile profile : profiles.values()) {
            boolean isActive = profile.getId().equals(activeProfileId);
            info.append(isActive ? "* " : "  ");
            info.append(profile.getName()).append(" (").append(profile.getId()).append(")\n");
            info.append("    Path: ").append(profile.getDatabasePath()).append("\n");
            info.append("    File Types: ").append(profile.getEnabledFileTypes().size()).append("/").append(DatabaseConfig.getAllSupportedTypes().length).append("\n");
            if (profile.getDescription() != null && !profile.getDescription().trim().isEmpty()) {
                info.append("    Description: ").append(profile.getDescription()).append("\n");
            }
            info.append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Checks if a profile name already exists.
     */
    public boolean isProfileNameExists(String name) {
        return profiles.values().stream()
                .anyMatch(profile -> profile.getName().equals(name));
    }
    
    /**
     * Generates a unique profile name based on the given base name.
     */
    public String generateUniqueProfileName(String baseName) {
        if (!isProfileNameExists(baseName)) {
            return baseName;
        }
        
        int counter = 1;
        String uniqueName;
        do {
            uniqueName = baseName + " (" + counter++ + ")";
        } while (isProfileNameExists(uniqueName));
        
        return uniqueName;
    }
    
    /**
     * Validates a profile name.
     */
    public static boolean isValidProfileName(String name) {
        return name != null && 
               !name.trim().isEmpty() && 
               name.length() <= 100 &&
               !name.contains("\n") && 
               !name.contains("\r");
    }
    
    // ================================================================================================
    // DATABASE LOCK FALLBACK METHODS
    // Following self-documenting code philosophy: method names teach the fallback strategy
    // ================================================================================================
    
    /**
     * Attempts to activate the preferred profile, with automatic fallback to
     * available alternatives if the primary database is locked.
     * 
     * <p>This method implements the core database lock fallback strategy that ensures
     * the MP3Org application can always start successfully, even when multiple instances
     * are running or when the preferred database is unavailable.
     * 
     * <p><strong>Fallback Strategy (applied in order):</strong>
     * <ol>
     *   <li><strong>Try preferred profile:</strong> Attempt to use the requested profile</li>
     *   <li><strong>Scan existing profiles:</strong> Look for any unlocked alternative profiles</li>
     *   <li><strong>Create temporary profile:</strong> Generate a new profile if all others are locked</li>
     *   <li><strong>Notify user:</strong> Inform about any profile changes made automatically</li>
     * </ol>
     * 
     * <p>This method embodies the principle that "good code teaches its patterns" - 
     * future developers can understand the entire fallback strategy by reading this
     * method and following its clear delegation to helper methods.
     * 
     * @param preferredProfileId the ID of the profile the user wants to use
     * @return the database profile that was successfully activated (may be different from preferred)
     * @throws IllegalArgumentException if preferredProfileId is null
     * @throws RuntimeException if no fallback options are available (extremely rare)
     */
    public synchronized DatabaseProfile activateProfileWithAutomaticFallback(String preferredProfileId) {
        if (preferredProfileId == null) {
            throw new IllegalArgumentException("Preferred profile ID cannot be null");
        }
        
        DatabaseProfile preferredProfile = getProfile(preferredProfileId);
        if (preferredProfile == null) {
            getLogger().warning("Preferred profile '{}' not found, searching for alternatives", preferredProfileId);
            return activateFirstAvailableProfileOrCreateTemporary();
        }
        
        // Step 1: Try the preferred profile first
        if (DatabaseConnectionManager.isDatabaseAvailableForProfile(preferredProfile)) {
            boolean activated = setActiveProfile(preferredProfileId);
            if (activated) {
                getLogger().info("Successfully activated preferred profile: {}", preferredProfile.getName());
                return preferredProfile;
            }
        }
        
        // Step 2: Look for alternative existing profiles
        DatabaseProfile fallbackProfile = findFirstAvailableProfile();
        if (fallbackProfile != null) {
            return activateProfileWithUserNotification(fallbackProfile, preferredProfile);
        }
        
        // Step 3: Create temporary profile as last resort
        return createAndActivateTemporaryProfile(preferredProfile);
    }
    
    /**
     * Searches existing profiles to find the first one with an unlocked database.
     * 
     * <p>This method implements a simple but effective strategy: iterate through all
     * existing profiles and test each one until finding an available database. The
     * method name clearly communicates its purpose and return behavior.
     * 
     * <p>Profiles are tested in the order they appear in the profiles map, which
     * typically corresponds to their creation order. This provides predictable
     * fallback behavior that users can understand and rely upon.
     * 
     * @return the first profile with an available database, or null if all are locked
     */
    private DatabaseProfile findFirstAvailableProfile() {
        for (DatabaseProfile profile : profiles.values()) {
            if (DatabaseConnectionManager.isDatabaseAvailableForProfile(profile)) {
                getLogger().info("Found available fallback profile: {}", profile.getName());
                return profile;
            }
        }
        
        getLogger().warning("No existing profiles have available databases");
        return null;
    }
    
    /**
     * Activates an alternative profile and notifies the user about the automatic change.
     * 
     * <p>This method handles the communication aspect of automatic profile switching.
     * It activates the fallback profile and provides clear information to the user
     * about what happened and why, maintaining transparency in the automatic fallback process.
     * 
     * @param fallbackProfile the profile to activate as an alternative
     * @param originalProfile the profile that was originally requested but unavailable
     * @return the activated fallback profile
     */
    private DatabaseProfile activateProfileWithUserNotification(DatabaseProfile fallbackProfile, 
                                                                DatabaseProfile originalProfile) {
        boolean activated = setActiveProfile(fallbackProfile.getId());
        if (activated) {
            getLogger().warning("Automatically switched to profile '{}' because '{}' database was locked", 
                             fallbackProfile.getName(), originalProfile.getName());
            getLogger().info("You can switch back to '{}' later when it becomes available", 
                             originalProfile.getName());
            return fallbackProfile;
        } else {
            throw new RuntimeException("Failed to activate fallback profile: " + fallbackProfile.getName());
        }
    }
    
    /**
     * Creates a temporary profile with a unique database path when all existing profiles are locked.
     * 
     * <p>This method serves as the final fallback option to ensure the application can always start.
     * It creates a completely new profile with a unique database path, guaranteeing that no lock
     * conflicts will occur. The temporary profile inherits configuration settings from the original
     * preferred profile to maintain consistency in user experience.
     * 
     * <p><strong>Temporary Profile Characteristics:</strong>
     * <ul>
     *   <li>Unique database path with timestamp to avoid conflicts</li>
     *   <li>Clear naming that indicates its temporary/fallback nature</li>
     *   <li>Inherits file type and search configurations from original profile</li>
     *   <li>Automatically saved to profiles configuration for user access</li>
     * </ul>
     * 
     * @param originalProfile the profile that was originally requested but unavailable
     * @return the newly created and activated temporary profile
     */
    private DatabaseProfile createAndActivateTemporaryProfile(DatabaseProfile originalProfile) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String tempDbPath = "temp-fallback-" + timestamp;
        
        DatabaseProfile tempProfile = new DatabaseProfile("temp_fallback_" + timestamp, "Temporary Fallback Profile", tempDbPath);
        tempProfile.setDescription("Auto-created when '" + originalProfile.getName() + 
                                   "' was locked by another MP3Org instance");
        
        // Inherit configuration settings from the original profile to maintain consistency
        tempProfile.setEnabledFileTypes(originalProfile.getEnabledFileTypes());
        tempProfile.setFuzzySearchConfig(originalProfile.getFuzzySearchConfig());
        
        // Add and activate the temporary profile
        addProfile(tempProfile);
        boolean activated = setActiveProfile(tempProfile.getId());
        
        if (activated) {
            getLogger().warning("Created temporary profile '{}' with database at: {}", 
                             tempProfile.getName(), tempDbPath);
            getLogger().info("This temporary profile will be available for future use");
            return tempProfile;
        } else {
            throw new RuntimeException("Failed to activate temporary profile: " + tempProfile.getName());
        }
    }
    
    /**
     * Convenience method to activate any available profile or create a temporary one.
     * 
     * <p>This method implements the fallback strategy when no preferred profile is specified
     * or when the preferred profile doesn't exist. It demonstrates the same pattern as
     * the main fallback method but without a specific preferred profile to fall back from.
     * 
     * @return an activated database profile (either existing or newly created)
     */
    private DatabaseProfile activateFirstAvailableProfileOrCreateTemporary() {
        DatabaseProfile available = findFirstAvailableProfile();
        if (available != null) {
            boolean activated = setActiveProfile(available.getId());
            if (activated) {
                getLogger().info("Activated first available profile: {}", available.getName());
                return available;
            }
        }
        
        // Create a basic temporary profile when no existing profiles are available
        DatabaseProfile tempProfile = createBasicTemporaryProfile();
        addProfile(tempProfile);
        setActiveProfile(tempProfile.getId());
        
        getLogger().warning("Created basic temporary profile as no alternatives were available");
        return tempProfile;
    }
    
    /**
     * Creates a basic temporary profile with default settings.
     * 
     * <p>This method creates a minimal temporary profile when no existing profiles
     * are available to inherit settings from. It uses safe defaults that work
     * for most MP3Org use cases.
     * 
     * @return a new temporary profile with default configuration
     */
    private DatabaseProfile createBasicTemporaryProfile() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String tempDbPath = "temp-basic-" + timestamp;
        
        DatabaseProfile tempProfile = new DatabaseProfile("temp_basic_" + timestamp, "Temporary Basic Profile", tempDbPath);
        tempProfile.setDescription("Auto-created temporary profile with default settings");
        
        return tempProfile;
    }
    
    /**
     * Tests if the currently active profile's database is available.
     * 
     * <p>This convenience method provides a simple way to check if the current
     * profile can be used without lock conflicts. It's useful for periodic
     * checks or user interface status updates.
     * 
     * @return true if the active profile's database is available, false if locked
     */
    public boolean isActiveProfileDatabaseAvailable() {
        DatabaseProfile activeProfile = getActiveProfile();
        return activeProfile != null && 
               DatabaseConnectionManager.isDatabaseAvailableForProfile(activeProfile);
    }
    
    /**
     * Attempts to return to a preferred profile if it becomes available.
     * 
     * <p>This method supports the user experience of automatically returning to
     * a preferred profile once it becomes available (e.g., when another MP3Org
     * instance is closed). It can be called periodically or in response to user
     * requests to "retry preferred profile".
     * 
     * @param preferredProfileId the profile to attempt to return to
     * @return true if successfully switched back to preferred profile, false if still locked
     */
    public synchronized boolean attemptReturnToPreferredProfile(String preferredProfileId) {
        if (preferredProfileId == null || preferredProfileId.equals(activeProfileId)) {
            return true; // Already using preferred profile or no preference specified
        }
        
        DatabaseProfile preferredProfile = getProfile(preferredProfileId);
        if (preferredProfile == null) {
            return false; // Preferred profile no longer exists
        }
        
        if (DatabaseConnectionManager.isDatabaseAvailableForProfile(preferredProfile)) {
            boolean switched = setActiveProfile(preferredProfileId);
            if (switched) {
                getLogger().info("Successfully returned to preferred profile: {}", preferredProfile.getName());
                return true;
            }
        }
        
        return false; // Preferred profile still locked or activation failed
    }
}