package org.hasting.test;

import org.hasting.model.MusicFile;
import org.hasting.test.spec.TestDataSetSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a generated test data set containing multiple music files.
 * Provides convenient access to the generated files and metadata about the set.
 * 
 * @since 1.0
 */
public class TestDataSet {
    
    private final List<MusicFile> files;
    private final TestDataSetSpec specification;
    private final long generationTimeMs;
    
    /**
     * Creates a new test data set.
     * 
     * @param files The generated music files
     * @param specification The specification used to generate the files
     */
    public TestDataSet(List<MusicFile> files, TestDataSetSpec specification) {
        this.files = new ArrayList<>(files);
        this.specification = specification;
        this.generationTimeMs = System.currentTimeMillis();
    }
    
    /**
     * Gets all files in the data set.
     * 
     * @return Unmodifiable list of all files
     */
    public List<MusicFile> getFiles() {
        return Collections.unmodifiableList(files);
    }
    
    /**
     * Gets the specification used to generate this data set.
     * 
     * @return The specification
     */
    public TestDataSetSpec getSpecification() {
        return specification;
    }
    
    /**
     * Gets the number of files in the data set.
     * 
     * @return The file count
     */
    public int size() {
        return files.size();
    }
    
    /**
     * Gets the time when this data set was generated.
     * 
     * @return Generation time in milliseconds since epoch
     */
    public long getGenerationTimeMs() {
        return generationTimeMs;
    }
    
    /**
     * Gets a subset of files by format.
     * 
     * @param extension The file extension (e.g., "mp3")
     * @return List of files with the specified extension
     */
    public List<MusicFile> getFilesByFormat(String extension) {
        List<MusicFile> result = new ArrayList<>();
        String ext = extension.toLowerCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        
        for (MusicFile file : files) {
            if (file.getFilePath().toLowerCase().endsWith(ext)) {
                result.add(file);
            }
        }
        
        return result;
    }
    
    /**
     * Gets files that are identified as duplicates.
     * This uses the MusicFile's isSimilarTo method for detection.
     * 
     * @return List of lists, where each inner list contains similar files
     */
    public List<List<MusicFile>> getDuplicateGroups() {
        List<List<MusicFile>> groups = new ArrayList<>();
        List<MusicFile> processed = new ArrayList<>();
        
        for (MusicFile file : files) {
            if (processed.contains(file)) {
                continue;
            }
            
            List<MusicFile> group = new ArrayList<>();
            group.add(file);
            processed.add(file);
            
            // Find all similar files
            for (MusicFile other : files) {
                if (!processed.contains(other) && file.isSimilarTo(other)) {
                    group.add(other);
                    processed.add(other);
                }
            }
            
            if (group.size() > 1) {
                groups.add(group);
            }
        }
        
        return groups;
    }
    
    /**
     * Gets summary statistics about the data set.
     * 
     * @return A formatted string with statistics
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Test Data Set Statistics:\n");
        stats.append("------------------------\n");
        stats.append("Total files: ").append(files.size()).append("\n");
        
        // Count by format
        stats.append("\nBy Format:\n");
        for (String ext : new String[]{"mp3", "flac", "wav", "ogg"}) {
            List<MusicFile> formatFiles = getFilesByFormat(ext);
            if (!formatFiles.isEmpty()) {
                stats.append("  ").append(ext.toUpperCase()).append(": ")
                     .append(formatFiles.size()).append("\n");
            }
        }
        
        // Duplicate statistics
        List<List<MusicFile>> duplicates = getDuplicateGroups();
        if (!duplicates.isEmpty()) {
            stats.append("\nDuplicates:\n");
            stats.append("  Groups: ").append(duplicates.size()).append("\n");
            int totalDuplicates = duplicates.stream()
                    .mapToInt(List::size)
                    .sum();
            stats.append("  Total duplicated files: ").append(totalDuplicates).append("\n");
        }
        
        return stats.toString();
    }
    
    @Override
    public String toString() {
        return String.format("TestDataSet[files=%d, generated=%tF %<tT]", 
                files.size(), generationTimeMs);
    }
}