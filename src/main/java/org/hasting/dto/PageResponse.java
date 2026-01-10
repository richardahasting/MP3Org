package org.hasting.dto;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Used for returning paginated lists from REST endpoints.
 *
 * Part of Issue #69 - Web UI Migration
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * Creates a PageResponse from a list with pagination parameters.
     */
    public static <T> PageResponse<T> of(List<T> allItems, int page, int size) {
        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        List<T> content;
        if (startIndex >= totalElements) {
            content = List.of();
        } else {
            content = allItems.subList(startIndex, endIndex);
        }

        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
