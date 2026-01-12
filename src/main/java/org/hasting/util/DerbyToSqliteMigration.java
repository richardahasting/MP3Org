package org.hasting.util;

import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration utility for converting existing Derby databases to SQLite format.
 *
 * <p>This utility was created as part of Issue #72 to migrate MP3Org from Apache Derby
 * to SQLite. It provides a one-time migration path for users with existing Derby databases.
 *
 * <p><strong>Why SQLite?</strong>
 * <ul>
 *   <li>Single file database - no lock files, easy to backup/move</li>
 *   <li>No database locks - multiple processes can read</li>
 *   <li>Better tooling - can inspect with standard SQLite browsers</li>
 *   <li>Perfect for read-heavy single-user applications like MP3Org</li>
 * </ul>
 *
 * <p><strong>Usage:</strong>
 * <pre>
 * // Migrate a Derby database to SQLite
 * DerbyToSqliteMigration migration = new DerbyToSqliteMigration(
 *     "/path/to/derby/mp3org",  // Derby database path (directory)
 *     "/path/to/new/mp3org.db"  // SQLite database path (file)
 * );
 * migration.migrate();
 * </pre>
 *
 * @author MP3Org Development Team
 * @version 1.0
 * @since 2.0
 * @see DatabaseManager
 */
public class DerbyToSqliteMigration {
    private static final Logger logger = Log4Rich.getLogger(DerbyToSqliteMigration.class);

    private final String derbyPath;
    private final String sqlitePath;

    /**
     * Creates a new migration instance.
     *
     * @param derbyPath path to the Derby database directory (e.g., "/path/to/mp3org")
     * @param sqlitePath path for the new SQLite database file (e.g., "/path/to/mp3org.db")
     */
    public DerbyToSqliteMigration(String derbyPath, String sqlitePath) {
        this.derbyPath = derbyPath;
        this.sqlitePath = sqlitePath;
    }

    /**
     * Performs the database migration from Derby to SQLite.
     *
     * @return true if migration was successful, false otherwise
     */
    public boolean migrate() {
        logger.info("Starting Derby to SQLite migration");
        logger.info("  Source (Derby): {}", derbyPath);
        logger.info("  Target (SQLite): {}", sqlitePath);

        // Validate Derby database exists
        File derbyDir = new File(derbyPath);
        if (!derbyDir.exists() || !derbyDir.isDirectory()) {
            logger.error("Derby database directory not found: {}", derbyPath);
            return false;
        }

        // Check if SQLite database already exists
        File sqliteFile = new File(sqlitePath);
        if (sqliteFile.exists()) {
            logger.warn("SQLite database already exists at: {}. Migration will add to existing data.", sqlitePath);
        }

        Connection derbyConn = null;
        Connection sqliteConn = null;

        try {
            // Connect to Derby
            String derbyUrl = "jdbc:derby:" + derbyPath;
            logger.info("Connecting to Derby database...");
            derbyConn = DriverManager.getConnection(derbyUrl);

            // Connect to SQLite
            String sqliteUrl = "jdbc:sqlite:" + sqlitePath;
            logger.info("Connecting to SQLite database...");
            sqliteConn = DriverManager.getConnection(sqliteUrl);

            // Create tables in SQLite
            createSqliteTables(sqliteConn);

            // Migrate music_files table
            int musicFileCount = migrateMusicFiles(derbyConn, sqliteConn);
            logger.info("Migrated {} music file records", musicFileCount);

            // Migrate scan_directories table
            int scanDirCount = migrateScanDirectories(derbyConn, sqliteConn);
            logger.info("Migrated {} scan directory records", scanDirCount);

            logger.info("Migration completed successfully!");
            logger.info("  Total music files: {}", musicFileCount);
            logger.info("  Total scan directories: {}", scanDirCount);

            return true;

        } catch (SQLException e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection(derbyConn, "Derby");
            closeConnection(sqliteConn, "SQLite");
        }
    }

