package org.hasting.dto;

/**
 * DTO representing a music file within a duplicate group context.
 * Wraps MusicFileDTO with fingerprint similarity information.
 */
public record DuplicateFileDTO(
    MusicFileDTO file,
    Double similarity  // Fingerprint similarity to the reference file (0.0-1.0), null if not available
) {
    /**
     * Creates a DuplicateFileDTO from a MusicFileDTO without similarity.
     */
    public static DuplicateFileDTO fromFile(MusicFileDTO file) {
        return new DuplicateFileDTO(file, null);
    }

    /**
     * Creates a DuplicateFileDTO with similarity score.
     */
    public static DuplicateFileDTO withSimilarity(MusicFileDTO file, double similarity) {
        return new DuplicateFileDTO(file, similarity);
    }
}
