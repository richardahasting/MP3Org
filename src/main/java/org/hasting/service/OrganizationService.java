package org.hasting.service;

import org.hasting.dto.MusicFileDTO;
import org.hasting.dto.OrganizationPreviewDTO;
import org.hasting.model.MusicFile;
import org.hasting.model.PathTemplate;
import org.hasting.util.DatabaseManager;
import org.hasting.util.FileOrganizer;
import org.springframework.stereotype.Service;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for file organization operations.
 * Wraps the existing FileOrganizer to provide path generation and file copying.
 *
 * Part of Issue #69 - Web UI Migration
 */
@Service
public class OrganizationService {

    private static final Logger logger = Log4Rich.getLogger(OrganizationService.class);

    /**
     * Previews organization paths for a list of file IDs without copying files.
     *
     * @param fileIds List of music file IDs to preview
     * @param basePath Base path for organization
     * @param template Path template (null for default)
     * @param textFormat Text formatting style (null for default UNDERSCORE)
     * @param useSubdirectories Whether to use alphabetical subdirectory grouping
     * @param subdirectoryLevels Number of subdirectory groups (1-12)
     * @return List of preview results showing current and proposed paths
     */
    public List<OrganizationPreviewDTO> previewOrganization(
            List<Long> fileIds,
            String basePath,
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels
    ) {
        List<OrganizationPreviewDTO> previews = new ArrayList<>();
        PathTemplate pathTemplate = createPathTemplate(template, textFormat, useSubdirectories, subdirectoryLevels);

        for (Long id : fileIds) {
            MusicFile musicFile = DatabaseManager.getMusicFileById(id);
            if (musicFile != null) {
                String newPath = FileOrganizer.generateFilePath(musicFile, basePath, pathTemplate);
                previews.add(new OrganizationPreviewDTO(
                        id,
                        musicFile.getFilePath(),
                        newPath,
                        musicFile.getTitle(),
                        musicFile.getArtist(),
                        musicFile.getAlbum(),
                        true, // valid
                        null  // no error
                ));
            } else {
                previews.add(new OrganizationPreviewDTO(
                        id,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        "File not found in database"
                ));
            }
        }

        return previews;
    }

    /**
     * Previews organization paths for all files in the database.
     *
     * @param basePath Base path for organization
     * @param template Path template (null for default)
     * @param textFormat Text formatting style
     * @param useSubdirectories Whether to use alphabetical subdirectory grouping
     * @param subdirectoryLevels Number of subdirectory groups
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of preview results for the requested page
     */
    public List<OrganizationPreviewDTO> previewAllOrganization(
            String basePath,
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels,
            int page,
            int size
    ) {
        return previewAllOrganization(basePath, template, textFormat, useSubdirectories,
                subdirectoryLevels, page, size, null, null, null, null);
    }

    /**
     * Previews organization paths for files matching optional filters.
     *
     * @param basePath Base path for organization
     * @param template Path template (null for default)
     * @param textFormat Text formatting style
     * @param useSubdirectories Whether to use alphabetical subdirectory grouping
     * @param subdirectoryLevels Number of subdirectory groups
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param filterTitle Optional title filter
     * @param filterArtist Optional artist filter
     * @param filterAlbum Optional album filter
     * @param filterGenre Optional genre filter
     * @return List of preview results for the requested page
     */
    public List<OrganizationPreviewDTO> previewAllOrganization(
            String basePath,
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels,
            int page,
            int size,
            String filterTitle,
            String filterArtist,
            String filterAlbum,
            String filterGenre
    ) {
        List<MusicFile> files = DatabaseManager.searchMusicFilesWithFilters(
                filterTitle, filterArtist, filterAlbum, filterGenre, page, size);
        PathTemplate pathTemplate = createPathTemplate(template, textFormat, useSubdirectories, subdirectoryLevels);
        List<OrganizationPreviewDTO> previews = new ArrayList<>();

        for (MusicFile musicFile : files) {
            String newPath = FileOrganizer.generateFilePath(musicFile, basePath, pathTemplate);
            previews.add(new OrganizationPreviewDTO(
                    musicFile.getId(),
                    musicFile.getFilePath(),
                    newPath,
                    musicFile.getTitle(),
                    musicFile.getArtist(),
                    musicFile.getAlbum(),
                    true,
                    null
            ));
        }

        return previews;
    }

