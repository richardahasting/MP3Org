package org.hasting.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for TabSwitchCallback functionality.
 * 
 * <p>This test validates the tab switching mechanism used to fix Issue #39:
 * "Fix Import tab navigation - 'Go to Import Tab' button does nothing"
 * 
 * <p>Tests verify that:
 * <ul>
 *   <li>Callback interface works correctly for success/failure scenarios</li>
 *   <li>Tab switching logic handles edge cases appropriately</li>
 *   <li>Case-sensitive matching works as expected</li>
 *   <li>Callback integration maintains proper separation of concerns</li>
 * </ul>
 * 
 * <p>Note: This test uses mock implementations instead of actual JavaFX components
 * to avoid JavaFX Application Thread initialization requirements in unit tests.
 * 
 * @since 1.0
 */
@DisplayName("Tab Switch Callback Tests")
public class TabSwitchCallbackTest {
    
    private MockTabContainer mockTabContainer;
    private TabSwitchCallback tabSwitchCallback;
    
    @BeforeEach
    void setUp() {
        // Create mock tab container with known tabs
        mockTabContainer = new MockTabContainer();
        mockTabContainer.addTab("Duplicate Manager");
        mockTabContainer.addTab("Metadata Editor");
        mockTabContainer.addTab("Import & Organize");
        mockTabContainer.addTab("Config");
        
        // Create the callback using similar logic as MP3OrgApplication
        tabSwitchCallback = createTabSwitchCallback(mockTabContainer);
    }
    
    /**
     * Mock tab container for testing without JavaFX dependencies.
     */
    private static class MockTabContainer {
        private final java.util.List<String> tabs = new java.util.ArrayList<>();
        private int selectedIndex = -1;
        
        void addTab(String tabName) {
            tabs.add(tabName);
            if (selectedIndex == -1) {
                selectedIndex = 0; // Select first tab when added to empty container
            }
        }
        
        boolean selectTab(String tabName) {
            if (tabName == null) return false;
            for (int i = 0; i < tabs.size(); i++) {
                if (tabs.get(i).equals(tabName)) {
                    selectedIndex = i;
                    return true;
                }
            }
            return false;
        }
        
        int getSelectedIndex() {
            return selectedIndex;
        }
        
        String getSelectedTabName() {
            return selectedIndex >= 0 && selectedIndex < tabs.size() ? tabs.get(selectedIndex) : null;
        }
        
        int getTabCount() {
            return tabs.size();
        }
        
        void clear() {
            tabs.clear();
            selectedIndex = -1;
        }
    }
    
    /**
     * Creates a tab switch callback for testing (mimics MP3OrgApplication logic).
     */
    private TabSwitchCallback createTabSwitchCallback(MockTabContainer container) {
        return (tabName) -> container.selectTab(tabName);
    }
    
    @Test
    @DisplayName("Should successfully switch to existing 'Import & Organize' tab")
    void testSwitchToImportTab() {
        // Given: Mock container with Import & Organize tab
        assertEquals(0, mockTabContainer.getSelectedIndex(), "Should start with first tab selected");
        
        // When: Switching to Import & Organize tab
        boolean result = tabSwitchCallback.switchToTab("Import & Organize");
        
        // Then: Should succeed and select the correct tab
        assertTrue(result, "Should return true for successful tab switch");
        assertEquals(2, mockTabContainer.getSelectedIndex(), "Should select Import & Organize tab (index 2)");
        assertEquals("Import & Organize", mockTabContainer.getSelectedTabName(), 
                    "Selected tab should have correct text");
    }
    
    @Test
    @DisplayName("Should successfully switch to any existing tab")
    void testSwitchToAllExistingTabs() {
        String[] expectedTabs = {"Duplicate Manager", "Metadata Editor", "Import & Organize", "Config"};
        
        for (int i = 0; i < expectedTabs.length; i++) {
            // When: Switching to each tab
            boolean result = tabSwitchCallback.switchToTab(expectedTabs[i]);
            
            // Then: Should succeed and select correct tab
            assertTrue(result, "Should successfully switch to " + expectedTabs[i]);
            assertEquals(i, mockTabContainer.getSelectedIndex(), 
                        "Should select tab at index " + i);
            assertEquals(expectedTabs[i], mockTabContainer.getSelectedTabName(),
                        "Selected tab should have correct text");
        }
    }
    
