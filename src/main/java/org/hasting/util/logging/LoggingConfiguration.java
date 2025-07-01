package org.hasting.util.logging;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration class for the MP3Org logging framework.
 * 
 * <p>This class manages all logging configuration including:</p>
 * <ul>
 *   <li>Default and logger-specific log levels</li>
 *   <li>Output handler configuration (console, file)</li>
 *   <li>File logging settings (path, rotation, etc.)</li>
 *   <li>Formatting preferences</li>
 *   <li>Runtime configuration changes</li>
 * </ul>
 * 
 * @since 1.0
 */
public class LoggingConfiguration {
    
    // Configuration keys
    private static final String PROP_DEFAULT_LEVEL = "mp3org.logging.level";
    private static final String PROP_CONSOLE_ENABLED = "mp3org.logging.console.enabled";
    private static final String PROP_CONSOLE_MODE = "mp3org.logging.console.mode";
    private static final String PROP_FILE_ENABLED = "mp3org.logging.file.enabled";
    private static final String PROP_FILE_PATH = "mp3org.logging.file.path";
    private static final String PROP_FILE_MAX_SIZE = "mp3org.logging.file.maxSize";
    private static final String PROP_FILE_MAX_FILES = "mp3org.logging.file.maxFiles";
    private static final String PROP_LOGGER_PREFIX = "mp3org.logging.logger.";
    
    private LogLevel defaultLevel;
    private boolean consoleEnabled;
    private ConsoleLogHandler.OutputMode consoleMode;
    private boolean fileEnabled;
    private String filePath;
    private long fileMaxSize;
    private int fileMaxFiles;
    private final Map<String, LogLevel> loggerLevels;
    private final List<LogHandler> handlers;
    
    /**
     * Creates a new logging configuration with default settings.
     */
    public LoggingConfiguration() {
        this.defaultLevel = LogLevel.INFO;
        this.consoleEnabled = true;
        this.consoleMode = ConsoleLogHandler.OutputMode.SPLIT_BY_LEVEL;
        this.fileEnabled = false;
        this.filePath = "mp3org/logs/mp3org.log";
        this.fileMaxSize = 10 * 1024 * 1024; // 10MB
        this.fileMaxFiles = 5;
        this.loggerLevels = new ConcurrentHashMap<>();
        this.handlers = new ArrayList<>();
        
        initializeHandlers();
    }
    
    /**
     * Creates a default configuration suitable for most use cases.
     * 
     * @return A default logging configuration
     */
    public static LoggingConfiguration createDefault() {
        LoggingConfiguration config = new LoggingConfiguration();
        config.setConsoleEnabled(true);
        config.setFileEnabled(false); // Start with console only
        config.setDefaultLevel(LogLevel.INFO);
        return config;
    }
    
    /**
     * Creates a configuration for development with verbose logging.
     * 
     * @return A development-oriented logging configuration
     */
    public static LoggingConfiguration createDevelopment() {
        LoggingConfiguration config = new LoggingConfiguration();
        config.setConsoleEnabled(true);
        config.setFileEnabled(true);
        config.setDefaultLevel(LogLevel.DEBUG);
        
        // Enable debug logging for MP3Org packages
        config.setLoggerLevel("org.hasting", LogLevel.DEBUG);
        
        return config;
    }
    
