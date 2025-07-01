package org.hasting.util.logging;

/**
 * Example class demonstrating usage of the MP3Org custom logging framework.
 * 
 * <p>This class shows how to:</p>
 * <ul>
 *   <li>Get a logger for a class</li>
 *   <li>Log messages at different levels</li>
 *   <li>Use parameterized logging</li>
 *   <li>Log exceptions</li>
 *   <li>Configure logging settings</li>
 * </ul>
 * 
 * @since 1.0
 */
public class LoggingExample {
    
    // Get a logger for this class - this is the standard pattern
    private static final Logger logger = LoggerFactory.getLogger(LoggingExample.class);
    
    public static void main(String[] args) {
        // Example 1: Configure logging for development
        System.out.println("=== Configuring Logging ===");
        LoggingConfiguration config = LoggingConfiguration.createDevelopment();
        config.setConsoleEnabled(true);
        config.setFileEnabled(true);
        config.setFilePath("mp3org/logs/example.log");
        LoggerFactory.configure(config);
        
        // Example 2: Basic logging at different levels
        System.out.println("\n=== Basic Logging Examples ===");
        logger.debug("This is a debug message - very detailed information");
        logger.info("Application started successfully");
        logger.warning("This is a warning - something might be wrong");
        logger.error("This is an error - something definitely went wrong");
        logger.critical("This is critical - system might shut down");
        
        // Example 3: Parameterized logging (SLF4J-style)
        System.out.println("\n=== Parameterized Logging ===");
        String fileName = "music.mp3";
        int fileSize = 1024;
        logger.info("Processing file: {} (size: {} bytes)", fileName, fileSize);
        logger.debug("File details - name: {}, size: {}, type: {}", fileName, fileSize, "MP3");
        
        // Example 4: Exception logging
        System.out.println("\n=== Exception Logging ===");
        try {
            // Simulate an error
            throw new RuntimeException("Simulated file processing error");
        } catch (Exception e) {
            logger.error("Failed to process file {}: {}", fileName, e.getMessage(), e);
        }
        
        // Example 5: Level-specific configuration
        System.out.println("\n=== Logger-Specific Configuration ===");
        Logger specificLogger = LoggerFactory.getLogger("org.hasting.example.verbose");
        LoggerFactory.setLoggerLevel("org.hasting.example.verbose", LogLevel.DEBUG);
        
        specificLogger.debug("This debug message will be shown because we set DEBUG level");
        specificLogger.info("This info message will also be shown");
        
        // Example 6: Demonstrating level filtering
        System.out.println("\n=== Level Filtering Demo ===");
        Logger filteredLogger = LoggerFactory.getLogger("org.hasting.example.filtered");
        filteredLogger.setLevel(LogLevel.ERROR);
        
        filteredLogger.debug("This debug won't be shown (level too low)");
        filteredLogger.info("This info won't be shown (level too low)");
        filteredLogger.warning("This warning won't be shown (level too low)");
        filteredLogger.error("This error WILL be shown");
        filteredLogger.critical("This critical WILL be shown");
        
        // Example 7: Performance consideration - conditional logging
        System.out.println("\n=== Performance-Conscious Logging ===");
        if (logger.isLoggable(LogLevel.DEBUG)) {
            // Only do expensive operations if debug logging is enabled
            String expensiveDebugInfo = generateExpensiveDebugInfo();
            logger.debug("Expensive debug info: {}", expensiveDebugInfo);
        }
        
        // Cleanup
        System.out.println("\n=== Cleanup ===");
        LoggerFactory.flushAll(); // Ensure all messages are written
        
        System.out.println("Logging example completed. Check console output and mp3org/logs/example.log");
    }
    
    private static String generateExpensiveDebugInfo() {
        // Simulate expensive operation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("data-").append(i).append(" ");
        }
        return sb.toString();
    }
}