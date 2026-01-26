import type { BrowseResponse } from '../types/music';

const API_BASE = 'http://localhost:9090/api/v1/scanning';

/**
 * Starts a directory scan.
 * Returns session ID for WebSocket subscription.
 */
export async function startScan(directories: string[]): Promise<{ sessionId: string; message: string }> {
  const response = await fetch(`${API_BASE}/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ directories }),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Failed to start scan');
  }

  return response.json();
}

/**
 * Gets the status of a scanning session.
 */
export async function getScanStatus(sessionId: string): Promise<{
  sessionId: string;
  directories: string[];
  startTime: number;
  filesFound: number;
  cancelled: boolean;
  completed: boolean;
}> {
  const response = await fetch(`${API_BASE}/status/${sessionId}`);

  if (!response.ok) {
    throw new Error('Session not found');
  }

  return response.json();
}

/**
 * Cancels an active scanning session.
 */
export async function cancelScan(sessionId: string): Promise<void> {
  const response = await fetch(`${API_BASE}/cancel/${sessionId}`, {
    method: 'POST',
  });

  if (!response.ok) {
    throw new Error('Failed to cancel scan');
  }
}

/**
 * Gets the list of previously scanned directories.
 */
export async function getScanDirectories(): Promise<string[]> {
  const response = await fetch(`${API_BASE}/directories`);

  if (!response.ok) {
    throw new Error('Failed to get directories');
  }

  return response.json();
}

/**
 * Browses a directory on the server.
 */
export async function browseDirectory(path?: string): Promise<BrowseResponse> {
  const url = path ? `${API_BASE}/browse?path=${encodeURIComponent(path)}` : `${API_BASE}/browse`;
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error('Failed to browse directory');
  }

  return response.json();
}

/**
 * Creates a new directory on the server.
 */
export async function createDirectory(parentPath: string, name: string): Promise<{ success: boolean; path: string }> {
  const response = await fetch(`${API_BASE}/create-directory`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ parentPath, name }),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Failed to create directory');
  }

  return response.json();
}
