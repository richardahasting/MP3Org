package org.hasting.util.logging;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages log file backup, rotation, and compression for the MP3Org logging framework.
 * 
 * <p>This utility class provides comprehensive log file management including:</p>
 * <ul>
 *   <li>Automatic log file rotation based on size thresholds</li>
 *   <li>Configurable backup file retention with automatic cleanup</li>
 *   <li>Gzip compression of backup files to save disk space</li>
 *   <li>Timestamp-based backup file naming for easy identification</li>
 *   <li>Backup directory management and organization</li>
 * </ul>
 * 
 * <p>The backup system integrates seamlessly with the existing logging configuration
 * and provides both automatic and manual backup capabilities.</p>
 * 
 * @see LoggingConfiguration
 * @see MP3OrgLoggingManager
 * @since 1.0
 */
public class LogBackupManager {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(LogBackupManager.class);
    
    // Date format for backup file timestamps
    private static final SimpleDateFormat BACKUP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    
    // Pattern to match backup files for cleanup
    private static final Pattern BACKUP_FILE_PATTERN = Pattern.compile(".*-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.log(\\.gz)?$");
    
    /**
     * Checks if the specified log file exceeds the size threshold and needs rotation.
     * 
     * @param logFilePath The path to the log file to check
     * @param maxSizeMB The maximum size in megabytes before rotation
     * @return true if the file should be rotated, false otherwise
     */
    public static boolean shouldRotateLogFile(String logFilePath, int maxSizeMB) {
        if (logFilePath == null || maxSizeMB <= 0) {
            return false;
        }
        
        File logFile = new File(logFilePath);
        if (!logFile.exists() || !logFile.isFile()) {
            return false;
        }
        
        long fileSizeBytes = logFile.length();
        long maxSizeBytes = maxSizeMB * 1024L * 1024L; // Convert MB to bytes
        
        boolean shouldRotate = fileSizeBytes >= maxSizeBytes;
        
        if (shouldRotate) {
            logger.info(String.format("Log file {} has reached size limit: {} bytes (limit: {} MB)", logFilePath, fileSizeBytes, maxSizeMB));
        }
        
        return shouldRotate;
    }
    
    /**
     * Creates a backup of the specified log file with optional compression.
     * 
     * @param config The logging configuration containing backup settings
     * @return true if backup was successful, false otherwise
     */
    public static boolean createBackup(LoggingConfiguration config) {
        if (config == null || !config.isBackupEnabled() || config.getFilePath() == null) {
            logger.debug("Backup not enabled or configured, skipping backup creation");
            return false;
        }
        
        File logFile = new File(config.getFilePath());
        if (!logFile.exists() || logFile.length() == 0) {
            logger.debug(String.format("Log file does not exist or is empty, skipping backup: {}", config.getFilePath()));
            return false;
        }
        
        try {
            // Ensure backup directory exists
            String backupDirPath = config.getFullBackupDirectoryPath();
            File backupDir = new File(backupDirPath);
            if (!backupDir.exists()) {
                if (!backupDir.mkdirs()) {
                    logger.error(String.format("Failed to create backup directory: {}", backupDirPath));
                    return false;
                }
                logger.info(String.format("Created backup directory: {}", backupDirPath));
            }
            
            // Generate backup file name with timestamp
            String timestamp = BACKUP_DATE_FORMAT.format(new Date());
            String logFileName = logFile.getName();
            String baseFileName = logFileName.replaceFirst("\\.log$", ""); // Remove .log extension
            String backupFileName = baseFileName + "-" + timestamp + ".log";
            
            // Add compression extension if enabled
            if (config.isCompressionEnabled()) {
                backupFileName += ".gz";
            }
            
            File backupFile = new File(backupDir, backupFileName);
            
            // Create the backup
            boolean success;
            if (config.isCompressionEnabled()) {
                success = createCompressedBackup(logFile, backupFile, config.getCompressionLevel());
            } else {
                success = createUncompressedBackup(logFile, backupFile);
            }
            
            if (success) {
                logger.info(String.format("Successfully created backup: {}", backupFile.getAbsolutePath()));
                
                // Clean up old backups
                cleanupOldBackups(config);
                
                return true;
            } else {
                logger.error(String.format("Failed to create backup: {}", backupFile.getAbsolutePath()));
                return false;
            }
            
        } catch (Exception e) {
            logger.error(String.format("Unexpected error during backup creation: {}", e.getMessage()), e);
            return false;
        }
    }
    
