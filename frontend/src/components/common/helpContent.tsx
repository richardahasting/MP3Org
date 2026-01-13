import React from 'react';

interface HelpSection {
  title: string;
  content: string | React.ReactNode;
}

// Duplicates Tab Help
export const duplicatesHelp: HelpSection[] = [
  {
    title: 'Overview',
    content: 'The Duplicates tab helps you find and manage duplicate music files in your collection. It uses audio fingerprinting technology to identify files that sound the same, even if they have different filenames or metadata.'
  },
  {
    title: 'Scan for Duplicates',
    content: (
      <div>
        <p>Click <strong>Scan for Duplicates</strong> to analyze your music collection and find duplicate files. The scan uses:</p>
        <ul>
          <li><strong>Audio Fingerprinting</strong> - Compares the actual audio content to find matches</li>
          <li><strong>Metadata Matching</strong> - Falls back to comparing title, artist, and duration if fingerprints aren't available</li>
        </ul>
        <p>Progress is shown in real-time as groups are discovered.</p>
      </div>
    )
  },
  {
    title: 'Auto-Resolve',
    content: (
      <div>
        <p>Click <strong>Auto-Resolve</strong> to automatically select which duplicates to delete based on these rules:</p>
        <ol>
          <li><strong>Highest Bitrate</strong> - Keep the file with better audio quality</li>
          <li><strong>Best Metadata</strong> - If bitrates match, keep the file with more complete metadata (artist, title, album)</li>
          <li><strong>Directory Match</strong> - If metadata matches, prefer files in directories that match the artist or album name</li>
        </ol>
        <p>Files that can't be automatically resolved are marked for manual review.</p>
      </div>
    )
  },
  {
    title: 'Preview Panel',
    content: (
      <div>
        <p>The preview panel shows all files that will be deleted before you confirm:</p>
        <ul>
          <li><strong>Play Button</strong> - Listen to the file before deciding</li>
          <li><strong>Match %</strong> - Shows how similar the files are (fingerprint match)</li>
          <li><strong>Open Folder</strong> - Opens the file location in Finder/Explorer</li>
          <li><strong>Show Kept</strong> - Reveals which file will be kept for comparison</li>
          <li><strong>Keep Both</strong> - Check this to exclude both files from deletion</li>
          <li><strong>Swap</strong> - Reverses which file gets deleted vs kept</li>
        </ul>
      </div>
    )
  },
  {
    title: 'Manual Resolution',
    content: (
      <div>
        <p>For groups that need manual review, or to handle duplicates individually:</p>
        <ol>
          <li>Click on a duplicate group in the left panel</li>
          <li>Review the files shown on the right - compare bitrate, file size, and paths</li>
          <li>Use the <strong>Play</strong> button to listen to each file</li>
          <li>Click <strong>Delete This File</strong> on the file(s) you want to remove</li>
        </ol>
        <p>The "Reference" badge indicates the first file in the group (used for comparison).</p>
      </div>
    )
  },
  {
    title: 'Need Review Groups',
    content: 'Groups marked "need review" have no clear winner - all files have the same bitrate, metadata completeness, and no matching directory paths. These must be resolved manually by listening to the files or checking their locations.'
  },
  {
    title: 'Refresh',
    content: 'Click <strong>Refresh</strong> to reload the duplicate groups from the database. Use this after making changes outside the app or if the display seems out of sync.'
  }
];

// Metadata Tab Help
export const metadataHelp: HelpSection[] = [
  {
    title: 'Overview',
    content: 'The Metadata tab allows you to search, view, and edit the metadata (tags) of your music files. You can edit individual files or make bulk changes to multiple files at once.'
  },
  {
    title: 'Searching',
    content: (
      <div>
        <p>Use the search box to find files by any metadata field:</p>
        <ul>
          <li>Title, Artist, Album</li>
          <li>Genre, Year</li>
          <li>File path</li>
        </ul>
        <p>Search is case-insensitive and matches partial text.</p>
      </div>
    )
  },
  {
    title: 'Viewing Files',
    content: (
      <div>
        <p>The file list shows all matching files with key information:</p>
        <ul>
          <li><strong>Title</strong> - Song title from metadata</li>
          <li><strong>Artist</strong> - Performing artist</li>
          <li><strong>Album</strong> - Album name</li>
          <li><strong>Duration</strong> - Track length</li>
          <li><strong>Bitrate</strong> - Audio quality (kbps)</li>
        </ul>
        <p>Click on a row to select it for editing.</p>
      </div>
    )
  },
  {
    title: 'Editing Metadata',
    content: (
      <div>
        <p>To edit a file's metadata:</p>
        <ol>
          <li>Click on a file to select it</li>
          <li>The edit panel appears showing all editable fields</li>
          <li>Make your changes</li>
          <li>Click <strong>Save</strong> to apply changes</li>
        </ol>
        <p>Editable fields include: Title, Artist, Album, Genre, Track Number, and Year.</p>
      </div>
    )
  },
  {
    title: 'Bulk Editing',
    content: (
      <div>
        <p>To edit multiple files at once:</p>
        <ol>
          <li>Select multiple files using checkboxes or Shift+Click</li>
          <li>Click <strong>Bulk Edit</strong></li>
          <li>Enter values to apply to all selected files</li>
          <li>Only non-empty fields will be updated</li>
        </ol>
        <p>This is useful for setting the same album or artist across multiple tracks.</p>
      </div>
    )
  },
  {
    title: 'Pagination',
    content: 'Large collections are paginated. Use the page controls at the bottom to navigate through results. You can also change the number of items displayed per page.'
  }
];

