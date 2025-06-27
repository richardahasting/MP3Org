package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MusicFileScannerTest {

    @TempDir
    Path tempDir;
    
    private MusicFileScanner scanner;
    private Path musicDir;
    private Path subDir;

    @BeforeEach
    void setUp() throws IOException {
        scanner = new MusicFileScanner();
        
        // Create test directory structure
        musicDir = tempDir.resolve("music");
        subDir = musicDir.resolve("subdir");
        Files.createDirectories(subDir);
        
        // Create test music files
        createTestFile(musicDir, "song1.mp3", "MP3 content");
        createTestFile(musicDir, "song2.flac", "FLAC content");
        createTestFile(musicDir, "song3.wav", "WAV content");
        createTestFile(subDir, "song4.mp3", "MP3 in subdir");
        createTestFile(musicDir, "notmusic.txt", "Text file"); // Should be ignored
        createTestFile(musicDir, "image.jpg", "Image file"); // Should be ignored
    }

    @Test
    @Order(1)
    @DisplayName("Test isMusicFile static method")
    void testIsMusicFile() throws IOException {
        // Test with valid music files
        Path mp3File = tempDir.resolve("test.mp3");
        Files.createFile(mp3File);
        assertTrue(MusicFileScanner.isMusicFile(mp3File.toFile()));
        
        Path flacFile = tempDir.resolve("test.flac");
        Files.createFile(flacFile);
        assertTrue(MusicFileScanner.isMusicFile(flacFile.toFile()));
        
        Path wavFile = tempDir.resolve("test.wav");
        Files.createFile(wavFile);
        assertTrue(MusicFileScanner.isMusicFile(wavFile.toFile()));
        
        // Test with non-music files
        Path textFile = tempDir.resolve("test.txt");
        Files.createFile(textFile);
        assertFalse(MusicFileScanner.isMusicFile(textFile.toFile()));
        
        Path imageFile = tempDir.resolve("test.jpg");
        Files.createFile(imageFile);
        assertFalse(MusicFileScanner.isMusicFile(imageFile.toFile()));
        
        // Test with null and non-existent files
        assertFalse(MusicFileScanner.isMusicFile(null));
        assertFalse(MusicFileScanner.isMusicFile(new File("/nonexistent/file.mp3")));
        
        // Test with directory
        assertFalse(MusicFileScanner.isMusicFile(tempDir.toFile()));
    }

    @Test
    @Order(2)
    @DisplayName("Test findAllMusicFiles with valid directory")
    void testFindAllMusicFilesValidDirectory() {
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(musicDir.toString());
        
        assertNotNull(musicFiles);
        assertEquals(4, musicFiles.size()); // 4 music files created
        
        // Verify file types are detected correctly
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFileType().equals("mp3")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFileType().equals("flac")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFileType().equals("wav")));
        
        // Verify subdirectory files are included
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("subdir")));
    }

    @Test
    @Order(3)
    @DisplayName("Test findAllMusicFiles with invalid directory")
    void testFindAllMusicFilesInvalidDirectory() {
        List<MusicFile> musicFiles = scanner.findAllMusicFiles("/nonexistent/directory");
        
        assertNotNull(musicFiles);
        assertEquals(0, musicFiles.size());
    }

    @Test
    @Order(4)
    @DisplayName("Test findAllMusicFiles with empty directory")
    void testFindAllMusicFilesEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);
        
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(emptyDir.toString());
        
        assertNotNull(musicFiles);
        assertEquals(0, musicFiles.size());
    }

    @Test
    @Order(5)
    @DisplayName("Test scanMusicFiles with single directory")
    void testScanMusicFilesSingleDirectory() {
        List<String> directories = Arrays.asList(musicDir.toString());
        List<MusicFile> musicFiles = scanner.scanMusicFiles(directories);
        
        assertNotNull(musicFiles);
        assertEquals(4, musicFiles.size());
    }

    @Test
    @Order(6)
    @DisplayName("Test scanMusicFiles with multiple directories")
    void testScanMusicFilesMultipleDirectories() throws IOException {
        // Create second music directory
        Path musicDir2 = tempDir.resolve("music2");
        Files.createDirectories(musicDir2);
        createTestFile(musicDir2, "song5.mp3", "Another MP3");
        createTestFile(musicDir2, "song6.ogg", "OGG file");
        
        List<String> directories = Arrays.asList(musicDir.toString(), musicDir2.toString());
        List<MusicFile> musicFiles = scanner.scanMusicFiles(directories);
        
        assertNotNull(musicFiles);
        assertEquals(6, musicFiles.size()); // 4 from first dir + 2 from second dir
    }

    @Test
    @Order(7)
    @DisplayName("Test status callback functionality")
    void testStatusCallback() {
        AtomicReference<String> lastStatus = new AtomicReference<>();
        scanner.setStatusCallback(lastStatus::set);
        
        scanner.findAllMusicFiles(musicDir.toString());
        
        assertNotNull(lastStatus.get());
        assertTrue(lastStatus.get().contains("music files"));
    }

    @Test
    @Order(8)
    @DisplayName("Test progress callback functionality")
    void testProgressCallback() {
        AtomicReference<Integer> lastProgress = new AtomicReference<>();
        scanner.setProgressCallback(lastProgress::set);
        
        // Progress callback is not currently used in the implementation,
        // but we test that setting it doesn't cause errors
        assertDoesNotThrow(() -> scanner.findAllMusicFiles(musicDir.toString()));
    }

    @Test
    @Order(9)
    @DisplayName("Test stop request functionality")
    void testStopRequest() {
        // Test that requesting stop doesn't cause errors
        assertDoesNotThrow(() -> scanner.requestStop());
        
        // The current implementation doesn't actually use the stop flag,
        // but we verify it can be set without issues
        scanner.findAllMusicFiles(musicDir.toString());
    }

    @Test
    @Order(10)
    @DisplayName("Test file extension detection")
    void testFileExtensionDetection() throws IOException {
        // Test various supported extensions
        String[] supportedExtensions = {"mp3", "flac", "ogg", "wav", "aac", "m4a", "wma", "aiff", "ape", "opus"};
        
        Path testDir = tempDir.resolve("extensions");
        Files.createDirectories(testDir);
        
        for (String ext : supportedExtensions) {
            createTestFile(testDir, "test." + ext, "Test content");
        }
        
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(testDir.toString());
        
        assertEquals(supportedExtensions.length, musicFiles.size());
        
        // Verify all extensions are detected
        for (String ext : supportedExtensions) {
            assertTrue(musicFiles.stream().anyMatch(f -> f.getFileType().equals(ext)),
                      "Extension " + ext + " should be detected");
        }
    }

    @Test
    @Order(11)
    @DisplayName("Test case insensitive extension detection")
    void testCaseInsensitiveExtensions() throws IOException {
        Path testDir = tempDir.resolve("case_test");
        Files.createDirectories(testDir);
        
        createTestFile(testDir, "test.MP3", "MP3 content");
        createTestFile(testDir, "test.FLAC", "FLAC content");
        createTestFile(testDir, "test.WaV", "WAV content");
        
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(testDir.toString());
        
        assertEquals(3, musicFiles.size());
        
        // Verify extensions are normalized to lowercase
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFileType().equals("mp3")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFileType().equals("flac")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFileType().equals("wav")));
    }

    @Test
    @Order(12)
    @DisplayName("Test music file metadata extraction")
    void testMusicFileMetadataExtraction() {
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(musicDir.toString());
        
        for (MusicFile musicFile : musicFiles) {
            // Verify basic properties are set
            assertNotNull(musicFile.getFilePath());
            assertNotNull(musicFile.getFileType());
            assertNotNull(musicFile.getFileSizeBytes());
            assertTrue(musicFile.getFileSizeBytes() > 0);
            assertNotNull(musicFile.getLastModified());
            assertNotNull(musicFile.getDateAdded());
            
            // Title should be derived from filename if no metadata
            assertNotNull(musicFile.getTitle());
            assertFalse(musicFile.getTitle().isEmpty());
        }
    }

    @Test
    @Order(13)
    @DisplayName("Test scanning with file access errors")
    void testScanningWithFileAccessErrors() {
        // Test with a file path instead of directory
        Path regularFile = musicDir.resolve("song1.mp3");
        
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(regularFile.toString());
        
        // Should handle gracefully and return empty list
        assertNotNull(musicFiles);
        assertEquals(0, musicFiles.size());
    }

    @Test
    @Order(14)
    @DisplayName("Test caching functionality")
    void testCachingFunctionality() {
        // First scan
        List<MusicFile> firstScan = scanner.scanMusicFiles(Arrays.asList(musicDir.toString()));
        assertEquals(4, firstScan.size());
        
        // Second scan of same directory should use cache
        List<MusicFile> secondScan = scanner.scanMusicFiles(Arrays.asList(musicDir.toString()));
        assertEquals(0, secondScan.size()); // Should return 0 new files due to caching
    }

    @Test
    @Order(15)
    @DisplayName("Test scanner with special characters in filenames")
    void testScannerWithSpecialCharacters() throws IOException {
        Path specialDir = tempDir.resolve("special");
        Files.createDirectories(specialDir);
        
        // Create files with special characters
        createTestFile(specialDir, "song with spaces.mp3", "MP3 content");
        createTestFile(specialDir, "song-with-dashes.flac", "FLAC content");
        createTestFile(specialDir, "song_with_underscores.wav", "WAV content");
        createTestFile(specialDir, "song(with)parentheses.mp3", "MP3 content");
        
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(specialDir.toString());
        
        assertEquals(4, musicFiles.size());
        
        // Verify all files are found despite special characters
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("with spaces")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("with-dashes")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("with_underscores")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("(with)")));
    }

    @Test
    @Order(16)
    @DisplayName("Test deep directory structure")
    void testDeepDirectoryStructure() throws IOException {
        // Create deep nested structure
        Path level1 = musicDir.resolve("level1");
        Path level2 = level1.resolve("level2");
        Path level3 = level2.resolve("level3");
        Files.createDirectories(level3);
        
        createTestFile(level1, "deep1.mp3", "MP3 at level 1");
        createTestFile(level2, "deep2.mp3", "MP3 at level 2");
        createTestFile(level3, "deep3.mp3", "MP3 at level 3");
        
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(musicDir.toString());
        
        // Should find all files including the original 4 + 3 new ones
        assertEquals(7, musicFiles.size());
        
        // Verify files from deep levels are found
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("level1")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("level2")));
        assertTrue(musicFiles.stream().anyMatch(f -> f.getFilePath().contains("level3")));
    }

    @Test
    @Order(17)
    @DisplayName("Test concurrent scanning")
    void testConcurrentScanning() {
        // Test that multiple scanners can work simultaneously
        MusicFileScanner scanner2 = new MusicFileScanner();
        
        assertDoesNotThrow(() -> {
            List<MusicFile> files1 = scanner.findAllMusicFiles(musicDir.toString());
            List<MusicFile> files2 = scanner2.findAllMusicFiles(musicDir.toString());
            
            // Both should find the same files
            assertEquals(files1.size(), files2.size());
        });
    }

    // Helper method to create test files
    private void createTestFile(Path directory, String filename, String content) throws IOException {
        Path file = directory.resolve(filename);
        Files.write(file, content.getBytes());
    }
}