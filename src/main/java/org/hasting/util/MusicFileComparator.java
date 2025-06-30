package org.hasting.util;

import org.hasting.model.MusicFile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class providing various comparison and similarity matching operations for MusicFile objects.
 * Handles fuzzy matching, exact matching, and similarity ranking for duplicate detection and sorting.
 */
public class MusicFileComparator {
    
    private static final int DEFAULT_NUM_OF_SIMILAR_FILES = 10;
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MusicFileComparator() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Creates a comparator for MusicFile objects that compares based on fuzzy match scores
     * of both the artist and the title, and considers track numbers if available.
     * 
     * @param target The target MusicFile to compare against
     * @return A Comparator that sorts by similarity to the target
     */
    public static Comparator<MusicFile> createFuzzyComparator(MusicFile target) {
        return (MusicFile mf1, MusicFile mf2) -> {
            // Fuzzy match scores for artist and title
            int artistScore1 = StringUtils.fuzzyMatch(mf1.getArtist(), target.getArtist());
            int artistScore2 = StringUtils.fuzzyMatch(mf2.getArtist(), target.getArtist());

            int titleScore1 = StringUtils.fuzzyMatch(mf1.getTitle(), target.getTitle());
            int titleScore2 = StringUtils.fuzzyMatch(mf2.getTitle(), target.getTitle());

            int totalScore1 = artistScore1 + titleScore1;
            int totalScore2 = artistScore2 + titleScore2;

            // Higher total score means closer match, so we reverse the order for descending
            return Integer.compare(totalScore2, totalScore1);
        };
    }
    
    /**
     * Creates a comparator for MusicFile objects that compares based on exact matches
     * of the artist and/or the title, ignoring whitespace.
     *
     * @param target        The target MusicFile to compare against
     * @param matchByTitle  If true, compare by title
     * @param matchByArtist If true, compare by artist
     * @return A Comparator that prioritizes exact matches
     */
    public static Comparator<MusicFile> createExactMatchComparator(MusicFile target, boolean matchByTitle, boolean matchByArtist) {
        return (MusicFile mf1, MusicFile mf2) -> {
            int matchScore1 = 0;
            int matchScore2 = 0;

            if (matchByTitle) {
                String targetTitle = target.getTitle().replaceAll("\\s+", "").toLowerCase();
                if (mf1.getTitle().replaceAll("\\s+", "").equalsIgnoreCase(targetTitle)) {
                    matchScore1++;
                }
                if (mf2.getTitle().replaceAll("\\s+", "").equalsIgnoreCase(targetTitle)) {
                    matchScore2++;
                }
            }

            if (matchByArtist) {
                String targetArtist = target.getArtist().replaceAll("\\s+", "").toLowerCase();
                if (mf1.getArtist().replaceAll("\\s+", "").equalsIgnoreCase(targetArtist)) {
                    matchScore1++;
                }
                if (mf2.getArtist().replaceAll("\\s+", "").equalsIgnoreCase(targetArtist)) {
                    matchScore2++;
                }
            }

            // Higher match score means closer match
            return Integer.compare(matchScore2, matchScore1);
        };
    }
    
