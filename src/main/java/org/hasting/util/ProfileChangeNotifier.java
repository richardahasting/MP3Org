package org.hasting.util;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

/**
 * Manages profile change notifications to UI components.
 * This is a singleton that allows UI components to register for profile change events.
 */
public class ProfileChangeNotifier {
    
    private static final Logger logger = Log4Rich.getLogger(ProfileChangeNotifier.class);
    
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
            logger.debug(String.format("Registered profile change listener: %s", listener.getClass().getSimpleName()));
        }
    }
    
    /**
     * Unregisters a listener.
     */
    public void removeListener(ProfileChangeListener listener) {
        if (listener != null) {
            boolean removed = listeners.remove(listener);
            if (removed) {
                logger.debug(String.format("Unregistered profile change listener: %s", listener.getClass().getSimpleName()));
            }
        }
    }
    
    /**
     * Notifies all listeners that the active profile has changed.
     */
    public void notifyProfileChanged(String oldProfileId, String newProfileId, DatabaseProfile newProfile) {
        logger.info(String.format("Notifying {} listeners of profile change: {} -> {}", listeners.size()), oldProfileId, newProfileId);
        
        for (ProfileChangeListener listener : listeners) {
            try {
                listener.onProfileChanged(oldProfileId, newProfileId, newProfile);
            } catch (Exception e) {
                logger.error(String.format("Error notifying profile change listener %s: %s", listener.getClass().getSimpleName(), e.getMessage()), e);
            }
        }
    }
    
    /**
     * Notifies all listeners that the database location has changed.
     */
    public void notifyDatabaseChanged(String oldDatabasePath, String newDatabasePath, boolean isNewDatabase) {
        logger.info(String.format("Notifying {} listeners of database change: {} -> {} (new: {})", listeners.size()), oldDatabasePath, newDatabasePath, isNewDatabase);
        
        for (ProfileChangeListener listener : listeners) {
            try {
                listener.onDatabaseChanged(oldDatabasePath, newDatabasePath, isNewDatabase);
            } catch (Exception e) {
                logger.error(String.format("Error notifying database change listener %s: %s", listener.getClass().getSimpleName(), e.getMessage()), e);
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
        logger.debug("Cleared all profile change listeners");
    }
}