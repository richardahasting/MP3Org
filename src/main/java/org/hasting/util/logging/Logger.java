package org.hasting.util.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main logging class that provides the API for application logging.
 * 
 * <p>This logger provides a simple, efficient logging interface with support for:</p>
 * <ul>
 *   <li>Five log levels: DEBUG, INFO, WARNING, ERROR, CRITICAL</li>
 *   <li>Parameterized message formatting (SLF4J-style {} placeholders)</li>
 *   <li>Multiple output handlers (console, file, custom)</li>
 *   <li>Thread-safe operation</li>
 *   <li>Efficient level checking to avoid unnecessary work</li>
 * </ul>
 * 
 * <p>Usage examples:</p>
 * <pre>
 * Logger logger = LoggerFactory.getLogger(MyClass.class);
 * 
 * logger.info("Starting operation");
 * logger.debug("Processing file: {}", fileName);
 * logger.error("Failed to process {}: {}", fileName, exception.getMessage(), exception);
 * </pre>
 * 
 * @since 1.0
 */
public class Logger {
    
    private final String name;
    private volatile LogLevel level;
    private final List<LogHandler> handlers;
    
    /**
     * Creates a new logger with the specified name.
     * 
     * @param name The logger name (typically the class name)
     */
    Logger(String name) {
        this.name = name;
        this.level = LogLevel.INFO; // Default level
        this.handlers = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Gets the name of this logger.
     * 
     * @return The logger name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the current minimum log level for this logger.
     * 
     * @return The current log level
     */
    public LogLevel getLevel() {
        return level;
    }
    
    /**
     * Sets the minimum log level for this logger.
     * 
     * @param level The new log level
     */
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    
    /**
     * Adds a log handler to this logger.
     * 
     * @param handler The handler to add
     */
    public void addHandler(LogHandler handler) {
        if (handler != null) {
            handlers.add(handler);
        }
    }
    
    /**
     * Removes a log handler from this logger.
     * 
     * @param handler The handler to remove
     */
    public void removeHandler(LogHandler handler) {
        handlers.remove(handler);
    }
    
    /**
     * Gets all handlers associated with this logger.
     * 
     * @return A copy of the handlers list
     */
    public List<LogHandler> getHandlers() {
        return List.copyOf(handlers);
    }
    
    /**
     * Checks if a given log level would be logged by this logger.
     * 
     * @param level The level to check
     * @return true if the level would be logged, false otherwise
     */
    public boolean isLoggable(LogLevel level) {
        return level.shouldLog(this.level);
    }
    
    // DEBUG level methods
    
    /**
     * Logs a DEBUG level message.
     * 
     * @param message The log message
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    /**
     * Logs a DEBUG level message with parameters.
     * 
     * @param message The log message template with {} placeholders
     * @param params Parameters to substitute into the message
     */
    public void debug(String message, Object... params) {
        log(LogLevel.DEBUG, message, params);
    }
    
    /**
     * Logs a DEBUG level message with an exception.
     * 
     * @param message The log message
     * @param exception The exception to log
     */
    public void debug(String message, Throwable exception) {
        log(LogLevel.DEBUG, message, exception);
    }
    
    // INFO level methods
    
    /**
     * Logs an INFO level message.
     * 
     * @param message The log message
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * Logs an INFO level message with parameters.
     * 
     * @param message The log message template with {} placeholders
     * @param params Parameters to substitute into the message
     */
    public void info(String message, Object... params) {
        log(LogLevel.INFO, message, params);
    }
    
    /**
     * Logs an INFO level message with an exception.
     * 
     * @param message The log message
     * @param exception The exception to log
     */
    public void info(String message, Throwable exception) {
        log(LogLevel.INFO, message, exception);
    }
    
    // WARNING level methods
    
    /**
     * Logs a WARNING level message.
     * 
     * @param message The log message
     */
    public void warning(String message) {
        log(LogLevel.WARNING, message);
    }
    
    /**
     * Logs a WARNING level message with parameters.
     * 
     * @param message The log message template with {} placeholders
     * @param params Parameters to substitute into the message
     */
    public void warning(String message, Object... params) {
        log(LogLevel.WARNING, message, params);
    }
    
    /**
     * Logs a WARNING level message with an exception.
     * 
     * @param message The log message
     * @param exception The exception to log
     */
    public void warning(String message, Throwable exception) {
        log(LogLevel.WARNING, message, exception);
    }
    
    // ERROR level methods
    
    /**
     * Logs an ERROR level message.
     * 
     * @param message The log message
     */
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    /**
     * Logs an ERROR level message with parameters.
     * 
     * @param message The log message template with {} placeholders
     * @param params Parameters to substitute into the message
     */
    public void error(String message, Object... params) {
        log(LogLevel.ERROR, message, params);
    }
    
    /**
     * Logs an ERROR level message with an exception.
     * 
     * @param message The log message
     * @param exception The exception to log
     */
    public void error(String message, Throwable exception) {
        log(LogLevel.ERROR, message, exception);
    }
    
    // CRITICAL level methods
    
    /**
     * Logs a CRITICAL level message.
     * 
     * @param message The log message
     */
    public void critical(String message) {
        log(LogLevel.CRITICAL, message);
    }
    
    /**
     * Logs a CRITICAL level message with parameters.
     * 
     * @param message The log message template with {} placeholders
     * @param params Parameters to substitute into the message
     */
    public void critical(String message, Object... params) {
        log(LogLevel.CRITICAL, message, params);
    }
    
    /**
     * Logs a CRITICAL level message with an exception.
     * 
     * @param message The log message
     * @param exception The exception to log
     */
    public void critical(String message, Throwable exception) {
        log(LogLevel.CRITICAL, message, exception);
    }
    
    // Core logging methods
    
    /**
     * Logs a message at the specified level.
     * 
     * @param level The log level
     * @param message The log message
     * @param params Optional parameters for message formatting
     */
    public void log(LogLevel level, String message, Object... params) {
        if (!isLoggable(level)) {
            return;
        }
        
        LogRecord record = new LogRecord(level, name, message, params);
        publishRecord(record);
    }
    
    /**
     * Logs a message with an exception at the specified level.
     * 
     * @param level The log level
     * @param message The log message
     * @param exception The exception to log
     * @param params Optional parameters for message formatting
     */
    public void log(LogLevel level, String message, Throwable exception, Object... params) {
        if (!isLoggable(level)) {
            return;
        }
        
        LogRecord record = new LogRecord(level, name, message, exception, params);
        publishRecord(record);
    }
    
    /**
     * Publishes a log record to all registered handlers.
     * 
     * @param record The log record to publish
     */
    private void publishRecord(LogRecord record) {
        // Use a copy of handlers to avoid issues with concurrent modification
        for (LogHandler handler : handlers) {
            try {
                if (handler.isEnabled()) {
                    handler.handle(record);
                }
            } catch (Exception e) {
                // Don't let handler errors break logging
                System.err.println("Error in log handler " + handler.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Flushes all handlers associated with this logger.
     */
    public void flush() {
        for (LogHandler handler : handlers) {
            try {
                handler.flush();
            } catch (Exception e) {
                // Ignore flush errors
            }
        }
    }
    
    /**
     * Closes all handlers associated with this logger.
     * After calling this method, the logger should not be used.
     */
    public void close() {
        for (LogHandler handler : handlers) {
            try {
                handler.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
        handlers.clear();
    }
}