package org.hasting.dto;

/**
 * DTO for organization preview results.
 * Shows current and proposed file paths for organization operations.
 *
 * Part of Issue #69 - Web UI Migration
 */
public record OrganizationPreviewDTO(
        Long id,
        String currentPath,
        String proposedPath,
        String title,
        String artist,
        String album,
        boolean valid,
        String error
) {}
