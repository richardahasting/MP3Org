# MP3Org - Complete User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Configuration](#configuration)
4. [Database Profiles](#database-profiles)
5. [Importing Music](#importing-music)
6. [Finding and Managing Duplicates](#finding-and-managing-duplicates)
7. [Editing Metadata](#editing-metadata)
8. [Organizing Your Collection](#organizing-your-collection)
9. [Advanced Features](#advanced-features)
10. [Troubleshooting](#troubleshooting)
11. [Tips and Best Practices](#tips-and-best-practices)

---

## Introduction

MP3Org is a powerful music collection management tool designed to help you organize large music libraries by finding and removing duplicate files, organizing music into structured folders, and editing metadata for better collection management.

### Key Features
- **Smart Duplicate Detection**: Advanced fuzzy matching algorithms find duplicates even when metadata differs slightly
- **File Organization**: Automatically organize music into Artist/Album/Track structure
- **Metadata Editing**: Search and edit song information for better organization
- **Multiple Database Support**: Maintain separate music collections with profiles
- **Format Support**: Works with MP3, FLAC, M4A, WAV, OGG, WMA, AIFF, APE, and OPUS files

---

## Getting Started

### First Launch

When you first start MP3Org, the application will:
1. Create a default database in the `mp3org` folder in your current directory
2. Initialize with default settings that work well for most users
3. Show an empty collection ready for your music files

### Quick Start Guide

**Step 1: Configure Settings (Optional)**
- Go to the **Config** tab
- Choose which audio formats to include (all are enabled by default)
- Adjust duplicate detection settings if needed (defaults are recommended for beginners)

**Step 2: Import Your Music**
- Go to the **Import & Organize** tab
- Click **"Add Directories to Scan"**
- Select folders containing your music files
- Wait for the scan to complete

**Step 3: Find Duplicates**
- Go to the **Duplicate Manager** tab
- Click **"Refresh Duplicates"**
- Review potential duplicates and remove unwanted files

**Step 4: Organize (Optional)**
- Return to **Import & Organize** tab
- Choose a destination folder
- Click **"Organize Music Files"** to create a clean folder structure

---

## Configuration

The **Config** tab contains all application settings and is divided into several sections:

### Database Location

**Current Database Location**: Shows where your music database is stored. This location can be changed by:
- **System Property**: `java -Dmp3org.database.path=/path/to/database -jar mp3org.jar`
- **Environment Variable**: `MP3ORG_DATABASE_PATH=/path/to/database`
- **Configuration File**: Create `mp3org.properties` with `database.path=/path/to/database`
- **GUI**: Use the "Change Database Location" button

### File Type Filters

Control which audio formats are included in scans and searches:

| Format | Description |
|--------|-------------|
| MP3 | Most common audio format |
| FLAC | Lossless audio format |
| M4A | Apple's AAC format |
| WAV | Uncompressed audio |
| OGG | Open source audio format |
| WMA | Windows Media Audio |
| AIFF | Apple's uncompressed format |
| APE | Monkey's Audio lossless format |
| OPUS | Modern open audio codec |

**Tips:**
- Enable all formats unless you have specific reasons to exclude some
- Use "Select All" to quickly enable everything
- Changes take effect immediately after clicking "Apply File Type Filters"

### Duplicate Detection Settings

This is the most important configuration section, controlling how MP3Org identifies potential duplicates.

#### Presets

| Preset | Description | Best For |
|--------|-------------|----------|
| **Strict** | High similarity thresholds, fewer matches | High-quality collections with consistent metadata |
| **Balanced** | ⭐ Recommended for most users | General music collections |
| **Lenient** | Lower thresholds, more matches | Collections with inconsistent metadata |
| **Custom** | Manually configured settings | Advanced users with specific needs |

#### Similarity Thresholds

- **Title Similarity (85%)**: How closely song titles must match
- **Artist Similarity (90%)**: How closely artist names must match  
- **Album Similarity (85%)**: How closely album names must match

Higher percentages = stricter matching = fewer false positives but might miss some duplicates.

#### Duration Matching

- **Tolerance (Seconds)**: Maximum time difference allowed (default: 10 seconds)
- **Tolerance (Percent)**: Alternative percentage-based tolerance (default: 5%)

The system uses whichever tolerance is more permissive.

#### Text Normalization Options

| Option | Effect | Example |
|--------|--------|---------|
| **Ignore Case** | Treats different cases as identical | "Artist" = "artist" |
| **Ignore Punctuation** | Ignores punctuation differences | "Rock 'n' Roll" = "Rock n Roll" |
| **Ignore Artist Prefixes** | Ignores "The", "A", "An" | "Beatles" = "The Beatles" |
| **Ignore Featuring** | Ignores featuring artist info | "Song" = "Song (feat. Artist)" |
| **Ignore Album Editions** | Ignores edition information | "Album" = "Album - Deluxe Edition" |

#### Matching Requirements

- **Track Numbers Must Match**: Whether track numbers must be identical for duplicates
- **Minimum Fields Match**: How many fields (title, artist, album, duration) must meet thresholds

---

## Database Profiles

Profiles allow you to maintain multiple separate music databases, each with its own settings.

### Why Use Profiles?

- **Separate Collections**: Personal music vs. work music vs. classical collection
- **Different Detection Settings**: Strict settings for high-quality collection, lenient for downloaded music
- **Different Locations**: Keep databases in different folders or drives
- **Multiple Users**: Each family member can have their own music profile

### Managing Profiles

**Creating a New Profile:**
1. Click **"New Profile"** in the Config tab
2. Enter a descriptive name
3. Choose the database location
4. Configure settings as needed

**Duplicating a Profile:**
1. Select the profile to copy
2. Click **"Duplicate"**
3. The new profile inherits all settings from the original

**Switching Profiles:**
1. Use the dropdown menu to select a different profile
2. The application switches databases and loads that profile's settings
3. All your music and settings are now from the selected profile

**Profile Settings:**
Each profile remembers:
- Database location and files
- File type preferences
- Duplicate detection configuration
- Last used date

---

## Importing Music

The **Import & Organize** tab handles adding music files to your database.

### Adding Music to Database

**Step 1: Select Directories**
- Click **"Add Directories to Scan"**
- Choose one or more folders containing music
- Subdirectories are automatically included

**Step 2: Monitor Progress**
- Watch the progress bar during scanning
- Large collections may take several minutes
- The process can be interrupted and resumed later

**Step 3: Review Results**
- Check the status messages for any errors
- Only files matching your enabled formats are imported
- Metadata is automatically extracted from file tags

### Supported Metadata

MP3Org extracts the following information from your music files:
- **Title**: Song name
- **Artist**: Performing artist or band
- **Album**: Album or collection name
- **Genre**: Musical style
- **Track Number**: Position on the album
- **Year**: Release year
- **Duration**: Song length in seconds
- **Bitrate**: Audio quality (kbps)
- **File Size**: Size in bytes
- **File Path**: Location on disk

### Import Tips

- **Start Small**: Test with a small folder first to understand the process
- **Check Metadata**: Ensure your files have good metadata before importing
- **Backup First**: Always backup your music collection before making changes
- **Clean Folders**: Remove non-music files from directories before scanning

---

## Finding and Managing Duplicates

The **Duplicate Manager** tab is where you identify and remove duplicate files.

### Finding Duplicates

**Step 1: Scan for Duplicates**
- Click **"Refresh Duplicates"** to analyze your collection
- The process uses your configured similarity settings
- Results appear in the "Potential Duplicates" table

**Step 2: Review Results**
- Select a file from the left table to see similar matches on the right
- Compare the files using the displayed information
- Look for actual duplicates vs. different versions/qualities

### Comparison Factors

When deciding which file to keep, consider:

| Factor | Higher is Better | Notes |
|--------|------------------|-------|
| **Bitrate** | ✓ | Generally indicates better audio quality |
| **File Size** | ± | Larger often means better quality, but not always |
| **File Format** | ± | FLAC > MP3 > other formats for quality |
| **Metadata Quality** | ✓ | Complete, accurate tags are valuable |
| **File Location** | ± | Consider your preferred folder structure |

### Removal Actions

**Delete Selected**
- Manually select and delete specific files
- Use this for careful, file-by-file review
- Confirms deletion before proceeding

**Keep Better Quality**
- Automatically compares bitrates between two selected files
- Deletes the lower quality version
- Useful for obvious quality differences

### Safety Tips

⚠️ **Important**: File deletion is permanent!

- **Backup Everything**: Always backup your collection before removing duplicates
- **Review Carefully**: The system may occasionally suggest non-duplicates
- **Start Conservative**: Use strict settings initially, then relax if needed
- **Check Results**: Listen to files if you're unsure about deletion

---

## Editing Metadata

The **Metadata Editor** tab allows you to search for and edit song information.

### Searching for Music

**Search Options:**
- **All Fields**: Searches title, artist, album, and genre
- **Title**: Searches only song titles
- **Artist**: Searches only artist names
- **Album**: Searches only album names

**Search Tips:**
- Use partial words (e.g., "beat" finds "Beatles")
- Search is case-insensitive
- Use specific terms for better results

### Editing Information

**Available Fields:**
- **Title**: Song name as it should appear
- **Artist**: Primary performing artist or band
- **Album**: Album or collection name
- **Genre**: Musical style (Rock, Pop, Jazz, etc.)
- **Track Number**: Position on album (numbers only)
- **Year**: Release year (4 digits)

**Read-Only Information:**
- **File Path**: Location on disk
- **File Size**: Size in megabytes
- **Bitrate**: Audio quality in kbps
- **Duration**: Song length

### Metadata Best Practices

**Consistency is Key:**
- Use consistent artist names ("The Beatles" vs "Beatles")
- Use standard genre names
- Include track numbers for proper album organization
- Use full album names

**Accuracy Helps Duplicates:**
- Correct metadata improves duplicate detection
- Fix typos and inconsistencies
- Standardize featuring artist formats

**Save Regularly:**
- Changes are saved to the database when you click "Save Changes"
- Use "Revert" to undo unsaved changes
- Large batches can be time-consuming

---

## Organizing Your Collection

The **Import & Organize** tab also handles creating an organized copy of your music collection.

### Organization Structure

MP3Org creates a hierarchical folder structure:
```
Destination Folder/
├── Artist Name/
│   ├── Album Name/
│   │   ├── 01-Song Title.mp3
│   │   ├── 02-Another Song.mp3
│   │   └── ...
│   └── Another Album/
│       └── ...
└── Another Artist/
    └── ...
```

### Organization Process

**Step 1: Choose Destination**
- Select an empty folder for your organized collection
- Ensure adequate free space (copies are created, originals remain)
- Choose a location that's easy to access

**Step 2: Start Organization**
- Click **"Organize Music Files"**
- Monitor progress through the progress bar
- Large collections may take considerable time

**Step 3: Verify Results**
- Check the destination folder for proper organization
- Verify that files play correctly
- Compare file counts to ensure completeness

### Organization Features

**File Naming:**
- Format: `[Track Number]-[Song Title].[Extension]`
- Track numbers are zero-padded (01, 02, etc.)
- Invalid filename characters are replaced

**Folder Naming:**
- Artist and album names are used as folder names
- Invalid folder characters are replaced
- Empty or missing names use placeholders

**File Handling:**
- Original files remain unchanged
- Copies maintain original quality and metadata
- Duplicate files in the same album are numbered

---

## Advanced Features

### Database Management

**Multiple Databases:**
- Each profile maintains its own database
- Switch between profiles to access different collections
- Profiles can share the same music files but have different organization

**Database Maintenance:**
- Use "Reload Configuration" to refresh settings
- Database files are stored in Apache Derby format
- Backup the entire database folder to preserve your work

### Performance Optimization

**Large Collections:**
- Import music in smaller batches for better responsiveness
- Use optimized duplicate detection for faster processing
- Consider profile organization for different collection types

**Memory Usage:**
- Close other applications when working with very large collections
- Restart MP3Org if performance degrades over long sessions

### Customization Options

**File Type Configuration:**
- Disable formats you don't use for faster scanning
- Re-enable formats as needed for specific imports

**Detection Tuning:**
- Start with "Balanced" preset and adjust based on results
- Use "Custom" settings for specific collection types
- Save different configurations in different profiles

---

## Troubleshooting

### Common Issues

**No Music Found During Import**
- Check that selected folders contain supported file types
- Verify file type filters include your music formats
- Ensure files have proper file extensions

**Duplicate Detection Not Working**
- Verify that files have metadata (title, artist, album)
- Try more lenient similarity settings
- Check that files are actually duplicates (not different versions)

**Organization Creates Empty Folders**
- Files may be missing essential metadata
- Edit metadata before organizing
- Check for invalid characters in artist/album names

**Performance Issues**
- Close other memory-intensive applications
- Work with smaller collections at a time
- Restart the application if it becomes unresponsive

### Error Messages

**"Database Connection Failed"**
- Check that the database location is accessible
- Verify folder permissions
- Try changing the database location

**"File Already Exists"**
- During organization, files with identical names exist
- The system automatically handles this with numbering
- Check destination folder for the resulting files

**"Access Denied"**
- File or folder permissions prevent access
- Run as administrator if necessary (Windows)
- Check file system permissions (Linux/Mac)

### Getting Help

**Built-in Help:**
- Use the "?" buttons throughout the interface
- Press F1 for general help
- Each tab has context-specific help available

**Logs and Debugging:**
- Check console output for detailed error messages
- Note the exact steps that caused problems
- Record file paths and error messages for support

---

## Tips and Best Practices

### Before You Start

1. **Backup Your Music**: Always backup your entire music collection before using any organization tool
2. **Start Small**: Test with a small subset of your collection first
3. **Clean Your Files**: Remove non-music files from folders before importing
4. **Check Metadata**: Ensure your music files have good metadata tags

### Workflow Recommendations

**Initial Setup:**
1. Configure file types for your collection
2. Create profiles if you have different music types
3. Set up duplicate detection (start with "Balanced")
4. Test with a small folder first

**Regular Maintenance:**
1. Import new music regularly
2. Run duplicate detection periodically
3. Clean up metadata as you notice issues
4. Organize into clean folder structure when ready

**Advanced Usage:**
1. Use multiple profiles for different music types
2. Adjust detection settings based on your collection quality
3. Combine with other tools for complete music management

### Duplicate Detection Strategies

**For High-Quality Collections:**
- Use "Strict" preset
- Enable track number matching
- Use higher similarity thresholds

**For Mixed Collections:**
- Use "Balanced" or "Lenient" presets
- Enable text normalization options
- Review results carefully

**For Downloaded Music:**
- Use "Lenient" preset with normalization
- Focus on artist and title matching
- Be prepared for more false positives

### Metadata Management

**Consistency Rules:**
- Decide on artist name formats ("The Beatles" vs "Beatles")
- Use standard genre classifications
- Be consistent with featuring artist formats
- Include complete album information

**Quality Indicators:**
- Complete metadata improves duplicate detection
- Accurate track numbers enable proper organization
- Consistent spelling reduces false negatives

### Organization Planning

**Before Organizing:**
- Plan your destination folder structure
- Ensure adequate disk space (at least 2x your collection size)
- Clean up metadata first for better folder names
- Consider if you want copies or to move originals

**After Organizing:**
- Verify the organization worked correctly
- Update your media player libraries
- Consider cleaning up the original folders
- Backup the organized collection

---

## Conclusion

MP3Org provides powerful tools for managing large music collections, from duplicate detection to organization and metadata editing. By following this guide and starting with small test collections, you can effectively organize and clean up even the largest music libraries.

Remember that music collection management is an iterative process - start with conservative settings, learn how your collection responds, and gradually refine your approach for the best results.

For additional help, use the built-in help system throughout the application, and don't hesitate to experiment with different settings on backup copies of your music.

---

*Last updated: [Current Date]*
*Version: MP3Org 1.0*