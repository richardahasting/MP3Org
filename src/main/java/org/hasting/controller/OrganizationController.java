package org.hasting.controller;

import org.hasting.dto.OrganizationPreviewDTO;
import org.hasting.service.OrganizationService;
import org.hasting.service.OrganizationService.OrganizationResultDTO;
import org.hasting.service.OrganizationService.TextFormatDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for file organization operations.
 * Provides endpoints for previewing and executing file organization.
 *
 * Part of Issue #69 - Web UI Migration
 */
@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * POST /api/v1/organization/preview - Preview organization paths for specific files.
     *
     * @param request Preview request with file IDs and template settings
     * @return List of preview results
     */
    @PostMapping("/preview")
    public List<OrganizationPreviewDTO> previewOrganization(@RequestBody PreviewRequest request) {
        return organizationService.previewOrganization(
                request.fileIds(),
                request.basePath(),
                request.template(),
                request.textFormat(),
                request.useSubdirectories(),
                request.subdirectoryLevels()
        );
    }

    /**
     * POST /api/v1/organization/preview-all - Preview organization paths for all files.
     *
     * @param request Preview request with template settings
     * @return Paginated list of preview results
     */
    @PostMapping("/preview-all")
    public PreviewAllResponse previewAllOrganization(@RequestBody PreviewAllRequest request) {
        List<OrganizationPreviewDTO> previews = organizationService.previewAllOrganization(
                request.basePath(),
                request.template(),
                request.textFormat(),
                request.useSubdirectories(),
                request.subdirectoryLevels(),
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 50
        );

        long totalCount = organizationService.getTotalFileCount();

        return new PreviewAllResponse(
                previews,
                totalCount,
                request.page() != null ? request.page() : 0,
                (int) Math.ceil((double) totalCount / (request.size() != null ? request.size() : 50))
        );
    }

    /**
     * POST /api/v1/organization/execute - Execute organization (copy files).
     *
     * @param request Execute request with file IDs and template settings
     * @return Summary of the operation
     */
    @PostMapping("/execute")
    public OrganizationResultDTO executeOrganization(@RequestBody ExecuteRequest request) {
        return organizationService.executeOrganization(
                request.fileIds(),
                request.basePath(),
                request.template(),
                request.textFormat(),
                request.useSubdirectories(),
                request.subdirectoryLevels()
        );
    }

    /**
     * GET /api/v1/organization/templates - Get example templates.
     *
     * @return List of example template strings
     */
    @GetMapping("/templates")
    public Map<String, Object> getTemplates() {
        return Map.of(
                "examples", organizationService.getExampleTemplates(),
                "default", "{subdirectory}/{artist}/{album}/{track_number:02d}-{title}.{file_type}"
        );
    }

    /**
     * GET /api/v1/organization/fields - Get available field placeholders.
     *
     * @return List of available fields
     */
    @GetMapping("/fields")
    public String[] getAvailableFields() {
        return organizationService.getAvailableFields();
    }

    /**
     * GET /api/v1/organization/formats - Get available text format options.
     *
     * @return List of text format options with descriptions
     */
    @GetMapping("/formats")
    public TextFormatDTO[] getTextFormats() {
        return organizationService.getTextFormats();
    }

    /**
     * Request body for preview operations.
     */
    public record PreviewRequest(
            List<Long> fileIds,
            String basePath,
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels
    ) {}

    /**
     * Request body for preview-all operations.
     */
    public record PreviewAllRequest(
            String basePath,
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels,
            Integer page,
            Integer size
    ) {}

    /**
     * Response for preview-all operations.
     */
    public record PreviewAllResponse(
            List<OrganizationPreviewDTO> previews,
            long totalCount,
            int page,
            int totalPages
    ) {}

    /**
     * Request body for execute operations.
     */
    public record ExecuteRequest(
            List<Long> fileIds,
            String basePath,
            String template,
            String textFormat,
            Boolean useSubdirectories,
            Integer subdirectoryLevels
    ) {}
}
