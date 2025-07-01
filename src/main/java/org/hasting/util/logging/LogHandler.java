package org.hasting.util.logging;

/**
 * Interface for handling log records by outputting them to various destinations.
 * 
 * <p>Implementations of this interface are responsible for:</p>
 * <ul>
 *   <li>Formatting log records for output</li>
 *   <li>Writing formatted logs to their destination (console, file, etc.)</li>
 *   <li>Managing resources (file handles, connections, etc.)</li>
 *   <li>Handling errors during log output</li>
 * </ul>
 * 
 * <p>Handlers should be thread-safe as they may be called from multiple threads
 * simultaneously.</p>
 * 
 * @since 1.0
 */
public interface LogHandler {
    
    /**
     * Handles a log record by outputting it to this handler's destination.
     * 
     * <p>This method should be thread-safe and should not throw exceptions
     * under normal circumstances. If an error occurs during output, it should
     * be handled gracefully (e.g., by writing to stderr or suppressing the error).</p>
     * 
     * @param record The log record to handle
     */
    void handle(LogRecord record);
    
    /**
     * Flushes any buffered output to ensure all log messages are written.
     * 
     * <p>This method should be called periodically and before application
     * shutdown to ensure no log messages are lost.</p>
     */
    void flush();
    
    /**
     * Closes this handler and releases any resources it holds.
     * 
     * <p>After calling this method, the handler should not be used again.
     * This method should be idempotent (safe to call multiple times).</p>
     */
    void close();
    
    /**
     * Gets the name of this handler for identification purposes.
     * 
     * @return A descriptive name for this handler
     */
    String getName();
    
    /**
     * Checks if this handler is currently enabled and functional.
     * 
     * @return true if the handler can process log records, false otherwise
     */
    boolean isEnabled();
}