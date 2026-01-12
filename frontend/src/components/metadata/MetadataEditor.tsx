import { useState, useCallback } from 'react';
import { useMusicFiles } from '../../hooks/useMusicFiles';
import type { MusicFile } from '../../types/music';
import { updateMusicFile, deleteMusicFile } from '../../api/musicApi';

type SearchField = 'all' | 'title' | 'artist' | 'album';

export default function MetadataEditor() {
  const {
    files,
    page,
    totalPages,
    totalElements,
    totalCount,
    loading,
    error,
    searchQuery,
    searchType: _searchType,
    search,
    clearSearch,
    goToPage,
    refresh,
  } = useMusicFiles({ pageSize: 25 });

  const [localQuery, setLocalQuery] = useState('');
  const [localField, setLocalField] = useState<SearchField>('all');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<Partial<MusicFile>>({});

  const handleSearch = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    search(localQuery, localField);
  }, [localQuery, localField, search]);

  const handleClear = useCallback(() => {
    setLocalQuery('');
    setLocalField('all');
    clearSearch();
  }, [clearSearch]);

  const startEdit = useCallback((file: MusicFile) => {
    setEditingId(file.id);
    setEditForm({
      title: file.title,
      artist: file.artist,
      album: file.album,
      genre: file.genre,
      trackNumber: file.trackNumber,
      year: file.year,
    });
  }, []);

  const cancelEdit = useCallback(() => {
    setEditingId(null);
    setEditForm({});
  }, []);

  const saveEdit = useCallback(async () => {
    if (editingId === null) return;
    try {
      await updateMusicFile(editingId, editForm);
      setEditingId(null);
      setEditForm({});
      refresh();
    } catch (err) {
      console.error('Failed to save:', err);
    }
  }, [editingId, editForm, refresh]);

  const handleDelete = useCallback(async (id: number) => {
    if (!confirm('Are you sure you want to delete this file from the database?')) return;
    try {
      await deleteMusicFile(id);
      refresh();
    } catch (err) {
      console.error('Failed to delete:', err);
    }
  }, [refresh]);

  const renderPagination = () => {
    if (totalPages <= 1) return null;

    const pages: (number | string)[] = [];
    const showPages = 5;
    let start = Math.max(0, page - Math.floor(showPages / 2));
    let end = Math.min(totalPages, start + showPages);

    if (end - start < showPages) {
      start = Math.max(0, end - showPages);
    }

    if (start > 0) {
      pages.push(0);
      if (start > 1) pages.push('...');
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }

    if (end < totalPages) {
      if (end < totalPages - 1) pages.push('...');
      pages.push(totalPages - 1);
    }

    return (
      <div className="pagination">
        <button
          className="pagination-btn"
          onClick={() => goToPage(page - 1)}
          disabled={page === 0}
        >
          ← Prev
        </button>
        <div className="pagination-pages">
          {pages.map((p, i) =>
            typeof p === 'string' ? (
              <span key={`ellipsis-${i}`} className="pagination-ellipsis">{p}</span>
            ) : (
              <button
                key={p}
                className={`pagination-page ${page === p ? 'active' : ''}`}
                onClick={() => goToPage(p)}
              >
                {p + 1}
              </button>
            )
          )}
        </div>
        <button
          className="pagination-btn"
          onClick={() => goToPage(page + 1)}
          disabled={page >= totalPages - 1}
        >
          Next →
        </button>
      </div>
    );
  };

  return (
    <div className="metadata-editor">
      <div className="metadata-header">
        <div className="metadata-stats">
          <span className="stat-item">
            <span className="stat-label">Total Files</span>
            <span className="stat-value">{totalCount.toLocaleString()}</span>
          </span>
          {searchQuery && (
            <span className="stat-item">
              <span className="stat-label">Results</span>
              <span className="stat-value">{totalElements.toLocaleString()}</span>
            </span>
          )}
        </div>

        <form className="search-form" onSubmit={handleSearch}>
          <div className="search-field-select">
            <select
              value={localField}
              onChange={(e) => setLocalField(e.target.value as SearchField)}
              className="field-select"
            >
              <option value="all">All Fields</option>
              <option value="title">Title</option>
              <option value="artist">Artist</option>
              <option value="album">Album</option>
            </select>
          </div>
          <div className="search-input-wrapper">
            <input
              type="text"
              value={localQuery}
              onChange={(e) => setLocalQuery(e.target.value)}
              placeholder="Search your collection..."
              className="search-input"
            />
            {(localQuery || searchQuery) && (
              <button type="button" className="search-clear" onClick={handleClear}>
                ×
              </button>
            )}
          </div>
          <button type="submit" className="search-btn">
            Search
          </button>
        </form>
      </div>

      {error && (
        <div className="error-message">
          <span className="error-icon">⚠</span>
          {error}
        </div>
      )}

      {loading ? (
        <div className="loading-state">
          <div className="loading-spinner">◎</div>
          <p>Loading your collection...</p>
        </div>
      ) : files.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">♫</div>
          <h3>No music files found</h3>
          <p>Try adjusting your search or import some music files.</p>
        </div>
      ) : (
        <>
          <div className="table-container">
            <table className="music-table">
              <thead>
                <tr>
                  <th className="col-title">Title</th>
                  <th className="col-artist">Artist</th>
                  <th className="col-album">Album</th>
                  <th className="col-genre">Genre</th>
                  <th className="col-track">#</th>
                  <th className="col-year">Year</th>
                  <th className="col-duration">Duration</th>
                  <th className="col-actions">Actions</th>
                </tr>
              </thead>
              <tbody>
                {files.map((file) => (
                  <tr key={file.id} className={editingId === file.id ? 'editing' : ''}>
                    {editingId === file.id ? (
                      <>
                        <td>
                          <input
                            type="text"
                            value={editForm.title || ''}
                            onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                            className="edit-input"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={editForm.artist || ''}
                            onChange={(e) => setEditForm({ ...editForm, artist: e.target.value })}
                            className="edit-input"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={editForm.album || ''}
                            onChange={(e) => setEditForm({ ...editForm, album: e.target.value })}
                            className="edit-input"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={editForm.genre || ''}
                            onChange={(e) => setEditForm({ ...editForm, genre: e.target.value })}
                            className="edit-input"
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            value={editForm.trackNumber || ''}
                            onChange={(e) => setEditForm({ ...editForm, trackNumber: e.target.value ? parseInt(e.target.value) : null })}
                            className="edit-input edit-input-small"
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            value={editForm.year || ''}
                            onChange={(e) => setEditForm({ ...editForm, year: e.target.value ? parseInt(e.target.value) : null })}
                            className="edit-input edit-input-small"
                          />
                        </td>
                        <td>{file.formattedDuration}</td>
                        <td className="action-cell">
                          <button className="action-btn save" onClick={saveEdit}>✓</button>
                          <button className="action-btn cancel" onClick={cancelEdit}>×</button>
                        </td>
                      </>
                    ) : (
                      <>
                        <td className="cell-title" title={file.filePath}>{file.title || '—'}</td>
                        <td className="cell-artist">{file.artist || '—'}</td>
                        <td className="cell-album">{file.album || '—'}</td>
                        <td className="cell-genre">{file.genre || '—'}</td>
                        <td className="cell-track">{file.trackNumber || '—'}</td>
                        <td className="cell-year">{file.year || '—'}</td>
                        <td className="cell-duration">{file.formattedDuration}</td>
                        <td className="action-cell">
                          <button className="action-btn edit" onClick={() => startEdit(file)}>✎</button>
                          <button className="action-btn delete" onClick={() => handleDelete(file.id)}>✕</button>
                        </td>
                      </>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {renderPagination()}
        </>
      )}
    </div>
  );
}
