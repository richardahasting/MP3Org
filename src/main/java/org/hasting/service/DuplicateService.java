package org.hasting.service;

import org.hasting.dto.DuplicateGroupDTO;
import org.hasting.dto.MusicFileDTO;
import org.hasting.model.MusicFile;
import org.hasting.util.DatabaseManager;
import org.hasting.util.FingerprintMatcher;
import org.hasting.util.FuzzyMatcher;
import org.hasting.util.FuzzySearchConfig;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for duplicate detection operations.
 * Wraps the existing FuzzyMatcher logic with REST API support.
 */
@Service
public class DuplicateService {

    private static final Logger logger = Log4Rich.getLogger(DuplicateService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Track active duplicate detection sessions
    private final Map<String, DuplicateSession> activeSessions = new ConcurrentHashMap<>();

    // Cache for duplicate groups (refreshed on demand)
    private List<DuplicateGroupDTO> cachedDuplicateGroups = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 60000; // 1 minute cache
    private static final int GROUP_BATCH_SIZE = 25; // Send groups to frontend every 25 found

    public DuplicateService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Gets all duplicate groups from the database.
     * Uses fingerprint-based matching for accurate duplicate detection.
     * Falls back to metadata matching if fingerprints are not available.
     */
    public List<DuplicateGroupDTO> getDuplicateGroups() {
        // Check cache validity
        if (cachedDuplicateGroups != null &&
            System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
            logger.debug("Returning cached duplicate groups");
            return cachedDuplicateGroups;
        }

        logger.info("Computing duplicate groups...");

        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();

        // Count files with fingerprints
        long filesWithFingerprints = allFiles.stream().filter(MusicFile::hasFingerprint).count();
        logger.info("Files with fingerprints: {}/{}", filesWithFingerprints, allFiles.size());

        List<List<MusicFile>> groups;

        if (filesWithFingerprints > allFiles.size() / 2) {
            // Use fingerprint matching if majority of files have fingerprints
            logger.info("Using fingerprint-based duplicate detection");
            groups = FingerprintMatcher.groupDuplicates(allFiles);
        } else {
            // Fall back to metadata matching
            logger.info("Using metadata-based duplicate detection (fingerprints not available for most files)");
            FuzzySearchConfig config = new FuzzySearchConfig();
            groups = FuzzyMatcher.groupDuplicates(allFiles, config);
        }

        AtomicInteger groupId = new AtomicInteger(1);
        cachedDuplicateGroups = groups.stream()
            .map(group -> DuplicateGroupDTO.fromFiles(
                groupId.getAndIncrement(),
                group.stream().map(MusicFileDTO::fromEntity).collect(Collectors.toList())
            ))
            .collect(Collectors.toList());

        cacheTimestamp = System.currentTimeMillis();

        logger.info("Found {} duplicate groups", cachedDuplicateGroups.size());
        return cachedDuplicateGroups;
    }

    /**
     * Gets the count of duplicate groups.
     */
    public int getDuplicateGroupCount() {
        return getDuplicateGroups().size();
    }

    /**
     * Gets a specific duplicate group by ID.
     */
    public Optional<DuplicateGroupDTO> getDuplicateGroup(int groupId) {
        return getDuplicateGroups().stream()
            .filter(g -> g.groupId() == groupId)
            .findFirst();
    }

    /**
     * Finds files similar to a specific file.
     * Uses fingerprint matching when available, falls back to metadata matching.
     */
    public List<MusicFileDTO> findSimilarFiles(long fileId) {
        MusicFile target = DatabaseManager.getMusicFileById(fileId);
        if (target == null) {
            return List.of();
        }

        List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();

        // Use fingerprint matching if target has fingerprint
        if (target.hasFingerprint()) {
            return FingerprintMatcher.findSimilarFiles(target, allFiles, FingerprintMatcher.DEFAULT_SIMILARITY_THRESHOLD)
                .stream()
                .map(sf -> MusicFileDTO.fromEntity(sf.file()))
                .collect(Collectors.toList());
        }

        // Fall back to metadata matching
        FuzzySearchConfig config = new FuzzySearchConfig();
        return allFiles.stream()
            .filter(f -> f.getId() != fileId)
            .filter(f -> FuzzyMatcher.areDuplicates(target, f, config))
            .map(MusicFileDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Compares two files and returns similarity details.
     */
    public Map<String, Object> compareFiles(long fileId1, long fileId2) {
        MusicFile file1 = DatabaseManager.getMusicFileById(fileId1);
        MusicFile file2 = DatabaseManager.getMusicFileById(fileId2);

        if (file1 == null || file2 == null) {
            return Map.of("error", "One or both files not found");
        }

        FuzzySearchConfig config = new FuzzySearchConfig();

        double similarity = FuzzyMatcher.calculateSimilarity(file1, file2, config);
        boolean areDuplicates = FuzzyMatcher.areDuplicates(file1, file2, config);
        String breakdown = FuzzyMatcher.getSimilarityBreakdown(file1, file2, config);

        return Map.of(
            "file1", MusicFileDTO.fromEntity(file1),
            "file2", MusicFileDTO.fromEntity(file2),
            "similarity", similarity,
            "areDuplicates", areDuplicates,
            "breakdown", breakdown
        );
    }

    /**
     * Starts an asynchronous duplicate detection scan.
     * Progress is broadcast via WebSocket.
     */
    public String startDuplicateScan() {
        String sessionId = UUID.randomUUID().toString();
        DuplicateSession session = new DuplicateSession(sessionId);
        activeSessions.put(sessionId, session);

        executor.submit(() -> runDuplicateScan(session));

        return sessionId;
    }

    /**
     * Gets the status of a duplicate detection session.
     */
    public Optional<DuplicateScanStatus> getScanStatus(String sessionId) {
        DuplicateSession session = activeSessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        return Optional.of(session.getStatus());
    }

    /**
     * Cancels a running duplicate scan.
     */
    public boolean cancelScan(String sessionId) {
        DuplicateSession session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        session.cancel();
        return true;
    }

    /**
     * Invalidates the duplicate cache, forcing a refresh on next request.
     */
    public void invalidateCache() {
        cachedDuplicateGroups = null;
        cacheTimestamp = 0;
        logger.info("Duplicate cache invalidated");
    }

    /**
     * Deletes a file and removes it from duplicate groups.
     */
    public boolean deleteFile(long fileId) {
        MusicFile musicFile = DatabaseManager.getMusicFileById(fileId);
        if (musicFile == null) {
            return false;
        }
        boolean deleted = DatabaseManager.deleteMusicFile(musicFile);
        if (deleted) {
            invalidateCache();
        }
        return deleted;
    }

    /**
     * Keeps one file from a duplicate group and deletes the rest.
     */
    public int keepFileDeleteOthers(int groupId, long keepFileId) {
        Optional<DuplicateGroupDTO> groupOpt = getDuplicateGroup(groupId);
        if (groupOpt.isEmpty()) {
            return 0;
        }

        DuplicateGroupDTO group = groupOpt.get();
        int deletedCount = 0;

        for (MusicFileDTO file : group.files()) {
            if (file.id() != keepFileId) {
                MusicFile musicFile = DatabaseManager.getMusicFileById(file.id());
                if (musicFile != null && DatabaseManager.deleteMusicFile(musicFile)) {
                    deletedCount++;
                }
            }
        }

        if (deletedCount > 0) {
            invalidateCache();
        }

        logger.info("Kept file {} and deleted {} others from group {}", keepFileId, deletedCount, groupId);
        return deletedCount;
    }

    // Private helper methods

    private void runDuplicateScan(DuplicateSession session) {
        try {
            session.setStage("loading");
            broadcastProgress(session);

            List<MusicFile> allFiles = DatabaseManager.getAllMusicFiles();
            FuzzySearchConfig config = new FuzzySearchConfig();

            // Determine if we should use fingerprint matching
            long filesWithFingerprints = allFiles.stream().filter(MusicFile::hasFingerprint).count();
            boolean useFingerprints = filesWithFingerprints > allFiles.size() / 2;

            logger.info("Duplicate scan: {} files, {} with fingerprints, using {} matching",
                allFiles.size(), filesWithFingerprints, useFingerprints ? "fingerprint" : "metadata");

            session.setTotalFiles(allFiles.size());
            session.setStage("scanning");
            broadcastProgress(session);

            List<List<MusicFile>> duplicateGroups;

            if (useFingerprints) {
                // Use fingerprint-based matching (parallel, efficient)
                duplicateGroups = FingerprintMatcher.groupDuplicates(allFiles);
                session.setGroupsFound(duplicateGroups.size());

                // Broadcast all groups at once for fingerprint matching
                AtomicInteger groupIdCounter = new AtomicInteger(1);
                List<DuplicateGroupDTO> allGroupDTOs = duplicateGroups.stream()
                    .map(g -> DuplicateGroupDTO.fromFiles(
                        groupIdCounter.getAndIncrement(),
                        g.stream().map(MusicFileDTO::fromEntity).collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());

                if (!allGroupDTOs.isEmpty()) {
                    broadcastGroups(session, allGroupDTOs);
                }
            } else {
                // Fall back to metadata-based fuzzy matching
                duplicateGroups = new ArrayList<>();
                Set<MusicFile> processed = new HashSet<>();
                AtomicInteger groupIdCounter = new AtomicInteger(1);
                int lastBroadcastedGroupCount = 0;

                session.setTotalComparisons((allFiles.size() * (allFiles.size() - 1)) / 2);

                for (int i = 0; i < allFiles.size() && !session.isCancelled(); i++) {
                    MusicFile file1 = allFiles.get(i);
                    if (processed.contains(file1)) continue;

                    List<MusicFile> group = new ArrayList<>();
                    group.add(file1);
                    processed.add(file1);

                    for (int j = i + 1; j < allFiles.size() && !session.isCancelled(); j++) {
                        MusicFile file2 = allFiles.get(j);
                        if (processed.contains(file2)) continue;

                        if (FuzzyMatcher.areDuplicates(file1, file2, config)) {
                            group.add(file2);
                            processed.add(file2);
                        }

                        session.incrementComparisons();
                    }

                    if (group.size() > 1) {
                        duplicateGroups.add(group);
                        session.setGroupsFound(duplicateGroups.size());

                        // Broadcast new groups in batches of GROUP_BATCH_SIZE
                        if (duplicateGroups.size() - lastBroadcastedGroupCount >= GROUP_BATCH_SIZE) {
                            List<DuplicateGroupDTO> newGroups = duplicateGroups
                                .subList(lastBroadcastedGroupCount, duplicateGroups.size())
                                .stream()
                                .map(g -> DuplicateGroupDTO.fromFiles(
                                    groupIdCounter.getAndIncrement(),
                                    g.stream().map(MusicFileDTO::fromEntity).collect(Collectors.toList())
                                ))
                                .collect(Collectors.toList());

                            broadcastGroups(session, newGroups);
                            lastBroadcastedGroupCount = duplicateGroups.size();
                        }
                    }

                    session.setFilesProcessed(i + 1);

                    // Broadcast progress every 100 files
                    if (i % 100 == 0) {
                        broadcastProgress(session);
                    }
                }

                // Broadcast any remaining groups that weren't in a full batch
                if (duplicateGroups.size() > lastBroadcastedGroupCount) {
                    List<DuplicateGroupDTO> remainingGroups = duplicateGroups
                        .subList(lastBroadcastedGroupCount, duplicateGroups.size())
                        .stream()
                        .map(g -> DuplicateGroupDTO.fromFiles(
                            groupIdCounter.getAndIncrement(),
                            g.stream().map(MusicFileDTO::fromEntity).collect(Collectors.toList())
                        ))
                        .collect(Collectors.toList());

                    broadcastGroups(session, remainingGroups);
                }
            }

            if (session.isCancelled()) {
                session.setStage("cancelled");
            } else {
                session.setStage("completed");

                // Update cache with results
                AtomicInteger cacheGroupId = new AtomicInteger(1);
                cachedDuplicateGroups = duplicateGroups.stream()
                    .map(group -> DuplicateGroupDTO.fromFiles(
                        cacheGroupId.getAndIncrement(),
                        group.stream().map(MusicFileDTO::fromEntity).collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());
                cacheTimestamp = System.currentTimeMillis();
            }

            broadcastProgress(session);

        } catch (Exception e) {
            logger.error("Error during duplicate scan", e);
            session.setStage("error");
            session.setError(e.getMessage());
            broadcastProgress(session);
        }
    }

    private void broadcastProgress(DuplicateSession session) {
        messagingTemplate.convertAndSend(
            "/topic/duplicates/" + session.getSessionId(),
            session.getStatus()
        );
    }

    private void broadcastGroups(DuplicateSession session, List<DuplicateGroupDTO> groups) {
        messagingTemplate.convertAndSend(
            "/topic/duplicates/" + session.getSessionId() + "/groups",
            new DuplicateGroupBatch(groups, session.getStatus().groupsFound())
        );
        logger.info("Broadcasted {} new duplicate groups (total: {})", groups.size(), session.getStatus().groupsFound());
    }

    /**
     * Batch of duplicate groups sent via WebSocket.
     */
    public record DuplicateGroupBatch(
        List<DuplicateGroupDTO> groups,
        int totalGroupsFound
    ) {}

    /**
     * Inner class to track duplicate scan session state.
     */
    private static class DuplicateSession {
        private final String sessionId;
        private String stage = "starting";
        private int totalFiles = 0;
        private int filesProcessed = 0;
        private long totalComparisons = 0;
        private long comparisonsCompleted = 0;
        private int groupsFound = 0;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private String error = null;

        DuplicateSession(String sessionId) {
            this.sessionId = sessionId;
        }

        String getSessionId() { return sessionId; }
        void setStage(String stage) { this.stage = stage; }
        void setTotalFiles(int total) { this.totalFiles = total; }
        void setFilesProcessed(int processed) { this.filesProcessed = processed; }
        void setTotalComparisons(long total) { this.totalComparisons = total; }
        void incrementComparisons() { this.comparisonsCompleted++; }
        void setGroupsFound(int groups) { this.groupsFound = groups; }
        void setError(String error) { this.error = error; }
        void cancel() { this.cancelled.set(true); }
        boolean isCancelled() { return cancelled.get(); }

        DuplicateScanStatus getStatus() {
            int percentComplete = totalComparisons > 0
                ? (int) ((comparisonsCompleted * 100) / totalComparisons)
                : 0;

            return new DuplicateScanStatus(
                sessionId, stage, totalFiles, filesProcessed,
                totalComparisons, comparisonsCompleted, groupsFound,
                percentComplete, cancelled.get(),
                stage.equals("completed"), error
            );
        }
    }

    /**
     * Status record for duplicate scan progress.
     */
    public record DuplicateScanStatus(
        String sessionId,
        String stage,
        int totalFiles,
        int filesProcessed,
        long totalComparisons,
        long comparisonsCompleted,
        int groupsFound,
        int percentComplete,
        boolean isCancelled,
        boolean isComplete,
        String error
    ) {}
}
