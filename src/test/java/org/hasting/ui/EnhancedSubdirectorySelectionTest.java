package org.hasting.ui;

import org.hasting.ui.ImportOrganizeView.DirectoryItem;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for enhanced subdirectory selection functionality in ImportOrganizeView.
 * Tests the hierarchical directory system with auto-selection and multiple subdirectories for Issue #55.
 */
@DisplayName("Enhanced Subdirectory Selection Tests - Issue #55")
public class EnhancedSubdirectorySelectionTest {
    
    @Test
    @DisplayName("Test original directory creation and properties")
    void testOriginalDirectoryCreation() {
        String path = "/Users/music";
        DirectoryItem originalDir = new DirectoryItem(path);
        
        // Verify original directory properties
        assertTrue(originalDir.isOriginalDirectory(), "Should be original directory");
        assertFalse(originalDir.isSelected(), "Original directories should start unselected");
        assertEquals("Ready", originalDir.getStatus(), "Original directory status should be 'Ready'");
        assertEquals(path, originalDir.getOriginalRootPath(), "Original root path should match directory path");
        assertEquals(path, originalDir.getPath(), "Path should match constructor parameter");
    }
    
    @Test
    @DisplayName("Test subdirectory creation and auto-selection")
    void testSubdirectoryCreationAndAutoSelection() {
        String originalPath = "/Users/music";
        String subdirectoryPath = "/Users/music/Jazz";
        
        DirectoryItem subdirectory = new DirectoryItem(subdirectoryPath, originalPath);
        
        // Verify subdirectory properties
        assertFalse(subdirectory.isOriginalDirectory(), "Should not be original directory");
        assertTrue(subdirectory.isSelected(), "Subdirectories should be auto-selected");
        assertEquals("Subdirectory", subdirectory.getStatus(), "Subdirectory status should be 'Subdirectory'");
        assertEquals(originalPath, subdirectory.getOriginalRootPath(), "Should store original root path");
        assertEquals(subdirectoryPath, subdirectory.getPath(), "Path should match subdirectory path");
        assertEquals("Never", subdirectory.getLastScanned(), "New subdirectories should have 'Never' for last scanned");
    }
    
    @Test
    @DisplayName("Test directory type differentiation")
    void testDirectoryTypeDifferentiation() {
        DirectoryItem originalDir = new DirectoryItem("/Users/music");
        DirectoryItem subdirectory = new DirectoryItem("/Users/music/Jazz", "/Users/music");
        
        // Verify they have different types
        assertTrue(originalDir.isOriginalDirectory(), "Original should be original directory");
        assertFalse(subdirectory.isOriginalDirectory(), "Subdirectory should not be original directory");
        
        // Verify different selection states
        assertFalse(originalDir.isSelected(), "Original should start unselected");
        assertTrue(subdirectory.isSelected(), "Subdirectory should be auto-selected");
        
        // Verify different statuses
        assertEquals("Ready", originalDir.getStatus(), "Original should have 'Ready' status");
        assertEquals("Subdirectory", subdirectory.getStatus(), "Subdirectory should have 'Subdirectory' status");
    }
    
    @Test
    @DisplayName("Test multiple subdirectories under same original directory")
    void testMultipleSubdirectories() {
        String originalPath = "/Users/music";
        DirectoryItem originalDir = new DirectoryItem(originalPath);
        
        // Create multiple subdirectories
        DirectoryItem jazzDir = new DirectoryItem("/Users/music/Jazz", originalPath);
        DirectoryItem rockDir = new DirectoryItem("/Users/music/Rock", originalPath);
        DirectoryItem classicalDir = new DirectoryItem("/Users/music/Classical", originalPath);
        
        // Verify all subdirectories reference the same original root
        assertEquals(originalPath, jazzDir.getOriginalRootPath(), "Jazz should reference original path");
        assertEquals(originalPath, rockDir.getOriginalRootPath(), "Rock should reference original path");
        assertEquals(originalPath, classicalDir.getOriginalRootPath(), "Classical should reference original path");
        
        // Verify all subdirectories are auto-selected
        assertTrue(jazzDir.isSelected(), "Jazz subdirectory should be auto-selected");
        assertTrue(rockDir.isSelected(), "Rock subdirectory should be auto-selected");
        assertTrue(classicalDir.isSelected(), "Classical subdirectory should be auto-selected");
        
        // Verify original directory remains unselected
        assertFalse(originalDir.isSelected(), "Original directory should remain unselected");
    }
    