    /**
     * Creates the required tables in SQLite database.
     */
    private void createSqliteTables(Connection conn) throws SQLException {
        logger.info("Creating SQLite tables...");

        // Create music_files table
        String musicFilesTable = "CREATE TABLE IF NOT EXISTS music_files (" +
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

        // Create scan_directories table
        String scanDirsTable = "CREATE TABLE IF NOT EXISTS scan_directories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "root_path TEXT NOT NULL UNIQUE, " +
                "scan_date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "last_rescan TEXT, " +
                "file_count INTEGER DEFAULT 0" +
                ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(musicFilesTable);
            stmt.executeUpdate(scanDirsTable);
            logger.info("SQLite tables created successfully");
        }
    }

    /**
     * Migrates music_files data from Derby to SQLite.
     */
    private int migrateMusicFiles(Connection derbyConn, Connection sqliteConn) throws SQLException {
        logger.info("Migrating music_files table...");

        String selectSql = "SELECT file_path, title, artist, album, genre, track_number, " +
                          "yr, duration_seconds, file_size_bytes, bit_rate, sample_rate, " +
                          "file_type, last_modified, date_added FROM music_files";

        String insertSql = "INSERT OR IGNORE INTO music_files (file_path, title, artist, album, genre, " +
                          "track_number, yr, duration_seconds, file_size_bytes, bit_rate, sample_rate, " +
                          "file_type, last_modified, date_added) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int count = 0;

        try (Statement stmt = derbyConn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement pstmt = sqliteConn.prepareStatement(insertSql)) {

            sqliteConn.setAutoCommit(false);

            while (rs.next()) {
                pstmt.setString(1, rs.getString("file_path"));
                pstmt.setString(2, rs.getString("title"));
                pstmt.setString(3, rs.getString("artist"));
                pstmt.setString(4, rs.getString("album"));
                pstmt.setString(5, rs.getString("genre"));
                setIntOrNull(pstmt, 6, rs, "track_number");
                setIntOrNull(pstmt, 7, rs, "yr");
                setIntOrNull(pstmt, 8, rs, "duration_seconds");
                setLongOrNull(pstmt, 9, rs, "file_size_bytes");
                setIntOrNull(pstmt, 10, rs, "bit_rate");
                setIntOrNull(pstmt, 11, rs, "sample_rate");
                pstmt.setString(12, rs.getString("file_type"));

                // Convert timestamps to ISO strings for SQLite
                Timestamp lastModified = rs.getTimestamp("last_modified");
                pstmt.setString(13, lastModified != null ? lastModified.toString() : null);

                Timestamp dateAdded = rs.getTimestamp("date_added");
                pstmt.setString(14, dateAdded != null ? dateAdded.toString() : null);

                pstmt.addBatch();
                count++;

                // Commit in batches of 1000
                if (count % 1000 == 0) {
                    pstmt.executeBatch();
                    sqliteConn.commit();
                    logger.info("  Migrated {} records...", count);
                }
            }

            // Final batch
            pstmt.executeBatch();
            sqliteConn.commit();
            sqliteConn.setAutoCommit(true);
        }

        return count;
    }

    /**
     * Migrates scan_directories data from Derby to SQLite.
     */
    private int migrateScanDirectories(Connection derbyConn, Connection sqliteConn) throws SQLException {
        logger.info("Migrating scan_directories table...");

        // Check if scan_directories table exists in Derby
        try (ResultSet tables = derbyConn.getMetaData().getTables(null, null, "SCAN_DIRECTORIES", null)) {
            if (!tables.next()) {
                logger.info("No scan_directories table found in Derby database - skipping");
                return 0;
            }
        }

        String selectSql = "SELECT root_path, scan_date, last_rescan, file_count FROM scan_directories";
        String insertSql = "INSERT OR IGNORE INTO scan_directories (root_path, scan_date, last_rescan, file_count) " +
                          "VALUES (?, ?, ?, ?)";

        int count = 0;

        try (Statement stmt = derbyConn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement pstmt = sqliteConn.prepareStatement(insertSql)) {

            while (rs.next()) {
                pstmt.setString(1, rs.getString("root_path"));

                Timestamp scanDate = rs.getTimestamp("scan_date");
                pstmt.setString(2, scanDate != null ? scanDate.toString() : null);

                Timestamp lastRescan = rs.getTimestamp("last_rescan");
                pstmt.setString(3, lastRescan != null ? lastRescan.toString() : null);

                pstmt.setInt(4, rs.getInt("file_count"));
                pstmt.executeUpdate();
                count++;
            }
        }

        return count;
    }

    /**
     * Helper method to set integer value or null.
     */
    private void setIntOrNull(PreparedStatement pstmt, int paramIndex, ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        if (rs.wasNull()) {
            pstmt.setNull(paramIndex, Types.INTEGER);
        } else {
            pstmt.setInt(paramIndex, value);
        }
    }

    /**
     * Helper method to set long value or null.
     */
    private void setLongOrNull(PreparedStatement pstmt, int paramIndex, ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        if (rs.wasNull()) {
            pstmt.setNull(paramIndex, Types.INTEGER);
        } else {
            pstmt.setLong(paramIndex, value);
        }
    }

    /**
     * Safely closes a database connection.
     */
    private void closeConnection(Connection conn, String name) {
        if (conn != null) {
            try {
                conn.close();
                logger.debug("{} connection closed", name);
            } catch (SQLException e) {
                logger.warn("Error closing {} connection: {}", name, e.getMessage());
            }
        }
    }

    /**
     * Command-line interface for running migration.
     *
     * <p>Usage: java DerbyToSqliteMigration &lt;derby-path&gt; &lt;sqlite-path&gt;
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DerbyToSqliteMigration <derby-path> <sqlite-path>");
            System.out.println("  derby-path  : Path to existing Derby database directory");
            System.out.println("  sqlite-path : Path for new SQLite database file");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  java DerbyToSqliteMigration /path/to/mp3org /path/to/mp3org.db");
            System.exit(1);
        }

        DerbyToSqliteMigration migration = new DerbyToSqliteMigration(args[0], args[1]);
        boolean success = migration.migrate();
        System.exit(success ? 0 : 1);
    }
}
