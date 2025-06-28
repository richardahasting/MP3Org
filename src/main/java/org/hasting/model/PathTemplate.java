package org.hasting.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a configurable path template for organizing music files.
 * Supports field placeholders like {artist}, {album}, etc. with text formatting options.
 */
public class PathTemplate {
    
    public enum TextFormat {
        NONE("No formatting"),
        UNDERSCORE("Replace spaces with underscores"),
        DASH("Replace spaces with dashes"),
        CAMEL_CASE("camelCase formatting"),
        PASCAL_CASE("PascalCase formatting"),
        LOWER_CASE("lowercase formatting"),
        UPPER_CASE("UPPERCASE formatting");
        
        private final String description;
        
        TextFormat(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private String template;
    private TextFormat textFormat;
    private boolean useSubdirectoryGrouping;
    private int subdirectoryLevels;
    
    // Default template matching the current behavior
    public static final String DEFAULT_TEMPLATE = "{subdirectory}/{artist}/{album}/{track_number:02d}-{title}.{file_type}";
    
    public PathTemplate() {
        this.template = DEFAULT_TEMPLATE;
        this.textFormat = TextFormat.UNDERSCORE;
        this.useSubdirectoryGrouping = true;
        this.subdirectoryLevels = 7; // Match current MusicFile default
    }
    
    public PathTemplate(String template, TextFormat textFormat, boolean useSubdirectoryGrouping, int subdirectoryLevels) {
        this.template = template;
        this.textFormat = textFormat;
        this.useSubdirectoryGrouping = useSubdirectoryGrouping;
        this.subdirectoryLevels = subdirectoryLevels;
    }
    
    /**
     * Generates the file path for a music file using this template.
     */
    public String generatePath(String basePath, MusicFile musicFile) {
        String result = template;
        
        // Handle subdirectory grouping if enabled
        if (useSubdirectoryGrouping && result.contains("{subdirectory}")) {
            String subdirectory = generateSubdirectory(musicFile);
            result = result.replace("{subdirectory}", subdirectory);
        }
        
        // Replace field placeholders and apply text formatting to each field value
        result = replaceFieldPlaceholdersWithFormatting(result, musicFile);
        
        // Combine with base path
        return basePath + "/" + result;
    }
    
    /**
     * Generates subdirectory grouping based on the artist's first letter.
     */
    private String generateSubdirectory(MusicFile musicFile) {
        // Set the subdirectory levels and regenerate initials if needed
        if (MusicFile.getNumberOfSubdirectorys() != subdirectoryLevels) {
            MusicFile.setNumberOfSubdirectorys(subdirectoryLevels);
            MusicFile.resetArtistCounts(); // This will force regeneration
        }
        
        String artist = musicFile.getArtist();
        if (artist == null || artist.isEmpty()) {
            return "Unknown";
        }
        
        String firstLetter = artist.substring(0, 1).toUpperCase();
        if (!firstLetter.matches("[A-Z]")) {
            return "Misc";
        }
        
        // Use reflection to access the static whatInitials method and initials map
        try {
            java.lang.reflect.Method whatInitialsMethod = MusicFile.class.getDeclaredMethod("whatInitials");
            whatInitialsMethod.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.HashMap<String, String> initials = (java.util.HashMap<String, String>) whatInitialsMethod.invoke(null);
            return initials.getOrDefault(firstLetter, "misc");
        } catch (Exception e) {
            // Fallback to simple grouping if reflection fails
            return firstLetter;
        }
    }
    
    /**
     * Replaces field placeholders like {artist}, {album}, etc.
     */
    private String replaceFieldPlaceholders(String text, MusicFile musicFile) {
        // Pattern to match field placeholders with optional formatting
        Pattern pattern = Pattern.compile("\\{([a-z_]+)(?::(\\w+))?\\}");
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String formatting = matcher.group(2);
            
            String value = getFieldValue(fieldName, musicFile, formatting);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Replaces field placeholders and applies text formatting to each field value.
     */
    private String replaceFieldPlaceholdersWithFormatting(String text, MusicFile musicFile) {
        // Pattern to match field placeholders with optional formatting
        Pattern pattern = Pattern.compile("\\{([a-z_]+)(?::(\\w+))?\\}");
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String formatting = matcher.group(2);
            
            String value = getFieldValue(fieldName, musicFile, formatting);
            // Apply text formatting to each field value
            String formattedValue = applyTextFormatting(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(formattedValue));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Gets the value for a specific field with optional formatting.
     */
    private String getFieldValue(String fieldName, MusicFile musicFile, String formatting) {
        String value;
        
        switch (fieldName.toLowerCase()) {
            case "artist":
                value = musicFile.getArtist();
                break;
            case "album":
                value = musicFile.getAlbum();
                break;
            case "title":
                value = musicFile.getTitle();
                break;
            case "genre":
                value = musicFile.getGenre();
                break;
            case "year":
                value = musicFile.getYear() != null ? musicFile.getYear().toString() : "";
                break;
            case "track_number":
                if (musicFile.getTrackNumber() != null) {
                    if (formatting != null && formatting.matches("\\d+d")) {
                        // Format like "02d" for zero-padded numbers
                        int width = Integer.parseInt(formatting.substring(0, formatting.length() - 1));
                        value = String.format("%0" + width + "d", musicFile.getTrackNumber());
                    } else {
                        value = musicFile.getTrackNumber().toString();
                    }
                } else {
                    value = "00";
                }
                break;
            case "bit_rate":
                value = musicFile.getBitRate() != null ? musicFile.getBitRate().toString() : "";
                break;
            case "sample_rate":
                value = musicFile.getSampleRate() != null ? musicFile.getSampleRate().toString() : "";
                break;
            case "file_type":
                value = musicFile.getFileType() != null ? musicFile.getFileType() : "unknown";
                break;
            default:
                value = "Unknown";
        }
        
        // Handle null values
        if (value == null || value.isEmpty()) {
            value = "Unknown";
        }
        
        return value;
    }
    
    /**
     * Applies text formatting based on the configured format.
     */
    private String applyTextFormatting(String text) {
        switch (textFormat) {
            case UNDERSCORE:
                return text.replaceAll("\\s+", "_");
            case DASH:
                return text.replaceAll("\\s+", "-");
            case CAMEL_CASE:
                return toCamelCase(text);
            case PASCAL_CASE:
                return toPascalCase(text);
            case LOWER_CASE:
                return text.toLowerCase();
            case UPPER_CASE:
                return text.toUpperCase();
            case NONE:
            default:
                return text;
        }
    }
    
    /**
     * Converts text to camelCase.
     */
    private String toCamelCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (i == 0) {
                result.append(word);
            } else if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Converts text to PascalCase.
     */
    private String toPascalCase(String text) {
        String camelCase = toCamelCase(text);
        if (camelCase.length() > 0) {
            return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
        }
        return camelCase;
    }
    
    // Getters and setters
    public String getTemplate() {
        return template;
    }
    
    public void setTemplate(String template) {
        this.template = template;
    }
    
    public TextFormat getTextFormat() {
        return textFormat;
    }
    
    public void setTextFormat(TextFormat textFormat) {
        this.textFormat = textFormat;
    }
    
    public boolean isUseSubdirectoryGrouping() {
        return useSubdirectoryGrouping;
    }
    
    public void setUseSubdirectoryGrouping(boolean useSubdirectoryGrouping) {
        this.useSubdirectoryGrouping = useSubdirectoryGrouping;
    }
    
    public int getSubdirectoryLevels() {
        return subdirectoryLevels;
    }
    
    public void setSubdirectoryLevels(int subdirectoryLevels) {
        this.subdirectoryLevels = Math.max(0, Math.min(12, subdirectoryLevels)); // Clamp to 0-12 range
    }
    
    /**
     * Gets available field names for template building.
     */
    public static String[] getAvailableFields() {
        return new String[] {
            "artist", "album", "title", "genre", "year", 
            "track_number", "bit_rate", "sample_rate", "file_type", "subdirectory"
        };
    }
    
    /**
     * Gets example templates for common organizations.
     */
    public static String[] getExampleTemplates() {
        return new String[] {
            "{subdirectory}/{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            "{artist}/{album}/{track_number:02d} {title}.{file_type}",
            "{genre}/{artist}/{year} - {album}/{track_number:02d} - {title}.{file_type}",
            "{artist}/{title}.{file_type}",
            "{year}/{artist} - {album}/{title}.{file_type}"
        };
    }
    
    @Override
    public String toString() {
        return String.format("PathTemplate{template='%s', format=%s, subdirs=%s(%d)}", 
                           template, textFormat, useSubdirectoryGrouping, subdirectoryLevels);
    }
}