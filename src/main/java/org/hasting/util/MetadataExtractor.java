package org.hasting.util;

import org.hasting.model.MusicFile;
import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.Date;

/**
 * Utility class for extracting metadata from audio files using the JAudioTagger library.
 * Handles reading of audio file properties, tags, and basic file information.
 */
public class MetadataExtractor {
    
    private static final Logger logger = Log4Rich.getLogger(MetadataExtractor.class);
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MetadataExtractor() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Extracts complete metadata from an audio file and populates a MusicFile object.
     * This method reads both audio properties (duration, bitrate, etc.) and tag information
     * (title, artist, album, etc.) from the file.
     * 
     * @param musicFile The MusicFile object to populate with metadata
     * @param audioFile The File object representing the audio file to read
     * @return true if metadata extraction was successful, false if errors occurred
     */
    public static boolean extractMetadata(MusicFile musicFile, File audioFile) {
        if (musicFile == null || audioFile == null) {
            return false;
        }
        
        try {
            // Set basic file information
            musicFile.setFilePath(audioFile.getAbsolutePath());
            musicFile.setFileType(getFileExtension(audioFile.getName()));
            musicFile.setFileSizeBytes(audioFile.length());
            musicFile.setLastModified(new Date(audioFile.lastModified()));

            // Extract audio metadata using JAudioTagger
            AudioFile jAudioFile = AudioFileIO.read(audioFile);

            // Get audio header information
            AudioHeader header = jAudioFile.getAudioHeader();
            if (header != null) {
                extractAudioProperties(musicFile, header);
            }

            // Get tag information
            Tag tag = jAudioFile.getTag();
            if (tag != null) {
                extractTagInformation(musicFile, tag);
            }

            // Use filename as title if no title was found
            if (musicFile.getTitle() == null || musicFile.getTitle().isEmpty()) {
                musicFile.setTitle(getFilenameWithoutExtension(audioFile.getName()));
            }
            
            return true;

        } catch (Exception e) {
            // Log error and set basic file information
            logger.error(String.format("Error reading metadata from file: {}", audioFile.getAbsolutePath()), e);

            // Set basic information even if metadata extraction fails
            musicFile.setFilePath(audioFile.getAbsolutePath());
            musicFile.setFileType(getFileExtension(audioFile.getName()));
            musicFile.setFileSizeBytes(audioFile.length());
            musicFile.setLastModified(new Date(audioFile.lastModified()));
            musicFile.setTitle(getFilenameWithoutExtension(audioFile.getName()));
            
            return false;
        }
    }
    
    /**
     * Extracts audio properties from the audio header and sets them on the MusicFile.
     * This includes duration, bitrate, and sample rate information.
     * 
     * @param musicFile The MusicFile to populate
     * @param header The AudioHeader containing the properties
     */
    private static void extractAudioProperties(MusicFile musicFile, AudioHeader header) {
        if (musicFile == null || header == null) {
            return;
        }
        
        try {
            musicFile.setDurationSeconds(header.getTrackLength());
        } catch (Exception e) {
            logger.debug(String.format("Failed to extract duration from audio header: {}", e.getMessage()));
        }
        
        try {
            musicFile.setBitRate(header.getBitRateAsNumber());
        } catch (Exception e) {
            logger.debug(String.format("Failed to extract bit rate from audio header: {}", e.getMessage()));
        }
        
        try {
            musicFile.setSampleRate(header.getSampleRateAsNumber());
        } catch (Exception e) {
            logger.debug(String.format("Failed to extract sample rate from audio header: {}", e.getMessage()));
        }
    }
    
    /**
     * Extracts tag information from the audio file tags and sets them on the MusicFile.
     * This includes title, artist, album, genre, track number, and year.
     * 
     * @param musicFile The MusicFile to populate
     * @param tag The Tag containing the metadata
     */
    private static void extractTagInformation(MusicFile musicFile, Tag tag) {
        if (musicFile == null || tag == null) {
            return;
        }
        
        // Extract basic metadata
        musicFile.setTitle(getTagField(tag, FieldKey.TITLE));
        
        String artist = getTagField(tag, FieldKey.ARTIST);
        musicFile.setArtist(artist);
        if (artist != null) {
            ArtistStatisticsManager.addArtist(artist);
        }
        
        String album = getTagField(tag, FieldKey.ALBUM);
        musicFile.setAlbum(album);
        if (album != null) {
            ArtistStatisticsManager.addAlbum(album);
        }
        
        String genre = getTagField(tag, FieldKey.GENRE);
        musicFile.setGenre(genre);
        if (genre != null) {
            ArtistStatisticsManager.addGenre(genre);
        }

        // Extract track number
        String trackNumberStr = getTagField(tag, FieldKey.TRACK);
        if (trackNumberStr != null && !trackNumberStr.isEmpty()) {
            try {
                // Handle track numbers in format "1/10"
                if (trackNumberStr.contains("/")) {
                    trackNumberStr = trackNumberStr.split("/")[0];
                }
                musicFile.setTrackNumber(Integer.parseInt(trackNumberStr.trim()));
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        // Extract year
        String yearStr = getTagField(tag, FieldKey.YEAR);
        if (yearStr != null && !yearStr.isEmpty()) {
            try {
                musicFile.setYear(Integer.parseInt(yearStr.trim()));
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
    }
    
    /**
     * Safely extracts a tag field value, handling potential exceptions.
     * 
     * @param tag The tag to extract from
     * @param key The field key to extract
     * @return The field value, or null if not available or empty
     */
    private static String getTagField(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return value != null && !value.isEmpty() ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the file extension from a filename.
     * 
     * @param filename The filename to extract the extension from
     * @return The file extension in lowercase, or empty string if no extension found
     */
    private static String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Gets the filename without its extension.
     * 
     * @param filename The filename to process
     * @return The filename without extension, or the original filename if no extension found
     */
    private static String getFilenameWithoutExtension(String filename) {
        if (filename == null) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
    
    /**
     * Checks if a file is a supported audio format based on its extension.
     * 
     * @param file The file to check
     * @return true if the file appears to be a supported audio format
     */
    public static boolean isSupportedAudioFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String extension = getFileExtension(file.getName()).toLowerCase();
        return isSupportedAudioExtension(extension);
    }
    
    /**
     * Checks if a file extension represents a supported audio format.
     * 
     * @param extension The file extension to check (without the dot)
     * @return true if the extension is supported
     */
    public static boolean isSupportedAudioExtension(String extension) {
        if (extension == null) {
            return false;
        }
        
        // Common audio formats supported by JAudioTagger
        String[] supportedExtensions = {
            "mp3", "flac", "ogg", "mp4", "m4a", "m4p", "wma", "wav", "aiff", "aif"
        };
        
        String lowerExt = extension.toLowerCase();
        for (String supported : supportedExtensions) {
            if (supported.equals(lowerExt)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Creates a basic MusicFile object with metadata extracted from the given audio file.
     * This is a convenience method that creates a new MusicFile and extracts metadata in one step.
     * 
     * @param audioFile The audio file to create a MusicFile from
     * @return A new MusicFile with extracted metadata, or null if extraction failed
     */
    public static MusicFile createMusicFileFromAudioFile(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            return null;
        }
        
        MusicFile musicFile = new MusicFile();
        boolean success = extractMetadata(musicFile, audioFile);
        
        return success ? musicFile : null;
    }
}