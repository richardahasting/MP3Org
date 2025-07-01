package org.hasting.util.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log handler that outputs log records to files with rotation support.
 * 
 * <p>This handler provides file-based logging with features including:</p>
 * <ul>
 *   <li>Automatic file creation and directory setup</li>
 *   <li>File rotation based on size or time</li>
 *   <li>Configurable file naming patterns</li>
 *   <li>Automatic cleanup of old log files</li>
 *   <li>Thread-safe file writing</li>
 * </ul>
 * 
 * @since 1.0
 */
public class FileLogHandler implements LogHandler {
    
    private final Path logFilePath;
    private final LogFormatter formatter;
    private final long maxFileSize;
    private final int maxFiles;
    private volatile PrintWriter writer;
    private volatile boolean enabled;
    private final Object writeLock = new Object();
    
    /**
     * Creates a file log handler with default settings.
     * 
     * @param logFilePath Path to the log file
     */
    public FileLogHandler(String logFilePath) {
        this(logFilePath, new DefaultLogFormatter(), 10 * 1024 * 1024, 5); // 10MB, 5 files
    }
    
    /**
     * Creates a file log handler with custom settings.
     * 
     * @param logFilePath Path to the log file
     * @param formatter The formatter to use for log messages
     * @param maxFileSize Maximum size in bytes before rotation (0 = no limit)
     * @param maxFiles Maximum number of rotated files to keep (0 = unlimited)
     */
    public FileLogHandler(String logFilePath, LogFormatter formatter, long maxFileSize, int maxFiles) {
        this.logFilePath = Paths.get(logFilePath);
        this.formatter = formatter;
        this.maxFileSize = maxFileSize;
        this.maxFiles = maxFiles;
        this.enabled = true;
        
        initializeWriter();
    }
    
    /**
     * Initializes the file writer, creating directories and files as needed.
     */
    private void initializeWriter() {
        try {
            // Create parent directories if they don't exist
            Path parentDir = logFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Create the file writer with append mode
            FileWriter fileWriter = new FileWriter(logFilePath.toFile(), true);
            this.writer = new PrintWriter(new BufferedWriter(fileWriter), false); // Manual flush control
            
        } catch (IOException e) {
            System.err.println("Failed to initialize log file writer: " + e.getMessage());
            this.enabled = false;
        }
    }
    
    @Override
    public void handle(LogRecord record) {
        if (!enabled || writer == null) {
            return;
        }
        
        synchronized (writeLock) {
            try {
                // Check if rotation is needed
                if (needsRotation()) {
                    rotateFile();
                }
                
                // Write the log message
                String formattedMessage = formatter.format(record);
                writer.println(formattedMessage);
                
                // Write exception stack trace if present
                if (record.hasException()) {
                    record.getException().printStackTrace(writer);
                }
                
                // Flush periodically for important messages
                if (record.getLevel().getPriority() >= LogLevel.WARNING.getPriority()) {
                    writer.flush();
                }
                
            } catch (Exception e) {
                // Fallback error handling
                System.err.println("LOG ERROR: Failed to write to log file: " + e.getMessage());
                System.err.println("Original message: " + record.getFormattedMessage());
            }
        }
    }
    
    @Override
    public void flush() {
        if (enabled && writer != null) {
            synchronized (writeLock) {
                writer.flush();
            }
        }
    }
    
    @Override
    public void close() {
        enabled = false;
        synchronized (writeLock) {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        }
    }
    
    @Override
    public String getName() {
        return "File (" + logFilePath.toString() + ")";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Checks if the log file needs to be rotated based on size.
     * 
     * @return true if rotation is needed, false otherwise
     */
    private boolean needsRotation() {
        if (maxFileSize <= 0) {
            return false; // No size limit
        }
        
        try {
            return Files.exists(logFilePath) && Files.size(logFilePath) >= maxFileSize;
        } catch (IOException e) {
            return false; // If we can't check size, don't rotate
        }
    }
    
    /**
     * Rotates the current log file by moving it to a backup name and creating a new file.
     */
    private void rotateFile() {
        try {
            // Close current writer
            if (writer != null) {
                writer.close();
            }
            
            // Generate timestamp for backup file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            
            // Move current file to backup
            Path backupPath = Paths.get(logFilePath.toString() + "." + timestamp);
            Files.move(logFilePath, backupPath);
            
            // Clean up old files if needed
            cleanupOldFiles();
            
            // Reinitialize writer with new file
            initializeWriter();
            
        } catch (IOException e) {
            System.err.println("Failed to rotate log file: " + e.getMessage());
            // Try to reinitialize writer anyway
            initializeWriter();
        }
    }
    
    /**
     * Removes old log files to stay within the maximum file count limit.
     */
    private void cleanupOldFiles() {
        if (maxFiles <= 0) {
            return; // No limit on file count
        }
        
        try {
            Path parentDir = logFilePath.getParent();
            if (parentDir == null) {
                return;
            }
            
            String baseFileName = logFilePath.getFileName().toString();
            
            // Find all backup files for this log
            File[] backupFiles = parentDir.toFile().listFiles((dir, name) -> 
                name.startsWith(baseFileName + ".") && 
                name.matches(baseFileName + "\\.\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}")
            );
            
            if (backupFiles != null && backupFiles.length > maxFiles) {
                // Sort by last modified time (oldest first)
                java.util.Arrays.sort(backupFiles, 
                    (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                
                // Delete oldest files
                int filesToDelete = backupFiles.length - maxFiles;
                for (int i = 0; i < filesToDelete; i++) {
                    if (!backupFiles[i].delete()) {
                        System.err.println("Failed to delete old log file: " + backupFiles[i].getName());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to cleanup old log files: " + e.getMessage());
        }
    }
    
    /**
     * Gets the path to the log file.
     * 
     * @return The log file path
     */
    public Path getLogFilePath() {
        return logFilePath;
    }
    
    /**
     * Gets the maximum file size before rotation.
     * 
     * @return The maximum file size in bytes
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * Gets the maximum number of backup files to keep.
     * 
     * @return The maximum number of files
     */
    public int getMaxFiles() {
        return maxFiles;
    }
}