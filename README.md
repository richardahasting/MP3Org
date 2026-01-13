# MP3Org - Music Collection Manager

A powerful application for organizing and managing large music collections. MP3Org helps you find and remove duplicate songs, organize files into structured folders, and edit metadata for better collection management.

**Now available as a modern web application!** The project has been migrated from JavaFX desktop to Spring Boot + React web interface.

## What's New (January 2026)

### Latest Updates
- **Directory-Based Duplicate Resolution** - Group duplicates by directory and resolve entire folder conflicts at once (Issue #92)
- **Smart Bulk Edit** - Combobox inputs with suggestions showing values and frequency counts from selected files
- **Audio Fingerprinting** - Optional Chromaprint integration for acoustic duplicate detection
- **Contextual Help System** - Built-in documentation with OS-specific setup instructions

### Previous Updates
- **Web UI Migration** - Modern React frontend with Spring Boot REST API (Issue #69)
- **SQLite Database** - Migrated from Apache Derby for better portability (Issue #72)
- **Java 21 LTS** - Standardized on Java 21 for long-term support
- **Real-time Scanning** - WebSocket-based progress updates during directory scans
- **Comprehensive API** - Full REST API for music file management

## Features

### Smart Duplicate Detection
- **Advanced fuzzy matching** algorithms find duplicates even when metadata differs slightly
- **Audio fingerprinting** (optional) uses Chromaprint for acoustic similarity detection
- **Directory-based grouping** to resolve entire folder conflicts at once
- **Configurable similarity thresholds** for title, artist, album, and duration
- **Text normalization** handles artist prefixes, featuring artists, album editions
- **Multiple detection presets**: Strict, Balanced, Lenient, or Custom settings

### File Organization
- **Automatic folder structure** creation: Artist/Album/Track-Title format
- **Preserves original files** while creating organized copies
- **Batch processing** for large collections
- **File type filtering** for selective organization

### Metadata Management
- **Search and edit** song information across your collection
- **Smart bulk editing** with combobox suggestions showing distinct values from selected files
- **Frequency-based pre-selection** automatically selects the most common value
- **Year field support** in bulk edit alongside artist, album, and genre
- **Automatic metadata extraction** from file tags
- **Support for all common fields**: title, artist, album, genre, year, track number

### Multiple Database Profiles
- **Separate collections** for different music types (personal, work, classical, etc.)
- **Profile-specific settings** for optimal duplicate detection
- **Easy switching** between collections

### Format Support
Supports all major audio formats:
- **Lossless**: FLAC, AIFF, APE, WAV
- **Compressed**: MP3, M4A (AAC), OGG Vorbis, OPUS, WMA

## Quick Start

### Prerequisites
- **Java 21** (LTS) - download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/)
- **Node.js 18+** (for frontend development)
- **Chromaprint** (optional, for audio fingerprinting)

### Installing Chromaprint (Optional)

Audio fingerprinting requires the Chromaprint library:

**macOS (Homebrew):**
```bash
brew install chromaprint
```

**Windows:**
1. Download from https://acoustid.org/chromaprint
2. Extract and add `fpcalc.exe` to your PATH

**Linux (Ubuntu/Debian):**
```bash
sudo apt install libchromaprint-tools
```

**Linux (Fedora/RHEL):**
```bash
sudo dnf install chromaprint-tools
```

**Linux (Arch):**
```bash
sudo pacman -S chromaprint
```

### Using Version Managers

```bash
# With SDKMAN
sdk env

# With jenv
jenv local
```

### Running the Web Application

**macOS / Linux:**
```bash
# Clone the repository
git clone https://github.com/richardahasting/MP3Org.git
cd MP3Org

# Start the backend (port 9090)
./gradle21 bootRun

# In another terminal, start the frontend (port 5173)
cd frontend
npm install
npm run dev
```

**Windows (Command Prompt or PowerShell):**
```cmd
git clone https://github.com/richardahasting/MP3Org.git
cd MP3Org

REM Start the backend
gradle21.cmd bootRun

REM In another terminal, start the frontend
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173 in your browser.

### Building from Source

**macOS / Linux:**
```bash
./gradle21 build
```

**Windows:**
```cmd
gradle21.cmd build
```

**Frontend (all platforms):**
```bash
cd frontend
npm run build
```

### How Java 21 Auto-Detection Works

The `gradle21` scripts automatically locate Java 21 on your system:

| Platform | Locations Checked |
|----------|-------------------|
| **macOS** | `/usr/libexec/java_home -v 21` (system utility) |
| **Linux** | SDKMAN (`~/.sdkman`), `/usr/lib/jvm/java-21-*`, `/opt/java/jdk-21*`, Homebrew |
| **Windows** | Program Files (Adoptium, Oracle, Microsoft, Amazon, Zulu, BellSoft) |

If Java 21 is already your default `JAVA_HOME`, you can use `./gradlew` (or `gradlew.bat`) directly.

## Architecture

MP3Org uses a modern full-stack architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐ ┌───────┐ │
│  │Duplicate│ │ Metadata │ │ Import │ │ Organize │ │Config │ │
│  │ Manager │ │  Editor  │ │  View  │ │   View   │ │ View  │ │
│  └────┬────┘ └────┬─────┘ └───┬────┘ └────┬─────┘ └───┬───┘ │
└───────┼──────────┼─────────────┼───────────┼──────────┼─────┘
        │          │             │           │          │
        └──────────┴─────────────┴───────────┴──────────┘
                              │ HTTP/WebSocket
        ┌─────────────────────┴─────────────────────────┐
        │              Spring Boot REST API              │
        │  ┌─────────────────────────────────────────┐  │
        │  │             Service Layer               │  │
        │  │  MusicFileService, ScanningService,     │  │
        │  │  DuplicateService, OrganizationService  │  │
        │  └────────────────────┬────────────────────┘  │
        └───────────────────────┼───────────────────────┘
                                │
                        ┌───────┴───────┐
                        │ SQLite Database│
                        └───────────────┘
```

### Project Structure

```
MP3Org/
├── src/main/java/org/hasting/
│   ├── MP3OrgWebApplication.java    # Spring Boot entry point
│   ├── controller/                   # REST API endpoints
│   │   ├── MusicFileController.java
│   │   ├── DuplicateController.java
│   │   └── ScanningController.java
│   ├── service/                      # Business logic
│   │   ├── MusicFileService.java
│   │   ├── DuplicateService.java
│   │   └── ScanningService.java
│   ├── dto/                          # Data transfer objects
│   │   ├── MusicFileDTO.java
│   │   ├── DirectoryConflictDTO.java
│   │   └── DuplicatePairDTO.java
│   ├── model/                        # Domain models
│   └── util/                         # Core utilities
├── frontend/
│   ├── src/
│   │   ├── components/               # React components
│   │   │   ├── duplicates/           # Duplicate manager UI
│   │   │   ├── metadata/             # Metadata editor UI
│   │   │   └── common/               # Shared components
│   │   ├── hooks/                    # Custom React hooks
│   │   ├── api/                      # API client
│   │   └── types/                    # TypeScript types
│   └── tests/                        # Puppeteer E2E tests
└── build.gradle.kts                  # Gradle build configuration
```

## REST API

### Music Files (`/api/v1/music`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/music` | GET | List all (paginated) |
| `/api/v1/music/{id}` | GET | Get by ID |
| `/api/v1/music/{id}` | PUT | Update metadata |
| `/api/v1/music/{id}` | DELETE | Delete file |
| `/api/v1/music/{id}/stream` | GET | Stream audio file |
| `/api/v1/music/search` | GET | Search files |
| `/api/v1/music/count` | GET | Get total count |
| `/api/v1/music/bulk` | PUT | Bulk update (artist, album, genre, year) |

### Duplicates (`/api/v1/duplicates`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/duplicates` | GET | Get duplicate groups |
| `/api/v1/duplicates/by-directory` | GET | Get directory conflicts |
| `/api/v1/duplicates/resolve-directory/preview` | POST | Preview directory resolution |
| `/api/v1/duplicates/resolve-directory/execute` | POST | Execute directory resolution |

### Scanning (`/api/v1/scanning`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/scanning/start` | POST | Start scan (returns sessionId) |
| `/api/v1/scanning/status/{id}` | GET | Get scan status |
| `/api/v1/scanning/cancel/{id}` | POST | Cancel scan |
| `/api/v1/scanning/browse` | GET | Browse server directories |

WebSocket: `/ws` with `/topic/scanning/{sessionId}` for real-time progress

## Testing

**macOS / Linux:**
```bash
# Run backend tests
./gradle21 test

# Run frontend E2E tests
cd frontend
npm run build
npm run preview &
npm test
```

**Windows:**
```cmd
REM Run backend tests
gradle21.cmd test

REM Run frontend E2E tests
cd frontend
npm run build
npm run preview
REM In another terminal:
npm test
```

## Configuration

### Duplicate Detection Settings

| Setting | Description | Recommended |
|---------|-------------|-------------|
| **Title Similarity** | How closely song titles must match | 85% |
| **Artist Similarity** | How closely artist names must match | 90% |
| **Album Similarity** | How closely album names must match | 85% |
| **Duration Tolerance** | Maximum time difference allowed | 10 seconds |
| **Audio Fingerprinting** | Use acoustic similarity (requires Chromaprint) | Optional |

### Audio Fingerprinting

When enabled, MP3Org uses Chromaprint to generate acoustic fingerprints of your music files. This allows detection of duplicates even when:
- Metadata is completely different
- Files are different encodings of the same recording
- Tags have been modified or corrupted

**Requirements:**
- Chromaprint must be installed (see installation instructions above)
- `fpcalc` executable must be in your system PATH
- Initial fingerprint generation may take time for large collections

## Contributing

Contributions are welcome! Please see our open issues:

- **Issue #69** - Web UI Migration (completed)
- **Issue #70** - API Test Suite
- **Issue #72** - SQLite Migration (completed)
- **Issue #92** - Directory-Based Duplicate Resolution (completed)

### Development Setup

1. **Fork and clone** the repository
2. **Set Java version**: `sdk env` or `jenv local`
3. **Run tests**: `./gradlew test`
4. **Create a feature branch**: `git checkout -b feature/issue-XX-description`
5. **Submit a pull request**

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4, SQLite, WebSocket
- **Frontend**: React 18, TypeScript, Vite
- **Build**: Gradle 8.12, npm
- **Testing**: JUnit 5, Puppeteer
- **Audio**: JAudioTagger, Chromaprint (optional)

## Privacy and Security

MP3Org prioritizes your privacy:
- **Local processing only** - no data sent to external servers
- **No telemetry or tracking**
- **Local database storage** using SQLite
- **Open source** - audit the code yourself

## Roadmap

- [x] Spring Boot REST API (Phase 1)
- [x] React frontend foundation (Phase 1)
- [x] SQLite database migration
- [x] Import/Scanning with WebSocket progress (Phase 2)
- [x] Duplicate Detection UI (Phase 3)
- [x] Directory-based duplicate resolution
- [x] Metadata Editor with smart bulk edit (Phase 4)
- [x] Audio fingerprinting support
- [x] Contextual help system
- [ ] File Organization UI (Phase 5)
- [ ] Configuration UI improvements (Phase 6)
- [ ] UI Polish and accessibility (Phase 7)

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **JAudioTagger** library for metadata extraction
- **Chromaprint** for audio fingerprinting
- **SQLite** for embedded database functionality
- **Spring Boot** for the backend framework
- **React** for the modern user interface

---

**Made with care for music lovers who want organized collections**

*Star this repository if MP3Org helps you organize your music collection!*
