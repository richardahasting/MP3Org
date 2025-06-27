package org.hasting.util;

import org.hasting.model.MusicFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.hasting.util.DatabaseManager;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {

    @BeforeAll
    public static void setUp() {
        DatabaseManager.initialize();
    }

    @AfterAll
    public static void tearDown() {
        DatabaseManager.shutdown();
    }

    @Test
    public void testDatabaseConnection() {
        assertNotNull(DatabaseManager.getConnection(), "Connection should not be null");
    }

    @Test
    public void testSaveAndRetrieveMusicFile() {
        MusicFile musicFile = new MusicFile();
        musicFile.setFilePath("test/path/song.mp3");
        musicFile.setTitle("Test Song");
        musicFile.setArtist("Test Artist");
        musicFile.setAlbum("Test Album");
        musicFile.setGenre("Test Genre");
        musicFile.setTrackNumber(1);
        musicFile.setYear(2023);
        musicFile.setDurationSeconds(300);
        musicFile.setFileSizeBytes(5000L);
        musicFile.setBitRate(320L);
        musicFile.setSampleRate(44100);
        musicFile.setFileType("mp3");
        musicFile.setLastModified(new Date());

        // Save the music file
        DatabaseManager.saveMusicFile(musicFile);

        // Retrieve the music file by ID
        MusicFile retrievedMusicFile = DatabaseManager.getMusicFileById(musicFile.getId());

        assertNotNull(retrievedMusicFile, "Retrieved music file should not be null");
        assertEquals(musicFile.getFilePath(), retrievedMusicFile.getFilePath(), "File paths should match");
        assertEquals(musicFile.getTitle(), retrievedMusicFile.getTitle(), "Titles should match");
        assertEquals(musicFile.getArtist(), retrievedMusicFile.getArtist(), "Artists should match");
        assertEquals(musicFile.getAlbum(), retrievedMusicFile.getAlbum(), "Albums should match");
        assertEquals(musicFile.getGenre(), retrievedMusicFile.getGenre(), "Genres should match");
        assertEquals(musicFile.getTrackNumber(), retrievedMusicFile.getTrackNumber(), "Track numbers should match");
        assertEquals(musicFile.getYear(), retrievedMusicFile.getYear(), "Years should match");
        assertEquals(musicFile.getDurationSeconds(), retrievedMusicFile.getDurationSeconds(), "Durations should match");
        assertEquals(musicFile.getFileSizeBytes(), retrievedMusicFile.getFileSizeBytes(), "File sizes should match");
        assertEquals(musicFile.getBitRate(), retrievedMusicFile.getBitRate(), "Bit rates should match");
        assertEquals(musicFile.getSampleRate(), retrievedMusicFile.getSampleRate(), "Sample rates should match");
        assertEquals(musicFile.getFileType(), retrievedMusicFile.getFileType(), "File types should match");
    }
}