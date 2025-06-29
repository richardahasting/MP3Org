package org.hasting.util;

import java.util.*;

/**
 * Utility class for managing artist statistics and subdirectory grouping calculations.
 * Handles artist counting, alphabet-based grouping, and directory organization for music files.
 */
public class ArtistStatisticsManager {
    
    private static final int DEFAULT_NUMBER_OF_SUBDIRECTORIES = 7;
    
    private static Set<String> artistNames = new HashSet<>(Arrays.asList("Unknown"));
    private static Set<String> albumNames = new HashSet<>(Arrays.asList("Unknown"));
    private static Set<String> genreNames = new HashSet<>(Arrays.asList("Unknown"));

    private static HashMap<String, Integer> firstFieldMap = new HashMap<>();
    private static HashMap<String, Integer> albumMap = new HashMap<>();
    private static HashMap<String, Integer> genreMap = new HashMap<>();
    private static HashMap<String, Integer> initialsMap = new HashMap<>();
    private static int totalFirstFieldWithAlphaNames = 0;
    private static int totalAlbumNamesWithAlphaNames = 0;
    private static int totalGenreNamesWithAlphaNames = 0;

    private static HashMap<String, String> initials = new HashMap<>();
    private static int numberOfSubdirectories = DEFAULT_NUMBER_OF_SUBDIRECTORIES;
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ArtistStatisticsManager() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Gets the current number of subdirectories used for grouping.
     * 
     * @return The number of subdirectories
     */
    public static int getNumberOfSubdirectories() {
        return numberOfSubdirectories;
    }

    /**
     * Sets the number of subdirectories to use for grouping.
     * 
     * @param numberOfSubdirectories The number of subdirectories
     */
    public static void setNumberOfSubdirectories(int numberOfSubdirectories) {
        ArtistStatisticsManager.numberOfSubdirectories = numberOfSubdirectories;
    }
    
    /**
     * Adds an artist name to the collection of known artists.
     * 
     * @param artistName The artist name to add
     */
    public static void addArtist(String artistName) {
        if (artistName != null) {
            artistNames.add(artistName);
        }
    }
    
    /**
     * Adds an album name to the collection of known albums.
     * 
     * @param albumName The album name to add
     */
    public static void addAlbum(String albumName) {
        if (albumName != null) {
            albumNames.add(albumName);
        }
    }
    
    /**
     * Adds a genre name to the collection of known genres.
     * 
     * @param genreName The genre name to add
     */
    public static void addGenre(String genreName) {
        if (genreName != null) {
            genreNames.add(genreName);
        }
    }
    
    /**
     * Gets a copy of all known artist names.
     * 
     * @return A set containing all artist names
     */
    public static Set<String> getArtistNames() {
        return new HashSet<>(artistNames);
    }
    
    /**
     * Gets a copy of all known album names.
     * 
     * @return A set containing all album names
     */
    public static Set<String> getAlbumNames() {
        return new HashSet<>(albumNames);
    }
    
    /**
     * Gets a copy of all known genre names.
     * 
     * @return A set containing all genre names
     */
    public static Set<String> getGenreNames() {
        return new HashSet<>(genreNames);
    }

    /**
     * Resets all artist counts and collections to their initial state.
     * Clears all maps and sets to prepare for fresh data collection.
     */
    public static void resetArtistCounts() {
        firstFieldMap.clear();
        totalFirstFieldWithAlphaNames = 0;
        totalAlbumNamesWithAlphaNames = 0;
        totalGenreNamesWithAlphaNames = 0;
        artistNames.clear();
        albumNames.clear();
        genreNames.clear();
        initials.clear();
        albumMap.clear();
        genreMap.clear();
        
        // Reset to default values
        artistNames.add("Unknown");
        albumNames.add("Unknown");
        genreNames.add("Unknown");
    }

