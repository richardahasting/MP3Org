package org.hasting.dto;

/**
 * Represents a pair of duplicate files from different directories.
 */
public record DuplicatePairDTO(
    MusicFileDTO fileA,
    MusicFileDTO fileB,
    Double similarity
) {}
