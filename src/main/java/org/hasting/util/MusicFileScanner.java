package org.hasting.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.hasting.model.MusicFile;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import org.hasting.util.logging.MP3OrgLoggingManager;
import org.hasting.util.logging.Logger;

/**
 * High-performance music file scanner for recursive directory traversal and metadata extraction.
 * 
 * <p>This utility class provides comprehensive file system scanning capabilities specifically
 * optimized for music file collections. It supports multiple audio formats, provides detailed
 * progress feedback, and includes caching mechanisms for improved performance on subsequent scans.
 * 
 * <p>Key capabilities include:
 * <ul>
 * <li><strong>Multi-format support</strong> - Handles MP3, FLAC, OGG, WAV, AAC, M4A, WMA, AIFF, APE, OPUS</li>
 * <li><strong>Recursive scanning</strong> - Traverses entire directory trees efficiently</li>
 * <li><strong>Progress tracking</strong> - Detailed progress callbacks with file and directory counts</li>
 * <li><strong>Metadata extraction</strong> - Extracts audio metadata using JAudioTagger library</li>
 * <li><strong>Caching system</strong> - Caches metadata to avoid re-processing unchanged files</li>
 * <li><strong>Cancellation support</strong> - Graceful cancellation of long-running operations</li>
 * <li><strong>Error handling</strong> - Robust error handling for corrupted or inaccessible files</li>
 * </ul>
 * 
 * <p>Performance features:
 * <ul>
 * <li><strong>File filtering</strong> - Pre-filters files by extension before processing</li>
 * <li><strong>Batch processing</strong> - Processes files in batches for memory efficiency</li>
 * <li><strong>Directory counting</strong> - Pre-counts directories for accurate progress reporting</li>
 * <li><strong>Metadata caching</strong> - Avoids re-reading unchanged file metadata</li>
 * </ul>
 * 
 * <p>Progress tracking provides multiple callback mechanisms:
 * <ul>
 * <li><strong>Status callbacks</strong> - High-level status messages for user feedback</li>
 * <li><strong>Progress callbacks</strong> - Numeric progress percentages (0-100)</li>
 * <li><strong>File processing callbacks</strong> - Individual file processing notifications</li>
 * <li><strong>Detailed progress callbacks</strong> - Comprehensive progress data structures</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>{@code
 * MusicFileScanner scanner = new MusicFileScanner();
 * scanner.setStatusCallback(status -> System.out.println(status));
 * scanner.setProgressCallback(percent -> updateProgressBar(percent));
 * 
 * List<String> directories = Arrays.asList("/music/rock", "/music/jazz");
 * List<MusicFile> musicFiles = scanner.scanDirectories(directories);
 * }</pre>
 * 
 * <p>The scanner is designed to be reusable across multiple scanning operations while
 * maintaining state for caching and progress tracking. All callback operations are
 * performed on the calling thread, so UI updates may require additional threading considerations.
 * 
 * @see MusicFile for the data model produced by scanning
 * @see ScanProgress for detailed progress information structure
 * @since 1.0
 */
public class MusicFileScanner {
    private static final Logger logger = MP3OrgLoggingManager.getLogger(MusicFileScanner.class);
    // List of supported music file extensions
    private static final String[] SUPPORTED_EXTENSIONS = {
        "mp3", "flac", "ogg", "wav", "aac", "m4a", "wma", "aiff", "ape", "opus"
    };

    private HashMap<String, MusicFile> musicFileCache = new HashMap<>(); // Cache for music files
    private Consumer<String> statusCallback;
    private Consumer<Integer> progressCallback;
    private Consumer<String> fileProcessingCallback; // New callback for individual file processing
    private Consumer<ScanProgress> detailedProgressCallback; // New callback for detailed progress info
    private boolean stopRequested = false;
    private int totalFilesScanned = 0;
    
    // Progress tracking data structure
    public static class ScanProgress {
        public final String currentDirectory;
        public final String currentFile;
        public final int filesFound;
        public final int filesProcessed;
        public final int totalDirectories;
        public final int directoriesProcessed;
        public final String stage; // "scanning", "reading_tags", "saving"
        
