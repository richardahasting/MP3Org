package org.hasting.service;

import org.hasting.dto.MusicFileDTO;
import org.hasting.dto.PageResponse;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for music file operations.
 * Wraps the existing DatabaseManager to provide a clean API for controllers.
 *
 * Part of Issue #69 - Web UI Migration
 */
@Service
public class MusicFileService {

    /**
     * Retrieves all music files with pagination.
     *
     * @param page Page number (0-indexed)
     * @param size Number of items per page
     * @return Paginated response with music files
     */
    public PageResponse<MusicFileDTO> getAllMusicFiles(int page, int size) {
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        List<MusicFileDTO> dtos = allFiles.stream()
                .map(MusicFileDTO::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(dtos, page, size);
    }

    /**
     * Retrieves a music file by ID.
     *
     * @param id The database ID
     * @return Optional containing the music file if found
     */
    public Optional<MusicFileDTO> getMusicFileById(Long id) {
        MusicFile entity = DatabaseManager.getMusicFileById(id);
        return Optional.ofNullable(entity).map(MusicFileDTO::fromEntity);
    }

    /**
     * Updates a music file's metadata.
     *
     * @param id  The database ID
     * @param dto The updated music file data
     * @return The updated music file
     */
    public Optional<MusicFileDTO> updateMusicFile(Long id, MusicFileDTO dto) {
        MusicFile existing = DatabaseManager.getMusicFileById(id);
        if (existing == null) {
            return Optional.empty();
        }

        // Update fields from DTO
        existing.setTitle(dto.title());
        existing.setArtist(dto.artist());
        existing.setAlbum(dto.album());
        existing.setGenre(dto.genre());
        existing.setTrackNumber(dto.trackNumber());
        existing.setYear(dto.year());

        DatabaseManager.updateMusicFile(existing);
        return Optional.of(MusicFileDTO.fromEntity(existing));
    }

    /**
     * Deletes a music file from the database.
     *
     * @param id The database ID
     * @return true if deleted, false if not found
     */
    public boolean deleteMusicFile(Long id) {
        MusicFile existing = DatabaseManager.getMusicFileById(id);
        if (existing == null) {
            return false;
        }
        DatabaseManager.deleteMusicFile(existing);
        return true;
    }

    /**
     * Searches music files by term across all fields.
     *
     * @param term Search term
     * @param page Page number
     * @param size Page size
     * @return Paginated search results
     */
    public PageResponse<MusicFileDTO> searchMusicFiles(String term, int page, int size) {
        List<MusicFile> results = DatabaseManager.searchMusicFiles(term);
        List<MusicFileDTO> dtos = results.stream()
                .map(MusicFileDTO::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(dtos, page, size);
    }

    /**
     * Searches music files by title.
     *
     * @param title Search term
     * @param page  Page number
     * @param size  Page size
     * @return Paginated search results
     */
    public PageResponse<MusicFileDTO> searchByTitle(String title, int page, int size) {
        List<MusicFile> results = DatabaseManager.searchMusicFilesByTitle(title);
        List<MusicFileDTO> dtos = results.stream()
                .map(MusicFileDTO::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(dtos, page, size);
    }

    /**
     * Searches music files by artist.
     *
     * @param artist Search term
     * @param page   Page number
     * @param size   Page size
     * @return Paginated search results
     */
    public PageResponse<MusicFileDTO> searchByArtist(String artist, int page, int size) {
        List<MusicFile> results = DatabaseManager.searchMusicFilesByArtist(artist);
        List<MusicFileDTO> dtos = results.stream()
                .map(MusicFileDTO::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(dtos, page, size);
    }

    /**
     * Searches music files by album.
     *
     * @param album Search term
     * @param page  Page number
     * @param size  Page size
     * @return Paginated search results
     */
    public PageResponse<MusicFileDTO> searchByAlbum(String album, int page, int size) {
        List<MusicFile> results = DatabaseManager.searchMusicFilesByAlbum(album);
        List<MusicFileDTO> dtos = results.stream()
                .map(MusicFileDTO::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(dtos, page, size);
    }

    /**
     * Gets the total count of music files in the database.
     *
     * @return Total number of music files
     */
    public long getMusicFileCount() {
        return DatabaseManager.getMusicFileCount();
    }

    /**
     * Bulk updates multiple music files.
     *
     * @param ids    List of IDs to update
     * @param artist New artist (null to keep existing)
     * @param album  New album (null to keep existing)
     * @param genre  New genre (null to keep existing)
     * @param year   New year (null to keep existing)
     * @return Number of files updated
     */
    public int bulkUpdate(List<Long> ids, String artist, String album, String genre, Integer year) {
        int updated = 0;
        for (Long id : ids) {
            MusicFile existing = DatabaseManager.getMusicFileById(id);
            if (existing != null) {
                boolean changed = false;
                if (artist != null) {
                    existing.setArtist(artist);
                    changed = true;
                }
                if (album != null) {
                    existing.setAlbum(album);
                    changed = true;
                }
                if (genre != null) {
                    existing.setGenre(genre);
                    changed = true;
                }
                if (year != null) {
                    existing.setYear(year);
                    changed = true;
                }
                if (changed) {
                    DatabaseManager.updateMusicFile(existing);
                    updated++;
                }
            }
        }
        return updated;
    }

    /**
     * Gets the audio file for streaming.
     *
     * @param id The database ID
     * @return Optional containing the File if found and exists
     */
    public Optional<File> getAudioFile(Long id) {
        MusicFile entity = DatabaseManager.getMusicFileById(id);
        if (entity == null) {
            return Optional.empty();
        }
        File file = new File(entity.getFilePath());
        if (!file.exists() || !file.canRead()) {
            return Optional.empty();
        }
        return Optional.of(file);
    }

    /**
     * Gets the file type (extension) for a music file.
     *
     * @param id The database ID
     * @return Optional containing the file type if found
     */
    public Optional<String> getFileType(Long id) {
        MusicFile entity = DatabaseManager.getMusicFileById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entity.getFileType());
    }

    // ============= Bulk Download Support =============

    /**
     * Gets list of all artists with their file counts.
     *
     * @return List of maps containing artist name and file count
     */
    public List<Map<String, Object>> getArtistsWithCounts() {
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        Map<String, Long> artistCounts = new LinkedHashMap<>();

        for (MusicFile file : allFiles) {
            String artist = file.getArtist();
            if (artist == null || artist.isBlank()) {
                artist = "Unknown Artist";
            }
            artistCounts.merge(artist, 1L, Long::sum);
        }

        // Sort by artist name
        return artistCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(e -> Map.<String, Object>of("artist", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Gets list of all albums with their file counts, optionally filtered by artist.
     *
     * @param artist Optional artist to filter by
     * @return List of maps containing album name, artist, and file count
     */
    public List<Map<String, Object>> getAlbumsWithCounts(String artist) {
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        Map<String, Map<String, Object>> albumData = new LinkedHashMap<>();

        for (MusicFile file : allFiles) {
            String fileArtist = file.getArtist();
            if (fileArtist == null || fileArtist.isBlank()) {
                fileArtist = "Unknown Artist";
            }

            // Filter by artist if specified
            if (artist != null && !artist.isBlank() && !fileArtist.equalsIgnoreCase(artist)) {
                continue;
            }

            String album = file.getAlbum();
            if (album == null || album.isBlank()) {
                album = "Unknown Album";
            }

            final String finalArtist = fileArtist;
            final String finalAlbum = album;
            String key = finalArtist + "|||" + finalAlbum;
            albumData.compute(key, (k, v) -> {
                if (v == null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("artist", finalArtist);
                    m.put("album", finalAlbum);
                    m.put("count", 1L);
                    return m;
                }
                v.put("count", ((Long) v.get("count")) + 1);
                return v;
            });
        }

        // Sort by artist, then album
        return albumData.values().stream()
                .sorted((a, b) -> {
                    int artistCmp = ((String) a.get("artist")).compareToIgnoreCase((String) b.get("artist"));
                    if (artistCmp != 0) return artistCmp;
                    return ((String) a.get("album")).compareToIgnoreCase((String) b.get("album"));
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets all files by a specific artist.
     *
     * @param artist Artist name
     * @return List of File objects
     */
    public List<File> getFilesByArtist(String artist) {
        List<MusicFile> results = DatabaseManager.searchMusicFilesByArtist(artist);
        List<File> files = new ArrayList<>();

        for (MusicFile mf : results) {
            // Only include exact artist matches (case-insensitive)
            if (mf.getArtist() != null && mf.getArtist().equalsIgnoreCase(artist)) {
                File file = new File(mf.getFilePath());
                if (file.exists() && file.canRead()) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    /**
     * Gets all files from a specific album by a specific artist.
     *
     * @param artist Artist name
     * @param album  Album name
     * @return List of File objects
     */
    public List<File> getFilesByAlbum(String artist, String album) {
        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
        List<File> files = new ArrayList<>();

        for (MusicFile mf : allFiles) {
            boolean artistMatch = (mf.getArtist() != null && mf.getArtist().equalsIgnoreCase(artist)) ||
                                  (artist.equals("Unknown Artist") && (mf.getArtist() == null || mf.getArtist().isBlank()));
            boolean albumMatch = (mf.getAlbum() != null && mf.getAlbum().equalsIgnoreCase(album)) ||
                                 (album.equals("Unknown Album") && (mf.getAlbum() == null || mf.getAlbum().isBlank()));

            if (artistMatch && albumMatch) {
                File file = new File(mf.getFilePath());
                if (file.exists() && file.canRead()) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    /**
     * Gets files by their IDs.
     *
     * @param ids List of file IDs
     * @return List of File objects
     */
    public List<File> getFilesByIds(List<Long> ids) {
        List<File> files = new ArrayList<>();

        for (Long id : ids) {
            MusicFile mf = DatabaseManager.getMusicFileById(id);
            if (mf != null) {
                File file = new File(mf.getFilePath());
                if (file.exists() && file.canRead()) {
                    files.add(file);
                }
            }
        }

        return files;
    }
}
