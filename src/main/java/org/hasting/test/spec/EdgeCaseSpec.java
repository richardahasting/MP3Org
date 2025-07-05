package org.hasting.test.spec;

import java.util.EnumSet;
import java.util.Set;

/**
 * Specification for generating edge case test files.
 * Used to test handling of unusual or problematic metadata.
 * 
 * <p>Example usage:</p>
 * <pre>
 * EdgeCaseSpec spec = EdgeCaseSpec.builder()
 *     .addType(EdgeCaseType.UNICODE_TITLE)
 *     .addType(EdgeCaseType.LONG_STRINGS)
 *     .addType(EdgeCaseType.SPECIAL_CHARACTERS)
 *     .includeUnicode(true)
 *     .build();
 * </pre>
 * 
 * @since 1.0
 */
public class EdgeCaseSpec {
    
    /**
     * Types of edge cases that can be tested.
     */
    public enum EdgeCaseType {
        UNICODE_TITLE("Title with Unicode characters"),
        UNICODE_ARTIST("Artist with Unicode characters"),
        UNICODE_ALBUM("Album with Unicode characters"),
        LONG_STRINGS("Maximum length strings (255 chars)"),
        SPECIAL_CHARACTERS("Special characters in metadata"),
        MISSING_METADATA("Files with missing metadata fields"),
        EMPTY_STRINGS("Empty string metadata"),
        NULL_VALUES("Null metadata values"),
        NUMERIC_STRINGS("Numeric-only strings"),
        WHITESPACE_ONLY("Whitespace-only strings"),
        CONTROL_CHARACTERS("Control characters in strings"),
        MIXED_ENCODING("Mixed character encodings");
        
        private final String description;
        
        EdgeCaseType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final Set<EdgeCaseType> types;
    private final boolean includeUnicode;
    private final boolean includeLongStrings;
    private final boolean includeMissingMetadata;
    private final AudioFormat preferredFormat;
    
    private EdgeCaseSpec(Builder builder) {
        this.types = EnumSet.copyOf(builder.types);
        this.includeUnicode = builder.includeUnicode;
        this.includeLongStrings = builder.includeLongStrings;
        this.includeMissingMetadata = builder.includeMissingMetadata;
        this.preferredFormat = builder.preferredFormat;
    }
    
    // Getters
    public Set<EdgeCaseType> getTypes() { return EnumSet.copyOf(types); }
    public boolean isIncludeUnicode() { return includeUnicode; }
    public boolean isIncludeLongStrings() { return includeLongStrings; }
    public boolean isIncludeMissingMetadata() { return includeMissingMetadata; }
    public AudioFormat getPreferredFormat() { return preferredFormat; }
    
    /**
     * Creates a new builder instance.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a specification with all edge case types.
     * 
     * @return A spec with all edge cases enabled
     */
    public static EdgeCaseSpec allTypes() {
        return builder()
                .addAllTypes()
                .includeUnicode(true)
                .includeLongStrings(true)
                .includeMissingMetadata(true)
                .build();
    }
    
    /**
     * Builder class for EdgeCaseSpec.
     */
    public static class Builder {
        private final Set<EdgeCaseType> types = EnumSet.noneOf(EdgeCaseType.class);
        private boolean includeUnicode = false;
        private boolean includeLongStrings = false;
        private boolean includeMissingMetadata = false;
        private AudioFormat preferredFormat;
        
        private Builder() {
            // Private constructor
        }
        
        /**
         * Adds an edge case type to test.
         * 
         * @param type The edge case type
         * @return This builder for chaining
         */
        public Builder addType(EdgeCaseType type) {
            types.add(type);
            
            // Update flags based on type
            switch (type) {
                case UNICODE_TITLE:
                case UNICODE_ARTIST:
                case UNICODE_ALBUM:
                    includeUnicode = true;
                    break;
                case LONG_STRINGS:
                    includeLongStrings = true;
                    break;
                case MISSING_METADATA:
                case NULL_VALUES:
                    includeMissingMetadata = true;
                    break;
            }
            
            return this;
        }
        
        /**
         * Adds multiple edge case types.
         * 
         * @param types The edge case types to add
         * @return This builder for chaining
         */
        public Builder addTypes(EdgeCaseType... types) {
            for (EdgeCaseType type : types) {
                addType(type);
            }
            return this;
        }
        
        /**
         * Adds all available edge case types.
         * 
         * @return This builder for chaining
         */
        public Builder addAllTypes() {
            for (EdgeCaseType type : EdgeCaseType.values()) {
                addType(type);
            }
            return this;
        }
        
        /**
         * Sets whether to include Unicode test cases.
         * 
         * @param includeUnicode true to include Unicode cases
         * @return This builder for chaining
         */
        public Builder includeUnicode(boolean includeUnicode) {
            this.includeUnicode = includeUnicode;
            if (includeUnicode) {
                addTypes(EdgeCaseType.UNICODE_TITLE, 
                        EdgeCaseType.UNICODE_ARTIST, 
                        EdgeCaseType.UNICODE_ALBUM);
            }
            return this;
        }
        
        /**
         * Sets whether to include long string test cases.
         * 
         * @param includeLongStrings true to include long string cases
         * @return This builder for chaining
         */
        public Builder includeLongStrings(boolean includeLongStrings) {
            this.includeLongStrings = includeLongStrings;
            if (includeLongStrings) {
                addType(EdgeCaseType.LONG_STRINGS);
            }
            return this;
        }
        
        /**
         * Sets whether to include missing metadata test cases.
         * 
         * @param includeMissingMetadata true to include missing metadata cases
         * @return This builder for chaining
         */
        public Builder includeMissingMetadata(boolean includeMissingMetadata) {
            this.includeMissingMetadata = includeMissingMetadata;
            if (includeMissingMetadata) {
                addTypes(EdgeCaseType.MISSING_METADATA, 
                        EdgeCaseType.NULL_VALUES,
                        EdgeCaseType.EMPTY_STRINGS);
            }
            return this;
        }
        
        /**
         * Sets the preferred audio format for edge case files.
         * 
         * @param format The preferred format
         * @return This builder for chaining
         */
        public Builder preferredFormat(AudioFormat format) {
            this.preferredFormat = format;
            return this;
        }
        
        /**
         * Builds the EdgeCaseSpec instance.
         * 
         * @return The configured EdgeCaseSpec
         * @throws IllegalStateException if no types are selected
         */
        public EdgeCaseSpec build() {
            if (types.isEmpty()) {
                throw new IllegalStateException("At least one edge case type must be selected");
            }
            
            return new EdgeCaseSpec(this);
        }
    }
}