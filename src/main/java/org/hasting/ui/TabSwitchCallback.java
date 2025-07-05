package org.hasting.ui;

/**
 * Callback interface for requesting tab switches in the main application.
 * 
 * <p>This interface allows views and panels to request that the main application
 * switch to a specific tab without having direct access to the main TabPane.
 * This maintains proper separation of concerns and loose coupling between 
 * application components.
 * 
 * @since 1.0
 */
@FunctionalInterface
public interface TabSwitchCallback {
    
    /**
     * Requests that the main application switch to the specified tab.
     * 
     * @param tabName The name/title of the tab to switch to (e.g., "Import & Organize")
     * @return true if the tab switch was successful, false if the tab was not found
     */
    boolean switchToTab(String tabName);
}