import type { MusicFile, PageResponse } from '../types/music';

const API_BASE = '/api/v1/music';

export async function fetchMusicFiles(
  page: number = 0,
  size: number = 50
): Promise<PageResponse<MusicFile>> {
  const response = await fetch(`${API_BASE}?page=${page}&size=${size}`);
  if (!response.ok) throw new Error('Failed to fetch music files');
  return response.json();
}

export async function searchMusicFiles(
  query: string,
  searchType: 'all' | 'title' | 'artist' | 'album' = 'all',
  page: number = 0,
  size: number = 50
): Promise<PageResponse<MusicFile>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });

  if (searchType === 'title') {
    params.set('title', query);
  } else if (searchType === 'artist') {
    params.set('artist', query);
  } else if (searchType === 'album') {
    params.set('album', query);
  } else {
    params.set('q', query);
  }

  const response = await fetch(`${API_BASE}/search?${params}`);
  if (!response.ok) throw new Error('Search failed');
  return response.json();
}

export async function getMusicFileCount(): Promise<number> {
  const response = await fetch(`${API_BASE}/count`);
  if (!response.ok) throw new Error('Failed to get count');
  const data = await response.json();
  return data.count;
}

export async function updateMusicFile(
  id: number,
  updates: Partial<MusicFile>
): Promise<MusicFile> {
  const response = await fetch(`${API_BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(updates),
  });
  if (!response.ok) throw new Error('Update failed');
  return response.json();
}

export async function deleteMusicFile(id: number): Promise<void> {
  const response = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
  if (!response.ok) throw new Error('Delete failed');
}

export async function bulkUpdateMusicFiles(
  ids: number[],
  updates: { artist?: string; album?: string; genre?: string; year?: number }
): Promise<{ updated: number }> {
  const response = await fetch(`${API_BASE}/bulk`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ids, ...updates }),
  });
  if (!response.ok) throw new Error('Bulk update failed');
  return response.json();
}

/**
 * Returns the URL for streaming an audio file.
 * Can be used directly as the src for an HTML5 audio element.
 */
export function getAudioStreamUrl(id: number): string {
  return `${API_BASE}/${id}/stream`;
}
