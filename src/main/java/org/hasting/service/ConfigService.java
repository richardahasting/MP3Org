package org.hasting.service;

import org.hasting.util.DatabaseConfig;
import org.hasting.util.DatabaseProfile;
import org.hasting.util.DatabaseProfileManager;
import org.hasting.util.FuzzySearchConfig;
import org.springframework.stereotype.Service;
import com.log4rich.core.Logger;
import com.log4rich.Log4Rich;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service layer for configuration operations.
 * Wraps DatabaseConfig, DatabaseProfileManager, and FuzzySearchConfig.
 *
 * Part of Issue #69 - Web UI Migration
 */
@Service
public class ConfigService {

    private static final Logger logger = Log4Rich.getLogger(ConfigService.class);

    private final DatabaseConfig databaseConfig;
    private final DatabaseProfileManager profileManager;

    public ConfigService() {
        this.databaseConfig = DatabaseConfig.getInstance();
        this.profileManager = DatabaseProfileManager.getInstance();
    }

    // ============= Fuzzy Search Configuration =============

    /**
     * Gets the current fuzzy search configuration from the active profile.
     */
    public FuzzySearchConfigDTO getFuzzySearchConfig() {
        DatabaseProfile activeProfile = databaseConfig.getActiveProfile();
        FuzzySearchConfig config = activeProfile != null ?
            activeProfile.getFuzzySearchConfig() :
            new FuzzySearchConfig();

        return toFuzzySearchConfigDTO(config);
    }

    /**
     * Updates the fuzzy search configuration for the active profile.
     */
    public FuzzySearchConfigDTO updateFuzzySearchConfig(FuzzySearchConfigDTO dto) {
        DatabaseProfile activeProfile = databaseConfig.getActiveProfile();
        if (activeProfile == null) {
            throw new IllegalStateException("No active profile");
        }

        FuzzySearchConfig config = activeProfile.getFuzzySearchConfig();

        // Update all fields
        if (dto.configName() != null) config.setConfigName(dto.configName());
        config.setTitleSimilarityThreshold(dto.titleSimilarityThreshold());
        config.setArtistSimilarityThreshold(dto.artistSimilarityThreshold());
        config.setAlbumSimilarityThreshold(dto.albumSimilarityThreshold());
        config.setDurationToleranceSeconds(dto.durationToleranceSeconds());
        config.setDurationTolerancePercent(dto.durationTolerancePercent());
        config.setIgnoreCaseDifferences(dto.ignoreCaseDifferences());
        config.setIgnorePunctuation(dto.ignorePunctuation());
        config.setWordOrderSensitive(dto.wordOrderSensitive());
        config.setTrackNumberMustMatch(dto.trackNumberMustMatch());
        config.setIgnoreMissingTrackNumber(dto.ignoreMissingTrackNumber());
        config.setIgnoreArtistPrefixes(dto.ignoreArtistPrefixes());
        config.setIgnoreFeaturing(dto.ignoreFeaturing());
        config.setIgnoreAlbumEditions(dto.ignoreAlbumEditions());
        config.setMinimumFieldsToMatch(dto.minimumFieldsToMatch());
        config.setBitrateToleranceKbps(dto.bitrateToleranceKbps());

        // Save the profile
        profileManager.updateProfile(activeProfile);

        logger.info("Updated fuzzy search configuration: {}", config.getConfigName());
        return toFuzzySearchConfigDTO(config);
    }

    /**
     * Resets fuzzy search configuration to defaults.
     */
    public FuzzySearchConfigDTO resetFuzzySearchConfig() {
        DatabaseProfile activeProfile = databaseConfig.getActiveProfile();
        if (activeProfile == null) {
            throw new IllegalStateException("No active profile");
        }

        FuzzySearchConfig config = new FuzzySearchConfig();
        activeProfile.setFuzzySearchConfig(config);
        profileManager.updateProfile(activeProfile);

        logger.info("Reset fuzzy search configuration to defaults");
        return toFuzzySearchConfigDTO(config);
    }

