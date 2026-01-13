package org.hasting.controller;

import org.hasting.dto.MusicFileDTO;
import org.hasting.dto.PageResponse;
import org.hasting.service.MusicFileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * REST controller for music file operations.
 * Provides endpoints for CRUD operations and search functionality.
 *
 * Part of Issue #69 - Web UI Migration
 */
@RestController
@RequestMapping("/api/v1/music")
public class MusicFileController {

    private final MusicFileService musicFileService;

    public MusicFileController(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }

    /**
     * GET /api/v1/music - List all music files with pagination.
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     * @return Paginated list of music files
     */
    @GetMapping
    public PageResponse<MusicFileDTO> getAllMusicFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return musicFileService.getAllMusicFiles(page, size);
    }

    /**
     * GET /api/v1/music/{id} - Get a specific music file by ID.
     *
     * @param id The database ID
     * @return The music file or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<MusicFileDTO> getMusicFileById(@PathVariable Long id) {
        return musicFileService.getMusicFileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/v1/music/{id} - Update a music file's metadata.
     *
     * @param id  The database ID
     * @param dto The updated metadata
     * @return The updated music file or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<MusicFileDTO> updateMusicFile(
            @PathVariable Long id,
            @RequestBody MusicFileDTO dto) {
        return musicFileService.updateMusicFile(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v1/music/{id} - Delete a music file.
     *
     * @param id The database ID
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMusicFile(@PathVariable Long id) {
        if (musicFileService.deleteMusicFile(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/v1/music/search - Search music files.
     *
     * @param q      General search term (searches all fields)
     * @param title  Search by title
     * @param artist Search by artist
     * @param album  Search by album
     * @param page   Page number
     * @param size   Page size
     * @return Paginated search results
     */
    @GetMapping("/search")
    public PageResponse<MusicFileDTO> searchMusicFiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String album,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (title != null && !title.isBlank()) {
            return musicFileService.searchByTitle(title, page, size);
        } else if (artist != null && !artist.isBlank()) {
            return musicFileService.searchByArtist(artist, page, size);
        } else if (album != null && !album.isBlank()) {
            return musicFileService.searchByAlbum(album, page, size);
        } else if (q != null && !q.isBlank()) {
            return musicFileService.searchMusicFiles(q, page, size);
        }

        // If no search criteria, return all files
        return musicFileService.getAllMusicFiles(page, size);
    }

    /**
     * GET /api/v1/music/count - Get total number of music files.
     *
     * @return Map containing the count
     */
    @GetMapping("/count")
    public Map<String, Long> getMusicFileCount() {
        return Map.of("count", musicFileService.getMusicFileCount());
    }

    /**
     * PUT /api/v1/music/bulk - Bulk update multiple music files.
     *
     * @param request Bulk update request with IDs and new values
     * @return Number of files updated
     */
    @PutMapping("/bulk")
    public Map<String, Integer> bulkUpdate(@RequestBody BulkUpdateRequest request) {
        int updated = musicFileService.bulkUpdate(
                request.ids(),
                request.artist(),
                request.album(),
                request.genre()
        );
        return Map.of("updated", updated);
    }

    /**
     * GET /api/v1/music/{id}/stream - Stream audio file for playback.
     *
     * @param id The database ID
     * @return Audio file stream with appropriate content type
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamAudioFile(@PathVariable Long id) {
        return musicFileService.getAudioFile(id)
                .map(file -> {
                    Resource resource = new FileSystemResource(file);
                    String contentType = getContentType(file.getName());

                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_TYPE, contentType);
                    headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                    headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Determines the content type based on file extension.
     */
    private String getContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lower.endsWith(".flac")) {
            return "audio/flac";
        } else if (lower.endsWith(".wav")) {
            return "audio/wav";
        } else if (lower.endsWith(".m4a") || lower.endsWith(".aac")) {
            return "audio/mp4";
        } else if (lower.endsWith(".ogg")) {
            return "audio/ogg";
        } else if (lower.endsWith(".wma")) {
            return "audio/x-ms-wma";
        }
        return "application/octet-stream";
    }

    /**
     * Request body for bulk update operations.
     */
    public record BulkUpdateRequest(
            List<Long> ids,
            String artist,
            String album,
            String genre
    ) {
    }
}
