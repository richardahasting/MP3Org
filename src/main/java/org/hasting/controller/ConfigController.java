package org.hasting.controller;

import org.hasting.service.ConfigService;
import org.hasting.service.ConfigService.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for configuration operations.
 * Provides endpoints for fuzzy search settings, file types, and database profiles.
 *
 * Part of Issue #69 - Web UI Migration
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    // ============= Fuzzy Search Configuration =============

    /**
     * GET /api/v1/config/fuzzy-search - Get fuzzy search configuration.
     */
    @GetMapping("/fuzzy-search")
    public FuzzySearchConfigDTO getFuzzySearchConfig() {
        return configService.getFuzzySearchConfig();
    }

    /**
     * PUT /api/v1/config/fuzzy-search - Update fuzzy search configuration.
     */
    @PutMapping("/fuzzy-search")
    public FuzzySearchConfigDTO updateFuzzySearchConfig(@RequestBody FuzzySearchConfigDTO config) {
        return configService.updateFuzzySearchConfig(config);
    }

    /**
     * POST /api/v1/config/fuzzy-search/reset - Reset fuzzy search to defaults.
     */
    @PostMapping("/fuzzy-search/reset")
    public FuzzySearchConfigDTO resetFuzzySearchConfig() {
        return configService.resetFuzzySearchConfig();
    }

    /**
     * POST /api/v1/config/fuzzy-search/preset - Apply a preset configuration.
     */
    @PostMapping("/fuzzy-search/preset")
    public FuzzySearchConfigDTO applyPreset(@RequestBody Map<String, String> request) {
        String presetName = request.get("preset");
        if (presetName == null || presetName.isBlank()) {
            throw new IllegalArgumentException("Preset name is required");
        }
        return configService.applyPreset(presetName);
    }

    // ============= File Types Configuration =============

    /**
     * GET /api/v1/config/file-types - Get file type configuration.
     */
    @GetMapping("/file-types")
    public FileTypesDTO getFileTypes() {
        return configService.getFileTypes();
    }

    /**
     * PUT /api/v1/config/file-types - Update enabled file types.
     */
    @PutMapping("/file-types")
    public FileTypesDTO updateFileTypes(@RequestBody FileTypesUpdateRequest request) {
        return configService.updateFileTypes(request.enabledTypes());
    }

    // ============= Database Profiles =============

    /**
     * GET /api/v1/config/profiles - Get all profiles.
     */
    @GetMapping("/profiles")
    public List<DatabaseProfileDTO> getAllProfiles() {
        return configService.getAllProfiles();
    }

    /**
     * GET /api/v1/config/profiles/active - Get active profile.
     */
    @GetMapping("/profiles/active")
    public ResponseEntity<DatabaseProfileDTO> getActiveProfile() {
        DatabaseProfileDTO profile = configService.getActiveProfile();
        return profile != null ? ResponseEntity.ok(profile) : ResponseEntity.notFound().build();
    }

    /**
     * POST /api/v1/config/profiles - Create a new profile.
     */
    @PostMapping("/profiles")
    public DatabaseProfileDTO createProfile(@RequestBody CreateProfileRequest request) {
        return configService.createProfile(request.name(), request.description(), request.databasePath());
    }

    /**
     * PUT /api/v1/config/profiles/{id} - Update a profile.
     */
    @PutMapping("/profiles/{id}")
    public DatabaseProfileDTO updateProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest request
    ) {
        return configService.updateProfile(id, request.name(), request.description(), request.databasePath());
    }

    /**
     * DELETE /api/v1/config/profiles/{id} - Delete a profile.
     */
    @DeleteMapping("/profiles/{id}")
    public ResponseEntity<Map<String, Object>> deleteProfile(@PathVariable String id) {
        boolean deleted = configService.deleteProfile(id);
        return ResponseEntity.ok(Map.of("success", deleted));
    }

    /**
     * POST /api/v1/config/profiles/{id}/activate - Switch to a profile.
     */
    @PostMapping("/profiles/{id}/activate")
    public DatabaseProfileDTO activateProfile(@PathVariable String id) {
        return configService.switchProfile(id);
    }

    /**
     * POST /api/v1/config/profiles/{id}/duplicate - Duplicate a profile.
     */
    @PostMapping("/profiles/{id}/duplicate")
    public DatabaseProfileDTO duplicateProfile(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String newName = request.get("name");
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("New profile name is required");
        }
        return configService.duplicateProfile(id, newName);
    }

    // ============= Database Info =============

    /**
     * GET /api/v1/config/database - Get database information.
     */
    @GetMapping("/database")
    public DatabaseInfoDTO getDatabaseInfo() {
        return configService.getDatabaseInfo();
    }

    // ============= Network Info =============

    /**
     * GET /api/v1/config/network - Get network information for sharing.
     */
    @GetMapping("/network")
    public Map<String, Object> getNetworkInfo() {
        List<String> addresses = new ArrayList<>();
        String primaryIp = null;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    // Only include IPv4 addresses
                    if (addr.getHostAddress().contains(":")) continue; // Skip IPv6

                    String ip = addr.getHostAddress();
                    addresses.add(ip);

                    // Prefer addresses that look like local network IPs
                    if (primaryIp == null ||
                        ip.startsWith("192.168.") ||
                        ip.startsWith("10.") ||
                        ip.startsWith("172.")) {
                        primaryIp = ip;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to localhost
        }

        return Map.of(
            "primaryIp", primaryIp != null ? primaryIp : "localhost",
            "allAddresses", addresses,
            "port", 9090
        );
    }

    // ============= Request DTOs =============

    public record FileTypesUpdateRequest(Set<String> enabledTypes) {}

    public record CreateProfileRequest(String name, String description, String databasePath) {}

    public record UpdateProfileRequest(String name, String description, String databasePath) {}
}
