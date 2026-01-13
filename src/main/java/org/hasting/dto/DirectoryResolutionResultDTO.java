package org.hasting.dto;

/**
 * Result of executing a directory-based duplicate resolution.
 */
public record DirectoryResolutionResultDTO(
    int filesDeleted,
    int filesAttempted,
    String directoryKept,
    String directoryCleared
) {}
