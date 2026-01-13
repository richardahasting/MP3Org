package org.hasting.dto;

import java.util.List;

/**
 * Preview of automatic duplicate resolution showing what will be deleted and kept.
 */
public record AutoResolutionPreviewDTO(
    List<ResolutionItem> resolutions,
    List<DuplicateGroupDTO> holdMyHandGroups,
    int totalFilesToDelete,
    int totalFilesToKeep,
    int totalGroupsNeedingReview
) {
    /**
     * A single resolution item showing one file to delete and the file being kept.
     */
    public record ResolutionItem(
        int groupId,
        MusicFileDTO fileToDelete,
        MusicFileDTO fileToKeep,
        String reason,
        Double similarity  // Fingerprint similarity between the two files (0.0-1.0), null if not available
    ) {}

    public static AutoResolutionPreviewDTO create(
            List<ResolutionItem> resolutions,
            List<DuplicateGroupDTO> holdMyHandGroups) {
        return new AutoResolutionPreviewDTO(
            resolutions,
            holdMyHandGroups,
            resolutions.size(),
            (int) resolutions.stream().map(r -> r.fileToKeep().id()).distinct().count(),
            holdMyHandGroups.size()
        );
    }
}
