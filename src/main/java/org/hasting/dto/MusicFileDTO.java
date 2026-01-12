package org.hasting.dto;

import org.hasting.model.MusicFile;

import java.util.Date;

/**
 * Data Transfer Object for MusicFile entities.
 * Uses Java 21 record for immutable, concise representation.
 *
 * Part of Issue #69 - Web UI Migration
 */
public record MusicFileDTO(
        Long id,
        String filePath,
        String title,
        String artist,
        String album,
        String genre,
        Integer trackNumber,
        Integer year,
        Integer durationSeconds,
        Long fileSizeBytes,
        Long bitRate,
        Integer sampleRate,
        String fileType,
        Date lastModified,
        Date dateAdded
) {
    /**
     * Creates a DTO from a MusicFile entity.
     */
    public static MusicFileDTO fromEntity(MusicFile entity) {
        if (entity == null) {
            return null;
        }
        return new MusicFileDTO(
                entity.getId(),
                entity.getFilePath(),
                entity.getTitle(),
                entity.getArtist(),
                entity.getAlbum(),
                entity.getGenre(),
                entity.getTrackNumber(),
                entity.getYear(),
                entity.getDurationSeconds(),
                entity.getFileSizeBytes(),
                entity.getBitRate(),
                entity.getSampleRate(),
                entity.getFileType(),
                entity.getLastModified(),
                entity.getDateAdded()
        );
    }

    /**
     * Converts this DTO back to a MusicFile entity.
     */
    public MusicFile toEntity() {
        MusicFile entity = new MusicFile();
        entity.setId(this.id);
        entity.setFilePath(this.filePath);
        entity.setTitle(this.title);
        entity.setArtist(this.artist);
        entity.setAlbum(this.album);
        entity.setGenre(this.genre);
        entity.setTrackNumber(this.trackNumber);
        entity.setYear(this.year);
        entity.setDurationSeconds(this.durationSeconds);
        entity.setFileSizeBytes(this.fileSizeBytes);
        entity.setBitRate(this.bitRate);
        entity.setSampleRate(this.sampleRate);
        entity.setFileType(this.fileType);
        return entity;
    }

    /**
     * Returns a human-readable duration string (e.g., "3:45").
     */
    public String getFormattedDuration() {
        if (durationSeconds == null || durationSeconds <= 0) {
            return "0:00";
        }
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Returns a human-readable file size (e.g., "5.2 MB").
     */
    public String getFormattedFileSize() {
        if (fileSizeBytes == null || fileSizeBytes <= 0) {
            return "0 B";
        }
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else if (fileSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", fileSizeBytes / (1024.0 * 1024 * 1024));
        }
    }
}
