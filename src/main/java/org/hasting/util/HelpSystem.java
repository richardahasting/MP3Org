package org.hasting.util;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized help system for providing tooltips and context-sensitive help.
 */
public class HelpSystem {
    
    private static final Map<String, String> tooltips = new HashMap<>();
    private static final Map<String, String> helpContent = new HashMap<>();
    
    static {
        initializeTooltips();
        initializeHelpContent();
    }
    
    /**
     * Initialize all tooltip texts.
     */
    private static void initializeTooltips() {
        // Configuration View - Database Management
        tooltips.put("config.change.location", "Select a new location for the MP3Org database. This will create a new database at the chosen location.");
        tooltips.put("config.open.location", "Open the current database folder in your system's file manager.");
        tooltips.put("config.reload.config", "Reload configuration settings from files and environment variables.");
        tooltips.put("config.current.path", "The current database location. This path is determined by system properties, environment variables, or configuration files.");
        
        // Configuration View - Database Profiles
        tooltips.put("config.profile.combo", "Select and switch between different database profiles. Each profile can have its own database location and settings.");
        tooltips.put("config.profile.new", "Create a new database profile with a custom name and location.");
        tooltips.put("config.profile.duplicate", "Create a copy of the currently active profile with identical settings.");
        tooltips.put("config.profile.rename", "Change the name of the currently active profile.");
        tooltips.put("config.profile.delete", "Remove the selected profile and delete its database files from disk. You cannot delete the last remaining profile.");
        tooltips.put("config.profile.name", "Enter a descriptive name for this database profile.");
        tooltips.put("config.profile.description", "Optional description to help identify this profile's purpose or contents.");
        
        // Configuration View - File Type Filters
        tooltips.put("config.filetypes.list", "Select which audio file formats to include when scanning directories. Unchecked formats will be ignored.");
        tooltips.put("config.filetypes.selectall", "Enable all supported audio file formats (MP3, FLAC, M4A, AAC, WAV, OGG, WMA, AIFF, APE, OPUS).");
        tooltips.put("config.filetypes.selectnone", "Disable all file formats. Note: At least one format must be selected.");
        tooltips.put("config.filetypes.apply", "Save the selected file type preferences. This affects future directory scans and searches.");
        
        // Configuration View - Fuzzy Search Configuration
        tooltips.put("config.fuzzy.preset", "Choose a predefined similarity detection profile: Strict (fewer matches), Balanced (recommended), Lenient (more matches), or Custom.");
        tooltips.put("config.fuzzy.title.similarity", "How similar song titles must be to consider them duplicates (0-100%). Higher values require closer matches.");
        tooltips.put("config.fuzzy.artist.similarity", "How similar artist names must be to consider songs duplicates (0-100%). Higher values require closer matches.");
        tooltips.put("config.fuzzy.album.similarity", "How similar album names must be to consider songs duplicates (0-100%). Higher values require closer matches.");
        tooltips.put("config.fuzzy.duration.tolerance", "Maximum difference in song duration (seconds) to still consider songs as potential duplicates.");
        tooltips.put("config.fuzzy.duration.percent", "Alternative duration tolerance as a percentage of the song length (±0-20%).");
        tooltips.put("config.fuzzy.ignore.case", "Ignore uppercase/lowercase differences when comparing text fields (e.g., 'Artist' matches 'artist').");
        tooltips.put("config.fuzzy.ignore.punctuation", "Ignore punctuation marks when comparing text (e.g., 'Rock n Roll' matches 'Rock 'n' Roll').");
        tooltips.put("config.fuzzy.track.match", "Require track numbers to match exactly for songs to be considered duplicates.");
        tooltips.put("config.fuzzy.ignore.prefixes", "Ignore common prefixes like 'The', 'A', 'An' when comparing artist names.");
        tooltips.put("config.fuzzy.ignore.featuring", "Ignore featuring artist information when comparing (e.g., 'Song (feat. Artist)' matches 'Song').");
        tooltips.put("config.fuzzy.ignore.editions", "Ignore edition information like 'Deluxe', 'Remastered', 'Extended' when comparing albums.");
        tooltips.put("config.fuzzy.min.fields", "Minimum number of metadata fields (out of 4: title, artist, album, duration) that must match for duplicate detection.");
        tooltips.put("config.fuzzy.apply", "Save the current duplicate detection settings to the active profile.");
        tooltips.put("config.fuzzy.reset", "Restore all duplicate detection settings to the default balanced configuration.");
        
        // Duplicate Manager View
        tooltips.put("duplicates.refresh", "Scan the database for potential duplicate songs using the current similarity settings.");
        tooltips.put("duplicates.delete.selected", "Permanently delete the selected song file from disk and remove it from the database.");
        tooltips.put("duplicates.keep.better", "Compare selected songs and automatically delete the one with lower audio quality (bitrate).");
        tooltips.put("duplicates.potential.table", "Songs that might be duplicates based on similarity analysis. Select a song to see similar matches.");
        tooltips.put("duplicates.similar.table", "Songs similar to the selected potential duplicate. Use this to compare and decide which to keep or delete.");
        
        // Import & Organize View
        tooltips.put("import.add.directories", "Select folders containing music files to scan and add to the database. Subdirectories will be included automatically.");
        tooltips.put("import.directories.area", "Directories that will be scanned for music files. Files found will be added to the database for organization and duplicate detection.");
        tooltips.put("import.clear.database", "⚠️ Remove all music file records from the database. The actual music files on disk will NOT be deleted.");
        tooltips.put("import.destination.field", "Choose where to copy and organize your music files. A new folder structure will be created here.");
        tooltips.put("import.destination.browse", "Select the destination folder where organized music will be copied.");
        tooltips.put("import.organize.button", "Copy all database files to the destination folder, organized as Artist/Album/Track-Title.ext structure.");
        tooltips.put("import.progress.bar", "Shows the current progress of scanning or organizing operations.");
        tooltips.put("import.progress.label", "Details about the current file being processed during operations.");
        
        // Metadata Editor View
        tooltips.put("metadata.search.field", "Enter keywords to find songs in your database. Use partial words to find matches.");
        tooltips.put("metadata.search.type", "Choose which metadata field to search in: Title, Artist, Album, or All Fields for comprehensive search.");
        tooltips.put("metadata.search.button", "Execute the search using the entered keywords and selected field type.");
        tooltips.put("metadata.results.table", "Songs matching your search criteria. Select a song to edit its metadata information.");
        tooltips.put("metadata.title.field", "The song title. This will be used for duplicate detection and file organization.");
        tooltips.put("metadata.artist.field", "The performing artist or band name. Used for duplicate detection and organizing files by artist.");
        tooltips.put("metadata.album.field", "The album or collection name. Used for duplicate detection and organizing files by album.");
        tooltips.put("metadata.genre.field", "Musical genre classification (e.g., Rock, Pop, Jazz). Optional but helpful for organization.");
        tooltips.put("metadata.track.field", "Position of this song on the album. Enter numbers only (e.g., 1, 2, 3).");
        tooltips.put("metadata.year.field", "Release year of the song or album. Enter 4-digit year (e.g., 2023).");
        tooltips.put("metadata.save.button", "Save the edited metadata to the database. This updates the song information used for searches and duplicate detection.");
        tooltips.put("metadata.revert.button", "Discard any unsaved changes and restore the original metadata values.");
        tooltips.put("metadata.delete.button", "⚠️ Permanently delete this music file from disk and remove it from the database. This cannot be undone.");
        
        // Main Application Tabs
        tooltips.put("tab.duplicates", "Find and manage duplicate songs in your music collection.");
        tooltips.put("tab.metadata", "Search for songs and edit their information (title, artist, album, etc.).");
        tooltips.put("tab.import", "Add music files to the database and organize them into a structured folder system.");
        tooltips.put("tab.config", "Configure database location, file types, duplicate detection settings, and manage profiles.");
    }
    
