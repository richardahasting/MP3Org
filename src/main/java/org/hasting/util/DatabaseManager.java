package org.hasting.util;

import org.hasting.model.MusicFile;
import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central database management class providing all database operations for the MP3Org application.
 * 
 * <p>This class manages:
 * <ul>
 *   <li><strong>Database Connections:</strong> Connection pooling, initialization, and cleanup</li>
 *   <li><strong>CRUD Operations:</strong> Create, read, update, delete operations for music files</li>
 *   <li><strong>Search Functionality:</strong> Advanced search across multiple metadata fields</li>
 *   <li><strong>Duplicate Detection:</strong> Sophisticated fuzzy matching for finding duplicate files</li>
 *   <li><strong>Profile Management:</strong> Multiple database profiles with dynamic switching</li>
 *   <li><strong>Configuration Management:</strong> Dynamic configuration loading and reloading</li>
 * </ul>
 * 
 * <p>The class uses SQLite as the underlying database and provides thread-safe operations
 * through synchronized methods. It supports multiple database profiles allowing users to
 * maintain separate music collections with different configurations.
 * 
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Automatic table creation and schema management</li>
 *   <li>File type filtering based on user configuration</li>
 *   <li>Advanced duplicate detection with configurable similarity thresholds</li>
 *   <li>Full-text search across title, artist, album, and other metadata fields</li>
 *   <li>Integrated file system operations (delete files from disk)</li>
 * </ul>
 * 
 * @author MP3Org Development Team
 * @version 1.0
 * @since 1.0
 * @see DatabaseConfig
 * @see MusicFile
 */
public class DatabaseManager {
    private static final Logger logger = Log4Rich.getLogger(DatabaseManager.class);
    private static DatabaseConfig config;
    private static Connection connection;
    private static DatabaseConnectionPool connectionPool;
    private static final ConcurrentHashMap<String, Long> filePathsMap = new ConcurrentHashMap<>();  // Load all paths for quick lookups  issue#41
    static {
        // Initialize configuration
        config = DatabaseConfig.getInstance();
        // Create sample config file if it doesn't exist
        DatabaseConfig.createSampleConfigFile();
    }

    /**
     * Initializes the database connection and creates necessary tables.
     * 
     * <p>This method performs the following operations:
     * <ol>
     *   <li>Establishes a connection to the SQLite database using configuration settings</li>
     *   <li>Creates the music_files table if it doesn't exist</li>
     *   <li>Verifies the connection is working properly</li>
     * </ol>
     * 
     * <p>The method is thread-safe and will only initialize once. Subsequent calls
     * will be ignored if a connection already exists.
     * 
     * @throws RuntimeException if database initialization fails due to connection issues,
     *                         invalid configuration, or table creation problems
     */
    public static synchronized void initialize() {
        if (connection == null && connectionPool == null) {
            try {
                // Initialize connection pool
                connectionPool = new DatabaseConnectionPool(config);
                
                // Open a single connection for legacy compatibility
                connection = DriverManager.getConnection(
                    config.getJdbcUrl(), 
                    config.getUsername(), 
                    config.getPassword()
                );

                filePathsMap.clear();  // Clear existing entries  issue#41
                logger.info(String.format("Connected to database at: {}", config.getDatabasePath()));

                // Create table if not exists
                // deleteMusicFilesTable();
                createMusicFilesTable();
                createScanDirectoriesTable();
                
                // Initialize file path cache for performance  issue#41
                initFilePathCacheWithRetry();
            } catch (Exception e) {
                logger.error(String.format("Failed to initialize database connection: {}", e.getMessage()), e);
                throw new RuntimeException("Failed to initialize database connection", e);
            }
        }
    }
    
    /**
     * Initializes the database connection with automatic profile fallback for locked databases.
     * 
     * <p>This method implements the self-teaching database initialization pattern that ensures
     * the MP3Org application can always start successfully, even when the preferred database
     * is locked by another application instance. The method name clearly communicates its
     * purpose and the automatic fallback behavior.
     * 
     * <p><strong>Initialization Strategy:</strong>
     * <ol>
     *   <li><strong>Use profile manager:</strong> Leverage DatabaseProfileManager's fallback logic</li>
     *   <li><strong>Activate with fallback:</strong> Automatically handle locked database scenarios</li>
     *   <li><strong>Reload configuration:</strong> Update DatabaseConfig with the resolved profile</li>
     *   <li><strong>Initialize normally:</strong> Use standard initialization once profile is resolved</li>
     * </ol>
     * 
     * <p>This approach follows the development philosophy of "code that teaches its patterns" -
     * future developers can understand the initialization flow by reading the method delegation
     * and following the clear sequence of operations.
     * 
     * @param preferredProfileId the database profile the user wants to use
     * @return the database profile that was actually activated (may differ from preferred due to fallback)
     * @throws RuntimeException if no database profiles can be activated (extremely rare)
     */
    public static synchronized DatabaseProfile initializeWithAutomaticFallback(String preferredProfileId) {
        if (connection != null) {
            // Already initialized - return current active profile
            return DatabaseProfileManager.getInstance().getActiveProfile();
        }
        
        try {
            // Use profile manager's fallback logic to resolve the best available profile
            DatabaseProfileManager profileManager = DatabaseProfileManager.getInstance();
            DatabaseProfile resolvedProfile = profileManager.activateProfileWithAutomaticFallback(preferredProfileId);
            
            // Reload configuration to reflect the resolved profile
            config = DatabaseConfig.getInstance();
            config.reload();
            
            // Now initialize with the resolved, available database
            initialize();
            
            return resolvedProfile;
            
        } catch (Exception e) {
            logger.error(String.format("Failed to initialize database connection with fallback: {}", e.getMessage()), e);
            throw new RuntimeException("Failed to initialize database connection with fallback: " + e.getMessage(), e);
        }
    }


