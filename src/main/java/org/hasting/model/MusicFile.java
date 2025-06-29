package org.hasting.model;

import org.hasting.util.StringUtils;
import org.hasting.util.MusicFileComparator;
import org.hasting.util.ArtistStatisticsManager;
import org.hasting.util.FileOrganizer;
import org.hasting.util.MetadataExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MusicFile {

    final private static int DEFAULT_NUM_OF_SIMILAR_FILES = 10;


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
    /**
     * @deprecated Use {@link ArtistStatisticsManager#getNumberOfSubdirectories()} instead
     */
    @Deprecated
    public static int getNumberOfSubdirectorys() {
        return ArtistStatisticsManager.getNumberOfSubdirectories();
    }

    /**
     * @deprecated Use {@link ArtistStatisticsManager#setNumberOfSubdirectories(int)} instead
     */
    @Deprecated
    public static void setNumberOfSubdirectorys(int numberOfSubdirectorys) {
        ArtistStatisticsManager.setNumberOfSubdirectories(numberOfSubdirectorys);
    }

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
        
        // Use MetadataExtractor to populate this object with metadata
        MetadataExtractor.extractMetadata(this, musicFile);
    }

    /**
     * Comparator for MusicFile objects that compares based on fuzzy match scores
     * of both the artist and the title, and considers track numbers if available.
     * 
     * @deprecated Use {@link MusicFileComparator#createFuzzyComparator(MusicFile)} instead
     */
    @Deprecated
    public static Comparator<MusicFile> fuzzyComparator(MusicFile target) {
        return MusicFileComparator.createFuzzyComparator(target);
    }

    /**
     * Comparator for MusicFile objects that compares based on exact matches
     * of the artist and/or the title, ignoring whitespace.
     *
     * @param target        The target MusicFile to compare against.
     * @param matchByTitle  If true, compare by title.
     * @param matchByArtist If true, compare by artist.
     * @return A Comparator that prioritizes exact matches.
     * @deprecated Use {@link MusicFileComparator#createExactMatchComparator(MusicFile, boolean, boolean)} instead
     */
    @Deprecated
    public static Comparator<MusicFile> exactMatchComparator(MusicFile target, boolean matchByTitle, boolean matchByArtist) {
        return MusicFileComparator.createExactMatchComparator(target, matchByTitle, matchByArtist);
    }

    /**
     * Finds the top 'num' most similar MusicFile objects to the target MusicFile
     * from the provided list, based on fuzzy match scores.
     *
     * @param musicFiles The list of MusicFile objects to search through.
     * @param target     The target MusicFile to compare against.
     * @param num        The number of most similar MusicFile objects to return.
     * @return A list of the 'num' most similar MusicFile objects.
     * @deprecated Use {@link MusicFileComparator#findMostSimilarFiles(List, MusicFile, int)} instead
     */
    @Deprecated
    public static List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles, MusicFile target, int num) {
        return MusicFileComparator.findMostSimilarFiles(musicFiles, target, num);
    }

    /**
     * @deprecated Use {@link ArtistStatisticsManager#resetArtistCounts()} instead
     */
    @Deprecated
    public static void resetArtistCounts() {
        ArtistStatisticsManager.resetArtistCounts();
    }

    /**
     * @deprecated Use {@link ArtistStatisticsManager#calculateInitials()} instead
     */
    @Deprecated
    private static synchronized HashMap<String, String> whatInitials() {
        return ArtistStatisticsManager.calculateInitials();
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
            ArtistStatisticsManager.addArtist(artist);  // Add artist to set of known artists
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
        return MusicFileComparator.isLikelyTheSameSong(this, other);
    }

    public List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles, int num) {
        return MusicFileComparator.findMostSimilarFiles(musicFiles, this, num);
    }

    public List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles) {
        return MusicFileComparator.findMostSimilarFiles(musicFiles, this);
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
        return FileOrganizer.generateFilePath(this, startingPath);
    }

    /**
     * Generates a new file name and location for the music file using a custom template.
     *
     * @param startingPath The starting path to prepend to the file name.
     * @param template The PathTemplate to use for generating the path.
     * @return A string representing the new file name and location.
     */
    public String newFileNameAndLocation(String startingPath, PathTemplate template) {
        return FileOrganizer.generateFilePath(this, startingPath, template);
    }

    /**
     * Copies the music file from its current location to a new location.
     * The new location is determined by the newFileNameAndLocation() method.
     *
     * @param startingPath The starting path to prepend to the new file name.
     * @throws IOException If an I/O error occurs during copying.
     */
    public void copyToNewLocation(String startingPath) throws IOException {
        FileOrganizer.copyToNewLocation(this, startingPath);
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
        return FileOrganizer.generateOrganizationalPath(this, basePath, template);
    }
}