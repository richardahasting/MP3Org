package org.hasting.test.generator;

import org.hasting.test.spec.AudioFormat;
import org.hasting.test.spec.TestFileSpec;
import org.hasting.test.template.TestTemplateManager;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Generates test audio files based on specifications.
 * Uses template files and JAudioTagger to create files with custom metadata.
 * 
 * @since 1.0
 */
public class TestFileGenerator {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(TestFileGenerator.class);
    
    private final TestTemplateManager templateManager;
    private final Path tempDirectory;
    
    /**
     * Creates a new test file generator.
     */
    public TestFileGenerator() {
        this.templateManager = new TestTemplateManager();
        
        try {
            this.tempDirectory = Files.createTempDirectory("mp3org-test-");
            logger.info("Created temporary directory for test files: {}", tempDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp directory for test files", e);
        }
    }
    
    /**
     * Generates a test file from the given specification.
     * 
     * @param spec The file specification
     * @return The generated file
     * @throws IOException if generation fails
     */
    public File generateFromSpec(TestFileSpec spec) throws IOException {
        logger.debug("Generating file from spec: {}", spec);
        
        // Get template for the format
        File template = templateManager.getTemplate(spec.getFormat());
        if (template == null) {
            throw new IOException("No template available for format: " + spec.getFormat());
        }
        
        // Generate unique filename
        String filename = generateFilename(spec);
        Path targetPath = tempDirectory.resolve(filename);
        
        // Copy template to target location
        Files.copy(template.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        File generatedFile = targetPath.toFile();
        
        // Embed metadata
        embedMetadata(generatedFile, spec);
        
        logger.info("Generated test file: {}", generatedFile.getAbsolutePath());
        return generatedFile;
    }
    
    /**
     * Embeds metadata into an audio file based on the specification.
     * 
     * @param audioFile The audio file to modify
     * @param spec The metadata specification
     * @throws IOException if metadata embedding fails
     */
    public void embedMetadata(File audioFile, TestFileSpec spec) throws IOException {
        if (!spec.getFormat().supportsMetadata()) {
            logger.debug("Format {} has limited metadata support, skipping embedding", spec.getFormat());
            return;
        }
        
        try {
            AudioFile f = AudioFileIO.read(audioFile);
            Tag tag = f.getTag();
            
            if (tag == null) {
                tag = f.createDefaultTag();
                if (tag == null) {
                    logger.warning("Unable to create tag for file: {}", audioFile);
                    return;
                }
            }
            
            // Set metadata fields
            if (spec.getTitle() != null) {
                tag.setField(FieldKey.TITLE, spec.getTitle());
            }
            if (spec.getArtist() != null) {
                tag.setField(FieldKey.ARTIST, spec.getArtist());
            }
            if (spec.getAlbum() != null) {
                tag.setField(FieldKey.ALBUM, spec.getAlbum());
            }
            if (spec.getGenre() != null) {
                tag.setField(FieldKey.GENRE, spec.getGenre());
            }
            if (spec.getTrackNumber() != null) {
                tag.setField(FieldKey.TRACK, spec.getTrackNumber().toString());
            }
            if (spec.getYear() != null) {
                tag.setField(FieldKey.YEAR, spec.getYear().toString());
            }
            
            // Save the file with new metadata
            f.commit();
            
            logger.debug("Successfully embedded metadata for: {}", audioFile.getName());
            
        } catch (Exception e) {
            logger.error("Failed to embed metadata in file: {}", audioFile, e);
            throw new IOException("Failed to embed metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generates a unique filename for the test file.
     * 
     * @param spec The file specification
     * @return The generated filename
     */
    private String generateFilename(TestFileSpec spec) {
        StringBuilder filename = new StringBuilder();
        
        // Use title and artist if available
        if (spec.getArtist() != null && spec.getTitle() != null) {
            String safeArtist = sanitizeFilename(spec.getArtist());
            String safeTitle = sanitizeFilename(spec.getTitle());
            filename.append(safeArtist).append(" - ").append(safeTitle);
        } else {
            filename.append("test-file");
        }
        
        // Add unique identifier to prevent collisions
        filename.append("-").append(UUID.randomUUID().toString().substring(0, 8));
        
        // Add extension
        filename.append(".").append(spec.getFormat().getExtension());
        
        return filename.toString();
    }
    
    /**
     * Sanitizes a string for use in a filename.
     * 
     * @param input The input string
     * @return The sanitized string
     */
    private String sanitizeFilename(String input) {
        if (input == null) {
            return "unknown";
        }
        
        // Replace problematic characters
        String sanitized = input.replaceAll("[<>:\"/\\\\|?*]", "_");
        
        // Limit length
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }
    
    /**
     * Gets the temporary directory where files are generated.
     * 
     * @return The temporary directory path
     */
    public Path getTempDirectory() {
        return tempDirectory;
    }
    
    /**
     * Cleans up the temporary directory and all generated files.
     */
    public void cleanup() {
        try {
            if (Files.exists(tempDirectory)) {
                Files.walk(tempDirectory)
                        .sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                logger.error("Failed to delete: {}", path, e);
                            }
                        });
                logger.info("Cleaned up temporary directory: {}", tempDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to cleanup temporary directory", e);
        }
    }
}