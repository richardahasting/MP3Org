package org.hasting.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ImportView class.
 * 
 * <p>These tests verify the basic functionality of the ImportView component
 * including initialization, component structure, and basic behavior.
 * 
 * @since 2.0
 */
@DisplayName("Import View Tests")
public class ImportViewTest {
    
    private ImportView importView;
    
    @BeforeEach
    void setUp() {
        // Skip JavaFX initialization for unit tests
        // Tests focus on non-UI logic and structure
    }
    
    @Test
    @DisplayName("DirectoryItem constructor initializes correctly")
    void testDirectoryItemConstructor() {
        String testPath = "/test/path";
        ImportView.DirectoryItem item = new ImportView.DirectoryItem(testPath);
        
        assertEquals(testPath, item.getPath());
        assertEquals("Ready", item.getStatus());
        assertEquals("Never", item.getLastScanned());
        assertTrue(item.isOriginalDirectory());
        assertEquals(testPath, item.getOriginalRootPath());
        assertFalse(item.isSelected());
    }
    
    @Test
    @DisplayName("DirectoryItem subdirectory constructor initializes correctly")
    void testDirectoryItemSubdirectoryConstructor() {
        String subPath = "/test/path/sub";
        String rootPath = "/test/path";
        ImportView.DirectoryItem item = new ImportView.DirectoryItem(subPath, rootPath);
        
        assertEquals(subPath, item.getPath());
        assertEquals("Subdirectory", item.getStatus());
        assertEquals("Never", item.getLastScanned());
        assertFalse(item.isOriginalDirectory());
        assertEquals(rootPath, item.getOriginalRootPath());
        assertTrue(item.isSelected()); // Subdirectories auto-selected
    }
    
    @Test
    @DisplayName("DirectoryItem selection state can be modified")
    void testDirectoryItemSelection() {
        ImportView.DirectoryItem item = new ImportView.DirectoryItem("/test");
        
        assertFalse(item.isSelected());
        
        item.setSelected(true);
        assertTrue(item.isSelected());
        
        item.setSelected(false);
        assertFalse(item.isSelected());
    }
    
    @Test
    @DisplayName("DirectoryItem status can be updated")
    void testDirectoryItemStatusUpdate() {
        ImportView.DirectoryItem item = new ImportView.DirectoryItem("/test");
        
        assertEquals("Ready", item.getStatus());
        
        item.setStatus("Scanning...");
        assertEquals("Scanning...", item.getStatus());
        
        item.setStatus("Completed");
        assertEquals("Completed", item.getStatus());
    }
    
    @Test
    @DisplayName("DirectoryItem last scanned can be updated")
    void testDirectoryItemLastScannedUpdate() {
        ImportView.DirectoryItem item = new ImportView.DirectoryItem("/test");
        
        assertEquals("Never", item.getLastScanned());
        
        item.setLastScanned("2025-07-15 10:30:00");
        assertEquals("2025-07-15 10:30:00", item.getLastScanned());
    }
}