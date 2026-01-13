package org.hasting.controller;

import org.hasting.dto.DuplicateGroupDTO;
import org.hasting.dto.MusicFileDTO;
import org.hasting.service.DuplicateService;
import org.hasting.service.DuplicateService.DuplicateScanStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for duplicate detection operations.
 *
 * Endpoints:
 * - GET  /api/v1/duplicates              - Get all duplicate groups
 * - GET  /api/v1/duplicates/count        - Get count of duplicate groups
 * - GET  /api/v1/duplicates/{groupId}    - Get specific duplicate group
 * - GET  /api/v1/duplicates/similar/{id} - Find files similar to a specific file
 * - POST /api/v1/duplicates/compare      - Compare two files
 * - POST /api/v1/duplicates/scan         - Start async duplicate scan
 * - GET  /api/v1/duplicates/scan/{id}    - Get scan status
 * - POST /api/v1/duplicates/scan/{id}/cancel - Cancel scan
 * - POST /api/v1/duplicates/refresh      - Invalidate cache and refresh
 * - DELETE /api/v1/duplicates/{groupId}/keep/{fileId} - Keep one file, delete others
 */
@RestController
@RequestMapping("/api/v1/duplicates")
public class DuplicateController {

    private final DuplicateService duplicateService;

    public DuplicateController(DuplicateService duplicateService) {
        this.duplicateService = duplicateService;
    }

    /**
     * Get duplicate groups with pagination.
     * @param page Page number (0-indexed), default 0
     * @param size Page size, default 25
     */
    @GetMapping
    public ResponseEntity<DuplicateGroupsResponse> getDuplicateGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        List<DuplicateGroupDTO> allGroups = duplicateService.getDuplicateGroups();
        int totalGroups = allGroups.size();
        int totalPages = (int) Math.ceil((double) totalGroups / size);

        int start = page * size;
        int end = Math.min(start + size, totalGroups);

        List<DuplicateGroupDTO> pageGroups = start < totalGroups
            ? allGroups.subList(start, end)
            : List.of();

        return ResponseEntity.ok(new DuplicateGroupsResponse(
            pageGroups, page, size, totalGroups, totalPages, page < totalPages - 1
        ));
    }

    /**
     * Response for paginated duplicate groups.
     */
    public record DuplicateGroupsResponse(
        List<DuplicateGroupDTO> groups,
        int page,
        int size,
        int totalGroups,
        int totalPages,
        boolean hasMore
    ) {}

    /**
     * Get count of duplicate groups.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getDuplicateCount() {
        int count = duplicateService.getDuplicateGroupCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get a specific duplicate group by ID.
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<DuplicateGroupDTO> getDuplicateGroup(@PathVariable int groupId) {
        return duplicateService.getDuplicateGroup(groupId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Find files similar to a specific file.
     */
    @GetMapping("/similar/{fileId}")
    public ResponseEntity<List<MusicFileDTO>> findSimilarFiles(@PathVariable long fileId) {
        List<MusicFileDTO> similar = duplicateService.findSimilarFiles(fileId);
        return ResponseEntity.ok(similar);
    }

    /**
     * Compare two files and get similarity details.
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareFiles(@RequestBody CompareRequest request) {
        Map<String, Object> result = duplicateService.compareFiles(request.fileId1(), request.fileId2());
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Start an asynchronous duplicate detection scan.
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, String>> startDuplicateScan() {
        String sessionId = duplicateService.startDuplicateScan();
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    /**
     * Get the status of a duplicate scan.
     */
    @GetMapping("/scan/{sessionId}")
    public ResponseEntity<DuplicateScanStatus> getScanStatus(@PathVariable String sessionId) {
        return duplicateService.getScanStatus(sessionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel a running duplicate scan.
     */
    @PostMapping("/scan/{sessionId}/cancel")
    public ResponseEntity<Map<String, Boolean>> cancelScan(@PathVariable String sessionId) {
        boolean cancelled = duplicateService.cancelScan(sessionId);
        if (!cancelled) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("cancelled", true));
    }

    /**
     * Invalidate the duplicate cache and force a refresh.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshDuplicates() {
        duplicateService.invalidateCache();
        return ResponseEntity.ok(Map.of("status", "cache_invalidated"));
    }

    /**
     * Keep one file from a duplicate group and delete the rest.
     */
    @DeleteMapping("/{groupId}/keep/{fileId}")
    public ResponseEntity<Map<String, Object>> keepFileDeleteOthers(
            @PathVariable int groupId,
            @PathVariable long fileId) {
        int deletedCount = duplicateService.keepFileDeleteOthers(groupId, fileId);
        if (deletedCount == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "keptFileId", fileId,
            "deletedCount", deletedCount
        ));
    }

    /**
     * Delete a single file from the collection.
     */
    @DeleteMapping("/file/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable long fileId) {
        boolean deleted = duplicateService.deleteFile(fileId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "deletedFileId", fileId,
            "success", true
        ));
    }

    /**
     * Request body for comparing two files.
     */
    public record CompareRequest(long fileId1, long fileId2) {}
}
