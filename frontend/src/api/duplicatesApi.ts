import type { DuplicateGroup, DuplicateScanStatus, CompareResult, MusicFile, AutoResolutionResult, AutoResolutionPreview } from '../types/music';

const API_BASE = '/api/v1/duplicates';

export interface DuplicateGroupsResponse {
  groups: DuplicateGroup[];
  page: number;
  size: number;
  totalGroups: number;
  totalPages: number;
  hasMore: boolean;
}

/**
 * Fetches duplicate groups with pagination.
 */
export async function fetchDuplicateGroups(
  page: number = 0,
  size: number = 25
): Promise<DuplicateGroupsResponse> {
  const response = await fetch(`${API_BASE}?page=${page}&size=${size}`);
  if (!response.ok) throw new Error('Failed to fetch duplicate groups');
  return response.json();
}

/**
 * Gets the count of duplicate groups.
 */
export async function getDuplicateCount(): Promise<number> {
  const response = await fetch(`${API_BASE}/count`);
  if (!response.ok) throw new Error('Failed to get duplicate count');
  const data = await response.json();
  return data.count;
}

/**
 * Gets a specific duplicate group by ID.
 */
export async function getDuplicateGroup(groupId: number): Promise<DuplicateGroup> {
  const response = await fetch(`${API_BASE}/${groupId}`);
  if (!response.ok) throw new Error('Failed to fetch duplicate group');
  return response.json();
}

/**
 * Finds files similar to a specific file.
 */
export async function findSimilarFiles(fileId: number): Promise<MusicFile[]> {
  const response = await fetch(`${API_BASE}/similar/${fileId}`);
  if (!response.ok) throw new Error('Failed to find similar files');
  return response.json();
}

/**
 * Compares two files and gets similarity details.
 */
export async function compareFiles(fileId1: number, fileId2: number): Promise<CompareResult> {
  const response = await fetch(`${API_BASE}/compare`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fileId1, fileId2 }),
  });
  if (!response.ok) throw new Error('Failed to compare files');
  return response.json();
}

/**
 * Starts an asynchronous duplicate detection scan.
 */
export async function startDuplicateScan(): Promise<string> {
  const response = await fetch(`${API_BASE}/scan`, { method: 'POST' });
  if (!response.ok) throw new Error('Failed to start duplicate scan');
  const data = await response.json();
  return data.sessionId;
}

/**
 * Gets the status of a duplicate scan.
 */
export async function getScanStatus(sessionId: string): Promise<DuplicateScanStatus> {
  const response = await fetch(`${API_BASE}/scan/${sessionId}`);
  if (!response.ok) throw new Error('Failed to get scan status');
  return response.json();
}

/**
 * Cancels a running duplicate scan.
 */
export async function cancelScan(sessionId: string): Promise<boolean> {
  const response = await fetch(`${API_BASE}/scan/${sessionId}/cancel`, { method: 'POST' });
  if (!response.ok) throw new Error('Failed to cancel scan');
  const data = await response.json();
  return data.cancelled;
}

/**
 * Invalidates the duplicate cache and forces a refresh.
 */
export async function refreshDuplicates(): Promise<void> {
  const response = await fetch(`${API_BASE}/refresh`, { method: 'POST' });
  if (!response.ok) throw new Error('Failed to refresh duplicates');
}

/**
 * Keeps one file from a duplicate group and deletes the rest.
 */
export async function keepFileDeleteOthers(
  groupId: number,
  keepFileId: number
): Promise<{ keptFileId: number; deletedCount: number }> {
  const response = await fetch(`${API_BASE}/${groupId}/keep/${keepFileId}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Failed to process duplicate group');
  return response.json();
}

/**
 * Deletes a single file from the collection.
 */
export async function deleteFile(fileId: number): Promise<{ deletedFileId: number; success: boolean }> {
  const response = await fetch(`${API_BASE}/file/${fileId}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Failed to delete file');
  return response.json();
}

/**
 * Automatically resolves duplicates based on bitrate, metadata, and path matching.
 * Returns files that couldn't be auto-resolved (need manual review).
 */
export async function autoResolveDuplicates(): Promise<AutoResolutionResult> {
  const response = await fetch(`${API_BASE}/auto-resolve`, {
    method: 'POST',
  });
  if (!response.ok) throw new Error('Failed to auto-resolve duplicates');
  return response.json();
}

/**
 * Previews automatic duplicate resolution without deleting any files.
 * Shows what files would be deleted and which would be kept.
 */
export async function previewAutoResolution(): Promise<AutoResolutionPreview> {
  const response = await fetch(`${API_BASE}/auto-resolve/preview`);
  if (!response.ok) throw new Error('Failed to preview auto-resolution');
  return response.json();
}

/**
 * Executes automatic duplicate resolution with optional file exclusions.
 * Files in the exclude list will be kept even if they would normally be deleted.
 */
export async function executeAutoResolution(excludeFileIds?: number[]): Promise<AutoResolutionResult> {
  const response = await fetch(`${API_BASE}/auto-resolve/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ excludeFileIds: excludeFileIds || [] }),
  });
  if (!response.ok) throw new Error('Failed to execute auto-resolution');
  return response.json();
}

/**
 * Opens the folder containing a file in the OS file manager (Finder, Explorer, etc.).
 */
export async function openFileFolder(filePath: string): Promise<{ success: boolean; folder?: string; error?: string }> {
  const response = await fetch(`${API_BASE}/open-folder`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ filePath }),
  });
  return response.json();
}
