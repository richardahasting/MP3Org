package org.hasting.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.hasting.util.logging.Logger;
import org.hasting.util.logging.MP3OrgLoggingManager;

/**
 * Represents a database profile configuration containing database location,
 * file type filters, and metadata about the database.
 */
public class DatabaseProfile {
    
    private static final Logger logger = MP3OrgLoggingManager.getLogger(DatabaseProfile.class);
    
    private String id;
    private String name;
    private String description;
    private String databasePath;
    private Set<String> enabledFileTypes;
    private LocalDateTime createdDate;
    private LocalDateTime lastUsedDate;
    private Map<String, String> metadata;
    private FuzzySearchConfig fuzzySearchConfig;
    
    // Default profile constants
    public static final String DEFAULT_PROFILE_ID = "default";
    public static final String DEFAULT_PROFILE_NAME = "Default Database";
    
    /**
     * Creates a new database profile.
     */
    public DatabaseProfile(String id, String name, String databasePath) {
        this.id = id != null ? id : generateId();
        this.name = name != null ? name : "Unnamed Profile";
        this.databasePath = databasePath;
        this.enabledFileTypes = new HashSet<>(Arrays.asList(DatabaseConfig.getAllSupportedTypes()));
        this.createdDate = LocalDateTime.now();
        this.lastUsedDate = LocalDateTime.now();
        this.metadata = new HashMap<>();
        this.description = "";
        this.fuzzySearchConfig = new FuzzySearchConfig();
    }
    
    /**
     * Creates a default database profile.
     */
    public static DatabaseProfile createDefault() {
        return new DatabaseProfile(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME, "mp3org");
    }
    
    /**
     * Generates a unique ID for the profile.
     */
    private String generateId() {
        return "profile_" + System.currentTimeMillis();
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }
    
