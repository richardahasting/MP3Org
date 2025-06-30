package org.hasting.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive tests for file organization path generation.
 * Tests various PathTemplate configurations without actually copying files.
 */
public class FileOrganizationTest {
    
    private List<MusicFile> testFiles;
    
    @BeforeEach
    void setUp() {
        // Reset static state
        MusicFile.resetArtistCounts();
        
        // Create diverse test music files
        testFiles = new ArrayList<>();
        
        // Classic rock
        testFiles.add(createTestFile("The Beatles", "Abbey Road", "Come Together", 1, 1969, "mp3", 320, 44100));
        testFiles.add(createTestFile("The Beatles", "Abbey Road", "Something", 2, 1969, "mp3", 320, 44100));
        testFiles.add(createTestFile("Led Zeppelin", "Led Zeppelin IV", "Stairway to Heaven", 4, 1971, "flac", 1411, 44100));
        
        // Pop
        testFiles.add(createTestFile("Madonna", "Like a Virgin", "Material Girl", 3, 1984, "mp3", 256, 44100));
        testFiles.add(createTestFile("Adele", "21", "Rolling in the Deep", 1, 2011, "m4a", 256, 44100));
        
        // Electronic/Alternative  
        testFiles.add(createTestFile("Daft Punk", "Random Access Memories", "Get Lucky", 8, 2013, "wav", 1411, 48000));
        testFiles.add(createTestFile("Radiohead", "OK Computer", "Paranoid Android", 2, 1997, "opus", 160, 48000));
        
        // Artists with special characters and edge cases
        testFiles.add(createTestFile("AC/DC", "Back in Black", "You Shook Me All Night Long", 5, 1980, "mp3", 192, 44100));
        testFiles.add(createTestFile("Guns N' Roses", "Appetite for Destruction", "Sweet Child O' Mine", 9, 1987, "mp3", 320, 44100));
        testFiles.add(createTestFile("Niña Pastori", "Cañana", "Tu Me Camelas", 1, 2004, "mp3", 128, 44100));
        
        // Edge cases
        testFiles.add(createTestFile("", "Unknown Album", "Untitled Track", null, null, "mp3", null, null));
        testFiles.add(createTestFile("Various Artists", "Compilation Vol. 1", "Mixed Track", 12, 2020, "ogg", 256, 44100));
        
        // Add artists to simulate a larger collection for subdirectory grouping
        for (char c = 'A'; c <= 'Z'; c++) {
            testFiles.add(createTestFile(c + "rtist Name", "Test Album", "Test Song", 1, 2000, "mp3", 128, 44100));
        }
    }
    
    @Test
    @DisplayName("Test default template with subdirectory grouping")
    void testDefaultTemplate() {
        PathTemplate template = new PathTemplate(); // Default template
        
        for (MusicFile file : testFiles.subList(0, 5)) { // Test first 5 files
            String path = file.generateOrganizationalPath("/music", template);
            
            // Verify path structure: /music/{subdirectory}/{artist}/{album}/{track:02d}-{title}.{ext}
            assertTrue(path.startsWith("/music/"));
            assertTrue(path.contains(file.getArtist() != null ? file.getArtist().replaceAll("\\s+", "_") : "Unknown"));
            assertTrue(path.contains(file.getAlbum() != null ? file.getAlbum().replaceAll("\\s+", "_") : "Unknown"));
            assertTrue(path.endsWith("." + file.getFileType()));
            
            System.out.println("Default template: " + file.getArtist() + " -> " + path);
        }
    }
    
