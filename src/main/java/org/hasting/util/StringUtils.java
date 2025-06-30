package org.hasting.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.text.Normalizer;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class StringUtils {

    private static final LevenshteinDistance levenshtein = new LevenshteinDistance();

    /**
     * Calculates a fuzzy match score between two strings by comparing their word sets
     * and allowing for minor misspellings. The score is normalized to a range between
     * 0 and 100, where 0 indicates maximum dissimilarity and 100 indicates
     * maximum similarity.
     * 
     * @param str1 The first string to compare. It can be any non-null string.
     * @param str2 The second string to compare. It can be any non-null string.
     * @return An integer score between 0 and 100 indicating the similarity
     *         between the two strings. A score of 0 is returned if either string
     *         is null, indicating maximum difference. A score of 100 indicates 
     *         perfect similarity.
     */
    public static int fuzzyMatch(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0; // Return 0 for maximum difference if either string is null
        }

        // Normalize strings for comparison
        String normalized1 = normalizeForComparison(str1);
        String normalized2 = normalizeForComparison(str2);
        
        // Handle exact matches after normalization
        if (normalized1.equals(normalized2)) {
            return 100;
        }
        
        // Calculate multiple similarity metrics and use the best one
        int charScore = calculateCharacterSimilarity(normalized1, normalized2);
        int wordScore = calculateWordSimilarity(normalized1, normalized2);
        int substringScore = calculateSubstringSimilarity(normalized1, normalized2);
        
        // Return the highest score among all methods
        return Math.max(charScore, Math.max(wordScore, substringScore));
    }
    
    /**
     * Calculates similarity based on character-level Levenshtein distance
     */
    private static int calculateCharacterSimilarity(String str1, String str2) {
        int distance = levenshtein.apply(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        
        if (maxLength == 0) {
            return 100;
        }
        
        double similarity = 1.0 - ((double) distance / maxLength);
        return Math.max(0, (int) (similarity * 100));
    }
    
    /**
     * Calculates similarity based on word overlap (handles rearrangements)
     */
    private static int calculateWordSimilarity(String str1, String str2) {
        String[] words1 = str1.split("\\s+");
        String[] words2 = str2.split("\\s+");
        
        if (words1.length == 0 && words2.length == 0) {
            return 100;
        }
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        // Calculate intersection
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        // Calculate union
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) {
            return 100;
        }
        
        double jaccardSimilarity = (double) intersection.size() / union.size();
        return (int) (jaccardSimilarity * 100);
    }
    
    /**
     * Calculates similarity based on substring containment (handles partial matches)
     */
    private static int calculateSubstringSimilarity(String str1, String str2) {
        String shorter = str1.length() <= str2.length() ? str1 : str2;
        String longer = str1.length() > str2.length() ? str1 : str2;
        
        if (shorter.isEmpty()) {
            return longer.isEmpty() ? 100 : 0;
        }
        
        if (longer.contains(shorter)) {
            // Give good score for full substring containment
            return Math.max(50, (int) (((double) shorter.length() / longer.length()) * 100));
        }
        
        // Check for partial substring matches
        int maxMatchLength = 0;
        for (int i = 0; i < shorter.length(); i++) {
            for (int j = i + 1; j <= shorter.length(); j++) {
                String substring = shorter.substring(i, j);
                if (substring.length() > 2 && longer.contains(substring)) {
                    maxMatchLength = Math.max(maxMatchLength, substring.length());
                }
            }
        }
        
        if (maxMatchLength > 0) {
            return Math.max(20, (int) (((double) maxMatchLength / shorter.length()) * 80));
        }
        
        return 0;
    }
    
    /**
     * Normalizes a string for fuzzy comparison by:
     * - Converting to lowercase
     * - Removing common articles (the, a, an)
     * - Removing extra whitespace
     * - Removing common punctuation
     * - Trimming
     */
    private static String normalizeForComparison(String str) {
        if (str == null) {
            return "";
        }
        
        // Normalize Unicode characters (remove diacritics/accents)
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
                                     .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        return normalized.toLowerCase()
                  .replaceAll("^(the|a|an)\\s+", "")     // Remove leading articles
                  .replaceAll("\\s*&\\s*", " and ")      // Replace & with "and"
                  .replaceAll("\\s*\\+\\s*", " and ")     // Replace + with "and" 
                  .replaceAll("[\\s',.:-_/()\\[\\]]+", " ") // Replace delimiters and brackets with space
                  .replaceAll("\\s+", " ")               // Collapse multiple spaces
                  .trim();                               // Remove leading/trailing whitespace
    }
}