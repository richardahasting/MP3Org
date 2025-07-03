package org.hasting.util.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the custom MP3Org logging framework.
 * 
 * <p>Tests core functionality including:</p>
 * <ul>
 *   <li>Logger creation and configuration</li>
 *   <li>Log level filtering</li>
 *   <li>Message formatting with parameters</li>
 *   <li>Console and file output</li>
 *   <li>Configuration management</li>
 * </ul>
 */
public class LoggingFrameworkTest {
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Reset logging system before each test
        LoggerFactory.shutdown();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        LoggerFactory.shutdown();
    }
    
    @Test
    void testLoggerCreation() {
        Logger logger = LoggerFactory.getLogger(LoggingFrameworkTest.class);
        
        assertNotNull(logger);
        assertEquals(LoggingFrameworkTest.class.getName(), logger.getName());
        assertEquals(LogLevel.INFO, logger.getLevel()); // Default level
    }
    
    @Test
    void testLoggerSingleton() {
        Logger logger1 = LoggerFactory.getLogger("test.logger");
        Logger logger2 = LoggerFactory.getLogger("test.logger");
        
        assertSame(logger1, logger2, "Same logger name should return same instance");
    }
    
    @Test
    void testLogLevels() {
        Logger logger = LoggerFactory.getLogger("test.levels");
        
        // Test level hierarchy
        logger.setLevel(LogLevel.WARNING);
        
        assertFalse(logger.isLoggable(LogLevel.DEBUG));
        assertFalse(logger.isLoggable(LogLevel.INFO));
        assertTrue(logger.isLoggable(LogLevel.WARNING));
        assertTrue(logger.isLoggable(LogLevel.ERROR));
        assertTrue(logger.isLoggable(LogLevel.CRITICAL));
    }
    
    @Test
    void testLogLevelParsing() {
        assertEquals(LogLevel.DEBUG, LogLevel.fromString("DEBUG"));
        assertEquals(LogLevel.INFO, LogLevel.fromString("info"));
        assertEquals(LogLevel.WARNING, LogLevel.fromString("Warning"));
        
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromString("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromString(""));
    }
    
    @Test
    void testMessageFormatting() {
        LogRecord record1 = new LogRecord(LogLevel.INFO, "test", "Simple message");
        assertEquals("Simple message", record1.getFormattedMessage());
        
        LogRecord record2 = new LogRecord(LogLevel.INFO, "test", "Message with {}", "parameter");
        assertEquals("Message with parameter", record2.getFormattedMessage());
        
        LogRecord record3 = new LogRecord(LogLevel.INFO, "test", "Multiple {} and {}", "param1", "param2");
        assertEquals("Multiple param1 and param2", record3.getFormattedMessage());
        
        LogRecord record4 = new LogRecord(LogLevel.INFO, "test", "Null param: {}", (Object) null);
        assertEquals("Null param: null", record4.getFormattedMessage());
    }
    
    @Test
    void testFileLogging() throws IOException {
        Path logFile = tempDir.resolve("test.log");
        
        // Configure file logging
        LoggingConfiguration config = new LoggingConfiguration();
        config.setFileEnabled(true);
        config.setFilePath(logFile.toString());
        config.setConsoleEnabled(false); // Disable console for this test
        
        LoggerFactory.configure(config);
        
        Logger logger = LoggerFactory.getLogger("test.file");
        logger.info("Test message for file logging");
        logger.error("Error message with parameter: {}", "test-param");
        
        // Flush to ensure data is written
        LoggerFactory.flushAll();
        
        // Verify file was created and contains expected content
        assertTrue(Files.exists(logFile), "Log file should be created");
        
        List<String> lines = Files.readAllLines(logFile);
        assertFalse(lines.isEmpty(), "Log file should contain log entries");
        
        String content = String.join("\n", lines);
        assertTrue(content.contains("Test message for file logging"));
        assertTrue(content.contains("Error message with parameter: test-param"));
        assertTrue(content.contains("[INFO]"));
        assertTrue(content.contains("[ERROR]"));
    }
    
    @Test
    void testConfiguration() {
        LoggingConfiguration config = LoggingConfiguration.createDevelopment();
        
        assertEquals(LogLevel.DEBUG, config.getDefaultLevel());
        assertTrue(config.isConsoleEnabled());
        assertTrue(config.isFileEnabled());
        assertNotNull(config.getFilePath());
    }
    
    @Test
    void testConfigurationPersistence() throws IOException {
        LoggingConfiguration originalConfig = new LoggingConfiguration();
        originalConfig.setDefaultLevel(LogLevel.DEBUG);
        originalConfig.setConsoleEnabled(true);
        originalConfig.setFileEnabled(true);
        originalConfig.setFilePath("custom/path/log.txt");
        originalConfig.setLoggerLevel("org.example", LogLevel.WARNING);
        
        // Save and reload
        File configFile = tempDir.resolve("logging.properties").toFile();
        originalConfig.saveToFile(configFile);
        
        LoggingConfiguration loadedConfig = LoggingConfiguration.loadFromFile(configFile);
        
        assertEquals(originalConfig.getDefaultLevel(), loadedConfig.getDefaultLevel());
        assertEquals(originalConfig.isConsoleEnabled(), loadedConfig.isConsoleEnabled());
        assertEquals(originalConfig.isFileEnabled(), loadedConfig.isFileEnabled());
        assertEquals(originalConfig.getFilePath(), loadedConfig.getFilePath());
        assertEquals(originalConfig.getLoggerLevel("org.example"), loadedConfig.getLoggerLevel("org.example"));
    }
    
    @Test
    void testLoggerFactoryConfiguration() {
        LoggingConfiguration config = new LoggingConfiguration();
        config.setDefaultLevel(LogLevel.DEBUG);
        config.setLoggerLevel("specific.logger", LogLevel.ERROR);
        
        LoggerFactory.configure(config);
        
        Logger defaultLogger = LoggerFactory.getLogger("default.logger");
        Logger specificLogger = LoggerFactory.getLogger("specific.logger");
        
        assertEquals(LogLevel.DEBUG, defaultLogger.getLevel());
        assertEquals(LogLevel.ERROR, specificLogger.getLevel());
    }
    
    @Test
    void testExceptionLogging() {
        LogRecord record = new LogRecord(LogLevel.ERROR, "test", "Error occurred", 
                                        new RuntimeException("Test exception"));
        
        assertTrue(record.hasException());
        assertNotNull(record.getException());
        assertEquals("Test exception", record.getException().getMessage());
    }
    
    @Test
    void testDefaultLogFormatter() {
        LogFormatter formatter = new DefaultLogFormatter();
        LogRecord record = new LogRecord(LogLevel.INFO, "test.logger", "Test message");
        
        String formatted = formatter.format(record);
        
        assertTrue(formatted.contains("[INFO]"));
        assertTrue(formatted.contains("test.logger"));
        assertTrue(formatted.contains("Test message"));
        assertTrue(formatted.contains("-")); // Separator
    }
}