package org.hasting.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

/**
 * Centralized configuration management for database connectivity and application settings.
 * 
 * <p>This singleton class provides a comprehensive configuration system that supports
 * multiple configuration sources with clear precedence ordering. It manages database
 * connectivity, file type filtering, and profile-based configuration switching for
 * the MP3Org application.
 * 
 * <p>Configuration source precedence (highest to lowest):
 * <ol>
 * <li><strong>Active Database Profile</strong> - User-defined profiles with complete settings</li>
 * <li><strong>System Properties</strong> - JVM arguments like {@code -Dmp3org.database.path=/path}</li>
 * <li><strong>Environment Variables</strong> - {@code MP3ORG_DATABASE_PATH} and related vars</li>
 * <li><strong>Configuration File</strong> - {@code mp3org.properties} in current or home directory</li>
 * <li><strong>Default Values</strong> - Built-in defaults ({@code ./mp3org} database path)</li>
 * </ol>
 * 
 * <p>Key features include:
 * <ul>
 * <li><strong>Profile Management</strong> - Multiple database profiles with easy switching</li>
 * <li><strong>File Type Filtering</strong> - Configurable support for audio file formats</li>
 * <li><strong>Path Normalization</strong> - Automatic path resolution and directory creation</li>
 * <li><strong>JDBC Configuration</strong> - Apache Derby embedded database setup</li>
 * <li><strong>Dynamic Reconfiguration</strong> - Runtime configuration changes with persistence</li>
 * <li><strong>Legacy Compatibility</strong> - Backward compatibility with older configuration methods</li>
 * </ul>
 * 
 * <p>Supported audio file types:
 * <ul>
 * <li><strong>MP3</strong> - MPEG Audio Layer 3</li>
 * <li><strong>FLAC</strong> - Free Lossless Audio Codec</li>
 * <li><strong>OGG</strong> - Ogg Vorbis</li>
 * <li><strong>WAV</strong> - Waveform Audio File Format</li>
 * <li><strong>AAC</strong> - Advanced Audio Coding</li>
 * <li><strong>M4A</strong> - MPEG-4 Audio</li>
 * <li><strong>WMA</strong> - Windows Media Audio</li>
 * <li><strong>AIFF</strong> - Audio Interchange File Format</li>
 * <li><strong>APE</strong> - Monkey's Audio</li>
 * <li><strong>OPUS</strong> - Opus Audio Codec</li>
 * </ul>
 * 
 * <p>Database configuration uses Apache Derby embedded database with automatic
 * table creation and schema management. Connection pooling and transaction
 * management are handled by the {@link DatabaseManager} class.
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Get configuration instance
 * DatabaseConfig config = DatabaseConfig.getInstance();
 * 
 * // Check current database path
 * String dbPath = config.getDatabasePath();
 * 
 * // Change database location
 * config.setDatabasePath("/new/database/path");
 * 
 * // Configure file types
 * Set<String> types = Set.of("mp3", "flac", "wav");
 * config.setEnabledFileTypes(types);
 * 
 * // Switch between profiles
 * config.switchToProfileByName("Work Collection");
 * 
 * // Get JDBC connection details
 * String jdbcUrl = config.getJdbcUrl();
 * String driver = config.getJdbcDriver();
 * }</pre>
 * 
 * <p>The class is thread-safe and uses synchronized methods for configuration
 * changes. All configuration modifications are automatically persisted to ensure
 * settings survive application restarts.
 * 
 * @see DatabaseManager for database operations using this configuration
 * @see DatabaseProfile for profile-based configuration management
 * @see DatabaseProfileManager for profile lifecycle management
 * @since 1.0
 */
public class DatabaseConfig {
    private static final Logger logger = Log4Rich.getLogger(DatabaseConfig.class);
    
    // Configuration keys
    private static final String SYSTEM_PROPERTY_DB_PATH = "mp3org.database.path";
    private static final String ENV_VAR_DB_PATH = "MP3ORG_DATABASE_PATH";
    private static final String CONFIG_FILE_NAME = "mp3org.properties";
    private static final String CONFIG_KEY_DB_PATH = "database.path";
    private static final String CONFIG_KEY_ENABLED_TYPES = "enabled.file.types";
    private static final String DEFAULT_DB_PATH = "mp3org";
    
