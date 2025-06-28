package org.hasting.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to understand how subdirectory depth actually works.
 */
public class SubdirectoryDepthTest {
    
    @BeforeEach
    void setUp() {
        // Reset static state
        MusicFile.resetArtistCounts();
    }
    
    @Test
    @DisplayName("Test subdirectory generation with depth 5")
    void testSubdirectoryDepth5() {
        // Create some test music files with different artists
        MusicFile[] testFiles = {
            createTestFile("The Beatles", "Abbey Road", "Come Together"),
            createTestFile("Madonna", "Like a Virgin", "Material Girl"),
            createTestFile("Elvis Presley", "That's All Right", "Blue Moon"),
            createTestFile("Adele", "21", "Rolling in the Deep"),
            createTestFile("Beatles", "Abbey Road", "Something"),
            createTestFile("Bob Dylan", "Highway 61", "Like a Rolling Stone")
        };
        
        // Add artists to the system first
        for (MusicFile file : testFiles) {
            // This simulates files being in the database
        }
        
        // Create a path template with depth 5
        PathTemplate template = new PathTemplate(
            "{subdirectory}/{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            true, // use subdirectory grouping
            5     // depth of 5
        );
        
        // Test path generation
        for (MusicFile file : testFiles) {
            String path = template.generatePath("/music", file);
            System.out.println("Artist: " + file.getArtist() + " -> Path: " + path);
        }
        
        // Print subdirectory mappings for debugging
        try {
            java.lang.reflect.Method whatInitialsMethod = MusicFile.class.getDeclaredMethod("whatInitials");
            whatInitialsMethod.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.HashMap<String, String> initials = (java.util.HashMap<String, String>) whatInitialsMethod.invoke(null);
            
            System.out.println("\nSubdirectory mappings with depth 5:");
            for (java.util.Map.Entry<String, String> entry : initials.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
        } catch (Exception e) {
            System.err.println("Could not access initials mapping: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Test subdirectory generation with depth 1")
    void testSubdirectoryDepth1() {
        // Same test but with depth 1
        MusicFile[] testFiles = {
            createTestFile("The Beatles", "Abbey Road", "Come Together"),
            createTestFile("Madonna", "Like a Virgin", "Material Girl"),
            createTestFile("Elvis Presley", "That's All Right", "Blue Moon"),
            createTestFile("Adele", "21", "Rolling in the Deep")
        };
        
        PathTemplate template = new PathTemplate(
            "{subdirectory}/{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            true, // use subdirectory grouping
            1     // depth of 1
        );
        
        // Test path generation
        for (MusicFile file : testFiles) {
            String path = template.generatePath("/music", file);
            System.out.println("Artist: " + file.getArtist() + " -> Path: " + path);
        }
        
        // Print subdirectory mappings for debugging
        try {
            java.lang.reflect.Method whatInitialsMethod = MusicFile.class.getDeclaredMethod("whatInitials");
            whatInitialsMethod.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.HashMap<String, String> initials = (java.util.HashMap<String, String>) whatInitialsMethod.invoke(null);
            
            System.out.println("\nSubdirectory mappings with depth 1:");
            for (java.util.Map.Entry<String, String> entry : initials.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
        } catch (Exception e) {
            System.err.println("Could not access initials mapping: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Test subdirectory generation with depth 26 (one per letter)")
    void testSubdirectoryDepth26() {
        // Test with maximum possible subdirectories
        MusicFile[] testFiles = {
            createTestFile("The Beatles", "Abbey Road", "Come Together"),
            createTestFile("Madonna", "Like a Virgin", "Material Girl"),
            createTestFile("Elvis Presley", "That's All Right", "Blue Moon"),
            createTestFile("Adele", "21", "Rolling in the Deep"),
            createTestFile("Zeppelin Led", "IV", "Stairway to Heaven")
        };
        
        PathTemplate template = new PathTemplate(
            "{subdirectory}/{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            true, // use subdirectory grouping
            26    // one per letter
        );
        
        // Test path generation
        for (MusicFile file : testFiles) {
            String path = template.generatePath("/music", file);
            System.out.println("Artist: " + file.getArtist() + " -> Path: " + path);
        }
    }
    
    private MusicFile createTestFile(String artist, String album, String title) {
        MusicFile file = new MusicFile();
        file.setArtist(artist);
        file.setAlbum(album);
        file.setTitle(title);
        file.setFileType("mp3");
        file.setTrackNumber(1);
        return file;
    }
}