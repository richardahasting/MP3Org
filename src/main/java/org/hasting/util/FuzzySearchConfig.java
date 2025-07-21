package org.hasting.util;

import java.util.Properties;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

/**
 * Comprehensive configuration class for fine-tuning fuzzy search algorithms in duplicate detection.
 * 
 * <p>This class provides extensive customization options for controlling how the duplicate
 * detection system determines similarity between music files. It supports multiple matching
 * strategies, configurable thresholds, and various normalization options to handle the
 * complexities of real-world music metadata variations.
 * 
 * <p>Core configuration categories:
 * <ul>
 * <li><strong>Similarity Thresholds</strong> - Percentage-based matching requirements for each field</li>
 * <li><strong>Text Normalization</strong> - Case, punctuation, and formatting handling options</li>
 * <li><strong>Music-Specific Rules</strong> - Artist prefixes, featuring credits, album editions</li>
 * <li><strong>Duration Matching</strong> - Time-based tolerance for audio length comparison</li>
 * <li><strong>Track Number Handling</strong> - Strict or flexible track number matching</li>
 * <li><strong>Quality Parameters</strong> - Bitrate tolerance and minimum field requirements</li>
 * </ul>
 * 
 * <p>Similarity threshold configuration (0-100%):
 * <ul>
 * <li><strong>Title Similarity</strong> - Default 85% (configurable)</li>
 * <li><strong>Artist Similarity</strong> - Default 90% (configurable)</li>
 * <li><strong>Album Similarity</strong> - Default 85% (configurable)</li>
 * <li><strong>Minimum Fields to Match</strong> - Default 2 out of 4 fields (configurable 1-4)</li>
 * </ul>
 * 
 * <p>Text normalization options:
 * <ul>
 * <li><strong>Case Sensitivity</strong> - Optional case-insensitive matching (default: ignore case)</li>
 * <li><strong>Punctuation Handling</strong> - Optional punctuation normalization (default: ignore)</li>
 * <li><strong>Word Order Sensitivity</strong> - Whether word order affects similarity (default: insensitive)</li>
 * <li><strong>Artist Prefixes</strong> - Handle "The", "A", "An" prefixes (default: ignore)</li>
 * <li><strong>Featuring Credits</strong> - Handle "feat.", "ft.", "featuring" (default: keep)</li>
 * <li><strong>Album Editions</strong> - Handle "Deluxe", "Remastered", etc. (default: ignore)</li>
 * </ul>
 * 
 * <p>Duration tolerance supports both absolute and percentage-based matching:
 * <ul>
 * <li><strong>Absolute Tolerance</strong> - Default ±10 seconds</li>
 * <li><strong>Percentage Tolerance</strong> - Default ±5% of average duration</li>
 * <li>Files match if they satisfy <em>either</em> tolerance condition</li>
 * </ul>
 * 
 * <p>Pre-configured profiles for common use cases:
 * <ul>
 * <li><strong>Strict</strong> - 100% exact matching with all requirements</li>
 * <li><strong>Balanced</strong> - Default settings for typical collections (recommended)</li>
 * <li><strong>Lenient</strong> - Lower thresholds for collections with inconsistent metadata</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Create default configuration
 * FuzzySearchConfig config = new FuzzySearchConfig();
 * 
 * // Customize for strict matching
 * config.setTitleSimilarityThreshold(95.0);
 * config.setTrackNumberMustMatch(true);
 * config.setMinimumFieldsToMatch(3);
 * 
 * // Use predefined profiles
 * FuzzySearchConfig strict = FuzzySearchConfig.createStrictConfig();
 * FuzzySearchConfig lenient = FuzzySearchConfig.createLenientConfig();
 * 
 * // Save and load configurations
 * Properties props = config.toProperties();
 * FuzzySearchConfig restored = FuzzySearchConfig.fromProperties(props);
 * 
 * // Use with duplicate detection
 * boolean isDuplicate = FuzzyMatcher.areDuplicates(file1, file2, config);
 * double similarity = FuzzyMatcher.calculateSimilarity(file1, file2, config);
 * }</pre>
 * 
 * <p>All setter methods include automatic range validation to ensure sensible values.
 * Percentage thresholds are clamped to 0-100%, and minimum field counts are constrained
 * to 1-4 (corresponding to title, artist, album, and duration fields).
 * 
 * <p>The configuration can be serialized to and from Properties format for persistence,
 * making it suitable for user preferences and configuration file storage.
 * 
 * @see FuzzyMatcher for the algorithms that use these configurations
 * @see MusicFile for the data model being compared
 * @since 1.0
 */
