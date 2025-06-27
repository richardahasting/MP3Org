package org.hasting.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MusicFileTest {

    private List<MusicFile> musicFiles;

    @BeforeEach
    public void setUp() {
        musicFiles = new ArrayList<>();
        musicFiles.add(createMusicFile("Red_Skies_at_night", "Fixx"));
        musicFiles.add(createMusicFile("Red Skies and nite", "The Fixx"));
        musicFiles.add(createMusicFile("Red Skies AT NIGHT", "Fixx, the"));
        musicFiles.add(createMusicFile("Stand or fall", "THE_FIXX"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqua lung", "Jethro  Tull"));
        musicFiles.add(createMusicFile("Song A", "Artist A"));
        musicFiles.add(createMusicFile("Red Skies AT NIGHT", "Fixx, the"));
        musicFiles.add(createMusicFile("Stand or fall", "THE_FIXX"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqua lung", "Jethro  Tull"));
        musicFiles.add(createMusicFile("Song A", "Artist A"));
        musicFiles.add(createMusicFile("Song B", "Artist B"));
        musicFiles.add(createMusicFile("Song C", "Artist C"));
        musicFiles.add(createMusicFile("Song D", "Artist B"));
        musicFiles.add(createMusicFile("Song E", "Artist C"));
        musicFiles.add(createMusicFile("North Sea Oil", "Jethro Tull"));
    }

    @Test
    public void testFindMostSimilarFiles() {
        MusicFile target = createMusicFile("Song A", "Artist A");
        List<MusicFile> result = MusicFile.findMostSimilarFiles(musicFiles, target, 3);

        assertEquals(3, result.size(), "The result should contain 3 music files.");
        assertEquals("Song A", result.get(0).getTitle(), "The first result should be 'Song A'.");
        assertEquals("Artist A", result.get(0).getArtist(), "The first result should be by 'Artist A'.");
    }

    @Test
    public void testFindMostSimilarFilesWithDifferentTarget() {
        MusicFile target = createMusicFile("Song C", "Artist C");
        List<MusicFile> result = MusicFile.findMostSimilarFiles(musicFiles, target, 2);

        assertEquals(2, result.size(), "The result should contain 2 music files.");
        assertEquals("Song C", result.get(0).getTitle(), "The first result should be 'Song C'.");
        assertEquals("Artist C", result.get(0).getArtist(), "The first result should be by 'Artist C'.");
    }

    @Test
    public void testFindMostSimilarFilesWithMultipleTargets() {
        List<MusicFile> targets = new ArrayList<>();
        targets.add(createMusicFile("Song A", "Artist A"));
        targets.add(createMusicFile("Aqualung", "Jethr Tull"));
        targets.add(createMusicFile("Red Skies", "Fixx"));
        targets.add(createMusicFile("Stand or fail", "THE_FIXX"));
        targets.add(createMusicFile("North Sea Oil", "Jethro Tull"));

        for (MusicFile target : targets) {
            List<MusicFile> result = MusicFile.findMostSimilarFiles(musicFiles, target, 3);
            assertEquals(3, result.size(), "The result should contain 3 music files.");
            System.out.println("Testing findMostSimilarFiles with target: " + target.getTitle() + " by " + target.getArtist());
            for (MusicFile mf : result) {
                System.out.println(mf);
            }
        }
    }

    /**
     * The main method serves as the entry point for the application. It creates a list of 
     * MusicFile objects, performs sorting operations using different comparators, and 
     * prints the sorted lists to the console.
     *
     * @param args command-line arguments passed to the application (not used in this method)
     */
    public static void main(String[] args) {
        // Create a list of MusicFile objects
        List<MusicFile> musicFiles = new ArrayList<>();
        musicFiles.add(createMusicFile("Red_Skies_at_night", "Fixx"));
        musicFiles.add(createMusicFile("Red Skies and nite", "The Fixx"));
        musicFiles.add(createMusicFile("Red Skies AT NIGHT", "Fixx, the"));
        musicFiles.add(createMusicFile("Stand or fall", "THE_FIXX"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqualung", "Jethro Tull"));
        musicFiles.add(createMusicFile("Aqua lung", "Jethro  Tull"));

        // List of target MusicFiles for comparison
        List<MusicFile> targets = new ArrayList<>();
        targets.add(createMusicFile("North Sea Oil", "Jethro Tull"));
        targets.add(createMusicFile("Song A", "Artist A"));
        targets.add(createMusicFile("Stand or fall", "THE_FIXX"));
        targets.add(createMusicFile("Aqualung", "Jethro Tull"));
        targets.add(createMusicFile("Song B", "Artist B"));
        targets.add(createMusicFile("Song C", "Artist C"));
        targets.add(createMusicFile("Song D", "Artist D"));

        for (MusicFile target : targets) {
            // Test fuzzyComparator
            System.out.println("Testing fuzzyComparator: " + target);
            Collections.sort(musicFiles, MusicFile.fuzzyComparator(target));
            for (MusicFile mf : musicFiles) {
                System.out.println(mf);
            }

            // Test exactMatchComparator by title
            System.out.println("\nTesting exactMatchComparator by title: " + target);
            Collections.sort(musicFiles, MusicFile.exactMatchComparator(target, true, false));
            for (MusicFile mf : musicFiles) {
                System.out.println(mf);
            }

            // Test exactMatchComparator by artist
            System.out.println("\nTesting exactMatchComparator by artist: " + target);
            Collections.sort(musicFiles, MusicFile.exactMatchComparator(target, false, true));
            for (MusicFile mf : musicFiles) {
                System.out.println(mf);
            }

            // Test exactMatchComparator by both title and artist
            System.out.println("\nTesting exactMatchComparator by both title and artist: " + target);
            Collections.sort(musicFiles, MusicFile.exactMatchComparator(target, true, true));
            for (MusicFile mf : musicFiles) {
                System.out.println(mf);
            }
        }
    }

    /**
     * Creates a new MusicFile object with the specified title and artist.
     *
     * @param title  the title of the music file
     * @param artist the artist of the music file
     * @return a MusicFile object with the given title and artist
     */
    private static MusicFile createMusicFile(String title, String artist) {
        MusicFile musicFile = new MusicFile();
        musicFile.setTitle(title);
        musicFile.setArtist(artist);
        return musicFile;
    }
}