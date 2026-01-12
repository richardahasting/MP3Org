package org.hasting.dto;

/**
 * Data Transfer Object for real-time scanning progress updates.
 * Sent via WebSocket to connected clients during directory scanning.
 *
 * Part of Issue #69 - Web UI Migration (Phase 2)
 */
public record ScanProgressDTO(
    String sessionId,
    String stage,           // "starting", "scanning", "reading_tags", "saving", "completed", "cancelled", "error"
    String currentDirectory,
    String currentFile,
    int filesFound,
    int filesProcessed,
    int totalDirectories,
    int directoriesProcessed,
    int percentComplete,
    String message,
    boolean isComplete,
    boolean isCancelled,
    String error
) {
    /**
     * Creates a starting progress message.
     */
    public static ScanProgressDTO starting(String sessionId, int totalDirectories) {
        return new ScanProgressDTO(
            sessionId, "starting", "", "", 0, 0, totalDirectories, 0, 0,
            "Starting scan...", false, false, null
        );
    }

    /**
     * Creates a scanning progress message.
     */
    public static ScanProgressDTO scanning(String sessionId, String directory, int filesFound,
                                           int totalDirs, int dirsProcessed) {
        int percent = totalDirs > 0 ? (dirsProcessed * 100) / totalDirs : 0;
        return new ScanProgressDTO(
            sessionId, "scanning", directory, "", filesFound, 0, totalDirs, dirsProcessed, percent,
            "Scanning: " + directory, false, false, null
        );
    }

    /**
     * Creates a tag reading progress message.
     */
    public static ScanProgressDTO readingTags(String sessionId, String directory, String file,
                                              int filesFound, int filesProcessed,
                                              int totalDirs, int dirsProcessed) {
        int percent = filesFound > 0 ? (filesProcessed * 100) / filesFound : 0;
        return new ScanProgressDTO(
            sessionId, "reading_tags", directory, file, filesFound, filesProcessed, totalDirs, dirsProcessed, percent,
            "Reading: " + file, false, false, null
        );
    }

    /**
     * Creates a saving progress message.
     */
    public static ScanProgressDTO saving(String sessionId, int filesProcessed) {
        return new ScanProgressDTO(
            sessionId, "saving", "", "", filesProcessed, filesProcessed, 0, 0, 95,
            "Saving " + filesProcessed + " files to database...", false, false, null
        );
    }

    /**
     * Creates a completion message.
     */
    public static ScanProgressDTO completed(String sessionId, int totalFiles) {
        return new ScanProgressDTO(
            sessionId, "completed", "", "", totalFiles, totalFiles, 0, 0, 100,
            "Scan complete: " + totalFiles + " files found", true, false, null
        );
    }

    /**
     * Creates a cancellation message.
     */
    public static ScanProgressDTO cancelled(String sessionId, int filesProcessed) {
        return new ScanProgressDTO(
            sessionId, "cancelled", "", "", 0, filesProcessed, 0, 0, 0,
            "Scan cancelled", true, true, null
        );
    }

    /**
     * Creates an error message.
     */
    public static ScanProgressDTO error(String sessionId, String errorMessage) {
        return new ScanProgressDTO(
            sessionId, "error", "", "", 0, 0, 0, 0, 0,
            "Error: " + errorMessage, true, false, errorMessage
        );
    }
}
