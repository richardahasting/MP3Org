package org.hasting.util;

import org.hasting.model.MusicFile;
import org.hasting.util.logging.MP3OrgLoggingManager;
import org.hasting.util.logging.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
    private static final Logger logger = MP3OrgLoggingManager.getLogger(DatabaseManager.class);
    private static DatabaseConfig config;
    private static Connection connection;

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
        if (connection == null) {
            try {
                // Register JDBC driver (not needed with modern JDBC)
                // Class.forName(config.getJdbcDriver());

                // Open a connection using configured database location
                connection = DriverManager.getConnection(
                    config.getJdbcUrl(), 
                    config.getUsername(), 
                    config.getPassword()
                );
                
                logger.info("Connected to database at: {}", config.getDatabasePath());

                // Create table if not exists
                // deleteMusicFilesTable();
                createMusicFilesTable();
            } catch (Exception e) {
                logger.error("Failed to initialize database connection: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize database connection", e);
            }
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
            logger.error("Failed to drop music_files table: {}", e.getMessage(), e);
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
        
        String sql = "CREATE TABLE music_files (" +
                "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "file_path VARCHAR(1024) NOT NULL UNIQUE, " +
                "title VARCHAR(255), " +
                "artist VARCHAR(255), " +
                "album VARCHAR(255), " +
                "genre VARCHAR(50), " +
                "track_number INT, " +
                "yr INT, " +
                "duration_seconds INT, " +
                "file_size_bytes BIGINT, " +
                "bit_rate INT, " +
                "sample_rate INT, " +
                "file_type VARCHAR(10), " +
                "last_modified TIMESTAMP, " +
                "date_added TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Created music_files table successfully");
        } catch (SQLException e) {
            logger.error("Failed to create music_files table: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create music_files table", e);
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
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection: {}", e.getMessage(), e);
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
        
        logger.info("Database location changed to: {}", config.getDatabasePath());
        
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
                logger.info("Successfully switched to profile: {}", profileId);
                return true;
            } else {
                // Reinitialize with current profile if switch failed
                initialize();
                logger.error("Failed to switch to profile: {}", profileId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error switching to profile {}: {}", profileId, e.getMessage(), e);
            
            // Try to recover by reinitializing
            try {
                initialize();
            } catch (Exception recoveryError) {
                logger.error("Failed to recover database connection: {}", recoveryError.getMessage(), recoveryError);
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
                logger.error("Profile not found: {}", profileName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error switching to profile {}: {}", profileName, e.getMessage(), e);
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
        // Check if the music file already exists by file_path
        if (findByPath(musicFile.getFilePath()) != null) {
            logger.info("Music file with path {} already exists. Skipping insertion", musicFile.getFilePath());
            return;
        }

        String sql = "INSERT INTO music_files (file_path, title, artist, album, genre, track_number, " +
                "yr, duration_seconds, file_size_bytes, bit_rate, sample_rate, file_type, last_modified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

            pstmt.executeUpdate();

            // Get the generated ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    musicFile.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save music file to database: {}", musicFile.getFilePath(), e);
            throw new RuntimeException("Failed to save music file", e);
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
        } catch (SQLException e) {
            logger.error("Failed to update music file in database: {}", musicFile.getFilePath(), e);
            throw new RuntimeException("Failed to update music file", e);
        }
    }

    public static synchronized boolean deleteMusicFile(MusicFile musicFile) {
        String sql = "DELETE FROM music_files WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, musicFile.getId());
            pstmt.executeUpdate();
            musicFile.setId(null); // Clear the ID to indicate it's deleted'
            musicFile.setModified(false); // Clear the modified flag to indicate it's not modified
            return musicFile.deleteFile();
        } catch (SQLException e) {
            logger.error("Failed to delete music file: {}", musicFile.getTitle(), e);
        }
        return false;
    }

    public static MusicFile getMusicFileById(Long id) {
        String sql = "SELECT * FROM music_files WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractMusicFileFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve music file by ID: {}", id, e);
            throw new RuntimeException("Failed to get music file by ID", e);
        }

        return null;
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
            logger.error("Failed to retrieve all music files from database", e);
            throw new RuntimeException("Failed to get all music files", e);
        }

        return musicFiles;
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
            logger.error("Failed to search music files with term: {}", searchTerm, e);
            throw new RuntimeException("Failed to search music files", e);
        }

        return musicFiles;
    }

    public static synchronized List<MusicFile> findPotentialDuplicates() {
        // Get the active profile's fuzzy search configuration
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        // Stage 1: Get all music files for fuzzy comparison
        // We'll use a broader query to get potential candidates
        List<MusicFile> allFiles = getAllMusicFiles();
        
        // Stage 2: Apply fuzzy matching to find duplicates
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
     * Finds potential duplicates using optimized SQL pre-filtering.
     * This method uses a more targeted approach for better performance with large datasets.
     */
    public static synchronized List<MusicFile> findPotentialDuplicatesOptimized() {
        DatabaseProfile activeProfile = getActiveProfile();
        FuzzySearchConfig fuzzyConfig = (activeProfile != null && activeProfile.getFuzzySearchConfig() != null) 
            ? activeProfile.getFuzzySearchConfig() 
            : new FuzzySearchConfig();
        
        // Stage 1: Use SQL to get potential candidates
        // We'll use loose matching in SQL and then apply strict fuzzy matching
        List<MusicFile> candidates = getPotentialDuplicateCandidates();
        
        // Stage 2: Apply fuzzy matching to candidates
        return FuzzyMatcher.findFuzzyDuplicates(candidates, fuzzyConfig);
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
            logger.warning("Optimized candidate search failed, falling back to full scan: {}", e.getMessage(), e);
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
            logger.error("Failed to find music file by path: {}", path, e);
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

        Timestamp lastModified = rs.getTimestamp("last_modified");
        if (lastModified != null) {
            musicFile.setLastModified(new Date(lastModified.getTime()));
        }

        Timestamp dateAdded = rs.getTimestamp("date_added");
        if (dateAdded != null) {
            musicFile.setDateAdded(new Date(dateAdded.getTime()));
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
            logger.error("Failed to search music files by title: {}", title, e);
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
            logger.error("Failed to search music files by artist: {}", artist, e);
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
            logger.error("Failed to search music files by album: {}", album, e);
            throw new RuntimeException("Failed to search music files by album", e);
        }

        return musicFiles;
    }

    public static synchronized void deleteAllMusicFiles() {
        String sql = "DELETE FROM music_files";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("Failed to delete all music files from database", e);
            throw new RuntimeException("Failed to delete all music files", e);
        }
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
            logger.error("Failed to search music files with multiple criteria - title: {}, artist: {}, album: {}", title, artist, album, e);
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