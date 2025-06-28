package org.hasting.util;

/**
 * Interface for components that need to be notified when database profiles change.
 */
public interface ProfileChangeListener {
    
    /**
     * Called when the active profile is changed.
     * 
     * @param oldProfileId The ID of the previous active profile (may be null)
     * @param newProfileId The ID of the new active profile
     * @param newProfile The new active profile
     */
    void onProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile);
    
    /**
     * Called when a database location changes (either through profile switch or direct change).
     * This indicates that all cached data should be cleared and reloaded.
     * 
     * @param oldDatabasePath The previous database path (may be null)
     * @param newDatabasePath The new database path
     * @param isNewDatabase True if this is a completely new/empty database
     */
    void onDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase);
}