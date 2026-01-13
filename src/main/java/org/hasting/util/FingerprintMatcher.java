package org.hasting.util;

import org.hasting.model.MusicFile;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for comparing audio fingerprints to detect duplicate recordings.
 * Uses bit-level comparison of Chromaprint fingerprints for accurate matching.
 */
public class FingerprintMatcher {

    private static final Logger logger = Log4Rich.getLogger(FingerprintMatcher.class);

    /**
     * Default similarity threshold for considering two files as duplicates.
     * 0.85 (85%) is a good balance between catching true duplicates and avoiding false positives.
     */
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.85;

    /**
     * Minimum number of fingerprint segments needed for reliable comparison.
     */
    private static final int MIN_FINGERPRINT_LENGTH = 10;

    /**
     * Calculates the similarity between two fingerprints.
     * Uses bit-level comparison of 32-bit integer fingerprint segments.
     *
     * @param fp1 first fingerprint as comma-separated integers
     * @param fp2 second fingerprint as comma-separated integers
     * @return similarity score between 0.0 and 1.0
     */
    public static double calculateSimilarity(String fp1, String fp2) {
        if (fp1 == null || fp2 == null || fp1.isEmpty() || fp2.isEmpty()) {
            return 0.0;
        }

        int[] arr1 = parseFingerprint(fp1);
        int[] arr2 = parseFingerprint(fp2);

        if (arr1.length < MIN_FINGERPRINT_LENGTH || arr2.length < MIN_FINGERPRINT_LENGTH) {
            return 0.0;
        }

        return calculateSimilarity(arr1, arr2);
    }

    /**
     * Calculates the similarity between two fingerprint arrays.
     *
     * @param fp1 first fingerprint array
     * @param fp2 second fingerprint array
     * @return similarity score between 0.0 and 1.0
     */
    public static double calculateSimilarity(int[] fp1, int[] fp2) {
        int length = Math.min(fp1.length, fp2.length);
        if (length < MIN_FINGERPRINT_LENGTH) {
            return 0.0;
        }

        double totalSimilarity = 0.0;

        for (int i = 0; i < length; i++) {
            int xor = fp1[i] ^ fp2[i];
            int bitsDifferent = Integer.bitCount(xor);
            // Each int is 32 bits, so similarity is (32 - bitsDifferent) / 32
            totalSimilarity += 1.0 - (bitsDifferent / 32.0);
        }

        return totalSimilarity / length;
    }