    /**
     * Loads configuration from a Properties object.
     * 
     * @param props The properties to load from
     * @return A new logging configuration
     */
    public static LoggingConfiguration fromProperties(Properties props) {
        LoggingConfiguration config = new LoggingConfiguration();
        
        // Load basic settings
        String levelStr = props.getProperty(PROP_DEFAULT_LEVEL, "INFO");
        try {
            config.setDefaultLevel(LogLevel.fromString(levelStr));
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid log level in configuration: " + levelStr);
            config.setDefaultLevel(LogLevel.INFO);
        }
        
        config.setConsoleEnabled(Boolean.parseBoolean(props.getProperty(PROP_CONSOLE_ENABLED, "true")));
        config.setFileEnabled(Boolean.parseBoolean(props.getProperty(PROP_FILE_ENABLED, "false")));
        config.setFilePath(props.getProperty(PROP_FILE_PATH, "mp3org/logs/mp3org.log"));
        
        // Parse console mode
        String modeStr = props.getProperty(PROP_CONSOLE_MODE, "SPLIT_BY_LEVEL");
        try {
            config.setConsoleMode(ConsoleLogHandler.OutputMode.valueOf(modeStr));
        } catch (IllegalArgumentException e) {
            config.setConsoleMode(ConsoleLogHandler.OutputMode.SPLIT_BY_LEVEL);
        }
        
        // Parse file settings
        try {
            String maxSizeStr = props.getProperty(PROP_FILE_MAX_SIZE, "10485760"); // 10MB
            config.setFileMaxSize(Long.parseLong(maxSizeStr));
        } catch (NumberFormatException e) {
            config.setFileMaxSize(10 * 1024 * 1024);
        }
        
        try {
            String maxFilesStr = props.getProperty(PROP_FILE_MAX_FILES, "5");
            config.setFileMaxFiles(Integer.parseInt(maxFilesStr));
        } catch (NumberFormatException e) {
            config.setFileMaxFiles(5);
        }
        
        // Load logger-specific levels
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(PROP_LOGGER_PREFIX)) {
                String loggerName = key.substring(PROP_LOGGER_PREFIX.length());
                String loggerLevel = props.getProperty(key);
                try {
                    config.setLoggerLevel(loggerName, LogLevel.fromString(loggerLevel));
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid log level for logger " + loggerName + ": " + loggerLevel);
                }
            }
        }
        
        return config;
    }
    
    /**
     * Saves this configuration to a Properties object.
     * 
     * @return A Properties object containing this configuration
     */
    public Properties toProperties() {
        Properties props = new Properties();
        
        props.setProperty(PROP_DEFAULT_LEVEL, defaultLevel.name());
        props.setProperty(PROP_CONSOLE_ENABLED, String.valueOf(consoleEnabled));
        props.setProperty(PROP_CONSOLE_MODE, consoleMode.name());
        props.setProperty(PROP_FILE_ENABLED, String.valueOf(fileEnabled));
        props.setProperty(PROP_FILE_PATH, filePath);
        props.setProperty(PROP_FILE_MAX_SIZE, String.valueOf(fileMaxSize));
        props.setProperty(PROP_FILE_MAX_FILES, String.valueOf(fileMaxFiles));
        
        // Save logger-specific levels
        for (Map.Entry<String, LogLevel> entry : loggerLevels.entrySet()) {
            props.setProperty(PROP_LOGGER_PREFIX + entry.getKey(), entry.getValue().name());
        }
        
        return props;
    }
    
    /**
     * Initializes the log handlers based on current configuration.
     */
    private void initializeHandlers() {
        handlers.clear();
        
        if (consoleEnabled) {
            handlers.add(new ConsoleLogHandler(consoleMode));
        }
        
        if (fileEnabled) {
            try {
                handlers.add(new FileLogHandler(filePath, new DefaultLogFormatter(), fileMaxSize, fileMaxFiles));
            } catch (Exception e) {
                System.err.println("Failed to create file log handler: " + e.getMessage());
            }
        }
    }
    
    // Getters and setters
    
    public LogLevel getDefaultLevel() {
        return defaultLevel;
    }
    
    public void setDefaultLevel(LogLevel defaultLevel) {
        this.defaultLevel = defaultLevel;
    }
    
    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }
    
    public void setConsoleEnabled(boolean consoleEnabled) {
        this.consoleEnabled = consoleEnabled;
        initializeHandlers();
    }
    
    public ConsoleLogHandler.OutputMode getConsoleMode() {
        return consoleMode;
    }
    
    public void setConsoleMode(ConsoleLogHandler.OutputMode consoleMode) {
        this.consoleMode = consoleMode;
        initializeHandlers();
    }
    
    public boolean isFileEnabled() {
        return fileEnabled;
    }
    
    public void setFileEnabled(boolean fileEnabled) {
        this.fileEnabled = fileEnabled;
        initializeHandlers();
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        initializeHandlers();
    }
    
    public long getFileMaxSize() {
        return fileMaxSize;
    }
    
    public void setFileMaxSize(long fileMaxSize) {
        this.fileMaxSize = fileMaxSize;
        initializeHandlers();
    }
    
    public int getFileMaxFiles() {
        return fileMaxFiles;
    }
    
    public void setFileMaxFiles(int fileMaxFiles) {
        this.fileMaxFiles = fileMaxFiles;
        initializeHandlers();
    }
    
    public LogLevel getLoggerLevel(String loggerName) {
        return loggerLevels.get(loggerName);
    }
    
    public void setLoggerLevel(String loggerName, LogLevel level) {
        if (level == null) {
            loggerLevels.remove(loggerName);
        } else {
            loggerLevels.put(loggerName, level);
        }
    }
    
    public Map<String, LogLevel> getAllLoggerLevels() {
        return new HashMap<>(loggerLevels);
    }
    
    public List<LogHandler> getHandlers() {
        return new ArrayList<>(handlers);
    }
    
    /**
     * Saves this configuration to a file.
     * 
     * @param file The file to save to
     * @throws IOException If saving fails
     */
    public void saveToFile(File file) throws IOException {
        Properties props = toProperties();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "MP3Org Logging Configuration");
        }
    }
    
    /**
     * Loads configuration from a file.
     * 
     * @param file The file to load from
     * @return The loaded configuration
     * @throws IOException If loading fails
     */
    public static LoggingConfiguration loadFromFile(File file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        return fromProperties(props);
    }
}