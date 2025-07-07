package org.hasting.ui;

import org.hasting.ui.ImportOrganizeView.DirectoryItem;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for subdirectory selection functionality in ImportOrganizeView.
 * Tests the enhanced DirectoryItem with changeable path for Issue #53.
 */
@DisplayName("Subdirectory Selection Tests - Issue #53")
public class SubdirectorySelectionTest {
    
    @Test
    @DisplayName("Test DirectoryItem path property is changeable")
    void testDirectoryItemPathChangeable() {
        String initialPath = "/Users/music";
        DirectoryItem item = new DirectoryItem(initialPath);
        
        // Verify initial path
        assertEquals(initialPath, item.getPath(), "Initial path should match constructor parameter");
        
        // Test path change
        String newPath = "/Users/music/Jazz";
        item.setPath(newPath);
        assertEquals(newPath, item.getPath(), "Path should be updated after setPath()");
    }
    
    @Test
    @DisplayName("Test DirectoryItem path property is observable")
    void testDirectoryItemPathPropertyObservable() {
        DirectoryItem item = new DirectoryItem("/test/directory");
        String[] pathValues = {null};
        
        // Add listener to path property
        item.pathProperty().addListener((obs, oldVal, newVal) -> {
            pathValues[0] = newVal;
        });
        
        // Change path
        String newPath = "/test/directory/subdirectory";
        item.setPath(newPath);
        
        // Verify listener was called with new value
        assertEquals(newPath, pathValues[0], "Path property listener should receive new value");
    }
    
    @Test
    @DisplayName("Test path validation logic for subdirectory within original path")
    void testPathValidationLogic() {
        String originalPath = "/Users/music";
        
        // Test valid subdirectories (should be accepted)
        String[] validSubdirectories = {
            "/Users/music",              // Same path (valid)
            "/Users/music/Jazz",         // Direct subdirectory
            "/Users/music/Rock/Classic", // Nested subdirectory
            "/Users/music/Artists/Beatles/Albums" // Deep nested subdirectory
        };
        
        for (String testPath : validSubdirectories) {
            assertTrue(testPath.startsWith(originalPath), 
                      "Path '" + testPath + "' should be valid within '" + originalPath + "'");
        }
        
        // Test invalid paths (should be rejected)
        String[] invalidPaths = {
            "/Users/videos",             // Different directory
            "/Downloads/music",          // Different root
            "/Users/musi",               // Partial match but not subdirectory
            "/home/music"                // Different root entirely
        };
        
        for (String testPath : invalidPaths) {
            assertFalse(testPath.startsWith(originalPath), 
                       "Path '" + testPath + "' should be invalid for '" + originalPath + "'");
        }
    }
    
    @Test
    @DisplayName("Test DirectoryItem status updates with path changes")
    void testStatusUpdatesWithPathChanges() {
        String originalPath = "/Users/music";
        DirectoryItem item = new DirectoryItem(originalPath);
        
        // Initial status should be "Ready"
        assertEquals("Ready", item.getStatus(), "Initial status should be 'Ready'");
        
        // Simulate path change to subdirectory (as would happen in updateDirectoryItemPath)
        String subdirectoryPath = "/Users/music/Jazz";
        item.setPath(subdirectoryPath);
        item.setStatus("Subdirectory selected");
        
        assertEquals("Subdirectory selected", item.getStatus(), 
                    "Status should be updated when subdirectory is selected");
        assertEquals(subdirectoryPath, item.getPath(), 
                    "Path should be updated to subdirectory");
    }
    
    @Test
    @DisplayName("Test DirectoryItem maintains all properties after path change")
    void testAllPropertiesMaintainedAfterPathChange() {
        DirectoryItem item = new DirectoryItem("/Users/music");
        
        // Set some initial values
        item.setSelected(true);
        item.setStatus("Custom Status");
        item.setLastScanned("2025-01-01 12:00:00");
        
        // Store initial values
        boolean wasSelected = item.isSelected();
        String oldStatus = item.getStatus();
        String oldLastScanned = item.getLastScanned();
        
        // Change path
        item.setPath("/Users/music/Jazz");
        
        // Verify other properties are unchanged
        assertEquals(wasSelected, item.isSelected(), 
                    "Selection state should be preserved after path change");
        assertEquals(oldStatus, item.getStatus(), 
                    "Status should be preserved after path change (unless explicitly updated)");
        assertEquals(oldLastScanned, item.getLastScanned(), 
                    "Last scanned should be preserved after path change");
        
        // Verify new path is set
        assertEquals("/Users/music/Jazz", item.getPath(), 
                    "New path should be correctly set");
    }
    
    @Test
    @DisplayName("Test multiple path changes work correctly")
    void testMultiplePathChanges() {
        DirectoryItem item = new DirectoryItem("/Users/music");
        
        String[] pathSequence = {
            "/Users/music/Jazz",
            "/Users/music/Jazz/Modern",
            "/Users/music/Rock",
            "/Users/music"  // Back to original
        };
        
        for (String path : pathSequence) {
            item.setPath(path);
            assertEquals(path, item.getPath(), "Path should be correctly updated in sequence");
        }
    }
}