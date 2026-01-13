# MP3Org User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [The Interface](#the-interface)
4. [Importing Music](#importing-music)
5. [Finding and Managing Duplicates](#finding-and-managing-duplicates)
6. [Editing Metadata](#editing-metadata)
7. [Organizing Your Collection](#organizing-your-collection)
8. [Configuration](#configuration)
9. [Audio Fingerprinting](#audio-fingerprinting)
10. [Troubleshooting](#troubleshooting)
11. [Tips and Best Practices](#tips-and-best-practices)

---

## Introduction

MP3Org is a powerful web-based music collection management tool that helps you organize large music libraries. It runs as a local web application with a Spring Boot backend and React frontend.

### Key Features
- **Smart Duplicate Detection**: Advanced fuzzy matching finds duplicates even with inconsistent metadata
- **Audio Fingerprinting**: Optional Chromaprint integration detects acoustic duplicates
- **Directory-Based Resolution**: Resolve entire folder conflicts at once
- **Metadata Editing**: Search and bulk-edit song information
- **File Organization**: Automatically organize into Artist/Album/Track structure
- **Real-time Progress**: WebSocket-based scanning with live updates
- **Modern Web UI**: Clean, responsive interface accessible via browser

### Supported Formats
- **Lossless**: FLAC, AIFF, APE, WAV
- **Compressed**: MP3, M4A (AAC), OGG Vorbis, OPUS, WMA

---

## Getting Started

### Prerequisites
- **Java 21** (LTS) - Required for the backend
- **Node.js 18+** - Required for the frontend
- **Chromaprint** - Optional, for audio fingerprinting

### Starting the Application

**Terminal 1 - Backend (port 9090):**
```bash
cd MP3Org
./gradle21 bootRun          # macOS/Linux
gradle21.cmd bootRun        # Windows
```

**Terminal 2 - Frontend (port 5173):**
```bash
cd MP3Org/frontend
npm install                 # First time only
npm run dev
```

**Access the Application:**
Open http://localhost:5173 in your web browser.

### Quick Start Workflow

1. **Import Music** - Scan directories to add files to the database
2. **Find Duplicates** - Detect and review potential duplicates
3. **Clean Up** - Delete unwanted duplicates or resolve by directory
4. **Edit Metadata** - Fix inconsistent tags
5. **Organize** - Create a clean folder structure (optional)

---

## The Interface

MP3Org has five main tabs:

### Duplicate Manager
Find and manage duplicate files in your collection. Features:
- Similarity-based duplicate groups
- Directory conflict view for folder-level resolution
- Auto-resolve with preview capability
- File comparison with quality indicators

### Metadata Editor
Search and edit music file information:
- Full-text search across all fields
- Individual file editing
- Bulk edit for multiple files (artist, album, genre, year)
- Smart combobox suggestions based on selected files

### Import
Add music files to your database:
- Browse and select directories
- Real-time scan progress via WebSocket
- Support for all major audio formats
- Automatic metadata extraction

### Organize
Create an organized copy of your music:
- Artist/Album/Track folder structure
- Preserves original files
- Handles filename conflicts

### Config
Application settings and help:
- Duplicate detection thresholds
- File type filters
- Audio fingerprinting toggle
- Built-in help documentation

---

## Importing Music

### Scanning Directories

1. Go to the **Import** tab
2. Click **Browse** to navigate server directories
3. Select the folder containing your music
4. Click **Start Scan**

### During the Scan
- Progress updates in real-time via WebSocket
- Shows files found, processed, and any errors
- Can be cancelled if needed

### What Gets Imported
- All audio files matching enabled formats
- Metadata is extracted automatically from file tags
- Files are stored in a local SQLite database

### Metadata Extracted
| Field | Description |
|-------|-------------|
| Title | Song name |
| Artist | Performing artist |
| Album | Album name |
| Genre | Musical style |
| Track Number | Position on album |
| Year | Release year |
| Duration | Length in seconds |
| Bitrate | Audio quality (kbps) |
| Sample Rate | Audio sample rate |
| File Size | Size in bytes |
| File Path | Location on disk |

---

## Finding and Managing Duplicates

### View Modes

The Duplicate Manager offers two ways to view duplicates:

#### By Similarity (Default)
Groups files that match based on metadata similarity:
- Title, artist, album fuzzy matching
- Duration tolerance checking
- Optional audio fingerprint matching

#### By Directory
Groups duplicates that exist in different folders:
- See folder conflicts at a glance
- Resolve entire directories at once
- Ideal for cleaning up album copies

### Reviewing Duplicates

1. Select a duplicate group from the left panel
2. Review all files in the group on the right
3. Compare quality indicators:
   - **Bitrate**: Higher is generally better
   - **File Size**: Larger often means better quality
   - **Format**: FLAC > MP3 for quality
   - **Metadata**: More complete tags are valuable

### Resolving Duplicates

**Delete Individual Files:**
- Click **Delete** on unwanted files
- Confirms before deletion
- Group auto-resolves when only 1 file remains

**Auto-Resolve:**
1. Click **Preview Auto-Resolution**
2. Review what would be deleted vs. kept
3. Exclude files you want to keep
4. Click **Execute** to perform deletion

**Directory Resolution:**
1. Switch to **By Directory** view
2. Select a directory conflict
3. Choose which directory to keep
4. Preview and execute resolution

### Safety Tips

- **Backup first**: File deletion is permanent
- **Preview always**: Use preview before auto-resolve
- **Check quality**: Higher bitrate isn't always better (re-encodes)
- **Open folder**: Use the folder icon to verify files before deletion

---

## Editing Metadata

### Searching

Use the search bar to find files:
- Searches title, artist, album, and genre
- Case-insensitive, partial matching
- Results appear in the table below

### Individual Editing

1. Click a file in the search results
2. Edit fields in the form panel:
   - Title, Artist, Album, Genre
   - Track Number, Year
3. Click **Save** to apply changes

### Bulk Editing

Edit multiple files at once:

1. Select files using checkboxes in the table
2. Click **Bulk Edit**
3. Choose fields to update:
   - **Artist**: Combobox shows values from selected files
   - **Album**: With frequency counts
   - **Genre**: Most common value pre-selected
   - **Year**: Numeric input
4. Click **Apply** to update all selected files

### Smart Suggestions

The bulk edit comboboxes show:
- All distinct values from selected files
- Frequency count for each value
- Most common value pre-selected
- Type to filter or enter new values

---

## Organizing Your Collection

### Organization Structure

MP3Org creates a hierarchical folder structure:
```
Destination/
├── Artist Name/
│   ├── Album Name/
│   │   ├── 01-Song Title.mp3
│   │   ├── 02-Another Song.mp3
│   │   └── ...
│   └── Another Album/
└── Another Artist/
```

### Organization Process

1. Go to the **Organize** tab
2. Select source (your imported files)
3. Choose destination folder
4. Click **Organize**
5. Monitor progress

### Features

- **Non-destructive**: Creates copies, originals remain
- **Smart naming**: Track numbers zero-padded (01, 02...)
- **Conflict handling**: Duplicate names numbered automatically
- **Invalid characters**: Replaced in file/folder names

---

## Configuration

### Duplicate Detection Settings

Access via **Config** tab:

#### Similarity Thresholds
| Setting | Default | Description |
|---------|---------|-------------|
| Title Similarity | 85% | How closely titles must match |
| Artist Similarity | 90% | How closely artists must match |
| Album Similarity | 85% | How closely albums must match |
| Duration Tolerance | 10 sec | Max time difference allowed |

#### Presets
| Preset | Best For |
|--------|----------|
| **Strict** | High-quality collections with consistent metadata |
| **Balanced** | General music collections (recommended) |
| **Lenient** | Collections with inconsistent metadata |
| **Custom** | Advanced users with specific needs |

#### Text Normalization
- **Ignore Case**: "Artist" = "artist"
- **Ignore Punctuation**: "Rock 'n' Roll" = "Rock n Roll"
- **Ignore Artist Prefixes**: "Beatles" = "The Beatles"
- **Ignore Featuring**: "Song" = "Song (feat. Artist)"
- **Ignore Album Editions**: "Album" = "Album - Deluxe Edition"

### File Type Filters

Enable/disable which formats to include:
- Changes affect scanning and duplicate detection
- Use **Select All** for comprehensive scanning

---

## Audio Fingerprinting

### What is Audio Fingerprinting?

Audio fingerprinting uses Chromaprint to create acoustic "fingerprints" of your music. This allows detection of duplicates even when:
- Metadata is completely different
- Files are different encodings of the same recording
- Tags have been modified or corrupted

### Installing Chromaprint

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

### Enabling Fingerprinting

1. Go to **Config** tab
2. Toggle **Enable Audio Fingerprinting**
3. Click **Save Settings**

### Performance Note

Initial fingerprint generation takes time for large collections. Fingerprints are cached in the database for subsequent scans.

---

## Troubleshooting

### Common Issues

**Backend won't start:**
- Verify Java 21 is installed: `java --version`
- Use `./gradle21 bootRun` instead of `./gradlew bootRun`
- Check port 9090 isn't in use

**Frontend won't start:**
- Verify Node.js 18+: `node --version`
- Run `npm install` in the frontend directory
- Check port 5173 isn't in use

**No duplicates found:**
- Verify files have metadata (title, artist)
- Try more lenient similarity settings
- Check file type filters include your formats

**Fingerprinting not working:**
- Verify Chromaprint is installed: `fpcalc -version`
- Ensure `fpcalc` is in your system PATH
- Check Config tab for fingerprinting errors

**Scan not progressing:**
- Check browser console for WebSocket errors
- Verify backend is running on port 9090
- Try refreshing the page

### Getting Help

**Built-in Help:**
- Click the **?** help button on any tab
- Context-specific documentation

**External Resources:**
- GitHub Issues: Report bugs and request features
- README.md: Technical documentation
- INSTALLATION.md: Detailed setup instructions

---

## Tips and Best Practices

### Before You Start

1. **Backup your music**: Always backup before bulk operations
2. **Start small**: Test with a subset first
3. **Check metadata**: Better tags = better duplicate detection
4. **Plan organization**: Decide on folder structure before organizing

### Duplicate Detection Strategy

**For High-Quality Collections:**
- Use "Strict" preset
- Enable track number matching
- Higher similarity thresholds

**For Mixed Collections:**
- Use "Balanced" preset
- Enable text normalization
- Review results carefully

**For Downloaded Music:**
- Use "Lenient" preset
- Enable all normalization options
- Consider audio fingerprinting

### Workflow Recommendations

**Initial Setup:**
1. Configure detection settings (start with Balanced)
2. Enable audio fingerprinting if available
3. Import a small test folder
4. Verify duplicate detection works as expected

**Regular Maintenance:**
1. Import new music periodically
2. Run duplicate detection after imports
3. Use directory view for album conflicts
4. Clean up metadata as issues are found

**Large Collections:**
- Import in batches for responsiveness
- Use directory-based resolution for speed
- Preview auto-resolve before executing

### Metadata Best Practices

**Consistency Matters:**
- Decide on artist name format ("The Beatles" vs "Beatles")
- Use standard genre names
- Include track numbers for proper organization

**Quality Indicators:**
- Complete metadata improves duplicate detection
- Consistent spelling reduces false negatives
- Track numbers enable proper organization

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Enter` | Execute search |
| `Escape` | Close dialogs |
| `Ctrl/Cmd + A` | Select all (in tables) |

---

## API Reference

MP3Org exposes a REST API on port 9090:

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/music` | List music files (paginated) |
| `GET /api/v1/duplicates` | Get duplicate groups |
| `GET /api/v1/duplicates/by-directory` | Get directory conflicts |
| `POST /api/v1/scanning/start` | Start a scan |
| `WS /ws` | WebSocket for real-time updates |

See README.md for complete API documentation.

---

*Last updated: January 2026*
*Version: MP3Org 2.0*