    @Test
    @DisplayName("Test subdirectory property observability")
    void testSubdirectoryPropertyObservability() {
        DirectoryItem subdirectory = new DirectoryItem("/Users/music/Jazz", "/Users/music");
        
        // Test that all properties are observable
        assertNotNull(subdirectory.pathProperty(), "Path property should not be null");
        assertNotNull(subdirectory.selectedProperty(), "Selected property should not be null");
        assertNotNull(subdirectory.statusProperty(), "Status property should not be null");
        assertNotNull(subdirectory.lastScannedProperty(), "LastScanned property should not be null");
        
        // Test property change listeners
        boolean[] selectionChanged = {false};
        boolean[] statusChanged = {false};
        
        subdirectory.selectedProperty().addListener((obs, oldVal, newVal) -> selectionChanged[0] = true);
        subdirectory.statusProperty().addListener((obs, oldVal, newVal) -> statusChanged[0] = true);
        
        // Trigger changes
        subdirectory.setSelected(false);
        subdirectory.setStatus("Test Status");
        
        // Verify listeners were called
        assertTrue(selectionChanged[0], "Selection property listener should be called");
        assertTrue(statusChanged[0], "Status property listener should be called");
    }
    
    @Test
    @DisplayName("Test path validation for subdirectories")
    void testPathValidationForSubdirectories() {
        String originalPath = "/Users/music";
        
        // Test valid subdirectory paths
        String[] validPaths = {
            "/Users/music/Jazz",
            "/Users/music/Rock/Classic",
            "/Users/music/Artists/Beatles/Albums"
        };
        
        for (String path : validPaths) {
            assertTrue(path.startsWith(originalPath), 
                      "Path '" + path + "' should be valid subdirectory of '" + originalPath + "'");
            
            DirectoryItem subdirectory = new DirectoryItem(path, originalPath);
            assertEquals(originalPath, subdirectory.getOriginalRootPath(), 
                        "Should store correct original root path");
        }
        
        // Test that original path itself is valid (selecting same directory)
        assertTrue(originalPath.startsWith(originalPath), 
                  "Original path should be valid for itself");
    }
    
    @Test
    @DisplayName("Test subdirectory selection state changes")
    void testSubdirectorySelectionStateChanges() {
        DirectoryItem subdirectory = new DirectoryItem("/Users/music/Jazz", "/Users/music");
        
        // Verify starts selected
        assertTrue(subdirectory.isSelected(), "Should start auto-selected");
        
        // Test deselection
        subdirectory.setSelected(false);
        assertFalse(subdirectory.isSelected(), "Should be deselected after setSelected(false)");
        
        // Test re-selection
        subdirectory.setSelected(true);
        assertTrue(subdirectory.isSelected(), "Should be selected after setSelected(true)");
        
        // Verify other properties remain unchanged during selection changes
        assertEquals("Subdirectory", subdirectory.getStatus(), "Status should remain unchanged");
        assertEquals("/Users/music", subdirectory.getOriginalRootPath(), "Original root should remain unchanged");
    }
    
    @Test
    @DisplayName("Test directory hierarchy relationships")
    void testDirectoryHierarchyRelationships() {
        String originalPath = "/Users/music";
        DirectoryItem originalDir = new DirectoryItem(originalPath);
        
        // Create subdirectories at different levels
        DirectoryItem level1 = new DirectoryItem("/Users/music/Jazz", originalPath);
        DirectoryItem level2 = new DirectoryItem("/Users/music/Rock/Classic", originalPath);
        DirectoryItem level3 = new DirectoryItem("/Users/music/Artists/Beatles/Albums", originalPath);
        
        // Verify all point to the same original root regardless of depth
        assertEquals(originalPath, level1.getOriginalRootPath(), "Level 1 should reference original");
        assertEquals(originalPath, level2.getOriginalRootPath(), "Level 2 should reference original");
        assertEquals(originalPath, level3.getOriginalRootPath(), "Level 3 should reference original");
        
        // Verify none are original directories
        assertFalse(level1.isOriginalDirectory(), "Level 1 should not be original");
        assertFalse(level2.isOriginalDirectory(), "Level 2 should not be original");
        assertFalse(level3.isOriginalDirectory(), "Level 3 should not be original");
        
        // Verify original directory is different
        assertTrue(originalDir.isOriginalDirectory(), "Original should be original directory");
    }
}