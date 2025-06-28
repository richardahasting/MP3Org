package org.hasting.util;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Manages profile change notifications to UI components.
 * This is a singleton that allows UI components to register for profile change events.
 */
public class ProfileChangeNotifier {
    
    private static ProfileChangeNotifier instance;
    private final List<ProfileChangeListener> listeners;
    
    private ProfileChangeNotifier() {
        listeners = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized ProfileChangeNotifier getInstance() {
        if (instance == null) {
            instance = new ProfileChangeNotifier();
        }
        return instance;
    }
    
    /**
     * Registers a listener for profile change events.
     */
    public void addListener(ProfileChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println("Registered profile change listener: " + listener.getClass().getSimpleName());
        }
    }
    
    /**
     * Unregisters a listener.
     */
    public void removeListener(ProfileChangeListener listener) {
        if (listener != null) {
            boolean removed = listeners.remove(listener);
            if (removed) {
                System.out.println("Unregistered profile change listener: " + listener.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * Notifies all listeners that the active profile has changed.
     */
    public void notifyProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile) {
        System.out.println("Notifying " + listeners.size() + " listeners of profile change: " + 
                          oldProfileId + " -> " + newProfileId);
        
        for (ProfileChangeListener listener : listeners) {
            try {
                listener.onProfileChanged(oldProfileId, newProfileId, newProfile);
            } catch (Exception e) {
                System.err.println("Error notifying profile change listener " + 
                                 listener.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Notifies all listeners that the database location has changed.
     */
    public void notifyDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase) {
        System.out.println("Notifying " + listeners.size() + " listeners of database change: " + 
                          oldDatabasePath + " -> " + newDatabasePath + " (new: " + isNewDatabase + ")");
        
        for (ProfileChangeListener listener : listeners) {
            try {
                listener.onDatabaseChanged(oldDatabasePath, newDatabasePath, isNewDatabase);
            } catch (Exception e) {
                System.err.println("Error notifying database change listener " + 
                                 listener.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Gets the number of registered listeners.
     */
    public int getListenerCount() {
        return listeners.size();
    }
    
    /**
     * Clears all listeners (useful for testing).
     */
    public void clearAllListeners() {
        listeners.clear();
        System.out.println("Cleared all profile change listeners");
    }
}