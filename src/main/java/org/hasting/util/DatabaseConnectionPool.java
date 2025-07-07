package org.hasting.util;

import org.hasting.util.logging.MP3OrgLoggingManager;
import org.hasting.util.logging.Logger;

import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple connection pool implementation for managing database connections.
 * This ensures connections are properly closed and reused efficiently.
 * 
 * <p>Features:
 * <ul>
 *   <li>Connection validation before use</li>
 *   <li>Automatic connection recovery</li>
 *   <li>Configurable pool size</li>
 *   <li>Connection timeout handling</li>
 * </ul>
 */
public class DatabaseConnectionPool {
    private static final Logger logger = MP3OrgLoggingManager.getLogger(DatabaseConnectionPool.class);
    
    private static final int MIN_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 5;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int VALIDATION_TIMEOUT_SECONDS = 3;
    
    private final BlockingQueue<Connection> connectionPool;
    private final DatabaseConfig config;
    private volatile boolean isShutdown = false;
    
    /**
     * Creates a new connection pool with the specified configuration.
     */
    public DatabaseConnectionPool(DatabaseConfig config) {
        this.config = config;
        this.connectionPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
        initializePool();
    }
    
    /**
     * Initializes the connection pool with minimum number of connections.
     */
    private void initializePool() {
        for (int i = 0; i < MIN_POOL_SIZE; i++) {
            try {
                Connection conn = createNewConnection();
                connectionPool.offer(conn);
            } catch (SQLException e) {
                logger.error("Failed to create initial connection", e);
            }
        }
        logger.info("Connection pool initialized with {} connections", connectionPool.size());
    }
    
    /**
     * Creates a new database connection.
     */
    private Connection createNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(
            config.getJdbcUrl(),
            config.getUsername(),
            config.getPassword()
        );
        
        // Configure connection for optimal performance
        conn.setAutoCommit(false); // We'll manage commits explicitly
        return conn;
    }
    
    /**
     * Gets a connection from the pool, creating a new one if necessary.
     * 
     * @return a valid database connection
     * @throws SQLException if unable to obtain a valid connection
     */
    public Connection getConnection() throws SQLException {
        if (isShutdown) {
            throw new SQLException("Connection pool is shutdown");
        }
        
        try {
            Connection conn = connectionPool.poll(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (conn == null) {
                // Pool is empty, create new connection if under limit
                if (connectionPool.size() < MAX_POOL_SIZE) {
                    conn = createNewConnection();
                    logger.debug("Created new connection, pool size: {}", connectionPool.size() + 1);
                } else {
                    throw new SQLException("Connection pool exhausted, timeout waiting for connection");
                }
            }
            
            // Validate connection before returning
            if (!isConnectionValid(conn)) {
                logger.debug("Connection invalid, creating new one");
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
                conn = createNewConnection();
            }
            
            return conn;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        }
    }
    
    /**
     * Returns a connection to the pool for reuse.
     * 
     * @param conn the connection to return
     */
    public void returnConnection(Connection conn) {
        if (conn == null || isShutdown) {
            return;
        }
        
        try {
            // Reset connection state
            if (!conn.getAutoCommit()) {
                conn.rollback(); // Rollback any uncommitted changes
                conn.setAutoCommit(false); // Keep auto-commit off
            }
            
            // Only return valid connections to pool
            if (isConnectionValid(conn) && connectionPool.size() < MAX_POOL_SIZE) {
                connectionPool.offer(conn);
            } else {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.debug("Error closing invalid connection", e);
                }
            }
        } catch (SQLException e) {
            logger.debug("Error resetting connection", e);
            try {
                conn.close();
            } catch (SQLException ex) {
                // Ignore close errors
            }
        }
    }
    
    /**
     * Validates if a connection is still usable.
     */
    private boolean isConnectionValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Executes a database operation with automatic connection management.
     * This ensures connections are always properly returned to the pool.
     * 
     * @param operation the database operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws SQLException if the operation fails
     */
    public <T> T executeWithConnection(ConnectionOperation<T> operation) throws SQLException {
        Connection conn = null;
        boolean success = false;
        
        try {
            conn = getConnection();
            T result = operation.execute(conn);
            conn.commit(); // Commit on success
            success = true;
            return result;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Failed to rollback transaction", ex);
                }
            }
            throw e;
            
        } finally {
            if (conn != null) {
                if (success) {
                    returnConnection(conn);
                } else {
                    // Don't return failed connections to pool
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        logger.debug("Error closing failed connection", e);
                    }
                }
            }
        }
    }
    
    /**
     * Shuts down the connection pool and closes all connections.
     */
    public synchronized void shutdown() {
        isShutdown = true;
        
        // Close all connections
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.debug("Error closing connection during shutdown", e);
            }
        }
        
        logger.info("Connection pool shutdown complete");
    }
    
    /**
     * Functional interface for database operations.
     */
    @FunctionalInterface
    public interface ConnectionOperation<T> {
        T execute(Connection conn) throws SQLException;
    }
}