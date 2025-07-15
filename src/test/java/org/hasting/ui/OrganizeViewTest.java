package org.hasting.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.hasting.model.MusicFile;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrganizeView class.
 * 
 * <p>These tests verify the basic functionality of the OrganizeView component
 * including search result handling, organization queue management, and file selection.
 * 
 * @since 2.0
 */
@DisplayName("Organize View Tests")
public class OrganizeViewTest {
    
    private OrganizeView organizeView;
    private MusicFile testMusicFile;
    
    @BeforeEach
    void setUp() {
        // Skip JavaFX initialization for unit tests
        // Tests focus on non-UI logic and data structures
        
        // Create test music file
        testMusicFile = new MusicFile(new File("/test/path/song.mp3"));
        testMusicFile.setArtist("Test Artist");
        testMusicFile.setTitle("Test Song");
        testMusicFile.setAlbum("Test Album");
    }
    
    @Test
    @DisplayName("MusicFileSearchResult constructor initializes correctly")
    void testMusicFileSearchResultConstructor() {
        OrganizeView.MusicFileSearchResult result = new OrganizeView.MusicFileSearchResult(testMusicFile);
        
        assertEquals(testMusicFile, result.getMusicFile());
        assertFalse(result.isSelected());
        assertNotNull(result.selectedProperty());
    }
    
    @Test
    @DisplayName("MusicFileSearchResult selection state can be modified")
    void testMusicFileSearchResultSelection() {
        OrganizeView.MusicFileSearchResult result = new OrganizeView.MusicFileSearchResult(testMusicFile);
        
        assertFalse(result.isSelected());
        
        result.setSelected(true);
        assertTrue(result.isSelected());
        
        result.setSelected(false);
        assertFalse(result.isSelected());
    }
    
    @Test
    @DisplayName("MusicFileSelection constructor initializes correctly")
    void testMusicFileSelectionConstructor() {
        OrganizeView.MusicFileSelection selection = new OrganizeView.MusicFileSelection(testMusicFile);
        
        assertEquals(testMusicFile, selection.getMusicFile());
        assertFalse(selection.isSelected());
        assertNotNull(selection.selectedProperty());
    }
    
    @Test
    @DisplayName("MusicFileSelection selection state can be modified")
    void testMusicFileSelectionState() {
        OrganizeView.MusicFileSelection selection = new OrganizeView.MusicFileSelection(testMusicFile);
        
        assertFalse(selection.isSelected());
        
        selection.setSelected(true);
        assertTrue(selection.isSelected());
        
        selection.setSelected(false);
        assertFalse(selection.isSelected());
    }
    
    @Test
    @DisplayName("Search result and organization selection preserve music file data")
    void testMusicFileDataPreservation() {
        OrganizeView.MusicFileSearchResult searchResult = new OrganizeView.MusicFileSearchResult(testMusicFile);
        OrganizeView.MusicFileSelection orgSelection = new OrganizeView.MusicFileSelection(testMusicFile);
        
        // Verify music file data is preserved in search results
        assertEquals("Test Artist", searchResult.getMusicFile().getArtist());
        assertEquals("Test Song", searchResult.getMusicFile().getTitle());
        assertEquals("Test Album", searchResult.getMusicFile().getAlbum());
        assertEquals("/test/path/song.mp3", searchResult.getMusicFile().getFilePath());
        
        // Verify music file data is preserved in organization selection
        assertEquals("Test Artist", orgSelection.getMusicFile().getArtist());
        assertEquals("Test Song", orgSelection.getMusicFile().getTitle());
        assertEquals("Test Album", orgSelection.getMusicFile().getAlbum());
        assertEquals("/test/path/song.mp3", orgSelection.getMusicFile().getFilePath());
    }
    
    @Test
    @DisplayName("Multiple search results can have independent selection states")
    void testIndependentSelectionStates() {
        MusicFile file2 = new MusicFile(new File("/test/path/song2.mp3"));
        file2.setArtist("Artist 2");
        file2.setTitle("Song 2");
        
        OrganizeView.MusicFileSearchResult result1 = new OrganizeView.MusicFileSearchResult(testMusicFile);
        OrganizeView.MusicFileSearchResult result2 = new OrganizeView.MusicFileSearchResult(file2);
        
        // Select only first result
        result1.setSelected(true);
        result2.setSelected(false);
        
        assertTrue(result1.isSelected());
        assertFalse(result2.isSelected());
        
        // Change selection states
        result1.setSelected(false);
        result2.setSelected(true);
        
        assertFalse(result1.isSelected());
        assertTrue(result2.isSelected());
    }
}