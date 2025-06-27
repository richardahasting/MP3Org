# MP3Org - Music Collection Manager

A powerful JavaFX application for organizing and managing large music collections. MP3Org helps you find and remove duplicate songs, organize files into structured folders, and edit metadata for better collection management.

![MP3Org Screenshot](https://via.placeholder.com/800x600/2C3E50/FFFFFF?text=MP3Org+Screenshot)

## ✨ Features

### 🔍 Smart Duplicate Detection
- **Advanced fuzzy matching** algorithms find duplicates even when metadata differs slightly
- **Configurable similarity thresholds** for title, artist, album, and duration
- **Text normalization** handles artist prefixes, featuring artists, album editions
- **Multiple detection presets**: Strict, Balanced, Lenient, or Custom settings

### 📁 File Organization
- **Automatic folder structure** creation: Artist/Album/Track-Title format
- **Preserves original files** while creating organized copies
- **Batch processing** for large collections
- **File type filtering** for selective organization

### 🎵 Metadata Management
- **Search and edit** song information across your collection
- **Batch metadata updates** for consistency
- **Automatic metadata extraction** from file tags
- **Support for all common fields**: title, artist, album, genre, year, track number

### 🗂️ Multiple Database Profiles
- **Separate collections** for different music types (personal, work, classical, etc.)
- **Profile-specific settings** for optimal duplicate detection
- **Easy switching** between collections
- **Independent configuration** per profile

### 🎯 Format Support
Supports all major audio formats:
- **Lossless**: FLAC, AIFF, APE, WAV
- **Compressed**: MP3, M4A (AAC), OGG Vorbis, OPUS, WMA

### 🆘 Comprehensive Help System
- **Interactive tooltips** for all UI elements
- **Context-sensitive help** dialogs
- **Complete user guide** with best practices
- **Getting started wizard** for new users

## 🚀 Quick Start

### Prerequisites
- **Java 11 or higher** (download from [OpenJDK](https://openjdk.org/) or [Oracle](https://www.oracle.com/java/))
- **JavaFX runtime** (included in most Java distributions)

### Download and Run

1. **Download the latest release** from the [Releases](https://github.com/richardahasting/MP3Org/releases) page
2. **Run the application**:
   ```bash
   java -jar mp3org-1.0.jar
   ```

### Building from Source

```bash
# Clone the repository
git clone https://github.com/richardahasting/MP3Org.git
cd MP3Org

# Build the application
./gradlew build

# Run the application
./gradlew run
```

## 📖 Usage Guide

### 1. Initial Setup
- Launch MP3Org and go to the **Config** tab
- Choose which audio formats to include (all enabled by default)
- Adjust duplicate detection settings if needed (defaults work well for most users)

### 2. Import Your Music
- Go to the **Import & Organize** tab
- Click **"Add Directories to Scan"** and select your music folders
- Wait for the scan to complete

### 3. Find and Remove Duplicates
- Go to the **Duplicate Manager** tab
- Click **"Refresh Duplicates"** to analyze your collection
- Review potential duplicates and remove unwanted files

### 4. Organize Your Collection (Optional)
- Return to the **Import & Organize** tab
- Choose a destination folder
- Click **"Organize Music Files"** to create a clean folder structure

### 5. Edit Metadata (As Needed)
- Use the **Metadata Editor** tab to search and edit song information
- This helps improve duplicate detection and organization

## 🛠️ Advanced Configuration

### Duplicate Detection Settings

| Setting | Description | Recommended |
|---------|-------------|-------------|
| **Title Similarity** | How closely song titles must match | 85% |
| **Artist Similarity** | How closely artist names must match | 90% |
| **Album Similarity** | How closely album names must match | 85% |
| **Duration Tolerance** | Maximum time difference allowed | 10 seconds |
| **Minimum Fields** | Fields that must match for duplicates | 2 out of 4 |

### Text Normalization Options
- **Ignore Case**: Treats "Artist" and "artist" as identical
- **Ignore Punctuation**: Handles "Rock 'n' Roll" vs "Rock n Roll"
- **Ignore Artist Prefixes**: Treats "Beatles" and "The Beatles" as same
- **Ignore Featuring**: Handles "Song" vs "Song (feat. Artist)"
- **Ignore Album Editions**: Handles "Album" vs "Album - Deluxe Edition"

## 🏗️ Architecture

MP3Org is built with a modular architecture:

```
src/main/java/org/hasting/
├── MP3OrgApplication.java          # Main JavaFX application
├── model/
│   └── MusicFile.java             # Music file data model
├── ui/                            # User interface components
│   ├── ConfigurationView.java     # Settings and configuration
│   ├── DuplicateManagerView.java  # Duplicate detection and management
│   ├── ImportOrganizeView.java    # File import and organization
│   └── MetadataEditorView.java    # Metadata search and editing
└── util/                          # Core utilities and algorithms
    ├── DatabaseManager.java       # Database operations
    ├── FuzzyMatcher.java          # Similarity algorithms
    ├── HelpSystem.java           # Tooltip and help system
    └── ...
```

## 🧪 Testing

The project includes comprehensive test coverage:

```bash
# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "*FuzzyMatcher*"
./gradlew test --tests "*DatabaseManager*"
```

## 🤝 Contributing

Contributions are welcome! Please feel free to:

1. **Report bugs** or request features via [Issues](https://github.com/richardahasting/MP3Org/issues)
2. **Submit pull requests** with improvements
3. **Improve documentation** or add examples
4. **Share feedback** on your experience

### Development Setup

1. **Fork and clone** the repository
2. **Open in your IDE** (IntelliJ IDEA recommended)
3. **Run tests** to ensure everything works: `./gradlew test`
4. **Make your changes** and add tests
5. **Submit a pull request**

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## 🔒 Privacy and Security

MP3Org prioritizes your privacy:
- **Local processing only** - no data sent to external servers
- **No telemetry or tracking**
- **Local database storage** using Apache Derby
- **Open source** - audit the code yourself

## 📞 Support

- **User Guide**: See [MP3Org_User_Guide.md](MP3Org_User_Guide.md) for detailed instructions
- **Built-in Help**: Press F1 in the application or use the Help menu
- **Issues**: Report bugs on [GitHub Issues](https://github.com/richardahasting/MP3Org/issues)
- **Discussions**: Ask questions in [GitHub Discussions](https://github.com/richardahasting/MP3Org/discussions)

## 🎯 Roadmap

Future enhancements planned:
- [ ] **Native installers** for Windows, macOS, and Linux
- [ ] **Playlist management** and export
- [ ] **Audio fingerprinting** for more accurate duplicate detection
- [ ] **Cloud storage integration** (Google Drive, Dropbox, etc.)
- [ ] **Music streaming service** metadata lookup
- [ ] **Plugin system** for extensibility

## 🙏 Acknowledgments

- **JAudioTagger** library for metadata extraction
- **Apache Derby** for embedded database functionality
- **JavaFX** for the modern user interface
- **OpenJDK** community for the Java platform

---

**Made with ❤️ for music lovers who want organized collections**

*Star ⭐ this repository if MP3Org helps you organize your music collection!*