package org.hasting.util;

import org.hasting.model.MusicFile;
import org.hasting.model.PathTemplate;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for organizing and copying music files based on configured templates.
 * Handles file path generation, directory creation, and file copying operations.
 */
public class FileOrganizer {
    
    private static final Logger logger = Log4Rich.getLogger(FileOrganizer.class);
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private FileOrganizer() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Generates a new file name and location for the music file using the default template.
     * The format is "startingPath/artistName/AlbumName/trackNumber-title.fileType"
     * where all whitespace is converted to underscores, and the trackNumber is 2 digits.
     *
     * @param musicFile The music file to generate a path for
     * @param startingPath The starting path to prepend to the file name
     * @return A string representing the new file name and location
     */
    public static String generateFilePath(MusicFile musicFile, String startingPath) {
        // Use default template for backward compatibility
        PathTemplate defaultTemplate = new PathTemplate();
        return generateFilePath(musicFile, startingPath, defaultTemplate);
    }

    /**
     * Generates a new file name and location for the music file using a custom template.
     *
     * @param musicFile The music file to generate a path for
     * @param startingPath The starting path to prepend to the file name
     * @param template The PathTemplate to use for generating the path
     * @return A string representing the new file name and location
     */
    public static String generateFilePath(MusicFile musicFile, String startingPath, PathTemplate template) {
        if (musicFile == null) {
            throw new IllegalArgumentException("MusicFile cannot be null");
        }
        
        if (startingPath == null) {
            startingPath = "";
        }
        
        if (template == null) {
            template = new PathTemplate(); // Use default if null
        }
        
        // Ensure initials are calculated if subdirectory grouping is enabled
        if (template.isUseSubdirectoryGrouping()) {
            ArtistStatisticsManager.calculateInitials();
        }
        
        return template.generatePath(startingPath, musicFile);
    }

    /**
     * Copies the music file from its current location to a new location.
     * The new location is determined by the generateFilePath() method using the default template.
     *
     * @param musicFile The music file to copy
     * @param startingPath The starting path to prepend to the new file name
     * @throws IOException If an I/O error occurs during copying
     * @throws IllegalArgumentException If the music file or its file path is null
     */
    public static void copyToNewLocation(MusicFile musicFile, String startingPath) throws IOException {
        copyToNewLocation(musicFile, startingPath, new PathTemplate());
    }
    
    /**
     * Copies the music file from its current location to a new location.
     * The new location is determined by the generateFilePath() method using the specified template.
     *
     * @param musicFile The music file to copy
     * @param startingPath The starting path to prepend to the new file name
     * @param template The PathTemplate to use for generating the path
     * @throws IOException If an I/O error occurs during copying
     * @throws IllegalArgumentException If the music file or its file path is null
     */
    public static void copyToNewLocation(MusicFile musicFile, String startingPath, PathTemplate template) throws IOException {
        if (musicFile == null) {
            throw new IllegalArgumentException("MusicFile cannot be null");
        }
        
        if (musicFile.getFilePath() == null) {
            throw new IllegalArgumentException("MusicFile file path cannot be null");
        }
        
        String newFilePath = generateFilePath(musicFile, startingPath, template);
        Path sourcePath = Paths.get(musicFile.getFilePath());
        Path destinationPath = Paths.get(newFilePath);

        // Verify source file exists
        if (!Files.exists(sourcePath)) {
            throw new IOException("Source file does not exist: " + sourcePath);
        }

        // Create directories if they do not exist
        Files.createDirectories(destinationPath.getParent());

        // Copy the file to the new location
        Files.copy(sourcePath, destinationPath);
        logger.info(String.format("Copied file: {} -> {}", sourcePath, destinationPath));
    }
    
    /**
     * Generates and stores the organizational path without copying the file.
     * This is useful for testing path templates and organization logic.
     * 
     * @param musicFile The music file to generate an organizational path for
     * @param basePath The base path for organization
     * @param template The path template to use (if null, uses default)
     * @return The generated organizational path
     */
    public static String generateOrganizationalPath(MusicFile musicFile, String basePath, PathTemplate template) {
        if (musicFile == null) {
            throw new IllegalArgumentException("MusicFile cannot be null");
        }
        
        if (template == null) {
            template = new PathTemplate(); // Use default if null
        }
        
        // Ensure initials are calculated if subdirectory grouping is enabled
        if (template.isUseSubdirectoryGrouping()) {
            ArtistStatisticsManager.calculateInitials();
        }
        
        String path = template.generatePath(basePath, musicFile);
        musicFile.setOrganizationalPath(path);
        return path;
    }
    
    /**
     * Validates that a file path is safe and doesn't contain dangerous characters.
     * 
     * @param filePath The file path to validate
     * @return true if the path is safe, false otherwise
     */
    public static boolean isValidFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        
        // Check for dangerous characters or patterns
        String[] dangerousPatterns = {
            "..", // Directory traversal
            "//", // Double slashes
            "\\\\", // Double backslashes (Windows)
            "<", ">", ":", "\"", "|", "?", "*" // Invalid filename characters
        };
        
        for (String pattern : dangerousPatterns) {
            if (filePath.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Sanitizes a filename by removing or replacing invalid characters.
     * 
     * @param filename The filename to sanitize
     * @return A sanitized filename safe for file system use
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        
        // Replace invalid characters with underscores
        String sanitized = filename.replaceAll("[<>:\"|?*]", "_");
        
        // Remove leading/trailing periods and spaces
        sanitized = sanitized.trim().replaceAll("^\\.*", "").replaceAll("\\.*$", "");
        
        // Ensure filename is not empty
        if (sanitized.isEmpty()) {
            sanitized = "unknown";
        }
        
        return sanitized;
    }
    
    /**
     * Creates the directory structure for a given file path if it doesn't exist.
     * 
     * @param filePath The full file path (directories will be created for the parent)
     * @throws IOException If directory creation fails
     */
    public static void ensureDirectoryExists(String filePath) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }
}