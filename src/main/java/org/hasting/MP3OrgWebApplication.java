package org.hasting;

import org.hasting.service.FingerprintService;
import org.hasting.util.DatabaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Spring Boot application entry point for MP3Org web interface.
 *
 * This replaces the JavaFX-based MP3OrgApplication, providing a REST API
 * and web frontend while preserving all existing backend functionality.
 *
 * Part of Issue #69 - Web UI Migration
 */
@SpringBootApplication
public class MP3OrgWebApplication {

    @Autowired
    private FingerprintService fingerprintService;

    public static void main(String[] args) {
        SpringApplication.run(MP3OrgWebApplication.class, args);
    }

    /**
     * Initialize the database when the application is ready.
     * This reuses the existing DatabaseManager infrastructure.
     *
     * Uses SQLite as the database backend (Issue #72 migration from Derby).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // Use basic initialization - SQLite doesn't have lock issues like Derby
            DatabaseManager.initialize();
            System.out.println("MP3Org Web Application started successfully");
            System.out.println("Database initialized: " + DatabaseManager.getMusicFileCount() + " music files");
            System.out.println("Database location: " + DatabaseManager.getConfig().getDatabasePath());

            // Check fingerprint capabilities
            if (fingerprintService.isFpcalcAvailable()) {
                long withFingerprints = fingerprintService.getFilesWithFingerprintsCount();
                long withoutFingerprints = fingerprintService.getFilesWithoutFingerprintsCount();
                System.out.println("Audio fingerprinting: ENABLED (fpcalc available)");
                System.out.println("  Files with fingerprints: " + withFingerprints);
                System.out.println("  Files without fingerprints: " + withoutFingerprints);
            } else {
                System.out.println("Audio fingerprinting: DISABLED (fpcalc not found)");
                System.out.println("  Install with: brew install chromaprint");
            }
        } catch (Exception e) {
            System.err.println("Warning: Database initialization failed: " + e.getMessage());
            System.err.println("Application will continue but database features may be limited");
            e.printStackTrace();
        }
    }
}
