package org.hasting.util;

import org.hasting.model.MusicFile;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MusicFileListUtils {
    
    private static final Logger logger = Log4Rich.getLogger(MusicFileListUtils.class);

    /**
     * Retrieves a list of MusicFiles from the database and finds those that have likely
     * the same song as others in the database.
     *
     * @return A list of MusicFiles that have likely the same song as others.
     */
    public static List<MusicFile> hasLikelySameSongs() {
        List<MusicFile> allMusicFiles = DatabaseManager.getAllMusicFiles();
        AtomicInteger counter = new AtomicInteger(0);

        return allMusicFiles.parallelStream()
                .peek(musicFile -> {
                    int count = counter.incrementAndGet();
                    logger.debug(String.format("Processing duplicate detection: {} by {} (Processed: {})", musicFile.getTitle()), musicFile.getArtist(), count);
                })
                .filter(musicFile -> musicFile.findMostSimilarFiles(allMusicFiles, 4).stream()
                        .anyMatch(similarFile -> musicFile.isItLikelyTheSameSong(similarFile)))
                .collect(Collectors.toList());
    }

    /**
     * Finds all MusicFiles that are likely the same song as the given musicFile,
     * have a different ID, and sorts them by bit rate in ascending order.
     *
     * @param musicFile The MusicFile to compare against.
     * @return A sorted list of MusicFiles that are likely the same song.
     */
    public static List<MusicFile> findLikelySameSongsSortedByBitRate(MusicFile musicFile) {
        List<MusicFile> allMusicFiles = DatabaseManager.getAllMusicFiles();

        return allMusicFiles.stream()
                .filter(otherFile -> !otherFile.getId().equals(musicFile.getId()) &&
                                     musicFile.isItLikelyTheSameSong(otherFile))
                .sorted(Comparator.comparingLong(MusicFile::getBitRate))
                .collect(Collectors.toList());
    }
    
    /**
     * Removes all MusicFiles with a null ID from the given list.
     *
     * @param musicFiles The list of MusicFiles to filter.
     * @return The same list with MusicFiles having null IDs removed.
     */
    public static List<MusicFile> removeMusicFilesWithNullId(List<MusicFile> musicFiles) {
        if (musicFiles == null || musicFiles.isEmpty()) { return musicFiles;}
        Iterator<MusicFile> iterator = musicFiles.iterator();
        while (iterator.hasNext()) {
            MusicFile musicFile = iterator.next();
            if (musicFile.getId() == null) {
                iterator.remove();
            }
        }
        return musicFiles;
    }

    /**
     * Finds potential duplicate music files in the given list based on fuzzy matching.
     * This method uses the isItLikelyTheSameSong logic to identify duplicates.
     *
     * @param musicFiles The list of MusicFiles to analyze.
     * @return A list of MusicFiles that are potential duplicates.
     */
    public static List<MusicFile> findPotentialDuplicates(List<MusicFile> musicFiles) {
        if (musicFiles == null || musicFiles.size() < 2) {
            return new java.util.ArrayList<>();
        }

        return musicFiles.stream()
                .filter(musicFile -> 
                    musicFiles.stream()
                        .anyMatch(otherFile -> 
                            !otherFile.equals(musicFile) && 
                            musicFile.isItLikelyTheSameSong(otherFile)))
                .collect(Collectors.toList());
    }
}