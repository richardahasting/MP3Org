package org.hasting.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the MusicFile refactoring maintains backward compatibility
 * and that all extracted utility services work correctly.
 */
public class MusicFileRefactorTest {
    
    private List<MusicFile> musicFiles;
    
    @BeforeEach
    void setUp() {
        musicFiles = new ArrayList<>();
        
        // Create test music files with file paths to avoid null pointer issues
        MusicFile file1 = new MusicFile();
        file1.setTitle("Test Song");
        file1.setArtist("Test Artist");
        file1.setAlbum("Test Album");
        file1.setFilePath("/test/path1.mp3");
        musicFiles.add(file1);
        
        MusicFile file2 = new MusicFile();
        file2.setTitle("Another Song");
        file2.setArtist("Test Artist");
        file2.setAlbum("Test Album");
        file2.setFilePath("/test/path2.mp3");
        musicFiles.add(file2);
        
        MusicFile file3 = new MusicFile();
        file3.setTitle("Test Song Similar");
        file3.setArtist("Test Artist Similar");
        file3.setAlbum("Test Album");
        file3.setFilePath("/test/path3.mp3");
        musicFiles.add(file3);
    }
    
    @Test
    @DisplayName("MusicFile comparison methods should work after refactoring")
    void testComparisonMethods() {
        MusicFile target = musicFiles.get(0);
        
        // Test fuzzy comparator delegation
        assertDoesNotThrow(() -> {
            var comparator = MusicFile.fuzzyComparator(target);
            assertNotNull(comparator);
        });
        
        // Test exact match comparator delegation
        assertDoesNotThrow(() -> {
            var comparator = MusicFile.exactMatchComparator(target, true, true);
            assertNotNull(comparator);
        });
        
        // Test findMostSimilarFiles delegation
        assertDoesNotThrow(() -> {
            List<MusicFile> similar = MusicFile.findMostSimilarFiles(musicFiles, target, 2);
            assertNotNull(similar);
            assertEquals(2, similar.size());
        });
    }
    
    @Test
    @DisplayName("MusicFile instance methods should work after refactoring")
    void testInstanceMethods() {
        MusicFile target = musicFiles.get(0);
        
        // Test instance findMostSimilarFiles methods
        assertDoesNotThrow(() -> {
            List<MusicFile> similar1 = target.findMostSimilarFiles(musicFiles, 2);
            assertNotNull(similar1);
            assertEquals(2, similar1.size());
            
            List<MusicFile> similar2 = target.findMostSimilarFiles(musicFiles);
            assertNotNull(similar2);
            assertTrue(similar2.size() <= 10); // Should use default limit
        });
        
        // Test isItLikelyTheSameSong delegation
        MusicFile other = musicFiles.get(1);
        assertDoesNotThrow(() -> {
            boolean result = target.isItLikelyTheSameSong(other);
            // Result doesn't matter, just that it doesn't throw
        });
    }
    
    @Test
    @DisplayName("Artist statistics methods should work after refactoring")
    @SuppressWarnings("deprecation")
    void testArtistStatisticsMethods() {
        // Test deprecated methods still work
        assertDoesNotThrow(() -> {
            int subdirs1 = MusicFile.getNumberOfSubdirectorys();
            assertTrue(subdirs1 > 0);
            
            MusicFile.resetArtistCounts(); // Test reset works
            MusicFile.setNumberOfSubdirectorys(5); // Test setter works
            
            int subdirs2 = MusicFile.getNumberOfSubdirectorys();
            assertEquals(5, subdirs2);
        });
    }
    
    @Test
    @DisplayName("File organization methods should work after refactoring")
    void testFileOrganizationMethods() {
        MusicFile target = musicFiles.get(0);
        
        // Test newFileNameAndLocation delegation
        assertDoesNotThrow(() -> {
            String path1 = target.newFileNameAndLocation("/test/base");
            assertNotNull(path1);
            assertTrue(path1.startsWith("/test/base"));
            
            PathTemplate template = new PathTemplate();
            String path2 = target.newFileNameAndLocation("/test/base", template);
            assertNotNull(path2);
            assertTrue(path2.startsWith("/test/base"));
        });
        
        // Test generateOrganizationalPath delegation
        assertDoesNotThrow(() -> {
            PathTemplate template = new PathTemplate();
            String orgPath = target.generateOrganizationalPath("/test/org", template);
            assertNotNull(orgPath);
            assertEquals(orgPath, target.getOrganizationalPath());
        });
    }
    
    @Test
    @DisplayName("Field access methods should work correctly")
    void testFieldAccessMethods() {
        MusicFile file = musicFiles.get(0);
        
        // Test Fields enum and getField method
        assertEquals("Test Song", file.getField(MusicFile.Fields.TITLE));
        assertEquals("Test Artist", file.getField(MusicFile.Fields.ARTIST));
        assertEquals("Test Album", file.getField(MusicFile.Fields.ALBUM));
        
        // Test numeric fields return empty string when null
        assertEquals("", file.getField(MusicFile.Fields.TRACK_NUMBER));
        assertEquals("", file.getField(MusicFile.Fields.YEAR));
        assertEquals("", file.getField(MusicFile.Fields.BIT_RATE));
        assertEquals("", file.getField(MusicFile.Fields.SAMPLE_RATE));
        
        // Test setting numeric fields
        file.setTrackNumber(5);
        file.setYear(2023);
        file.setBitRate(320L);
        file.setSampleRate(44100);
        
        assertEquals("5", file.getField(MusicFile.Fields.TRACK_NUMBER));
        assertEquals("2023", file.getField(MusicFile.Fields.YEAR));
        assertEquals("320", file.getField(MusicFile.Fields.BIT_RATE));
        assertEquals("44100", file.getField(MusicFile.Fields.SAMPLE_RATE));
    }
    
    @Test
    @DisplayName("MusicFile constructors should work correctly")
    void testConstructors() {
        // Test default constructor
        assertDoesNotThrow(() -> {
            MusicFile file = new MusicFile();
            assertNotNull(file);
            assertNotNull(file.getDateAdded());
        });
        
        // Test that setters work and trigger isModified flag
        MusicFile file = new MusicFile();
        assertFalse(file.isModified());
        
        file.setTitle("New Title");
        assertTrue(file.isModified());
        
        file.setModified(false);
        assertFalse(file.isModified());
        
        file.setArtist("New Artist");
        assertTrue(file.isModified());
    }
}