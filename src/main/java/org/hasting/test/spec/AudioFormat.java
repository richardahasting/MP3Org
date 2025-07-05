package org.hasting.test.spec;

/**
 * Enumeration of supported audio formats for test data generation.
 * 
 * @since 1.0
 */
public enum AudioFormat {
    MP3("mp3", "audio/mpeg"),
    FLAC("flac", "audio/flac"),
    WAV("wav", "audio/wav"),
    OGG("ogg", "audio/ogg");
    
    private final String extension;
    private final String mimeType;
    
    AudioFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }
    
    /**
     * Gets the file extension for this format.
     * 
     * @return The file extension without dot (e.g., "mp3")
     */
    public String getExtension() {
        return extension;
    }
    
    /**
     * Gets the MIME type for this format.
     * 
     * @return The MIME type (e.g., "audio/mpeg")
     */
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * Determines if this format supports metadata embedding.
     * 
     * @return true if metadata can be embedded, false otherwise
     */
    public boolean supportsMetadata() {
        // WAV has limited metadata support
        return this != WAV;
    }
    
    /**
     * Gets the format from a file extension.
     * 
     * @param extension The file extension (with or without dot)
     * @return The matching format, or null if not found
     */
    public static AudioFormat fromExtension(String extension) {
        if (extension == null) {
            return null;
        }
        
        String ext = extension.toLowerCase();
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        
        for (AudioFormat format : values()) {
            if (format.extension.equals(ext)) {
                return format;
            }
        }
        
        return null;
    }
}