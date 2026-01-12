package org.hasting.dto;

import java.util.List;

/**
 * DTO representing a group of duplicate music files.
 * Each group contains files that are considered duplicates of each other.
 */
public record DuplicateGroupDTO(
    int groupId,
    int fileCount,
    List<MusicFileDTO> files,
    String representativeTitle,
    String representativeArtist
) {
    /**
     * Creates a DuplicateGroupDTO from a list of music file DTOs.
     */
    public static DuplicateGroupDTO fromFiles(int groupId, List<MusicFileDTO> files) {
        if (files == null || files.isEmpty()) {
            return new DuplicateGroupDTO(groupId, 0, List.of(), "", "");
        }

        MusicFileDTO representative = files.get(0);
        return new DuplicateGroupDTO(
            groupId,
            files.size(),
            files,
            representative.title() != null ? representative.title() : "",
            representative.artist() != null ? representative.artist() : ""
        );
    }
}