    @Test
    @DisplayName("Test simple artist/album template")
    void testSimpleTemplate() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            false, // No subdirectory grouping
            0
        );
        
        MusicFile beatles = testFiles.get(0); // The Beatles
        String path = beatles.generateOrganizationalPath("/music", template);
        
        assertEquals("/music/The_Beatles/Abbey_Road/Come_Together.mp3", path);
        System.out.println("Simple template: " + path);
    }
    
    @Test
    @DisplayName("Test genre-based organization")
    void testGenreTemplate() {
        PathTemplate template = new PathTemplate(
            "{genre}/{artist}/{year} - {album}/{track_number:02d} - {title}.{file_type}",
            PathTemplate.TextFormat.DASH,
            false,
            0
        );
        
        MusicFile file = testFiles.get(2); // Led Zeppelin
        file.setGenre("Rock");
        String path = file.generateOrganizationalPath("/music", template);
        
        assertTrue(path.contains("Rock/"));
        assertTrue(path.contains("1971 -"));
        assertTrue(path.contains("04 -"));
        System.out.println("Genre template: " + path);
    }
    
    @Test
    @DisplayName("Test different text formatting options")
    void testTextFormattingOptions() {
        MusicFile gunsNRoses = testFiles.get(8); // "Guns N' Roses"
        
        // Test UNDERSCORE formatting
        PathTemplate underscoreTemplate = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            false, 0
        );
        String underscorePath = gunsNRoses.generateOrganizationalPath("/music", underscoreTemplate);
        assertTrue(underscorePath.contains("Guns_N'_Roses"));
        System.out.println("Underscore: " + underscorePath);
        
        // Test DASH formatting
        PathTemplate dashTemplate = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.DASH,
            false, 0
        );
        String dashPath = gunsNRoses.generateOrganizationalPath("/music", dashTemplate);
        assertTrue(dashPath.contains("Guns-N'-Roses"));
        System.out.println("Dash: " + dashPath);
        
        // Test CAMEL_CASE formatting
        PathTemplate camelTemplate = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.CAMEL_CASE,
            false, 0
        );
        String camelPath = gunsNRoses.generateOrganizationalPath("/music", camelTemplate);
        assertTrue(camelPath.contains("gunsN'Roses"));
        System.out.println("CamelCase: " + camelPath);
        
        // Test UPPER_CASE formatting
        PathTemplate upperTemplate = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.UPPER_CASE,
            false, 0
        );
        String upperPath = gunsNRoses.generateOrganizationalPath("/music", upperTemplate);
        assertTrue(upperPath.contains("GUNS N' ROSES"));
        System.out.println("UpperCase: " + upperPath);
    }
    
    @Test
    @DisplayName("Test track number formatting")
    void testTrackNumberFormatting() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            PathTemplate.TextFormat.NONE,
            false, 0
        );
        
        MusicFile singleDigit = testFiles.get(0); // Track 1
        singleDigit.setTrackNumber(1);
        String path1 = singleDigit.generateOrganizationalPath("/music", template);
        assertTrue(path1.contains("01-"));
        
        MusicFile doubleDigit = testFiles.get(11); // Track 12
        doubleDigit.setTrackNumber(12);
        String path12 = doubleDigit.generateOrganizationalPath("/music", template);
        assertTrue(path12.contains("12-"));
        
        System.out.println("Track formatting: " + path1 + " and " + path12);
    }
    
    @Test
    @DisplayName("Test subdirectory grouping with different group counts")
    void testSubdirectoryGrouping() {
        // Test with 3 subdirectory groups
        PathTemplate template3 = new PathTemplate(
            "{subdirectory}/{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            true, 3
        );
        
        // Test with 8 subdirectory groups  
        PathTemplate template8 = new PathTemplate(
            "{subdirectory}/{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            true, 8
        );
        
        for (MusicFile file : testFiles.subList(0, 10)) {
            String path3 = file.generateOrganizationalPath("/music", template3);
            String path8 = file.generateOrganizationalPath("/music", template8);
            
            System.out.println("Artist: " + file.getArtist() + 
                             " | 3 groups: " + extractSubdirectory(path3) + 
                             " | 8 groups: " + extractSubdirectory(path8));
        }
    }
    
    @Test
    @DisplayName("Test bitrate and sample rate in paths")
    void testBitrateAndSampleRate() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album} [{bit_rate}kbps-{sample_rate}Hz]/{title}.{file_type}",
            PathTemplate.TextFormat.NONE,
            false, 0
        );
        
        MusicFile flacFile = testFiles.get(2); // FLAC file with high quality
        String path = flacFile.generateOrganizationalPath("/music", template);
        
        assertTrue(path.contains("[1411kbps-44100Hz]"));
        System.out.println("Quality in path: " + path);
    }
    
    @Test
    @DisplayName("Test year-based organization")
    void testYearBasedOrganization() {
        PathTemplate template = new PathTemplate(
            "{year}/{artist} - {album}/{track_number:02d} {title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            false, 0
        );
        
        MusicFile oldFile = testFiles.get(0); // 1969
        MusicFile newFile = testFiles.get(4); // 2011
        
        String oldPath = oldFile.generateOrganizationalPath("/music", template);
        String newPath = newFile.generateOrganizationalPath("/music", template);
        
        assertTrue(oldPath.startsWith("/music/1969/"));
        assertTrue(newPath.startsWith("/music/2011/"));
        
        System.out.println("Year-based old: " + oldPath);
        System.out.println("Year-based new: " + newPath);
    }
    
    @Test
    @DisplayName("Test special characters handling")
    void testSpecialCharacters() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            false, 0
        );
        
        MusicFile acdc = testFiles.get(7); // AC/DC
        MusicFile nina = testFiles.get(9); // Niña Pastori (international chars)
        
        String acdcPath = acdc.generateOrganizationalPath("/music", template);
        String ninaPath = nina.generateOrganizationalPath("/music", template);
        
        // Should handle slash in AC/DC
        assertTrue(acdcPath.contains("AC/DC"));
        
        // Should handle international characters
        assertTrue(ninaPath.contains("Niña_Pastori"));
        
        System.out.println("Special chars AC/DC: " + acdcPath);
        System.out.println("Special chars Niña: " + ninaPath);
    }
    
    @Test
    @DisplayName("Test edge cases with null/empty values")
    void testEdgeCases() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            false, 0
        );
        
        MusicFile emptyFile = testFiles.get(10); // File with empty/null values
        String path = emptyFile.generateOrganizationalPath("/music", template);
        
        // Should handle nulls gracefully
        assertTrue(path.contains("Unknown"));
        assertTrue(path.contains("00-")); // Null track number becomes 00
        
        System.out.println("Edge case path: " + path);
    }
    
    @Test
    @DisplayName("Test complex template with all fields")
    void testComplexTemplate() {
        PathTemplate template = new PathTemplate(
            "{subdirectory}/{genre}/{year}/{artist}/{album} [{bit_rate}kbps]/{track_number:02d} - {title} [{sample_rate}Hz].{file_type}",
            PathTemplate.TextFormat.DASH,
            true, 5
        );
        
        MusicFile complex = testFiles.get(2); // Led Zeppelin with full metadata
        complex.setGenre("Progressive Rock");
        
        String path = complex.generateOrganizationalPath("/music", template);
        
        System.out.println("Complex template generated path: " + path);
        
        // Verify all components are present
        assertTrue(path.contains("Progressive") || path.contains("PROGRESSIVE"), "Should contain genre: " + path);
        assertTrue(path.contains("1971"), "Should contain year: " + path);
        assertTrue(path.contains("Led") && path.contains("Zeppelin"), "Should contain artist: " + path);
        assertTrue(path.contains("1411"), "Should contain bitrate: " + path);
        assertTrue(path.contains("44100"), "Should contain sample rate: " + path);
        assertTrue(path.contains("04"), "Should contain track number: " + path);
    }
    
    @Test
    @DisplayName("Test path length and filesystem compatibility")
    void testPathLength() {
        // Create a file with very long names to test length limits
        MusicFile longFile = createTestFile(
            "This Is A Very Long Artist Name That Might Cause Issues With Some Filesystems",
            "This Is An Even Longer Album Name That Could Potentially Cause Problems With Path Length Limitations",
            "This Is An Extremely Long Song Title That Definitely Will Test The Limits Of What Most Filesystems Can Handle",
            1, 2023, "mp3", 320, 44100
        );
        
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{track_number:02d} - {title}.{file_type}",
            PathTemplate.TextFormat.UNDERSCORE,
            false, 0
        );
        
        String path = longFile.generateOrganizationalPath("/very/long/base/path/structure", template);
        
        System.out.println("Long path length: " + path.length() + " characters");
        System.out.println("Long path: " + path);
        
        // Most filesystems support 255 chars per component, ~4096 total path
        assertTrue(path.length() < 4096, "Path should be under filesystem limits");
    }
    
    /**
     * Helper method to extract subdirectory from generated path
     */
    private String extractSubdirectory(String path) {
        String[] parts = path.split("/");
        if (parts.length > 2) {
            return parts[2]; // /music/{subdirectory}/...
        }
        return "unknown";
    }
    
    /**
     * Helper method to create test MusicFile instances
     */
    private MusicFile createTestFile(String artist, String album, String title, Integer trackNumber, 
                                   Integer year, String fileType, Integer bitRate, Integer sampleRate) {
        MusicFile file = new MusicFile();
        file.setArtist(artist);
        file.setAlbum(album);
        file.setTitle(title);
        file.setTrackNumber(trackNumber);
        file.setYear(year);
        file.setFileType(fileType);
        file.setBitRate(bitRate != null ? bitRate.longValue() : null);
        file.setSampleRate(sampleRate);
        file.setFilePath("/fake/path/" + title + "." + fileType);
        return file;
    }
}