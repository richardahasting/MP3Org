package org.hasting.util.logging;

/**
 * Enumeration of logging levels for the MP3Org custom logging framework.
 * 
 * <p>Levels are ordered from most verbose (DEBUG) to least verbose (CRITICAL).
 * Each level includes all higher-priority levels when enabled.</p>
 * 
 * <p><strong>Level Hierarchy:</strong></p>
 * <ul>
 *   <li><strong>DEBUG</strong> - Detailed diagnostic information, typically only enabled during development</li>
 *   <li><strong>INFO</strong> - General informational messages about normal operation</li>
 *   <li><strong>WARNING</strong> - Warning messages for potentially problematic situations</li>
 *   <li><strong>ERROR</strong> - Error messages for serious problems that don't halt execution</li>
 *   <li><strong>CRITICAL</strong> - Critical error messages for severe problems that may halt execution</li>
 * </ul>
 * 
 * @since 1.0
 */
public enum LogLevel {
    /**
     * Debug level - Most verbose logging for development and troubleshooting.
     * Includes detailed diagnostic information, method entry/exit, variable values.
     */
    DEBUG(0, "DEBUG"),
    
    /**
     * Info level - General informational messages about normal application operation.
     * Includes startup messages, configuration changes, normal workflow progress.
     */
    INFO(1, "INFO"),
    
    /**
     * Warning level - Potentially problematic situations that don't prevent operation.
     * Includes deprecated usage, fallback behavior, recoverable errors.
     */
    WARNING(2, "WARNING"),
    
    /**
     * Error level - Serious problems that affect functionality but don't halt execution.
     * Includes failed operations, invalid data, resource access problems.
     */
    ERROR(3, "ERROR"),
    
    /**
     * Critical level - Severe problems that may cause application termination.
     * Includes unrecoverable errors, system failures, critical resource problems.
     */
    CRITICAL(4, "CRITICAL");
    
    private final int priority;
    private final String displayName;
    
    LogLevel(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }
    
    /**
     * Gets the numeric priority of this log level.
     * Higher numbers indicate more severe/important messages.
     * 
     * @return The priority value (0=DEBUG, 4=CRITICAL)
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Gets the display name for this log level.
     * 
     * @return The human-readable level name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Checks if this level should be logged when the minimum level is set.
     * 
     * @param minimumLevel The minimum level that should be logged
     * @return true if this level should be logged, false otherwise
     */
    public boolean shouldLog(LogLevel minimumLevel) {
        return this.priority >= minimumLevel.priority;
    }
    
    /**
     * Parses a string to a LogLevel, case-insensitive.
     * 
     * @param levelName The level name to parse
     * @return The corresponding LogLevel
     * @throws IllegalArgumentException if the level name is not recognized
     */
    public static LogLevel fromString(String levelName) {
        if (levelName == null || levelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Log level name cannot be null or empty");
        }
        
        String upperName = levelName.trim().toUpperCase();
        try {
            return LogLevel.valueOf(upperName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown log level: " + levelName + 
                ". Valid levels are: DEBUG, INFO, WARNING, ERROR, CRITICAL");
        }
    }
    
    /**
     * Gets all available log levels as a string array for UI components.
     * 
     * @return Array of level display names
     */
    public static String[] getDisplayNames() {
        LogLevel[] levels = LogLevel.values();
        String[] names = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            names[i] = levels[i].getDisplayName();
        }
        return names;
    }
}