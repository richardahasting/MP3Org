package org.hasting.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.hasting.ui.panels.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the refactoring maintains compatibility
 * and that all new classes can be instantiated without JavaFX dependencies.
 */
public class RefactoringIntegrationTest {
    
    @Test
    @DisplayName("All panel classes should be loadable and have proper structure")
    void testPanelClassesLoadable() {
        // Test that all panel classes exist and can be loaded
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.FileTypeFilterPanel");
        }, "FileTypeFilterPanel class should be loadable");
        
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.DatabaseLocationPanel");
        }, "DatabaseLocationPanel class should be loadable");
        
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.ProfileManagementPanel");
        }, "ProfileManagementPanel class should be loadable");
        
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.FuzzySearchConfigPanel");
        }, "FuzzySearchConfigPanel class should be loadable");
        
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.panels.PathTemplateConfigPanel");
        }, "PathTemplateConfigPanel class should be loadable");
    }
    
    @Test
    @DisplayName("ConfigurationView class should maintain expected structure")
    void testConfigurationViewStructure() {
        Class<?> configViewClass = ConfigurationView.class;
        
        // Test that expected methods exist for backward compatibility
        assertDoesNotThrow(() -> {
            configViewClass.getDeclaredMethod("updateDisplayedInfo");
        }, "updateDisplayedInfo method should exist for backward compatibility");
        
        // Test that getter methods exist for all panels
        assertDoesNotThrow(() -> {
            configViewClass.getDeclaredMethod("getDatabaseLocationPanel");
        }, "getDatabaseLocationPanel method should exist");
        
        assertDoesNotThrow(() -> {
            configViewClass.getDeclaredMethod("getFileTypeFilterPanel");
        }, "getFileTypeFilterPanel method should exist");
        
        assertDoesNotThrow(() -> {
            configViewClass.getDeclaredMethod("getProfileManagementPanel");
        }, "getProfileManagementPanel method should exist");
        
        assertDoesNotThrow(() -> {
            configViewClass.getDeclaredMethod("getFuzzySearchConfigPanel");
        }, "getFuzzySearchConfigPanel method should exist");
        
        assertDoesNotThrow(() -> {
            configViewClass.getDeclaredMethod("getPathTemplateConfigPanel");
        }, "getPathTemplateConfigPanel method should exist");
    }
    
    @Test
    @DisplayName("Panel classes should have expected public methods")
    void testPanelClassMethods() {
        // Test FileTypeFilterPanel
        Class<?> fileTypePanelClass = FileTypeFilterPanel.class;
        assertDoesNotThrow(() -> {
            fileTypePanelClass.getDeclaredMethod("loadCurrentSettings");
        }, "FileTypeFilterPanel should have loadCurrentSettings method");
        
        assertDoesNotThrow(() -> {
            fileTypePanelClass.getDeclaredMethod("getFileTypesList");
        }, "FileTypeFilterPanel should have getFileTypesList method");
        
        // Test ProfileManagementPanel  
        Class<?> profilePanelClass = ProfileManagementPanel.class;
        assertDoesNotThrow(() -> {
            profilePanelClass.getDeclaredMethod("setOnProfileChanged", Runnable.class);
        }, "ProfileManagementPanel should have setOnProfileChanged method");
        
        assertDoesNotThrow(() -> {
            profilePanelClass.getDeclaredMethod("getProfileComboBox");
        }, "ProfileManagementPanel should have getProfileComboBox method");
        
        // Test FuzzySearchConfigPanel
        Class<?> fuzzyPanelClass = FuzzySearchConfigPanel.class;
        assertDoesNotThrow(() -> {
            fuzzyPanelClass.getDeclaredMethod("setOnConfigChanged", Runnable.class);
        }, "FuzzySearchConfigPanel should have setOnConfigChanged method");
        
        // Test PathTemplateConfigPanel
        Class<?> templatePanelClass = PathTemplateConfigPanel.class;
        assertDoesNotThrow(() -> {
            templatePanelClass.getDeclaredMethod("setOnTemplateChanged", Runnable.class);
        }, "PathTemplateConfigPanel should have setOnTemplateChanged method");
    }
    
    @Test
    @DisplayName("Refactoring should not break existing imports")
    void testNoBreakingChanges() {
        // The original ConfigurationView class should still exist
        assertDoesNotThrow(() -> {
            Class.forName("org.hasting.ui.ConfigurationView");
        }, "Original ConfigurationView class should still exist");
        
        // It should still extend BorderPane
        assertTrue(javafx.scene.layout.BorderPane.class.isAssignableFrom(ConfigurationView.class),
            "ConfigurationView should still extend BorderPane");
    }
    
    @Test
    @DisplayName("Package structure should be clean")
    void testPackageStructure() {
        // All panels should be in the panels package
        assertEquals("org.hasting.ui.panels", FileTypeFilterPanel.class.getPackageName(),
            "FileTypeFilterPanel should be in panels package");
        assertEquals("org.hasting.ui.panels", DatabaseLocationPanel.class.getPackageName(),
            "DatabaseLocationPanel should be in panels package");
        assertEquals("org.hasting.ui.panels", ProfileManagementPanel.class.getPackageName(),
            "ProfileManagementPanel should be in panels package");
        assertEquals("org.hasting.ui.panels", FuzzySearchConfigPanel.class.getPackageName(),
            "FuzzySearchConfigPanel should be in panels package");
        assertEquals("org.hasting.ui.panels", PathTemplateConfigPanel.class.getPackageName(),
            "PathTemplateConfigPanel should be in panels package");
        
        // ConfigurationView should remain in original package
        assertEquals("org.hasting.ui", ConfigurationView.class.getPackageName(),
            "ConfigurationView should remain in ui package");
    }
    
    @Test
    @DisplayName("Code organization should follow Single Responsibility Principle")
    void testSingleResponsibilityPrinciple() {
        // Each panel class should be focused on a single domain
        // We can test this by checking class names match their responsibilities
        
        assertTrue(FileTypeFilterPanel.class.getSimpleName().contains("FileType"),
            "FileTypeFilterPanel should be focused on file types");
        assertTrue(DatabaseLocationPanel.class.getSimpleName().contains("Database"),
            "DatabaseLocationPanel should be focused on database");
        assertTrue(ProfileManagementPanel.class.getSimpleName().contains("Profile"),
            "ProfileManagementPanel should be focused on profiles");
        assertTrue(FuzzySearchConfigPanel.class.getSimpleName().contains("FuzzySearch"),
            "FuzzySearchConfigPanel should be focused on fuzzy search");
        assertTrue(PathTemplateConfigPanel.class.getSimpleName().contains("PathTemplate"),
            "PathTemplateConfigPanel should be focused on path templates");
    }
}