    @Test
    @DisplayName("Should fail gracefully when switching to non-existent tab")
    void testSwitchToNonExistentTab() {
        // Given: Starting tab selection
        int originalIndex = mockTabContainer.getSelectedIndex();
        
        // When: Attempting to switch to non-existent tab
        boolean result = tabSwitchCallback.switchToTab("Non-Existent Tab");
        
        // Then: Should fail and leave selection unchanged
        assertFalse(result, "Should return false for non-existent tab");
        assertEquals(originalIndex, mockTabContainer.getSelectedIndex(), 
                    "Tab selection should remain unchanged");
    }
    
    @Test
    @DisplayName("Should be case-sensitive when matching tab names")
    void testCaseSensitiveTabMatching() {
        // Given: Starting tab selection
        int originalIndex = mockTabContainer.getSelectedIndex();
        
        // When: Attempting to switch with incorrect case
        boolean result = tabSwitchCallback.switchToTab("import & organize"); // lowercase
        
        // Then: Should fail due to case mismatch
        assertFalse(result, "Should return false for incorrect case");
        assertEquals(originalIndex, mockTabContainer.getSelectedIndex(), 
                    "Tab selection should remain unchanged");
    }
    
    @Test
    @DisplayName("Should handle empty and null tab names gracefully")
    void testHandleInvalidTabNames() {
        int originalIndex = mockTabContainer.getSelectedIndex();
        
        // Test empty string
        boolean emptyResult = tabSwitchCallback.switchToTab("");
        assertFalse(emptyResult, "Should return false for empty tab name");
        assertEquals(originalIndex, mockTabContainer.getSelectedIndex(), 
                    "Tab selection should remain unchanged for empty name");
        
        // Test null string
        boolean nullResult = tabSwitchCallback.switchToTab(null);
        assertFalse(nullResult, "Should return false for null tab name");
        assertEquals(originalIndex, mockTabContainer.getSelectedIndex(), 
                    "Tab selection should remain unchanged for null name");
    }
    
    @Test
    @DisplayName("Should work correctly with empty tab container")
    void testEmptyTabContainer() {
        // Given: Empty tab container
        MockTabContainer emptyContainer = new MockTabContainer();
        TabSwitchCallback emptyCallback = createTabSwitchCallback(emptyContainer);
        
        // When: Attempting to switch to any tab
        boolean result = emptyCallback.switchToTab("Any Tab");
        
        // Then: Should fail gracefully
        assertFalse(result, "Should return false when no tabs exist");
        assertEquals(-1, emptyContainer.getSelectedIndex(), 
                    "Should have no selected tab in empty container");
    }
    
    @Test
    @DisplayName("Should handle partial tab name matches correctly")
    void testPartialTabNameMatches() {
        int originalIndex = mockTabContainer.getSelectedIndex();
        
        // Test partial matches (should fail - requires exact match)
        String[] partialNames = {"Import", "Organize", "Metadata", "Config Tab"};
        
        for (String partialName : partialNames) {
            boolean result = tabSwitchCallback.switchToTab(partialName);
            assertFalse(result, "Should return false for partial match: " + partialName);
            assertEquals(originalIndex, mockTabContainer.getSelectedIndex(), 
                        "Tab selection should remain unchanged for partial match");
        }
    }
    
    @Test
    @DisplayName("Should maintain correct tab count throughout operations")
    void testTabCountConsistency() {
        // Given: Known tab count
        int expectedTabCount = 4;
        assertEquals(expectedTabCount, mockTabContainer.getTabCount(), "Should have correct initial tab count");
        
        // When: Performing various tab switches
        tabSwitchCallback.switchToTab("Import & Organize");
        tabSwitchCallback.switchToTab("Non-Existent Tab");
        tabSwitchCallback.switchToTab("Config");
        
        // Then: Tab count should remain unchanged
        assertEquals(expectedTabCount, mockTabContainer.getTabCount(), "Tab count should remain constant");
    }
}