    public String getDatabasePath() {
        return databasePath;
    }
    
    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }
    
    public Set<String> getEnabledFileTypes() {
        return new HashSet<>(enabledFileTypes);
    }
    
    public void setEnabledFileTypes(Set<String> enabledFileTypes) {
        this.enabledFileTypes = enabledFileTypes != null ? 
            new HashSet<>(enabledFileTypes) : 
            new HashSet<>(Arrays.asList(DatabaseConfig.getAllSupportedTypes()));
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDateTime getLastUsedDate() {
        return lastUsedDate;
    }
    
    public void setLastUsedDate(LocalDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }
    
    public void updateLastUsedDate() {
        this.lastUsedDate = LocalDateTime.now();
    }
    
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public void setMetadataValue(String key, String value) {
        if (key != null) {
            metadata.put(key, value);
        }
    }
    
    public String getMetadataValue(String key) {
        return metadata.get(key);
    }
    
    public FuzzySearchConfig getFuzzySearchConfig() {
        return fuzzySearchConfig;
    }
    
    public void setFuzzySearchConfig(FuzzySearchConfig fuzzySearchConfig) {
        this.fuzzySearchConfig = fuzzySearchConfig != null ? fuzzySearchConfig : new FuzzySearchConfig();
    }
    
    // Utility methods
    
    /**
     * Checks if a file type is enabled in this profile.
     */
    public boolean isFileTypeEnabled(String fileType) {
        return enabledFileTypes.contains(fileType.toLowerCase());
    }
    
    /**
     * Gets the JDBC URL for this profile's database.
     */
    public String getJdbcUrl() {
        String normalizedPath = normalizePath(databasePath);
        return "jdbc:derby:" + normalizedPath + ";create=true";
    }
    
    /**
     * Normalizes the database path.
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "mp3org";
        }
        
        path = path.trim();
        
        // If it's a relative path, make it relative to current working directory
        Path dbPath = Paths.get(path);
        if (!dbPath.isAbsolute()) {
            dbPath = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        
        // Ensure parent directories exist
        try {
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
        } catch (IOException e) {
            logger.warning("Could not create database directory: {}", e.getMessage());
        }
        
        return dbPath.toString();
    }
    
    /**
     * Gets a formatted display name for the profile.
     */
    public String getDisplayName() {
        if (description != null && !description.trim().isEmpty()) {
            return name + " - " + description;
        }
        return name;
    }
    
    /**
     * Gets profile information as a formatted string.
     */
    public String getProfileInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Profile: ").append(name).append("\n");
        info.append("  ID: ").append(id).append("\n");
        if (description != null && !description.trim().isEmpty()) {
            info.append("  Description: ").append(description).append("\n");
        }
        info.append("  Database Path: ").append(databasePath).append("\n");
        info.append("  JDBC URL: ").append(getJdbcUrl()).append("\n");
        info.append("  Enabled File Types: ").append(
            enabledFileTypes.stream().sorted().collect(Collectors.joining(", "))).append("\n");
        info.append("  Created: ").append(createdDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        info.append("  Last Used: ").append(lastUsedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        if (fuzzySearchConfig != null) {
            info.append("  Fuzzy Search Config: ").append(fuzzySearchConfig.getConfigName()).append("\n");
        }
        
        if (!metadata.isEmpty()) {
            info.append("  Metadata:\n");
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                info.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return info.toString();
    }
    
    /**
     * Serializes the profile to Properties format.
     */
    public Properties toProperties() {
        Properties props = new Properties();
        
        props.setProperty("id", id);
        props.setProperty("name", name);
        props.setProperty("description", description);
        props.setProperty("databasePath", databasePath);
        props.setProperty("enabledFileTypes", 
            enabledFileTypes.stream().sorted().collect(Collectors.joining(",")));
        props.setProperty("createdDate", createdDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        props.setProperty("lastUsedDate", lastUsedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Add metadata
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            props.setProperty("metadata." + entry.getKey(), entry.getValue());
        }
        
        // Add fuzzy search configuration
        if (fuzzySearchConfig != null) {
            Properties fuzzyProps = fuzzySearchConfig.toProperties();
            for (String key : fuzzyProps.stringPropertyNames()) {
                props.setProperty("fuzzySearch." + key, fuzzyProps.getProperty(key));
            }
        }
        
        return props;
    }
    
    /**
     * Creates a profile from Properties.
     */
    public static DatabaseProfile fromProperties(Properties props) {
        String id = props.getProperty("id");
        String name = props.getProperty("name", "Unnamed Profile");
        String databasePath = props.getProperty("databasePath", "mp3org");
        
        DatabaseProfile profile = new DatabaseProfile(id, name, databasePath);
        
        profile.setDescription(props.getProperty("description", ""));
        
        // Parse enabled file types
        String fileTypesStr = props.getProperty("enabledFileTypes");
        if (fileTypesStr != null && !fileTypesStr.trim().isEmpty()) {
            Set<String> fileTypes = Arrays.stream(fileTypesStr.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(type -> Arrays.asList(DatabaseConfig.getAllSupportedTypes()).contains(type))
                    .collect(Collectors.toSet());
            profile.setEnabledFileTypes(fileTypes);
        }
        
        // Parse dates
        try {
            String createdStr = props.getProperty("createdDate");
            if (createdStr != null) {
                profile.setCreatedDate(LocalDateTime.parse(createdStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        } catch (Exception e) {
            // Keep default created date if parsing fails
        }
        
        try {
            String lastUsedStr = props.getProperty("lastUsedDate");
            if (lastUsedStr != null) {
                profile.setLastUsedDate(LocalDateTime.parse(lastUsedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        } catch (Exception e) {
            // Keep default last used date if parsing fails
        }
        
        // Parse metadata
        Map<String, String> metadata = new HashMap<>();
        Properties fuzzySearchProps = new Properties();
        
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("metadata.")) {
                String metaKey = key.substring("metadata.".length());
                metadata.put(metaKey, props.getProperty(key));
            } else if (key.startsWith("fuzzySearch.")) {
                String fuzzyKey = key.substring("fuzzySearch.".length());
                fuzzySearchProps.setProperty(fuzzyKey, props.getProperty(key));
            }
        }
        profile.setMetadata(metadata);
        
        // Parse fuzzy search configuration
        if (!fuzzySearchProps.isEmpty()) {
            try {
                FuzzySearchConfig fuzzyConfig = FuzzySearchConfig.fromProperties(fuzzySearchProps);
                profile.setFuzzySearchConfig(fuzzyConfig);
            } catch (Exception e) {
                logger.warning("Could not load fuzzy search configuration, using defaults: {}", e.getMessage());
                profile.setFuzzySearchConfig(new FuzzySearchConfig());
            }
        }
        
        return profile;
    }
    
    /**
     * Creates a copy of this profile with a new ID and name.
     */
    public DatabaseProfile copy(String newName) {
        DatabaseProfile copy = new DatabaseProfile(null, newName, this.databasePath);
        copy.setDescription(this.description);
        copy.setEnabledFileTypes(this.enabledFileTypes);
        copy.setMetadata(this.metadata);
        if (this.fuzzySearchConfig != null) {
            copy.setFuzzySearchConfig(this.fuzzySearchConfig.copy(this.fuzzySearchConfig.getConfigName()));
        }
        return copy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DatabaseProfile that = (DatabaseProfile) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
}