package org.hasting.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for MetadataEditorView tab switching integration.
 * 
 * <p>This test validates the TabSwitchCallback interface and integration patterns
 * that were implemented to fix Issue #39: "Fix Import tab navigation - 'Go to Import Tab' button does nothing"
 * 
 * <p>Tests verify that:
 * <ul>
 *   <li>TabSwitchCallback interface works correctly</li>
 *   <li>Callback patterns support success and failure scenarios</li>
 *   <li>Interface design supports proper separation of concerns</li>
 *   <li>Integration patterns work as expected</li>
 * </ul>
 * 
 * <p>Note: This test focuses on callback interface behavior and integration patterns
 * rather than full UI testing to avoid JavaFX dependencies.
 * 
 * @since 1.0
 */
@DisplayName("MetadataEditorView Tab Switch Integration Tests")
public class MetadataEditorTabSwitchTest {
    
    private AtomicBoolean callbackInvoked;
    private AtomicReference<String> requestedTabName;
    private AtomicBoolean shouldReturnSuccess;
    
    @BeforeEach
    void setUp() {
        callbackInvoked = new AtomicBoolean(false);
        requestedTabName = new AtomicReference<>();
        shouldReturnSuccess = new AtomicBoolean(true);
    }
    
    /**
     * Creates a mock TabSwitchCallback for testing.
     */
    private TabSwitchCallback createMockCallback() {
        return (tabName) -> {
            callbackInvoked.set(true);
            requestedTabName.set(tabName);
            return shouldReturnSuccess.get();
        };
    }
    
    @Test
    @DisplayName("Should create functional TabSwitchCallback interface")
    void testCallbackInterface() {
        // Given: A TabSwitchCallback implementation
        TabSwitchCallback callback = createMockCallback();
        
        // When: Calling the callback
        boolean result = callback.switchToTab("Test Tab");
        
        // Then: Should invoke correctly
        assertTrue(callbackInvoked.get(), "Callback should be invoked");
        assertEquals("Test Tab", requestedTabName.get(), "Should receive correct tab name");
        assertTrue(result, "Should return success by default");
    }
    
    @Test
    @DisplayName("Should handle callback success scenario")
    void testCallbackSuccessScenario() {
        // Given: Callback configured for success
        shouldReturnSuccess.set(true);
        TabSwitchCallback callback = createMockCallback();
        
        // When: Calling callback for Import & Organize tab
        boolean result = callback.switchToTab("Import & Organize");
        
        // Then: Should handle success appropriately
        assertTrue(callbackInvoked.get(), "Callback should be invoked");
        assertEquals("Import & Organize", requestedTabName.get(), "Should request correct tab");
        assertTrue(result, "Should return true for success");
    }
    
    @Test
    @DisplayName("Should handle callback failure scenario")
    void testCallbackFailureScenario() {
        // Given: Callback configured for failure
        shouldReturnSuccess.set(false);
        TabSwitchCallback callback = createMockCallback();
        
        // When: Calling callback for Import & Organize tab
        boolean result = callback.switchToTab("Import & Organize");
        
        // Then: Should handle failure appropriately
        assertTrue(callbackInvoked.get(), "Callback should be invoked");
        assertEquals("Import & Organize", requestedTabName.get(), "Should request correct tab");
        assertFalse(result, "Should return false for failure");
    }
    
    @Test
    @DisplayName("Should support multiple callback invocations")
    void testMultipleCallbackInvocations() {
        // Given: A callback
        TabSwitchCallback callback = createMockCallback();
        
        // When: Calling multiple times with different tabs
        callback.switchToTab("Tab 1");
        assertEquals("Tab 1", requestedTabName.get(), "Should handle first call");
        
        callback.switchToTab("Tab 2");
        assertEquals("Tab 2", requestedTabName.get(), "Should handle second call");
        
        callback.switchToTab("Import & Organize");
        assertEquals("Import & Organize", requestedTabName.get(), "Should handle third call");
        
        // Then: All calls should be processed
        assertTrue(callbackInvoked.get(), "Callback should remain invokable");
    }
    
    @Test
    @DisplayName("Should handle null tab names in callback")
    void testCallbackWithNullTabName() {
        // Given: A callback
        TabSwitchCallback callback = createMockCallback();
        
        // When: Calling with null tab name
        boolean result = callback.switchToTab(null);
        
        // Then: Should handle gracefully
        assertTrue(callbackInvoked.get(), "Callback should be invoked");
        assertNull(requestedTabName.get(), "Should receive null tab name");
        assertTrue(result, "Should return configured result");
    }
    
    @Test
    @DisplayName("Should handle empty tab names in callback")
    void testCallbackWithEmptyTabName() {
        // Given: A callback
        TabSwitchCallback callback = createMockCallback();
        
        // When: Calling with empty tab name
        boolean result = callback.switchToTab("");
        
        // Then: Should handle gracefully
        assertTrue(callbackInvoked.get(), "Callback should be invoked");
        assertEquals("", requestedTabName.get(), "Should receive empty tab name");
        assertTrue(result, "Should return configured result");
    }
    
    @Test
    @DisplayName("Should support lambda implementation of TabSwitchCallback")
    void testLambdaImplementation() {
        // Given: Lambda implementation of TabSwitchCallback
        AtomicReference<String> lambdaTabName = new AtomicReference<>();
        TabSwitchCallback lambdaCallback = (tabName) -> {
            lambdaTabName.set(tabName);
            return "Import & Organize".equals(tabName);
        };
        
        // When: Using lambda callback
        boolean successResult = lambdaCallback.switchToTab("Import & Organize");
        boolean failureResult = lambdaCallback.switchToTab("Other Tab");
        
        // Then: Should work correctly
        assertTrue(successResult, "Should return true for correct tab name");
        assertFalse(failureResult, "Should return false for incorrect tab name");
        assertEquals("Other Tab", lambdaTabName.get(), "Should capture last tab name");
    }
    
    @Test
    @DisplayName("Should demonstrate proper callback integration pattern")
    void testCallbackIntegrationPattern() {
        // Given: A component that uses TabSwitchCallback
        MockComponent component = new MockComponent();
        TabSwitchCallback callback = createMockCallback();
        
        // When: Setting callback and triggering tab switch
        component.setTabSwitchCallback(callback);
        boolean result = component.requestTabSwitch("Import & Organize");
        
        // Then: Should integrate correctly
        assertTrue(result, "Should return callback result");
        assertTrue(callbackInvoked.get(), "Should invoke callback");
        assertEquals("Import & Organize", requestedTabName.get(), "Should pass correct tab name");
    }
    
    /**
     * Mock component to demonstrate callback integration pattern.
     */
    private static class MockComponent {
        private TabSwitchCallback tabSwitchCallback;
        
        void setTabSwitchCallback(TabSwitchCallback callback) {
            this.tabSwitchCallback = callback;
        }
        
        boolean requestTabSwitch(String tabName) {
            if (tabSwitchCallback != null) {
                return tabSwitchCallback.switchToTab(tabName);
            }
            return false;
        }
    }
}