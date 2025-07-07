package org.hasting.util;

import org.hasting.util.logging.MP3OrgLoggingManager;
import org.hasting.util.logging.Logger;

import java.sql.*;

/**
 * Improved database connection manager that ensures connections are properly closed after use.
 * This class provides methods that automatically manage connection lifecycle.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Automatic connection closing after each operation</li>
 *   <li>Explicit commit for all write operations</li>
 *   <li>Proper rollback on errors</li>
 *   <li>Connection validation before use</li>
 * </ul>
 */
public class DatabaseConnectionManager {
    private static final Logger logger = MP3OrgLoggingManager.getLogger(DatabaseConnectionManager.class);
    private static final int CONNECTION_TIMEOUT_SECONDS = 5;
    
    /**
     * Creates a new database connection that must be closed after use.
     * 
     * @param config the database configuration
     * @return a new database connection
     * @throws SQLException if connection creation fails
     */
    public static Connection createConnection(DatabaseConfig config) throws SQLException {
        Connection conn = DriverManager.getConnection(
            config.getJdbcUrl(),
            config.getUsername(), 
            config.getPassword()
        );
        
        // Set auto-commit to false for explicit transaction control
        conn.setAutoCommit(false);
        
        return conn;
    }
    
    /**
     * Validates if a connection is still usable.
     * 
     * @param conn the connection to validate
     * @return true if the connection is valid, false otherwise
     */
    public static boolean isConnectionValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(CONNECTION_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Executes a database operation with automatic connection management.
     * This method ensures the connection is properly closed after use.
     * 
     * @param config the database configuration
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws SQLException if the operation fails
     */
    public static <T> T executeWithNewConnection(DatabaseConfig config, ConnectionOperation<T> operation) 
            throws SQLException {
        Connection conn = null;
        boolean success = false;
        
        try {
            conn = createConnection(config);
            T result = operation.execute(conn);
            conn.commit(); // Explicitly commit on success
            success = true;
            logger.debug("Operation completed successfully, connection committed");
            return result;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.debug("Transaction rolled back due to error");
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    logger.debug("Connection closed");
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }
    
    /**
     * Executes a read-only operation with automatic connection management.
     * This method uses read-only mode and auto-commit for better performance.
     * 
     * @param config the database configuration
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws SQLException if the operation fails
     */
    public static <T> T executeReadOnly(DatabaseConfig config, ConnectionOperation<T> operation) 
            throws SQLException {
        Connection conn = null;
        
        try {
            conn = createConnection(config);
            conn.setAutoCommit(true); // Use auto-commit for read-only
            conn.setReadOnly(true);
            
            T result = operation.execute(conn);
            logger.debug("Read-only operation completed successfully");
            return result;
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    logger.debug("Read-only connection closed");
                } catch (SQLException e) {
                    logger.error("Error closing read-only connection", e);
                }
            }
        }
    }
    
    /**
     * Tests if a database connection can be established with the given configuration.
     * 
     * @param jdbcUrl the JDBC URL to test
     * @param username the database username
     * @param password the database password
     * @return true if connection successful, false otherwise
     */
    public static boolean testConnection(String jdbcUrl, String username, String password) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, username, password);
            return conn.isValid(CONNECTION_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            logger.debug("Connection test failed: {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore close errors in test
                }
            }
        }
    }
    
    /**
     * Checks if a Derby database is locked by another process.
     * 
     * @param databasePath the path to the database to check
     * @return true if the database is locked, false if available
     */
    public static boolean isDerbyDatabaseLockedByAnotherProcess(String databasePath) {
        Connection testConnection = null;
        try {
            String jdbcUrl = "jdbc:derby:" + databasePath;
            testConnection = DriverManager.getConnection(jdbcUrl);
            return false; // Successfully connected, not locked
        } catch (SQLException e) {
            // Check for Derby lock exception
            String sqlState = e.getSQLState();
            if ("XJ040".equals(sqlState) || "XSDB6".equals(sqlState)) {
                logger.debug("Database is locked by another process: {}", databasePath);
                return true;
            }
            // Other errors mean database doesn't exist or other issues
            logger.debug("Database connection test failed for other reasons: {}", e.getMessage());
            return false;
        } finally {
            if (testConnection != null) {
                try {
                    testConnection.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    /**
     * Tests if a database profile's configured database is currently available.
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
     * Provides user-friendly explanation for database connection failures.
     * 
     * @param e the SQLException that occurred
     * @param databasePath the database path that failed
     * @return human-readable explanation of the error
     */
    public static String explainConnectionFailure(SQLException e, String databasePath) {
        if (e == null) {
            return "Unknown database error occurred";
        }
        
        String sqlState = e.getSQLState();
        String message = e.getMessage();
        
        // Derby-specific error codes
        if ("XJ040".equals(sqlState) || "XSDB6".equals(sqlState)) {
            return "Database is locked by another instance of MP3Org. Please close other instances.";
        } else if ("XJ004".equals(sqlState)) {
            return "Database not found at: " + databasePath;
        } else if ("08001".equals(sqlState)) {
            return "Unable to connect to database. Check if the path exists: " + databasePath;
        }
        
        // Generic message
        return "Database error: " + message;
    }
    
    /**
     * Functional interface for database operations.
     */
    @FunctionalInterface
    public interface ConnectionOperation<T> {
        T execute(Connection conn) throws SQLException;
    }
}