    /**
     * Parses a fingerprint string into an array of integers.
     *
     * @param fingerprint comma-separated string of integers
     * @return array of integers
     */
    public static int[] parseFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) {
            return new int[0];
        }

        String[] parts = fingerprint.split(",");
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                // Parse as unsigned 32-bit integer (may overflow to negative)
                result[i] = (int) Long.parseLong(parts[i].trim());
            } catch (NumberFormatException e) {
                // Skip invalid values
                result[i] = 0;
            }
        }

        return result;
    }

    /**
     * Checks if two music files are duplicates based on fingerprint similarity.
     *
     * @param file1 first music file
     * @param file2 second music file
     * @param threshold similarity threshold (0.0 to 1.0)
     * @return true if the files are likely duplicates
     */
    public static boolean areDuplicates(MusicFile file1, MusicFile file2, double threshold) {
        if (!file1.hasFingerprint() || !file2.hasFingerprint()) {
            return false;
        }

        double similarity = calculateSimilarity(file1.getFingerprint(), file2.getFingerprint());
        return similarity >= threshold;
    }

    /**
     * Checks if two music files are duplicates using the default threshold.
     */
    public static boolean areDuplicates(MusicFile file1, MusicFile file2) {
        return areDuplicates(file1, file2, DEFAULT_SIMILARITY_THRESHOLD);
    }

    /**
     * Groups music files into duplicate clusters based on fingerprint similarity.
     * Uses parallel processing for efficient comparison of large file collections.
     *
     * @param files list of music files with fingerprints
     * @param threshold similarity threshold
     * @return list of duplicate groups (each group contains 2+ similar files)
     */
    public static List<List<MusicFile>> groupDuplicates(List<MusicFile> files, double threshold) {
        // Filter to files with fingerprints
        List<MusicFile> filesWithFingerprints = files.stream()
            .filter(MusicFile::hasFingerprint)
            .filter(f -> f.getId() != null)
            .collect(Collectors.toList());

        int n = filesWithFingerprints.size();
        if (n < 2) {
            return Collections.emptyList();
        }

        logger.info("Starting parallel fingerprint comparison for {} files ({} comparisons)",
            n, (long) n * (n - 1) / 2);
        long startTime = System.currentTimeMillis();

        // Pre-parse all fingerprints to avoid repeated string parsing
        int[][] parsedFingerprints = new int[n][];
        for (int i = 0; i < n; i++) {
            parsedFingerprints[i] = parseFingerprint(filesWithFingerprints.get(i).getFingerprint());
        }

        // Use Union-Find for efficient grouping
        UnionFind uf = new UnionFind(n);

        // Process comparisons in parallel using ForkJoinPool
        // Use up to 20 threads for maximum parallelism on multi-core systems
        int numThreads = Math.min(20, Math.max(Runtime.getRuntime().availableProcessors() * 2, 8));
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        AtomicInteger progressCounter = new AtomicInteger(0);
        int totalComparisons = n * (n - 1) / 2;

        try {
            // Parallel comparison: for each i, compare with all j > i
            pool.submit(() ->
                IntStream.range(0, n - 1).parallel().forEach(i -> {
                    int[] fp1 = parsedFingerprints[i];
                    if (fp1.length < MIN_FINGERPRINT_LENGTH) return;

                    for (int j = i + 1; j < n; j++) {
                        int[] fp2 = parsedFingerprints[j];
                        if (fp2.length < MIN_FINGERPRINT_LENGTH) continue;

                        double similarity = calculateSimilarity(fp1, fp2);
                        if (similarity >= threshold) {
                            uf.union(i, j);
                        }
                    }

                    // Log progress every 500 files
                    int processed = progressCounter.incrementAndGet();
                    if (processed % 500 == 0) {
                        logger.debug("Fingerprint comparison progress: {} / {} files processed", processed, n);
                    }
                })
            ).get();
        } catch (Exception e) {
            logger.error("Error during parallel fingerprint comparison", e);
        } finally {
            pool.shutdown();
        }

        // Convert Union-Find results to groups
        Map<Integer, List<MusicFile>> groupMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            groupMap.computeIfAbsent(root, k -> new ArrayList<>()).add(filesWithFingerprints.get(i));
        }

        // Filter to groups with 2+ files
        List<List<MusicFile>> groups = groupMap.values().stream()
            .filter(g -> g.size() > 1)
            .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Found {} duplicate groups using fingerprint matching in {}ms ({} files, {} threads)",
            groups.size(), elapsed, n, numThreads);

        return groups;
    }

    /**
     * Union-Find data structure for efficient grouping.
     * Thread-safe for concurrent union operations.
     */
    private static class UnionFind {
        private final int[] parent;
        private final int[] rank;

        UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        synchronized int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        synchronized void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX != rootY) {
                // Union by rank
                if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }
    }

    /**
     * Groups duplicates using the default threshold.
     */
    public static List<List<MusicFile>> groupDuplicates(List<MusicFile> files) {
        return groupDuplicates(files, DEFAULT_SIMILARITY_THRESHOLD);
    }

    /**
     * Finds files similar to a target file based on fingerprint.
     *
     * @param target the target music file
     * @param candidates list of candidate files to compare against
     * @param threshold similarity threshold
     * @return list of similar files with their similarity scores
     */
    public static List<SimilarFile> findSimilarFiles(MusicFile target, List<MusicFile> candidates, double threshold) {
        if (!target.hasFingerprint()) {
            return Collections.emptyList();
        }

        int[] targetFp = parseFingerprint(target.getFingerprint());
        if (targetFp.length < MIN_FINGERPRINT_LENGTH) {
            return Collections.emptyList();
        }

        List<SimilarFile> results = new ArrayList<>();

        for (MusicFile candidate : candidates) {
            if (candidate.getId() != null && candidate.getId().equals(target.getId())) {
                continue; // Skip self
            }

            if (!candidate.hasFingerprint()) {
                continue;
            }

            int[] candidateFp = parseFingerprint(candidate.getFingerprint());
            double similarity = calculateSimilarity(targetFp, candidateFp);

            if (similarity >= threshold) {
                results.add(new SimilarFile(candidate, similarity));
            }
        }

        // Sort by similarity (highest first)
        results.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));

        return results;
    }

    /**
     * Record representing a similar file with its similarity score.
     */
    public record SimilarFile(MusicFile file, double similarity) {}

    /**
     * Computes similarity scores for each file in a group relative to the first file.
     * The first file gets similarity 1.0 (reference), others get their actual similarity.
     *
     * @param group list of music files in a duplicate group
     * @return list of similarity scores (same order as input)
     */
    public static List<Double> computeGroupSimilarities(List<MusicFile> group) {
        if (group == null || group.isEmpty()) {
            return List.of();
        }

        List<Double> similarities = new ArrayList<>();
        MusicFile reference = group.get(0);
        int[] refFp = reference.hasFingerprint() ? parseFingerprint(reference.getFingerprint()) : null;

        for (int i = 0; i < group.size(); i++) {
            if (i == 0) {
                similarities.add(1.0); // Reference file has 100% similarity to itself
            } else {
                MusicFile file = group.get(i);
                if (refFp != null && file.hasFingerprint()) {
                    int[] fileFp = parseFingerprint(file.getFingerprint());
                    double similarity = calculateSimilarity(refFp, fileFp);
                    similarities.add(similarity);
                } else {
                    similarities.add(null); // No fingerprint available
                }
            }
        }

        return similarities;
    }

    /**
     * Gets a detailed comparison breakdown between two fingerprints.
     *
     * @param fp1 first fingerprint
     * @param fp2 second fingerprint
     * @return human-readable comparison breakdown
     */
    public static String getComparisonBreakdown(String fp1, String fp2) {
        if (fp1 == null || fp2 == null) {
            return "Cannot compare: one or both fingerprints missing";
        }

        int[] arr1 = parseFingerprint(fp1);
        int[] arr2 = parseFingerprint(fp2);

        int length = Math.min(arr1.length, arr2.length);
        double similarity = calculateSimilarity(arr1, arr2);

        StringBuilder sb = new StringBuilder();
        sb.append("Fingerprint Comparison:\n");
        sb.append("  Fingerprint 1 length: ").append(arr1.length).append(" segments\n");
        sb.append("  Fingerprint 2 length: ").append(arr2.length).append(" segments\n");
        sb.append("  Compared segments: ").append(length).append("\n");
        sb.append("  Overall similarity: ").append(String.format("%.1f%%", similarity * 100)).append("\n");
        sb.append("  Threshold for duplicate: ").append(String.format("%.0f%%", DEFAULT_SIMILARITY_THRESHOLD * 100)).append("\n");
        sb.append("  Verdict: ").append(similarity >= DEFAULT_SIMILARITY_THRESHOLD ? "DUPLICATE" : "NOT DUPLICATE");

        return sb.toString();
    }
}