// Import Tab Help
export const importHelp: HelpSection[] = [
  {
    title: 'Overview',
    content: 'The Import tab allows you to scan directories on your computer to add music files to your collection. The scanner reads metadata from audio files and stores it in the database for searching and organization.'
  },
  {
    title: 'Selecting a Directory',
    content: (
      <div>
        <p>To select a directory to scan:</p>
        <ol>
          <li>Use the directory browser to navigate to your music folder</li>
          <li>Click on folders to expand them</li>
          <li>Select the folder you want to scan</li>
        </ol>
        <p>The scanner will process all music files in the selected directory and its subdirectories.</p>
      </div>
    )
  },
  {
    title: 'Starting a Scan',
    content: (
      <div>
        <p>Click <strong>Start Scan</strong> to begin importing files. The scan:</p>
        <ul>
          <li>Recursively searches all subdirectories</li>
          <li>Reads metadata from MP3, FLAC, M4A, and other audio formats</li>
          <li>Generates audio fingerprints for duplicate detection</li>
          <li>Skips files that are already in the database</li>
        </ul>
      </div>
    )
  },
  {
    title: 'Scan Progress',
    content: (
      <div>
        <p>During scanning, you'll see real-time progress:</p>
        <ul>
          <li><strong>Files Found</strong> - Total audio files discovered</li>
          <li><strong>Files Processed</strong> - Files with metadata read</li>
          <li><strong>Current File</strong> - File being processed</li>
          <li><strong>Progress Bar</strong> - Overall completion percentage</li>
        </ul>
      </div>
    )
  },
  {
    title: 'Canceling a Scan',
    content: 'Click <strong>Cancel</strong> to stop a scan in progress. Files already processed will remain in the database.'
  },
  {
    title: 'Supported Formats',
    content: (
      <div>
        <p>The scanner supports these audio formats:</p>
        <ul>
          <li>MP3 (.mp3)</li>
          <li>FLAC (.flac)</li>
          <li>AAC/M4A (.m4a, .aac)</li>
          <li>OGG Vorbis (.ogg)</li>
          <li>WAV (.wav)</li>
          <li>WMA (.wma)</li>
        </ul>
      </div>
    )
  }
];

// Organize Tab Help
export const organizeHelp: HelpSection[] = [
  {
    title: 'Overview',
    content: 'The Organize tab helps you reorganize your music files into a consistent folder structure based on their metadata. Files are moved to directories based on artist and album names.'
  },
  {
    title: 'Organization Templates',
    content: (
      <div>
        <p>Choose how files should be organized:</p>
        <ul>
          <li><strong>Artist/Album/Track</strong> - Standard hierarchy: Artist → Album → Song</li>
          <li><strong>Genre/Artist/Album</strong> - Grouped by genre first</li>
          <li><strong>Year/Artist/Album</strong> - Organized chronologically</li>
        </ul>
        <p>The template determines the folder structure for organized files.</p>
      </div>
    )
  },
  {
    title: 'Destination Directory',
    content: 'Select the root directory where organized files should be placed. A new folder structure will be created based on your chosen template.'
  },
  {
    title: 'Preview Changes',
    content: (
      <div>
        <p>Click <strong>Preview</strong> to see what changes will be made:</p>
        <ul>
          <li>Current file location</li>
          <li>New file location after organization</li>
          <li>Files that will be renamed</li>
        </ul>
        <p>Review the preview carefully before executing.</p>
      </div>
    )
  },
  {
    title: 'Execute Organization',
    content: (
      <div>
        <p>Click <strong>Organize</strong> to move files to their new locations:</p>
        <ul>
          <li>Files are moved (not copied) to save disk space</li>
          <li>Empty source directories are removed</li>
          <li>The database is updated with new file paths</li>
        </ul>
        <p><strong>Warning:</strong> This operation moves files on disk. Make sure you have a backup.</p>
      </div>
    )
  },
  {
    title: 'Filename Formatting',
    content: (
      <div>
        <p>Files are renamed based on metadata:</p>
        <ul>
          <li>Track numbers are zero-padded (01, 02, etc.)</li>
          <li>Special characters are replaced with safe alternatives</li>
          <li>Leading/trailing spaces are removed</li>
        </ul>
      </div>
    )
  }
];

