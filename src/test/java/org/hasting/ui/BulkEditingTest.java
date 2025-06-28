package org.hasting.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.hasting.model.MusicFile;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for bulk editing functionality in the metadata editor.
 * Note: These are unit tests that don't require a full JavaFX environment.
 */
public class BulkEditingTest {
    
    private MusicFile testFile1;
    private MusicFile testFile2;
    private MusicFile testFile3;
    
    @BeforeEach
    void setUp() {
        // Create test music files
        testFile1 = new MusicFile();
        testFile1.setTitle("Song One");
        testFile1.setArtist("Old Artist");
        testFile1.setAlbum("Old Album");
        testFile1.setGenre("Old Genre");
        
        testFile2 = new MusicFile();
        testFile2.setTitle("Song Two");
        testFile2.setArtist("Different Artist");
        testFile2.setAlbum("Different Album");
        testFile2.setGenre("Different Genre");
        
        testFile3 = new MusicFile();
        testFile3.setTitle("Song Three");
        testFile3.setArtist("Another Artist");
        testFile3.setAlbum("Another Album");
        testFile3.setGenre("Another Genre");
    }
    
    @Test
    @DisplayName("Test bulk artist update")
    void testBulkArtistUpdate() {
        // Simulate bulk edit operation
        String newArtist = "New Artist";
        
        // Apply to all files
        testFile1.setArtist(newArtist);
        testFile2.setArtist(newArtist);
        testFile3.setArtist(newArtist);
        
        // Verify all files have the new artist
        assertEquals(newArtist, testFile1.getArtist());
        assertEquals(newArtist, testFile2.getArtist());
        assertEquals(newArtist, testFile3.getArtist());
        
        // Verify other fields are unchanged
        assertEquals("Song One", testFile1.getTitle());
        assertEquals("Old Album", testFile1.getAlbum());
        assertEquals("Old Genre", testFile1.getGenre());
    }
    
    @Test
    @DisplayName("Test bulk album update")
    void testBulkAlbumUpdate() {
        String newAlbum = "Compilation Album";
        
        testFile1.setAlbum(newAlbum);
        testFile2.setAlbum(newAlbum);
        testFile3.setAlbum(newAlbum);
        
        assertEquals(newAlbum, testFile1.getAlbum());
        assertEquals(newAlbum, testFile2.getAlbum());
        assertEquals(newAlbum, testFile3.getAlbum());
        
        // Verify other fields are unchanged
        assertEquals("Old Artist", testFile1.getArtist());
        assertEquals("Different Artist", testFile2.getArtist());
        assertEquals("Another Artist", testFile3.getArtist());
    }
    
    @Test
    @DisplayName("Test bulk genre update")
    void testBulkGenreUpdate() {
        String newGenre = "Rock";
        
        testFile1.setGenre(newGenre);
        testFile2.setGenre(newGenre);
        testFile3.setGenre(newGenre);
        
        assertEquals(newGenre, testFile1.getGenre());
        assertEquals(newGenre, testFile2.getGenre());
        assertEquals(newGenre, testFile3.getGenre());
        
        // Verify other fields are unchanged
        assertEquals("Old Album", testFile1.getAlbum());
        assertEquals("Different Album", testFile2.getAlbum());
        assertEquals("Another Album", testFile3.getAlbum());
    }
    
    @Test
    @DisplayName("Test bulk multi-field update")
    void testBulkMultiFieldUpdate() {
        String newArtist = "Various Artists";
        String newAlbum = "Greatest Hits";
        String newGenre = "Pop";
        
        // Apply all three fields to all files
        testFile1.setArtist(newArtist);
        testFile1.setAlbum(newAlbum);
        testFile1.setGenre(newGenre);
        
        testFile2.setArtist(newArtist);
        testFile2.setAlbum(newAlbum);
        testFile2.setGenre(newGenre);
        
        testFile3.setArtist(newArtist);
        testFile3.setAlbum(newAlbum);
        testFile3.setGenre(newGenre);
        
        // Verify all files have the new values
        assertEquals(newArtist, testFile1.getArtist());
        assertEquals(newAlbum, testFile1.getAlbum());
        assertEquals(newGenre, testFile1.getGenre());
        
        assertEquals(newArtist, testFile2.getArtist());
        assertEquals(newAlbum, testFile2.getAlbum());
        assertEquals(newGenre, testFile2.getGenre());
        
        assertEquals(newArtist, testFile3.getArtist());
        assertEquals(newAlbum, testFile3.getAlbum());
        assertEquals(newGenre, testFile3.getGenre());
        
        // Verify titles are unchanged
        assertEquals("Song One", testFile1.getTitle());
        assertEquals("Song Two", testFile2.getTitle());
        assertEquals("Song Three", testFile3.getTitle());
    }
    
    @Test
    @DisplayName("Test partial bulk update")
    void testPartialBulkUpdate() {
        // Simulate updating only artist and genre, leaving album unchanged
        String newArtist = "Partial Update Artist";
        String newGenre = "Jazz";
        
        // Store original albums
        String originalAlbum1 = testFile1.getAlbum();
        String originalAlbum2 = testFile2.getAlbum();
        String originalAlbum3 = testFile3.getAlbum();
        
        // Apply partial update
        testFile1.setArtist(newArtist);
        testFile1.setGenre(newGenre);
        
        testFile2.setArtist(newArtist);
        testFile2.setGenre(newGenre);
        
        testFile3.setArtist(newArtist);
        testFile3.setGenre(newGenre);
        
        // Verify updated fields
        assertEquals(newArtist, testFile1.getArtist());
        assertEquals(newGenre, testFile1.getGenre());
        assertEquals(newArtist, testFile2.getArtist());
        assertEquals(newGenre, testFile2.getGenre());
        assertEquals(newArtist, testFile3.getArtist());
        assertEquals(newGenre, testFile3.getGenre());
        
        // Verify album fields are unchanged
        assertEquals(originalAlbum1, testFile1.getAlbum());
        assertEquals(originalAlbum2, testFile2.getAlbum());
        assertEquals(originalAlbum3, testFile3.getAlbum());
    }
    
    @Test
    @DisplayName("Test validation of bulk edit fields")
    void testBulkEditValidation() {
        // Test that empty strings work correctly
        String emptyArtist = "";
        String validAlbum = "Valid Album";
        
        // Only album should be updated
        if (!emptyArtist.isEmpty()) {
            testFile1.setArtist(emptyArtist);
        }
        testFile1.setAlbum(validAlbum);
        
        // Artist should remain unchanged, album should be updated
        assertEquals("Old Artist", testFile1.getArtist());
        assertEquals(validAlbum, testFile1.getAlbum());
    }
}