    /**
     * Applies a preset configuration (Strict, Balanced, or Lenient).
     */
    public FuzzySearchConfigDTO applyPreset(String presetName) {
        DatabaseProfile activeProfile = databaseConfig.getActiveProfile();
        if (activeProfile == null) {
            throw new IllegalStateException("No active profile");
        }

        FuzzySearchConfig config = switch (presetName.toLowerCase()) {
            case "strict" -> FuzzySearchConfig.createStrictConfig();
            case "lenient" -> FuzzySearchConfig.createLenientConfig();
            default -> FuzzySearchConfig.createBalancedConfig();
        };

        activeProfile.setFuzzySearchConfig(config);
        profileManager.updateProfile(activeProfile);

        logger.info("Applied fuzzy search preset: {}", presetName);
        return toFuzzySearchConfigDTO(config);
    }

    // ============= File Types Configuration =============

    /**
     * Gets all supported file types and their enabled status.
     */
    public FileTypesDTO getFileTypes() {
        String[] allTypes = DatabaseConfig.getAllSupportedTypes();
        Set<String> enabledTypes = databaseConfig.getEnabledFileTypes();

        return new FileTypesDTO(allTypes, enabledTypes.toArray(new String[0]));
    }

    /**
     * Updates the enabled file types.
     */
    public FileTypesDTO updateFileTypes(Set<String> enabledTypes) {
        databaseConfig.setEnabledFileTypes(enabledTypes);
        logger.info("Updated enabled file types: {}", enabledTypes);
        return getFileTypes();
    }

    // ============= Database Profiles =============

    /**
     * Gets all database profiles.
     */
    public List<DatabaseProfileDTO> getAllProfiles() {
        return profileManager.getAllProfiles().stream()
            .map(this::toProfileDTO)
            .toList();
    }

    /**
     * Gets the active profile.
     */
    public DatabaseProfileDTO getActiveProfile() {
        DatabaseProfile profile = databaseConfig.getActiveProfile();
        return profile != null ? toProfileDTO(profile) : null;
    }

    /**
     * Creates a new profile.
     */
    public DatabaseProfileDTO createProfile(String name, String description, String databasePath) {
        DatabaseProfile profile = new DatabaseProfile(null, name, databasePath);
        profile.setDescription(description);
        profileManager.addProfile(profile);
        logger.info("Created new profile: {}", name);
        return toProfileDTO(profile);
    }

    /**
     * Updates an existing profile.
     */
    public DatabaseProfileDTO updateProfile(String profileId, String name, String description, String databasePath) {
        DatabaseProfile profile = profileManager.getProfile(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found: " + profileId);
        }

        profile.setName(name);
        profile.setDescription(description);
        profile.setDatabasePath(databasePath);
        profileManager.updateProfile(profile);

        logger.info("Updated profile: {}", name);
        return toProfileDTO(profile);
    }

    /**
     * Deletes a profile.
     */
    public boolean deleteProfile(String profileId) {
        if (DatabaseProfile.DEFAULT_PROFILE_ID.equals(profileId)) {
            throw new IllegalArgumentException("Cannot delete default profile");
        }

        boolean deleted = profileManager.removeProfile(profileId);
        if (deleted) {
            logger.info("Deleted profile: {}", profileId);
        }
        return deleted;
    }

    /**
     * Switches to a different profile.
     */
    public DatabaseProfileDTO switchProfile(String profileId) {
        boolean success = databaseConfig.switchToProfile(profileId);
        if (!success) {
            throw new IllegalArgumentException("Failed to switch to profile: " + profileId);
        }

        logger.info("Switched to profile: {}", profileId);
        return getActiveProfile();
    }

