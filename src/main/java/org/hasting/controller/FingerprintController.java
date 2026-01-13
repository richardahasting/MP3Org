package org.hasting.controller;

import org.hasting.service.FingerprintService;
import org.hasting.service.FingerprintService.FingerprintProgress;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for audio fingerprint operations.
 *
 * Endpoints:
 * - GET  /api/v1/fingerprints/status        - Get fingerprint system status
 * - POST /api/v1/fingerprints/generate      - Start fingerprint generation for all missing
 * - GET  /api/v1/fingerprints/generate/{id} - Get generation progress (via WebSocket preferred)
 *
 * WebSocket: /topic/fingerprints/{sessionId} for real-time progress
 *
 * Part of Issue #88 - Audio Fingerprinting
 */
@RestController
@RequestMapping("/api/v1/fingerprints")
public class FingerprintController {

    private final FingerprintService fingerprintService;
    private final Map<String, FingerprintGenerationSession> activeSessions = new ConcurrentHashMap<>();

    public FingerprintController(FingerprintService fingerprintService) {
        this.fingerprintService = fingerprintService;
    }

    /**
     * Get fingerprint system status including fpcalc availability and file counts.
     */
    @GetMapping("/status")
    public ResponseEntity<FingerprintStatus> getStatus() {
        boolean available = fingerprintService.isFpcalcAvailable();
        long withFingerprints = available ? fingerprintService.getFilesWithFingerprintsCount() : 0;
        long withoutFingerprints = available ? fingerprintService.getFilesWithoutFingerprintsCount() : 0;

        return ResponseEntity.ok(new FingerprintStatus(
            available,
            withFingerprints,
            withoutFingerprints,
            available ? "fpcalc available" : "fpcalc not installed - run: brew install chromaprint"
        ));
    }

    /**
     * Start fingerprint generation for all files that don't have fingerprints.
     * Returns a session ID for tracking progress via WebSocket.
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> startGeneration() {
        if (!fingerprintService.isFpcalcAvailable()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "fpcalc not available",
                "message", "Install chromaprint to enable fingerprinting: brew install chromaprint"
            ));
        }

        long filesNeeded = fingerprintService.getFilesWithoutFingerprintsCount();
        if (filesNeeded == 0) {
            return ResponseEntity.ok(Map.of(
                "status", "complete",
                "message", "All files already have fingerprints"
            ));
        }

        String sessionId = UUID.randomUUID().toString();
        FingerprintGenerationSession session = new FingerprintGenerationSession(sessionId, filesNeeded);
        activeSessions.put(sessionId, session);

        // Run fingerprint generation asynchronously
        final long totalToProcess = filesNeeded;
        CompletableFuture.runAsync(() -> {
            try {
                int generated = fingerprintService.generateMissingFingerprints(sessionId);
                int failed = (int) totalToProcess - generated;
                session.setCompleted(generated, failed);

                // If there were failures, set an error message with summary
                if (failed > 0) {
                    Map<String, String> failures = fingerprintService.getFailedFiles();
                    if (!failures.isEmpty()) {
                        // Get the first few failure reasons for the error message
                        StringBuilder errorMsg = new StringBuilder();
                        errorMsg.append(failed).append(" files failed. ");
                        int shown = 0;
                        for (Map.Entry<String, String> entry : failures.entrySet()) {
                            if (shown >= 3) {
                                errorMsg.append("... and ").append(failures.size() - 3).append(" more");
                                break;
                            }
                            if (shown > 0) errorMsg.append("; ");
                            // Extract just the filename from the path
                            String path = entry.getKey();
                            String filename = path.substring(path.lastIndexOf('/') + 1);
                            errorMsg.append(filename).append(": ").append(entry.getValue());
                            shown++;
                        }
                        session.setError(errorMsg.toString());
                    }
                }
            } catch (Exception e) {
                session.setError(e.getMessage());
            }
        });

        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "status", "started",
            "filesToProcess", filesNeeded,
            "websocket", "/topic/fingerprints/" + sessionId
        ));
    }

    /**
     * Get the status of a fingerprint generation session.
     * Note: WebSocket provides real-time updates at /topic/fingerprints/{sessionId}
     */
    @GetMapping("/generate/{sessionId}")
    public ResponseEntity<Map<String, Object>> getGenerationStatus(@PathVariable String sessionId) {
        FingerprintGenerationSession session = activeSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("sessionId", sessionId);
        response.put("status", session.getStatus());
        response.put("totalFiles", session.getTotalFiles());
        response.put("completed", session.getCompleted());
        response.put("failed", session.getFailed());
        response.put("error", session.getError() != null ? session.getError() : "");

        return ResponseEntity.ok(response);
    }

    /**
     * Get the list of files that failed fingerprint generation.
     */
    @GetMapping("/failures")
    public ResponseEntity<Map<String, Object>> getFailedFiles() {
        Map<String, String> failures = fingerprintService.getFailedFiles();
        return ResponseEntity.ok(Map.of(
            "count", failures.size(),
            "failures", failures
        ));
    }

    /**
     * Response for fingerprint system status.
     */
    public record FingerprintStatus(
        boolean fpcalcAvailable,
        long filesWithFingerprints,
        long filesWithoutFingerprints,
        String message
    ) {}

    /**
     * Tracks a fingerprint generation session.
     */
    private static class FingerprintGenerationSession {
        private final String sessionId;
        private final long totalFiles;
        private int completed = 0;
        private int failed = 0;
        private String status = "running";
        private String error = null;

        FingerprintGenerationSession(String sessionId, long totalFiles) {
            this.sessionId = sessionId;
            this.totalFiles = totalFiles;
        }

        String getSessionId() { return sessionId; }
        long getTotalFiles() { return totalFiles; }
        int getCompleted() { return completed; }
        int getFailed() { return failed; }
        String getStatus() { return status; }
        String getError() { return error; }

        void setCompleted(int completed, int failed) {
            this.completed = completed;
            this.failed = failed;
            this.status = "completed";
        }

        void setError(String error) {
            this.error = error;
            this.status = "error";
        }
    }
}
