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
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(DatabaseProfileManager.class);
    private static final String PROFILES_CONFIG_FILE = "mp3org-profiles.properties";
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
                    System.err.println("Warning: Could not load profile " + entry.getKey() + ": " + e.getMessage());
                }
            }
            
            System.out.println("Loaded " + profiles.size() + " database profiles");
            
        } catch (IOException e) {
            System.err.println("Warning: Could not load profiles configuration: " + e.getMessage());
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
                System.out.println("Saved " + profiles.size() + " profiles to: " + configPath.toAbsolutePath());
            }
            
        } catch (IOException e) {
            System.err.println("Warning: Could not save profiles configuration: " + e.getMessage());
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
        
        System.out.println("Added profile: " + profile.getName() + " (ID: " + profile.getId() + ")");
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
        
        System.out.println("Updated profile: " + profile.getName());
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
            logger.debug("Closed database connection before profile deletion");
        } catch (Exception e) {
            logger.warning("Could not close database connection before profile deletion: {}", e.getMessage());
        }
        
        // Remove profile from registry
        DatabaseProfile removed = profiles.remove(profileId);
        saveProfiles();
        
        // Delete the database directory and all its files
        boolean databaseDeleted = deleteDatabaseFiles(databasePath);
        
        logger.info("Removed profile: {}", removed != null ? removed.getName() : profileId);
        if (databaseDeleted) {
            logger.info("Successfully deleted database files at: {}", databasePath);
        } else {
            logger.warning("Could not delete database files at: {}", databasePath);
        }
        
        return true;
    }
    
    /**
     * Deletes the database directory and all its files.
     * Apache Derby databases are stored as directories with multiple files.
     */
    private boolean deleteDatabaseFiles(String databasePath) {
        if (databasePath == null || databasePath.trim().isEmpty()) {
            logger.warning("Cannot delete database files: path is null or empty");
            return false;
        }
        
        java.io.File databaseDir = new java.io.File(databasePath);
        if (!databaseDir.exists()) {
            // Database directory doesn't exist, consider this success
            logger.debug("Database directory does not exist: {}", databasePath);
            return true;
        }
        
        if (!databaseDir.isDirectory()) {
            // Path exists but is not a directory, try to delete as single file
            logger.debug("Database path is a file, not directory: {}", databasePath);
            boolean deleted = databaseDir.delete();
            if (deleted) {
                logger.debug("Successfully deleted database file: {}", databasePath);
            } else {
                logger.warning("Failed to delete database file: {}", databasePath);
            }
            return deleted;
        }
        
        // Recursively delete the database directory and all its contents
        logger.debug("Recursively deleting database directory: {}", databasePath);
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
                        logger.error("Failed to delete file: {}", file.getAbsolutePath());
                        return false;
                    }
                }
            }
        }
        
        boolean deleted = directory.delete();
        if (deleted) {
            logger.debug("Successfully deleted: {}", directory.getAbsolutePath());
        } else {
            logger.error("Failed to delete directory: {}", directory.getAbsolutePath());
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
        
        System.out.println("Switched from profile " + oldActiveId + " to " + profileId);
        
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
}