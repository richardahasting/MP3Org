package org.hasting.test.spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Specification for generating test files in different audio formats.
 * Used to test format-specific functionality and compatibility.
 * 
 * <p>Example usage:</p>
 * <pre>
 * FormatSpec spec = FormatSpec.builder()
 *     .baseTitle("Format Test Song")
 *     .baseArtist("Format Test Artist")
 *     .addFormat(AudioFormat.MP3)
 *     .addFormat(AudioFormat.FLAC)
 *     .addFormat(AudioFormat.WAV)
 *     .build();
 * </pre>
 * 
 * @since 1.0
 */
public class FormatSpec {
    
    private final String baseTitle;
    private final String baseArtist;
    private final String baseAlbum;
    private final List<AudioFormat> formats;
    
    private FormatSpec(Builder builder) {
        this.baseTitle = builder.baseTitle;
        this.baseArtist = builder.baseArtist;
        this.baseAlbum = builder.baseAlbum;
        this.formats = new ArrayList<>(builder.formats);
    }
    
    // Getters
    public String getBaseTitle() { return baseTitle; }
    public String getBaseArtist() { return baseArtist; }
    public String getBaseAlbum() { return baseAlbum; }
    public List<AudioFormat> getFormats() { return new ArrayList<>(formats); }
    
    /**
     * Creates a new builder instance.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a specification for all supported formats.
     * 
     * @param baseTitle The base title for all files
     * @param baseArtist The base artist for all files
     * @return A spec with all formats
     */
    public static FormatSpec allFormats(String baseTitle, String baseArtist) {
        return builder()
                .baseTitle(baseTitle)
                .baseArtist(baseArtist)
                .addAllFormats()
                .build();
    }
    
    /**
     * Builder class for FormatSpec.
     */
    public static class Builder {
        private String baseTitle;
        private String baseArtist;
        private String baseAlbum;
        private final List<AudioFormat> formats = new ArrayList<>();
        
        private Builder() {
            // Private constructor
        }
        
        public Builder baseTitle(String baseTitle) {
            this.baseTitle = baseTitle;
            return this;
        }
        
        public Builder baseArtist(String baseArtist) {
            this.baseArtist = baseArtist;
            return this;
        }
        
        public Builder baseAlbum(String baseAlbum) {
            this.baseAlbum = baseAlbum;
            return this;
        }
        
        /**
         * Adds a format to test.
         * 
         * @param format The audio format
         * @return This builder for chaining
         */
        public Builder addFormat(AudioFormat format) {
            if (!formats.contains(format)) {
                formats.add(format);
            }
            return this;
        }
        
        /**
         * Adds multiple formats to test.
         * 
         * @param formats The audio formats
         * @return This builder for chaining
         */
        public Builder addFormats(AudioFormat... formats) {
            for (AudioFormat format : formats) {
                addFormat(format);
            }
            return this;
        }
        
        /**
         * Adds all available formats.
         * 
         * @return This builder for chaining
         */
        public Builder addAllFormats() {
            formats.clear();
            formats.addAll(Arrays.asList(AudioFormat.values()));
            return this;
        }
        
        /**
         * Builds the FormatSpec instance.
         * 
         * @return The configured FormatSpec
         * @throws IllegalStateException if required fields are not set
         */
        public FormatSpec build() {
            if (baseTitle == null || baseTitle.trim().isEmpty()) {
                throw new IllegalStateException("Base title is required");
            }
            if (baseArtist == null || baseArtist.trim().isEmpty()) {
                throw new IllegalStateException("Base artist is required");
            }
            if (formats.isEmpty()) {
                throw new IllegalStateException("At least one format must be specified");
            }
            
            return new FormatSpec(this);
        }
    }
}