    /**
     * Finds the top 'num' most similar MusicFile objects to the target MusicFile
     * from the provided list, based on fuzzy match scores.
     *
     * @param musicFiles The list of MusicFile objects to search through
     * @param target     The target MusicFile to compare against
     * @param num        The number of most similar MusicFile objects to return
     * @return A list of the 'num' most similar MusicFile objects
     */
    public static List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles, MusicFile target, int num) {
        final String currentFilePath = target.getFilePath();
        return musicFiles.stream()
                .filter(mf -> {
                    // Handle null file paths gracefully
                    String mfPath = mf.getFilePath();
                    if (currentFilePath == null && mfPath == null) {
                        // If both paths are null, consider them different objects (for testing)
                        return mf != target;
                    }
                    return currentFilePath != null && !currentFilePath.equals(mfPath);
                })
                .sorted(createFuzzyComparator(target))
                .limit(num)
                .collect(Collectors.toList());
    }
    
    /**
     * Finds the most similar MusicFile objects using default number of results.
     * 
     * @param musicFiles The list of MusicFile objects to search through
     * @param target     The target MusicFile to compare against
     * @return A list of the most similar MusicFile objects
     */
    public static List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles, MusicFile target) {
        return findMostSimilarFiles(musicFiles, target, DEFAULT_NUM_OF_SIMILAR_FILES);
    }
    
    /**
     * Determines if the current MusicFile is likely the same song as the given MusicFile.
     * Uses fuzzy matching on multiple fields and duration comparison for accurate detection.
     *
     * @param file1 The first MusicFile to compare
     * @param file2 The second MusicFile to compare
     * @return true if the songs are likely the same, false otherwise
     */
    public static boolean isLikelyTheSameSong(MusicFile file1, MusicFile file2) {
        if (file1 == null || file2 == null) {
            return false;
        }

        if (file1.getTitle() == null || file2.getTitle() == null) {
            return false;
        }
        if (file1.getArtist() == null || file2.getArtist() == null) {
            return false;
        }
        if (file1.getAlbum() == null || file2.getAlbum() == null) {
            return false;
        }

        // Check track numbers with album context
        if (file1.getTrackNumber() != null && file2.getTrackNumber() != null &&
                file1.getTrackNumber() != 0 && file2.getTrackNumber() != 0 &&
                !file1.getTrackNumber().equals(file2.getTrackNumber())) {
            
            // Only exclude if albums are similar - different tracks on same album = different songs
            if (file1.getAlbum() != null && file2.getAlbum() != null) {
                int albumSimilarity = StringUtils.fuzzyMatch(file1.getAlbum(), file2.getAlbum());
                if (albumSimilarity > 70) {
                    return false; // Different tracks on same/similar album = definitely different songs
                }
            }
            // If albums are different or unknown, track numbers might not matter (compilations, etc.)
        }

        // Fuzzy match scores for title and artist
        int titleScore = StringUtils.fuzzyMatch(file1.getTitle(), file2.getTitle());
        int artistScore = StringUtils.fuzzyMatch(file1.getArtist(), file2.getArtist());

        // Check if duration is within 10%
        boolean durationMatch = false;
        int durationDifference = 1000;
        if (file1.getDurationSeconds() != null && file2.getDurationSeconds() != null) {
            durationDifference = Math.abs(file1.getDurationSeconds() - file2.getDurationSeconds());
            int maxAllowedDifference = (int) (0.05 * Math.max(file1.getDurationSeconds(), file2.getDurationSeconds()));
            durationMatch = durationDifference <= maxAllowedDifference;
        }

        // Fuzzy match score for album if not null
        int albumScore = 0;
        if (file1.getAlbum() != null && file2.getAlbum() != null) {
            albumScore = StringUtils.fuzzyMatch(file1.getAlbum(), file2.getAlbum());
        }

        // Check for strong track number evidence
        boolean trackNumbersMatch = file1.getTrackNumber() != null && file2.getTrackNumber() != null &&
                file1.getTrackNumber() != 0 && file2.getTrackNumber() != 0 &&
                file1.getTrackNumber().equals(file2.getTrackNumber());
        
        // Determine if it's likely the same song based on scores and duration match
        if (durationMatch && durationDifference < 3 && titleScore > 90) {
            return true;
        }
        
        // If track numbers match on same album, lower the threshold for other checks
        if (trackNumbersMatch && albumScore > 70 && durationMatch) {
            return titleScore > 80 && artistScore > 80; // Lower thresholds with track number confidence
        }
        
        return titleScore > 90 && artistScore > 90 && durationMatch && (file1.getAlbum() == null || albumScore > 90);
    }
    
    /**
     * Calculates a similarity score between two MusicFile objects.
     * Higher scores indicate greater similarity.
     * 
     * @param file1 The first MusicFile to compare
     * @param file2 The second MusicFile to compare
     * @return A similarity score (higher = more similar)
     */
    public static double calculateSimilarityScore(MusicFile file1, MusicFile file2) {
        if (file1 == null || file2 == null) {
            return 0.0;
        }
        
        double titleScore = 0.0;
        double artistScore = 0.0;
        double albumScore = 0.0;
        double durationScore = 0.0;
        
        // Title similarity (40% weight)
        if (file1.getTitle() != null && file2.getTitle() != null) {
            titleScore = StringUtils.fuzzyMatch(file1.getTitle(), file2.getTitle()) * 0.4;
        }
        
        // Artist similarity (30% weight)
        if (file1.getArtist() != null && file2.getArtist() != null) {
            artistScore = StringUtils.fuzzyMatch(file1.getArtist(), file2.getArtist()) * 0.3;
        }
        
        // Album similarity (20% weight)
        if (file1.getAlbum() != null && file2.getAlbum() != null) {
            albumScore = StringUtils.fuzzyMatch(file1.getAlbum(), file2.getAlbum()) * 0.2;
        }
        
        // Duration similarity (10% weight)
        if (file1.getDurationSeconds() != null && file2.getDurationSeconds() != null) {
            int diff = Math.abs(file1.getDurationSeconds() - file2.getDurationSeconds());
            int maxDuration = Math.max(file1.getDurationSeconds(), file2.getDurationSeconds());
            if (maxDuration > 0) {
                double durationSimilarity = Math.max(0, 100 - (diff * 100.0 / maxDuration));
                durationScore = durationSimilarity * 0.1;
            }
        }
        
        return titleScore + artistScore + albumScore + durationScore;
    }
}