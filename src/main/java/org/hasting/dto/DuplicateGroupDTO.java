package org.hasting.dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO representing a group of duplicate music files.
 * Each group contains files that are considered duplicates of each other.
 */
public record DuplicateGroupDTO(
    int groupId,
    int fileCount,
    List<DuplicateFileDTO> files,
    String representativeTitle,
    String representativeArtist
) {
    /**
     * Creates a DuplicateGroupDTO from a list of music file DTOs (without similarity scores).
     */
    public static DuplicateGroupDTO fromFiles(int groupId, List<MusicFileDTO> files) {
        if (files == null || files.isEmpty()) {
            return new DuplicateGroupDTO(groupId, 0, List.of(), "", "");
        }

        MusicFileDTO representative = files.get(0);
        List<DuplicateFileDTO> duplicateFiles = files.stream()
            .map(DuplicateFileDTO::fromFile)
            .collect(Collectors.toList());

        return new DuplicateGroupDTO(
            groupId,
            files.size(),
            duplicateFiles,
            representative.title() != null ? representative.title() : "",
            representative.artist() != null ? representative.artist() : ""
        );
    }

    /**
     * Creates a DuplicateGroupDTO with similarity scores.
     * The first file is the reference (similarity 1.0), others have their similarity to the reference.
     */
    public static DuplicateGroupDTO fromFilesWithSimilarity(int groupId, List<MusicFileDTO> files, List<Double> similarities) {
        if (files == null || files.isEmpty()) {
            return new DuplicateGroupDTO(groupId, 0, List.of(), "", "");
        }

        MusicFileDTO representative = files.get(0);
        List<DuplicateFileDTO> duplicateFiles = new java.util.ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            double similarity = (similarities != null && i < similarities.size()) ? similarities.get(i) : 1.0;
            duplicateFiles.add(DuplicateFileDTO.withSimilarity(files.get(i), similarity));
        }

        return new DuplicateGroupDTO(
            groupId,
            files.size(),
            duplicateFiles,
            representative.title() != null ? representative.title() : "",
            representative.artist() != null ? representative.artist() : ""
        );
    }
}
