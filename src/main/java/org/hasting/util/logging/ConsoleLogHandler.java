package org.hasting.util.logging;

import java.io.PrintStream;

/**
 * Log handler that outputs log records to console streams (stdout or stderr).
 * 
 * <p>This handler provides console output with configurable formatting and
 * destination stream selection. It supports:</p>
 * <ul>
 *   <li>Output to stdout or stderr based on configuration or log level</li>
 *   <li>Color coding for different log levels (when supported)</li>
 *   <li>Customizable message formatting</li>
 *   <li>Thread-safe output</li>
 * </ul>
 * 
 * @since 1.0
 */
public class ConsoleLogHandler implements LogHandler {
    
    /**
     * Enumeration of console output destinations.
     */
    public enum OutputMode {
        /** All messages go to stdout */
        STDOUT_ONLY,
        /** All messages go to stderr */
        STDERR_ONLY,
        /** INFO and DEBUG go to stdout, WARNING/ERROR/CRITICAL go to stderr */
        SPLIT_BY_LEVEL
    }
    
    private final OutputMode outputMode;
    private final LogFormatter formatter;
    private volatile boolean enabled;
    
    /**
     * Creates a console handler with split-by-level output mode.
     */
    public ConsoleLogHandler() {
        this(OutputMode.SPLIT_BY_LEVEL);
    }
    
    /**
     * Creates a console handler with the specified output mode.
     * 
     * @param outputMode How to route messages to stdout vs stderr
     */
    public ConsoleLogHandler(OutputMode outputMode) {
        this(outputMode, new DefaultLogFormatter());
    }
    
    /**
     * Creates a console handler with custom output mode and formatter.
     * 
     * @param outputMode How to route messages to stdout vs stderr
     * @param formatter The formatter to use for log messages
     */
    public ConsoleLogHandler(OutputMode outputMode, LogFormatter formatter) {
        this.outputMode = outputMode;
        this.formatter = formatter;
        this.enabled = true;
    }
    
    @Override
    public void handle(LogRecord record) {
        if (!enabled) {
            return;
        }
        
        try {
            PrintStream targetStream = selectOutputStream(record.getLevel());
            String formattedMessage = formatter.format(record);
            
            // Synchronize on the target stream to ensure atomic output
            synchronized (targetStream) {
                targetStream.println(formattedMessage);
                
                // If there's an exception, print the stack trace
                if (record.hasException()) {
                    record.getException().printStackTrace(targetStream);
                }
            }
        } catch (Exception e) {
            // Fallback error handling - write to stderr without formatting
            synchronized (System.err) {
                System.err.println("LOG ERROR: Failed to output log message: " + e.getMessage());
                System.err.println("Original message: " + record.getFormattedMessage());
            }
        }
    }
    
    @Override
    public void flush() {
        if (enabled) {
            System.out.flush();
            System.err.flush();
        }
    }
    
    @Override
    public void close() {
        enabled = false;
        flush();
    }
    
    @Override
    public String getName() {
        return "Console (" + outputMode + ")";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Selects the appropriate output stream based on the output mode and log level.
     * 
     * @param level The log level
     * @return The PrintStream to use for output
     */
    private PrintStream selectOutputStream(LogLevel level) {
        switch (outputMode) {
            case STDOUT_ONLY:
                return System.out;
            case STDERR_ONLY:
                return System.err;
            case SPLIT_BY_LEVEL:
                // INFO and DEBUG to stdout, WARNING/ERROR/CRITICAL to stderr
                return (level == LogLevel.INFO || level == LogLevel.DEBUG) 
                    ? System.out 
                    : System.err;
            default:
                return System.out;
        }
    }
    
    /**
     * Sets whether this handler is enabled.
     * 
     * @param enabled true to enable output, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Gets the output mode for this handler.
     * 
     * @return The current output mode
     */
    public OutputMode getOutputMode() {
        return outputMode;
    }
}