        public ScanProgress(String currentDirectory, String currentFile, int filesFound, 
                           int filesProcessed, int totalDirectories, int directoriesProcessed, String stage) {
            this.currentDirectory = currentDirectory;
            this.currentFile = currentFile;
            this.filesFound = filesFound;
            this.filesProcessed = filesProcessed;
            this.totalDirectories = totalDirectories;
            this.directoriesProcessed = directoriesProcessed;
            this.stage = stage;
        }
    }
    
    
    public List<MusicFile> scanMusicFiles(List<String> directoryPaths) {
        List<MusicFile> musicFiles = new ArrayList<>();
        musicFiles = DatabaseManager.getAllMusicFiles();   // Add this line to load music files from the database
        if (musicFiles.isEmpty()) {
            logger.info("No music files found in the database.");
        }
        else {
            for (var musicFile: musicFiles) {
                musicFileCache.put(musicFile.getFilePath(), musicFile); // Replace backslashes with forward slashes
            }
        }
        musicFiles.clear(); // Clear the List before scanning new directories
        directoryPaths.stream()
            .map(this::findAllMusicFiles)
            .forEach(musicFiles::addAll);
        
        return musicFiles;
    }
    
    /**
     * Enhanced method for scanning with detailed progress feedback.
     */
    public List<MusicFile> findAllMusicFilesWithProgress(List<String> directoryPaths) {
        List<MusicFile> allMusicFiles = new ArrayList<>();
        int totalDirectories = directoryPaths.size();
        int directoriesProcessed = 0;
        int totalFilesFound = 0;
        int totalFilesProcessed = 0;
        
        for (String directoryPath : directoryPaths) {
            if (stopRequested) break;
            
            File directory = new File(directoryPath.trim());
            if (!directory.exists() || !directory.isDirectory()) {
                logger.warning("Invalid directory: " + directoryPath);
                continue;
            }
            
            // Stage 1: Directory scanning
            if (detailedProgressCallback != null) {
                detailedProgressCallback.accept(new ScanProgress(
                    directoryPath, "", totalFilesFound, totalFilesProcessed, 
                    totalDirectories, directoriesProcessed, "scanning"
                ));
            }
            
            try {
                // Get all files with enabled extensions
                String[] enabledExtensions = getEnabledExtensions();
                Collection<File> files = FileUtils.listFiles(
                    directory, 
                    enabledExtensions, 
                    true // Include subdirectories
                );
                
                List<File> newFiles = new ArrayList<>();
                for (File file : files) {
                    if (!musicFileCache.containsKey(file.getPath())) {
                        newFiles.add(file);
                    }
                }
                
                totalFilesFound += newFiles.size();
                
                // Stage 2: Tag reading and processing
                int fileCount = 0;
                for (File file : newFiles) {
                    if (stopRequested) break;
                    
                    fileCount++;
                    String fileName = file.getName();
                    
                    // Notify about current file being processed
                    if (fileProcessingCallback != null) {
                        fileProcessingCallback.accept("Reading tags: " + fileName);
                    }
                    
                    if (detailedProgressCallback != null) {
                        detailedProgressCallback.accept(new ScanProgress(
                            directoryPath, fileName, totalFilesFound, totalFilesProcessed + fileCount, 
                            totalDirectories, directoriesProcessed, "reading_tags"
                        ));
                    }
                    
                    try {
                        MusicFile musicFile = new MusicFile(file);
                        musicFileCache.put(file.getPath(), musicFile);
                        allMusicFiles.add(musicFile);
                        
                        // Log the file processing (as mentioned in requirements)
                        logger.info("Processed: " + fileName + " - " + 
                                  musicFile.getArtist() + " - " + 
                                  musicFile.getAlbum() + " - " + 
                                  musicFile.getTitle());
                        
                    } catch (Exception e) {
                        logger.warning("Error processing file " + fileName + ": " + e.getMessage());
                    }
                }
                
                totalFilesProcessed += newFiles.size();
                
            } catch (Exception e) {
                logger.error("Error scanning directory: {}", e.getMessage(), e);
                if (statusCallback != null) {
                    statusCallback.accept("Error scanning directory: " + e.getMessage());
                }
            }
            
            directoriesProcessed++;
        }
        
        return allMusicFiles;
    }
    
    /**
     * Finds all music files in the given directory and its subdirectories.
     * 
     * @param directoryPath The path to the directory to scan
     * @return A list of MusicFile objects representing music files
     */
    public List<MusicFile> findAllMusicFiles(String directoryPath) {
        File directory = new File(directoryPath);
        List<MusicFile> musicFiles = new ArrayList<>();
        
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warning("Invalid directory: " + directoryPath);
            if (statusCallback != null) {
                statusCallback.accept("Invalid directory: " + directoryPath);
            }
            return musicFiles;
        }
        
