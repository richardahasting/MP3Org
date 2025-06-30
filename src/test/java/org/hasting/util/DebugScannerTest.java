package org.hasting.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import org.hasting.model.MusicFile;

public class DebugScannerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void debugCaseInsensitive() throws IOException {
        Path testDir = tempDir.resolve("debug_test");
        Files.createDirectories(testDir);
        
        // Create test files
        Files.write(testDir.resolve("test.MP3"), "MP3 content".getBytes());
        Files.write(testDir.resolve("test.FLAC"), "FLAC content".getBytes());
        Files.write(testDir.resolve("test.WaV"), "WAV content".getBytes());
        
        System.out.println("Created files in: " + testDir);
        Files.list(testDir).forEach(f -> System.out.println("  " + f.getFileName()));
        
        MusicFileScanner scanner = new MusicFileScanner();
        List<MusicFile> musicFiles = scanner.findAllMusicFiles(testDir.toString());
        
        System.out.println("Found " + musicFiles.size() + " music files:");
        musicFiles.forEach(f -> System.out.println("  " + f.getFileType() + ": " + f.getFilePath()));
        
        // Check DatabaseConfig
        DatabaseConfig config = DatabaseConfig.getInstance();
        System.out.println("MP3 enabled: " + config.isFileTypeEnabled("mp3"));
        System.out.println("FLAC enabled: " + config.isFileTypeEnabled("flac"));
        System.out.println("WAV enabled: " + config.isFileTypeEnabled("wav"));
    }
}