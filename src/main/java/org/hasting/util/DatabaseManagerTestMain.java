package org.hasting.util;

import org.hasting.model.MusicFile;

import java.util.Date;
import java.util.List;

public class DatabaseManagerTestMain {
    /**
     * The main method serves as the entry point for the application.
     * It demonstrates the basic operations of initializing a database,
     * creating, saving, retrieving, updating, and deleting a music file,
     * as well as shutting down the database.
     *
     * @param args Command line arguments (not used in this application).
     */
    public static void main(String[] args) {
        // Initialize the database
        DatabaseManager.initialize();

        // Create a new MusicFile object
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

        // Save the music file to the database
        DatabaseManager.saveMusicFile(musicFile);
        System.out.println("Saved music file with ID: " + musicFile.getId());

        // Retrieve the music file by ID
        MusicFile retrievedMusicFile = DatabaseManager.getMusicFileById(musicFile.getId());
        System.out.println("Retrieved music file: " + retrievedMusicFile.getTitle());

        // Update the music file
        retrievedMusicFile.setTitle("Updated Test Song");
        DatabaseManager.updateMusicFile(retrievedMusicFile);
        System.out.println("Updated music file title to: " + retrievedMusicFile.getTitle());

        // Retrieve all music files
        List<MusicFile> allMusicFiles = DatabaseManager.getAllMusicFiles();
        System.out.println("Total music files in database: " + allMusicFiles.size());

        // Delete the music file
        DatabaseManager.deleteMusicFile(retrievedMusicFile);
        System.out.println("Deleted music file with ID: " + retrievedMusicFile.getId());

        // Shutdown the database
        DatabaseManager.shutdown();
    }
}