# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MP3Org is a Java-based music file organization tool that scans MP3 files, extracts metadata using JAudioTagger, and provides duplicate detection and file organization capabilities. The application uses a Swing GUI and Apache Derby embedded database for persistence.

## Key Commands

### Build and Run
```bash
./gradlew build          # Build the project
./gradlew run            # Run the application
./gradlew jar            # Create executable JAR
```

### Testing
```bash
./gradlew test           # Run all tests
./gradlew test --tests "ClassName.methodName"  # Run specific test
```

### Development
```bash
./gradlew clean          # Clean build artifacts
./gradlew compileJava    # Compile Java sources only
```

## Architecture

### Core Components

**Model Layer**
- `MusicFile` (src/main/java/org/hasting/model/): Core entity representing music files with metadata extraction, fuzzy matching, and duplicate detection algorithms. Contains sophisticated file comparison logic and artist directory organization.

**UI Layer**
- `MP3OrgMainFrame`: Main Swing application window
- `MusicFileEditor`: Interface for editing music file metadata
- `DirectorySelector`: Directory selection components
- `MusicFileComparisonDialog`: Compare similar music files

**Utility Layer**
- `DatabaseManager`: Handles Apache Derby database operations and connection management
- `MusicFileScanner`: Recursively scans directories for music files
- `MusicFileListUtils`: Utilities for working with collections of music files
- `StringUtils`: Fuzzy string matching algorithms for duplicate detection

### Database Schema
Uses Apache Derby embedded database with music_files table storing metadata. Schema defined in `src/main/resources/schema.sql` with indexes for efficient querying.

### Key Dependencies
- JAudioTagger 3.0.1: MP3 metadata extraction (lib/jaudiotagger-3.0.1.jar)
- Apache Derby: Embedded database
- Swing: GUI framework
- MigLayout: Layout manager

## Development Notes

### File Organization Algorithm
The application includes a sophisticated file organization system that:
- Groups artists into 8 directories based on alphabetical distribution
- Generates standardized paths: `startingPath/artistInitials/artistName/albumName/trackNumber-title.fileType`
- Handles artist name distribution to balance directory sizes

### Duplicate Detection
Uses fuzzy string matching with configurable thresholds:
- Title and artist fuzzy matching (>90% similarity)
- Duration comparison (within 5% tolerance)
- Bitrate filtering (prefers higher bitrate files)
- Track number validation

### Database Management
- Database files stored in `mp3org/` directory
- Automatic table creation on startup
- Unique constraints on file paths to prevent duplicates