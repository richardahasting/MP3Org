package org.hasting.test.template;

import org.hasting.test.spec.AudioFormat;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages template audio files used for test data generation.
 * Templates are short audio files that serve as the basis for generating test data.
 * 
 * @since 1.0
 */
public class TestTemplateManager {
    
    private static final Logger logger = Log4Rich.getLogger(TestTemplateManager.class);
    
    // Cache for template files
    private final Map<AudioFormat, File> templateCache = new HashMap<>();
    
    // Default paths to look for templates
    private static final String[] TEMPLATE_SEARCH_PATHS = {
        "testdata",
        "src/test/resources/templates",
        "templates"
    };
    
    /**
     * Creates a new template manager and discovers available templates.
     */
    public TestTemplateManager() {
        discoverTemplates();
    }
    
    /**
     * Gets a template file for the specified format.
     * 
     * @param format The audio format
     * @return The template file, or null if not available
     */
    public File getTemplate(AudioFormat format) {
        return templateCache.get(format);
    }
    
    /**
     * Registers a template file for a specific format.
     * 
     * @param format The audio format
     * @param template The template file
     */
    public void registerTemplate(AudioFormat format, File template) {
        if (template.exists() && template.isFile()) {
            templateCache.put(format, template);
            logger.info(String.format("Registered template for {}: {}", format, template.getAbsolutePath()));
        } else {
            logger.warn(String.format("Cannot register non-existent template: {}", template));
        }
    }
    
    /**
     * Discovers template files in known locations.
     */
    private void discoverTemplates() {
        logger.info("Discovering template audio files...");
        
        // First, check for user-provided files in testdata
        File testDataDir = new File("testdata");
        if (testDataDir.exists() && testDataDir.isDirectory()) {
            // Look for the short recordings provided by the user
            File mp3Template10 = new File(testDataDir, "shortRecording10sec.mp3");
            File mp3Template20 = new File(testDataDir, "shortRecording20sec.mp3");
            
            if (mp3Template10.exists()) {
                registerTemplate(AudioFormat.MP3, mp3Template10);
                logger.info(String.format("Found user-provided MP3 template: {}", mp3Template10.getName()));
            } else if (mp3Template20.exists()) {
                registerTemplate(AudioFormat.MP3, mp3Template20);
                logger.info(String.format("Found user-provided MP3 template: {}", mp3Template20.getName()));
            }
            
            // Also check for other formats in testdata/originalMusicFiles
            File originalFiles = new File(testDataDir, "originalMusicFiles");
            if (originalFiles.exists() && originalFiles.isDirectory()) {
                discoverTemplatesInDirectory(originalFiles);
            }
        }
        
        // Then check other standard locations
        for (String searchPath : TEMPLATE_SEARCH_PATHS) {
            File dir = new File(searchPath);
            if (dir.exists() && dir.isDirectory()) {
                discoverTemplatesInDirectory(dir);
            }
        }
        
        // If no templates found, try to use existing test resources
        if (templateCache.isEmpty()) {
            File testResources = new File("src/test/resources/audio");
            if (testResources.exists() && testResources.isDirectory()) {
                logger.info("Using existing test audio files as templates");
                discoverTemplatesInDirectory(testResources);
            }
        }
        
        logger.info(String.format("Template discovery complete. Found {} templates", templateCache.size()));
    }
    
    /**
     * Discovers template files in a specific directory.
     * 
     * @param directory The directory to search
     */
    private void discoverTemplatesInDirectory(File directory) {
        try {
            Files.walk(directory.toPath(), 1)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        File file = path.toFile();
                        String filename = file.getName().toLowerCase();
                        
                        // Check each audio format
                        for (AudioFormat format : AudioFormat.values()) {
                            if (filename.endsWith("." + format.getExtension()) 
                                && !templateCache.containsKey(format)) {
                                registerTemplate(format, file);
                                break;
                            }
                        }
                    });
        } catch (IOException e) {
            logger.error(String.format("Error discovering templates in directory: {}", directory), e);
        }
    }
    
    /**
     * Creates default template files if none exist.
     * This would require audio generation capability.
     * 
     * @throws IOException if creation fails
     */
    public void createDefaultTemplates() throws IOException {
        logger.info("Creating default templates is not yet implemented");
        // This would require FFmpeg or similar to generate silent audio files
        // For now, we rely on pre-existing template files
    }
    
    /**
     * Gets information about available templates.
     * 
     * @return A formatted string with template information
     */
    public String getTemplateInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Available Templates:\n");
        info.append("-------------------\n");
        
        if (templateCache.isEmpty()) {
            info.append("No templates available\n");
        } else {
            for (Map.Entry<AudioFormat, File> entry : templateCache.entrySet()) {
                File template = entry.getValue();
                info.append(String.format("%s: %s (%.1f KB)\n", 
                        entry.getKey(),
                        template.getName(),
                        template.length() / 1024.0));
            }
        }
        
        return info.toString();
    }
    
    /**
     * Checks if templates are available for all formats.
     * 
     * @return true if all formats have templates
     */
    public boolean hasAllFormats() {
        for (AudioFormat format : AudioFormat.values()) {
            if (!templateCache.containsKey(format)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets the formats that have available templates.
     * 
     * @return Array of available formats
     */
    public AudioFormat[] getAvailableFormats() {
        return templateCache.keySet().toArray(new AudioFormat[0]);
    }
}