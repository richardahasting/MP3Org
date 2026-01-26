import { useState, useEffect, useCallback } from 'react';
import { browseDirectory, createDirectory } from '../../api/scanningApi';
import type { DirectoryEntry } from '../../types/music';

interface DirectoryPickerProps {
  isOpen: boolean;
  onSelect: (path: string) => void;
  onCancel: () => void;
  title?: string;
  initialPath?: string;
}

export default function DirectoryPicker({
  isOpen,
  onSelect,
  onCancel,
  title = 'Select Directory',
  initialPath,
}: DirectoryPickerProps) {
  const [currentPath, setCurrentPath] = useState<string | null>(initialPath || null);
  const [entries, setEntries] = useState<DirectoryEntry[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showNewFolder, setShowNewFolder] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  const loadDirectory = useCallback(async (path: string | null) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await browseDirectory(path || undefined);
      setCurrentPath(response.currentPath);
      setEntries(response.entries);
    } catch (err) {
      setError('Failed to browse directory');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOpen) {
      loadDirectory(initialPath || null);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen, initialPath, loadDirectory]);

  const handleEntryClick = (entry: DirectoryEntry) => {
    if (entry.isDirectory && entry.canRead) {
      loadDirectory(entry.path);
    }
  };

  const handleSelect = () => {
    if (currentPath) {
      onSelect(currentPath);
    }
  };

  const handleCreateFolder = async () => {
    if (!currentPath || !newFolderName.trim()) return;

    setIsCreating(true);
    setError(null);
    try {
      const result = await createDirectory(currentPath, newFolderName.trim());
      if (result.success) {
        // Navigate to the newly created directory
        setNewFolderName('');
        setShowNewFolder(false);
        loadDirectory(result.path);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create folder');
    } finally {
      setIsCreating(false);
    }
  };

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      onCancel();
    }
  }, [onCancel]);

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, handleKeyDown]);

  if (!isOpen) return null;

  return (
    <div className="directory-picker-overlay" onClick={onCancel}>
      <div className="directory-picker-modal" onClick={e => e.stopPropagation()}>
        <div className="directory-picker-header">
          <h3>{title}</h3>
          <button className="directory-picker-close" onClick={onCancel}>&times;</button>
        </div>

        <div className="directory-picker-path">
          <span className="path-label">Current:</span>
          <span className="path-value">{currentPath || 'Loading...'}</span>
          {currentPath && !showNewFolder && (
            <button
              className="new-folder-btn"
              onClick={() => setShowNewFolder(true)}
              title="Create new folder"
            >
              + New Folder
            </button>
          )}
        </div>

        {showNewFolder && (
          <div className="new-folder-input">
            <input
              type="text"
              placeholder="New folder name"
              value={newFolderName}
              onChange={(e) => setNewFolderName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleCreateFolder();
                if (e.key === 'Escape') {
                  setShowNewFolder(false);
                  setNewFolderName('');
                }
              }}
              autoFocus
              disabled={isCreating}
            />
            <button
              onClick={handleCreateFolder}
              disabled={!newFolderName.trim() || isCreating}
              className="create-btn"
            >
              {isCreating ? 'Creating...' : 'Create'}
            </button>
            <button
              onClick={() => {
                setShowNewFolder(false);
                setNewFolderName('');
              }}
              className="cancel-create-btn"
              disabled={isCreating}
            >
              Cancel
            </button>
          </div>
        )}

        {error && <div className="directory-picker-error">{error}</div>}

        <div className="directory-picker-entries">
          {isLoading ? (
            <div className="directory-picker-loading">Loading...</div>
          ) : (
            entries.map((entry) => (
              <div
                key={entry.path}
                className={`directory-picker-entry ${entry.isDirectory ? 'directory' : 'file'} ${
                  !entry.canRead ? 'disabled' : ''
                }`}
                onClick={() => handleEntryClick(entry)}
              >
                <span className="entry-icon">
                  {entry.name === '..' ? '⬆' : entry.isDirectory ? '📁' : '📄'}
                </span>
                <span className="entry-name">{entry.name}</span>
              </div>
            ))
          )}
        </div>

        <div className="directory-picker-actions">
          <button className="directory-picker-btn cancel" onClick={onCancel}>
            Cancel
          </button>
          <button
            className="directory-picker-btn select"
            onClick={handleSelect}
            disabled={!currentPath}
          >
            Select This Directory
          </button>
        </div>
      </div>
    </div>
  );
}
