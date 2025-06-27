package org.hasting.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class StringUtils {

    private static final LevenshteinDistance levenshtein = new LevenshteinDistance();

    /**
     * Calculates a fuzzy match score between two strings by comparing their word sets
     * and allowing for minor misspellings. The score is normalized to a range between
     * -100 and 100, where -100 indicates maximum dissimilarity and 100 indicates
     * maximum similarity.
     * 
     * @param str1 The first string to compare. It can be any non-null string.
     * @param str2 The second string to compare. It can be any non-null string.
     * @return An integer score between -100 and 100 indicating the similarity
     *         between the two strings. A score of -100 is returned if either string
     *         is null, indicating maximum difference. A score of 0 indicates no
     *         similarity, while a score of 100 indicates perfect similarity.
     */
    public static int fuzzyMatch(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return -100; // Return -100 for maximum difference if either string is null
        }

        // Normalize case
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

        // Tokenize strings into words using additional delimiters
        Set<String> words1 = new HashSet<>(Arrays.asList(str1.split("[\\s',.:-_+]+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(str2.split("[\\s',.:-_+]+")));

        // Calculate intersection and union of word sets with allowance for misspellings
        int similarWordsCount = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                int distance = levenshtein.apply(word1, word2);
                int maxLength = Math.max(word1.length(), word2.length());
                // Consider words similar if the Levenshtein distance is less than a threshold
                if (distance <= maxLength * 0.2) { // Allow up to 20% of the word length as edits
                    similarWordsCount++;
                    break;
                }
            }
        }

        int totalWords = words1.size() + words2.size() - similarWordsCount;
        if (totalWords == 0) {
            return 0; // Return 0 for perfect match if both sets are empty
        }

        double similarity = (double) similarWordsCount / totalWords;
        int similarityScore = (int) (similarity * 100);

        // Map similarity score to range -100 to 100
        return (similarityScore * 2) - 100;
    }
}