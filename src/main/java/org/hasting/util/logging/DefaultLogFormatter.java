package org.hasting.util.logging;

/**
 * Default log formatter that provides a standard, readable format for log messages.
 * 
 * <p>The default format is:</p>
 * <pre>
 * [TIMESTAMP] [LEVEL] [THREAD] LOGGER_NAME - MESSAGE
 * </pre>
 * 
 * <p>For example:</p>
 * <pre>
 * [2024-06-30 14:30:15.123] [INFO] [main] org.hasting.util.MusicFileScanner - Starting directory scan: /music
 * </pre>
 * 
 * @since 1.0
 */
public class DefaultLogFormatter implements LogFormatter {
    
    private final boolean includeThread;
    private final boolean includeLogger;
    
    /**
     * Creates a default formatter with all fields included.
     */
    public DefaultLogFormatter() {
        this(true, true);
    }
    
    /**
     * Creates a default formatter with optional field inclusion.
     * 
     * @param includeThread Whether to include thread name in output
     * @param includeLogger Whether to include logger name in output
     */
    public DefaultLogFormatter(boolean includeThread, boolean includeLogger) {
        this.includeThread = includeThread;
        this.includeLogger = includeLogger;
    }
    
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        
        // Timestamp
        sb.append("[").append(record.getFormattedTimestamp()).append("]");
        
        // Log level
        sb.append(" [").append(record.getLevel().getDisplayName()).append("]");
        
        // Thread name (optional)
        if (includeThread) {
            sb.append(" [").append(record.getThreadName()).append("]");
        }
        
        // Logger name (optional)
        if (includeLogger) {
            sb.append(" ").append(record.getLoggerName());
        }
        
        // Message
        sb.append(" - ").append(record.getFormattedMessage());
        
        return sb.toString();
    }
}

/**
 * Simple log formatter that outputs only the essential information.
 * 
 * <p>The simple format is:</p>
 * <pre>
 * LEVEL: MESSAGE
 * </pre>
 */
class SimpleLogFormatter implements LogFormatter {
    
    @Override
    public String format(LogRecord record) {
        return record.getLevel().getDisplayName() + ": " + record.getFormattedMessage();
    }
}