package org.hasting.service;

import org.hasting.dto.ScanProgressDTO;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.hasting.util.MusicFileScanner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for managing directory scanning operations with real-time WebSocket progress.
 * Wraps the existing MusicFileScanner to provide web-friendly scanning capabilities.
 *
 * Part of Issue #69 - Web UI Migration (Phase 2)
 */
@Service
public class ScanningService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Map<String, ScanSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Represents an active scanning session.
     */
    public static class ScanSession {
        public final String sessionId;
        public final List<String> directories;
        public final long startTime;
        public volatile boolean cancelled = false;
        public volatile boolean completed = false;
        public volatile int filesFound = 0;
        private MusicFileScanner scanner;

        public ScanSession(String sessionId, List<String> directories) {
            this.sessionId = sessionId;
            this.directories = directories;
            this.startTime = System.currentTimeMillis();
        }

        public void setScanner(MusicFileScanner scanner) {
            this.scanner = scanner;
        }

        public void cancel() {
            this.cancelled = true;
            if (scanner != null) {
                scanner.requestStop();
            }
        }
    }

    public ScanningService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Starts a new scanning session asynchronously.
     *
     * @param directories List of directory paths to scan
     * @return The session ID for tracking progress
     */
    public String startScan(List<String> directories) {
        String sessionId = UUID.randomUUID().toString();
        ScanSession session = new ScanSession(sessionId, directories);
        activeSessions.put(sessionId, session);

        // Send initial progress
        sendProgress(ScanProgressDTO.starting(sessionId, directories.size()));

        // Start scanning in background thread
        executorService.submit(() -> performScan(session));

        return sessionId;
    }

    /**
     * Performs the actual scanning operation.
     */
    private void performScan(ScanSession session) {
        String sessionId = session.sessionId;
        MusicFileScanner scanner = new MusicFileScanner();
        session.setScanner(scanner);

        List<MusicFile> allFiles = new ArrayList<>();

        try {
            // Set up progress callbacks
            scanner.setDetailedProgressCallback(progress -> {
                if (session.cancelled) return;

                ScanProgressDTO dto;
                if ("scanning".equals(progress.stage)) {
                    dto = ScanProgressDTO.scanning(
                        sessionId, progress.currentDirectory, progress.filesFound,
                        progress.totalDirectories, progress.directoriesProcessed
                    );
                } else {
                    dto = ScanProgressDTO.readingTags(
                        sessionId, progress.currentDirectory, progress.currentFile,
                        progress.filesFound, progress.filesProcessed,
                        progress.totalDirectories, progress.directoriesProcessed
                    );
                }
                session.filesFound = progress.filesFound;
                sendProgress(dto);
            });

            // Perform the scan
            List<MusicFile> files = scanner.findAllMusicFilesWithProgress(session.directories);

            if (session.cancelled) {
                sendProgress(ScanProgressDTO.cancelled(sessionId, session.filesFound));
                return;
            }

            allFiles.addAll(files);

            // Save to database
            if (!allFiles.isEmpty()) {
                sendProgress(ScanProgressDTO.saving(sessionId, allFiles.size()));

                // Use batch save for performance
                int savedCount = DatabaseManager.saveMusicFilesBatch(allFiles);

                // Record the scan directories
                for (String dir : session.directories) {
                    DatabaseManager.recordScanDirectory(dir);
                }
            }

            // Mark complete
            session.completed = true;
            session.filesFound = allFiles.size();
            sendProgress(ScanProgressDTO.completed(sessionId, allFiles.size()));

        } catch (Exception e) {
            sendProgress(ScanProgressDTO.error(sessionId, e.getMessage()));
        } finally {
            // Clean up after a delay to allow clients to receive final message
            executorService.submit(() -> {
                try {
                    Thread.sleep(30000); // Keep session for 30 seconds after completion
                } catch (InterruptedException ignored) {}
                activeSessions.remove(sessionId);
            });
        }
    }

    /**
     * Cancels an active scanning session.
     *
     * @param sessionId The session to cancel
     * @return true if session was found and cancelled
     */
    public boolean cancelScan(String sessionId) {
        ScanSession session = activeSessions.get(sessionId);
        if (session != null && !session.completed) {
            session.cancel();
            return true;
        }
        return false;
    }

    /**
     * Gets the status of a scanning session.
     */
    public Optional<ScanSession> getSession(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    /**
     * Gets all previously scanned directories from the database.
     */
    public List<String> getScanDirectories() {
        return DatabaseManager.getScanDirectories();
    }

    /**
     * Browses a directory on the server and returns its contents.
     *
     * @param path The directory path to browse (null/empty for root)
     * @return List of directory entries
     */
    public List<DirectoryEntry> browseDirectory(String path) {
        List<DirectoryEntry> entries = new ArrayList<>();

        // Determine the starting path
        File dir;
        if (path == null || path.isEmpty()) {
            // Return common root paths
            String userHome = System.getProperty("user.home");
            entries.add(new DirectoryEntry(userHome, "Home", true, true));
            entries.add(new DirectoryEntry(userHome + "/Music", "Music", true, true));
            entries.add(new DirectoryEntry("/Volumes", "Volumes", true, true));
            return entries;
        }

        dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return entries;
        }

        // Add parent directory option
        File parent = dir.getParentFile();
        if (parent != null) {
            entries.add(new DirectoryEntry(parent.getAbsolutePath(), "..", true, true));
        }

        // List directory contents
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                // Directories first, then alphabetically
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                // Skip hidden files
                if (file.getName().startsWith(".")) continue;

                entries.add(new DirectoryEntry(
                    file.getAbsolutePath(),
                    file.getName(),
                    file.isDirectory(),
                    file.canRead()
                ));
            }
        }

        return entries;
    }

    /**
     * Sends a progress update via WebSocket.
     */
    private void sendProgress(ScanProgressDTO progress) {
        messagingTemplate.convertAndSend("/topic/scanning/" + progress.sessionId(), progress);
    }

    /**
     * Represents a directory entry for the browser.
     */
    public record DirectoryEntry(
        String path,
        String name,
        boolean isDirectory,
        boolean canRead
    ) {}
}
