package org.hasting.dto;

/**
 * Request body for directory-based duplicate resolution.
 */
public record DirectoryResolutionRequest(
    String directoryToKeep,
    String directoryToDelete
) {}
