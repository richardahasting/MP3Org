export interface OrganizationPreview {
  id: number;
  currentPath: string | null;
  proposedPath: string | null;
  title: string | null;
  artist: string | null;
  album: string | null;
  valid: boolean;
  error: string | null;
}

export interface PreviewAllResponse {
  previews: OrganizationPreview[];
  totalCount: number;
  page: number;
  totalPages: number;
}

export interface OrganizationResult {
  successCount: number;
  failureCount: number;
  errors: string[];
}

export interface TextFormat {
  name: string;
  description: string;
}

export interface TemplatesResponse {
  examples: string[];
  default: string;
}

const API_BASE = '/api/v1/organization';

export async function previewOrganization(
  fileIds: number[],
  basePath: string,
  template?: string,
  textFormat?: string,
  useSubdirectories?: boolean,
  subdirectoryLevels?: number
): Promise<OrganizationPreview[]> {
  const response = await fetch(`${API_BASE}/preview`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fileIds,
      basePath,
      template,
      textFormat,
      useSubdirectories,
      subdirectoryLevels,
    }),
  });
  if (!response.ok) throw new Error('Preview failed');
  return response.json();
}

export interface SearchFilters {
  filterTitle?: string;
  filterArtist?: string;
  filterAlbum?: string;
  filterGenre?: string;
}

export async function previewAllOrganization(
  basePath: string,
  template?: string,
  textFormat?: string,
  useSubdirectories?: boolean,
  subdirectoryLevels?: number,
  page: number = 0,
  size: number = 50,
  filters?: SearchFilters
): Promise<PreviewAllResponse> {
  const response = await fetch(`${API_BASE}/preview-all`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      basePath,
      template,
      textFormat,
      useSubdirectories,
      subdirectoryLevels,
      page,
      size,
      ...filters,
    }),
  });
  if (!response.ok) throw new Error('Preview all failed');
  return response.json();
}

export async function getMatchingIds(filters?: SearchFilters): Promise<{ ids: number[]; count: number }> {
  const response = await fetch(`${API_BASE}/matching-ids`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(filters || {}),
  });
  if (!response.ok) throw new Error('Failed to get matching IDs');
  return response.json();
}

export async function executeOrganization(
  fileIds: number[],
  basePath: string,
  template?: string,
  textFormat?: string,
  useSubdirectories?: boolean,
  subdirectoryLevels?: number
): Promise<OrganizationResult> {
  const response = await fetch(`${API_BASE}/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fileIds,
      basePath,
      template,
      textFormat,
      useSubdirectories,
      subdirectoryLevels,
    }),
  });
  if (!response.ok) throw new Error('Execute failed');
  return response.json();
}

export async function getTemplates(): Promise<TemplatesResponse> {
  const response = await fetch(`${API_BASE}/templates`);
  if (!response.ok) throw new Error('Failed to get templates');
  return response.json();
}

export async function getAvailableFields(): Promise<string[]> {
  const response = await fetch(`${API_BASE}/fields`);
  if (!response.ok) throw new Error('Failed to get fields');
  return response.json();
}

export async function getTextFormats(): Promise<TextFormat[]> {
  const response = await fetch(`${API_BASE}/formats`);
  if (!response.ok) throw new Error('Failed to get formats');
  return response.json();
}
