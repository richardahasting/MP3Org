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
