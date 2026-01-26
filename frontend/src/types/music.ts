export interface MusicFile {
  id: number;
  filePath: string;
  title: string;
  artist: string;
  album: string;
  genre: string;
  trackNumber: number | null;
  year: number | null;
  durationSeconds: number;
  fileSizeBytes: number;
  bitRate: number;
  sampleRate: number;
  fileType: string;
  lastModified: string;
  dateAdded: string;
  formattedDuration: string;
  formattedFileSize: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type TabId = 'duplicates' | 'metadata' | 'import' | 'organize' | 'share' | 'config';

// Scanning types for Phase 2
export interface ScanProgress {
  sessionId: string;
  stage: 'starting' | 'scanning' | 'reading_tags' | 'saving' | 'completed' | 'cancelled' | 'error';
  currentDirectory: string;
  currentFile: string;
  filesFound: number;
  filesProcessed: number;
  totalDirectories: number;
  directoriesProcessed: number;
  percentComplete: number;
  message: string;
  isComplete: boolean;
  isCancelled: boolean;
  error: string | null;
}

export interface DirectoryEntry {
  path: string;
  name: string;
  isDirectory: boolean;
  canRead: boolean;
}

export interface BrowseResponse {
  currentPath: string | null;
  entries: DirectoryEntry[];
}

// Duplicate detection types for Phase 3
export interface DuplicateFile {
  file: MusicFile;
  similarity: number | null;  // Fingerprint similarity to reference file (0.0-1.0), null if not available
}

export interface DuplicateGroup {
  groupId: number;
  fileCount: number;
  files: DuplicateFile[];
  representativeTitle: string;
  representativeArtist: string;
}

export interface DuplicateScanStatus {
  sessionId: string;
  stage: 'starting' | 'loading' | 'scanning' | 'completed' | 'cancelled' | 'error';
  totalFiles: number;
  filesProcessed: number;
  totalComparisons: number;
  comparisonsCompleted: number;
  groupsFound: number;
  percentComplete: number;
  isCancelled: boolean;
  isComplete: boolean;
  error: string | null;
}

export interface CompareResult {
  file1: MusicFile;
  file2: MusicFile;
  similarity: number;
  areDuplicates: boolean;
  breakdown: string;
}

export interface AutoResolutionResult {
  groupsProcessed: number;
  filesDeleted: number;
  filesKept: number;
  holdMyHandGroups: DuplicateGroup[];  // Groups needing manual review
  summary: string;
}

export interface ResolutionItem {
  groupId: number;
  fileToDelete: MusicFile;
  fileToKeep: MusicFile;
  reason: string;
  similarity: number | null;  // Fingerprint similarity (0.0-1.0), null if not available
}

export interface AutoResolutionPreview {
  resolutions: ResolutionItem[];
  holdMyHandGroups: DuplicateGroup[];
  totalFilesToDelete: number;
  totalFilesToKeep: number;
  totalGroupsNeedingReview: number;
}

// Directory-based duplicate grouping types (Issue #92)
export interface DuplicatePair {
  fileA: MusicFile;
  fileB: MusicFile;
  similarity: number | null;
}

export interface DirectoryConflict {
  directoryA: string;
  directoryB: string;
  filesInA: number;
  filesInB: number;
  totalDuplicatePairs: number;
  pairs: DuplicatePair[];
}

export interface DirectoryResolutionPreview {
  directoryToKeep: string;
  directoryToDelete: string;
  filesToDelete: MusicFile[];
  filesToKeep: MusicFile[];
  totalFilesToDelete: number;
}

export interface DirectoryResolutionResult {
  filesDeleted: number;
  filesAttempted: number;
  directoryKept: string;
  directoryCleared: string;
}