    /**
     * Gets the total count of files for pagination.
     *
     * @return Total number of music files
     */
    public long getTotalFileCount() {
        return DatabaseManager.getMusicFileCount();
    }

    /**
     * Gets the count of files matching optional filters.
     *
     * @param filterTitle Optional title filter
     * @param filterArtist Optional artist filter
     * @param filterAlbum Optional album filter
     * @param filterGenre Optional genre filter
     * @return Count of matching files
     */
    public int getFilteredFileCount(String filterTitle, String filterArtist, String filterAlbum, String filterGenre) {
        return DatabaseManager.countMusicFilesWithFilters(filterTitle, filterArtist, filterAlbum, filterGenre);
    }

    /**
     * Gets all file IDs matching optional filters.
     * Used for "Select All" functionality.
     *
     * @param filterTitle Optional title filter
     * @param filterArtist Optional artist filter
     * @param filterAlbum Optional album filter
     * @param filterGenre Optional genre filter
     * @return List of matching file IDs
     */
    public List<Long> getMatchingFileIds(String filterTitle, String filterArtist, String filterAlbum, String filterGenre) {
        return DatabaseManager.getMatchingFileIds(filterTitle, filterArtist, filterAlbum, filterGenre);
    }

    /**
     * Executes organization (copies files) for a list of file IDs.
     *
     * @param fileIds List of music file IDs to organize
     * @param basePath Base path for organization
     * @param template Path template (null for default)
     * @param textFormat Text formatting style
     * @param useSubdirectories Whether to use subdirectory grouping
     * @param subdirectoryLevels Number of subdirectory groups
     * @return Summary of the operation
     */
    public OrganizationResultDTO executeOrganization(
            List<Long> fileIds,
            String basePath,
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels
    ) {
        PathTemplate pathTemplate = createPathTemplate(template, textFormat, useSubdirectories, subdirectoryLevels);
        int successful = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long id : fileIds) {
            MusicFile musicFile = DatabaseManager.getMusicFileById(id);
            if (musicFile != null) {
                try {
                    FileOrganizer.copyToNewLocation(musicFile, basePath, pathTemplate);
                    successful++;
                } catch (IOException e) {
                    failed++;
                    errors.add(String.format("File %d (%s): %s", id, musicFile.getTitle(), e.getMessage()));
                    logger.warn("Failed to organize file {}: {}", id, e.getMessage());
                }
            } else {
                failed++;
                errors.add(String.format("File %d: Not found in database", id));
            }
        }

        return new OrganizationResultDTO(successful, failed, errors);
    }

    /**
     * Gets available template examples.
     *
     * @return List of example templates
     */
    public String[] getExampleTemplates() {
        return PathTemplate.getExampleTemplates();
    }

    /**
     * Gets available field placeholders.
     *
     * @return List of available fields
     */
    public String[] getAvailableFields() {
        return PathTemplate.getAvailableFields();
    }

    /**
     * Gets available text format options.
     *
     * @return Array of text format options with descriptions
     */
    public TextFormatDTO[] getTextFormats() {
        PathTemplate.TextFormat[] formats = PathTemplate.TextFormat.values();
        TextFormatDTO[] result = new TextFormatDTO[formats.length];
        for (int i = 0; i < formats.length; i++) {
            result[i] = new TextFormatDTO(formats[i].name(), formats[i].getDescription());
        }
        return result;
    }

    /**
     * Creates a PathTemplate from the provided parameters.
     */
    private PathTemplate createPathTemplate(
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels
    ) {
        PathTemplate pathTemplate = new PathTemplate();

        if (template != null && !template.isBlank()) {
            pathTemplate.setTemplate(template);
        }

        if (textFormat != null && !textFormat.isBlank()) {
            try {
                pathTemplate.setTextFormat(PathTemplate.TextFormat.valueOf(textFormat.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Keep default if invalid format
                logger.warn("Invalid text format '{}', using default", textFormat);
            }
        }

        if (useSubdirectories != null) {
            pathTemplate.setUseSubdirectoryGrouping(useSubdirectories);
        }

        if (subdirectoryLevels != null) {
            pathTemplate.setSubdirectoryLevels(subdirectoryLevels);
        }

        return pathTemplate;
    }

    /**
     * DTO for organization execution results.
     */
    public record OrganizationResultDTO(
            int successCount,
            int failureCount,
            List<String> errors
    ) {}

    /**
     * DTO for text format options.
     */
    public record TextFormatDTO(
            String name,
            String description
    ) {}
}
