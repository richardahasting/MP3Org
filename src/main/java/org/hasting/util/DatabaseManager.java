package org.hasting.util;

import org.hasting.model.MusicFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DatabaseManager {
    private static DatabaseConfig config;
    private static Connection connection;

    static {
        // Initialize configuration
        config = DatabaseConfig.getInstance();
        // Create sample config file if it doesn't exist
        DatabaseConfig.createSampleConfigFile();
    }

    public static synchronized void initialize() {
        if (connection == null) {
            try {
                // Register JDBC driver (not needed with modern JDBC)
                // Class.forName(config.getJdbcDriver());

                // Open a connection using configured database location
                connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
                
                System.out.println("Connected to database at: " + config.getDatabasePath());

                // Create table if not exists
                // deleteMusicFilesTable();
                createMusicFilesTable();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize database connection", e);
            }
        }
    }


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
            // e.printStackTrace();
            // throw new RuntimeException("Failed to drop music_files table", e);
        }
    }

    private static synchronized void createMusicFilesTable() {
        String sql = "CREATE TABLE music_files (" +
                "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " + // Keep id as the primary key
                "file_path VARCHAR(1024) NOT NULL UNIQUE, " + // Add unique constraint to file_path
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
        } catch (SQLException e) {
            // e.printStackTrace();
            // ignore the error and continue with the application
            // throw new RuntimeException("Failed to create music_files table", e);
        }
    }

    public static synchronized Connection getConnection() {
        if (connection == null) {
            initialize();
        }
        return connection;
    }

    public static synchronized void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
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
        
        System.out.println("Database location changed to: " + config.getDatabasePath());
        
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
                System.out.println("Successfully switched to profile: " + profileId);
                return true;
            } else {
                // Reinitialize with current profile if switch failed
                initialize();
                System.err.println("Failed to switch to profile: " + profileId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error switching to profile " + profileId + ": " + e.getMessage());
            e.printStackTrace();
            
            // Try to recover by reinitializing
            try {
                initialize();
            } catch (Exception recoveryError) {
                System.err.println("Failed to recover database connection: " + recoveryError.getMessage());
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
                System.err.println("Profile not found: " + profileName);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error switching to profile " + profileName + ": " + e.getMessage());
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

    public static synchronized void saveMusicFile(MusicFile musicFile) {
        // Check if the music file already exists by file_path
        if (findByPath(musicFile.getFilePath()) != null) {
            System.out.println("Music file with path " + musicFile.getFilePath() + " already exists. Skipping insertion.");
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
            e.printStackTrace();
            throw new RuntimeException("Failed to save music file", e);
        }
    }

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
            e.printStackTrace();
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
            System.err.println("Failed to delete music file: " + musicFile.getTitle());
            System.err.println(e.getMessage());
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
            e.printStackTrace();
            throw new RuntimeException("Failed to get music file by ID", e);
        }

        return null;
    }

    public static synchronized List<MusicFile> getAllMusicFiles() {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE 1=1" + getFileTypeFilterClause() + 
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, bit_rate, duration_seconds DESC";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                musicFiles.add(extractMusicFileFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get all music files", e);
        }

        return musicFiles;
    }

    public static synchronized List<MusicFile> searchMusicFiles(String searchTerm) {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE (" +
                "LOWER(title) LIKE ? OR " +
                "LOWER(artist) LIKE ? OR " +
                "LOWER(album) LIKE ? OR " +
                "LOWER(genre) LIKE ?)" + getFileTypeFilterClause() + 
                " ORDER BY lower(artist), lower(album), lower(title) ASC, bit_rate, duration_seconds DESC";

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
            e.printStackTrace();
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
            System.err.println("Warning: Optimized candidate search failed, falling back to full scan: " + e.getMessage());
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
            e.printStackTrace();
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
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, bit_rate, duration_seconds DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, "%" + title.toLowerCase() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to search music files by title", e);
        }

        return musicFiles;
    }

    public static synchronized List<MusicFile> searchMusicFilesByArtist(String artist) {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE LOWER(artist) LIKE ?" + getFileTypeFilterClause() + 
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, bit_rate, duration_seconds DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, "%" + artist.toLowerCase() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to search music files by artist", e);
        }

        return musicFiles;
    }

    public static synchronized List<MusicFile> searchMusicFilesByAlbum(String album) {
        List<MusicFile> musicFiles = new ArrayList<>();
        String sql = "SELECT * FROM music_files WHERE LOWER(album) LIKE ?" + getFileTypeFilterClause() + 
                    " ORDER BY lower(artist), lower(album), lower(title) ASC, bit_rate, duration_seconds DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, "%" + album.toLowerCase() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    musicFiles.add(extractMusicFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to search music files by album", e);
        }

        return musicFiles;
    }

    public static synchronized void deleteAllMusicFiles() {
        String sql = "DELETE FROM music_files";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
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

        sql.append(String.join(" AND ", conditions) + getFileTypeFilterClause() + " ORDER BY lower(artist), lower(album), lower(title) ASC, bit_rate, duration_seconds DESC");

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
            e.printStackTrace();
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