public class FuzzySearchConfig {
    
    private static final Logger logger = Log4Rich.getLogger(FuzzySearchConfig.class);
    
    // Default values
    public static final double DEFAULT_TITLE_SIMILARITY = 85.0;
    public static final double DEFAULT_ARTIST_SIMILARITY = 90.0;
    public static final double DEFAULT_ALBUM_SIMILARITY = 85.0;
    public static final int DEFAULT_DURATION_TOLERANCE_SECONDS = 10;
    public static final double DEFAULT_DURATION_TOLERANCE_PERCENT = 5.0;
    public static final boolean DEFAULT_IGNORE_CASE = true;
    public static final boolean DEFAULT_IGNORE_PUNCTUATION = true;
    public static final boolean DEFAULT_TRACK_NUMBER_MUST_MATCH = false;
    public static final boolean DEFAULT_IGNORE_MISSING_TRACK_NUMBER = true;
    public static final boolean DEFAULT_IGNORE_ARTIST_PREFIXES = true;
    public static final boolean DEFAULT_IGNORE_FEATURING = false;
    public static final boolean DEFAULT_IGNORE_ALBUM_EDITIONS = true;
    public static final int DEFAULT_MIN_FIELDS_MATCH = 2;
    public static final int DEFAULT_BITRATE_TOLERANCE = 64;
    public static final boolean DEFAULT_WORD_ORDER_SENSITIVE = false;
    
    // Configuration name
    private String configName;
    
    // Title matching parameters
    private double titleSimilarityThreshold;
    private boolean ignoreCaseDifferences;
    private boolean ignorePunctuation;
    private boolean wordOrderSensitive;
    
    // Artist matching parameters
    private double artistSimilarityThreshold;
    private boolean ignoreArtistPrefixes; // "The", "A", "An"
    private boolean ignoreFeaturing; // "feat.", "ft.", "featuring"
    
    // Album matching parameters
    private double albumSimilarityThreshold;
    private boolean ignoreAlbumEditions; // "Remastered", "Deluxe", etc.
    
    // Duration matching parameters
    private int durationToleranceSeconds;
    private double durationTolerancePercent;
    
    // Track number parameters
    private boolean trackNumberMustMatch;
    private boolean ignoreMissingTrackNumber;
    
    // Quality parameters
    private int bitrateToleranceKbps;
    
    // Advanced parameters
    private int minimumFieldsToMatch;
    
    /**
     * Creates a new fuzzy search configuration with default values.
     */
    public FuzzySearchConfig() {
        this("Default");
    }
    
    /**
     * Creates a new fuzzy search configuration with the specified name.
     */
    public FuzzySearchConfig(String configName) {
        this.configName = configName != null ? configName : "Default";
        loadDefaults();
    }
    
    /**
     * Loads default configuration values.
     */
    private void loadDefaults() {
        this.titleSimilarityThreshold = DEFAULT_TITLE_SIMILARITY;
        this.artistSimilarityThreshold = DEFAULT_ARTIST_SIMILARITY;
        this.albumSimilarityThreshold = DEFAULT_ALBUM_SIMILARITY;
        this.durationToleranceSeconds = DEFAULT_DURATION_TOLERANCE_SECONDS;
        this.durationTolerancePercent = DEFAULT_DURATION_TOLERANCE_PERCENT;
        this.ignoreCaseDifferences = DEFAULT_IGNORE_CASE;
        this.ignorePunctuation = DEFAULT_IGNORE_PUNCTUATION;
        this.trackNumberMustMatch = DEFAULT_TRACK_NUMBER_MUST_MATCH;
        this.ignoreMissingTrackNumber = DEFAULT_IGNORE_MISSING_TRACK_NUMBER;
        this.ignoreArtistPrefixes = DEFAULT_IGNORE_ARTIST_PREFIXES;
        this.ignoreFeaturing = DEFAULT_IGNORE_FEATURING;
        this.ignoreAlbumEditions = DEFAULT_IGNORE_ALBUM_EDITIONS;
        this.minimumFieldsToMatch = DEFAULT_MIN_FIELDS_MATCH;
        this.bitrateToleranceKbps = DEFAULT_BITRATE_TOLERANCE;
        this.wordOrderSensitive = DEFAULT_WORD_ORDER_SENSITIVE;
    }
    