        if (statusCallback != null) {
            String filesScanned = String.format("%8d files", totalFilesScanned);
            statusCallback.accept("Scanned " + filesScanned +"\nScanning for music files in: " + directoryPath);
        }
        
        try {
            // Get all files with enabled extensions
            String[] enabledExtensions = getEnabledExtensions();
            Collection<File> files = FileUtils.listFiles(
                directory, 
                enabledExtensions, 
                true // Include subdirectories
            );
            
            for (File file : files) {
                if (!musicFileCache.containsKey(file.getPath())) {
                    MusicFile newMusicFile = new MusicFile(file);
                    musicFileCache.put(file.getPath(), newMusicFile); // Replace backslashes with forward slashes
                    musicFiles.add(new MusicFile(file));
                    totalFilesScanned++; // Increment the counter for each file processed
                }
            }
            
            if (statusCallback != null) {
                statusCallback.accept("Found " + musicFiles.size() + " music files in " + directoryPath);
            }
            
        } catch (Exception e) {
            logger.error("Error scanning directory: {}", e.getMessage(), e);
            if (statusCallback != null) {
                statusCallback.accept("Error scanning directory: " + e.getMessage());
            }
        }
        
        return musicFiles;
    }
    
    /**
     * Checks if a file is a supported music file based on its extension.
     * 
     * @param file The file to check
     * @return true if the file is a supported music file, false otherwise
     */
    public static boolean isMusicFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        
        // Check if extension is supported
        if (!Arrays.asList(SUPPORTED_EXTENSIONS).contains(extension)) {
            return false;
        }
        
        // Check if file type is enabled in configuration
        try {
            DatabaseConfig config = DatabaseConfig.getInstance();
            return config.isFileTypeEnabled(extension);
        } catch (Exception e) {
            // If there's an error accessing config, fall back to checking supported extensions
            return Arrays.asList(SUPPORTED_EXTENSIONS).contains(extension);
        }
    }
    
    /**
     * Gets the list of currently enabled file extensions based on configuration.
     */
    public static String[] getEnabledExtensions() {
        try {
            DatabaseConfig config = DatabaseConfig.getInstance();
            return config.getEnabledFileTypes().toArray(new String[0]);
        } catch (Exception e) {
            // Fall back to all supported extensions if config is not available
            return SUPPORTED_EXTENSIONS.clone();
        }
    }
    
    /**
     * Sets a callback to receive status updates during scanning.
     * 
     * @param statusCallback A consumer that accepts status messages
     */
    public void setStatusCallback(Consumer<String> statusCallback) {
        this.statusCallback = statusCallback;
    }
    
    /**
     * Sets a callback to receive progress updates during scanning.
     * 
     * @param progressCallback A consumer that accepts progress percentage (0-100)
     */
    public void setProgressCallback(Consumer<Integer> progressCallback) {
        this.progressCallback = progressCallback;
    }
    
    /**
     * Sets a callback to receive individual file processing updates.
     * 
     * @param fileProcessingCallback A consumer that accepts file processing messages
     */
    public void setFileProcessingCallback(Consumer<String> fileProcessingCallback) {
        this.fileProcessingCallback = fileProcessingCallback;
    }
    
    /**
     * Sets a callback to receive detailed progress information.
     * 
     * @param detailedProgressCallback A consumer that accepts ScanProgress objects
     */
    public void setDetailedProgressCallback(Consumer<ScanProgress> detailedProgressCallback) {
        this.detailedProgressCallback = detailedProgressCallback;
    }
    
    /**
     * Requests the scanner to stop any ongoing operations.
     */
    public void requestStop() {
        this.stopRequested = true;
    }

    public static void main(String[] args) {
        MusicFileScanner scanner = new MusicFileScanner();

        // Set status callback to print status messages
        scanner.setStatusCallback(System.out::println);

        // Test with a single directory
        List<String> singleDirectory = Arrays.asList("/Users/richard/Music");
        List<MusicFile> singleDirMusicFiles = scanner.scanMusicFiles(singleDirectory);
        logger.info("Music files found in single directory: {}", singleDirMusicFiles.size());

        // Test with multiple directories
        List<String> multipleDirectories = Arrays.asList(
            "smb://HastingNASty._smb._tcp.local/Music/flac",
            "/Users/richard/Music/Music/Media"
        );
        List<MusicFile> multipleDirMusicFiles = scanner.scanMusicFiles(multipleDirectories);
        logger.info("Music files found in multiple directories: {}", multipleDirMusicFiles.size());
    }
}