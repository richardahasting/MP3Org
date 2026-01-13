import { useState, useCallback, useMemo, useRef } from 'react';
import { useMusicFiles } from '../../hooks/useMusicFiles';
import type { MusicFile } from '../../types/music';
import { updateMusicFile, deleteMusicFile, bulkUpdateMusicFiles, getAudioStreamUrl } from '../../api/musicApi';
import HelpModal, { HelpButton } from '../common/HelpModal';
import { metadataHelp } from '../common/helpContent';

type SearchField = 'all' | 'title' | 'artist' | 'album';

interface BulkEditForm {
  artist: string;
  album: string;
  genre: string;
  year: string;
}

interface ValueSuggestion {
  value: string;
  count: number;
}

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

  // Bulk selection state
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showBulkEdit, setShowBulkEdit] = useState(false);
  const [bulkEditForm, setBulkEditForm] = useState<BulkEditForm>({
    artist: '',
    album: '',
    genre: '',
    year: '',
  });
  const [bulkEditLoading, setBulkEditLoading] = useState(false);

  // Compute suggestions for bulk edit fields based on selected files
  const bulkEditSuggestions = useMemo(() => {
    const selectedFiles = files.filter(f => selectedIds.has(f.id));

    const countValues = (getter: (f: MusicFile) => string | number | null): ValueSuggestion[] => {
      const counts = new Map<string, number>();
      for (const file of selectedFiles) {
        const val = getter(file);
        if (val !== null && val !== undefined && val !== '') {
          const strVal = String(val);
          counts.set(strVal, (counts.get(strVal) || 0) + 1);
        }
      }
      return Array.from(counts.entries())
        .map(([value, count]) => ({ value, count }))
        .sort((a, b) => b.count - a.count);
    };

    return {
      artist: countValues(f => f.artist),
      album: countValues(f => f.album),
      genre: countValues(f => f.genre),
      year: countValues(f => f.year),
    };
  }, [files, selectedIds]);

  // Audio player state
  const [playingFile, setPlayingFile] = useState<MusicFile | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  // Help modal state
  const [showHelp, setShowHelp] = useState(false);

  // Computed values for selection
  const allSelected = useMemo(() =>
    files.length > 0 && files.every(f => selectedIds.has(f.id)),
    [files, selectedIds]
  );

  const someSelected = useMemo(() =>
    files.some(f => selectedIds.has(f.id)),
    [files, selectedIds]
  );

  const handleSearch = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    search(localQuery, localField);
    setSelectedIds(new Set()); // Clear selection on new search
  }, [localQuery, localField, search]);

  const handleClear = useCallback(() => {
    setLocalQuery('');
    setLocalField('all');
    clearSearch();
    setSelectedIds(new Set());
  }, [clearSearch]);

  const startEdit = useCallback((file: MusicFile) => {
    setEditingId(file.id);
    setEditForm({
      title: file.title ?? '',
      artist: file.artist ?? '',
      album: file.album ?? '',
      genre: file.genre ?? '',
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
      setSelectedIds(prev => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
      refresh();
    } catch (err) {
      console.error('Failed to delete:', err);
    }
  }, [refresh]);

  // Selection handlers
  const toggleSelect = useCallback((id: number) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  const toggleSelectAll = useCallback(() => {
    if (allSelected) {
      // Deselect all on current page
      setSelectedIds(prev => {
        const next = new Set(prev);
        files.forEach(f => next.delete(f.id));
        return next;
      });
    } else {
      // Select all on current page
      setSelectedIds(prev => {
        const next = new Set(prev);
        files.forEach(f => next.add(f.id));
        return next;
      });
    }
  }, [allSelected, files]);

  const clearSelection = useCallback(() => {
    setSelectedIds(new Set());
    setShowBulkEdit(false);
  }, []);

  // Bulk edit handlers
  const openBulkEdit = useCallback(() => {
    // Pre-select the most common value for each field (first in sorted list)
    setBulkEditForm({
      artist: bulkEditSuggestions.artist[0]?.value ?? '',
      album: bulkEditSuggestions.album[0]?.value ?? '',
      genre: bulkEditSuggestions.genre[0]?.value ?? '',
      year: bulkEditSuggestions.year[0]?.value ?? '',
    });
    setShowBulkEdit(true);
  }, [bulkEditSuggestions]);

  const closeBulkEdit = useCallback(() => {
    setShowBulkEdit(false);
  }, []);

  const handleBulkUpdate = useCallback(async () => {
    if (selectedIds.size === 0) return;

    const updates: { artist?: string; album?: string; genre?: string; year?: number } = {};
    if (bulkEditForm.artist.trim()) {
      updates.artist = bulkEditForm.artist.trim();
    }
    if (bulkEditForm.album.trim()) {
      updates.album = bulkEditForm.album.trim();
    }
    if (bulkEditForm.genre.trim()) {
      updates.genre = bulkEditForm.genre.trim();
    }
    if (bulkEditForm.year.trim()) {
      const yearNum = parseInt(bulkEditForm.year.trim(), 10);
      if (!isNaN(yearNum) && yearNum > 0) {
        updates.year = yearNum;
      }
    }

    if (Object.keys(updates).length === 0) {
      alert('Please provide at least one value to update.');
      return;
    }

    try {
      setBulkEditLoading(true);
      const result = await bulkUpdateMusicFiles(Array.from(selectedIds), updates);
      alert(`Successfully updated ${result.updated} files.`);
      setShowBulkEdit(false);
      setSelectedIds(new Set());
      refresh();
    } catch (err) {
      console.error('Bulk update failed:', err);
      alert('Bulk update failed. Please try again.');
    } finally {
      setBulkEditLoading(false);
    }
  }, [selectedIds, bulkEditForm, refresh]);

  // Audio playback handlers
  const playFile = useCallback((file: MusicFile) => {
    if (playingFile?.id === file.id) {
      // Toggle play/pause for same file
      if (audioRef.current) {
        if (isPlaying) {
          audioRef.current.pause();
        } else {
          audioRef.current.play();
        }
      }
    } else {
      // Play new file
      setPlayingFile(file);
      setIsPlaying(true);
      // Audio will auto-play via onLoadedData
    }
  }, [playingFile, isPlaying]);

  const stopPlayback = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    setPlayingFile(null);
    setIsPlaying(false);
  }, []);

  const handleAudioPlay = useCallback(() => setIsPlaying(true), []);
  const handleAudioPause = useCallback(() => setIsPlaying(false), []);
  const handleAudioEnded = useCallback(() => {
    setIsPlaying(false);
  }, []);

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
          {selectedIds.size > 0 && (
            <span className="stat-item selected-count">
              <span className="stat-label">Selected</span>
              <span className="stat-value">{selectedIds.size.toLocaleString()}</span>
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
          <HelpButton onClick={() => setShowHelp(true)} />
        </form>
      </div>

      {/* Bulk Action Bar */}
      {selectedIds.size > 0 && (
        <div className="bulk-action-bar">
          <span className="bulk-count">{selectedIds.size} file{selectedIds.size !== 1 ? 's' : ''} selected</span>
          <div className="bulk-actions">
            <button className="bulk-btn edit" onClick={openBulkEdit}>
              <span className="btn-icon">✎</span> Edit Selected
            </button>
            <button className="bulk-btn clear" onClick={clearSelection}>
              <span className="btn-icon">×</span> Clear Selection
            </button>
          </div>
        </div>
      )}

      {/* Bulk Edit Modal */}
      {showBulkEdit && (
        <div className="modal-overlay" onClick={closeBulkEdit}>
          <div className="bulk-edit-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Edit {selectedIds.size} Files</h3>
              <button type="button" className="modal-close" onClick={closeBulkEdit}>×</button>
            </div>
            <div className="modal-body">
              <p className="modal-description">
                Edit values below. Non-empty fields will be applied to all selected files.
                Clear a field to leave it unchanged.
              </p>

              <div className="bulk-field">
                <label className="bulk-field-label">Artist</label>
                <input
                  type="text"
                  className="bulk-field-input"
                  placeholder="Type or select artist..."
                  list="artist-suggestions"
                  value={bulkEditForm.artist}
                  onChange={(e) => setBulkEditForm(f => ({ ...f, artist: e.target.value }))}
                />
                <datalist id="artist-suggestions">
                  {bulkEditSuggestions.artist.map(s => (
                    <option key={s.value} value={s.value}>
                      {s.value} ({s.count} {s.count === 1 ? 'file' : 'files'})
                    </option>
                  ))}
                </datalist>
                {bulkEditSuggestions.artist.length > 0 && (
                  <div className="bulk-suggestions">
                    {bulkEditSuggestions.artist.slice(0, 3).map(s => (
                      <button
                        key={s.value}
                        type="button"
                        className={`suggestion-chip ${bulkEditForm.artist === s.value ? 'active' : ''}`}
                        onClick={() => setBulkEditForm(f => ({ ...f, artist: s.value }))}
                      >
                        {s.value} <span className="chip-count">({s.count})</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="bulk-field">
                <label className="bulk-field-label">Album</label>
                <input
                  type="text"
                  className="bulk-field-input"
                  placeholder="Type or select album..."
                  list="album-suggestions"
                  value={bulkEditForm.album}
                  onChange={(e) => setBulkEditForm(f => ({ ...f, album: e.target.value }))}
                />
                <datalist id="album-suggestions">
                  {bulkEditSuggestions.album.map(s => (
                    <option key={s.value} value={s.value}>
                      {s.value} ({s.count} {s.count === 1 ? 'file' : 'files'})
                    </option>
                  ))}
                </datalist>
                {bulkEditSuggestions.album.length > 0 && (
                  <div className="bulk-suggestions">
                    {bulkEditSuggestions.album.slice(0, 3).map(s => (
                      <button
                        key={s.value}
                        type="button"
                        className={`suggestion-chip ${bulkEditForm.album === s.value ? 'active' : ''}`}
                        onClick={() => setBulkEditForm(f => ({ ...f, album: s.value }))}
                      >
                        {s.value} <span className="chip-count">({s.count})</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="bulk-field">
                <label className="bulk-field-label">Genre</label>
                <input
                  type="text"
                  className="bulk-field-input"
                  placeholder="Type or select genre..."
                  list="genre-suggestions"
                  value={bulkEditForm.genre}
                  onChange={(e) => setBulkEditForm(f => ({ ...f, genre: e.target.value }))}
                />
                <datalist id="genre-suggestions">
                  {bulkEditSuggestions.genre.map(s => (
                    <option key={s.value} value={s.value}>
                      {s.value} ({s.count} {s.count === 1 ? 'file' : 'files'})
                    </option>
                  ))}
                </datalist>
                {bulkEditSuggestions.genre.length > 0 && (
                  <div className="bulk-suggestions">
                    {bulkEditSuggestions.genre.slice(0, 3).map(s => (
                      <button
                        key={s.value}
                        type="button"
                        className={`suggestion-chip ${bulkEditForm.genre === s.value ? 'active' : ''}`}
                        onClick={() => setBulkEditForm(f => ({ ...f, genre: s.value }))}
                      >
                        {s.value} <span className="chip-count">({s.count})</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="bulk-field">
                <label className="bulk-field-label">Year</label>
                <input
                  type="text"
                  className="bulk-field-input bulk-field-year"
                  placeholder="Type or select year..."
                  list="year-suggestions"
                  value={bulkEditForm.year}
                  onChange={(e) => setBulkEditForm(f => ({ ...f, year: e.target.value }))}
                />
                <datalist id="year-suggestions">
                  {bulkEditSuggestions.year.map(s => (
                    <option key={s.value} value={s.value}>
                      {s.value} ({s.count} {s.count === 1 ? 'file' : 'files'})
                    </option>
                  ))}
                </datalist>
                {bulkEditSuggestions.year.length > 0 && (
                  <div className="bulk-suggestions">
                    {bulkEditSuggestions.year.slice(0, 5).map(s => (
                      <button
                        key={s.value}
                        type="button"
                        className={`suggestion-chip ${bulkEditForm.year === s.value ? 'active' : ''}`}
                        onClick={() => setBulkEditForm(f => ({ ...f, year: s.value }))}
                      >
                        {s.value} <span className="chip-count">({s.count})</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="modal-btn cancel" onClick={closeBulkEdit}>
                Cancel
              </button>
              <button
                type="button"
                className="modal-btn apply"
                onClick={handleBulkUpdate}
                disabled={bulkEditLoading}
              >
                {bulkEditLoading ? 'Updating...' : `Update ${selectedIds.size} Files`}
              </button>
            </div>
          </div>
        </div>
      )}

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
                  <th className="col-select">
                    <input
                      type="checkbox"
                      checked={allSelected}
                      ref={el => {
                        if (el) el.indeterminate = someSelected && !allSelected;
                      }}
                      onChange={toggleSelectAll}
                      title="Select all on this page"
                    />
                  </th>
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
                  <tr
                    key={file.id}
                    className={`${editingId === file.id ? 'editing' : ''} ${selectedIds.has(file.id) ? 'selected' : ''}`}
                  >
                    <td className="cell-select">
                      <input
                        type="checkbox"
                        checked={selectedIds.has(file.id)}
                        onChange={() => toggleSelect(file.id)}
                      />
                    </td>
                    {editingId === file.id ? (
                      <>
                        <td>
                          <input
                            type="text"
                            value={editForm.title ?? ''}
                            onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                            className="edit-input"
                            placeholder="Title"
                            autoFocus
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={editForm.artist ?? ''}
                            onChange={(e) => setEditForm({ ...editForm, artist: e.target.value })}
                            className="edit-input"
                            placeholder="Artist"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={editForm.album ?? ''}
                            onChange={(e) => setEditForm({ ...editForm, album: e.target.value })}
                            className="edit-input"
                            placeholder="Album"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={editForm.genre ?? ''}
                            onChange={(e) => setEditForm({ ...editForm, genre: e.target.value })}
                            className="edit-input"
                            placeholder="Genre"
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            value={editForm.trackNumber ?? ''}
                            onChange={(e) => setEditForm({ ...editForm, trackNumber: e.target.value ? parseInt(e.target.value) : null })}
                            className="edit-input edit-input-small"
                            placeholder="#"
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            value={editForm.year ?? ''}
                            onChange={(e) => setEditForm({ ...editForm, year: e.target.value ? parseInt(e.target.value) : null })}
                            className="edit-input edit-input-small"
                            placeholder="Year"
                          />
                        </td>
                        <td>{file.formattedDuration}</td>
                        <td className="action-cell">
                          <button type="button" className="action-btn save" onClick={saveEdit}>✓</button>
                          <button type="button" className="action-btn cancel" onClick={cancelEdit}>×</button>
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
                          <button
                            className={`action-btn play ${playingFile?.id === file.id && isPlaying ? 'playing' : ''}`}
                            onClick={() => playFile(file)}
                            title={playingFile?.id === file.id && isPlaying ? 'Pause' : 'Play'}
                          >
                            {playingFile?.id === file.id && isPlaying ? '❚❚' : '▶'}
                          </button>
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

      {/* Audio Player */}
      {playingFile && (
        <div className="audio-player-bar">
          <div className="audio-player-info">
            <span className="audio-player-title">{playingFile.title || 'Unknown'}</span>
            <span className="audio-player-artist">{playingFile.artist || 'Unknown Artist'}</span>
          </div>
          <audio
            ref={audioRef}
            src={getAudioStreamUrl(playingFile.id)}
            autoPlay
            onPlay={handleAudioPlay}
            onPause={handleAudioPause}
            onEnded={handleAudioEnded}
            controls
            className="audio-player-controls"
          />
          <button className="audio-player-close" onClick={stopPlayback} title="Stop">
            ✕
          </button>
        </div>
      )}

      <HelpModal
        isOpen={showHelp}
        onClose={() => setShowHelp(false)}
        title="Metadata Editor Help"
        sections={metadataHelp}
      />
    </div>
  );
}
