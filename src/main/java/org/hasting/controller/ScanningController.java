package org.hasting.controller;

import org.hasting.service.ScanningService;
import org.hasting.service.ScanningService.DirectoryEntry;
import org.hasting.service.ScanningService.ScanSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for directory scanning operations.
 * Provides endpoints to start/cancel scans, browse directories, and view scan history.
 *
 * Part of Issue #69 - Web UI Migration (Phase 2)
 */
@RestController
@RequestMapping("/api/v1/scanning")
public class ScanningController {

    private final ScanningService scanningService;

    public ScanningController(ScanningService scanningService) {
        this.scanningService = scanningService;
    }

    /**
     * Starts a new directory scan.
     *
     * POST /api/v1/scanning/start
     * Body: { "directories": ["/path/to/music", "/another/path"] }
     *
     * @return Session ID for tracking progress via WebSocket
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startScan(@RequestBody ScanRequest request) {
        if (request.directories() == null || request.directories().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "At least one directory path is required"
            ));
        }

        String sessionId = scanningService.startScan(request.directories());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "Scan started. Subscribe to WebSocket topic /topic/scanning/" + sessionId + " for progress updates.");
        response.put("directories", request.directories());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets the status of a scanning session.
     *
     * GET /api/v1/scanning/status/{sessionId}
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String sessionId) {
        return scanningService.getSession(sessionId)
            .map(session -> {
                Map<String, Object> response = new HashMap<>();
                response.put("sessionId", session.sessionId);
                response.put("directories", session.directories);
                response.put("startTime", session.startTime);
                response.put("filesFound", session.filesFound);
                response.put("cancelled", session.cancelled);
                response.put("completed", session.completed);
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancels an active scanning session.
     *
     * POST /api/v1/scanning/cancel/{sessionId}
     */
    @PostMapping("/cancel/{sessionId}")
    public ResponseEntity<Map<String, Object>> cancelScan(@PathVariable String sessionId) {
        boolean cancelled = scanningService.cancelScan(sessionId);

        if (cancelled) {
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "Scan cancellation requested"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gets the list of previously scanned directories.
     *
     * GET /api/v1/scanning/directories
     */
    @GetMapping("/directories")
    public ResponseEntity<List<String>> getScanDirectories() {
        List<String> directories = scanningService.getScanDirectories();
        return ResponseEntity.ok(directories);
    }

    /**
     * Browses a directory on the server.
     *
     * GET /api/v1/scanning/browse?path=/some/path
     *
     * Returns directory contents for the file browser UI.
     */
    @GetMapping("/browse")
    public ResponseEntity<BrowseResponse> browseDirectory(
            @RequestParam(required = false) String path) {
        List<DirectoryEntry> entries = scanningService.browseDirectory(path);
        return ResponseEntity.ok(new BrowseResponse(path, entries));
    }

    /**
     * Request body for starting a scan.
     */
    public record ScanRequest(List<String> directories) {}

    /**
     * Response for directory browsing.
     */
    public record BrowseResponse(String currentPath, List<DirectoryEntry> entries) {}
}