    // Getters and Setters
    
    public String getConfigName() {
        return configName;
    }
    
    public void setConfigName(String configName) {
        this.configName = configName != null ? configName : "Default";
    }
    
    public double getTitleSimilarityThreshold() {
        return titleSimilarityThreshold;
    }
    
    public void setTitleSimilarityThreshold(double titleSimilarityThreshold) {
        this.titleSimilarityThreshold = Math.max(0, Math.min(100, titleSimilarityThreshold));
    }
    
    public double getArtistSimilarityThreshold() {
        return artistSimilarityThreshold;
    }
    
    public void setArtistSimilarityThreshold(double artistSimilarityThreshold) {
        this.artistSimilarityThreshold = Math.max(0, Math.min(100, artistSimilarityThreshold));
    }
    
    public double getAlbumSimilarityThreshold() {
        return albumSimilarityThreshold;
    }
    
    public void setAlbumSimilarityThreshold(double albumSimilarityThreshold) {
        this.albumSimilarityThreshold = Math.max(0, Math.min(100, albumSimilarityThreshold));
    }
    
    public int getDurationToleranceSeconds() {
        return durationToleranceSeconds;
    }
    
    public void setDurationToleranceSeconds(int durationToleranceSeconds) {
        this.durationToleranceSeconds = Math.max(0, durationToleranceSeconds);
    }
    
    public double getDurationTolerancePercent() {
        return durationTolerancePercent;
    }
    
    public void setDurationTolerancePercent(double durationTolerancePercent) {
        this.durationTolerancePercent = Math.max(0, durationTolerancePercent);
    }
    
    public boolean isIgnoreCaseDifferences() {
        return ignoreCaseDifferences;
    }
    
    public void setIgnoreCaseDifferences(boolean ignoreCaseDifferences) {
        this.ignoreCaseDifferences = ignoreCaseDifferences;
    }
    
    public boolean isIgnorePunctuation() {
        return ignorePunctuation;
    }
    
    public void setIgnorePunctuation(boolean ignorePunctuation) {
        this.ignorePunctuation = ignorePunctuation;
    }
    
    public boolean isWordOrderSensitive() {
        return wordOrderSensitive;
    }
    
    public void setWordOrderSensitive(boolean wordOrderSensitive) {
        this.wordOrderSensitive = wordOrderSensitive;
    }
    
    public boolean isTrackNumberMustMatch() {
        return trackNumberMustMatch;
    }
    
    public void setTrackNumberMustMatch(boolean trackNumberMustMatch) {
        this.trackNumberMustMatch = trackNumberMustMatch;
    }
    
    public boolean isIgnoreMissingTrackNumber() {
        return ignoreMissingTrackNumber;
    }
    
    public void setIgnoreMissingTrackNumber(boolean ignoreMissingTrackNumber) {
        this.ignoreMissingTrackNumber = ignoreMissingTrackNumber;
    }
    
    public boolean isIgnoreArtistPrefixes() {
        return ignoreArtistPrefixes;
    }
    
    public void setIgnoreArtistPrefixes(boolean ignoreArtistPrefixes) {
        this.ignoreArtistPrefixes = ignoreArtistPrefixes;
    }
    
    public boolean isIgnoreFeaturing() {
        return ignoreFeaturing;
    }
    
    public void setIgnoreFeaturing(boolean ignoreFeaturing) {
        this.ignoreFeaturing = ignoreFeaturing;
    }
    
    public boolean isIgnoreAlbumEditions() {
        return ignoreAlbumEditions;
    }
    
    public void setIgnoreAlbumEditions(boolean ignoreAlbumEditions) {
        this.ignoreAlbumEditions = ignoreAlbumEditions;
    }
    
    public int getMinimumFieldsToMatch() {
        return minimumFieldsToMatch;
    }
    
    public void setMinimumFieldsToMatch(int minimumFieldsToMatch) {
        this.minimumFieldsToMatch = Math.max(1, Math.min(4, minimumFieldsToMatch));
    }
    
