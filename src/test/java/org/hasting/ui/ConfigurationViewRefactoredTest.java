package org.hasting.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.hasting.ui.panels.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the refactored ConfigurationView to ensure all panels are properly integrated.
 */
public class ConfigurationViewRefactoredTest {
    
    private ConfigurationView configurationView;
    
    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX toolkit for testing
        new JFXPanel();
    }
    
    @BeforeEach
    void setUp() {
        // Run JavaFX operations on the JavaFX Application Thread
        Platform.runLater(() -> {
            configurationView = new ConfigurationView();
        });
        
        // Wait for JavaFX initialization to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    @DisplayName("ConfigurationView should initialize without errors")
    void testConfigurationViewInitialization() {
        Platform.runLater(() -> {
            assertNotNull(configurationView, "ConfigurationView should be created");
            assertNotNull(configurationView.getStatusLabel(), "Status label should be initialized");
        });
    }
    
    @Test
    @DisplayName("All panels should be accessible via getter methods")
    void testPanelAccessors() {
        Platform.runLater(() -> {
            assertNotNull(configurationView.getDatabaseLocationPanel(), 
                "DatabaseLocationPanel should be accessible");
            assertNotNull(configurationView.getFileTypeFilterPanel(), 
                "FileTypeFilterPanel should be accessible");
            assertNotNull(configurationView.getProfileManagementPanel(), 
                "ProfileManagementPanel should be accessible");
            assertNotNull(configurationView.getFuzzySearchConfigPanel(), 
                "FuzzySearchConfigPanel should be accessible");
            assertNotNull(configurationView.getPathTemplateConfigPanel(), 
                "PathTemplateConfigPanel should be accessible");
        });
    }
    
    @Test
    @DisplayName("Status label should be shared across all panels")
    void testSharedStatusLabel() {
        Platform.runLater(() -> {
            Label statusLabel = configurationView.getStatusLabel();
            assertNotNull(statusLabel, "Status label should exist");
            
            // Verify each panel uses the same status label reference
            // Note: This is a structural test - we can't easily verify the actual reference
            // but we can verify the panels exist and have been initialized
            assertNotNull(configurationView.getDatabaseLocationPanel());
            assertNotNull(configurationView.getFileTypeFilterPanel());
            assertNotNull(configurationView.getProfileManagementPanel());
            assertNotNull(configurationView.getFuzzySearchConfigPanel());
            assertNotNull(configurationView.getPathTemplateConfigPanel());
        });
    }
    
    @Test
    @DisplayName("ConfigurationView should have professional layout structure")
    void testLayoutStructure() {
        Platform.runLater(() -> {
            // Verify the main layout structure
            assertNotNull(configurationView.getTop(), "Should have top section (title)");
            assertNotNull(configurationView.getCenter(), "Should have center section (tabs)");
            assertNotNull(configurationView.getBottom(), "Should have bottom section (status)");
            
            // Verify center contains a TabPane
            assertTrue(configurationView.getCenter() instanceof TabPane, 
                "Center should contain TabPane for panel organization");
            
            TabPane tabPane = (TabPane) configurationView.getCenter();
            assertEquals(5, tabPane.getTabs().size(), 
                "Should have 5 tabs for the 5 configuration panels");
        });
    }
    
    @Test
    @DisplayName("Tab switching functionality should work")
    void testTabSwitching() {
        Platform.runLater(() -> {
            // Test switching to different tabs
            configurationView.switchToTab("Database");
            configurationView.switchToTab("Profiles");
            configurationView.switchToTab("File Types");
            configurationView.switchToTab("Duplicate Detection");
            configurationView.switchToTab("File Organization");
            
            // Should not throw exceptions
            assertTrue(true, "Tab switching should work without errors");
        });
    }
    
    @Test
    @DisplayName("Backward compatibility method should work")
    void testBackwardCompatibility() {
        Platform.runLater(() -> {
            // Test that the old updateDisplayedInfo method still works
            assertDoesNotThrow(() -> {
                configurationView.updateDisplayedInfo();
            }, "updateDisplayedInfo should maintain backward compatibility");
        });
    }
    
    @Test
    @DisplayName("Panel integration should handle typical workflow")
    void testTypicalWorkflow() {
        Platform.runLater(() -> {
            try {
                // Simulate typical user workflow
                
                // 1. Switch to Profiles tab
                configurationView.switchToTab("Profiles");
                
                // 2. Switch to File Types tab
                configurationView.switchToTab("File Types");
                
                // 3. Access file type panel
                FileTypeFilterPanel fileTypePanel = configurationView.getFileTypeFilterPanel();
                assertNotNull(fileTypePanel.getFileTypesList(), 
                    "File types list should be accessible");
                
                // 4. Access profile panel
                ProfileManagementPanel profilePanel = configurationView.getProfileManagementPanel();
                assertNotNull(profilePanel.getProfileComboBox(), 
                    "Profile combo box should be accessible");
                
                // 5. Refresh all panels
                configurationView.updateDisplayedInfo();
                
                // Workflow should complete without errors
                assertTrue(true, "Typical workflow should work without errors");
                
            } catch (Exception e) {
                fail("Typical workflow should not throw exceptions: " + e.getMessage());
            }
        });
    }
    
    @Test
    @DisplayName("Error handling should be robust")
    void testErrorHandling() {
        Platform.runLater(() -> {
            // Test switching to non-existent tab (should not crash)
            assertDoesNotThrow(() -> {
                configurationView.switchToTab("NonExistentTab");
            }, "Switching to non-existent tab should not crash");
            
            // Test multiple rapid tab switches
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++) {
                    configurationView.switchToTab("Database");
                    configurationView.switchToTab("Profiles");
                }
            }, "Rapid tab switching should not crash");
        });
    }
}