    /**
     * Counts the first field (typically artist names) by their initial letters.
     * This method analyzes all artist names and creates a frequency map based on their first letters.
     * Only considers names that start with alphabetic characters.
     */
    private static void countFirstFieldByLetters() {
        List<String> artists = new ArrayList<>(artistNames);
        totalFirstFieldWithAlphaNames = 0;
        for (String artist : artists) {
            if (artist != null && !artist.isEmpty()) {
                String artistInitial = artist.substring(0, 1).toUpperCase();
                if (artistInitial.matches("[A-Z]")) { // only count artists with alphabetic names
                    Integer count = firstFieldMap.getOrDefault(artistInitial, 0);
                    totalFirstFieldWithAlphaNames++;
                    System.out.println(String.format("%s: %d", artistInitial, count));
                    count++;
                    firstFieldMap.put(artistInitial, count);
                }
            }
        }
    }

    /**
     * Calculates the initial letter mappings for subdirectory grouping.
     * This method groups alphabetic ranges to create a balanced distribution
     * across the specified number of subdirectories.
     * 
     * @return A map where keys are individual letters and values are directory range strings (e.g., "A-C", "D-F")
     */
    public static synchronized HashMap<String, String> calculateInitials() {
        if (firstFieldMap.isEmpty()) {
            countFirstFieldByLetters();
        }
        int totalDirsCreated = 0;
        initials = new HashMap<>();
        final int targetTally = totalFirstFieldWithAlphaNames / numberOfSubdirectories - 5; // Target items per directory

        // Create directories for artists, format a-c, d-f ... and misc artists go to "misc" 
        for (char currentChar = 'A'; currentChar <= 'Z'; ) {
            char startingChar = currentChar;
            int currentTally = 0;
            while ((totalDirsCreated >= numberOfSubdirectories || currentTally < targetTally) && currentChar <= 'Z') {
                Integer artistCounterAtCurrentChar = firstFieldMap.getOrDefault(Character.toString(currentChar), 0);
                if (totalDirsCreated < numberOfSubdirectories && artistCounterAtCurrentChar / 2 + currentTally > targetTally) {
                    currentTally = targetTally + 1;
                } else {
                    currentTally += artistCounterAtCurrentChar;
                    currentChar++;
                }
            }
            totalDirsCreated++;
            char endingChar = currentChar;
            if (endingChar > startingChar) {
                endingChar--; // adjust endingChar to account for last artist in the range
            }
            String initialString = startingChar + "-" + endingChar;
            if (startingChar == endingChar) {
                initialString = "" + startingChar;
            }
            while (startingChar <= endingChar) {
                initials.put(Character.toString(startingChar), initialString);
                startingChar++;
            }
        }
        return new HashMap<>(initials);
    }
    
    /**
     * Gets the current initial letter mappings.
     * If the mappings haven't been calculated yet, this triggers the calculation.
     * 
     * @return A map where keys are individual letters and values are directory range strings
     */
    public static HashMap<String, String> getInitials() {
        if (initials.isEmpty()) {
            return calculateInitials();
        }
        return new HashMap<>(initials);
    }
    
    /**
     * Gets the directory range string for a given initial letter.
     * For example, if 'B' maps to "A-C", this method will return "A-C".
     * 
     * @param initial The initial letter to look up
     * @return The directory range string, or null if not found
     */
    public static String getDirectoryRangeForInitial(String initial) {
        if (initial == null || initial.isEmpty()) {
            return null;
        }
        
        if (initials.isEmpty()) {
            calculateInitials();
        }
        
        return initials.get(initial.toUpperCase());
    }
    
    /**
     * Gets statistics about the current artist distribution.
     * 
     * @return A map containing various statistics about artists, albums, and genres
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalArtists", artistNames.size());
        stats.put("totalAlbums", albumNames.size());
        stats.put("totalGenres", genreNames.size());
        stats.put("totalFirstFieldWithAlphaNames", totalFirstFieldWithAlphaNames);
        stats.put("totalAlbumNamesWithAlphaNames", totalAlbumNamesWithAlphaNames);
        stats.put("totalGenreNamesWithAlphaNames", totalGenreNamesWithAlphaNames);
        stats.put("numberOfSubdirectories", numberOfSubdirectories);
        stats.put("firstFieldMapSize", firstFieldMap.size());
        return stats;
    }
    
    /**
     * Clears all statistics and resets to initial state.
     * This is equivalent to calling resetArtistCounts().
     */
    public static void clearAll() {
        resetArtistCounts();
    }
}