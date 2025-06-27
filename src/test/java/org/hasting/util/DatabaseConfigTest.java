package org.hasting.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseConfigTest {

    @TempDir
    Path tempDir;
    
    private String originalSystemProperty;
    private String originalEnvVar;

    @BeforeEach
    void setUp() {
        // Save original values
        originalSystemProperty = System.getProperty("mp3org.database.path");
        
        // Clear system properties for clean testing
        System.clearProperty("mp3org.database.path");
    }

    @AfterEach
    void tearDown() {
        // Restore original values
        if (originalSystemProperty != null) {
            System.setProperty("mp3org.database.path", originalSystemProperty);
        } else {
            System.clearProperty("mp3org.database.path");
        }
        
        // Clear any test properties files
        try {
            Path configFile = tempDir.resolve("mp3org.properties");
            if (Files.exists(configFile)) {
                Files.delete(configFile);
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test singleton pattern")
    void testSingletonPattern() {
        DatabaseConfig instance1 = DatabaseConfig.getInstance();
        DatabaseConfig instance2 = DatabaseConfig.getInstance();
        
        assertSame(instance1, instance2, "Should return the same instance");
        assertNotNull(instance1, "Instance should not be null");
    }

    @Test
    @Order(2)
    @DisplayName("Test default configuration")
    void testDefaultConfiguration() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // Should use default path when no configuration is provided
        assertTrue(config.getDatabasePath().endsWith("mp3org") || 
                  config.getDatabasePath().contains("mp3org"));
        
        assertNotNull(config.getJdbcUrl());
        assertTrue(config.getJdbcUrl().startsWith("jdbc:derby:"));
        assertTrue(config.getJdbcUrl().contains("create=true"));
        
        assertEquals("org.apache.derby.jdbc.EmbeddedDriver", config.getJdbcDriver());
        assertEquals("", config.getUsername());
        assertEquals("", config.getPassword());
    }

    @Test
    @Order(3)
    @DisplayName("Test system property configuration")
    void testSystemPropertyConfiguration() {
        String testPath = tempDir.resolve("test-db-sysprop").toString();
        System.setProperty("mp3org.database.path", testPath);
        
        // Create new instance to pick up system property
        DatabaseConfig config = DatabaseConfig.getInstance();
        config.reload(); // Force reload to pick up system property
        
        assertTrue(config.getDatabasePath().contains("test-db-sysprop"));
        assertTrue(config.getJdbcUrl().contains(testPath));
    }

    @Test
    @Order(4)
    @DisplayName("Test configuration file")
    void testConfigurationFile() throws IOException {
        // Create a temporary config file
        Path configFile = tempDir.resolve("mp3org.properties");
        Properties props = new Properties();
        String testPath = tempDir.resolve("test-db-config").toString();
        props.setProperty("database.path", testPath);
        
        try (var output = Files.newOutputStream(configFile)) {
            props.store(output, "Test configuration");
        }
        
        // Temporarily change working directory context for this test
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            DatabaseConfig config = DatabaseConfig.getInstance();
            config.reload();
            
            // Should use the path from config file
            assertTrue(config.getDatabasePath().contains("test-db-config"));
            
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test set database path")
    void testSetDatabasePath() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        String originalPath = config.getDatabasePath();
        
        String newPath = tempDir.resolve("new-database").toString();
        config.setDatabasePath(newPath);
        
        assertTrue(config.getDatabasePath().contains("new-database"));
        assertNotEquals(originalPath, config.getDatabasePath());
        assertTrue(config.getJdbcUrl().contains("new-database"));
    }

    @Test
    @Order(6)
    @DisplayName("Test invalid database path")
    void testInvalidDatabasePath() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        assertThrows(IllegalArgumentException.class, () -> {
            config.setDatabasePath(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            config.setDatabasePath("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            config.setDatabasePath("   ");
        });
    }

    @Test
    @Order(7)
    @DisplayName("Test JDBC URL construction")
    void testJdbcUrlConstruction() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        String testPath = tempDir.resolve("test-jdbc").toString();
        
        config.setDatabasePath(testPath);
        
        String jdbcUrl = config.getJdbcUrl();
        assertTrue(jdbcUrl.startsWith("jdbc:derby:"));
        assertTrue(jdbcUrl.contains(testPath));
        assertTrue(jdbcUrl.endsWith(";create=true"));
    }

    @Test
    @Order(8)
    @DisplayName("Test configuration info")
    void testConfigurationInfo() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        String info = config.getConfigurationInfo();
        
        assertNotNull(info);
        assertFalse(info.isEmpty());
        assertTrue(info.contains("Database Configuration"));
        assertTrue(info.contains("Path:"));
        assertTrue(info.contains("JDBC URL:"));
        assertTrue(info.contains("System Property"));
        assertTrue(info.contains("Environment Variable"));
        assertTrue(info.contains("Configuration File"));
    }

    @Test
    @Order(9)
    @DisplayName("Test path normalization")
    void testPathNormalization() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // Test relative path
        config.setDatabasePath("relative/path");
        assertTrue(config.getDatabasePath().contains("relative"));
        
        // Test absolute path
        String absolutePath = tempDir.resolve("absolute").toString();
        config.setDatabasePath(absolutePath);
        assertEquals(absolutePath, config.getDatabasePath());
        
        // Test path with extra spaces
        config.setDatabasePath("  " + absolutePath + "  ");
        assertEquals(absolutePath, config.getDatabasePath());
    }

    @Test
    @Order(10)
    @DisplayName("Test reload functionality")
    void testReloadFunctionality() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        String originalPath = config.getDatabasePath();
        
        // Change system property
        String newPath = tempDir.resolve("reload-test").toString();
        System.setProperty("mp3org.database.path", newPath);
        
        // Reload should pick up the new system property
        config.reload();
        
        assertTrue(config.getDatabasePath().contains("reload-test"));
        assertNotEquals(originalPath, config.getDatabasePath());
    }

    @Test
    @Order(11)
    @DisplayName("Test create sample config file")
    void testCreateSampleConfigFile() {
        // Change working directory to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Create sample config file
            DatabaseConfig.createSampleConfigFile();
            
            // Verify file was created
            Path configFile = tempDir.resolve("mp3org.properties");
            assertTrue(Files.exists(configFile));
            
            // Verify file content
            String content = Files.readString(configFile);
            assertTrue(content.contains("database.path"));
            assertTrue(content.contains("MP3Org Database Configuration"));
            
        } catch (IOException e) {
            fail("Failed to test sample config file creation: " + e.getMessage());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @Order(12)
    @DisplayName("Test precedence order")
    void testPrecedenceOrder() throws IOException {
        // Set up config file
        Path configFile = tempDir.resolve("mp3org.properties");
        Properties props = new Properties();
        props.setProperty("database.path", tempDir.resolve("config-file-path").toString());
        try (var output = Files.newOutputStream(configFile)) {
            props.store(output, "Test configuration");
        }
        
        // Set system property (should override config file)
        String systemPropPath = tempDir.resolve("system-prop-path").toString();
        System.setProperty("mp3org.database.path", systemPropPath);
        
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            DatabaseConfig config = DatabaseConfig.getInstance();
            config.reload();
            
            // System property should win over config file
            assertTrue(config.getDatabasePath().contains("system-prop-path"));
            assertFalse(config.getDatabasePath().contains("config-file-path"));
            
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @Order(13)
    @DisplayName("Test directory creation")
    void testDirectoryCreation() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // Set path to non-existent directory
        Path newDbPath = tempDir.resolve("new").resolve("nested").resolve("database");
        config.setDatabasePath(newDbPath.toString());
        
        // Parent directories should be created
        assertTrue(Files.exists(newDbPath.getParent()));
    }

    @Test
    @Order(14)
    @DisplayName("Test thread safety")
    void testThreadSafety() throws InterruptedException {
        final DatabaseConfig config = DatabaseConfig.getInstance();
        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount];
        
        // Create multiple threads that access the singleton
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                DatabaseConfig threadConfig = DatabaseConfig.getInstance();
                results[threadIndex] = (threadConfig == config);
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // All threads should get the same instance
        for (boolean result : results) {
            assertTrue(result, "All threads should get the same singleton instance");
        }
    }

    @Test
    @Order(15)
    @DisplayName("Test configuration persistence")
    void testConfigurationPersistence() {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            DatabaseConfig config = DatabaseConfig.getInstance();
            String testPath = tempDir.resolve("persistent-test").toString();
            
            // Set new path (should save to config file)
            config.setDatabasePath(testPath);
            
            // Verify config file was created/updated
            Path configFile = tempDir.resolve("mp3org.properties");
            assertTrue(Files.exists(configFile));
            
            // Verify the path was saved
            Properties props = new Properties();
            try (var input = Files.newInputStream(configFile)) {
                props.load(input);
                assertEquals(testPath, props.getProperty("database.path"));
            }
            
        } catch (IOException e) {
            fail("Failed to test configuration persistence: " + e.getMessage());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }
}