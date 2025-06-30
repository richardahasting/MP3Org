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

## Claude Code Automatic Behaviors

**REQUIRED**: Claude must automatically follow these behaviors in every session:

### 1. Developer Log Documentation
- **Record all user prompts** verbatim in developer-log.md
- **Document all work progress** including decisions, implementations, and fixes
- **Update the log throughout each session**, not just at the end
- **Include session statistics** and next steps for continuity

### 2. GitHub Issues Management
- **Create missing labels automatically** when needed for proper categorization
- **Apply comprehensive labeling** to all GitHub issues
- **Create GitHub issues for every fix/improvement** if one doesn't already exist
- **Reference related issues** and dependencies appropriately

### 3. Todo List Management
- **Use TodoWrite/TodoRead** for any multi-step or complex tasks
- **Update task statuses in real-time** as work progresses
- **Mark tasks complete immediately** upon finishing each item

### 4. Issue Tracking for All Changes
- **Check for existing issues** before making any bug fix or improvement
- **Create new GitHub issue** if none exists for the work being done
- **Properly describe the issue** with context and solution approach
- **Reference issues in commits** when implementing fixes
- **Close issues when validated** and working correctly

### 5. Code Quality Standards
- **Follow existing code conventions** and patterns in the codebase
- **Add comprehensive JavaDoc** to new methods and classes
- **Apply consistent formatting** following project standards
- **Run linting and type checking** before completing tasks (when available)

### 6. Testing and Validation
- **Run existing tests** to ensure changes don't break functionality
- **Create tests for new functionality** when appropriate
- **Validate all changes compile** and work as expected

These behaviors ensure complete traceability, proper project management, and comprehensive documentation of all development work.

**Note**: These universal behaviors are also maintained in CLAUDE-TEMPLATE.md for use in other projects.

## Development Philosophy

**CRITICAL**: Always read and follow the principles in `DEVELOPMENT-PHILOSOPHY.md` before starting any development work.

**Core Principle**: "Documentation is communication with our future selves, for you especially."

Key philosophy points:
- **Good code is self-documenting, self-explaining, and self-evident**
- **Code that is hard to read will be hard to maintain** - always avoid this
- **Write code that explains its purpose** with clear naming and obvious design
- **Create patterns that teach themselves** for rapid understanding
- **Make the codebase tell its own story** through logical organization

The best continuity mechanism is code that doesn't need explanation. Focus on clear communication through the code itself rather than complex processes.