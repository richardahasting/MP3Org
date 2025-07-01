package org.hasting.util.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory class for creating and managing Logger instances.
 * 
 * <p>This class provides a centralized way to create and configure loggers
 * for the MP3Org application. It ensures that:</p>
 * <ul>
 *   <li>Only one Logger instance exists per name (singleton pattern)</li>
 *   <li>Global configuration can be applied to all loggers</li>
 *   <li>Default handlers are automatically added to new loggers</li>
 *   <li>Loggers can be efficiently retrieved and managed</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>
 * public class MyClass {
 *     private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
 *     
 *     public void doSomething() {
 *         logger.info("Doing something...");
 *     }
 * }
 * </pre>
 * 
 * @since 1.0
 */
public class LoggerFactory {
    
    private static final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private static volatile LoggingConfiguration globalConfig;
    private static volatile boolean initialized = false;
    
    // Prevent instantiation
    private LoggerFactory() {}
    
    /**
     * Gets a logger for the specified class.
     * 
     * @param clazz The class requesting the logger
     * @return A logger instance for the class
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }
    
    /**
     * Gets a logger for the specified name.
     * 
     * @param name The logger name
     * @return A logger instance for the name
     */
    public static Logger getLogger(String name) {
        ensureInitialized();
        
        return loggers.computeIfAbsent(name, loggerName -> {
            Logger logger = new Logger(loggerName);
            configureNewLogger(logger);
            return logger;
        });
    }
    
    /**
     * Configures global logging settings that apply to all loggers.
     * 
     * @param config The global logging configuration
     */
    public static void configure(LoggingConfiguration config) {
        globalConfig = config;
        initialized = true;
        
        // Apply configuration to existing loggers
        for (Logger logger : loggers.values()) {
            applyConfiguration(logger, config);
        }
    }
    
    /**
     * Gets the current global logging configuration.
     * 
     * @return The global configuration, or null if not set
     */
    public static LoggingConfiguration getGlobalConfiguration() {
        return globalConfig;
    }
    
    /**
     * Ensures the logging system is initialized with default configuration if needed.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (LoggerFactory.class) {
                if (!initialized) {
                    // Initialize with default configuration
                    configure(LoggingConfiguration.createDefault());
                }
            }
        }
    }
    
    /**
     * Configures a newly created logger with global settings.
     * 
     * @param logger The logger to configure
     */
    private static void configureNewLogger(Logger logger) {
        if (globalConfig != null) {
            applyConfiguration(logger, globalConfig);
        }
    }
    
    /**
     * Applies a configuration to a specific logger.
     * 
     * @param logger The logger to configure
     * @param config The configuration to apply
     */
    private static void applyConfiguration(Logger logger, LoggingConfiguration config) {
        // Set log level
        logger.setLevel(config.getDefaultLevel());
        
        // Clear existing handlers
        for (LogHandler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        
        // Add configured handlers
        for (LogHandler handler : config.getHandlers()) {
            logger.addHandler(handler);
        }
        
        // Apply logger-specific overrides
        LogLevel specificLevel = config.getLoggerLevel(logger.getName());
        if (specificLevel != null) {
            logger.setLevel(specificLevel);
        }
    }
    
    /**
     * Gets all currently active loggers.
     * 
     * @return A copy of all active loggers
     */
    public static java.util.Collection<Logger> getAllLoggers() {
        return java.util.Collections.unmodifiableCollection(loggers.values());
    }
    
    /**
     * Flushes all loggers and their handlers.
     * This should be called before application shutdown.
     */
    public static void flushAll() {
        for (Logger logger : loggers.values()) {
            try {
                logger.flush();
            } catch (Exception e) {
                // Ignore errors during shutdown
            }
        }
    }
    
    /**
     * Closes all loggers and their handlers.
     * After calling this method, the logging system should be reinitialized before use.
     */
    public static void shutdown() {
        for (Logger logger : loggers.values()) {
            try {
                logger.close();
            } catch (Exception e) {
                // Ignore errors during shutdown
            }
        }
        loggers.clear();
        globalConfig = null;
        initialized = false;
    }
    
    /**
     * Reloads the configuration for all loggers.
     * This can be used to update logging settings at runtime.
     */
    public static void reloadConfiguration() {
        if (globalConfig != null) {
            for (Logger logger : loggers.values()) {
                applyConfiguration(logger, globalConfig);
            }
        }
    }
    
    /**
     * Sets the log level for a specific logger by name.
     * 
     * @param loggerName The name of the logger
     * @param level The new log level
     */
    public static void setLoggerLevel(String loggerName, LogLevel level) {
        Logger logger = loggers.get(loggerName);
        if (logger != null) {
            logger.setLevel(level);
        }
        
        // Also update global config if it exists
        if (globalConfig != null) {
            globalConfig.setLoggerLevel(loggerName, level);
        }
    }
    
    /**
     * Gets the current log level for a specific logger by name.
     * 
     * @param loggerName The name of the logger
     * @return The current log level, or null if the logger doesn't exist
     */
    public static LogLevel getLoggerLevel(String loggerName) {
        Logger logger = loggers.get(loggerName);
        return logger != null ? logger.getLevel() : null;
    }
}