    /**
     * Initialize comprehensive help content for dialogs.
     */
    private static void initializeHelpContent() {
        helpContent.put("config.fuzzy.help", 
            "Fuzzy Search Configuration Help\n\n" +
            "The fuzzy search system helps identify potential duplicate songs even when metadata differs slightly. " +
            "Here's how to configure it effectively:\n\n" +
            
            "SIMILARITY THRESHOLDS:\n" +
            "• Title Similarity (85%): How closely song titles must match. Lower values find more matches but may include false positives.\n" +
            "• Artist Similarity (90%): How closely artist names must match. Generally keep higher than title similarity.\n" +
            "• Album Similarity (85%): How closely album names must match. Can be lower if you have compilation albums.\n\n" +
            
            "DURATION MATCHING:\n" +
            "• Tolerance (Seconds): Maximum time difference allowed (default: 10 seconds)\n" +
            "• Tolerance (Percent): Alternative percentage-based tolerance (default: 5%)\n" +
            "• The system uses whichever tolerance is more permissive\n\n" +
            
            "TEXT NORMALIZATION OPTIONS:\n" +
            "• Ignore Case: Treats 'Artist' and 'artist' as identical\n" +
            "• Ignore Punctuation: Treats 'Rock n Roll' and 'Rock 'n' Roll' as identical\n" +
            "• Ignore Artist Prefixes: Treats 'Beatles' and 'The Beatles' as identical\n" +
            "• Ignore Featuring: Treats 'Song' and 'Song (feat. Artist)' as identical\n" +
            "• Ignore Album Editions: Treats 'Album' and 'Album - Deluxe Edition' as identical\n\n" +
            
            "MATCHING REQUIREMENTS:\n" +
            "• Track Number Match: Whether track numbers must be identical\n" +
            "• Minimum Fields: How many fields (title, artist, album, duration) must meet their thresholds\n\n" +
            
            "PRESETS:\n" +
            "• Strict: High thresholds, fewer matches, high confidence\n" +
            "• Balanced: Recommended settings for most users\n" +
            "• Lenient: Lower thresholds, more matches, may include false positives\n" +
            "• Custom: Manually configured settings"
        );
        
        helpContent.put("duplicates.help",
            "Duplicate Management Help\n\n" +
            "This tab helps you find and manage duplicate songs in your collection.\n\n" +
            
            "HOW IT WORKS:\n" +
            "1. Click 'Refresh Duplicates' to scan for potential duplicates using your fuzzy search settings\n" +
            "2. Review the 'Potential Duplicates' list on the left\n" +
            "3. Select a song to see its similar matches in the right panel\n" +
            "4. Compare the songs and decide which to keep or delete\n\n" +
            
            "COMPARISON FACTORS:\n" +
            "• Bitrate: Higher bitrate generally means better audio quality\n" +
            "• File Size: Larger files often (but not always) have better quality\n" +
            "• Duration: Slight differences may indicate different versions\n" +
            "• File Path: Check if one is in a 'better' location or folder structure\n\n" +
            
            "ACTIONS:\n" +
            "• Keep Better Quality: Automatically deletes the lower bitrate version\n" +
            "• Delete Selected: Manually delete the selected file\n" +
            "• Individual Review: Examine each pair manually for best results\n\n" +
            
            "SAFETY TIPS:\n" +
            "• Always backup your music collection before deleting files\n" +
            "• Review matches carefully - the system may occasionally suggest non-duplicates\n" +
            "• Consider bitrate, file format, and source when choosing which file to keep"
        );
        
        helpContent.put("import.help",
            "Import & Organization Help\n\n" +
            "This tab handles adding music files to your database and organizing your collection.\n\n" +
            
            "IMPORTING MUSIC:\n" +
            "1. Click 'Add Directories to Scan' to select folders containing music files\n" +
            "2. The system will automatically scan subdirectories\n" +
            "3. Only files matching your enabled file types will be imported\n" +
            "4. Metadata is automatically extracted from file tags and filenames\n\n" +
            
            "ORGANIZING MUSIC:\n" +
            "1. Choose a destination folder for your organized collection\n" +
            "2. Click 'Organize Music Files' to copy files to the new structure\n" +
            "3. Files are organized as: Artist/Album/TrackNumber-Title.extension\n" +
            "4. Original files remain unchanged - only copies are organized\n\n" +
            
            "SUPPORTED FORMATS:\n" +
            "• MP3, FLAC, M4A (AAC), WAV, OGG Vorbis\n" +
            "• WMA, AIFF, APE, OPUS\n" +
            "• Configure which formats to include in the Config tab\n\n" +
            
            "IMPORTANT NOTES:\n" +
            "• Clearing the database only removes records, not actual files\n" +
            "• Organization creates copies - your original files are safe\n" +
            "• Large collections may take time to scan and organize\n" +
            "• Ensure destination folder has enough free space"
        );
        
        helpContent.put("metadata.help",
            "Metadata Editor Help\n\n" +
            "This tab allows you to search for and edit song information.\n\n" +
            
            "SEARCHING:\n" +
            "• Enter keywords in the search field\n" +
            "• Choose to search in Title, Artist, Album, or All Fields\n" +
            "• Partial matches are supported (e.g., 'beat' will find 'Beatles')\n" +
            "• Search is case-insensitive\n\n" +
            
            "EDITING METADATA:\n" +
            "• Select a song from the search results to edit\n" +
            "• Update any fields: Title, Artist, Album, Genre, Track Number, Year\n" +
            "• Changes are saved to the database when you click 'Save Changes'\n" +
            "• Use 'Revert' to undo unsaved changes\n\n" +
            
            "FIELD GUIDELINES:\n" +
            "• Title: The song name as it should appear\n" +
            "• Artist: Primary performing artist or band\n" +
            "• Album: Album or collection name\n" +
            "• Genre: Musical style (Rock, Pop, Jazz, etc.)\n" +
            "• Track Number: Position on album (numbers only)\n" +
            "• Year: Release year (4 digits)\n\n" +
            
            "METADATA TIPS:\n" +
            "• Consistent formatting helps with duplicate detection\n" +
            "• Use standard genre names for better organization\n" +
            "• Include track numbers for proper album organization\n" +
            "• Accurate artist names improve fuzzy matching"
        );
        
        helpContent.put("profiles.help",
            "Database Profiles Help\n\n" +
            "Profiles allow you to maintain multiple separate music databases.\n\n" +
            
            "WHY USE PROFILES:\n" +
            "• Separate different music collections (Personal, Work, Classical, etc.)\n" +
            "• Use different duplicate detection settings for different types of music\n" +
            "• Keep databases in different locations\n" +
            "• Switch between collections without losing settings\n\n" +
            
            "MANAGING PROFILES:\n" +
            "• New Profile: Creates a fresh profile with default settings\n" +
            "• Duplicate Profile: Copies current profile's settings to a new profile\n" +
            "• Rename Profile: Changes the display name of the current profile\n" +
            "• Delete Profile: Removes a profile (requires at least one to remain)\n\n" +
            
            "PROFILE SETTINGS:\n" +
            "Each profile maintains its own:\n" +
            "• Database location and files\n" +
            "• File type preferences\n" +
            "• Fuzzy search configuration\n" +
            "• Metadata and organization settings\n\n" +
            
            "SWITCHING PROFILES:\n" +
            "• Use the dropdown to select a different profile\n" +
            "• The application will switch databases and load that profile's settings\n" +
            "• Each profile remembers its last used date"
        );
    }
    
