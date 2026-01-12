# MP3Org - Music Collection Manager

A powerful application for organizing and managing large music collections. MP3Org helps you find and remove duplicate songs, organize files into structured folders, and edit metadata for better collection management.

**Now available as a modern web application!** The project is being migrated from JavaFX desktop to Spring Boot + React web interface.

## What's New (January 2026)

- **Web UI Migration** - Modern React frontend with Spring Boot REST API (Issue #69)
- **SQLite Database** - Migrated from Apache Derby for better portability (Issue #72)
- **Java 21 LTS** - Standardized on Java 21 for long-term support
- **Real-time Scanning** - WebSocket-based progress updates during directory scans
- **Comprehensive API** - Full REST API for music file management

## Features

### Smart Duplicate Detection
- **Advanced fuzzy matching** algorithms find duplicates even when metadata differs slightly
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
- **Batch metadata updates** for consistency
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

### Using Version Managers

```bash
# With SDKMAN
sdk env

# With jenv
jenv local
```

### Running the Web Application

```bash
# Clone the repository
git clone https://github.com/richardahasting/MP3Org.git
cd MP3Org

# Start the backend (port 9090)
./gradlew bootRun

# In another terminal, start the frontend (port 5173)
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173 in your browser.

### Building from Source

```bash
# Build backend
./gradlew build

# Build frontend for production
cd frontend
npm run build
```

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
│   ├── service/                      # Business logic
│   ├── dto/                          # Data transfer objects
│   ├── model/                        # Domain models
│   └── util/                         # Core utilities
├── frontend/
│   ├── src/
│   │   ├── components/               # React components
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
| `/api/v1/music/search` | GET | Search files |
| `/api/v1/music/count` | GET | Get total count |
| `/api/v1/music/bulk` | PUT | Bulk update |

### Scanning (`/api/v1/scanning`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/scanning/start` | POST | Start scan (returns sessionId) |
| `/api/v1/scanning/status/{id}` | GET | Get scan status |
| `/api/v1/scanning/cancel/{id}` | POST | Cancel scan |
| `/api/v1/scanning/browse` | GET | Browse server directories |

WebSocket: `/ws` with `/topic/scanning/{sessionId}` for real-time progress

## Testing

```bash
# Run backend tests
./gradlew test

# Run frontend E2E tests
cd frontend
npm run build
npm run preview &
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

## Contributing

Contributions are welcome! Please see our open issues:

- **Issue #69** - Web UI Migration (in progress)
- **Issue #70** - API Test Suite
- **Issue #72** - SQLite Migration (completed)

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
- [ ] Duplicate Detection UI (Phase 3)
- [ ] Metadata Editor enhancements (Phase 4)
- [ ] File Organization UI (Phase 5)
- [ ] Configuration UI (Phase 6)
- [ ] UI Polish (Phase 7)

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **JAudioTagger** library for metadata extraction
- **SQLite** for embedded database functionality
- **Spring Boot** for the backend framework
- **React** for the modern user interface

---

**Made with care for music lovers who want organized collections**

*Star this repository if MP3Org helps you organize your music collection!*
