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
        
        // Try to load from MP3Org's main configuration
        try {
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            Properties mainProps = new Properties();
            // If DatabaseConfig had a way to get all properties, we'd use it here
            // For now, create default configuration
            return LoggingConfiguration.createDefault();
        } catch (Exception e) {
            System.err.println("Failed to load MP3Org configuration: " + e.getMessage());
        }
        
        // Fall back to default configuration
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
}