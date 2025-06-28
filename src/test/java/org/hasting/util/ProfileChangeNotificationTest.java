package org.hasting.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for profile change notification system.
 */
public class ProfileChangeNotificationTest {
    
    private ProfileChangeNotifier notifier;
    private TestProfileChangeListener testListener;
    
    @BeforeEach
    void setUp() {
        notifier = ProfileChangeNotifier.getInstance();
        notifier.clearAllListeners(); // Start with clean state
        
        testListener = new TestProfileChangeListener();
        notifier.addListener(testListener);
    }
    
    @AfterEach
    void tearDown() {
        notifier.clearAllListeners();
    }
    
    @Test
    @DisplayName("Test profile change notification")
    void testProfileChangeNotification() {
        // Create test profile
        DatabaseProfile testProfile = new DatabaseProfile("test-id", "Test Profile", "/test/path");
        
        // Notify profile change
        notifier.notifyProfileChanged("old-id", "test-id", testProfile);
        
        // Verify listener was called
        assertTrue(testListener.profileChangeReceived);
        assertEquals("old-id", testListener.lastOldProfileId);
        assertEquals("test-id", testListener.lastNewProfileId);
        assertEquals(testProfile, testListener.lastNewProfile);
    }
    
    @Test
    @DisplayName("Test database change notification")
    void testDatabaseChangeNotification() {
        // Notify database change
        notifier.notifyDatabaseChanged("/old/path", "/new/path", true);
        
        // Verify listener was called
        assertTrue(testListener.databaseChangeReceived);
        assertEquals("/old/path", testListener.lastOldDatabasePath);
        assertEquals("/new/path", testListener.lastNewDatabasePath);
        assertTrue(testListener.lastIsNewDatabase);
    }
    
    @Test
    @DisplayName("Test multiple listeners")
    void testMultipleListeners() {
        TestProfileChangeListener secondListener = new TestProfileChangeListener();
        notifier.addListener(secondListener);
        
        assertEquals(2, notifier.getListenerCount());
        
        // Notify change
        DatabaseProfile testProfile = new DatabaseProfile("test-id", "Test Profile", "/test/path");
        notifier.notifyProfileChanged(null, "test-id", testProfile);
        
        // Both listeners should receive notification
        assertTrue(testListener.profileChangeReceived);
        assertTrue(secondListener.profileChangeReceived);
        
        // Clean up
        notifier.removeListener(secondListener);
        assertEquals(1, notifier.getListenerCount());
    }
    
    @Test
    @DisplayName("Test listener removal")
    void testListenerRemoval() {
        assertEquals(1, notifier.getListenerCount());
        
        notifier.removeListener(testListener);
        assertEquals(0, notifier.getListenerCount());
        
        // Notify change - listener should not receive it
        DatabaseProfile testProfile = new DatabaseProfile("test-id", "Test Profile", "/test/path");
        notifier.notifyProfileChanged(null, "test-id", testProfile);
        
        assertFalse(testListener.profileChangeReceived);
    }
    
    @Test
    @DisplayName("Test error handling in listeners")
    void testErrorHandlingInListeners() {
        // Add a listener that throws an exception
        ProfileChangeListener errorListener = new ProfileChangeListener() {
            @Override
            public void onProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile) {
                throw new RuntimeException("Test exception");
            }
            
            @Override
            public void onDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase) {
                throw new RuntimeException("Test exception");
            }
        };
        
        notifier.addListener(errorListener);
        
        // Notification should still work despite the exception
        DatabaseProfile testProfile = new DatabaseProfile("test-id", "Test Profile", "/test/path");
        
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            notifier.notifyProfileChanged(null, "test-id", testProfile);
        });
        
        // Normal listener should still receive notification
        assertTrue(testListener.profileChangeReceived);
        
        // Clean up
        notifier.removeListener(errorListener);
    }
    
    /**
     * Test implementation of ProfileChangeListener for testing purposes.
     */
    private static class TestProfileChangeListener implements ProfileChangeListener {
        boolean profileChangeReceived = false;
        boolean databaseChangeReceived = false;
        
        String lastOldProfileId;
        String lastNewProfileId;
        DatabaseProfile lastNewProfile;
        
        String lastOldDatabasePath;
        String lastNewDatabasePath;
        boolean lastIsNewDatabase;
        
        @Override
        public void onProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile) {
            profileChangeReceived = true;
            lastOldProfileId = oldProfileId;
            lastNewProfileId = newProfileId;
            lastNewProfile = newProfile;
        }
        
        @Override
        public void onDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase) {
            databaseChangeReceived = true;
            lastOldDatabasePath = oldDatabasePath;
            lastNewDatabasePath = newDatabasePath;
            lastIsNewDatabase = isNewDatabase;
        }
    }
}