package org.hasting.util;

import org.hasting.model.MusicFile;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for fuzzy string matching and duplicate detection.
 * Implements various string similarity algorithms and music-specific matching logic.
 */
public class FuzzyMatcher {
    
    // Common patterns for text normalization
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern ARTIST_PREFIX_PATTERN = Pattern.compile("^(the|a|an)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FEATURING_PATTERN = Pattern.compile("\\s+(feat\\.?|ft\\.?|featuring)\\s+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALBUM_EDITION_PATTERN = Pattern.compile("\\s+\\(?(deluxe|remastered|special|limited|extended|expanded|anniversary|collector's?)\\s*(edition|version)?\\)?.*$", Pattern.CASE_INSENSITIVE);
    
    /**
     * Calculates the similarity between two music files based on fuzzy search configuration.
     */
    public static double calculateSimilarity(MusicFile file1, MusicFile file2, FuzzySearchConfig config) {
        if (file1 == null || file2 == null || config == null) {
            return 0.0;
        }
        
        double titleSim = calculateFieldSimilarity(file1.getTitle(), file2.getTitle(), config, FieldType.TITLE);
        double artistSim = calculateFieldSimilarity(file1.getArtist(), file2.getArtist(), config, FieldType.ARTIST);
        double albumSim = calculateFieldSimilarity(file1.getAlbum(), file2.getAlbum(), config, FieldType.ALBUM);
        
        // Count matching fields
        int matchingFields = 0;
        if (titleSim >= config.getTitleSimilarityThreshold()) matchingFields++;
        if (artistSim >= config.getArtistSimilarityThreshold()) matchingFields++;
        if (albumSim >= config.getAlbumSimilarityThreshold()) matchingFields++;
        
        // Check duration
        boolean durationMatches = checkDurationMatch(file1, file2, config);
        if (durationMatches) matchingFields++;
        
        // Check track number if required
        if (config.isTrackNumberMustMatch()) {
            boolean trackMatches = checkTrackNumberMatch(file1, file2, config);
            if (!trackMatches) {
                return 0.0; // Fail immediately if track numbers don't match when required
            }
        }
        
        // Check minimum fields requirement
        if (matchingFields < config.getMinimumFieldsToMatch()) {
            return 0.0;
        }
        
        // Calculate weighted average (equal weights for now)
        return (titleSim + artistSim + albumSim) / 3.0;
    }
    
    /**
     * Checks if two music files are duplicates based on fuzzy search configuration.
     */
    public static boolean areDuplicates(MusicFile file1, MusicFile file2, FuzzySearchConfig config) {
        double similarity = calculateSimilarity(file1, file2, config);
        
        // Consider duplicates if:
        // 1. Individual fields meet their thresholds
        // 2. Minimum number of fields match
        // 3. Duration is within tolerance
        // 4. Track numbers match (if required)
        
        double titleSim = calculateFieldSimilarity(file1.getTitle(), file2.getTitle(), config, FieldType.TITLE);
        double artistSim = calculateFieldSimilarity(file1.getArtist(), file2.getArtist(), config, FieldType.ARTIST);
        double albumSim = calculateFieldSimilarity(file1.getAlbum(), file2.getAlbum(), config, FieldType.ALBUM);
        
        boolean titleMatches = titleSim >= config.getTitleSimilarityThreshold();
        boolean artistMatches = artistSim >= config.getArtistSimilarityThreshold();
        boolean albumMatches = albumSim >= config.getAlbumSimilarityThreshold();
        boolean durationMatches = checkDurationMatch(file1, file2, config);
        
        int matchingFields = 0;
        if (titleMatches) matchingFields++;
        if (artistMatches) matchingFields++;
        if (albumMatches) matchingFields++;
        if (durationMatches) matchingFields++;
        
        // Check track number requirement
        if (config.isTrackNumberMustMatch()) {
            boolean trackMatches = checkTrackNumberMatch(file1, file2, config);
            if (!trackMatches) return false;
        }
        
        return matchingFields >= config.getMinimumFieldsToMatch();
    }
    
    /**
     * Calculates similarity between two field values based on field type and configuration.
     */
    private static double calculateFieldSimilarity(String value1, String value2, FuzzySearchConfig config, FieldType fieldType) {
        if (value1 == null && value2 == null) return 100.0;
        if (value1 == null || value2 == null) return 0.0;
        if (value1.trim().isEmpty() && value2.trim().isEmpty()) return 100.0;
        if (value1.trim().isEmpty() || value2.trim().isEmpty()) return 0.0;
        
        // Normalize strings based on configuration and field type
        String normalized1 = normalizeString(value1, config, fieldType);
        String normalized2 = normalizeString(value2, config, fieldType);
        
        if (normalized1.equals(normalized2)) {
            return 100.0;
        }
        
        // Use Jaro-Winkler similarity for better results with names and titles
        return jaroWinklerSimilarity(normalized1, normalized2) * 100.0;
    }
    
    /**
     * Normalizes a string based on configuration and field type.
     */
    private static String normalizeString(String value, FuzzySearchConfig config, FieldType fieldType) {
        if (value == null) return "";
        
        String normalized = value.trim();
        
        // Apply case normalization
        if (config.isIgnoreCaseDifferences()) {
            normalized = normalized.toLowerCase();
        }
        
        // Apply field-specific normalizations
        switch (fieldType) {
            case ARTIST:
                if (config.isIgnoreArtistPrefixes()) {
                    normalized = ARTIST_PREFIX_PATTERN.matcher(normalized).replaceFirst("");
                }
                if (config.isIgnoreFeaturing()) {
                    normalized = FEATURING_PATTERN.matcher(normalized).replaceFirst("");
                }
                break;
                
            case ALBUM:
                if (config.isIgnoreAlbumEditions()) {
                    normalized = ALBUM_EDITION_PATTERN.matcher(normalized).replaceFirst("");
                }
                break;
                
            case TITLE:
                // Title-specific normalizations could go here
                break;
        }
        
        // Apply punctuation normalization
        if (config.isIgnorePunctuation()) {
            normalized = PUNCTUATION_PATTERN.matcher(normalized).replaceAll(" ");
        }
        
        // Normalize whitespace
        normalized = WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();
        
        return normalized;
    }
    
    /**
     * Checks if two files have matching durations within tolerance.
     */
    private static boolean checkDurationMatch(MusicFile file1, MusicFile file2, FuzzySearchConfig config) {
        Integer duration1 = file1.getDurationSeconds();
        Integer duration2 = file2.getDurationSeconds();
        
        // If either duration is missing, consider it a match if configured to ignore
        if (duration1 == null || duration2 == null) {
            return true; // Could be configurable in the future
        }
        
        int diff = Math.abs(duration1 - duration2);
        
        // Check absolute tolerance
        if (diff <= config.getDurationToleranceSeconds()) {
            return true;
        }
        
        // Check percentage tolerance
        double avgDuration = (duration1 + duration2) / 2.0;
        double percentDiff = (diff / avgDuration) * 100.0;
        
        return percentDiff <= config.getDurationTolerancePercent();
    }
    
    /**
     * Checks if two files have matching track numbers.
     */
    private static boolean checkTrackNumberMatch(MusicFile file1, MusicFile file2, FuzzySearchConfig config) {
        Integer track1 = file1.getTrackNumber();
        Integer track2 = file2.getTrackNumber();
        
        // If either track number is missing and we're configured to ignore missing
        if ((track1 == null || track2 == null) && config.isIgnoreMissingTrackNumber()) {
            return true;
        }
        
        // If both are present, they must match exactly
        return Objects.equals(track1, track2);
    }
    
    /**
     * Implements Jaro-Winkler string similarity algorithm.
     * Returns a value between 0.0 and 1.0, where 1.0 is an exact match.
     */
    public static double jaroWinklerSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        
        double jaro = jaroSimilarity(s1, s2);
        if (jaro < 0.7) return jaro; // Only apply Winkler bonus if Jaro similarity is high enough
        
        // Calculate common prefix length (up to 4 characters)
        int prefixLength = 0;
        int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));
        
        for (int i = 0; i < maxPrefix; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }
        
        // Apply Winkler bonus (0.1 scaling factor)
        return jaro + (0.1 * prefixLength * (1.0 - jaro));
    }
    
    /**
     * Implements Jaro string similarity algorithm.
     */
    private static double jaroSimilarity(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        if (len1 == 0 && len2 == 0) return 1.0;
        if (len1 == 0 || len2 == 0) return 0.0;
        
        // Calculate the maximum allowed distance for matches
        int matchWindow = Math.max(len1, len2) / 2 - 1;
        if (matchWindow < 0) matchWindow = 0;
        
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];
        
        int matches = 0;
        
        // Find matches
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(i + matchWindow + 1, len2);
            
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }
        
        if (matches == 0) return 0.0;
        
        // Count transpositions
        int transpositions = 0;
        int k = 0;
        
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }
        
        // Calculate Jaro similarity
        return (matches / (double) len1 + matches / (double) len2 + 
                (matches - transpositions / 2.0) / matches) / 3.0;
    }
    
    /**
     * Implements Levenshtein distance algorithm for exact edit distance.
     * Returns the number of single-character edits needed to transform one string into another.
     */
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Math.max(s1 != null ? s1.length() : 0, s2 != null ? s2.length() : 0);
        if (s1.equals(s2)) return 0;
        
        int len1 = s1.length();
        int len2 = s2.length();
        
        // Create a matrix to store distances
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        // Initialize first row and column
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;
        
        // Fill the matrix
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Converts Levenshtein distance to similarity percentage.
     */
    public static double levenshteinSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 100.0;
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 100.0;
        
        int distance = levenshteinDistance(s1, s2);
        return ((maxLength - distance) / (double) maxLength) * 100.0;
    }
    
    /**
     * Filters a list of music files to find potential duplicates based on fuzzy configuration.
     */
    public static List<MusicFile> findFuzzyDuplicates(List<MusicFile> musicFiles, FuzzySearchConfig config) {
        if (musicFiles == null || musicFiles.isEmpty() || config == null) {
            return new ArrayList<>();
        }
        
        Set<MusicFile> duplicates = new HashSet<>();
        
        for (int i = 0; i < musicFiles.size(); i++) {
            MusicFile file1 = musicFiles.get(i);
            
            for (int j = i + 1; j < musicFiles.size(); j++) {
                MusicFile file2 = musicFiles.get(j);
                
                if (areDuplicates(file1, file2, config)) {
                    duplicates.add(file1);
                    duplicates.add(file2);
                }
            }
        }
        
        return new ArrayList<>(duplicates);
    }
    
    /**
     * Groups duplicate music files together.
     */
    public static List<List<MusicFile>> groupDuplicates(List<MusicFile> musicFiles, FuzzySearchConfig config) {
        Map<MusicFile, List<MusicFile>> duplicateGroups = new HashMap<>();
        Set<MusicFile> processed = new HashSet<>();
        
        for (int i = 0; i < musicFiles.size(); i++) {
            MusicFile file1 = musicFiles.get(i);
            if (processed.contains(file1)) continue;
            
            List<MusicFile> group = new ArrayList<>();
            group.add(file1);
            processed.add(file1);
            
            for (int j = i + 1; j < musicFiles.size(); j++) {
                MusicFile file2 = musicFiles.get(j);
                if (processed.contains(file2)) continue;
                
                if (areDuplicates(file1, file2, config)) {
                    group.add(file2);
                    processed.add(file2);
                }
            }
            
            if (group.size() > 1) {
                duplicateGroups.put(file1, group);
            }
        }
        
        return new ArrayList<>(duplicateGroups.values());
    }
    
    /**
     * Field types for string normalization.
     */
    private enum FieldType {
        TITLE, ARTIST, ALBUM
    }
    
    /**
     * Debug method to show similarity breakdown.
     */
    public static String getSimilarityBreakdown(MusicFile file1, MusicFile file2, FuzzySearchConfig config) {
        StringBuilder breakdown = new StringBuilder();
        
        double titleSim = calculateFieldSimilarity(file1.getTitle(), file2.getTitle(), config, FieldType.TITLE);
        double artistSim = calculateFieldSimilarity(file1.getArtist(), file2.getArtist(), config, FieldType.ARTIST);
        double albumSim = calculateFieldSimilarity(file1.getAlbum(), file2.getAlbum(), config, FieldType.ALBUM);
        boolean durationMatch = checkDurationMatch(file1, file2, config);
        boolean trackMatch = checkTrackNumberMatch(file1, file2, config);
        
        breakdown.append("Similarity Breakdown:\n");
        breakdown.append(String.format("  Title: %.1f%% (threshold: %.1f%%)\n", titleSim, config.getTitleSimilarityThreshold()));
        breakdown.append(String.format("  Artist: %.1f%% (threshold: %.1f%%)\n", artistSim, config.getArtistSimilarityThreshold()));
        breakdown.append(String.format("  Album: %.1f%% (threshold: %.1f%%)\n", albumSim, config.getAlbumSimilarityThreshold()));
        breakdown.append(String.format("  Duration: %s\n", durationMatch ? "MATCH" : "NO MATCH"));
        breakdown.append(String.format("  Track: %s\n", trackMatch ? "MATCH" : "NO MATCH"));
        
        boolean isDuplicate = areDuplicates(file1, file2, config);
        breakdown.append(String.format("  Result: %s\n", isDuplicate ? "DUPLICATE" : "NOT DUPLICATE"));
        
        return breakdown.toString();
    }
}