    // Supported file types
    private static final String[] ALL_SUPPORTED_TYPES = {
        "mp3", "flac", "ogg", "wav", "aac", "m4a", "wma", "aiff", "ape", "opus"
    };
    private static final Set<String> DEFAULT_ENABLED_TYPES = new HashSet<>(Arrays.asList(ALL_SUPPORTED_TYPES));
    
    // JDBC configuration - SQLite (Issue #72 migration from Derby)
    private static final String JDBC_DRIVER = "org.sqlite.JDBC";
    private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";
    private static final String JDBC_URL_SUFFIX = "";  // SQLite auto-creates
    private static final String DEFAULT_USER = "";
    private static final String DEFAULT_PASSWORD = "";
    
    private static DatabaseConfig instance;
    private String databasePath;
    private String jdbcUrl;
    private Properties configProperties;
    private Set<String> enabledFileTypes;
    private DatabaseProfileManager profileManager;
    
    private DatabaseConfig() {
        loadConfiguration();
    }
    
    /**
     * Gets the singleton instance of DatabaseConfig.
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    /**
     * Loads configuration from various sources in order of precedence.
     */
    private void loadConfiguration() {
        // Initialize profile manager
        profileManager = DatabaseProfileManager.getInstance();
        
        // Load configuration file first (for legacy compatibility)
        loadConfigFile();
        
        // Load configuration from active profile
        loadFromActiveProfile();
        
        logger.info("MP3Org Database configured at: " + databasePath);
        logger.info("Enabled file types: " + enabledFileTypes);
        logger.info("Active profile: " + (profileManager.getActiveProfile() != null ? profileManager.getActiveProfile().getName() : "None"));
    }
    
