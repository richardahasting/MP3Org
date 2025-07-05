package org.hasting.test.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification for generating duplicate music files with variations.
 * Used to test duplicate detection algorithms.
 * 
 * <p>Example usage:</p>
 * <pre>
 * DuplicateSpec spec = DuplicateSpec.builder()
 *     .baseTitle("Test Song")
 *     .baseArtist("Test Artist")
 *     .addVariation(DuplicateVariation.TITLE_TYPO)
 *     .addVariation(DuplicateVariation.ARTIST_FEATURING)
 *     .addVariation(DuplicateVariation.BITRATE_DIFFERENT)
 *     .build();
 * </pre>
 * 
 * @since 1.0
 */
public class DuplicateSpec {
    
    /**
     * Types of variations that can be applied to create duplicates.
     */
    public enum DuplicateVariation {
        TITLE_TYPO("Introduces typos in the title"),
        ARTIST_FEATURING("Adds featuring information to artist"),
        CASE_DIFFERENT("Changes case of title/artist"),
        BITRATE_DIFFERENT("Uses different bitrate"),
        DURATION_SLIGHT("Slight duration difference"),
        ALBUM_DIFFERENT("Different album name"),
        TRACK_NUMBER_DIFFERENT("Different track number"),
        YEAR_DIFFERENT("Different year"),
        SPECIAL_CHARS("Adds/removes special characters");
        
        private final String description;
        
        DuplicateVariation(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final String baseTitle;
    private final String baseArtist;
    private final String baseAlbum;
    private final AudioFormat format;
    private final List<DuplicateVariation> variations;
    
    private DuplicateSpec(Builder builder) {
        this.baseTitle = builder.baseTitle;
        this.baseArtist = builder.baseArtist;
        this.baseAlbum = builder.baseAlbum;
        this.format = builder.format != null ? builder.format : AudioFormat.MP3;
        this.variations = new ArrayList<>(builder.variations);
    }
    
    // Getters
    public String getBaseTitle() { return baseTitle; }
    public String getBaseArtist() { return baseArtist; }
    public String getBaseAlbum() { return baseAlbum; }
    public AudioFormat getFormat() { return format; }
    public List<DuplicateVariation> getVariations() { return new ArrayList<>(variations); }
    
    /**
     * Gets the total number of files that will be generated (base + variations).
     * 
     * @return The total count
     */
    public int getVariationCount() {
        return variations.size() + 1; // +1 for the base file
    }
    
    /**
     * Creates a new builder instance.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for DuplicateSpec.
     */
    public static class Builder {
        private String baseTitle;
        private String baseArtist;
        private String baseAlbum;
        private AudioFormat format;
        private final List<DuplicateVariation> variations = new ArrayList<>();
        
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
        
        public Builder format(AudioFormat format) {
            this.format = format;
            return this;
        }
        
        /**
         * Adds a variation type to be generated.
         * 
         * @param variation The variation type
         * @return This builder for chaining
         */
        public Builder addVariation(DuplicateVariation variation) {
            if (!variations.contains(variation)) {
                variations.add(variation);
            }
            return this;
        }
        
        /**
         * Adds multiple variation types.
         * 
         * @param variations The variation types to add
         * @return This builder for chaining
         */
        public Builder addVariations(DuplicateVariation... variations) {
            for (DuplicateVariation variation : variations) {
                addVariation(variation);
            }
            return this;
        }
        
        /**
         * Sets the number of variations to generate.
         * This is a convenience method that adds common variations.
         * 
         * @param count The number of variations (max 9)
         * @return This builder for chaining
         */
        public Builder count(int count) {
            variations.clear();
            DuplicateVariation[] allVariations = DuplicateVariation.values();
            for (int i = 0; i < Math.min(count, allVariations.length); i++) {
                variations.add(allVariations[i]);
            }
            return this;
        }
        
        /**
         * Builds the DuplicateSpec instance.
         * 
         * @return The configured DuplicateSpec
         * @throws IllegalStateException if required fields are not set
         */
        public DuplicateSpec build() {
            if (baseTitle == null || baseTitle.trim().isEmpty()) {
                throw new IllegalStateException("Base title is required");
            }
            if (baseArtist == null || baseArtist.trim().isEmpty()) {
                throw new IllegalStateException("Base artist is required");
            }
            
            return new DuplicateSpec(this);
        }
    }
}