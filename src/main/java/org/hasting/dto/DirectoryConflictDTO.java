package org.hasting.dto;

import java.util.List;

/**
 * Represents a conflict between two directories containing duplicate files.
 * Each directory has files that are duplicates of files in the other directory.
 */
public record DirectoryConflictDTO(
    String directoryA,
    String directoryB,
    int filesInA,
    int filesInB,
    int totalDuplicatePairs,
    List<DuplicatePairDTO> pairs
) {}
