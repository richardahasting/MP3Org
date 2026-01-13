package org.hasting.controller;

import org.hasting.dto.ScanProgressDTO;
import org.hasting.service.ScanningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Example test class for ScanningController REST API endpoints.
 *
 * ============================================================================
 * TEST PATTERNS FOR COLLABORATORS (Issue #70)
 * ============================================================================
 *
 * This file demonstrates the testing patterns used in MP3Org. Follow these
 * conventions when adding new tests:
 *
 * 1. CLASS STRUCTURE
 *    - Use @WebMvcTest to isolate controller testing (no real DB/services)
 *    - @MockBean for service dependencies
 *    - @Nested classes to group tests by endpoint
 *    - @DisplayName for readable test descriptions
 *
 * 2. TEST NAMING CONVENTION
 *    methodUnderTest_Scenario_ExpectedBehavior()
 *    Examples:
 *      - startScan_ValidDirectories_ReturnsSessionId()
 *      - getScanStatus_InvalidSessionId_Returns404()
 *
 * 3. TEST STRUCTURE (Arrange-Act-Assert)
 *    - Arrange: Set up mocks with when(...).thenReturn(...)
 *    - Act: Perform HTTP request with mockMvc.perform(...)
 *    - Assert: Verify response with .andExpect(...)
 *    - Verify: Confirm service interactions with verify(...)
 *
 * 4. WHAT TO TEST
 *    - Success cases (happy path)
 *    - Error cases (404, 400, validation errors)
 *    - Edge cases (empty results, null values)
 *    - HTTP status codes
 *    - Response body structure (JSON paths)
 *    - Service method invocations
 *
 * ============================================================================
 */
@WebMvcTest(ScanningController.class)
class ScanningControllerTest {

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    /**
     * MockMvc is Spring's test utility for simulating HTTP requests.
     * It doesn't start a real server - tests run in-memory for speed.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * @MockBean creates a Mockito mock and registers it in the Spring context.
     * The controller will receive this mock instead of the real service.
     * This isolates the controller logic from database/business logic.
     */
    @MockBean
    private ScanningService scanningService;

    // ========================================================================
    // POST /api/v1/scanning/start - Start Directory Scan
    // ========================================================================

    /**
     * Group related tests using @Nested. This creates a clear hierarchy
     * in test reports and makes it easy to find tests for specific endpoints.
     */
    @Nested
    @DisplayName("POST /api/v1/scanning/start - Start Directory Scan")
    class StartScanTests {

        /**
         * EXAMPLE TEST: Success Case
         *
         * This test demonstrates the complete pattern for testing a successful
         * POST request that returns a session ID.
         *
         * Key elements:
         * 1. Mock the service to return expected data
         * 2. Perform POST with JSON body
         * 3. Assert HTTP 200 and response structure
         * 4. Verify the service was called correctly
         */
        @Test
        @DisplayName("Should start scan and return session ID when valid directories provided")
        void startScan_ValidDirectories_ReturnsSessionId() throws Exception {
            // ----------------------------------------------------------------
            // ARRANGE: Set up the mock service behavior
            // ----------------------------------------------------------------
            // When scanningService.startScan() is called with any list,
            // return a fake session ID. This isolates us from real scanning.
            String expectedSessionId = "abc-123-def-456";
            when(scanningService.startScan(anyList())).thenReturn(expectedSessionId);

            // The JSON body we'll send in the request
            String requestBody = """
                {
                    "directories": ["/Users/richard/Music", "/Users/richard/Downloads"]
                }
                """;

            // ----------------------------------------------------------------
            // ACT: Perform the HTTP request
            // ----------------------------------------------------------------
            mockMvc.perform(post("/api/v1/scanning/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))

                    // ------------------------------------------------------------
                    // ASSERT: Verify the response
                    // ------------------------------------------------------------
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // Use jsonPath to verify specific fields in the JSON response
                    .andExpect(jsonPath("$.sessionId", is(expectedSessionId)))
                    .andExpect(jsonPath("$.sessionId", not(emptyString())));

            // ----------------------------------------------------------------
            // VERIFY: Confirm service interactions
            // ----------------------------------------------------------------
            // This ensures the controller actually called the service
            verify(scanningService).startScan(anyList());
        }

        /**
         * EXAMPLE TEST: Error Case
         *
         * Always test error scenarios. What happens when input is invalid?
         * This test verifies proper error handling for empty directory list.
         */
        @Test
        @DisplayName("Should return 400 Bad Request when directories list is empty")
        void startScan_EmptyDirectories_Returns400() throws Exception {
            // Empty directories array
            String requestBody = """
                {
                    "directories": []
                }
                """;

            mockMvc.perform(post("/api/v1/scanning/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            // Service should NOT be called for invalid input
            verify(scanningService, never()).startScan(anyList());
        }
    }

    // ========================================================================
    // GET /api/v1/scanning/status/{sessionId} - Get Scan Status
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/scanning/status/{sessionId} - Get Scan Status")
    class GetScanStatusTests {

        /**
         * EXAMPLE TEST: Path Variable
         *
         * Demonstrates testing an endpoint with a path variable.
         * The session ID is part of the URL path, not a query parameter.
         */
        @Test
        @DisplayName("Should return scan progress when valid session ID provided")
        void getScanStatus_ValidSessionId_ReturnsProgress() throws Exception {
            // Create a sample progress DTO using the factory method
            ScanProgressDTO progress = ScanProgressDTO.scanning(
                    "abc-123",           // sessionId
                    "/Users/richard/Music", // currentDirectory
                    "song.mp3",          // currentFile
                    150,                 // filesFound
                    75,                  // filesProcessed
                    10,                  // totalDirectories
                    5,                   // directoriesProcessed
                    "Scanning directory..." // message
            );

            when(scanningService.getScanStatus("abc-123")).thenReturn(Optional.of(progress));

            mockMvc.perform(get("/api/v1/scanning/status/abc-123"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sessionId", is("abc-123")))
                    .andExpect(jsonPath("$.stage", is("scanning")))
                    .andExpect(jsonPath("$.filesFound", is(150)))
                    .andExpect(jsonPath("$.filesProcessed", is(75)))
                    .andExpect(jsonPath("$.percentComplete", greaterThanOrEqualTo(0)))
                    .andExpect(jsonPath("$.isComplete", is(false)));

            verify(scanningService).getScanStatus("abc-123");
        }

        /**
         * EXAMPLE TEST: 404 Not Found
         *
         * Critical to test: what happens when a resource doesn't exist?
         * The API should return 404, not 500 or empty 200.
         */
        @Test
        @DisplayName("Should return 404 when session ID not found")
        void getScanStatus_InvalidSessionId_Returns404() throws Exception {
            when(scanningService.getScanStatus("nonexistent-id")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/scanning/status/nonexistent-id"))
                    .andExpect(status().isNotFound());

            verify(scanningService).getScanStatus("nonexistent-id");
        }
    }

    // ========================================================================
    // GET /api/v1/scanning/directories - Get Previously Scanned Directories
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/scanning/directories - Get Scan History")
    class GetScanDirectoriesTests {

        /**
         * EXAMPLE TEST: List Response
         *
         * Tests an endpoint that returns a list/array.
         * Use hasSize() and contains() matchers for array assertions.
         */
        @Test
        @DisplayName("Should return list of previously scanned directories")
        void getScanDirectories_ReturnsDirectoryList() throws Exception {
            List<String> directories = List.of(
                    "/Users/richard/Music",
                    "/Users/richard/Downloads/Music"
            );
            when(scanningService.getPreviouslyScannedDirectories()).thenReturn(directories);

            mockMvc.perform(get("/api/v1/scanning/directories"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0]", is("/Users/richard/Music")))
                    .andExpect(jsonPath("$[1]", is("/Users/richard/Downloads/Music")));

            verify(scanningService).getPreviouslyScannedDirectories();
        }

        /**
         * EXAMPLE TEST: Empty List
         *
         * Edge case: what if there's no scan history?
         * Should return empty array, not null or error.
         */
        @Test
        @DisplayName("Should return empty list when no scan history exists")
        void getScanDirectories_NoHistory_ReturnsEmptyList() throws Exception {
            when(scanningService.getPreviouslyScannedDirectories()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/scanning/directories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(scanningService).getPreviouslyScannedDirectories();
        }
    }

    // ========================================================================
    // POST /api/v1/scanning/cancel/{sessionId} - Cancel Running Scan
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/scanning/cancel/{sessionId} - Cancel Scan")
    class CancelScanTests {

        @Test
        @DisplayName("Should cancel scan and return success when valid session")
        void cancelScan_ValidSession_ReturnsSuccess() throws Exception {
            when(scanningService.cancelScan("abc-123")).thenReturn(true);

            mockMvc.perform(post("/api/v1/scanning/cancel/abc-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cancelled", is(true)));

            verify(scanningService).cancelScan("abc-123");
        }

        @Test
        @DisplayName("Should return 404 when trying to cancel non-existent scan")
        void cancelScan_InvalidSession_Returns404() throws Exception {
            when(scanningService.cancelScan("nonexistent")).thenReturn(false);

            mockMvc.perform(post("/api/v1/scanning/cancel/nonexistent"))
                    .andExpect(status().isNotFound());

            verify(scanningService).cancelScan("nonexistent");
        }
    }
}