    public int getBitrateToleranceKbps() {
        return bitrateToleranceKbps;
    }
    
    public void setBitrateToleranceKbps(int bitrateToleranceKbps) {
        this.bitrateToleranceKbps = Math.max(0, bitrateToleranceKbps);
    }
    
    // Utility methods
    
    /**
     * Creates a strict configuration for exact matching.
     */
    public static FuzzySearchConfig createStrictConfig() {
        FuzzySearchConfig config = new FuzzySearchConfig("Strict");
        config.setTitleSimilarityThreshold(100.0);
        config.setArtistSimilarityThreshold(100.0);
        config.setAlbumSimilarityThreshold(100.0);
        config.setDurationToleranceSeconds(0);
        config.setDurationTolerancePercent(0.0);
        config.setTrackNumberMustMatch(true);
        config.setIgnoreMissingTrackNumber(false);
        config.setMinimumFieldsToMatch(4);
        return config;
    }
    
    /**
     * Creates a lenient configuration for loose matching.
     */
    public static FuzzySearchConfig createLenientConfig() {
        FuzzySearchConfig config = new FuzzySearchConfig("Lenient");
        config.setTitleSimilarityThreshold(70.0);
        config.setArtistSimilarityThreshold(75.0);
        config.setAlbumSimilarityThreshold(70.0);
        config.setDurationToleranceSeconds(30);
        config.setDurationTolerancePercent(10.0);
        config.setTrackNumberMustMatch(false);
        config.setIgnoreMissingTrackNumber(true);
        config.setMinimumFieldsToMatch(2);
        config.setIgnoreFeaturing(true);
        return config;
    }
    
    /**
     * Creates a balanced configuration for typical use.
     */
    public static FuzzySearchConfig createBalancedConfig() {
        return new FuzzySearchConfig("Balanced"); // Uses defaults
    }
    
    /**
     * Serializes the configuration to Properties format.
     */
    public Properties toProperties() {
        Properties props = new Properties();
        
        props.setProperty("configName", configName);
        props.setProperty("titleSimilarityThreshold", String.valueOf(titleSimilarityThreshold));
        props.setProperty("artistSimilarityThreshold", String.valueOf(artistSimilarityThreshold));
        props.setProperty("albumSimilarityThreshold", String.valueOf(albumSimilarityThreshold));
        props.setProperty("durationToleranceSeconds", String.valueOf(durationToleranceSeconds));
        props.setProperty("durationTolerancePercent", String.valueOf(durationTolerancePercent));
        props.setProperty("ignoreCaseDifferences", String.valueOf(ignoreCaseDifferences));
        props.setProperty("ignorePunctuation", String.valueOf(ignorePunctuation));
        props.setProperty("wordOrderSensitive", String.valueOf(wordOrderSensitive));
        props.setProperty("trackNumberMustMatch", String.valueOf(trackNumberMustMatch));
        props.setProperty("ignoreMissingTrackNumber", String.valueOf(ignoreMissingTrackNumber));
        props.setProperty("ignoreArtistPrefixes", String.valueOf(ignoreArtistPrefixes));
        props.setProperty("ignoreFeaturing", String.valueOf(ignoreFeaturing));
        props.setProperty("ignoreAlbumEditions", String.valueOf(ignoreAlbumEditions));
        props.setProperty("minimumFieldsToMatch", String.valueOf(minimumFieldsToMatch));
        props.setProperty("bitrateToleranceKbps", String.valueOf(bitrateToleranceKbps));
        
        return props;
    }
    
