package org.hasting.util.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single log record containing all information about a log message.
 * 
 * <p>This class is immutable and thread-safe. It contains:</p>
 * <ul>
 *   <li>Log level and logger name</li>
 *   <li>Message content and optional parameters</li>
 *   <li>Timestamp when the log was created</li>
 *   <li>Thread information</li>
 *   <li>Optional exception information</li>
 * </ul>
 * 
 * @since 1.0
 */
public final class LogRecord {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                         .withZone(ZoneId.systemDefault());
    
    private final LogLevel level;
    private final String loggerName;
    private final String message;
    private final Object[] parameters;
    private final Instant timestamp;
    private final String threadName;
    private final Throwable exception;
    
    /**
     * Creates a new log record.
     * 
     * @param level The log level
     * @param loggerName The name of the logger that created this record
     * @param message The log message (may contain {} placeholders)
     * @param parameters Optional parameters for message formatting
     */
    public LogRecord(LogLevel level, String loggerName, String message, Object... parameters) {
        this(level, loggerName, message, null, parameters);
    }
    
    /**
     * Creates a new log record with an exception.
     * 
     * @param level The log level
     * @param loggerName The name of the logger that created this record
     * @param message The log message (may contain {} placeholders)
     * @param exception The exception associated with this log record (may be null)
     * @param parameters Optional parameters for message formatting
     */
    public LogRecord(LogLevel level, String loggerName, String message, Throwable exception, Object... parameters) {
        this.level = level;
        this.loggerName = loggerName;
        this.message = message;
        this.exception = exception;
        this.parameters = parameters != null ? parameters.clone() : new Object[0];
        this.timestamp = Instant.now();
        this.threadName = Thread.currentThread().getName();
    }
    
    /**
     * Gets the log level for this record.
     * 
     * @return The log level
     */
    public LogLevel getLevel() {
        return level;
    }
    
    /**
     * Gets the name of the logger that created this record.
     * 
     * @return The logger name
     */
    public String getLoggerName() {
        return loggerName;
    }
    
    /**
     * Gets the raw message template for this record.
     * 
     * @return The message template (may contain {} placeholders)
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the formatted message with parameters substituted.
     * 
     * @return The formatted message
     */
    public String getFormattedMessage() {
        if (parameters.length == 0) {
            return message;
        }
        
        return formatMessage(message, parameters);
    }
    
    /**
     * Gets the parameters for message formatting.
     * 
     * @return A copy of the parameters array
     */
    public Object[] getParameters() {
        return parameters.clone();
    }
    
    /**
     * Gets the timestamp when this record was created.
     * 
     * @return The timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the formatted timestamp as a string.
     * 
     * @return The formatted timestamp
     */
    public String getFormattedTimestamp() {
        return TIMESTAMP_FORMATTER.format(timestamp);
    }
    
    /**
     * Gets the name of the thread that created this record.
     * 
     * @return The thread name
     */
    public String getThreadName() {
        return threadName;
    }
    
    /**
     * Gets the exception associated with this record, if any.
     * 
     * @return The exception, or null if none
     */
    public Throwable getException() {
        return exception;
    }
    
    /**
     * Checks if this record has an associated exception.
     * 
     * @return true if there is an exception, false otherwise
     */
    public boolean hasException() {
        return exception != null;
    }
    
    /**
     * Simple message formatting that replaces {} placeholders with parameter values.
     * This provides SLF4J-style parameter substitution without the dependency.
     * 
     * @param template The message template with {} placeholders
     * @param params The parameters to substitute
     * @return The formatted message
     */
    private static String formatMessage(String template, Object[] params) {
        if (template == null || params == null || params.length == 0) {
            return template;
        }
        
        StringBuilder result = new StringBuilder();
        int paramIndex = 0;
        int start = 0;
        int pos;
        
        while ((pos = template.indexOf("{}", start)) != -1 && paramIndex < params.length) {
            result.append(template, start, pos);
            
            Object param = params[paramIndex++];
            if (param == null) {
                result.append("null");
            } else {
                result.append(param.toString());
            }
            
            start = pos + 2; // Skip "{}"
        }
        
        // Append remaining template
        result.append(template.substring(start));
        
        return result.toString();
    }
    
    @Override
    public String toString() {
        return String.format("%s [%s] %s - %s", 
            getFormattedTimestamp(), 
            level.getDisplayName(), 
            loggerName, 
            getFormattedMessage());
    }
}