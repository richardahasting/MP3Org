package org.hasting.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connection testing and lock detection for the MP3Org application.
 * 
 * <p>This class provides utilities to test database availability without interfering
 * with the main application's database operations. It specializes in detecting Derby
 * database lock conditions that occur when multiple application instances attempt
 * to access the same database simultaneously.
 * 
 * <p><strong>Key Capabilities:</strong>
 * <ul>
 *   <li>Quick database availability testing with minimal resource usage</li>
 *   <li>Derby-specific lock exception recognition and handling</li>
 *   <li>Non-invasive connection testing that doesn't affect primary database usage</li>
 *   <li>Clear error reporting for different database lock scenarios</li>
 * </ul>
 * 
 * <p>This class follows the principle of self-documenting code where method names
 * clearly communicate their purpose and the implementation teaches the database
 * lock detection pattern to future developers.
 * 
 * @author MP3Org Development Team
 * @version 1.0
 * @since 1.0
 * @see DatabaseProfileManager
 * @see DatabaseManager
 */
public class DatabaseConnectionManager {
    
    private static final int CONNECTION_TIMEOUT_SECONDS = 5;
    
    /**
     * Tests if a Derby database at the given path is currently locked by another process.
     * 
     * <p>This method performs a quick connection test without creating tables or
     * performing any database operations that might interfere with existing connections.
     * It specifically looks for Derby database lock exceptions that indicate another
     * process is currently using the database.
     * 
     * <p><strong>Lock Detection Strategy:</strong>
     * <ol>
     *   <li>Attempt a brief connection to the database</li>
     *   <li>Immediately close the connection if successful</li>
     *   <li>Analyze any SQLException for Derby-specific lock indicators</li>
     *   <li>Return true if lock detected, false if database is available</li>
     * </ol>
     * 
     * @param databasePath the file system path to the Derby database directory
     * @return true if the database is locked by another process, false if available
     * @throws IllegalArgumentException if databasePath is null or empty
     */
    public static boolean isDerbyDatabaseLockedByAnotherProcess(String databasePath) {
        if (databasePath == null || databasePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Database path cannot be null or empty");
        }
        
        try (Connection testConnection = createQuickTestConnection(databasePath)) {
            // Connection succeeded - database is available
            return false;
        } catch (SQLException e) {
            // Analyze the exception to determine if it's a lock-related issue
            return isDerbyLockException(e);
        }
    }
    
    /**
     * Tests if a database profile's configured database is currently available.
     * 
     * <p>This is a convenience method that extracts the database path from a
     * DatabaseProfile and tests it for lock conditions. This method name clearly
     * communicates its purpose in the context of profile management.
     * 
     * @param profile the database profile to test for availability
     * @return true if the profile's database is available, false if locked
     * @throws IllegalArgumentException if profile is null or has invalid database path
     */
    public static boolean isDatabaseAvailableForProfile(DatabaseProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Database profile cannot be null");
        }
        
        String databasePath = profile.getDatabasePath();
        if (databasePath == null || databasePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile " + profile.getName() + " has invalid database path");
        }
        
        return !isDerbyDatabaseLockedByAnotherProcess(databasePath);
    }
    
    /**
     * Creates a quick test connection to the specified Derby database.
     * 
     * <p>This connection is designed for testing purposes only and includes:
     * <ul>
     *   <li>Short connection timeout to avoid blocking</li>
     *   <li>No database creation if it doesn't exist</li>
     *   <li>Minimal resource allocation for quick cleanup</li>
     * </ul>
     * 
     * @param databasePath the path to the Derby database
     * @return a test connection that should be closed immediately
     * @throws SQLException if connection cannot be established
     */
    private static Connection createQuickTestConnection(String databasePath) throws SQLException {
        String jdbcUrl = "jdbc:derby:" + databasePath + ";create=false";
        
        // Set a short timeout to avoid blocking the application startup
        DriverManager.setLoginTimeout(CONNECTION_TIMEOUT_SECONDS);
        
        return DriverManager.getConnection(jdbcUrl, "", "");
    }
    
    /**
     * Recognizes Derby-specific error codes that indicate database lock conflicts.
     * 
     * <p>Derby uses specific SQL state codes and error messages to indicate different
     * types of database lock conditions:
     * <ul>
     *   <li><strong>XJ040:</strong> Database cannot be accessed while in snapshot mode</li>
     *   <li><strong>XJ041:</strong> Database already exists and is being used by another process</li>
     *   <li><strong>XBM0J:</strong> Cannot create database because it's locked</li>
     * </ul>
     * 
     * <p>This method also checks for common lock-related keywords in error messages
     * to catch additional lock scenarios that might not use standard SQL states.
     * 
     * @param e the SQLException to analyze for lock indicators
     * @return true if the exception indicates a database lock condition
     */
    private static boolean isDerbyLockException(SQLException e) {
        String sqlState = e.getSQLState();
        String message = e.getMessage().toLowerCase();
        
        // Check for specific Derby lock-related SQL states
        if ("XJ040".equals(sqlState) || 
            "XJ041".equals(sqlState) || 
            "XBM0J".equals(sqlState)) {
            return true;
        }
        
        // Check for lock-related keywords in error messages
        return message.contains("locked") ||
               message.contains("another process") ||
               message.contains("already in use") ||
               message.contains("cannot access") ||
               message.contains("database already exists");
    }
    
    /**
     * Provides a human-readable explanation of why a database connection failed.
     * 
     * <p>This method transforms technical Derby error messages into clear explanations
     * that can be displayed to users or logged for troubleshooting. It helps bridge
     * the gap between technical database errors and user-friendly communication.
     * 
     * @param e the SQLException that occurred during connection attempt
     * @param databasePath the path that was being accessed when the error occurred
     * @return a clear, human-readable explanation of the connection failure
     */
    public static String explainConnectionFailure(SQLException e, String databasePath) {
        if (isDerbyLockException(e)) {
            return "Database at '" + databasePath + "' is currently being used by another " +
                   "MP3Org instance. The application will automatically use an alternative database.";
        }
        
        return "Cannot connect to database at '" + databasePath + "': " + e.getMessage();
    }
}