    /**
     * Creates a configuration from Properties.
     */
    public static FuzzySearchConfig fromProperties(Properties props) {
        String configName = props.getProperty("configName", "Default");
        FuzzySearchConfig config = new FuzzySearchConfig(configName);
        
        try {
            config.setTitleSimilarityThreshold(Double.parseDouble(props.getProperty("titleSimilarityThreshold", String.valueOf(DEFAULT_TITLE_SIMILARITY))));
            config.setArtistSimilarityThreshold(Double.parseDouble(props.getProperty("artistSimilarityThreshold", String.valueOf(DEFAULT_ARTIST_SIMILARITY))));
            config.setAlbumSimilarityThreshold(Double.parseDouble(props.getProperty("albumSimilarityThreshold", String.valueOf(DEFAULT_ALBUM_SIMILARITY))));
            config.setDurationToleranceSeconds(Integer.parseInt(props.getProperty("durationToleranceSeconds", String.valueOf(DEFAULT_DURATION_TOLERANCE_SECONDS))));
            config.setDurationTolerancePercent(Double.parseDouble(props.getProperty("durationTolerancePercent", String.valueOf(DEFAULT_DURATION_TOLERANCE_PERCENT))));
            config.setIgnoreCaseDifferences(Boolean.parseBoolean(props.getProperty("ignoreCaseDifferences", String.valueOf(DEFAULT_IGNORE_CASE))));
            config.setIgnorePunctuation(Boolean.parseBoolean(props.getProperty("ignorePunctuation", String.valueOf(DEFAULT_IGNORE_PUNCTUATION))));
            config.setWordOrderSensitive(Boolean.parseBoolean(props.getProperty("wordOrderSensitive", String.valueOf(DEFAULT_WORD_ORDER_SENSITIVE))));
            config.setTrackNumberMustMatch(Boolean.parseBoolean(props.getProperty("trackNumberMustMatch", String.valueOf(DEFAULT_TRACK_NUMBER_MUST_MATCH))));
            config.setIgnoreMissingTrackNumber(Boolean.parseBoolean(props.getProperty("ignoreMissingTrackNumber", String.valueOf(DEFAULT_IGNORE_MISSING_TRACK_NUMBER))));
            config.setIgnoreArtistPrefixes(Boolean.parseBoolean(props.getProperty("ignoreArtistPrefixes", String.valueOf(DEFAULT_IGNORE_ARTIST_PREFIXES))));
            config.setIgnoreFeaturing(Boolean.parseBoolean(props.getProperty("ignoreFeaturing", String.valueOf(DEFAULT_IGNORE_FEATURING))));
            config.setIgnoreAlbumEditions(Boolean.parseBoolean(props.getProperty("ignoreAlbumEditions", String.valueOf(DEFAULT_IGNORE_ALBUM_EDITIONS))));
            config.setMinimumFieldsToMatch(Integer.parseInt(props.getProperty("minimumFieldsToMatch", String.valueOf(DEFAULT_MIN_FIELDS_MATCH))));
            config.setBitrateToleranceKbps(Integer.parseInt(props.getProperty("bitrateToleranceKbps", String.valueOf(DEFAULT_BITRATE_TOLERANCE))));
        } catch (NumberFormatException e) {
            logger.warn(String.format("Error parsing fuzzy search configuration, using defaults: {}", e.getMessage()));
        }
        
        return config;
    }
    
    /**
     * Creates a copy of this configuration with a new name.
     */
    public FuzzySearchConfig copy(String newName) {
        Properties props = this.toProperties();
        props.setProperty("configName", newName);
        return fromProperties(props);
    }
    
    /**
     * Gets a summary of the configuration.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Fuzzy Search Configuration: ").append(configName).append("\n");
        summary.append("  Title Similarity: ").append(String.format("%.1f%%", titleSimilarityThreshold)).append("\n");
        summary.append("  Artist Similarity: ").append(String.format("%.1f%%", artistSimilarityThreshold)).append("\n");
        summary.append("  Album Similarity: ").append(String.format("%.1f%%", albumSimilarityThreshold)).append("\n");
        summary.append("  Duration Tolerance: ").append(durationToleranceSeconds).append("s / ").append(String.format("%.1f%%", durationTolerancePercent)).append("\n");
        summary.append("  Track Number Match: ").append(trackNumberMustMatch ? "Required" : "Optional").append("\n");
        summary.append("  Min Fields Match: ").append(minimumFieldsToMatch).append("/4").append("\n");
        
        summary.append("  Options: ");
        if (ignoreCaseDifferences) summary.append("IgnoreCase ");
        if (ignorePunctuation) summary.append("IgnorePunct ");
        if (ignoreArtistPrefixes) summary.append("IgnorePrefix ");
        if (ignoreFeaturing) summary.append("IgnoreFeat ");
        if (ignoreAlbumEditions) summary.append("IgnoreEditions ");
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return configName;
    }
}