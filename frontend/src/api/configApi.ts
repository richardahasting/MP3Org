export interface FuzzySearchConfig {
  configName: string;
  titleSimilarityThreshold: number;
  artistSimilarityThreshold: number;
  albumSimilarityThreshold: number;
  durationToleranceSeconds: number;
  durationTolerancePercent: number;
  ignoreCaseDifferences: boolean;
  ignorePunctuation: boolean;
  wordOrderSensitive: boolean;
  trackNumberMustMatch: boolean;
  ignoreMissingTrackNumber: boolean;
  ignoreArtistPrefixes: boolean;
  ignoreFeaturing: boolean;
  ignoreAlbumEditions: boolean;
  minimumFieldsToMatch: number;
  bitrateToleranceKbps: number;
}

export interface FileTypes {
  allTypes: string[];
  enabledTypes: string[];
}

export interface DatabaseProfile {
  id: string;
  name: string;
  description: string;
  databasePath: string;
  enabledFileTypes: string[];
  createdDate: string | null;
  lastUsedDate: string | null;
  active: boolean;
}

export interface DatabaseInfo {
  databasePath: string;
  jdbcUrl: string;
  jdbcDriver: string;
}

const API_BASE = '/api/v1/config';

// ============= Fuzzy Search Configuration =============

export async function getFuzzySearchConfig(): Promise<FuzzySearchConfig> {
  const response = await fetch(`${API_BASE}/fuzzy-search`);
  if (!response.ok) throw new Error('Failed to get fuzzy search config');
  return response.json();
}

export async function updateFuzzySearchConfig(config: FuzzySearchConfig): Promise<FuzzySearchConfig> {
  const response = await fetch(`${API_BASE}/fuzzy-search`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config),
  });
  if (!response.ok) throw new Error('Failed to update fuzzy search config');
  return response.json();
}

export async function resetFuzzySearchConfig(): Promise<FuzzySearchConfig> {
  const response = await fetch(`${API_BASE}/fuzzy-search/reset`, {
    method: 'POST',
  });
  if (!response.ok) throw new Error('Failed to reset fuzzy search config');
  return response.json();
}

export async function applyPreset(preset: 'strict' | 'balanced' | 'lenient'): Promise<FuzzySearchConfig> {
  const response = await fetch(`${API_BASE}/fuzzy-search/preset`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ preset }),
  });
  if (!response.ok) throw new Error('Failed to apply preset');
  return response.json();
}

// ============= File Types Configuration =============

export async function getFileTypes(): Promise<FileTypes> {
  const response = await fetch(`${API_BASE}/file-types`);
  if (!response.ok) throw new Error('Failed to get file types');
  return response.json();
}

export async function updateFileTypes(enabledTypes: string[]): Promise<FileTypes> {
  const response = await fetch(`${API_BASE}/file-types`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabledTypes }),
  });
  if (!response.ok) throw new Error('Failed to update file types');
  return response.json();
}

// ============= Database Profiles =============

export async function getProfiles(): Promise<DatabaseProfile[]> {
  const response = await fetch(`${API_BASE}/profiles`);
  if (!response.ok) throw new Error('Failed to get profiles');
  return response.json();
}

export async function getActiveProfile(): Promise<DatabaseProfile | null> {
  const response = await fetch(`${API_BASE}/profiles/active`);
  if (response.status === 404) return null;
  if (!response.ok) throw new Error('Failed to get active profile');
  return response.json();
}

export async function createProfile(
  name: string,
  description: string,
  databasePath: string
): Promise<DatabaseProfile> {
  const response = await fetch(`${API_BASE}/profiles`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, databasePath }),
  });
  if (!response.ok) throw new Error('Failed to create profile');
  return response.json();
}

export async function updateProfile(
  id: string,
  name: string,
  description: string,
  databasePath: string
): Promise<DatabaseProfile> {
  const response = await fetch(`${API_BASE}/profiles/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, databasePath }),
  });
  if (!response.ok) throw new Error('Failed to update profile');
  return response.json();
}

export async function deleteProfile(id: string): Promise<boolean> {
  const response = await fetch(`${API_BASE}/profiles/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Failed to delete profile');
  const data = await response.json();
  return data.success;
}

export async function activateProfile(id: string): Promise<DatabaseProfile> {
  const response = await fetch(`${API_BASE}/profiles/${id}/activate`, {
    method: 'POST',
  });
  if (!response.ok) throw new Error('Failed to activate profile');
  return response.json();
}

export async function duplicateProfile(id: string, newName: string): Promise<DatabaseProfile> {
  const response = await fetch(`${API_BASE}/profiles/${id}/duplicate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: newName }),
  });
  if (!response.ok) throw new Error('Failed to duplicate profile');
  return response.json();
}

// ============= Database Info =============

export async function getDatabaseInfo(): Promise<DatabaseInfo> {
  const response = await fetch(`${API_BASE}/database`);
  if (!response.ok) throw new Error('Failed to get database info');
  return response.json();
}
