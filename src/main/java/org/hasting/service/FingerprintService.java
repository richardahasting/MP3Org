package org.hasting.service;

import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for generating and managing audio fingerprints using Chromaprint's fpcalc tool.
 * Fingerprints are used for accurate duplicate detection based on audio content.
 */
@Service
public class FingerprintService {

    private static final Logger logger = Log4Rich.getLogger(FingerprintService.class);

    private static final int DEFAULT_FINGERPRINT_DURATION = 30; // seconds
    private static final int DEFAULT_THREAD_COUNT = 4;

    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executor;

    private volatile boolean fpcalcAvailable = false;
    private String fpcalcPath = "fpcalc";

    public FingerprintService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.executor = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        checkFpcalcAvailability();
    }

    /**
     * Checks if fpcalc is available on the system.
     */
    public void checkFpcalcAvailability() {
        try {
            ProcessBuilder pb = new ProcessBuilder(fpcalcPath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            int exitCode = process.waitFor();

            if (exitCode == 0 && line != null) {
                fpcalcAvailable = true;
                logger.info("fpcalc is available: " + line);
            } else {
                fpcalcAvailable = false;
                logger.warn("fpcalc not found or returned error. Audio fingerprinting will be disabled.");
            }
        } catch (Exception e) {
            fpcalcAvailable = false;
            logger.warn("fpcalc not available: " + e.getMessage() + ". Install with: brew install chromaprint");
        }
    }

    /**
     * Returns whether fpcalc is available for fingerprinting.
     */
    public boolean isFpcalcAvailable() {
        return fpcalcAvailable;
    }

    /**
     * Generates a fingerprint for a single music file.
     *
     * @param filePath the path to the audio file
     * @param duration the number of seconds to analyze (default 30)
     * @return the fingerprint as a comma-separated string of integers, or null on failure
     */
    public FingerprintResult generateFingerprint(String filePath, int duration) {
        if (!fpcalcAvailable) {
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                fpcalcPath,
                "-length", String.valueOf(duration),
                "-raw",
                filePath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String fingerprint = null;
            int fpDuration = 0;
            StringBuilder output = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (line.startsWith("FINGERPRINT=")) {
                    fingerprint = line.substring("FINGERPRINT=".length());
                } else if (line.startsWith("DURATION=")) {
                    fpDuration = Integer.parseInt(line.substring("DURATION=".length()));
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && fingerprint != null) {
                return new FingerprintResult(fingerprint, fpDuration);
            } else {
                // Log the failure with details
                String errorOutput = output.toString().trim();
                if (errorOutput.isEmpty()) {
                    errorOutput = "No output from fpcalc";
                }
                logger.warn("fpcalc failed for '{}': exit code {}, output: {}",
                    filePath, exitCode, errorOutput);
                trackFailedFile(filePath, "fpcalc exit code " + exitCode + ": " + errorOutput);
            }
        } catch (Exception e) {
            logger.warn("Exception generating fingerprint for '{}': {}", filePath, e.getMessage());
            trackFailedFile(filePath, "Exception: " + e.getMessage());
        }

        return null;
    }

    // Track failed files for reporting
    private final java.util.concurrent.ConcurrentHashMap<String, String> failedFiles = new java.util.concurrent.ConcurrentHashMap<>();

    private void trackFailedFile(String filePath, String reason) {
        // Keep only the last 100 failures to avoid memory issues
        if (failedFiles.size() < 100) {
            failedFiles.put(filePath, reason);
        }
    }

    /**
     * Get the list of files that failed fingerprint generation.
     */
    public java.util.Map<String, String> getFailedFiles() {
        return new java.util.HashMap<>(failedFiles);
    }

    /**
     * Clear the failed files list.
     */
    public void clearFailedFiles() {
        failedFiles.clear();
    }

    /**
     * Generates fingerprints for all files that don't have them yet.
     * Runs in parallel for performance.
     *
     * @param sessionId optional session ID for progress updates via WebSocket
     * @return the number of fingerprints generated
     */
    public int generateMissingFingerprints(String sessionId) {
        if (!fpcalcAvailable) {
            logger.warn("Cannot generate fingerprints - fpcalc not available");
            return 0;
        }

        // Clear previous failures
        clearFailedFiles();

        List<MusicFile> filesWithoutFingerprints = DatabaseManager.getFilesWithoutFingerprints();
        int total = filesWithoutFingerprints.size();

        if (total == 0) {
            logger.info("All files already have fingerprints");
            return 0;
        }

        logger.info("Generating fingerprints for {} files using {} threads", total, DEFAULT_THREAD_COUNT);

        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger successful = new AtomicInteger(0);

        // Create tasks for parallel execution
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (MusicFile file : filesWithoutFingerprints) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                FingerprintResult result = generateFingerprint(file.getFilePath(), DEFAULT_FINGERPRINT_DURATION);

                if (result != null) {
                    boolean updated = DatabaseManager.updateFingerprint(
                        file.getId(),
                        result.fingerprint(),
                        result.duration()
                    );
                    if (updated) {
                        successful.incrementAndGet();
                    }
                }

                int done = completed.incrementAndGet();

                // Broadcast progress every 50 files or at completion
                if (done % 50 == 0 || done == total) {
                    broadcastProgress(sessionId, done, total, successful.get());
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int failed = total - successful.get();
        logger.info("Fingerprint generation complete: {}/{} successful, {} failed", successful.get(), total, failed);

        // Log details of failed files
        if (!failedFiles.isEmpty()) {
            logger.warn("Failed to generate fingerprints for {} files:", failedFiles.size());
            failedFiles.forEach((path, reason) -> {
                logger.warn("  {} - {}", path, reason);
            });
        }

        return successful.get();
    }

    /**
     * Generates fingerprints for a specific list of files.
     *
     * @param files the list of MusicFile objects to fingerprint
     * @param sessionId optional session ID for progress updates
     * @return the number of fingerprints generated
     */
    public int generateFingerprints(List<MusicFile> files, String sessionId) {
        if (!fpcalcAvailable || files.isEmpty()) {
            return 0;
        }

        int total = files.size();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger successful = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (MusicFile file : files) {
            if (file.hasFingerprint()) {
                completed.incrementAndGet();
                continue;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                FingerprintResult result = generateFingerprint(file.getFilePath(), DEFAULT_FINGERPRINT_DURATION);

                if (result != null) {
                    file.setFingerprint(result.fingerprint());
                    file.setFingerprintDuration(result.duration());

                    if (file.getId() != null) {
                        DatabaseManager.updateFingerprint(file.getId(), result.fingerprint(), result.duration());
                    }
                    successful.incrementAndGet();
                }

                int done = completed.incrementAndGet();
                if (done % 50 == 0 || done == total) {
                    broadcastProgress(sessionId, done, total, successful.get());
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return successful.get();
    }

    private void broadcastProgress(String sessionId, int completed, int total, int successful) {
        if (sessionId != null && messagingTemplate != null) {
            int percent = total > 0 ? (completed * 100) / total : 0;
            messagingTemplate.convertAndSend(
                "/topic/fingerprints/" + sessionId,
                new FingerprintProgress(completed, total, successful, percent)
            );
        }
    }

    /**
     * Gets the count of files with fingerprints.
     */
    public long getFilesWithFingerprintsCount() {
        List<MusicFile> all = DatabaseManager.getAllMusicFiles();
        return all.stream().filter(MusicFile::hasFingerprint).count();
    }

    /**
     * Gets the count of files without fingerprints.
     */
    public long getFilesWithoutFingerprintsCount() {
        return DatabaseManager.getFilesWithoutFingerprints().size();
    }

    /**
     * Result of fingerprint generation.
     */
    public record FingerprintResult(String fingerprint, int duration) {}

    /**
     * Progress update for fingerprint generation.
     */
    public record FingerprintProgress(int completed, int total, int successful, int percentComplete) {}
}
