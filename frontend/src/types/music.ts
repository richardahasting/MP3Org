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

export type TabId = 'duplicates' | 'metadata' | 'import' | 'organize' | 'config';

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
