package org.hasting.dto;

import java.util.List;

/**
 * Preview of what will happen when resolving a directory conflict.
 */
public record DirectoryResolutionPreviewDTO(
    String directoryToKeep,
    String directoryToDelete,
    List<MusicFileDTO> filesToDelete,
    List<MusicFileDTO> filesToKeep,
    int totalFilesToDelete
) {}