    /**
     * Creates an uncompressed backup by copying the log file.
     * 
     * @param sourceFile The source log file
     * @param backupFile The destination backup file
     * @return true if successful, false otherwise
     */
    private static boolean createUncompressedBackup(File sourceFile, File backupFile) {
        try {
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.debug(String.format("Created uncompressed backup: {}", backupFile.getAbsolutePath()));
            return true;
        } catch (IOException e) {
            logger.error(String.format("Failed to create uncompressed backup: {}", e.getMessage()), e);
            return false;
        }
    }
    
    /**
     * Creates a compressed backup using gzip compression.
     * 
     * @param sourceFile The source log file
     * @param backupFile The destination backup file (should end with .gz)
     * @param compressionLevel The gzip compression level (1-9)
     * @return true if successful, false otherwise
     */
    private static boolean createCompressedBackup(File sourceFile, File backupFile, int compressionLevel) {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(backupFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos) {
                 {
                     // Set compression level
                     def.setLevel(compressionLevel);
                 }
             };
             BufferedInputStream bis = new BufferedInputStream(fis);
             BufferedOutputStream bos = new BufferedOutputStream(gzos)) {
            
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            
            logger.debug(String.format("Created compressed backup (level {}): {}", compressionLevel, backupFile.getAbsolutePath()));
            return true;
            
        } catch (IOException e) {
            logger.error(String.format("Failed to create compressed backup: {}", e.getMessage()), e);
            return false;
        }
    }
    
    /**
     * Cleans up old backup files, keeping only the specified number of most recent backups.
     * 
     * @param config The logging configuration containing cleanup settings
     */
    public static void cleanupOldBackups(LoggingConfiguration config) {
        if (config == null || !config.isBackupEnabled()) {
            return;
        }
        
        try {
            String backupDirPath = config.getFullBackupDirectoryPath();
            File backupDir = new File(backupDirPath);
            
            if (!backupDir.exists() || !backupDir.isDirectory()) {
                logger.debug(String.format("Backup directory does not exist, no cleanup needed: {}", backupDirPath));
                return;
            }
            
            // Find all backup files
            List<File> backupFiles = new ArrayList<>();
            try (Stream<Path> paths = Files.list(backupDir.toPath())) {
                paths.filter(Files::isRegularFile)
                     .map(Path::toFile)
                     .filter(file -> BACKUP_FILE_PATTERN.matcher(file.getName()).matches())
                     .forEach(backupFiles::add);
            }
            
            // Sort by last modified time (newest first)
            backupFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            
            // Remove excess backup files
            int maxBackups = config.getBackupCount();
            if (backupFiles.size() > maxBackups) {
                List<File> filesToDelete = backupFiles.subList(maxBackups, backupFiles.size());
                
                for (File fileToDelete : filesToDelete) {
                    try {
                        if (fileToDelete.delete()) {
                            logger.info(String.format("Deleted old backup file: {}", fileToDelete.getName()));
                        } else {
                            logger.warning(String.format("Failed to delete old backup file: %s", fileToDelete.getName()));
                        }
                    } catch (Exception e) {
                        logger.error(String.format("Error deleting backup file {}: {}", fileToDelete.getName()), e.getMessage());
                    }
                }
                
                logger.info(String.format("Cleanup completed: kept {} backups, removed {} old files", maxBackups, filesToDelete.size()));
            } else {
                logger.debug(String.format("No cleanup needed: {} backups (limit: {})", backupFiles.size()), maxBackups);
            }
            
        } catch (Exception e) {
            logger.error(String.format("Error during backup cleanup: {}", e.getMessage()), e);
        }
    }
    
    /**
     * Rotates the current log file by creating a backup and clearing the original.
     * 
     * @param config The logging configuration
     * @return true if rotation was successful, false otherwise
     */
    public static boolean rotateLogFile(LoggingConfiguration config) {
        if (config == null || config.getFilePath() == null) {
            logger.warning("Cannot rotate log file: invalid configuration");
            return false;
        }
        
        File logFile = new File(config.getFilePath());
        if (!logFile.exists()) {
            logger.debug(String.format("Log file does not exist, no rotation needed: {}", config.getFilePath()));
            return true;
        }
        
        logger.info(String.format("Starting log file rotation for: {}", config.getFilePath()));
        
        // Create backup first
        boolean backupSuccess = createBackup(config);
        if (!backupSuccess) {
            logger.error("Failed to create backup during rotation, aborting rotation");
            return false;
        }
        
        // Clear the original log file
        try {
            // Truncate the file to 0 bytes instead of deleting to avoid file handle issues
            try (FileWriter writer = new FileWriter(logFile, false)) {
                // Opening in overwrite mode truncates the file
            }
            
            logger.info(String.format("Successfully rotated log file: {}", config.getFilePath()));
            return true;
            
        } catch (IOException e) {
            logger.error(String.format("Failed to clear log file after backup: {}", e.getMessage()), e);
            return false;
        }
    }
    
    /**
     * Forces an immediate backup of the current log file.
     * This method can be called manually regardless of size thresholds.
     * 
     * @param config The logging configuration
     * @return true if backup was successful, false otherwise
     */
    public static boolean forceBackup(LoggingConfiguration config) {
        logger.info("Forcing immediate backup of log file");
        return createBackup(config);
    }
    
    /**
     * Gets information about existing backup files.
     * 
     * @param config The logging configuration
     * @return A list of backup file information
     */
    public static List<BackupFileInfo> getBackupFileInfo(LoggingConfiguration config) {
        List<BackupFileInfo> backupInfo = new ArrayList<>();
        
        if (config == null || !config.isBackupEnabled()) {
            return backupInfo;
        }
        
        try {
            String backupDirPath = config.getFullBackupDirectoryPath();
            File backupDir = new File(backupDirPath);
            
            if (!backupDir.exists() || !backupDir.isDirectory()) {
                return backupInfo;
            }
            
            try (Stream<Path> paths = Files.list(backupDir.toPath())) {
                paths.filter(Files::isRegularFile)
                     .map(Path::toFile)
                     .filter(file -> BACKUP_FILE_PATTERN.matcher(file.getName()).matches())
                     .forEach(file -> {
                         BackupFileInfo info = new BackupFileInfo(
                             file.getName(),
                             file.length(),
                             new Date(file.lastModified()),
                             file.getName().endsWith(".gz")
                         );
                         backupInfo.add(info);
                     });
            }
            
            // Sort by creation time (newest first)
            backupInfo.sort((a, b) -> b.getCreationTime().compareTo(a.getCreationTime()));
            
        } catch (Exception e) {
            logger.error(String.format("Error getting backup file information: {}", e.getMessage()), e);
        }
        
        return backupInfo;
    }
    
    /**
     * Information about a backup file.
     */
    public static class BackupFileInfo {
        private final String fileName;
        private final long sizeBytes;
        private final Date creationTime;
        private final boolean compressed;
        
        public BackupFileInfo(String fileName, long sizeBytes, Date creationTime, boolean compressed) {
            this.fileName = fileName;
            this.sizeBytes = sizeBytes;
            this.creationTime = creationTime;
            this.compressed = compressed;
        }
        
        public String getFileName() { return fileName; }
        public long getSizeBytes() { return sizeBytes; }
        public Date getCreationTime() { return creationTime; }
        public boolean isCompressed() { return compressed; }
        
        public String getFormattedSize() {
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            } else if (sizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", sizeBytes / 1024.0);
            } else {
                return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
            }
        }
    }
}