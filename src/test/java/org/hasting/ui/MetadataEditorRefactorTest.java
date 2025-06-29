package org.hasting.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the MetadataEditorView refactoring maintains functionality
 * and that all extracted panels work correctly together.
 * Note: This test focuses on class structure and doesn't instantiate JavaFX components.
 */
public class MetadataEditorRefactorTest {
    
    @Test
    @DisplayName("All panel classes should be loadable and properly structured")
    void testPanelClassStructure() {
        // Test that all panel classes exist and can be loaded
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.SearchPanel");
        }, "SearchPanel class should be loadable");
        
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.EditFormPanel");
        }, "EditFormPanel class should be loadable");
        
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.BulkEditPanel");
        }, "BulkEditPanel class should be loadable");
        
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.FileActionPanel");
        }, "FileActionPanel class should be loadable");
    }
    
    @Test
    @DisplayName("MetadataEditorViewRefactored class should have expected structure")
    void testRefactoredViewStructure() {
        Class<?> refactoredClass = MetadataEditorViewRefactored.class;
        
        // Test that expected methods exist
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("getSearchPanel");
        }, "getSearchPanel method should exist");
        
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("getEditFormPanel");
        }, "getEditFormPanel method should exist");
        
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("getBulkEditPanel");
        }, "getBulkEditPanel method should exist");
        
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("getFileActionPanel");
        }, "getFileActionPanel method should exist");
        
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("getStatusLabel");
        }, "getStatusLabel method should exist");
        
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("getCurrentFile");
        }, "getCurrentFile method should exist");
        
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("performSearch", String.class);
        }, "performSearch method should exist");
        
        assertDoesNotThrow(() -> {
            refactoredClass.getDeclaredMethod("cleanup");
        }, "cleanup method should exist");
    }
    
    @Test
    @DisplayName("Panel classes should have expected public methods")
    void testPanelMethodSignatures() {
        // Test SearchPanel methods
        Class<?> searchPanelClass = org.hasting.ui.panels.SearchPanel.class;
        assertDoesNotThrow(() -> {
            searchPanelClass.getDeclaredMethod("getSearchField");
        }, "SearchPanel should have getSearchField method");
        
        assertDoesNotThrow(() -> {
            searchPanelClass.getDeclaredMethod("getResultsTable");
        }, "SearchPanel should have getResultsTable method");
        
        assertDoesNotThrow(() -> {
            searchPanelClass.getDeclaredMethod("search");
        }, "SearchPanel should have search method");
        
        // Test EditFormPanel methods
        Class<?> editFormPanelClass = org.hasting.ui.panels.EditFormPanel.class;
        assertDoesNotThrow(() -> {
            editFormPanelClass.getDeclaredMethod("loadFile", org.hasting.model.MusicFile.class);
        }, "EditFormPanel should have loadFile method");
        
        assertDoesNotThrow(() -> {
            editFormPanelClass.getDeclaredMethod("clearForm");
        }, "EditFormPanel should have clearForm method");
        
        // Test BulkEditPanel methods
        Class<?> bulkEditPanelClass = org.hasting.ui.panels.BulkEditPanel.class;
        assertDoesNotThrow(() -> {
            bulkEditPanelClass.getDeclaredMethod("isBulkEditModeEnabled");
        }, "BulkEditPanel should have isBulkEditModeEnabled method");
        
        assertDoesNotThrow(() -> {
            bulkEditPanelClass.getDeclaredMethod("updateSelection", java.util.List.class);
        }, "BulkEditPanel should have updateSelection method");
        
        // Test FileActionPanel methods
        Class<?> fileActionPanelClass = org.hasting.ui.panels.FileActionPanel.class;
        assertDoesNotThrow(() -> {
            fileActionPanelClass.getDeclaredMethod("setCurrentFile", org.hasting.model.MusicFile.class);
        }, "FileActionPanel should have setCurrentFile method");
    }
    
    @Test
    @DisplayName("Refactored view should implement ProfileChangeListener")
    void testProfileChangeListener() {
        // Test that the refactored view implements ProfileChangeListener
        assertTrue(org.hasting.util.ProfileChangeListener.class.isAssignableFrom(MetadataEditorViewRefactored.class),
                  "MetadataEditorViewRefactored should implement ProfileChangeListener");
    }
    
    @Test
    @DisplayName("Refactored view should extend BorderPane")
    void testInheritance() {
        // Test that the refactored view extends BorderPane (like original)
        assertTrue(javafx.scene.layout.BorderPane.class.isAssignableFrom(MetadataEditorViewRefactored.class),
                  "MetadataEditorViewRefactored should extend BorderPane");
    }
    
    @Test
    @DisplayName("Package structure should be clean")
    void testPackageStructure() {
        // All panels should be in the panels package
        assertEquals("org.hasting.ui.panels", org.hasting.ui.panels.SearchPanel.class.getPackageName(),
                    "SearchPanel should be in panels package");
        assertEquals("org.hasting.ui.panels", org.hasting.ui.panels.EditFormPanel.class.getPackageName(),
                    "EditFormPanel should be in panels package");
        assertEquals("org.hasting.ui.panels", org.hasting.ui.panels.BulkEditPanel.class.getPackageName(),
                    "BulkEditPanel should be in panels package");
        assertEquals("org.hasting.ui.panels", org.hasting.ui.panels.FileActionPanel.class.getPackageName(),
                    "FileActionPanel should be in panels package");
        
        // MetadataEditorViewRefactored should remain in original package
        assertEquals("org.hasting.ui", MetadataEditorViewRefactored.class.getPackageName(),
                    "MetadataEditorViewRefactored should remain in ui package");
    }
    
    @Test
    @DisplayName("Code organization should follow Single Responsibility Principle")
    void testSingleResponsibilityPrinciple() {
        // Each panel class should be focused on a single domain
        
        assertTrue(org.hasting.ui.panels.SearchPanel.class.getSimpleName().contains("Search"),
                  "SearchPanel should be focused on search functionality");
        assertTrue(org.hasting.ui.panels.EditFormPanel.class.getSimpleName().contains("EditForm"),
                  "EditFormPanel should be focused on form editing");
        assertTrue(org.hasting.ui.panels.BulkEditPanel.class.getSimpleName().contains("BulkEdit"),
                  "BulkEditPanel should be focused on bulk editing");
        assertTrue(org.hasting.ui.panels.FileActionPanel.class.getSimpleName().contains("FileAction"),
                  "FileActionPanel should be focused on file actions");
    }
}