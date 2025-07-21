package org.hasting.model;

import org.hasting.util.StringUtils;
import org.hasting.util.MusicFileComparator;
import org.hasting.util.ArtistStatisticsManager;
import org.hasting.util.FileOrganizer;
import org.hasting.util.MetadataExtractor;
import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a music file with metadata and provides functionality for organization, comparison, and file operations.
 * 
 * <p>This class serves as the core model for music files in the MP3Org application. It stores all relevant
 * metadata extracted from audio files and provides methods for:
 * <ul>
 *   <li>Metadata access and modification with change tracking</li>
 *   <li>File similarity comparison and duplicate detection</li>
 *   <li>File organization and path generation</li>
 *   <li>Database persistence support</li>
 *   <li>File system operations (copy, delete)</li>
 * </ul>
 * 
 * <p>The class supports the following audio metadata fields:
 * <ul>
 *   <li><strong>Basic Info:</strong> title, artist, album, genre, track number, year</li>
 *   <li><strong>Technical Info:</strong> duration, file size, bit rate, sample rate, file type</li>
 *   <li><strong>System Info:</strong> file path, last modified date, date added</li>
 * </ul>
 * 
 * <p>Many methods in this class are deprecated and delegate to extracted utility classes for better
 * separation of concerns and maintainability. The utility classes include:
 * <ul>
 *   <li>{@link MusicFileComparator} - For comparison and similarity operations</li>
 *   <li>{@link ArtistStatisticsManager} - For artist statistics and organization</li>
 *   <li>{@link FileOrganizer} - For file organization and path generation</li>
 *   <li>{@link MetadataExtractor} - For metadata extraction from audio files</li>
 * </ul>
 * 
 * @author MP3Org Development Team
 * @version 1.0
 * @since 1.0
 */
public class MusicFile {

    private static final Logger logger = Log4Rich.getLogger(MusicFile.class);
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
    
    // Transient field to cache similar files found during duplicate detection
    // This is not persisted to the database and exists only in memory
    private transient List<MusicFile> similarFilesList = new ArrayList<>();

    /**
     * Enumeration of available metadata fields that can be accessed through the getField method.
     * 
     * <p>This enum is used for generic field access and is particularly useful for:
     * <ul>
     *   <li>Dynamic field value retrieval</li>
     *   <li>Template-based path generation</li>
     *   <li>Configurable display formatting</li>
     * </ul>
     */
    public enum Fields {
        /** Song title */
        TITLE, 
        /** Artist or performer name */
        ARTIST, 
        /** Album name */
        ALBUM, 
        /** Music genre */
        GENRE, 
        /** Track number on the album */
        TRACK_NUMBER, 
        /** Release year */
        YEAR, 
        /** Audio bit rate in kbps */
        BIT_RATE, 
        /** Audio sample rate in Hz */
        SAMPLE_RATE;
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

    /**
     * Retrieves the value of the specified metadata field as a string.
     * 
     * <p>This method provides a generic way to access any metadata field by its enum value.
     * Numeric fields (trackNumber, year, bitRate, sampleRate) are converted to strings,
     * with null values returning empty strings.
     * 
     * @param field the metadata field to retrieve (must not be null)
     * @return the field value as a string, or empty string for null numeric values
     * @throws IllegalArgumentException if the field parameter is null or not recognized
     */
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

    /**
     * Sets the primary field used for file organization and naming.
     * 
     * @param field the field to use as the primary organizational field (typically ARTIST)
     */
    private static void setFirstFileNameField(Fields field) {
        firstFileNameField = field;
    }

    /**
     * Gets the value of the primary field used for file organization.
     * 
     * @return the value of the primary organizational field as a string
     */
    private String firstFileNameField(){
        return getField(firstFileNameField);
    }


    /**
     * Default constructor that initializes a new MusicFile with the current date as dateAdded.
     * 
     * <p>This constructor is required by JPA for entity instantiation and sets the dateAdded
     * field to the current timestamp to track when the file was added to the system.
     */
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
    
    /**
     * Gets the unique database identifier for this music file.
     * 
     * @return the database ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique database identifier for this music file.
     * 
     * @param id the database ID to set
     */
    public void setId(Long id) {
        this.id = id;
        this.isModified = true;
    }

    /**
     * Gets the file system path to this music file.
     * 
     * @return the complete file path, or null if not set
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the file system path to this music file.
     * 
     * @param filePath the complete file path to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.isModified = true;
    }

    /**
     * Gets the song title.
     * 
     * @return the song title, or null if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the song title and marks the file as modified.
     * 
     * @param title the song title to set
     */
    public void setTitle(String title) {
        this.title = title;
        this.isModified = true;
    }

    public String getArtist() {
        return artist;
    }

    /**
     * Sets the artist name and marks the file as modified.
     * 
     * <p>When setting a non-null artist, the artist is automatically added to the
     * ArtistStatisticsManager for tracking and organization purposes.
     * 
     * @param artist the artist name to set
     */
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

    /**
     * Checks if this music file has been modified since creation or last save.
     * 
     * @return true if the file has unsaved changes, false otherwise
     */
    public boolean isModified() {
        return isModified;
    }

