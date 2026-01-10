package org.hasting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hasting.dto.MusicFileDTO;
import org.hasting.dto.PageResponse;
import org.hasting.service.MusicFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for MusicFileController REST API endpoints.
 *
 * Tests all endpoints defined in /api/v1/music with both success and error cases.
 * Uses @WebMvcTest for isolated controller testing with mocked service layer.
 *
 * Part of Issue #70 - API Test Suite
 */
@WebMvcTest(MusicFileController.class)
class MusicFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MusicFileService musicFileService;

    private MusicFileDTO sampleMusicFile;
    private PageResponse<MusicFileDTO> samplePageResponse;

    @BeforeEach
    void setUp() {
        sampleMusicFile = createSampleMusicFile(1L, "Test Song", "Test Artist", "Test Album");
        samplePageResponse = new PageResponse<>(
                List.of(sampleMusicFile),
                0, 50, 1, 1
        );
    }

    private MusicFileDTO createSampleMusicFile(Long id, String title, String artist, String album) {
        return new MusicFileDTO(
                id,
                "/path/to/file.mp3",
                title,
                artist,
                album,
                "Rock",
                1,          // trackNumber
                2023,       // year
                180,        // durationSeconds
                5000000L,   // fileSizeBytes
                320L,       // bitRate
                44100,      // sampleRate
                "mp3",      // fileType
                new Date(), // lastModified
                new Date()  // dateAdded
        );
    }

    // ========================================
    // GET /api/v1/music - List All (Paginated)
    // ========================================
    @Nested
    @DisplayName("GET /api/v1/music - List All Music Files")
    class GetAllMusicFilesTests {

        @Test
        @DisplayName("Should return paginated list with default parameters")
        void getAllMusicFiles_DefaultPagination_ReturnsPagedResults() throws Exception {
            when(musicFileService.getAllMusicFiles(0, 50)).thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/music"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title", is("Test Song")))
                    .andExpect(jsonPath("$.page", is(0)))
                    .andExpect(jsonPath("$.size", is(50)))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.totalPages", is(1)));

            verify(musicFileService).getAllMusicFiles(0, 50);
        }

        @Test
        @DisplayName("Should accept custom page and size parameters")
        void getAllMusicFiles_CustomPagination_UsesProvidedValues() throws Exception {
            PageResponse<MusicFileDTO> customPage = new PageResponse<>(
                    List.of(sampleMusicFile), 2, 25, 100, 4
            );
            when(musicFileService.getAllMusicFiles(2, 25)).thenReturn(customPage);

            mockMvc.perform(get("/api/v1/music")
                            .param("page", "2")
                            .param("size", "25"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page", is(2)))
                    .andExpect(jsonPath("$.size", is(25)));

            verify(musicFileService).getAllMusicFiles(2, 25);
        }

        @Test
        @DisplayName("Should return empty list when no music files exist")
        void getAllMusicFiles_NoFiles_ReturnsEmptyContent() throws Exception {
            PageResponse<MusicFileDTO> emptyPage = new PageResponse<>(
                    Collections.emptyList(), 0, 50, 0, 0
            );
            when(musicFileService.getAllMusicFiles(0, 50)).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/music"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    // ========================================
    // GET /api/v1/music/{id} - Get By ID
    // ========================================
    @Nested
    @DisplayName("GET /api/v1/music/{id} - Get Music File By ID")
    class GetMusicFileByIdTests {

        @Test
        @DisplayName("Should return music file when valid ID provided")
        void getMusicFileById_ValidId_ReturnsMusicFile() throws Exception {
            when(musicFileService.getMusicFileById(1L)).thenReturn(Optional.of(sampleMusicFile));

            mockMvc.perform(get("/api/v1/music/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.title", is("Test Song")))
                    .andExpect(jsonPath("$.artist", is("Test Artist")))
                    .andExpect(jsonPath("$.album", is("Test Album")));

            verify(musicFileService).getMusicFileById(1L);
        }

        @Test
        @DisplayName("Should return 404 when music file not found")
        void getMusicFileById_InvalidId_Returns404() throws Exception {
            when(musicFileService.getMusicFileById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/music/999"))
                    .andExpect(status().isNotFound());

            verify(musicFileService).getMusicFileById(999L);
        }
    }

    // ========================================
    // PUT /api/v1/music/{id} - Update Metadata
    // ========================================
    @Nested
    @DisplayName("PUT /api/v1/music/{id} - Update Music File")
    class UpdateMusicFileTests {

        @Test
        @DisplayName("Should update music file and return updated data")
        void updateMusicFile_ValidRequest_ReturnsUpdatedFile() throws Exception {
            MusicFileDTO updatedFile = createSampleMusicFile(1L, "Updated Title", "Updated Artist", "Updated Album");
            when(musicFileService.updateMusicFile(eq(1L), any(MusicFileDTO.class)))
                    .thenReturn(Optional.of(updatedFile));

            mockMvc.perform(put("/api/v1/music/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatedFile)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Updated Title")))
                    .andExpect(jsonPath("$.artist", is("Updated Artist")))
                    .andExpect(jsonPath("$.album", is("Updated Album")));

            verify(musicFileService).updateMusicFile(eq(1L), any(MusicFileDTO.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent file")
        void updateMusicFile_NotFound_Returns404() throws Exception {
            when(musicFileService.updateMusicFile(eq(999L), any(MusicFileDTO.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(put("/api/v1/music/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleMusicFile)))
                    .andExpect(status().isNotFound());

            verify(musicFileService).updateMusicFile(eq(999L), any(MusicFileDTO.class));
        }

        @Test
        @DisplayName("Should handle partial update with only some fields")
        void updateMusicFile_PartialUpdate_UpdatesOnlyProvidedFields() throws Exception {
            MusicFileDTO partialUpdate = new MusicFileDTO(
                    1L, null, "New Title", null, null, null,
                    null, null, null, null, null, null, null, null, null
            );
            MusicFileDTO result = createSampleMusicFile(1L, "New Title", "Test Artist", "Test Album");
            when(musicFileService.updateMusicFile(eq(1L), any(MusicFileDTO.class)))
                    .thenReturn(Optional.of(result));

            mockMvc.perform(put("/api/v1/music/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(partialUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("New Title")));
        }
    }

    // ========================================
    // DELETE /api/v1/music/{id} - Delete File
    // ========================================
    @Nested
    @DisplayName("DELETE /api/v1/music/{id} - Delete Music File")
    class DeleteMusicFileTests {

        @Test
        @DisplayName("Should delete music file and return 204 No Content")
        void deleteMusicFile_ValidId_Returns204() throws Exception {
            when(musicFileService.deleteMusicFile(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/music/1"))
                    .andExpect(status().isNoContent());

            verify(musicFileService).deleteMusicFile(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent file")
        void deleteMusicFile_NotFound_Returns404() throws Exception {
            when(musicFileService.deleteMusicFile(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/music/999"))
                    .andExpect(status().isNotFound());

            verify(musicFileService).deleteMusicFile(999L);
        }
    }

    // ========================================
    // GET /api/v1/music/search - Search Files
    // ========================================
    @Nested
    @DisplayName("GET /api/v1/music/search - Search Music Files")
    class SearchMusicFilesTests {

        @Test
        @DisplayName("Should search by general query parameter 'q'")
        void searchMusicFiles_GeneralQuery_ReturnsMatchingFiles() throws Exception {
            when(musicFileService.searchMusicFiles("rock", 0, 50)).thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/music/search")
                            .param("q", "rock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(musicFileService).searchMusicFiles("rock", 0, 50);
        }

        @Test
        @DisplayName("Should search by title parameter")
        void searchMusicFiles_ByTitle_ReturnsMatchingFiles() throws Exception {
            when(musicFileService.searchByTitle("Test Song", 0, 50)).thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/music/search")
                            .param("title", "Test Song"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title", is("Test Song")));

            verify(musicFileService).searchByTitle("Test Song", 0, 50);
        }

        @Test
        @DisplayName("Should search by artist parameter")
        void searchMusicFiles_ByArtist_ReturnsMatchingFiles() throws Exception {
            when(musicFileService.searchByArtist("Test Artist", 0, 50)).thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/music/search")
                            .param("artist", "Test Artist"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].artist", is("Test Artist")));

            verify(musicFileService).searchByArtist("Test Artist", 0, 50);
        }

        @Test
        @DisplayName("Should search by album parameter")
        void searchMusicFiles_ByAlbum_ReturnsMatchingFiles() throws Exception {
            when(musicFileService.searchByAlbum("Test Album", 0, 50)).thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/music/search")
                            .param("album", "Test Album"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].album", is("Test Album")));

            verify(musicFileService).searchByAlbum("Test Album", 0, 50);
        }

        @Test
        @DisplayName("Should return empty results when no matches found")
        void searchMusicFiles_NoMatches_ReturnsEmptyContent() throws Exception {
            PageResponse<MusicFileDTO> emptyPage = new PageResponse<>(
                    Collections.emptyList(), 0, 50, 0, 0
            );
            when(musicFileService.searchMusicFiles("nonexistent", 0, 50)).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/music/search")
                            .param("q", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @DisplayName("Should return all files when no search criteria provided")
        void searchMusicFiles_NoCriteria_ReturnsAllFiles() throws Exception {
            when(musicFileService.getAllMusicFiles(0, 50)).thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/music/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(musicFileService).getAllMusicFiles(0, 50);
        }

        @Test
        @DisplayName("Should prioritize title over other search parameters")
        void searchMusicFiles_MultipleCriteria_PrioritizesTitle() throws Exception {
            when(musicFileService.searchByTitle("Title", 0, 50)).thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/music/search")
                            .param("title", "Title")
                            .param("artist", "Artist")
                            .param("q", "General"))
                    .andExpect(status().isOk());

            verify(musicFileService).searchByTitle("Title", 0, 50);
            verify(musicFileService, never()).searchByArtist(anyString(), anyInt(), anyInt());
            verify(musicFileService, never()).searchMusicFiles(anyString(), anyInt(), anyInt());
        }
    }

    // ========================================
    // GET /api/v1/music/count - Get Total Count
    // ========================================
    @Nested
    @DisplayName("GET /api/v1/music/count - Get Music File Count")
    class GetMusicFileCountTests {

        @Test
        @DisplayName("Should return total count of music files")
        void getMusicFileCount_ReturnsCount() throws Exception {
            when(musicFileService.getMusicFileCount()).thenReturn(6791L);

            mockMvc.perform(get("/api/v1/music/count"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.count", is(6791)));

            verify(musicFileService).getMusicFileCount();
        }

        @Test
        @DisplayName("Should return zero when no music files exist")
        void getMusicFileCount_NoFiles_ReturnsZero() throws Exception {
            when(musicFileService.getMusicFileCount()).thenReturn(0L);

            mockMvc.perform(get("/api/v1/music/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(0)));
        }
    }

    // ========================================
    // PUT /api/v1/music/bulk - Bulk Update
    // ========================================
    @Nested
    @DisplayName("PUT /api/v1/music/bulk - Bulk Update Music Files")
    class BulkUpdateTests {

        @Test
        @DisplayName("Should bulk update multiple files and return count")
        void bulkUpdate_ValidRequest_ReturnsUpdatedCount() throws Exception {
            when(musicFileService.bulkUpdate(
                    eq(List.of(1L, 2L, 3L)),
                    eq("New Artist"),
                    eq("New Album"),
                    eq("New Genre")
            )).thenReturn(3);

            String requestBody = """
                {
                    "ids": [1, 2, 3],
                    "artist": "New Artist",
                    "album": "New Album",
                    "genre": "New Genre"
                }
                """;

            mockMvc.perform(put("/api/v1/music/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updated", is(3)));

            verify(musicFileService).bulkUpdate(
                    eq(List.of(1L, 2L, 3L)),
                    eq("New Artist"),
                    eq("New Album"),
                    eq("New Genre")
            );
        }

        @Test
        @DisplayName("Should handle partial bulk update with only some fields")
        void bulkUpdate_PartialFields_UpdatesOnlyProvided() throws Exception {
            when(musicFileService.bulkUpdate(
                    eq(List.of(1L, 2L)),
                    eq("New Artist"),
                    isNull(),
                    isNull()
            )).thenReturn(2);

            String requestBody = """
                {
                    "ids": [1, 2],
                    "artist": "New Artist"
                }
                """;

            mockMvc.perform(put("/api/v1/music/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updated", is(2)));
        }

        @Test
        @DisplayName("Should return zero when no files match provided IDs")
        void bulkUpdate_NoMatchingIds_ReturnsZero() throws Exception {
            when(musicFileService.bulkUpdate(
                    eq(List.of(999L, 998L)),
                    anyString(),
                    anyString(),
                    anyString()
            )).thenReturn(0);

            String requestBody = """
                {
                    "ids": [999, 998],
                    "artist": "New Artist",
                    "album": "New Album",
                    "genre": "New Genre"
                }
                """;

            mockMvc.perform(put("/api/v1/music/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updated", is(0)));
        }
    }
}
