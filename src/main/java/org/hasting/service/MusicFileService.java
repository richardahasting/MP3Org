package org.hasting.service;

import org.hasting.dto.MusicFileDTO;
import org.hasting.dto.PageResponse;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.springframework.stereotype.Service;

import java.util.List;
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
     * @return Number of files updated
     */
    public int bulkUpdate(List<Long> ids, String artist, String album, String genre) {
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
                if (changed) {
                    DatabaseManager.updateMusicFile(existing);
                    updated++;
                }
            }
        }
        return updated;
    }
}
