package org.hasting.model;

import org.hasting.util.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MusicFile {

    final private static int DEFAULT_NUM_OF_SIMILAR_FILES = 10;

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

    private Long id;
    private String filePath;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer trackNumber;
    private Integer year;
    private Integer durationSeconds;
    private Long fileSizeBytes;
    private Long bitRate;
    private Integer sampleRate;
    private String fileType;
    private Date lastModified;
    private Date dateAdded;
    private boolean isModified = false;
    
    // Field for testing file organization paths without actual file copying
    private String organizationalPath;

    public enum Fields {
        TITLE, ARTIST, ALBUM, GENRE, TRACK_NUMBER, YEAR, BIT_RATE, SAMPLE_RATE;
    }
    public static int getNumberOfSubdirectorys() {
        return numberOfSubdirectorys;
    }

    public static void setNumberOfSubdirectorys(int numberOfSubdirectorys) {
        MusicFile.numberOfSubdirectorys = numberOfSubdirectorys;
    }

    private static int numberOfSubdirectorys = 7;

    public String getField (Fields field) {
        String value = null;
        switch (field) {
            case TITLE:
                value = title;
                break;
            case ARTIST:
                value = artist;
                break;
            case ALBUM:
                value = album;
                break;
            case GENRE:
                value = genre;
                break;
            case TRACK_NUMBER:
                value = trackNumber == null? "" : trackNumber.toString();
                break;
            case YEAR:
                value = year == null? "" : year.toString();
                break;
            case BIT_RATE:
                value = bitRate == null? "" : bitRate.toString();
                break;
            case SAMPLE_RATE:
                value = sampleRate == null? "" : sampleRate.toString();
                break;
            default:
                throw new IllegalArgumentException("Invalid field: " + field);
        }
        return value;
    }

    private static Fields firstFileNameField = Fields.ARTIST;

    private static void setFirstFileNameField(Fields field) {
        firstFileNameField = field;
    }

    private String firstFileNameField(){
        return getField(firstFileNameField);
    }


    // Default constructor required by JPA
    public MusicFile() {
        this.dateAdded = new Date();
    }

    /**
     * Constructor that extracts metadata from a music file
     *
     * @param musicFile The music file to extract metadata from
     */
    public MusicFile(File musicFile) {
        this();  // Call default constructor to set dateAdded

        try {
            // Set basic file information
            this.filePath = musicFile.getAbsolutePath();
            this.fileType = getFileExtension(musicFile.getName());
            this.fileSizeBytes = musicFile.length();
            this.lastModified = new Date(musicFile.lastModified());

            // Extract audio metadata using JAudioTagger
            AudioFile audioFile = AudioFileIO.read(musicFile);

            // Get audio header information
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                this.durationSeconds = header.getTrackLength();
                this.bitRate = header.getBitRateAsNumber();
                this.sampleRate = header.getSampleRateAsNumber();
            }

            // Get tag information
            Tag tag = audioFile.getTag();
            if (tag != null) {
                // Extract basic metadata
                this.title = getTagField(tag, FieldKey.TITLE);
                this.artist = getTagField(tag, FieldKey.ARTIST);
                if (this.artist != null)
                    artistNames.add(this.artist);  // Add artist to set of known artists
                this.album = getTagField(tag, FieldKey.ALBUM);
                if (this.album!= null)
                    albumNames.add(this.album);  // Add album to set of known albums
                this.genre = getTagField(tag, FieldKey.GENRE);
                if (this.genre!= null)
                    genreNames.add(this.genre);  // Add genre to set of known genres

                // Extract track number
                String trackNumberStr = getTagField(tag, FieldKey.TRACK);
                if (trackNumberStr != null && !trackNumberStr.isEmpty()) {
                    try {
                        // Handle track numbers in format "1/10"
                        if (trackNumberStr.contains("/")) {
                            trackNumberStr = trackNumberStr.split("/")[0];
                        }
                        this.trackNumber = Integer.parseInt(trackNumberStr.trim());
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }

                // Extract year
                String yearStr = getTagField(tag, FieldKey.YEAR);
                if (yearStr != null && !yearStr.isEmpty()) {
                    try {
                        this.year = Integer.parseInt(yearStr.trim());
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }
            }

            // Use filename as title if no title was found
            if (this.title == null || this.title.isEmpty()) {
                this.title = getFilenameWithoutExtension(musicFile.getName());
            }

        } catch (Exception e) {
            // Log error and set basic file information
            System.err.println("Error reading metadata from file: " + musicFile.getAbsolutePath());
            System.err.println(e.getMessage());

            // Set basic information even if metadata extraction fails
            this.filePath = musicFile.getAbsolutePath();
            this.fileType = getFileExtension(musicFile.getName());
            this.fileSizeBytes = musicFile.length();
            this.lastModified = new Date(musicFile.lastModified());
            this.title = getFilenameWithoutExtension(musicFile.getName());
        }
    }

    /**
     * Comparator for MusicFile objects that compares based on fuzzy match scores
     * of both the artist and the title, and considers track numbers if available.
     */
    public static Comparator<MusicFile> fuzzyComparator(MusicFile target) {
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
     * Comparator for MusicFile objects that compares based on exact matches
     * of the artist and/or the title, ignoring whitespace.
     *
     * @param target        The target MusicFile to compare against.
     * @param matchByTitle  If true, compare by title.
     * @param matchByArtist If true, compare by artist.
     * @return A Comparator that prioritizes exact matches.
     */
    public static Comparator<MusicFile> exactMatchComparator(MusicFile target, boolean matchByTitle, boolean matchByArtist) {
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
     * @param musicFiles The list of MusicFile objects to search through.
     * @param target     The target MusicFile to compare against.
     * @param num        The number of most similar MusicFile objects to return.
     * @return A list of the 'num' most similar MusicFile objects.
     */
    public static List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles, MusicFile target, int num) {
        final String currentFilePath = target.getFilePath();
        return musicFiles.stream()
                .filter(mf -> !mf.getFilePath().equals(currentFilePath)) // Filter out files with the same path
                .sorted(fuzzyComparator(target))
                .limit(num)
                .collect(Collectors.toList());
    }

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
    }

    private static void countFirstFieldByLetters() {
        List<String> artists = new ArrayList<>(artistNames.stream().toList());
        totalFirstFieldWithAlphaNames = 0;
        for (String artist : artists) {
            String artistInitial = artist.substring(0, 1).toUpperCase();
            if (artistInitial.matches("[A-Z]")) {// only count artists with at least alpha names
                Integer count = firstFieldMap.getOrDefault(artistInitial, 0);
                totalFirstFieldWithAlphaNames++;
                System.out.println(String.format("%s: %d", artistInitial, count));
                count++;
                firstFieldMap.put(artistInitial, count); // Reset the count for the artist
            }
        }
    }

    private static synchronized HashMap<String, String> whatInitials() {
        if (firstFieldMap.isEmpty()) {
            countFirstFieldByLetters();
        }
        int totalDirsCreated = 0;
        initials = new HashMap<>();
        final int targetTally = totalFirstFieldWithAlphaNames / 8 -5; // 8 directories for artists

        // we want 8 directories for artists, format a-c, d-f ... and misc artists go to "misc" so 9 total directories

        for (char currentChar = 'A'; currentChar <= 'Z'; ) {
            char startingChar = currentChar;
            int currentTally = 0;
            while ((totalDirsCreated >= numberOfSubdirectorys || currentTally < targetTally) && currentChar <= 'Z') {
                Integer artistCounterAtCurrentChar = firstFieldMap.getOrDefault(Character.toString(currentChar), 0);
                if (totalDirsCreated < numberOfSubdirectorys && artistCounterAtCurrentChar / 2 + currentTally > targetTally)
                    currentTally = targetTally + 1;
                else {
                    currentTally += artistCounterAtCurrentChar;
                    currentChar++;
                }
            }
            totalDirsCreated++;
            char endingChar = currentChar;
            if (endingChar > startingChar) endingChar--;  // adjust endingChar to account for last artist in the range  }
            String initialString = startingChar + "-" + endingChar;
            if (startingChar == endingChar) {
                initialString = "" + startingChar;
            }
            while (startingChar <= endingChar) {
                initials.put(Character.toString(startingChar), initialString);
                startingChar++;
            }
        }
        return initials;
    }

    /**
     * Helper method to safely get a tag field
     */
    private String getTagField(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return value != null && !value.isEmpty() ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper method to get file extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Helper method to get filename without extension
     */
    private String getFilenameWithoutExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
        this.isModified = true;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.isModified = true;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.isModified = true;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
        this.isModified = true;
        if (artist != null)
            artistNames.add(artist);  // Add artist to set of known artists
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
        this.isModified = true;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
        this.isModified = true;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
        this.isModified = true;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
        this.isModified = true;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.isModified = true;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
        this.isModified = true;
    }

    public Long getBitRate() {
        return bitRate;
    }

    public void setBitRate(Long bitRate) {
        this.bitRate = bitRate;
        this.isModified = true;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
        this.isModified = true;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
        this.isModified = true;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
        this.isModified = true;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
        this.isModified = true;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        isModified = modified;
    }

    @Override
    public String toString() {
        if (trackNumber != null) {
            return String.format("%s - %s (%s - %d)", artist, title, album, trackNumber);
        } else {
            if (album == null)
                return String.format("%s - %s", artist, title);
            else
                return String.format("%s - %s (%s)", artist, title, album);
        }
    }

    /**
     * Determines if the current MusicFile is likely the same song as the given MusicFile.
     *
     * @param other The MusicFile to compare against.
     * @return true if the songs are likely the same, false otherwise.
     */
    public boolean isItLikelyTheSameSong(MusicFile other) {
        if (other == null) {
            return false;
        }

        if (this.title == null || other.title == null) {
            return false;
        }
        if (this.artist == null || other.artist == null) {
            return false;
        }
        if (this.album == null || other.album == null) {
            return false;
        }
        if (this.getBitRate() >= 192L) { // wanting to get rid of possible lower bitrate files
            return false;
        }

        // Check track numbers
        if (this.trackNumber != null && other.trackNumber != null &&
                this.trackNumber != 0 && other.trackNumber != 0 &&
                !this.trackNumber.equals(other.trackNumber)) {
            return false; // Track numbers differ
        }

        // Fuzzy match scores for title and artist
        int titleScore = StringUtils.fuzzyMatch(this.title, other.title);
        int artistScore = StringUtils.fuzzyMatch(this.artist, other.artist);

        // Check if duration is within 10%
        boolean durationMatch = false;
        int durationDifference = 1000;
        if (this.durationSeconds != null && other.durationSeconds != null) {
            durationDifference = Math.abs(this.durationSeconds - other.durationSeconds);
            int maxAllowedDifference = (int) (0.05 * Math.max(this.durationSeconds, other.durationSeconds));
            durationMatch = durationDifference <= maxAllowedDifference;
        }

        // Fuzzy match score for album if not null
        int albumScore = 0;
        if (this.album != null && other.album != null) {
            albumScore = StringUtils.fuzzyMatch(this.album, other.album);
        }

        // Determine if it's likely the same song based on scores and duration match
        if (durationMatch && durationDifference < 3 && titleScore > 90) {
            return true;
        }
        return titleScore > 90 && artistScore > 90 && durationMatch && (this.album == null || albumScore > 90);
    }

    public List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles, int num) {
        return findMostSimilarFiles(musicFiles, this, num);
    }

    public List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles) {
        return findMostSimilarFiles(musicFiles, this, DEFAULT_NUM_OF_SIMILAR_FILES);
    }

    /**
     * Deletes the file at the filePath if it exists and the id is null.
     * Sets all string attributes to "****deleted****" if the file is successfully deleted.
     */
    public boolean deleteFile() {
        if (this.id == null) {
            String pathname = this.title + "." + this.fileType;
            if (this.filePath != null) {
                String path = filePath;
                File file = new File(this.filePath);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        // Set all string attributes to "****deleted****"
                        this.filePath = "****deleted****";
                        this.id = null;
                        this.title = "****deleted****";
                        this.artist = "****deleted****";
                        this.album = "****deleted****";
                        this.genre = "****deleted****";
                        this.fileType = "****deleted****";
                        System.out.println(String.format("Deleted: %s", path));
                        return true;
                    } else {
                        System.err.println("Failed to delete the file.");
                    }
                } else {
                    System.err.println("File does not exist: " + pathname);
                }
            }
        } else {
            System.err.println("File cannot be deleted because the ID is not null.");
        }
        return false;
    }

    /**
     * Generates a new file name and location for the music file using the default template.
     * The format is "startingPath/artistName/AlbumName/trackNumber-title.fileType"
     * where all whitespace is converted to underscores, and the trackNumber is 2 digits.
     *
     * @param startingPath The starting path to prepend to the file name.
     * @return A string representing the new file name and location.
     */
    public String newFileNameAndLocation(String startingPath) {
        // Use default template for backward compatibility
        PathTemplate defaultTemplate = new PathTemplate();
        return newFileNameAndLocation(startingPath, defaultTemplate);
    }

    /**
     * Generates a new file name and location for the music file using a custom template.
     *
     * @param startingPath The starting path to prepend to the file name.
     * @param template The PathTemplate to use for generating the path.
     * @return A string representing the new file name and location.
     */
    public String newFileNameAndLocation(String startingPath, PathTemplate template) {
        if (template == null) {
            template = new PathTemplate(); // Use default if null
        }
        
        // Ensure initials are calculated if subdirectory grouping is enabled
        if (template.isUseSubdirectoryGrouping() && (initials == null || initials.isEmpty())) {
            whatInitials();
        }
        
        return template.generatePath(startingPath, this);
    }

    /**
     * Copies the music file from its current location to a new location.
     * The new location is determined by the newFileNameAndLocation() method.
     *
     * @param startingPath The starting path to prepend to the new file name.
     * @throws IOException If an I/O error occurs during copying.
     */
    public void copyToNewLocation(String startingPath) throws IOException {
        String newFilePath = newFileNameAndLocation(startingPath);
        Path sourcePath = Paths.get(this.filePath);
        Path destinationPath = Paths.get(newFilePath);

        // Create directories if they do not exist
        Files.createDirectories(destinationPath.getParent());

        // Copy the file to the new location
        Files.copy(sourcePath, destinationPath);
        System.out.println("Copying file..." + sourcePath + " -> " + destinationPath);

        // System.out.println("File copied to: " + newFilePath);
    }
    
    /**
     * Gets the organizational path for testing purposes.
     * This field stores the computed organization path without actually copying files.
     */
    public String getOrganizationalPath() {
        return organizationalPath;
    }
    
    /**
     * Sets the organizational path for testing purposes.
     * This allows us to test path generation without file system operations.
     */
    public void setOrganizationalPath(String organizationalPath) {
        this.organizationalPath = organizationalPath;
    }
    
    /**
     * Generates and stores the organizational path without copying the file.
     * This is useful for testing path templates and organization logic.
     * 
     * @param basePath The base path for organization
     * @param template The path template to use (if null, uses default)
     * @return The generated organizational path
     */
    public String generateOrganizationalPath(String basePath, PathTemplate template) {
        if (template == null) {
            template = new PathTemplate(); // Use default if null
        }
        
        // Ensure initials are calculated if subdirectory grouping is enabled
        if (template.isUseSubdirectoryGrouping()) {
            try {
                java.lang.reflect.Method whatInitialsMethod = MusicFile.class.getDeclaredMethod("whatInitials");
                whatInitialsMethod.setAccessible(true);
                whatInitialsMethod.invoke(null);
            } catch (Exception e) {
                // If reflection fails, continue without subdirectory grouping
                System.err.println("Warning: Could not initialize subdirectory grouping: " + e.getMessage());
            }
        }
        
        String path = template.generatePath(basePath, this);
        this.organizationalPath = path;
        return path;
    }
}