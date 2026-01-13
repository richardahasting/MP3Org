package org.hasting.dto;

import java.util.List;

/**
 * DTO for automatic duplicate resolution results.
 * Contains summary of actions taken and groups requiring manual review.
 */
public record AutoResolutionResultDTO(
    int groupsProcessed,
    int filesDeleted,
    int filesKept,
    List<DuplicateGroupDTO> holdMyHandGroups,  // Groups that couldn't be auto-resolved
    String summary
) {
    /**
     * Creates a result with computed summary message.
     */
    public static AutoResolutionResultDTO create(
            int groupsProcessed,
            int filesDeleted,
            int filesKept,
            List<DuplicateGroupDTO> holdMyHandGroups) {

        String summary;
        if (filesDeleted == 0 && holdMyHandGroups.isEmpty()) {
            summary = "No duplicates found to process.";
        } else if (holdMyHandGroups.isEmpty()) {
            summary = String.format("Auto-resolved %d groups: deleted %d files, kept %d files.",
                groupsProcessed, filesDeleted, filesKept);
        } else {
            summary = String.format("Auto-resolved %d groups: deleted %d files, kept %d files. " +
                "%d groups require manual review.",
                groupsProcessed - holdMyHandGroups.size(), filesDeleted, filesKept, holdMyHandGroups.size());
        }

        return new AutoResolutionResultDTO(groupsProcessed, filesDeleted, filesKept, holdMyHandGroups, summary);
    }
}
