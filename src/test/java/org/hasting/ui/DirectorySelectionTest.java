package org.hasting.ui;

import org.hasting.ui.ImportOrganizeView.DirectoryItem;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for directory selection functionality in ImportOrganizeView.
 * Tests the DirectoryItem selection behavior for Issue #51.
 */
@DisplayName("Directory Selection Tests - Issue #51")
public class DirectorySelectionTest {
    
    @Test
    @DisplayName("Test DirectoryItem selection property")
    void testDirectoryItemSelection() {
        // Create a DirectoryItem
        DirectoryItem item = new DirectoryItem("/test/directory");
        
        // Verify initial state
        assertFalse(item.isSelected(), "DirectoryItem should start unselected");
        
        // Test selection
        item.setSelected(true);
        assertTrue(item.isSelected(), "DirectoryItem should be selected after setSelected(true)");
        
        // Test deselection
        item.setSelected(false);
        assertFalse(item.isSelected(), "DirectoryItem should be unselected after setSelected(false)");
    }
    
    @Test
    @DisplayName("Test DirectoryItem selection property listener")
    void testDirectoryItemSelectionListener() {
        DirectoryItem item = new DirectoryItem("/test/directory");
        boolean[] listenerCalled = {false};
        
        // Add listener to selection property
        item.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            listenerCalled[0] = true;
        });
        
        // Change selection state
        item.setSelected(true);
        
        // Verify listener was called
        assertTrue(listenerCalled[0], "Selection property listener should be called when selection changes");
    }
    
    @Test
    @DisplayName("Test DirectoryItem path and status properties")
    void testDirectoryItemProperties() {
        String testPath = "/test/directory/path";
        DirectoryItem item = new DirectoryItem(testPath);
        
        // Test path property
        assertEquals(testPath, item.getPath(), "Path should match constructor parameter");
        
        // Test status property
        assertEquals("Ready", item.getStatus(), "Default status should be 'Ready'");
        
        item.setStatus("Scanning");
        assertEquals("Scanning", item.getStatus(), "Status should update correctly");
        
        // Test lastScanned property
        assertEquals("Never", item.getLastScanned(), "Default lastScanned should be 'Never'");
        
        item.setLastScanned("2025-01-01");
        assertEquals("2025-01-01", item.getLastScanned(), "LastScanned should update correctly");
    }
    
    @Test
    @DisplayName("Test DirectoryItem property observability")
    void testDirectoryItemPropertyObservability() {
        DirectoryItem item = new DirectoryItem("/test");
        
        // Test that properties are observable (not null)
        assertNotNull(item.selectedProperty(), "Selected property should not be null");
        assertNotNull(item.statusProperty(), "Status property should not be null");
        assertNotNull(item.lastScannedProperty(), "LastScanned property should not be null");
        
        // Test property binding capability
        String[] statusValues = {null};
        String[] lastScannedValues = {null};
        
        item.statusProperty().addListener((obs, oldVal, newVal) -> statusValues[0] = newVal);
        item.lastScannedProperty().addListener((obs, oldVal, newVal) -> lastScannedValues[0] = newVal);
        
        item.setStatus("Test Status");
        item.setLastScanned("Test Time");
        
        assertEquals("Test Status", statusValues[0], "Status property listener should receive new value");
        assertEquals("Test Time", lastScannedValues[0], "LastScanned property listener should receive new value");
    }
}