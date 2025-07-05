package org.hasting.test;

import org.hasting.model.MusicFile;
import org.hasting.test.spec.*;
import org.hasting.test.generator.TestFileGenerator;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for programmatic generation of test audio files and metadata combinations.
 * Provides a high-level API for creating test data for various scenarios including
 * duplicate detection, edge cases, format testing, and performance benchmarking.
 * 
 * <p>This factory uses a template-based approach with pre-created audio files
 * that are copied and modified with different metadata to create test variations.</p>
 * 
 * <p>All generated files are tracked and can be cleaned up using {@link #cleanupGeneratedFiles()}</p>
 * 
 * @see TestFileSpec for file specification options
 * @see DuplicateSpec for duplicate generation scenarios
 * @see EdgeCaseSpec for edge case testing
 * @since 1.0
 */
public class TestDataFactory {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(TestDataFactory.class);
    
    // Track all generated files for cleanup
    private static final Set<Path> generatedFiles = ConcurrentHashMap.newKeySet();
    
    // Singleton instance of the file generator
    private static final TestFileGenerator fileGenerator = new TestFileGenerator();
    
    // Shutdown hook for automatic cleanup
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!generatedFiles.isEmpty()) {
                logger.info("Cleaning up {} generated test files on JVM shutdown", generatedFiles.size());
                cleanupGeneratedFiles();
            }
        }));
    }
    
    /**
     * Creates a set of duplicate music files based on the provided specification.
     * 
     * @param spec The specification for duplicate generation
     * @return List of music files that are variations of the same base file
     * @throws IOException if file generation fails
     */
    public static List<MusicFile> createDuplicateSet(DuplicateSpec spec) throws IOException {
        logger.info("Creating duplicate set with {} variations", spec.getVariationCount());
        
        List<MusicFile> duplicates = new ArrayList<>();
        
        // Create the base file
        TestFileSpec baseSpec = TestFileSpec.builder()
                .title(spec.getBaseTitle())
                .artist(spec.getBaseArtist())
                .album(spec.getBaseAlbum())
                .format(spec.getFormat())
                .build();
                
        MusicFile baseFile = createCustomFile(baseSpec);
        duplicates.add(baseFile);
        
        // Generate variations
        for (DuplicateSpec.DuplicateVariation variation : spec.getVariations()) {
            TestFileSpec variantSpec = createVariantSpec(baseSpec, variation);
            MusicFile variant = createCustomFile(variantSpec);
            duplicates.add(variant);
        }
        
        logger.info("Successfully created {} duplicate files", duplicates.size());
        return duplicates;
    }
    
    /**
     * Creates a set of edge case test files based on the provided specification.
     * 
     * @param spec The specification for edge case generation
     * @return List of music files representing various edge cases
     * @throws IOException if file generation fails
     */
    public static List<MusicFile> createEdgeCaseSet(EdgeCaseSpec spec) throws IOException {
        logger.info("Creating edge case set with {} types", spec.getTypes().size());
        
        List<MusicFile> edgeCases = new ArrayList<>();
        
        for (EdgeCaseSpec.EdgeCaseType type : spec.getTypes()) {
            TestFileSpec fileSpec = createEdgeCaseSpec(type, spec);
            MusicFile edgeCase = createCustomFile(fileSpec);
            edgeCases.add(edgeCase);
        }
        
        logger.info("Successfully created {} edge case files", edgeCases.size());
        return edgeCases;
    }
    
    /**
     * Creates a set of music files in different formats for format testing.
     * 
     * @param spec The specification for format testing
     * @return List of music files in various formats
     * @throws IOException if file generation fails
     */
    public static List<MusicFile> createFormatTestSet(FormatSpec spec) throws IOException {
        logger.info("Creating format test set for {} formats", spec.getFormats().size());
        
        List<MusicFile> formatFiles = new ArrayList<>();
        
        for (AudioFormat format : spec.getFormats()) {
            TestFileSpec fileSpec = TestFileSpec.builder()
                    .title(spec.getBaseTitle())
                    .artist(spec.getBaseArtist())
                    .format(format)
                    .build();
                    
            MusicFile formatFile = createCustomFile(fileSpec);
            formatFiles.add(formatFile);
        }
        
        logger.info("Successfully created {} format test files", formatFiles.size());
        return formatFiles;
    }
    
    /**
     * Creates a single custom music file based on the provided specification.
     * 
     * @param spec The specification for the file
     * @return The generated music file
     * @throws IOException if file generation fails
     */
    public static MusicFile createCustomFile(TestFileSpec spec) throws IOException {
        logger.debug("Creating custom file: {}", spec);
        
        File generatedFile = fileGenerator.generateFromSpec(spec);
        generatedFiles.add(generatedFile.toPath());
        
        // Create MusicFile object from the generated file
        MusicFile musicFile = new MusicFile(generatedFile);
        
        // The metadata should already be embedded by the generator
        // MusicFile constructor automatically extracts metadata
        
        return musicFile;
    }
    
    /**
     * Creates a complete test data set based on the provided specification.
     * This is useful for performance testing and large-scale scenarios.
     * 
     * @param spec The specification for the test data set
     * @return The generated test data set
     * @throws IOException if file generation fails
     */
    public static TestDataSet createTestDataSet(TestDataSetSpec spec) throws IOException {
        logger.info("Creating test data set with {} files", spec.getFileCount());
        
        List<MusicFile> allFiles = new ArrayList<>();
        
        // Generate files according to format distribution
        Map<AudioFormat, Integer> distribution = spec.getFormatDistribution();
        for (Map.Entry<AudioFormat, Integer> entry : distribution.entrySet()) {
            AudioFormat format = entry.getKey();
            int count = entry.getValue();
            
            for (int i = 0; i < count; i++) {
                TestFileSpec fileSpec = spec.isRandomizeMetadata() 
                    ? TestFileSpec.randomized().format(format).build()
                    : TestFileSpec.builder()
                        .title("Test Song " + i)
                        .artist("Test Artist " + i)
                        .format(format)
                        .build();
                        
                MusicFile file = createCustomFile(fileSpec);
                allFiles.add(file);
            }
        }
        
        // Add duplicates if requested
        if (spec.isIncludeDuplicates()) {
            int duplicateCount = (int) (spec.getFileCount() * spec.getDuplicatePercentage() / 100.0);
            logger.info("Adding {} duplicate files", duplicateCount);
            
            for (int i = 0; i < duplicateCount && i < allFiles.size(); i++) {
                MusicFile original = allFiles.get(i);
                DuplicateSpec dupSpec = DuplicateSpec.builder()
                        .baseTitle(original.getTitle())
                        .baseArtist(original.getArtist())
                        .addVariation(DuplicateSpec.DuplicateVariation.TITLE_TYPO)
                        .build();
                        
                List<MusicFile> duplicates = createDuplicateSet(dupSpec);
                allFiles.addAll(duplicates.subList(1, duplicates.size())); // Skip the base file
            }
        }
        
        logger.info("Successfully created test data set with {} files", allFiles.size());
        return new TestDataSet(allFiles, spec);
    }
    
    /**
     * Cleans up all generated test files.
     * This method will delete all files created by this factory.
     */
    public static void cleanupGeneratedFiles() {
        logger.info("Cleaning up {} generated files", generatedFiles.size());
        
        int deleted = 0;
        int failed = 0;
        
        for (Path path : generatedFiles) {
            try {
                Files.deleteIfExists(path);
                deleted++;
            } catch (IOException e) {
                logger.error("Failed to delete generated file: {}", path, e);
                failed++;
            }
        }
        
        generatedFiles.clear();
        logger.info("Cleanup complete: {} files deleted, {} failed", deleted, failed);
    }
    
    /**
     * Creates a variant specification based on the variation type.
     */
    private static TestFileSpec createVariantSpec(TestFileSpec baseSpec, DuplicateSpec.DuplicateVariation variation) {
        TestFileSpec.Builder builder = TestFileSpec.builder()
                .artist(baseSpec.getArtist())
                .album(baseSpec.getAlbum())
                .genre(baseSpec.getGenre())
                .trackNumber(baseSpec.getTrackNumber())
                .year(baseSpec.getYear())
                .format(baseSpec.getFormat());
        
        switch (variation) {
            case TITLE_TYPO:
                builder.title(introduceTypo(baseSpec.getTitle()));
                break;
            case ARTIST_FEATURING:
                builder.artist(baseSpec.getArtist() + " ft. Someone");
                break;
            case CASE_DIFFERENT:
                builder.title(baseSpec.getTitle().toLowerCase());
                break;
            case BITRATE_DIFFERENT:
                builder.title(baseSpec.getTitle());
                // Handle null bitrate gracefully
                Integer currentBitRate = baseSpec.getBitRate();
                if (currentBitRate != null) {
                    builder.bitRate(currentBitRate == 320 ? 192 : 320);
                } else {
                    builder.bitRate(320); // Default to 320 if null
                }
                break;
            default:
                builder.title(baseSpec.getTitle());
        }
        
        return builder.build();
    }
    
    /**
     * Creates an edge case specification based on the edge case type.
     */
    private static TestFileSpec createEdgeCaseSpec(EdgeCaseSpec.EdgeCaseType type, EdgeCaseSpec spec) {
        TestFileSpec.Builder builder = TestFileSpec.builder();
        
        switch (type) {
            case UNICODE_TITLE:
                builder.title("日本語タイトル")
                       .artist("Normal Artist");
                break;
            case UNICODE_ARTIST:
                builder.title("Normal Title")
                       .artist("Björk");
                break;
            case LONG_STRINGS:
                String longString = "A".repeat(255);
                builder.title(longString)
                       .artist(longString);
                break;
            case SPECIAL_CHARACTERS:
                builder.title("Test / Song \\ With : Special * Characters")
                       .artist("Artist & Band | Feat. Someone");
                break;
            case MISSING_METADATA:
                // Builder will leave fields null
                break;
            case EMPTY_STRINGS:
                builder.title("")
                       .artist("");
                break;
            default:
                builder.title("Edge Case Test");
        }
        
        if (spec.getPreferredFormat() != null) {
            builder.format(spec.getPreferredFormat());
        }
        
        return builder.build();
    }
    
    /**
     * Introduces a simple typo into a string for testing.
     */
    private static String introduceTypo(String original) {
        if (original == null || original.length() < 2) {
            return original;
        }
        
        // Swap two characters in the middle
        int pos = original.length() / 2;
        char[] chars = original.toCharArray();
        if (pos > 0 && pos < chars.length - 1) {
            char temp = chars[pos];
            chars[pos] = chars[pos + 1];
            chars[pos + 1] = temp;
        }
        
        return new String(chars);
    }
}