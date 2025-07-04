package org.hasting.util.logging;

import org.hasting.util.DatabaseConfig;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Central logging manager for MP3Org that integrates with the application's configuration system.
 * 
 * <p>This manager provides:</p>
 * <ul>
 *   <li>Integration with MP3Org's existing configuration system</li>
 *   <li>Profile-specific logging configuration</li>
 *   <li>Automatic initialization of logging on application startup</li>
 *   <li>Runtime logging configuration changes</li>
 *   <li>Convenient factory methods for common configurations</li>
 * </ul>
 * 
 * @since 1.0
 */
public class MP3OrgLoggingManager {
    
    private static final String LOGGING_CONFIG_FILE = "mp3org-logging.properties";
    private static volatile LoggingConfiguration currentConfig;
    private static volatile boolean initialized = false;
    
    // Prevent instantiation
    private MP3OrgLoggingManager() {}
    
    /**
     * Initializes the MP3Org logging system with default configuration.
     * This should be called early in the application startup process.
     */
    public static void initialize() {
        if (!initialized) {
            synchronized (MP3OrgLoggingManager.class) {
                if (!initialized) {
                    try {
                        currentConfig = loadConfiguration();
                        LoggerFactory.configure(currentConfig);
                        initialized = true;
                        
                        Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
                        logger.info("MP3Org logging system initialized");
                        
                    } catch (Exception e) {
                        // Fallback to console-only logging
                        currentConfig = LoggingConfiguration.createDefault();
                        LoggerFactory.configure(currentConfig);
                        initialized = true;
                        
                        System.err.println("Failed to load logging configuration, using defaults: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Configures logging for development mode with verbose output.
     */
    public static void configureForDevelopment() {
        currentConfig = LoggingConfiguration.createDevelopment();
        
        // Enable debug logging for MP3Org packages
        currentConfig.setLoggerLevel("org.hasting", LogLevel.DEBUG);
        currentConfig.setLoggerLevel("org.hasting.util.MusicFileScanner", LogLevel.DEBUG);
        currentConfig.setLoggerLevel("org.hasting.util.DatabaseManager", LogLevel.INFO);
        
        // Enable file logging in development
        currentConfig.setFileEnabled(true);
        currentConfig.setFilePath("mp3org/logs/mp3org-dev.log");
        
        LoggerFactory.configure(currentConfig);
        
        Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
        logger.info("Logging configured for development mode");
    }
    
    /**
     * Configures logging for production mode with appropriate levels.
     */
    public static void configureForProduction() {
        currentConfig = new LoggingConfiguration();
        currentConfig.setDefaultLevel(LogLevel.INFO);
        currentConfig.setConsoleEnabled(true);
        currentConfig.setFileEnabled(true);
        currentConfig.setFilePath("mp3org/logs/mp3org.log");
        
        // Set appropriate levels for production
        currentConfig.setLoggerLevel("org.hasting.util.DatabaseManager", LogLevel.WARNING);
        currentConfig.setLoggerLevel("org.hasting.ui", LogLevel.INFO);
        
        LoggerFactory.configure(currentConfig);
        
        Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
        logger.info("Logging configured for production mode");
    }
    
    /**
     * Configures minimal logging for release builds.
     */
    public static void configureForRelease() {
        currentConfig = new LoggingConfiguration();
        currentConfig.setDefaultLevel(LogLevel.WARNING);
        currentConfig.setConsoleEnabled(true);
        currentConfig.setFileEnabled(false); // No file logging in release mode
        
        LoggerFactory.configure(currentConfig);
        
        Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
        logger.info("Logging configured for release mode");
    }
    
    /**
     * Gets the current logging configuration.
     * 
     * @return The current configuration
     */
    public static LoggingConfiguration getCurrentConfiguration() {
        if (!initialized) {
            initialize();
        }
        return currentConfig;
    }
    
    /**
     * Updates the logging configuration and applies it immediately.
     * 
     * @param config The new configuration to apply
     */
    public static void updateConfiguration(LoggingConfiguration config) {
        currentConfig = config;
        LoggerFactory.configure(config);
        
        Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
        logger.info("Logging configuration updated");
    }
    
    /**
     * Sets the log level for a specific component.
     * 
     * @param component The component name (class or package)
     * @param level The new log level
     */
    public static void setComponentLogLevel(String component, LogLevel level) {
        if (!initialized) {
            initialize();
        }
        
        LoggerFactory.setLoggerLevel(component, level);
        if (currentConfig != null) {
            currentConfig.setLoggerLevel(component, level);
        }
        
        Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
        logger.debug("Set log level for {}: {}", component, level);
    }
    
    /**
     * Enables or disables file logging.
     * 
     * @param enabled Whether to enable file logging
     * @param filePath Optional file path (uses default if null)
     */
    public static void setFileLogging(boolean enabled, String filePath) {
        if (!initialized) {
            initialize();
        }
        
        if (currentConfig != null) {
            currentConfig.setFileEnabled(enabled);
            if (filePath != null) {
                currentConfig.setFilePath(filePath);
            }
            LoggerFactory.configure(currentConfig);
            
            Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
            logger.info("File logging {}: {}", enabled ? "enabled" : "disabled", 
                       enabled ? currentConfig.getFilePath() : "N/A");
        }
    }
    
    /**
     * Gets a logger for the specified class using MP3Org's logging system.
     * This is the recommended way to get loggers in MP3Org components.
     * 
     * @param clazz The class requesting the logger
     * @return A configured logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        if (!initialized) {
            initialize();
        }
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Gets a logger for the specified name using MP3Org's logging system.
     * 
     * @param name The logger name
     * @return A configured logger instance
     */
    public static Logger getLogger(String name) {
        if (!initialized) {
            initialize();
        }
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * Loads the logging configuration from the configuration file or creates default.
     * 
     * @return The loaded configuration
     */
    private static LoggingConfiguration loadConfiguration() {
        // Try to load from configuration file
        File configFile = new File(LOGGING_CONFIG_FILE);
        if (configFile.exists()) {
            try {
                return LoggingConfiguration.loadFromFile(configFile);
            } catch (IOException e) {
                System.err.println("Failed to load logging configuration from " + LOGGING_CONFIG_FILE + ": " + e.getMessage());
            }
        }
        
        // Always use default configuration during initial startup
        // This avoids circular dependencies with DatabaseConfig/DatabaseProfileManager
        // Configuration can be reloaded from database later using reloadConfigurationFromDatabase()
        return LoggingConfiguration.createDefault();
    }
    
    /**
     * Saves the current logging configuration to file.
     * 
     * @throws IOException If saving fails
     */
    public static void saveConfiguration() throws IOException {
        if (currentConfig != null) {
            File configFile = new File(LOGGING_CONFIG_FILE);
            currentConfig.saveToFile(configFile);
            
            Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
            logger.debug("Logging configuration saved to {}", configFile.getAbsolutePath());
        }
    }
    
    /**
     * Shuts down the logging system cleanly.
     * This should be called before application termination.
     */
    public static void shutdown() {
        if (initialized) {
            Logger logger = LoggerFactory.getLogger(MP3OrgLoggingManager.class);
            logger.info("Shutting down MP3Org logging system");
            
            LoggerFactory.flushAll();
            LoggerFactory.shutdown();
            initialized = false;
        }
    }
    
    /**
     * Convenience method to log application startup.
     * 
     * @param applicationName The name of the application
     * @param version The application version
     */
    public static void logApplicationStartup(String applicationName, String version) {
        Logger logger = getLogger("org.hasting.Application");
        logger.info("Starting {} version {}", applicationName, version);
        logger.debug("Logging configuration: console={}, file={}, level={}", 
                    currentConfig.isConsoleEnabled(),
                    currentConfig.isFileEnabled() ? currentConfig.getFilePath() : "disabled",
                    currentConfig.getDefaultLevel());
    }
    
    /**
     * Reloads logging configuration from the database after it has been initialized.
     * This allows database-driven logging configuration without circular dependencies.
     * 
     * <p>This method should be called after the database system is fully initialized
     * and DatabaseConfig is available. It will attempt to load logging preferences
     * from the database and update the current logging configuration accordingly.
     * 
     * <p>If loading from database fails, the current configuration remains unchanged.
     */
    public static void reloadConfigurationFromDatabase() {
        if (!initialized) {
            System.err.println("Warning: Cannot reload logging configuration - logging system not initialized");
            return;
        }
        
        try {
            Logger logger = getLogger(MP3OrgLoggingManager.class);
            logger.debug("Attempting to reload logging configuration from database");
            
            // Now it's safe to access DatabaseConfig since circular dependency is broken
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            
            // For now, we'll just log that we attempted the reload
            // In the future, DatabaseConfig could provide logging-specific settings
            logger.info("Logging configuration reload from database completed (using defaults for now)");
            
        } catch (Exception e) {
            Logger logger = getLogger(MP3OrgLoggingManager.class);
            logger.warning("Failed to reload logging configuration from database: {}", e.getMessage());
            // Keep current configuration on failure
        }
    }
    
    /**
     * Checks if the current log file needs rotation and performs backup if needed.
     * This method should be called periodically or after significant logging activity.
     */
    public static void checkAndRotateLogFile() {
        if (!initialized || currentConfig == null || !currentConfig.isFileEnabled() || !currentConfig.isBackupEnabled()) {
            return;
        }
        
        try {
            String logFilePath = currentConfig.getFilePath();
            int maxSizeMB = currentConfig.getBackupMaxSizeMB();
            
            if (LogBackupManager.shouldRotateLogFile(logFilePath, maxSizeMB)) {
                Logger logger = getLogger(MP3OrgLoggingManager.class);
                logger.info("Log file size threshold reached, performing rotation...");
                
                boolean success = LogBackupManager.rotateLogFile(currentConfig);
                if (success) {
                    logger.info("Log file rotation completed successfully");
                } else {
                    logger.warning("Log file rotation failed - check backup configuration");
                }
            }
        } catch (Exception e) {
            Logger logger = getLogger(MP3OrgLoggingManager.class);
            logger.error("Error during log rotation check: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Forces an immediate backup of the current log file.
     * 
     * @return true if backup was successful, false otherwise
     */
    public static boolean backupLogFile() {
        if (!initialized || currentConfig == null || !currentConfig.isFileEnabled()) {
            return false;
        }
        
        try {
            Logger logger = getLogger(MP3OrgLoggingManager.class);
            logger.info("Performing manual log file backup...");
            
            boolean success = LogBackupManager.forceBackup(currentConfig);
            if (success) {
                logger.info("Manual log backup completed successfully");
            } else {
                logger.warning("Manual log backup failed");
            }
            
            return success;
        } catch (Exception e) {
            Logger logger = getLogger(MP3OrgLoggingManager.class);
            logger.error("Error during manual backup: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets information about existing backup files.
     * 
     * @return A list of backup file information
     */
    public static java.util.List<LogBackupManager.BackupFileInfo> getBackupFileInfo() {
        if (!initialized || currentConfig == null) {
            return new java.util.ArrayList<>();
        }
        
        return LogBackupManager.getBackupFileInfo(currentConfig);
    }
}