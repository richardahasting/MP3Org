package org.hasting.test.spec;

import java.util.HashMap;
import java.util.Map;

/**
 * Specification for generating complete test data sets.
 * Used for performance testing and large-scale scenarios.
 * 
 * <p>Example usage:</p>
 * <pre>
 * TestDataSetSpec spec = TestDataSetSpec.builder()
 *     .fileCount(1000)
 *     .randomizeMetadata(true)
 *     .formatDistribution(
 *         AudioFormat.MP3, 70,    // 70% MP3
 *         AudioFormat.FLAC, 20,   // 20% FLAC
 *         AudioFormat.WAV, 5,     // 5% WAV
 *         AudioFormat.OGG, 5      // 5% OGG
 *     )
 *     .includeDuplicates(true, 10) // 10% duplicates
 *     .build();
 * </pre>
 * 
 * @since 1.0
 */
public class TestDataSetSpec {
    
    private final int fileCount;
    private final boolean randomizeMetadata;
    private final Map<AudioFormat, Integer> formatDistribution;
    private final boolean includeDuplicates;
    private final int duplicatePercentage;
    private final boolean includeEdgeCases;
    private final int edgeCasePercentage;
    
    private TestDataSetSpec(Builder builder) {
        this.fileCount = builder.fileCount;
        this.randomizeMetadata = builder.randomizeMetadata;
        this.formatDistribution = normalizeDistribution(builder.formatDistribution, builder.fileCount);
        this.includeDuplicates = builder.includeDuplicates;
        this.duplicatePercentage = builder.duplicatePercentage;
        this.includeEdgeCases = builder.includeEdgeCases;
        this.edgeCasePercentage = builder.edgeCasePercentage;
    }
    
    // Getters
    public int getFileCount() { return fileCount; }
    public boolean isRandomizeMetadata() { return randomizeMetadata; }
    public Map<AudioFormat, Integer> getFormatDistribution() { return new HashMap<>(formatDistribution); }
    public boolean isIncludeDuplicates() { return includeDuplicates; }
    public int getDuplicatePercentage() { return duplicatePercentage; }
    public boolean isIncludeEdgeCases() { return includeEdgeCases; }
    public int getEdgeCasePercentage() { return edgeCasePercentage; }
    
    /**
     * Normalizes the format distribution to match the total file count.
     */
    private static Map<AudioFormat, Integer> normalizeDistribution(Map<AudioFormat, Integer> percentages, int totalFiles) {
        Map<AudioFormat, Integer> counts = new HashMap<>();
        int allocated = 0;
        
        // Calculate counts based on percentages
        for (Map.Entry<AudioFormat, Integer> entry : percentages.entrySet()) {
            int count = (int) (totalFiles * entry.getValue() / 100.0);
            counts.put(entry.getKey(), count);
            allocated += count;
        }
        
        // Distribute any remaining files to the most common format
        if (allocated < totalFiles) {
            AudioFormat mostCommon = percentages.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(AudioFormat.MP3);
            
            counts.put(mostCommon, counts.getOrDefault(mostCommon, 0) + (totalFiles - allocated));
        }
        
        return counts;
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
     * Builder class for TestDataSetSpec.
     */
    public static class Builder {
        private int fileCount = 100;
        private boolean randomizeMetadata = false;
        private final Map<AudioFormat, Integer> formatDistribution = new HashMap<>();
        private boolean includeDuplicates = false;
        private int duplicatePercentage = 10;
        private boolean includeEdgeCases = false;
        private int edgeCasePercentage = 5;
        
        private Builder() {
            // Default distribution if none specified
            formatDistribution.put(AudioFormat.MP3, 100);
        }
        
        /**
         * Sets the total number of files to generate.
         * 
         * @param fileCount The number of files
         * @return This builder for chaining
         */
        public Builder fileCount(int fileCount) {
            if (fileCount <= 0) {
                throw new IllegalArgumentException("File count must be positive");
            }
            this.fileCount = fileCount;
            return this;
        }
        
        /**
         * Sets whether to randomize metadata for each file.
         * 
         * @param randomizeMetadata true to randomize metadata
         * @return This builder for chaining
         */
        public Builder randomizeMetadata(boolean randomizeMetadata) {
            this.randomizeMetadata = randomizeMetadata;
            return this;
        }
        
        /**
         * Sets the format distribution as percentages.
         * The percentages should add up to 100.
         * 
         * @param format1 First format
         * @param percentage1 Percentage for first format
         * @param moreFormats Additional format/percentage pairs
         * @return This builder for chaining
         */
        public Builder formatDistribution(AudioFormat format1, int percentage1, Object... moreFormats) {
            formatDistribution.clear();
            formatDistribution.put(format1, percentage1);
            
            // Parse additional format/percentage pairs
            for (int i = 0; i < moreFormats.length - 1; i += 2) {
                if (moreFormats[i] instanceof AudioFormat && moreFormats[i + 1] instanceof Integer) {
                    formatDistribution.put((AudioFormat) moreFormats[i], (Integer) moreFormats[i + 1]);
                }
            }
            
            return this;
        }
        
        /**
         * Sets the format distribution using a map.
         * 
         * @param distribution Map of format to percentage
         * @return This builder for chaining
         */
        public Builder formatDistribution(Map<AudioFormat, Integer> distribution) {
            formatDistribution.clear();
            formatDistribution.putAll(distribution);
            return this;
        }
        
        /**
         * Sets whether to include duplicate files.
         * 
         * @param includeDuplicates true to include duplicates
         * @param percentage Percentage of files that should have duplicates
         * @return This builder for chaining
         */
        public Builder includeDuplicates(boolean includeDuplicates, int percentage) {
            this.includeDuplicates = includeDuplicates;
            this.duplicatePercentage = Math.max(0, Math.min(100, percentage));
            return this;
        }
        
        /**
         * Sets whether to include edge case files.
         * 
         * @param includeEdgeCases true to include edge cases
         * @param percentage Percentage of files that should be edge cases
         * @return This builder for chaining
         */
        public Builder includeEdgeCases(boolean includeEdgeCases, int percentage) {
            this.includeEdgeCases = includeEdgeCases;
            this.edgeCasePercentage = Math.max(0, Math.min(100, percentage));
            return this;
        }
        
        /**
         * Builds the TestDataSetSpec instance.
         * 
         * @return The configured TestDataSetSpec
         * @throws IllegalStateException if configuration is invalid
         */
        public TestDataSetSpec build() {
            // Validate format distribution
            int totalPercentage = formatDistribution.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            
            if (totalPercentage == 0) {
                // Use default if no distribution specified
                formatDistribution.clear();
                formatDistribution.put(AudioFormat.MP3, 100);
            } else if (Math.abs(totalPercentage - 100) > 5) {
                // Allow small rounding errors
                throw new IllegalStateException(
                        "Format distribution percentages should sum to approximately 100, but got " + totalPercentage);
            }
            
            return new TestDataSetSpec(this);
        }
    }
}