// Config Tab Help
export const configHelp: HelpSection[] = [
  {
    title: 'Audio Fingerprinting Setup (Required)',
    content: (
      <div>
        <p>Audio fingerprinting requires <strong>Chromaprint</strong>, an external tool that analyzes audio content. You must install it for accurate duplicate detection.</p>

        <h4 style={{ marginTop: '1rem', marginBottom: '0.5rem', color: 'var(--accent-primary)' }}>macOS</h4>
        <p>Install using Homebrew:</p>
        <pre style={{ background: 'var(--bg-base)', padding: '0.5rem', borderRadius: '4px', fontSize: '0.8125rem' }}>brew install chromaprint</pre>
        <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>If you don't have Homebrew, install it first from <a href="https://brew.sh" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent-primary)' }}>brew.sh</a></p>

        <h4 style={{ marginTop: '1rem', marginBottom: '0.5rem', color: 'var(--accent-primary)' }}>Windows</h4>
        <ol style={{ fontSize: '0.875rem' }}>
          <li>Download Chromaprint from <a href="https://acoustid.org/chromaprint" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent-primary)' }}>acoustid.org/chromaprint</a></li>
          <li>Extract the ZIP file</li>
          <li>Copy <code>fpcalc.exe</code> to a folder in your PATH (e.g., <code>C:\Windows</code>)</li>
          <li>Or add the extraction folder to your system PATH</li>
        </ol>

        <h4 style={{ marginTop: '1rem', marginBottom: '0.5rem', color: 'var(--accent-primary)' }}>Linux (Ubuntu/Debian)</h4>
        <pre style={{ background: 'var(--bg-base)', padding: '0.5rem', borderRadius: '4px', fontSize: '0.8125rem' }}>sudo apt install libchromaprint-tools</pre>

        <h4 style={{ marginTop: '1rem', marginBottom: '0.5rem', color: 'var(--accent-primary)' }}>Linux (Fedora/RHEL)</h4>
        <pre style={{ background: 'var(--bg-base)', padding: '0.5rem', borderRadius: '4px', fontSize: '0.8125rem' }}>sudo dnf install chromaprint-tools</pre>

        <h4 style={{ marginTop: '1rem', marginBottom: '0.5rem', color: 'var(--accent-primary)' }}>Linux (Arch)</h4>
        <pre style={{ background: 'var(--bg-base)', padding: '0.5rem', borderRadius: '4px', fontSize: '0.8125rem' }}>sudo pacman -S chromaprint</pre>

        <p style={{ marginTop: '1rem' }}><strong>Verify installation:</strong> Open a terminal and run <code>fpcalc -version</code>. You should see version information. Restart MP3Org after installing.</p>
      </div>
    )
  },
  {
    title: 'Fingerprint Settings',
    content: (
      <div>
        <p>Audio fingerprinting analyzes the actual sound to find duplicates:</p>
        <ul>
          <li><strong>Similarity Threshold</strong> - How similar fingerprints must be (recommended: 85-95%)</li>
          <li><strong>Generate Missing</strong> - Create fingerprints for files that don't have them</li>
        </ul>
        <p>Fingerprint matching is more accurate than metadata matching but requires more processing.</p>
      </div>
    )
  },
  {
    title: 'Generate Missing Fingerprints',
    content: (
      <div>
        <p>Click <strong>Generate Missing Fingerprints</strong> to create fingerprints for files that don't have them:</p>
        <ul>
          <li>Progress is shown as files are processed</li>
          <li>Files with errors are logged but don't stop the process</li>
          <li>This may take a while for large collections</li>
        </ul>
        <p>Fingerprints are required for accurate duplicate detection.</p>
      </div>
    )
  },
  {
    title: 'Fingerprint Status',
    content: (
      <div>
        <p>The status display shows:</p>
        <ul>
          <li><strong>Files with Fingerprints</strong> - Ready for duplicate detection</li>
          <li><strong>Files without Fingerprints</strong> - Need fingerprint generation</li>
          <li><strong>fpcalc Status</strong> - Whether the fingerprint tool is available</li>
        </ul>
      </div>
    )
  },
  {
    title: 'Overview',
    content: 'The Config tab contains settings for duplicate detection and audio fingerprinting. Adjust these settings to fine-tune how duplicates are identified in your collection.'
  },
  {
    title: 'Fuzzy Matching Settings',
    content: (
      <div>
        <p>These settings control metadata-based duplicate detection:</p>
        <ul>
          <li><strong>Title Threshold</strong> - How similar titles must be (0-100%)</li>
          <li><strong>Artist Threshold</strong> - How similar artist names must be</li>
          <li><strong>Duration Tolerance</strong> - Allowed difference in track length (seconds)</li>
        </ul>
        <p>Higher thresholds require closer matches; lower values find more potential duplicates.</p>
      </div>
    )
  },
  {
    title: 'Saving Settings',
    content: 'Changes to settings are saved automatically. The duplicate detection will use the new settings on the next scan.'
  }
];
