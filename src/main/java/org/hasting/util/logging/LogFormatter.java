package org.hasting.util.logging;

/**
 * Interface for formatting log records into string representations.
 * 
 * <p>Implementations of this interface are responsible for converting
 * LogRecord objects into formatted string output suitable for display
 * or storage. Different formatters can provide different styles:</p>
 * <ul>
 *   <li>Simple text formatting for console output</li>
 *   <li>JSON formatting for structured logging</li>
 *   <li>XML formatting for system integration</li>
 *   <li>Custom application-specific formats</li>
 * </ul>
 * 
 * @since 1.0
 */
public interface LogFormatter {
    
    /**
     * Formats a log record into a string representation.
     * 
     * @param record The log record to format
     * @return The formatted string representation
     */
    String format(LogRecord record);
}