package org.hasting.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Manages isolated database profiles for testing to prevent test interference
 * and ensure clean, predictable test environments.
 */
public class TestDatabaseProfileManager {
    private static final Logger logger = Logger.getLogger(TestDatabaseProfileManager.class.getName());
    
    /**
     * Creates an isolated test database profile with a unique path.
     * Each test run gets its own database to prevent interference.
     * 
     * @return DatabaseProfile configured for testing
     */
    public static DatabaseProfile createIsolatedTestProfile() {
        String testId = "test-" + System.currentTimeMillis() + "-" + 
                       Thread.currentThread().getId();
        String testPath = System.getProperty("java.io.tmpdir") + 
                         File.separator + "mp3org-test-" + testId;
        
        DatabaseProfileManager manager = DatabaseConfig.getInstance().getProfileManager();
        DatabaseProfile testProfile = manager.createProfile(
            "Test Profile " + testId, 
            testPath,
            "Isolated test database profile - " + testId
        );
        
        // Configure for testing with all supported file types
        testProfile.setEnabledFileTypes(Set.of("mp3", "flac", "wav", "ogg", "aac", "m4a", "wma", "aiff", "ape", "opus"));
        
        logger.info("Created isolated test profile: " + testProfile.getId() + " at path: " + testPath);
        
        return testProfile;
    }
    
    /**
     * Activates a test profile by adding it to the profile manager and switching to it.
     * 
     * @param testProfile The test profile to activate
     */
    public static void activateTestProfile(DatabaseProfile testProfile) {
        try {
            DatabaseProfileManager profileManager = DatabaseConfig.getInstance().getProfileManager();
            profileManager.addProfile(testProfile);
            boolean success = DatabaseConfig.getInstance().switchToProfile(testProfile.getId());
            
            if (!success) {
                throw new RuntimeException("Failed to switch to test profile: " + testProfile.getId());
            }
            
            logger.info("Activated test profile: " + testProfile.getId());
            
        } catch (Exception e) {
            logger.severe("Failed to activate test profile " + testProfile.getId() + ": " + e.getMessage());
            throw new RuntimeException("Test profile activation failed", e);
        }
    }
    
    /**
     * Cleans up a test profile by shutting down the database and removing files.
     * This ensures no test artifacts are left behind.
     * 
     * @param testProfile The test profile to clean up
     */
    public static void cleanupTestProfile(DatabaseProfile testProfile) {
        try {
            // Shutdown database connections
            DatabaseManager.shutdown();
            logger.info("Database shutdown for test profile: " + testProfile.getId());
            
            // Remove profile from manager
            DatabaseProfileManager profileManager = DatabaseConfig.getInstance().getProfileManager();
            profileManager.removeProfile(testProfile.getId());
            
            // Clean up database files
            cleanupTestDatabaseFiles(testProfile.getDatabasePath());
            
            logger.info("Cleaned up test profile: " + testProfile.getId());
            
        } catch (Exception e) {
            logger.warning("Error cleaning up test profile " + testProfile.getId() + ": " + e.getMessage());
            // Don't throw exception during cleanup - log and continue
        }
    }
    
    /**
     * Removes test database files from the filesystem.
     * 
     * @param databasePath The path to the test database to clean up
     */
    private static void cleanupTestDatabaseFiles(String databasePath) {
        try {
            Path dbPath = Paths.get(databasePath);
            if (Files.exists(dbPath)) {
                if (Files.isDirectory(dbPath)) {
                    FileUtils.deleteDirectory(dbPath.toFile());
                } else {
                    Files.delete(dbPath);
                }
                logger.fine("Deleted test database files at: " + databasePath);
            }
            
            // Also clean up Derby log files that might be created
            cleanupDerbyLogFiles();
            
        } catch (IOException e) {
            logger.warning("Could not clean up test database files at " + databasePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Cleans up Derby database log files that may be created during testing.
     */
    private static void cleanupDerbyLogFiles() {
        try {
            File currentDir = new File(".");
            File[] logFiles = currentDir.listFiles((dir, name) -> 
                name.startsWith("derby.log") || name.startsWith("derbyShutdownException.log"));
            
            if (logFiles != null) {
                for (File logFile : logFiles) {
                    if (logFile.delete()) {
                        logger.fine("Deleted Derby log file: " + logFile.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.fine("Could not clean up Derby log files: " + e.getMessage());
        }
    }
    
    /**
     * Creates a test profile with specific configuration for different test scenarios.
     * 
     * @param name Profile name
     * @param description Profile description
     * @param enabledFileTypes Set of file types to enable
     * @return Configured test profile
     */
    public static DatabaseProfile createCustomTestProfile(String name, String description, Set<String> enabledFileTypes) {
        String testId = "test-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + System.currentTimeMillis();
        String testPath = System.getProperty("java.io.tmpdir") + 
                         File.separator + "mp3org-" + testId;
        
        DatabaseProfileManager manager = DatabaseConfig.getInstance().getProfileManager();
        DatabaseProfile testProfile = manager.createProfile(name, testPath, description);
        
        testProfile.setEnabledFileTypes(enabledFileTypes);
        
        logger.info("Created custom test profile: " + testProfile.getId() + " with file types: " + enabledFileTypes);
        
        return testProfile;
    }
}