    /**
     * Loads configuration from properties file if it exists.
     */
    private void loadConfigFile() {
        configProperties = new Properties();
        
        // Try to load from current directory
        Path configPath = Paths.get(CONFIG_FILE_NAME);
        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                configProperties.load(input);
                logger.info("Loaded configuration from: " + configPath.toAbsolutePath());
            } catch (IOException e) {
                logger.warn("Could not load configuration file: " + e.getMessage());
            }
        }
        
        // Try to load from user home directory
        if (configProperties.isEmpty()) {
            Path homeConfigPath = Paths.get(System.getProperty("user.home"), CONFIG_FILE_NAME);
            if (Files.exists(homeConfigPath)) {
                try (InputStream input = Files.newInputStream(homeConfigPath)) {
                    configProperties.load(input);
                    logger.info("Loaded configuration from: " + homeConfigPath.toAbsolutePath());
                } catch (IOException e) {
                    logger.warn("Could not load configuration file from home: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Determines the database path from various sources in order of precedence.
     */
    private String determineDatabasePath() {
        // 1. System property (highest precedence)
        String path = System.getProperty(SYSTEM_PROPERTY_DB_PATH);
        if (path != null && !path.trim().isEmpty()) {
            logger.info("Using database path from system property: " + path);
            return path.trim();
        }
        
        // 2. Environment variable
        path = System.getenv(ENV_VAR_DB_PATH);
        if (path != null && !path.trim().isEmpty()) {
            logger.info("Using database path from environment variable: " + path);
            return path.trim();
        }
        
        // 3. Configuration file
        path = configProperties.getProperty(CONFIG_KEY_DB_PATH);
        if (path != null && !path.trim().isEmpty()) {
            logger.info("Using database path from configuration file: " + path);
            return path.trim();
        }
        
        // 4. Default location
        logger.info("Using default database path: " + DEFAULT_DB_PATH);
        return DEFAULT_DB_PATH;
    }
    
    /**
     * Loads enabled file types from configuration.
     */
    private Set<String> loadEnabledFileTypes() {
        String typesString = configProperties.getProperty(CONFIG_KEY_ENABLED_TYPES);
        if (typesString != null && !typesString.trim().isEmpty()) {
            return Arrays.stream(typesString.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(type -> Arrays.asList(ALL_SUPPORTED_TYPES).contains(type))
                    .collect(Collectors.toSet());
        }
        return new HashSet<>(DEFAULT_ENABLED_TYPES);
    }
    
    /**
     * Loads configuration from the active profile.
     */
    private void loadFromActiveProfile() {
        DatabaseProfile activeProfile = profileManager.getActiveProfile();
        if (activeProfile != null) {
            // Use profile configuration
            databasePath = normalizePath(activeProfile.getDatabasePath());
            enabledFileTypes = activeProfile.getEnabledFileTypes();
        } else {
            // Fallback to legacy configuration
            databasePath = determineDatabasePath();
            databasePath = normalizePath(databasePath);
            enabledFileTypes = loadEnabledFileTypes();
        }
        
        // Construct JDBC URL
        jdbcUrl = JDBC_URL_PREFIX + databasePath + JDBC_URL_SUFFIX;
    }
    
    /**
     * Normalizes the database path to ensure it's properly formatted.
     * For SQLite, ensures the path ends with .db extension.
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return DEFAULT_DB_PATH + ".db";
        }

        path = path.trim();

        // Ensure SQLite database file has .db extension
        if (!path.toLowerCase().endsWith(".db")) {
            path = path + ".db";
        }

        // If it's a relative path, make it relative to current working directory
        Path dbPath = Paths.get(path);
        if (!dbPath.isAbsolute()) {
            dbPath = Paths.get(System.getProperty("user.dir")).resolve(path);
        }

        // Ensure parent directories exist
        try {
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.info("Created database directory: " + parentDir);
            }
        } catch (IOException e) {
            logger.warn("Could not create database directory: " + e.getMessage());
        }

        return dbPath.toString();
    }
    
    /**
     * Gets the configured database path.
     */
    public String getDatabasePath() {
        return databasePath;
    }
    
    /**
     * Gets the JDBC URL for the database.
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }
    
    /**
     * Gets the JDBC driver class name.
     */
    public String getJdbcDriver() {
        return JDBC_DRIVER;
    }
    
    /**
     * Gets the database username.
     */
    public String getUsername() {
        return DEFAULT_USER;
    }
    
    /**
     * Gets the database password.
     */
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }
    
    /**
     * Sets a new database path and reconfigures the connection.
     * This will close any existing connections.
     */
    public synchronized void setDatabasePath(String newPath) {
        if (newPath == null || newPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Database path cannot be null or empty");
        }
        
        String oldPath = this.databasePath;
        this.databasePath = normalizePath(newPath);
        this.jdbcUrl = JDBC_URL_PREFIX + this.databasePath + JDBC_URL_SUFFIX;
        
        logger.info("Database path changed from: " + oldPath + " to: " + this.databasePath);
        
        // Update active profile
        updateActiveProfile();
        
        // Save the new path to configuration file (for legacy compatibility)
        saveConfigurationFile();
    }
    
    /**
     * Gets all supported file types.
     */
    public static String[] getAllSupportedTypes() {
        return ALL_SUPPORTED_TYPES.clone();
    }
    
    /**
     * Gets the currently enabled file types.
     */
    public Set<String> getEnabledFileTypes() {
        return new HashSet<>(enabledFileTypes);
    }
    
    /**
     * Sets the enabled file types.
     */
    public synchronized void setEnabledFileTypes(Set<String> types) {
        if (types == null) {
            this.enabledFileTypes = new HashSet<>(DEFAULT_ENABLED_TYPES);
        } else {
            // Only keep valid types
            this.enabledFileTypes = types.stream()
                    .map(String::toLowerCase)
                    .filter(type -> Arrays.asList(ALL_SUPPORTED_TYPES).contains(type))
                    .collect(Collectors.toSet());
        }
        
        logger.info("File type filters updated: " + this.enabledFileTypes);
        
        // Update active profile
        updateActiveProfile();
        
        // Save to configuration file (for legacy compatibility)
        saveConfigurationFile();
    }
    
    /**
     * Checks if a file type is enabled.
     */
    public boolean isFileTypeEnabled(String fileType) {
        return enabledFileTypes.contains(fileType.toLowerCase());
    }
    
    /**
     * Saves the current configuration to a properties file.
     */
    private void saveConfigurationFile() {
        try {
            if (configProperties == null) {
                configProperties = new Properties();
            }
            
            configProperties.setProperty(CONFIG_KEY_DB_PATH, databasePath);
            
            // Save enabled file types
            String typesString = enabledFileTypes.stream()
                    .sorted()
                    .collect(Collectors.joining(","));
            configProperties.setProperty(CONFIG_KEY_ENABLED_TYPES, typesString);
            
            Path configPath = Paths.get(CONFIG_FILE_NAME);
            try (OutputStream output = Files.newOutputStream(configPath)) {
                configProperties.store(output, "MP3Org Database Configuration");
                logger.info("Saved configuration to: " + configPath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.warn("Could not save configuration file: " + e.getMessage());
        }
    }
    
    /**
     * Creates a sample configuration file with documentation.
     */
    public static void createSampleConfigFile() {
        try {
            Path configPath = Paths.get(CONFIG_FILE_NAME);
            if (!Files.exists(configPath)) {
                Properties sampleProps = new Properties();
                sampleProps.setProperty(CONFIG_KEY_DB_PATH, DEFAULT_DB_PATH);
                
                // Add default file types
                String defaultTypes = Arrays.stream(ALL_SUPPORTED_TYPES)
                        .sorted()
                        .collect(Collectors.joining(","));
                sampleProps.setProperty(CONFIG_KEY_ENABLED_TYPES, defaultTypes);
                
                try (OutputStream output = Files.newOutputStream(configPath)) {
                    sampleProps.store(output, 
                        "MP3Org Database Configuration\n" +
                        "# You can specify the database location using:\n" +
                        "# 1. System property: -Dmp3org.database.path=/path/to/database\n" +
                        "# 2. Environment variable: MP3ORG_DATABASE_PATH=/path/to/database\n" +
                        "# 3. This configuration file\n" +
                        "# 4. Default: ./mp3org\n" +
                        "#\n" +
                        "# Examples:\n" +
                        "# database.path=./mp3org\n" +
                        "# database.path=/Users/username/Music/mp3org-database\n" +
                        "# database.path=C:\\\\Music\\\\mp3org-database\n");
                    
                    logger.info("Created sample configuration file: " + configPath.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            logger.warn("Could not create sample configuration file: " + e.getMessage());
        }
    }
    
    /**
     * Gets information about the current configuration.
     */
    public String getConfigurationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Database Configuration:\n");
        info.append("  Path: ").append(databasePath).append("\n");
        info.append("  JDBC URL: ").append(jdbcUrl).append("\n");
        info.append("  Config File: ").append(CONFIG_FILE_NAME).append("\n");
        
        // Profile information
        DatabaseProfile activeProfile = getActiveProfile();
        if (activeProfile != null) {
            info.append("\nActive Profile:\n");
            info.append("  Name: ").append(activeProfile.getName()).append("\n");
            info.append("  ID: ").append(activeProfile.getId()).append("\n");
            if (activeProfile.getDescription() != null && !activeProfile.getDescription().trim().isEmpty()) {
                info.append("  Description: ").append(activeProfile.getDescription()).append("\n");
            }
            
            // Add music file count with formatted display
            int fileCount = getMusicFileCountSafely();
            if (fileCount >= 0) {
                String formattedCount = String.format("%,d", fileCount);
                info.append("  Music Files: ").append(formattedCount)
                    .append(fileCount == 1 ? " file" : " files").append("\n");
            } else {
                info.append("  Music Files: Unknown (database error)\n");
            }
        }
        
        if (profileManager != null) {
            info.append("\nAvailable Profiles: ").append(profileManager.getAllProfiles().size()).append("\n");
            for (DatabaseProfile profile : profileManager.getAllProfiles()) {
                boolean isActive = profile.equals(activeProfile);
                info.append("  ").append(isActive ? "* " : "  ").append(profile.getName()).append("\n");
            }
        }
        
        info.append("\nFile Type Filters:\n");
        info.append("  Enabled Types: ").append(enabledFileTypes.stream().sorted().collect(Collectors.joining(", "))).append("\n");
        info.append("  All Supported: ").append(Arrays.stream(ALL_SUPPORTED_TYPES).collect(Collectors.joining(", "))).append("\n");
        
        info.append("\nConfiguration Sources (in order of precedence):\n");
        info.append("  1. Active Database Profile\n");
        info.append("  2. System Property: -D").append(SYSTEM_PROPERTY_DB_PATH).append("=<path>\n");
        info.append("  3. Environment Variable: ").append(ENV_VAR_DB_PATH).append("=<path>\n");
        info.append("  4. Configuration File: ").append(CONFIG_KEY_DB_PATH).append("=<path>\n");
        info.append("  5. Default: ").append(DEFAULT_DB_PATH).append("\n");
        return info.toString();
    }
    
    /**
     * Reloads the configuration from all sources.
     */
    public synchronized void reload() {
        logger.info("Reloading database configuration...");
        loadConfiguration();
    }
    
    /**
     * Gets the profile manager.
     */
    public DatabaseProfileManager getProfileManager() {
        return profileManager;
    }
    
    /**
     * Gets the currently active profile.
     */
    public DatabaseProfile getActiveProfile() {
        return profileManager != null ? profileManager.getActiveProfile() : null;
    }
    
    /**
     * Switches to a different profile.
     */
    public synchronized boolean switchToProfile(String profileId) {
        if (profileManager == null) {
            return false;
        }
        
        boolean success = profileManager.setActiveProfile(profileId);
        if (success) {
            // Reload configuration from new active profile
            loadFromActiveProfile();
            logger.info("Switched to profile: " + profileId);
        }
        return success;
    }
    
    /**
     * Switches to a profile by name.
     */
    public synchronized boolean switchToProfileByName(String profileName) {
        if (profileManager == null) {
            return false;
        }
        
        boolean success = profileManager.setActiveProfileByName(profileName);
        if (success) {
            // Reload configuration from new active profile
            loadFromActiveProfile();
            logger.info("Switched to profile: " + profileName);
        }
        return success;
    }
    
    /**
     * Updates the active profile with current settings.
     */
    public synchronized void updateActiveProfile() {
        DatabaseProfile activeProfile = getActiveProfile();
        if (activeProfile != null) {
            activeProfile.setDatabasePath(databasePath);
            activeProfile.setEnabledFileTypes(enabledFileTypes);
            profileManager.updateProfile(activeProfile);
        }
    }
    
    /**
     * Gets the music file count safely, handling database connection issues gracefully.
     * 
     * <p>This method provides a safe way to retrieve the music file count for display
     * in configuration panels. It handles potential database connection issues by
     * catching exceptions and returning -1 to indicate an error state, allowing
     * the UI to display appropriate messages like "Unknown" instead of crashing.
     * 
     * <p>This method is especially important during configuration reloads when the
     * database connection might be temporarily unavailable or being reinitialized.
     * 
     * @return the number of music files in the database, or -1 if unable to query
     */
    private int getMusicFileCountSafely() {
        try {
            // Check if we're in the middle of a configuration reload
            // by verifying basic database connectivity first
            Class<?> dbManagerClass = Class.forName("org.hasting.util.DatabaseManager");
            
            // First check if database connection is available
            java.lang.reflect.Method getConnectionMethod = dbManagerClass.getMethod("getConnection");
            Object connection = getConnectionMethod.invoke(null);
            
            if (connection == null) {
                // Database connection not available, return error state
                return -1;
            }
            
            // Now safely get the count
            java.lang.reflect.Method getCountMethod = dbManagerClass.getMethod("getMusicFileCount");
            return (Integer) getCountMethod.invoke(null);
            
        } catch (Exception e) {
            // Log the error but don't throw - return -1 to indicate error state
            // This is especially important during config reloads when connection might be temporarily unavailable
            logger.warn("Could not retrieve music file count (possibly during config reload): " + e.getMessage());
            return -1;
        }
    }
}