    /**
     * Drops the music_files table from the database and closes the connection.
     * 
     * <p><strong>WARNING:</strong> This operation is destructive and will permanently
     * delete all music file records from the database. Use with extreme caution.
     * 
     * <p>The method will:
     * <ol>
     *   <li>Execute a DROP TABLE command for the music_files table</li>
     *   <li>Close the current database connection</li>
     *   <li>Reset the connection to null</li>
     * </ol>
     * 
     * @throws SQLException if the table cannot be dropped or connection cannot be closed
     */
    public static synchronized void deleteMusicFilesTable() {
        String sql = "DROP TABLE music_files";
       try {
           if (connection!= null) {
               connection.close();
               connection = null;
           }
       } catch (SQLException e) {
           throw new RuntimeException(e);
       }
            try {
                // Register JDBC driver (not needed with modern JDBC)
                // Class.forName(config.getJdbcDriver());

                // Open a connection using configured database location
                connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sql);
                }
                connection.close();
                connection = null;
        } catch (SQLException e) {
            logger.error(String.format("Failed to drop music_files table: {}", e.getMessage()), e);
            throw new RuntimeException("Failed to drop music_files table", e);
        }
    }

    /**
     * Creates the music_files table with the required schema if it doesn't already exist.
     * 
     * <p>The table schema includes:
     * <ul>
     *   <li><strong>id:</strong> Auto-generated primary key (BIGINT)</li>
     *   <li><strong>file_path:</strong> Unique file system path (VARCHAR 1024)</li>
     *   <li><strong>title, artist, album:</strong> Basic metadata (VARCHAR 255)</li>
     *   <li><strong>genre:</strong> Music genre (VARCHAR 50)</li>
     *   <li><strong>track_number, yr, duration_seconds:</strong> Numeric metadata (INT)</li>
     *   <li><strong>file_size_bytes:</strong> File size in bytes (BIGINT)</li>
     *   <li><strong>bit_rate, sample_rate:</strong> Audio quality metrics (INT)</li>
     *   <li><strong>file_type:</strong> File extension (VARCHAR 10)</li>
     *   <li><strong>last_modified, date_added:</strong> Timestamp fields</li>
     * </ul>
     * 
     * @throws RuntimeException if table creation fails due to SQL errors or connection issues
     */
    private static synchronized void createMusicFilesTable() {
        // First check if table exists
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM music_files WHERE 1=0");
            rs.close();
            logger.debug("Music files table already exists, skipping creation");
            return; // Table exists, no need to create
        } catch (SQLException e) {
            // Table doesn't exist, proceed with creation
            logger.debug("Music files table does not exist, creating it");
        }
        
        // SQLite-compatible schema (Issue #72 - migrated from Derby)
        String sql = "CREATE TABLE music_files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_path TEXT NOT NULL UNIQUE, " +
                "title TEXT, " +
                "artist TEXT, " +
                "album TEXT, " +
                "genre TEXT, " +
                "track_number INTEGER, " +
                "yr INTEGER, " +
                "duration_seconds INTEGER, " +
                "file_size_bytes INTEGER, " +
                "bit_rate INTEGER, " +
                "sample_rate INTEGER, " +
                "file_type TEXT, " +
                "last_modified TEXT, " +
                "date_added TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Created music_files table successfully");
        } catch (SQLException e) {
            logger.error(String.format("Failed to create music_files table: {}", e.getMessage()), e);
            throw new RuntimeException("Failed to create music_files table", e);
        }
    }

    /**
     * Creates the scan_directories table if it doesn't exist.
     * 
     * <p>This table tracks the original root directories that users selected for scanning,
     * allowing the directory rescanning feature to show meaningful top-level directories
     * rather than every subdirectory that contains music files.
     * 
     * <p>Table schema:
     * <ul>
     *   <li>id: Primary key, auto-generated</li>
     *   <li>root_path: The original directory path selected by the user</li>
     *   <li>scan_date: When this directory was first scanned</li>
     *   <li>last_rescan: When this directory was last rescanned (nullable)</li>
     *   <li>file_count: Number of music files found in this directory tree</li>
     * </ul>
     * 
     * @since 1.0
     */
    private static synchronized void createScanDirectoriesTable() {
        // First check if table exists
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM scan_directories WHERE 1=0");
            rs.close();
            logger.debug("Scan directories table already exists, skipping creation");
            return; // Table exists, no need to create
        } catch (SQLException e) {
            // Table doesn't exist, proceed with creation
            logger.debug("Scan directories table does not exist, creating it");
        }
        
        // SQLite-compatible schema (Issue #72 - migrated from Derby)
        String sql = "CREATE TABLE scan_directories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "root_path TEXT NOT NULL UNIQUE, " +
                "scan_date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "last_rescan TEXT, " +
                "file_count INTEGER DEFAULT 0" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Created scan_directories table successfully");
        } catch (SQLException e) {
            logger.error(String.format("Failed to create scan_directories table: {}", e.getMessage()), e);
            throw new RuntimeException("Failed to create scan_directories table", e);
        }
    }

    /**
     * Gets the current database connection, initializing it if necessary.
     * 
     * <p>This method provides lazy initialization of the database connection.
     * If no connection exists, it will automatically call initialize() to create one.
     * 
     * <p>The connection is managed as a singleton and is thread-safe through synchronization.
     * All database operations in this class use this method to obtain the connection.
     * 
     * @return the active SQLite database connection
     * @throws RuntimeException if connection initialization fails
     */
    public static synchronized Connection getConnection() {
        if (connection == null) {
            initialize();
        }
        return connection;
    }

    /**
     * Ensures a valid database connection is available, with recovery mechanisms.
     * 
     * <p>This method provides robust connection validation and recovery:
     * <ol>
     *   <li>Attempts to get current connection</li>
     *   <li>Validates connection is not null and is valid</li>
     *   <li>If connection fails, attempts standard initialization</li>
     *   <li>If that fails, throws RuntimeException with clear error message</li>
     * </ol>
     * 
     * @return valid database connection, never null
     * @throws RuntimeException if no database connection can be established
     */
    public static synchronized Connection ensureConnection() {
        try {
            Connection conn = getConnection();
            
            // Check if connection is null or closed
            if (conn == null || conn.isClosed()) {
                logger.warn("Database connection is null or closed, attempting recovery");
                
                // Try standard initialization
                initialize();
                conn = getConnection();
                
                if (conn == null || conn.isClosed()) {
                    throw new RuntimeException("Failed to establish database connection after recovery attempt");
                }
                
                logger.info("Database connection recovered successfully");
            }
            
            return conn;
            
        } catch (SQLException e) {
            logger.error(String.format("Database connection validation failed: {}", e.getMessage()), e);
            throw new RuntimeException("Database connection validation failed", e);
        } catch (Exception e) {
            logger.error(String.format("Unexpected error during connection validation: {}", e.getMessage()), e);
            throw new RuntimeException("Failed to ensure database connection", e);
        }
    }

    /**
     * Properly closes the database connection and cleans up resources.
     * 
     * <p>This method should be called when the application is shutting down
     * to ensure the database connection is properly closed and resources are released.
     * 
     * <p>The method:
     * <ol>
     *   <li>Checks if a connection exists</li>
     *   <li>Closes the connection if it's open</li>
     *   <li>Sets the connection reference to null</li>
     *   <li>Logs the shutdown status</li>
     * </ol>
     * 
     * <p>It's safe to call this method multiple times - subsequent calls will be ignored.
     * 
     * @throws RuntimeException if connection closure fails (though this is typically logged and ignored)
     */
    public static synchronized void shutdown() {
        // Shutdown connection pool if available
        if (connectionPool != null) {
            connectionPool.shutdown();
            connectionPool = null;
        }
        
        // Close legacy single connection
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error(String.format("Error closing database connection: {}", e.getMessage()), e);
            }
        }
    }

    /**
     * Changes the database location and reinitializes the connection.
     * This will close the current connection and create a new one at the specified location.
     */
    public static synchronized void changeDatabaseLocation(String newPath) {
        String oldPath = config.getDatabasePath();
        
        // Close current connection
        shutdown();
        
        // Update configuration
        config.setDatabasePath(newPath);
        
        // Reinitialize with new location
        initialize();
        
        logger.info(String.format("Database location changed to: {}", config.getDatabasePath()));
        
        // Check if the new database is empty
        boolean isNewDatabase = false;
        try {
            List<org.hasting.model.MusicFile> allFiles = getAllMusicFiles();
            isNewDatabase = allFiles == null || allFiles.isEmpty();
        } catch (Exception e) {
            isNewDatabase = true;
        }
        
        // Notify listeners of database change
        ProfileChangeNotifier.getInstance().notifyDatabaseChanged(oldPath, newPath, isNewDatabase);
    }

    /**
     * Gets the current database configuration.
     */
    public static DatabaseConfig getConfig() {
        return config;
    }

    /**
     * Reloads the database configuration from all sources.
     */
    public static synchronized void reloadConfig() {
        config.reload();
        // If the database path changed, we need to reconnect
        // For safety, we'll close and reinitialize
        shutdown();
        initialize();
    }
    
    /**
     * Switches to a different database profile.
     */
    public static synchronized boolean switchToProfile(String profileId) {
        try {
            // Close current connection
            shutdown();
            
            // Switch profile in config
            boolean success = config.switchToProfile(profileId);
            
            if (success) {
                // Reinitialize with new profile
                initialize();
                logger.info(String.format("Successfully switched to profile: {}", profileId));
                return true;
            } else {
                // Reinitialize with current profile if switch failed
                initialize();
                logger.error(String.format("Failed to switch to profile: {}", profileId));
                return false;
            }
        } catch (Exception e) {
            logger.error(String.format("Error switching to profile {}: {}", profileId, e.getMessage()), e);
            
            // Try to recover by reinitializing
            try {
                initialize();
            } catch (Exception recoveryError) {
                logger.error(String.format("Failed to recover database connection: %s", recoveryError.getMessage()), recoveryError);
            }
            return false;
        }
    }
    
    /**
     * Switches to a profile by name.
     */
    public static synchronized boolean switchToProfileByName(String profileName) {
        try {
            DatabaseProfile profile = config.getProfileManager().getProfileByName(profileName);
            if (profile != null) {
                return switchToProfile(profile.getId());
            } else {
                logger.error(String.format("Profile not found: {}", profileName));
                return false;
            }
        } catch (Exception e) {
            logger.error(String.format("Error switching to profile {}: {}", profileName, e.getMessage()), e);
            return false;
        }
    }
    
    /**
     * Gets the current active profile.
     */
    public static DatabaseProfile getActiveProfile() {
        return config.getActiveProfile();
    }
    
    /**
     * Gets the profile manager.
     */
    public static DatabaseProfileManager getProfileManager() {
        return config.getProfileManager();
    }
    
    /**
     * Gets statistics about the file path cache for performance monitoring.
     * @return cache size or -1 if not initialized
     */
    public static int getFilePathCacheSize() {
        return filePathsMap.size();
    }
    
    /**
     * Initializes file path cache with retry logic and fallback handling.
     * Issue #42 - Critical fix for database appearing empty after restart
     */
    private static void initFilePathCacheWithRetry() {
        int maxRetries = 3;
        long delay = 100; // Start with 100ms delay
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                initAllPathsMap();
                
                // Verify cache was populated successfully
                int cacheSize = filePathsMap.size();
                int dbCount = getMusicFileCount();
                
                if (dbCount > 0 && cacheSize == 0) {
                    throw new RuntimeException("Cache initialization failed - database has " + dbCount + 
                                             " files but cache is empty");
                }
                
                logger.info(String.format("File path cache initialization successful on attempt {} - {} entries", attempt, cacheSize));
                return; // Success!
                
            } catch (Exception e) {
                logger.warn(String.format("Cache initialization attempt {} failed: {}", attempt, e.getMessage()));
                
                if (attempt == maxRetries) {
                    logger.error(String.format("Cache initialization failed after {} attempts. Enabling fallback mode.", maxRetries));
                    // Don't throw - enable fallback mode instead
                    return;
                } else {
                    try {
                        Thread.sleep(delay);
                        delay *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    // CRUD operations for MusicFile

    /**
     * Saves a new music file record to the database.
     * 
     * <p>This method performs the following operations:
     * <ol>
     *   <li>Checks if a file with the same path already exists in the database</li>
     *   <li>If duplicate exists, skips insertion and logs a message</li>
     *   <li>If new file, inserts all metadata fields into the music_files table</li>
     *   <li>Sets the generated database ID on the MusicFile object</li>
     *   <li>Marks the MusicFile as unmodified after successful save</li>
     * </ol>
     * 
     * <p>The method handles null values appropriately for optional fields like track number,
     * year, duration, file size, bit rate, and sample rate.
     * 
     * @param musicFile the MusicFile object to save (must not be null, must have a valid file path)
     * @throws RuntimeException if database operation fails or connection is unavailable
     * @throws IllegalArgumentException if musicFile is null or has no file path
     */
    public static synchronized void saveMusicFile(MusicFile musicFile) {
        // Check if the music file already exists by file_path  issue#42
        Long existingId = getFileIdByPath(musicFile.getFilePath());
        if (existingId != null) {
            logger.debug(String.format("File already exists in database with ID {}: {}", existingId, musicFile.getFilePath()));
            return; // Skip insertion of duplicate
        }

        String sql = "INSERT INTO music_files (file_path, title, artist, album, genre, track_number, " +
                "yr, duration_seconds, file_size_bytes, bit_rate, sample_rate, file_type, last_modified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setMusicFileParameters(pstmt, musicFile);
            pstmt.executeUpdate();
            // Put the filePath into the Set.

            // Get the generated ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    musicFile.setId(generatedKeys.getLong(1));
                    // No synchronization needed with ConcurrentHashMap  issue#41
                    filePathsMap.put(musicFile.getFilePath(), musicFile.getId());
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to save music file to database: {}", musicFile.getFilePath()), e);
            throw new RuntimeException("Failed to save music file", e);
        }

    }

    /**
     * Saves multiple music files to the database using a single batch INSERT statement for optimal performance.
     * 
     * <p>This method provides significant performance improvements over individual inserts by:
     * <ul>
     *   <li>Using JDBC batch operations to minimize database round-trips</li>
     *   <li>Processing all files in a single transaction for atomicity</li>
     *   <li>Filtering out files that already exist in the database</li>
     *   <li>Updating the cache efficiently for all new files</li>
     * </ul>
     * 
     * <p>Performance comparison:
     * <ul>
     *   <li>Individual inserts: ~1000 files = ~3-5 seconds</li>
     *   <li>Batch insert: ~1000 files = ~200-500ms</li>
     * </ul>
     * 
     * @param musicFiles the collection of MusicFile objects to save (null entries are skipped)
     * @return the number of files actually inserted (excluding duplicates)
     * @throws RuntimeException if database operation fails or connection is unavailable
     * @throws IllegalArgumentException if musicFiles collection is null
     */
    public static synchronized int saveMusicFilesBatch(Collection<MusicFile> musicFiles) {
        if (musicFiles == null) {
            throw new IllegalArgumentException("MusicFiles collection cannot be null");
        }
        
        if (musicFiles.isEmpty()) {
            logger.debug("saveMusicFilesBatch() - empty collection, nothing to save");
            return 0;
        }
        
        long startTime = System.currentTimeMillis();
        logger.info(String.format("Starting batch insert of {} music files", musicFiles.size()));
        
        // Filter out files that already exist
        List<MusicFile> newFiles = new ArrayList<>();
        int duplicateCount = 0;
        
        for (MusicFile musicFile : musicFiles) {
            if (musicFile == null || musicFile.getFilePath() == null || musicFile.getFilePath().trim().isEmpty()) {
                logger.warn("Skipping null or invalid music file");
                continue;
            }
            
            Long existingId = getFileIdByPath(musicFile.getFilePath());
            if (existingId != null) {
                logger.debug(String.format("File already exists in database with ID {}: {}", existingId, musicFile.getFilePath()));
                duplicateCount++;
            } else {
                newFiles.add(musicFile);
            }
        }
        
        if (newFiles.isEmpty()) {
            logger.info(String.format("All {} files already exist in database - no new files to insert", musicFiles.size()));
            return 0;
        }
        
        logger.info(String.format("Inserting %d new files (%d duplicates skipped)", newFiles.size(), duplicateCount));
        
        String sql = "INSERT INTO music_files (file_path, title, artist, album, genre, track_number, " +
                "yr, duration_seconds, file_size_bytes, bit_rate, sample_rate, file_type, last_modified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // Use connection pool if available, otherwise fall back to single connection
        if (connectionPool != null) {
            try {
                return connectionPool.executeWithConnection(conn -> {
                    try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        // Add all files to the batch
                        for (MusicFile musicFile : newFiles) {
                            setMusicFileParameters(pstmt, musicFile);
                            pstmt.addBatch();
                        }
                        
                        // Execute the batch
                        int[] results = pstmt.executeBatch();
                        
                        // Get generated IDs and update cache
                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            int index = 0;
                            while (generatedKeys.next() && index < newFiles.size()) {
                                MusicFile musicFile = newFiles.get(index);
                                long generatedId = generatedKeys.getLong(1);
                                musicFile.setId(generatedId);
                                
                                // Update cache with new entry
                                filePathsMap.put(musicFile.getFilePath(), generatedId);
                                index++;
                            }
                        }
                        
                        long totalTime = System.currentTimeMillis() - startTime;
                        logger.info(String.format("Batch insert completed: %d files inserted in %dms (avg: %.2fms/file)", newFiles.size(), totalTime, totalTime / (double) newFiles.size()));
                        
                        return newFiles.size();
                    }
                });
            } catch (SQLException e) {
                logger.error("Failed to batch insert music files to database");
                throw new RuntimeException("Failed to batch insert music files", e);
            }
        } else {
            // Legacy single connection approach
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                // Disable auto-commit for batch transaction
                Connection conn = getConnection();
                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                
                try {
                    // Add all files to the batch
                    for (MusicFile musicFile : newFiles) {
                        setMusicFileParameters(pstmt, musicFile);
                        pstmt.addBatch();
                    }
                    
                    // Execute the batch
                    int[] results = pstmt.executeBatch();
                    conn.commit(); // Commit the transaction
                    
                    // Get generated IDs and update cache
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        int index = 0;
                        while (generatedKeys.next() && index < newFiles.size()) {
                            MusicFile musicFile = newFiles.get(index);
                            long generatedId = generatedKeys.getLong(1);
                            musicFile.setId(generatedId);
                            
                            // Update cache with new entry
                            filePathsMap.put(musicFile.getFilePath(), generatedId);
                            index++;
                        }
                    }
                    
                    long totalTime = System.currentTimeMillis() - startTime;
                    logger.info(String.format("Batch insert completed: %d files inserted in %dms (avg: %.2fms/file)", newFiles.size(), totalTime, totalTime / (double) newFiles.size()));
                    
                    return newFiles.size();
                    
                } catch (SQLException e) {
                    conn.rollback(); // Rollback on error
                    throw e;
                } finally {
                    conn.setAutoCommit(originalAutoCommit); // Restore original auto-commit setting
                }
                
            } catch (SQLException e) {
                logger.error("Failed to batch insert music files to database");
                throw new RuntimeException("Failed to batch insert music files", e);
            }
        }
    }
    
    /**
     * Helper method to set parameters for a MusicFile in a PreparedStatement.
     * This reduces code duplication between single and batch insert methods.
     */
    private static void setMusicFileParameters(PreparedStatement pstmt, MusicFile musicFile) throws SQLException {
        pstmt.setString(1, musicFile.getFilePath());
        pstmt.setString(2, musicFile.getTitle());
        pstmt.setString(3, musicFile.getArtist());
        pstmt.setString(4, musicFile.getAlbum());
        pstmt.setString(5, musicFile.getGenre());

        if (musicFile.getTrackNumber() != null) {
            pstmt.setInt(6, musicFile.getTrackNumber());
        } else {
            pstmt.setNull(6, Types.INTEGER);
        }

        if (musicFile.getYear() != null) {
            pstmt.setInt(7, musicFile.getYear());
        } else {
            pstmt.setNull(7, Types.INTEGER);
        }

        if (musicFile.getDurationSeconds() != null) {
            pstmt.setInt(8, musicFile.getDurationSeconds());
        } else {
            pstmt.setNull(8, Types.INTEGER);
        }

        if (musicFile.getFileSizeBytes() != null) {
            pstmt.setLong(9, musicFile.getFileSizeBytes());
        } else {
            pstmt.setNull(9, Types.BIGINT);
        }

        if (musicFile.getBitRate() != null) {
            pstmt.setLong(10, musicFile.getBitRate());
        } else {
            pstmt.setNull(10, Types.INTEGER);
        }

        if (musicFile.getSampleRate() != null) {
            pstmt.setInt(11, musicFile.getSampleRate());
        } else {
            pstmt.setNull(11, Types.INTEGER);
        }

        pstmt.setString(12, musicFile.getFileType());

        if (musicFile.getLastModified() != null) {
            pstmt.setTimestamp(13, new Timestamp(musicFile.getLastModified().getTime()));
        } else {
            pstmt.setNull(13, Types.TIMESTAMP);
        }
    }

    /**
     * Saves a music file to the database, updating the existing record if the file path already exists.
     * 
     * <p>This method implements upsert functionality to handle duplicate file paths gracefully:
     * <ol>
     *   <li>First checks if a record with the same file_path already exists</li>
     *   <li>If exists: updates the existing record with new metadata</li>
     *   <li>If not exists: inserts a new record</li>
     * </ol>
     * 
     * <p>This approach prevents constraint violations on the unique file_path column while
     * ensuring that updated metadata for existing files is properly saved. The method is
     * particularly useful during directory rescanning where files may have been modified
     * but their paths remain the same.
     * 
     * <p><strong>Key Benefits:</strong>
     * <ul>
     *   <li>Prevents database constraint violations</li>
     *   <li>Handles metadata updates for existing files</li>
     *   <li>Maintains referential integrity by preserving existing IDs</li>
     *   <li>Supports both new file imports and file rescanning scenarios</li>
     * </ul>
     * 
     * @param musicFile the MusicFile object to save or update (must not be null, must have a valid file path)
     * @throws RuntimeException if database operation fails or connection is unavailable
     * @throws IllegalArgumentException if musicFile is null or has no file path
     */
    public static synchronized void saveOrUpdateMusicFile(MusicFile musicFile) {
        if (musicFile == null) {
            throw new IllegalArgumentException("MusicFile cannot be null");
        }
        
        if (musicFile.getFilePath() == null || musicFile.getFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("MusicFile must have a valid file path");
        }
        
        long startTime = System.currentTimeMillis();
        logger.debug(String.format("saveOrUpdateMusicFile() - entry: {}", musicFile.getFilePath()));
        

        Long id = null;
        // Try cache first, fallback to database query if cache is empty  issue#42
        id = getFileIdByPath(musicFile.getFilePath());
        long cacheCheckTime = System.currentTimeMillis() - startTime;

        if (id != null) {
            // File exists - update the existing record
            logger.debug(String.format("File path already exists, updating record ID {}: {}", id, musicFile.getFilePath()));
            
            // Preserve the existing ID and transfer updated metadata
            musicFile.setId(id);
            musicFile.setModified(true); // Ensure the update will be performed
            
            // Use the existing update method
            updateMusicFile(musicFile);
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.debug(String.format("saveOrUpdateMusicFile() - exit: updated existing record (cache check: {}ms, total: {}ms)", cacheCheckTime, totalTime));
        } else {
            // File doesn't exist - insert new record
            logger.debug(String.format("File path is new, inserting record: {}", musicFile.getFilePath()));
            
            // Use the existing save method (it will update the cache)
            saveMusicFile(musicFile);
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.debug(String.format("saveOrUpdateMusicFile() - exit: inserted new record with ID %d (cache check: %dms, total: %dms)", musicFile.getId(), cacheCheckTime, totalTime));
        }
    }

    /**
     * Updates an existing music file record in the database.
     * 
     * <p>This method only performs the update if the MusicFile has been marked as modified.
     * If the file is not modified, the method returns immediately without database interaction
     * for performance optimization.
     * 
     * <p>The update operation:
     * <ol>
     *   <li>Checks if the MusicFile has been modified using isModified()</li>
     *   <li>Updates all metadata fields in the database record</li>
     *   <li>Uses the ID field to identify which record to update</li>
     *   <li>Marks the MusicFile as unmodified after successful update</li>
     * </ol>
     * 
     * @param musicFile the MusicFile object to update (must not be null, must have a valid ID)
     * @throws RuntimeException if database operation fails or connection is unavailable
     * @throws IllegalStateException if musicFile has no ID (cannot update unsaved record)
     */
    public static synchronized void updateMusicFile(MusicFile musicFile) {
        String sql = "UPDATE music_files SET file_path = ?, title = ?, artist = ?, album = ?, " +
                "genre = ?, track_number = ?, yr = ?, duration_seconds = ?, file_size_bytes = ?, " +
                "bit_rate = ?, sample_rate = ?, file_type = ?, last_modified = ? WHERE id = ?";

        if(!musicFile.isModified())// only save when data changed.
            return;

        // Get old file path for cache management (issue #41)
        String oldFilePath = null;
        String getOldPathSql = "SELECT file_path FROM music_files WHERE id = ?";
        try (PreparedStatement getPathStmt = getConnection().prepareStatement(getOldPathSql)) {
            getPathStmt.setLong(1, musicFile.getId());
            try (ResultSet rs = getPathStmt.executeQuery()) {
                if (rs.next()) {
                    oldFilePath = rs.getString("file_path");
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("Could not retrieve old file path for cache update: {}", e.getMessage()));
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, musicFile.getFilePath());
            pstmt.setString(2, musicFile.getTitle());
            pstmt.setString(3, musicFile.getArtist());
            pstmt.setString(4, musicFile.getAlbum());
            pstmt.setString(5, musicFile.getGenre());

            if (musicFile.getTrackNumber() != null) {
                pstmt.setInt(6, musicFile.getTrackNumber());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }

            if (musicFile.getYear() != null) {
                pstmt.setInt(7, musicFile.getYear());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }

            if (musicFile.getDurationSeconds() != null) {
                pstmt.setInt(8, musicFile.getDurationSeconds());
            } else {
                pstmt.setNull(8, Types.INTEGER);
            }

            if (musicFile.getFileSizeBytes() != null) {
                pstmt.setLong(9, musicFile.getFileSizeBytes());
            } else {
                pstmt.setNull(9, Types.BIGINT);
            }

            if (musicFile.getBitRate() != null) {
                pstmt.setLong(10, musicFile.getBitRate());
            } else {
                pstmt.setNull(10, Types.INTEGER);
            }

            if (musicFile.getSampleRate() != null) {
                pstmt.setInt(11, musicFile.getSampleRate());
            } else {
                pstmt.setNull(11, Types.INTEGER);
            }

            pstmt.setString(12, musicFile.getFileType());

            if (musicFile.getLastModified() != null) {
                pstmt.setTimestamp(13, new Timestamp(musicFile.getLastModified().getTime()));
            } else {
                pstmt.setNull(13, Types.TIMESTAMP);
            }

            pstmt.setLong(14, musicFile.getId());

            pstmt.executeUpdate();
            
            // Update cache if file path changed (issue #41)
            if (oldFilePath != null && !oldFilePath.equals(musicFile.getFilePath())) {
                filePathsMap.remove(oldFilePath);
                filePathsMap.put(musicFile.getFilePath(), musicFile.getId());
                logger.debug(String.format("Updated cache: removed '{}', added '{}'", oldFilePath, musicFile.getFilePath()));
            }
            
            musicFile.setModified(false);
            logger.debug(String.format("updateMusicFile() - exit: successfully updated {}", musicFile.getFilePath()));
        } catch (SQLException e) {
            logger.error(String.format("Failed to update music file: %s - SQL error: %s", musicFile.getFilePath(), e.getMessage()), e);
            throw new RuntimeException("Failed to update music file: " + musicFile.getFilePath(), e);
        }
    }

    public static synchronized boolean deleteMusicFile(MusicFile musicFile) {
        logger.debug(String.format("deleteMusicFile() - entry: %s", musicFile != null ? musicFile.getFilePath() : "null"));
        
        if (musicFile == null) {
            logger.error("deleteMusicFile() - musicFile parameter is null");
            throw new IllegalArgumentException("MusicFile cannot be null");
        }
        
        if (musicFile.getId() == null) {
            logger.error(String.format("deleteMusicFile() - musicFile has no ID: {}", musicFile.getFilePath()));
            throw new IllegalStateException("Cannot delete MusicFile without ID");
        }
        
        String sql = "DELETE FROM music_files WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, musicFile.getId());
            pstmt.executeUpdate();
            
            // Remove from cache  issue#41
            filePathsMap.remove(musicFile.getFilePath());
            
            musicFile.setId(null); // Clear the ID to indicate it's deleted'
            musicFile.setModified(false); // Clear the modified flag to indicate it's not modified
            boolean fileDeleted = musicFile.deleteFile();
            logger.debug(String.format("deleteMusicFile() - exit: database record deleted, file deletion: {}", fileDeleted));
            return fileDeleted;
        } catch (SQLException e) {
            logger.error(String.format("Failed to delete music file from database: %s - SQL error: %s", musicFile.getTitle(), e.getMessage()), e);
        }
        return false;
    }

    public static MusicFile getMusicFileById(Long id) {
        logger.debug(String.format("getMusicFileById() - entry: {}", id));
        
        if (id == null) {
            logger.debug("getMusicFileById() - null ID provided");
            return null;
        }
        
        String sql = "SELECT * FROM music_files WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    MusicFile result = extractMusicFileFromResultSet(rs);
                    logger.debug(String.format("getMusicFileById() - exit: found music file {}", result.getFilePath()));
                    return result;
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to retrieve music file by ID: {} - SQL error: {}", id, e.getMessage()), e);
            throw new RuntimeException("Failed to get music file by ID: " + id, e);
        }

        logger.debug(String.format("getMusicFileById() - exit: no music file found with ID {}", id));
        return null;
    }

    /**
     * Gets the count of music files in the database matching current file type filters.
     * 
     * <p>This method provides an efficient way to get the total number of music files
     * without loading all the data into memory. It respects the current file type
     * filtering configuration to return only the count of enabled file types.
     * 
     * <p>The count reflects the same filtering logic as getAllMusicFiles() but with
     * optimized performance for scenarios where only the total number is needed,
     * such as displaying statistics in the configuration panel.
     * 
     * @return the number of music files in the database matching current filters, 
     *         or -1 if database query fails (allowing UI to display "Unknown")
     */
    public static synchronized int getMusicFileCount() {
        logger.debug("getMusicFileCount() - entry");
        String sql = "SELECT COUNT(*) as file_count FROM music_files WHERE 1=1" + getFileTypeFilterClause();
        
        try (Statement stmt = ensureConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int count = rs.getInt("file_count");
                logger.debug(String.format("getMusicFileCount() - exit: found {} files", count));
                return count;
            }
            logger.debug("getMusicFileCount() - exit: no results, returning 0");
            return 0;
            
        } catch (SQLException e) {
            logger.error(String.format("getMusicFileCount() - SQL error: {}", e.getMessage()), e);
            logger.error(String.format("Error getting music file count: {}", e.getMessage()), e);
            // Return -1 to indicate error state rather than throwing exception
            // This allows the UI to display "Unknown" or "Error" instead of crashing
            return -1;
        }
    }

    public static void initAllPathsMap(){ // Load all paths for quick lookups  issue#41
        long startTime = System.currentTimeMillis();
        logger.info("Starting file path cache initialization...");
        
        filePathsMap.clear();
        
        // Optimized query to load only id and file_path
        String sql = "SELECT id, file_path FROM music_files";
        int count = 0;
        
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                filePathsMap.put(rs.getString("file_path"), rs.getLong("id"));
                count++;
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(String.format("File path cache initialized: {} entries loaded in {} ms", count, elapsedTime));
            
        } catch (SQLException e) {
            logger.error(String.format("Failed to initialize file paths cache: {}", e.getMessage()), e);
            throw new RuntimeException("Failed to initialize file paths cache", e);
        }
    }

    /**
     * Retrieves all music files from the database with intelligent sorting and filtering.
     * 
     * <p>This method provides the primary data access for displaying music collections.
     * The results are:
     * <ul>
     *   <li><strong>Filtered:</strong> Only includes file types enabled in configuration</li>
     *   <li><strong>Sorted:</strong> Ordered by artist, album, title (case-insensitive), then by bit rate and duration (descending)</li>
     *   <li><strong>Complete:</strong> All metadata fields are populated from database</li>
     * </ul>
     * 
     * <p>The sorting logic prioritizes:
     * <ol>
     *   <li>Artist name (alphabetically, case-insensitive)</li>
     *   <li>Album name (alphabetically, case-insensitive)</li>
     *   <li>Track title (alphabetically, case-insensitive)</li>
     *   <li>Higher bit rate (for quality preference)</li>
     *   <li>Longer duration (for completeness preference)</li>
     * </ol>
     * 
     * @return a list of all MusicFile objects matching the current file type filter, sorted as described
     * @throws RuntimeException if database query fails or connection is unavailable
     */
    public static synchronized List<MusicFile> getAllMusicFiles() {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE 1=1" + getFileTypeFilterClause() + 
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, " +
                    "bit_rate, duration_seconds DESC";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                musicFiles.add(extractMusicFileFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve all music files from database");
            throw new RuntimeException("Failed to get all music files", e);
        }

        return musicFiles;
    }

    /**
     * Gets all distinct parent directories from music files in the database.
     * 
     * <p>This method extracts the parent directory from each music file's path
     * and returns a sorted list of unique directories. This is useful for:
     * <ul>
     *   <li>Populating directory rescanning tables</li>
     *   <li>Understanding the directory structure of the music collection</li>
     *   <li>Selective directory operations</li>
     * </ul>
     * 
     * <p>The directories are:
     * <ul>
     *   <li><strong>Filtered:</strong> Only includes file types enabled in configuration</li>
     *   <li><strong>Unique:</strong> Each directory appears only once in the result</li>
     *   <li><strong>Sorted:</strong> Alphabetically sorted for consistent ordering</li>
     * </ul>
     * 
     * @return a sorted list of distinct directory paths containing music files
     * @throws RuntimeException if database query fails or connection is unavailable
     * @since 1.0
     */
    public static synchronized List<String> getDistinctDirectories() {
        List<String> directories = new ArrayList<>();
        Set<String> uniqueDirectories = new HashSet<>();
        
        String sql = "SELECT DISTINCT file_path FROM music_files WHERE 1=1" + getFileTypeFilterClause();
        
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String filePath = rs.getString("file_path");
                if (filePath != null) {
                    // Extract parent directory from file path
                    java.io.File file = new java.io.File(filePath);
                    String parentDir = file.getParent();
                    if (parentDir != null && uniqueDirectories.add(parentDir)) {
                        directories.add(parentDir);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get distinct directories from database");
            throw new RuntimeException("Failed to get distinct directories", e);
        }
        
        Collections.sort(directories);
        return directories;
    }

    /**
     * Records an original scan directory in the database.
     * 
     * <p>This method stores the root directory path that was selected by the user
     * for scanning, allowing the rescanning feature to show only meaningful
     * top-level directories rather than every subdirectory.
     * 
     * @param rootPath the original directory path selected for scanning
     * @throws RuntimeException if database operation fails
     * @since 1.0
     */
    public static synchronized void recordScanDirectory(String rootPath) {
        if (rootPath == null || rootPath.trim().isEmpty()) {
            return;
        }
        
        // First try to update existing record
        String updateSql = "UPDATE scan_directories SET scan_date = CURRENT_TIMESTAMP WHERE root_path = ?";
        try (PreparedStatement updateStmt = ensureConnection().prepareStatement(updateSql)) {
            updateStmt.setString(1, rootPath.trim());
            int updated = updateStmt.executeUpdate();
            
            if (updated > 0) {
                logger.debug(String.format("Updated existing scan directory: {}", rootPath));
                return;
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to update scan directory: {}", e.getMessage()), e);
        }
        
        // If no update occurred, insert new record
        String insertSql = "INSERT INTO scan_directories (root_path) VALUES (?)";
        try (PreparedStatement insertStmt = ensureConnection().prepareStatement(insertSql)) {
            insertStmt.setString(1, rootPath.trim());
            insertStmt.executeUpdate();
            logger.debug(String.format("Recorded new scan directory: {}", rootPath));
        } catch (SQLException e) {
            // If it's a duplicate key error (directory already exists), that's fine
            if (!e.getMessage().contains("duplicate") && !e.getMessage().contains("UNIQUE") && !e.getMessage().contains("constraint")) {
                logger.error(String.format("Failed to record scan directory: {}", e.getMessage()), e);
                throw new RuntimeException("Failed to record scan directory", e);
            } else {
                logger.debug(String.format("Scan directory already exists: {}", rootPath));
            }
        }
    }

    /**
     * Gets all original scan directories from the database.
     * 
     * <p>This method returns the root directories that were originally selected
     * by users for scanning, providing a clean list for the directory rescanning
     * feature instead of showing every subdirectory that contains music files.
     * 
     * @return a sorted list of original scan directory paths
     * @throws RuntimeException if database query fails
     * @since 1.0
     */
    public static synchronized List<String> getScanDirectories() {
        List<String> scanDirectories = new ArrayList<>();
        
        String sql = "SELECT root_path FROM scan_directories ORDER BY root_path";
        
        try (Statement stmt = ensureConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String rootPath = rs.getString("root_path");
                if (rootPath != null) {
                    scanDirectories.add(rootPath);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get scan directories from database");
            throw new RuntimeException("Failed to get scan directories", e);
        }
        
        return scanDirectories;
    }

    /**
     * Updates the last rescan timestamp for a scan directory.
     * 
     * @param rootPath the scan directory path to update
     * @since 1.0
     */
    public static synchronized void updateScanDirectoryRescanTime(String rootPath) {
        if (rootPath == null || rootPath.trim().isEmpty()) {
            return;
        }
        
        String sql = "UPDATE scan_directories SET last_rescan = CURRENT_TIMESTAMP WHERE root_path = ?";
        
        try (PreparedStatement stmt = ensureConnection().prepareStatement(sql)) {
            stmt.setString(1, rootPath.trim());
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.debug(String.format("Updated rescan time for directory: {}", rootPath));
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to update rescan time for directory {}: {}", rootPath, e.getMessage()), e);
        }
    }

    /**
     * Performs a comprehensive search across multiple metadata fields.
     * 
     * <p>This method searches for the given term across the following fields:
     * <ul>
     *   <li>Title</li>
     *   <li>Artist</li>
     *   <li>Album</li>
     *   <li>Genre</li>
     * </ul>
     * 
     * <p>The search is:
     * <ul>
     *   <li><strong>Case-insensitive:</strong> Converts both search term and data to lowercase</li>
     *   <li><strong>Partial match:</strong> Uses SQL LIKE with wildcards for substring matching</li>
     *   <li><strong>Filtered:</strong> Only includes file types enabled in configuration</li>
     *   <li><strong>Sorted:</strong> Same intelligent sorting as getAllMusicFiles()</li>
     * </ul>
     * 
     * @param searchTerm the text to search for across metadata fields (null or empty returns empty list)
     * @return a list of MusicFile objects where any metadata field contains the search term
     * @throws RuntimeException if database query fails or connection is unavailable
     */
    public static synchronized List<MusicFile> searchMusicFiles(String searchTerm) {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE (" +
                "LOWER(title) LIKE ? OR " +
                "LOWER(artist) LIKE ? OR " +
                "LOWER(album) LIKE ? OR " +
                "LOWER(genre) LIKE ?)" + getFileTypeFilterClause() + 
                " ORDER BY lower(artist), lower(album), lower(title) ASC, " +
                "bit_rate, duration_seconds DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            String term = "%" + searchTerm.toLowerCase() + "%";
            pstmt.setString(1, term);
            pstmt.setString(2, term);
            pstmt.setString(3, term);
            pstmt.setString(4, term);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to search music files with term: {}", searchTerm), e);
            throw new RuntimeException("Failed to search music files", e);
        }

        return musicFiles;
    }

    /**
     * Legacy method - finds potential duplicates using blocking algorithm.
     * @deprecated Use findPotentialDuplicatesParallel() for better performance with large datasets.
     */
    @Deprecated
    public static synchronized List<MusicFile> findPotentialDuplicates() {
        // Get the active profile's fuzzy search configuration
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        // Get all music files and apply fuzzy matching
        List<MusicFile> allFiles = getAllMusicFiles();
        
        // Use legacy method for backward compatibility
        return FuzzyMatcher.findFuzzyDuplicates(allFiles, fuzzyConfig);
    }
    
    /**
     * Finds potential duplicates grouped together for easier management.
     * Each group contains files that are considered duplicates of each other.
     */
    public static synchronized List<List<MusicFile>> findDuplicateGroups() {
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        List<MusicFile> allFiles = getAllMusicFiles();
        return FuzzyMatcher.groupDuplicates(allFiles, fuzzyConfig);
    }
    
    /**
     * Finds potential duplicates using parallel processing for better performance.
     * This method streams results via callback and can be cancelled mid-process.
     */
    public static synchronized void findPotentialDuplicatesParallel(FuzzyMatcher.DuplicateCallback callback) {
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        // Get all music files and use parallel fuzzy matching
        List<MusicFile> allFiles = getAllMusicFiles();
        FuzzyMatcher.findFuzzyDuplicatesParallel(allFiles, fuzzyConfig, callback);
    }
    
    /**
     * Legacy method - finds potential duplicates using optimized SQL pre-filtering.
     * @deprecated Use findPotentialDuplicatesParallel() for better performance with large datasets.
     */
    @Deprecated
    public static synchronized List<MusicFile> findPotentialDuplicatesOptimized() {
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        // Skip SQL pre-filtering as it's ineffective for fuzzy search - just use all files
        List<MusicFile> allFiles = getAllMusicFiles();
        
        // Use legacy method for backward compatibility
        return FuzzyMatcher.findFuzzyDuplicates(allFiles, fuzzyConfig);
    }
    
    /**
     * Gets potential duplicate candidates using SQL with loose matching criteria.
     */
    private static List<MusicFile> getPotentialDuplicateCandidates() {
        List<MusicFile> candidates = new ArrayList<>();
        String typeFilter = getFileTypeFilterClause().replace(" AND file_type", " AND m1.file_type");
        
        // Use loose SQL matching - files with similar length titles/artists or matching first few words
        String sql = "SELECT DISTINCT m1.* FROM music_files m1 " +
                    "JOIN music_files m2 ON m1.id < m2.id " +
                    "WHERE (" +
                    "  (LOWER(SUBSTR(m1.title, 1, 10)) = LOWER(SUBSTR(m2.title, 1, 10)) AND LENGTH(m1.title) > 5) OR " +
                    "  (LOWER(SUBSTR(m1.artist, 1, 10)) = LOWER(SUBSTR(m2.artist, 1, 10)) AND LENGTH(m1.artist) > 5) OR " +
                    "  (ABS(COALESCE(m1.duration_seconds, 0) - COALESCE(m2.duration_seconds, 0)) <= 30) OR " +
                    "  (LOWER(m1.artist) = LOWER(m2.artist) AND LOWER(m1.album) = LOWER(m2.album))" +
                    ")" + typeFilter +
                    " ORDER BY LOWER(m1.artist), LOWER(m1.album), LOWER(m1.title)";
        
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                candidates.add(extractMusicFileFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.warn(String.format("Optimized candidate search failed, falling back to full scan: {}", e.getMessage()), e);
            // Fall back to getting all files if the optimized query fails
            return getAllMusicFiles();
        }
        
        return candidates;
    }
    
    /**
     * Gets detailed similarity information between two music files for debugging.
     */
    public static String getSimilarityBreakdown(MusicFile file1, MusicFile file2) {
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        return FuzzyMatcher.getSimilarityBreakdown(file1, file2, fuzzyConfig);
    }
    
    /**
     * Checks if two specific music files are considered duplicates.
     */
    public static boolean areDuplicates(MusicFile file1, MusicFile file2) {
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        return FuzzyMatcher.areDuplicates(file1, file2, fuzzyConfig);
    }

    public static synchronized MusicFile findByPath(String path) {
        String sql = "SELECT * FROM music_files WHERE file_path = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, path);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractMusicFileFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to find music file by path: {}", path), e);
            throw new RuntimeException("Failed to find music file by path", e);
        }

        return null;
    }

    private static synchronized MusicFile extractMusicFileFromResultSet(ResultSet rs) throws SQLException {
        MusicFile musicFile = new MusicFile();

        musicFile.setId(rs.getLong("id"));
        musicFile.setFilePath(rs.getString("file_path"));
        musicFile.setTitle(rs.getString("title"));
        musicFile.setArtist(rs.getString("artist"));
        musicFile.setAlbum(rs.getString("album"));
        musicFile.setGenre(rs.getString("genre"));

        if (rs.getObject("track_number") != null) {
            musicFile.setTrackNumber(rs.getInt("track_number"));
        }

        if (rs.getObject("yr") != null) {
            musicFile.setYear(rs.getInt("yr"));
        }

        if (rs.getObject("duration_seconds") != null) {
            musicFile.setDurationSeconds(rs.getInt("duration_seconds"));
        }

        if (rs.getObject("file_size_bytes") != null) {
            musicFile.setFileSizeBytes(rs.getLong("file_size_bytes"));
        }

        if (rs.getObject("bit_rate") != null) {
            musicFile.setBitRate(rs.getLong("bit_rate"));
        }

        if (rs.getObject("sample_rate") != null) {
            musicFile.setSampleRate(rs.getInt("sample_rate"));
        }

        musicFile.setFileType(rs.getString("file_type"));

        // SQLite stores timestamps as milliseconds (long), not formatted dates
        if (rs.getObject("last_modified") != null) {
            long lastModifiedMs = rs.getLong("last_modified");
            musicFile.setLastModified(new Date(lastModifiedMs));
        }

        if (rs.getObject("date_added") != null) {
            long dateAddedMs = rs.getLong("date_added");
            musicFile.setDateAdded(new Date(dateAddedMs));
        }

        return musicFile;
    }

    public static synchronized List<MusicFile> searchMusicFilesByTitle(String title) {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE LOWER(title) LIKE ?" + getFileTypeFilterClause() + 
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, " +
                    "bit_rate, duration_seconds DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, "%" + title.toLowerCase() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to search music files by title: {}", title), e);
            throw new RuntimeException("Failed to search music files by title", e);
        }

        return musicFiles;
    }

    public static synchronized List<MusicFile> searchMusicFilesByArtist(String artist) {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE LOWER(artist) LIKE ?" + getFileTypeFilterClause() + 
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, " +
                    "bit_rate, duration_seconds DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, "%" + artist.toLowerCase() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to search music files by artist: {}", artist), e);
            throw new RuntimeException("Failed to search music files by artist", e);
        }

        return musicFiles;
    }

    public static synchronized List<MusicFile> searchMusicFilesByAlbum(String album) {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE LOWER(album) LIKE ?" + getFileTypeFilterClause() + 
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, " +
                    "bit_rate, duration_seconds DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, "%" + album.toLowerCase() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to search music files by album: {}", album), e);
            throw new RuntimeException("Failed to search music files by album", e);
        }

        return musicFiles;
    }

    public static synchronized void deleteAllMusicFiles() {
        String sql = "DELETE FROM music_files";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
            
            // Clear the cache  issue#41
            filePathsMap.clear();
            logger.debug("File path cache cleared after deleting all music files");
        } catch (SQLException e) {
            logger.error("Failed to delete all music files from database");
            throw new RuntimeException("Failed to delete all music files", e);
        }
    }
    
    /**
     * Clears the file path cache. Used primarily for testing.
     * issue#41
     */
    public static void clearFilePathCache() {
        filePathsMap.clear();
        logger.debug("File path cache cleared manually");
    }
    
    /**
     * Rebuilds the file path cache if it appears to be out of sync.
     * Issue #42 - Provides user-accessible recovery mechanism
     */
    public static void rebuildFilePathCache() {
        logger.info("Rebuilding file path cache...");
        filePathsMap.clear();
        try {
            initAllPathsMap();
            logger.info(String.format("File path cache rebuilt successfully with {} entries", filePathsMap.size()));
        } catch (Exception e) {
            logger.error(String.format("Failed to rebuild file path cache: {}", e.getMessage()), e);
            throw new RuntimeException("Failed to rebuild file path cache", e);
        }
    }
    
    /**
     * Gets file ID by path with fallback to database query if cache is empty.
     * Issue #42 - Critical fix for database appearing empty after restart
     */
    public static Long getFileIdByPath(String filePath) {
        // Try cache first
        Long id = filePathsMap.get(filePath);
        if (id != null) {
            return id;
        }
        
        // If cache is empty but database has data, query database directly
        if (filePathsMap.isEmpty()) {
            MusicFile existing = findByPathDirect(filePath);
            if (existing != null) {
                // Cache the result for future use
                filePathsMap.put(filePath, existing.getId());
                logger.debug(String.format("Found file via database fallback and cached: {}", filePath));
                return existing.getId();
            }
        }
        
        return null; // File doesn't exist
    }
    
    /**
     * Direct database query for file by path (bypasses cache).
     * Used as fallback when cache is empty.
     */
    private static MusicFile findByPathDirect(String path) {
        String sql = "SELECT * FROM music_files WHERE file_path = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, path);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractMusicFileFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("Database fallback query failed for path {}: {}", path, e.getMessage()));
        }

        return null;
    }

    public static synchronized List<MusicFile> searchMusicFiles(String title, String artist, String album) {
        List<MusicFile> musicFiles = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM music_files WHERE ");
        List<String> conditions = new ArrayList<>();
        List<String> parameters = new ArrayList<>();

        if (title != null && !title.isEmpty()) {
            conditions.add("LOWER(title) LIKE ?");
            parameters.add("%" + title.toLowerCase() + "%");
        }
        if (artist != null && !artist.isEmpty()) {
            conditions.add("LOWER(artist) LIKE ?");
            parameters.add("%" + artist.toLowerCase() + "%");
        }
        if (album != null && !album.isEmpty()) {
            conditions.add("LOWER(album) LIKE ?");
            parameters.add("%" + album.toLowerCase() + "%");
        }

        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("At least one search parameter must be provided");
        }

        sql.append(String.join(" AND ", conditions) + getFileTypeFilterClause() + 
                " ORDER BY lower(artist), lower(album), lower(title) ASC, " +
                "bit_rate, duration_seconds DESC");

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setString(i + 1, parameters.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Failed to search music files with multiple criteria - title: {}, artist: {}, album: {}", title, artist, album), e);
            throw new RuntimeException("Failed to search music files", e);
        }

        return musicFiles;
    }

    /**
     * Gets information about the current database configuration.
     */
    public static String getDatabaseInfo() {
        return config.getConfigurationInfo();
    }
    
    /**
     * Generates a SQL WHERE clause fragment for file type filtering.
     */
    private static String getFileTypeFilterClause() {
        Set<String> enabledTypes = config.getEnabledFileTypes();
        if (enabledTypes.isEmpty() || enabledTypes.size() == DatabaseConfig.getAllSupportedTypes().length) {
            // No filtering needed if all types are enabled
            return "";
        }
        
        StringBuilder clause = new StringBuilder(" AND file_type IN (");
        String delimiter = "";
        for (String type : enabledTypes) {
            clause.append(delimiter).append("'").append(type.toLowerCase()).append("'");
            delimiter = ", ";
        }
        clause.append(")");
        return clause.toString();
    }
}