    /**
     * Duplicates an existing profile.
     */
    public DatabaseProfileDTO duplicateProfile(String profileId, String newName) {
        DatabaseProfile original = profileManager.getProfile(profileId);
        if (original == null) {
            throw new IllegalArgumentException("Profile not found: " + profileId);
        }

        DatabaseProfile duplicate = new DatabaseProfile(null, newName, original.getDatabasePath());
        duplicate.setDescription(original.getDescription());
        duplicate.setEnabledFileTypes(original.getEnabledFileTypes());
        duplicate.setFuzzySearchConfig(original.getFuzzySearchConfig().copy(newName + " Config"));

        profileManager.addProfile(duplicate);
        logger.info("Duplicated profile {} as {}", original.getName(), newName);
        return toProfileDTO(duplicate);
    }

    // ============= Database Info =============

    /**
     * Gets the current database path.
     */
    public String getDatabasePath() {
        return databaseConfig.getDatabasePath();
    }

    /**
     * Gets database configuration info.
     */
    public DatabaseInfoDTO getDatabaseInfo() {
        return new DatabaseInfoDTO(
            databaseConfig.getDatabasePath(),
            databaseConfig.getJdbcUrl(),
            databaseConfig.getJdbcDriver()
        );
    }

    // ============= DTO Converters =============

    private FuzzySearchConfigDTO toFuzzySearchConfigDTO(FuzzySearchConfig config) {
        return new FuzzySearchConfigDTO(
            config.getConfigName(),
            config.getTitleSimilarityThreshold(),
            config.getArtistSimilarityThreshold(),
            config.getAlbumSimilarityThreshold(),
            config.getDurationToleranceSeconds(),
            config.getDurationTolerancePercent(),
            config.isIgnoreCaseDifferences(),
            config.isIgnorePunctuation(),
            config.isWordOrderSensitive(),
            config.isTrackNumberMustMatch(),
            config.isIgnoreMissingTrackNumber(),
            config.isIgnoreArtistPrefixes(),
            config.isIgnoreFeaturing(),
            config.isIgnoreAlbumEditions(),
            config.getMinimumFieldsToMatch(),
            config.getBitrateToleranceKbps()
        );
    }

    private DatabaseProfileDTO toProfileDTO(DatabaseProfile profile) {
        DatabaseProfile activeProfile = databaseConfig.getActiveProfile();
        boolean isActive = activeProfile != null && activeProfile.getId().equals(profile.getId());

        return new DatabaseProfileDTO(
            profile.getId(),
            profile.getName(),
            profile.getDescription(),
            profile.getDatabasePath(),
            profile.getEnabledFileTypes().toArray(new String[0]),
            profile.getCreatedDate() != null ? profile.getCreatedDate().toString() : null,
            profile.getLastUsedDate() != null ? profile.getLastUsedDate().toString() : null,
            isActive
        );
    }

    // ============= DTOs =============

    public record FuzzySearchConfigDTO(
        String configName,
        double titleSimilarityThreshold,
        double artistSimilarityThreshold,
        double albumSimilarityThreshold,
        int durationToleranceSeconds,
        double durationTolerancePercent,
        boolean ignoreCaseDifferences,
        boolean ignorePunctuation,
        boolean wordOrderSensitive,
        boolean trackNumberMustMatch,
        boolean ignoreMissingTrackNumber,
        boolean ignoreArtistPrefixes,
        boolean ignoreFeaturing,
        boolean ignoreAlbumEditions,
        int minimumFieldsToMatch,
        int bitrateToleranceKbps
    ) {}

    public record FileTypesDTO(
        String[] allTypes,
        String[] enabledTypes
    ) {}

    public record DatabaseProfileDTO(
        String id,
        String name,
        String description,
        String databasePath,
        String[] enabledFileTypes,
        String createdDate,
        String lastUsedDate,
        boolean active
    ) {}

    public record DatabaseInfoDTO(
        String databasePath,
        String jdbcUrl,
        String jdbcDriver
    ) {}
}
