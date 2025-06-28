package org.hasting.util;

import org.hasting.model.PathTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages path templates for file organization.
 */
public class PathTemplateManager {
    
    private static final String PREF_CURRENT_TEMPLATE = "current_template";
    private static final String PREF_TEXT_FORMAT = "text_format";
    private static final String PREF_USE_SUBDIRS = "use_subdirectories";
    private static final String PREF_SUBDIR_LEVELS = "subdirectory_levels";
    private static final String PREF_CUSTOM_TEMPLATES = "custom_templates";
    
    private static PathTemplateManager instance;
    private Preferences prefs;
    private PathTemplate currentTemplate;
    private List<PathTemplate> predefinedTemplates;
    
    private PathTemplateManager() {
        prefs = Preferences.userNodeForPackage(PathTemplateManager.class);
        initializePredefinedTemplates();
        loadCurrentTemplate();
    }
    
    public static synchronized PathTemplateManager getInstance() {
        if (instance == null) {
            instance = new PathTemplateManager();
        }
        return instance;
    }
    
    private void initializePredefinedTemplates() {
        predefinedTemplates = new ArrayList<>();
        
        // Template 1: Current MP3Org format (default)
        predefinedTemplates.add(new PathTemplate(
            "{subdirectory}/{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            true,
            7
        ));
        
        // Template 2: Simple artist/album structure
        predefinedTemplates.add(new PathTemplate(
            "{artist}/{album}/{track_number:02d} {title}.{file_type}",
            PathTemplate.TextFormat.NONE,
            false,
            0
        ));
        
        // Template 3: Genre-based organization
        predefinedTemplates.add(new PathTemplate(
            "{genre}/{artist}/{year} - {album}/{track_number:02d} - {title}.{file_type}",
            PathTemplate.TextFormat.DASH,
            false,
            0
        ));
        
        // Template 4: Flat artist organization
        predefinedTemplates.add(new PathTemplate(
            "{artist}/{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            false,
            0
        ));
        
        // Template 5: Year-based organization
        predefinedTemplates.add(new PathTemplate(
            "{year}/{artist} - {album}/{title}.{file_type}",
            PathTemplate.TextFormat.DASH,
            false,
            0
        ));
    }
    
    /**
     * Loads the current template from preferences.
     */
    private void loadCurrentTemplate() {
        String templateStr = prefs.get(PREF_CURRENT_TEMPLATE, PathTemplate.DEFAULT_TEMPLATE);
        String formatStr = prefs.get(PREF_TEXT_FORMAT, PathTemplate.TextFormat.UNDERSCORE.name());
        boolean useSubdirs = prefs.getBoolean(PREF_USE_SUBDIRS, true);
        int subdirLevels = prefs.getInt(PREF_SUBDIR_LEVELS, 7);
        
        try {
            PathTemplate.TextFormat format = PathTemplate.TextFormat.valueOf(formatStr);
            currentTemplate = new PathTemplate(templateStr, format, useSubdirs, subdirLevels);
        } catch (IllegalArgumentException e) {
            // Fall back to default if invalid format
            currentTemplate = new PathTemplate();
        }
    }
    
    /**
     * Saves the current template to preferences.
     */
    public void saveCurrentTemplate() {
        if (currentTemplate != null) {
            prefs.put(PREF_CURRENT_TEMPLATE, currentTemplate.getTemplate());
            prefs.put(PREF_TEXT_FORMAT, currentTemplate.getTextFormat().name());
            prefs.putBoolean(PREF_USE_SUBDIRS, currentTemplate.isUseSubdirectoryGrouping());
            prefs.putInt(PREF_SUBDIR_LEVELS, currentTemplate.getSubdirectoryLevels());
        }
    }
    
    /**
     * Gets the current path template.
     */
    public PathTemplate getCurrentTemplate() {
        return currentTemplate;
    }
    
    /**
     * Sets the current path template.
     */
    public void setCurrentTemplate(PathTemplate template) {
        this.currentTemplate = template;
        saveCurrentTemplate();
    }
    
    /**
     * Gets the list of predefined templates.
     */
    public List<PathTemplate> getPredefinedTemplates() {
        return new ArrayList<>(predefinedTemplates);
    }
    
    /**
     * Gets template by index from predefined list.
     */
    public PathTemplate getPredefinedTemplate(int index) {
        if (index >= 0 && index < predefinedTemplates.size()) {
            return predefinedTemplates.get(index);
        }
        return new PathTemplate(); // Return default if index is invalid
    }
    
    /**
     * Gets descriptions for predefined templates.
     */
    public String[] getPredefinedTemplateDescriptions() {
        return new String[] {
            "MP3Org Default: Grouped directories with artist/album structure",
            "Simple: Artist/Album with track numbers",
            "Genre-based: Organized by music genre",
            "Flat Artist: Simple artist folders",
            "Year-based: Organized by release year"
        };
    }
    
    /**
     * Validates a template string.
     */
    public boolean isValidTemplate(String template) {
        if (template == null || template.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - ensure it contains at least one field and file extension
        boolean hasField = template.contains("{") && template.contains("}");
        boolean hasExtension = template.contains("{file_type}") || template.contains(".mp3");
        
        return hasField && hasExtension;
    }
    
    /**
     * Gets a preview of how a template would format a sample file.
     */
    public String getTemplatePreview(PathTemplate template) {
        // Create a sample music file for preview
        org.hasting.model.MusicFile sampleFile = new org.hasting.model.MusicFile();
        sampleFile.setArtist("Sample Artist");
        sampleFile.setAlbum("Sample Album");
        sampleFile.setTitle("Sample Song Title");
        sampleFile.setGenre("Rock");
        sampleFile.setYear(2023);
        sampleFile.setTrackNumber(5);
        sampleFile.setFileType("mp3");
        
        try {
            return template.generatePath("/music", sampleFile);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Resets to default template.
     */
    public void resetToDefault() {
        currentTemplate = new PathTemplate();
        saveCurrentTemplate();
    }
}