    /**
     * Sets the modification status of this music file.
     * 
     * <p>This method is typically used to mark a file as clean after saving to database.
     * 
     * @param modified true if the file has unsaved changes, false if clean
     */
    public void setModified(boolean modified) {
        isModified = modified;
    }
    
    /**
     * Gets the list of similar files found during duplicate detection.
     * 
     * <p>This transient field caches similar files found during analysis and is not
     * persisted to the database. It exists only in memory for the current session.
     * 
     * @return an unmodifiable list of similar MusicFile objects, or empty list if none found
     */
    public List<MusicFile> getSimilarFilesList() {
        return Collections.unmodifiableList(new ArrayList<>(similarFilesList));
    }

    /**
     * Sets the list of similar files found during duplicate detection.
     * 
     * <p>This method replaces the current list with a new list of similar files.
     * The provided list is copied to prevent external modification.
     * 
     * @param similarFiles the list of similar MusicFile objects to set (must not be null)
     * @throws IllegalArgumentException if the provided list is null
     */
    public void setSimilarFilesList(List<MusicFile> similarFiles) {
        if (similarFiles == null) {
            throw new IllegalArgumentException("Similar files list cannot be null");
        }
        this.similarFilesList = new ArrayList<>(similarFiles);
    }

    /**
     * Adds a music file to the list of similar files.
     * 
     * @param similarFile the MusicFile to add to the similar files list (must not be null)
     * @throws IllegalArgumentException if the provided file is null
     */
    public void addSimilarFile(MusicFile similarFile) {
        if (similarFile == null) {
            throw new IllegalArgumentException("Similar file cannot be null");
        }
        if (!this.similarFilesList.contains(similarFile)) {
            this.similarFilesList.add(similarFile);
        }
    }

    /**
     * Clears all similar files from the list.
     * 
     * <p>This method is useful when re-running duplicate detection to ensure
     * the list is fresh and contains only current results.
     */
    public void clearSimilarFiles() {
        this.similarFilesList.clear();
    }
    
    /**
     * Checks if this music file has any similar files in its cache.
     * 
     * @return true if the similar files list is not empty, false otherwise
     */
    public boolean hasSimilarFiles() {
        return !this.similarFilesList.isEmpty();
    }

    /**
     * Returns a string representation of the music file in a user-friendly format.
     * 
     * <p>The format varies based on available metadata:
     * <ul>
     *   <li>With track number: "Artist - Title (Album - TrackNumber)"</li>
     *   <li>With album but no track: "Artist - Title (Album)"</li>
     *   <li>Without album: "Artist - Title"</li>
     * </ul>
     * 
     * @return a formatted string representation of the music file
     */
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

    /**
     * Finds the most similar music files to this one from the provided list.
     * 
     * @param musicFiles the list of music files to search through
     * @param num the maximum number of similar files to return
     * @return a list of the most similar music files, ordered by similarity (most similar first)
     */
    public List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles, int num) {
        return MusicFileComparator.findMostSimilarFiles(musicFiles, this, num);
    }

    /**
     * Finds the most similar music files to this one using the default number of results.
     * 
     * @param musicFiles the list of music files to search through
     * @return a list of the most similar music files (up to default limit), ordered by similarity
     */
    public List<MusicFile> findMostSimilarFiles(List<MusicFile> musicFiles) {
        return MusicFileComparator.findMostSimilarFiles(musicFiles, this);
    }

    /**
     * Deletes the file at the filePath if it exists.
     * Sets all string attributes to "****deleted****" if the file is successfully deleted.
     */
    public boolean deleteFile() {
        // File can be deleted regardless of ID status - the ID check is handled by DatabaseManager
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
                    logger.info(String.format("Deleted file: {}", path));
                    return true;
                } else {
                    logger.error(String.format("Failed to delete file: {}", path));
                }
            } else {
                logger.warn(String.format("File does not exist: {}", path));
            }
        } else {
            logger.error("File cannot be deleted because filePath is null");
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

    /**
     * Determines if this music file is similar to another music file.
     * This is an alias for {@link #isItLikelyTheSameSong(MusicFile)} to provide
     * a more intuitive method name for test data factory usage.
     * 
     * @param other The other MusicFile to compare with
     * @return true if the files are similar, false otherwise
     * @see #isItLikelyTheSameSong(MusicFile)
     */
    public boolean isSimilarTo(MusicFile other) {
        return isItLikelyTheSameSong(other);
    }

    /**
     * Refreshes the metadata for this music file by re-reading from the file system.
     * This method re-extracts all metadata from the audio file on disk and updates
     * this MusicFile object with the current information.
     * 
     * @return true if metadata was successfully refreshed, false if the file doesn't exist or extraction failed
     */
    public boolean refreshMetadata() {
        if (this.filePath == null || this.filePath.trim().isEmpty()) {
            return false;
        }
        File file = new File(this.filePath);
        if (!file.exists()) {
            return false;
        }
        return MetadataExtractor.extractMetadata(this, file);
    }
}