    /**
     * Apply a tooltip to a UI component.
     */
    public static void setTooltip(Control component, String tooltipKey) {
        String tooltipText = tooltips.get(tooltipKey);
        if (tooltipText != null) {
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(400);
            component.setTooltip(tooltip);
        }
    }
    
    /**
     * Show a context-sensitive help dialog.
     */
    public static void showHelpDialog(String helpKey, String title, Window owner) {
        String content = helpContent.get(helpKey);
        if (content != null) {
            showCustomHelpDialog(title, content, owner);
        }
    }
    
    /**
     * Show a help dialog with custom content.
     */
    public static void showCustomHelpDialog(String title, String content, Window owner) {
        Stage helpStage = new Stage();
        helpStage.initModality(Modality.APPLICATION_MODAL);
        helpStage.initOwner(owner);
        helpStage.setTitle(title);
        
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        
        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(25);
        textArea.setPrefColumnCount(80);
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> helpStage.close());
        closeButton.setPrefWidth(100);
        
        layout.getChildren().addAll(textArea, closeButton);
        
        Scene scene = new Scene(layout);
        helpStage.setScene(scene);
        helpStage.setResizable(true);
        helpStage.show();
    }
    
    /**
     * Show the getting started guide.
     */
    public static void showGettingStarted(Window owner) {
        String gettingStartedContent = 
            "Welcome to MP3Org - Getting Started Guide\n\n" +
            
            "MP3Org helps you organize and manage your music collection by finding duplicates and organizing files.\n\n" +
            
            "STEP 1: CONFIGURE YOUR SETTINGS\n" +
            "• Go to the 'Config' tab\n" +
            "• Choose which audio file formats to include\n" +
            "• Adjust duplicate detection settings if needed (defaults work well for most users)\n\n" +
            
            "STEP 2: IMPORT YOUR MUSIC\n" +
            "• Go to the 'Import & Organize' tab\n" +
            "• Click 'Add Directories to Scan' and select your music folders\n" +
            "• Wait for the scan to complete\n\n" +
            
            "STEP 3: FIND AND MANAGE DUPLICATES\n" +
            "• Go to the 'Duplicate Manager' tab\n" +
            "• Click 'Refresh Duplicates' to find potential duplicates\n" +
            "• Review each potential duplicate and decide which files to keep\n\n" +
            
            "STEP 4: ORGANIZE YOUR COLLECTION (OPTIONAL)\n" +
            "• Return to the 'Import & Organize' tab\n" +
            "• Choose a destination folder\n" +
            "• Click 'Organize Music Files' to create a clean folder structure\n\n" +
            
            "STEP 5: EDIT METADATA (AS NEEDED)\n" +
            "• Use the 'Metadata Editor' tab to search and edit song information\n" +
            "• This helps improve duplicate detection and organization\n\n" +
            
            "TIPS FOR SUCCESS:\n" +
            "• Start with a small subset of your collection to learn the system\n" +
            "• Always backup your music before making changes\n" +
            "• Use the 'Balanced' duplicate detection preset for most situations\n" +
            "• Review duplicate suggestions carefully - not all matches are true duplicates\n\n" +
            
            "Need more help? Each tab has its own help button with detailed instructions.";
        
        showCustomHelpDialog("Getting Started with MP3Org", gettingStartedContent, owner);
    }
    
    /**
     * Show general application help.
     */
    public static void showGeneralHelp(Window owner) {
        String generalHelpContent = 
            "MP3Org - Music Collection Manager\n\n" +
            
            "MP3Org is designed to help you organize and manage large music collections by:\n" +
            "• Finding and removing duplicate songs\n" +
            "• Organizing files into a clean folder structure\n" +
            "• Editing song metadata for better organization\n" +
            "• Managing multiple music databases with profiles\n\n" +
            
            "MAIN FEATURES:\n\n" +
            
            "Duplicate Detection:\n" +
            "• Advanced fuzzy matching finds duplicates even with slightly different metadata\n" +
            "• Configurable similarity thresholds for different music types\n" +
            "• Compares title, artist, album, and duration\n" +
            "• Handles common variations (The Beatles vs Beatles, Deluxe editions, etc.)\n\n" +
            
            "File Organization:\n" +
            "• Automatically organizes music into Artist/Album/Track-Title structure\n" +
            "• Supports all major audio formats (MP3, FLAC, M4A, etc.)\n" +
            "• Preserves original files while creating organized copies\n" +
            "• Configurable file type filtering\n\n" +
            
            "Metadata Management:\n" +
            "• Search and edit song information\n" +
            "• Batch operations for large collections\n" +
            "• Automatic metadata extraction from files\n" +
            "• Consistent formatting for better organization\n\n" +
            
            "Database Profiles:\n" +
            "• Multiple database support for different collections\n" +
            "• Profile-specific settings and configurations\n" +
            "• Easy switching between different music libraries\n" +
            "• Independent duplicate detection settings per profile\n\n" +
            
            "WORKFLOW RECOMMENDATIONS:\n" +
            "1. Configure settings and create profiles as needed\n" +
            "2. Import music files from your existing collection\n" +
            "3. Find and resolve duplicate files\n" +
            "4. Edit metadata for better organization\n" +
            "5. Organize files into a clean structure\n\n" +
            
            "KEYBOARD SHORTCUTS:\n" +
            "• F1: Show this help\n" +
            "• Ctrl+H: Getting Started guide\n" +
            "• Ctrl+R: Refresh current view\n" +
            "• Tab navigation between sections\n\n" +
            
            "For detailed help on specific features, use the help buttons in each tab.";
        
        showCustomHelpDialog("MP3Org Help", generalHelpContent, owner);
    }
}