package org.hasting.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class PathTemplateTest {
    
    private MusicFile testFile;
    
    @BeforeEach
    void setUp() {
        testFile = new MusicFile();
        testFile.setArtist("Test Artist");
        testFile.setAlbum("Test Album");
        testFile.setTitle("Test Song Title");
        testFile.setGenre("Rock");
        testFile.setYear(2023);
        testFile.setTrackNumber(5);
        testFile.setFileType("mp3");
    }
    
    @Test
    void testDefaultTemplate() {
        PathTemplate template = new PathTemplate();
        String result = template.generatePath("/music", testFile);
        
        // Should use underscore formatting by default
        assertTrue(result.contains("Test_Artist"));
        assertTrue(result.contains("Test_Album"));
        assertTrue(result.contains("Test_Song_Title"));
        assertTrue(result.contains("05")); // Zero-padded track number
        assertTrue(result.endsWith(".mp3"));
    }
    
    @Test
    void testDashFormatting() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{track_number:02d}-{title}.{file_type}",
            PathTemplate.TextFormat.DASH,
            false,
            0
        );
        
        String result = template.generatePath("/music", testFile);
        
        assertTrue(result.contains("Test-Artist"));
        assertTrue(result.contains("Test-Album"));
        assertTrue(result.contains("Test-Song-Title"));
    }
    
    @Test
    void testCamelCaseFormatting() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.CAMEL_CASE,
            false,
            0
        );
        
        String result = template.generatePath("/music", testFile);
        
        assertTrue(result.contains("testArtist"));
        assertTrue(result.contains("testAlbum"));
        assertTrue(result.contains("testSongTitle"));
    }
    
    @Test
    void testPascalCaseFormatting() {
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.PASCAL_CASE,
            false,
            0
        );
        
        String result = template.generatePath("/music", testFile);
        
        // The PascalCase formatting converts "Test Artist" to "TestArtist"
        assertTrue(result.contains("TestArtist"));
        assertTrue(result.contains("TestAlbum"));
        assertTrue(result.contains("TestSongTitle"));
    }
    
    @Test
    void testCustomTemplate() {
        PathTemplate template = new PathTemplate(
            "{genre}/{year}/{artist} - {album}/{track_number:02d} - {title}.{file_type}",
            PathTemplate.TextFormat.NONE,
            false,
            0
        );
        
        String result = template.generatePath("/music", testFile);
        
        assertTrue(result.contains("Rock"));
        assertTrue(result.contains("2023"));
        assertTrue(result.contains("Test Artist - Test Album"));
        assertTrue(result.contains("05 - Test Song Title"));
    }
    
    @Test
    void testTrackNumberFormatting() {
        PathTemplate template = new PathTemplate(
            "{track_number:03d}-{title}.{file_type}",
            PathTemplate.TextFormat.NONE,
            false,
            0
        );
        
        String result = template.generatePath("/music", testFile);
        
        assertTrue(result.contains("005-Test Song Title.mp3"));
    }
    
    @Test
    void testNullValues() {
        MusicFile fileWithNulls = new MusicFile();
        fileWithNulls.setTitle("Title Only");
        fileWithNulls.setFileType("mp3");
        
        PathTemplate template = new PathTemplate(
            "{artist}/{album}/{title}.{file_type}",
            PathTemplate.TextFormat.NONE,
            false,
            0
        );
        
        String result = template.generatePath("/music", fileWithNulls);
        
        assertTrue(result.contains("Unknown/Unknown/Title Only.mp3"));
    }
    
    @Test
    void testAvailableFields() {
        String[] fields = PathTemplate.getAvailableFields();
        assertEquals(10, fields.length);
        
        // Check that key fields are present
        boolean hasArtist = false, hasAlbum = false, hasTitle = false;
        for (String field : fields) {
            if ("artist".equals(field)) hasArtist = true;
            if ("album".equals(field)) hasAlbum = true;
            if ("title".equals(field)) hasTitle = true;
        }
        
        assertTrue(hasArtist);
        assertTrue(hasAlbum);
        assertTrue(hasTitle);
    }
    
    @Test
    void testExampleTemplates() {
        String[] examples = PathTemplate.getExampleTemplates();
        assertTrue(examples.length > 0);
        
        // Test that each example template is valid (no exceptions)
        for (String exampleTemplate : examples) {
            PathTemplate template = new PathTemplate(
                exampleTemplate,
                PathTemplate.TextFormat.UNDERSCORE,
                true,
                7
            );
            
            assertDoesNotThrow(() -> {
                String result = template.generatePath("/music", testFile);
                assertNotNull(result);
                assertFalse(result.isEmpty());
            });
        }
    }
}