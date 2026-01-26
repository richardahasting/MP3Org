import { useState, useEffect, useCallback, useRef } from 'react';
import type { DuplicateGroup, DuplicateFile, MusicFile, DuplicateScanStatus, AutoResolutionResult, AutoResolutionPreview, DirectoryConflict, DirectoryResolutionPreview } from '../../types/music';
import {
  fetchDuplicateGroups,
  deleteFile,
  startDuplicateScan,
  refreshDuplicates,
  compareFiles,
  previewAutoResolution,
  executeAutoResolution,
  openFileFolder,
  getDirectoryConflicts,
  previewDirectoryResolution,
  executeDirectoryResolution,
} from '../../api/duplicatesApi';
import { getAudioStreamUrl } from '../../api/musicApi';
import { useDuplicateWebSocket } from '../../hooks/useDuplicateWebSocket';
import HelpModal, { HelpButton } from '../common/HelpModal';
import ConfirmModal from '../common/ConfirmModal';
import { duplicatesHelp } from '../common/helpContent';

type ViewMode = 'similarity' | 'directory';

interface ComparisonDetail {
  file1: MusicFile;
  file2: MusicFile;
  similarity: number;
  breakdown: string;
}

export default function DuplicateManager() {
  const [groups, setGroups] = useState<DuplicateGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<DuplicateGroup | null>(null);
  const [selectedFileId, setSelectedFileId] = useState<number | null>(null);
  const [scanning, setScanning] = useState(false);
  const [scanSessionId, setScanSessionId] = useState<string | null>(null);
  const [comparison, setComparison] = useState<ComparisonDetail | null>(null);
  const [duplicateCount, setDuplicateCount] = useState(0);

  // Auto-resolution state
  const [autoResolving, setAutoResolving] = useState(false);
  const [autoResolutionResult, setAutoResolutionResult] = useState<AutoResolutionResult | null>(null);

  // Preview mode state
  const [previewMode, setPreviewMode] = useState(false);
  const [preview, setPreview] = useState<AutoResolutionPreview | null>(null);
  const [excludedFileIds, setExcludedFileIds] = useState<Set<number>>(new Set());
  const [swappedItems, setSwappedItems] = useState<Set<string>>(new Set()); // Track swapped items by "groupId-fileId"
  const [showingKeptFile, setShowingKeptFile] = useState<MusicFile | null>(null);

  // Audio player state
  const [playingFile, setPlayingFile] = useState<MusicFile | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  // Help modal state
  const [showHelp, setShowHelp] = useState(false);

  // View mode state (Issue #92)
  const [viewMode, setViewMode] = useState<ViewMode>('similarity');
  const [directoryConflicts, setDirectoryConflicts] = useState<DirectoryConflict[]>([]);
  const [selectedConflict, setSelectedConflict] = useState<DirectoryConflict | null>(null);
  const [preferredDirectory, setPreferredDirectory] = useState<string | null>(null);
  const [directoryPreview, setDirectoryPreview] = useState<DirectoryResolutionPreview | null>(null);
  const [directoryLoading, setDirectoryLoading] = useState(false);
  const [executingDirectoryResolution, setExecutingDirectoryResolution] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Confirmation modal state
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string | React.ReactNode;
    onConfirm: () => void;
    confirmText?: string;
    variant?: 'danger' | 'warning' | 'default';
  }>({ isOpen: false, title: '', message: '', onConfirm: () => {} });

  // WebSocket for progressive duplicate scanning
  const { status: scanStatus } = useDuplicateWebSocket({
    sessionId: scanSessionId,
    onProgress: useCallback((status: DuplicateScanStatus) => {
      if (status.isComplete || status.isCancelled || status.stage === 'error') {
        setScanning(false);
        setScanSessionId(null);
        if (status.error) {
          setError(status.error);
        }
      }
    }, []),
    onGroupsReceived: useCallback((newGroups: DuplicateGroup[], totalFound: number) => {
      setGroups(prev => [...prev, ...newGroups]);
      setDuplicateCount(totalFound);
    }, []),
    onError: useCallback((err: string) => {
      setError(err);
      setScanning(false);
      setScanSessionId(null);
    }, []),
  });

  const loadDuplicates = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Load first page immediately
      const firstPage = await fetchDuplicateGroups(0, 25);
      setGroups(firstPage.groups);
      setDuplicateCount(firstPage.totalGroups);
      setLoading(false); // Show first results immediately

      // Load remaining pages progressively in background
      if (firstPage.hasMore) {
        let currentPage = 1;
        let allGroups = [...firstPage.groups];

        while (currentPage < firstPage.totalPages) {
          const nextPage = await fetchDuplicateGroups(currentPage, 25);
          allGroups = [...allGroups, ...nextPage.groups];
          setGroups(allGroups);
          currentPage++;
        }
      }

      // If we had a selected group, try to re-select it
      if (selectedGroup) {
        setGroups(currentGroups => {
          const updated = currentGroups.find(g => g.groupId === selectedGroup.groupId);
          if (updated) {
            setSelectedGroup(updated);
          } else {
            setSelectedGroup(null);
            setSelectedFileId(null);
          }
          return currentGroups;
        });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load duplicates');
      setLoading(false);
    }
  }, [selectedGroup]);

  useEffect(() => {
    loadDuplicates();
  }, []);

  const handleStartScan = useCallback(async () => {
    try {
      setScanning(true);
      setError(null);
      setGroups([]); // Clear existing groups for fresh scan
      setDuplicateCount(0);
      setSelectedGroup(null);
      setSelectedFileId(null);
      setComparison(null);

      const sessionId = await startDuplicateScan();
      setScanSessionId(sessionId); // WebSocket will handle progress and groups
    } catch (err) {
      setScanning(false);
      setError(err instanceof Error ? err.message : 'Failed to start scan');
    }
  }, []);

  const handleRefresh = useCallback(async () => {
    try {
      await refreshDuplicates();
      await loadDuplicates();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to refresh');
    }
  }, [loadDuplicates]);

  // Directory view handlers (Issue #92)
  const loadDirectoryConflicts = useCallback(async () => {
    try {
      setDirectoryLoading(true);
      setError(null);
      const conflicts = await getDirectoryConflicts();
      setDirectoryConflicts(conflicts);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load directory conflicts');
    } finally {
      setDirectoryLoading(false);
    }
  }, []);

  const handleSelectConflict = useCallback((conflict: DirectoryConflict) => {
    setSelectedConflict(conflict);
    setPreferredDirectory(null);
    setDirectoryPreview(null);
  }, []);

  const handleSelectPreferredDirectory = useCallback(async (directory: string) => {
    if (!selectedConflict) return;

    setPreferredDirectory(directory);

    // Determine which directory to delete from
    const directoryToDelete = directory === selectedConflict.directoryA
      ? selectedConflict.directoryB
      : selectedConflict.directoryA;

    try {
      const preview = await previewDirectoryResolution(directory, directoryToDelete);
      setDirectoryPreview(preview);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to preview resolution');
    }
  }, [selectedConflict]);

  const doExecuteDirectoryResolution = useCallback(async () => {
    if (!directoryPreview) return;

    try {
      setExecutingDirectoryResolution(true);
      const result = await executeDirectoryResolution(
        directoryPreview.directoryToKeep,
        directoryPreview.directoryToDelete
      );

      // Clear selection and reload
      setSelectedConflict(null);
      setPreferredDirectory(null);
      setDirectoryPreview(null);
      await loadDirectoryConflicts();

      setSuccessMessage(`Deleted ${result.filesDeleted} files from ${result.directoryCleared}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to execute resolution');
    } finally {
      setExecutingDirectoryResolution(false);
    }
  }, [directoryPreview, loadDirectoryConflicts]);

  const handleExecuteDirectoryResolution = useCallback(() => {
    if (!selectedConflict || !preferredDirectory || !directoryPreview) return;

    const fileCount = directoryPreview.totalFilesToDelete;
    const dirName = directoryPreview.directoryToDelete.split('/').slice(-2).join('/');

    setConfirmModal({
      isOpen: true,
      title: 'Delete Files',
      message: (
        <>
          <p>Delete <strong>{fileCount} files</strong> from:</p>
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
            {dirName}
          </p>
          <p style={{ marginTop: '1rem', color: 'var(--error)' }}>This cannot be undone.</p>
        </>
      ),
      confirmText: `Delete ${fileCount} Files`,
      variant: 'danger',
      onConfirm: () => {
        setConfirmModal(prev => ({ ...prev, isOpen: false }));
        doExecuteDirectoryResolution();
      }
    });
  }, [selectedConflict, preferredDirectory, directoryPreview, doExecuteDirectoryResolution]);

  // Load directory conflicts when switching to directory view
  useEffect(() => {
    if (viewMode === 'directory' && directoryConflicts.length === 0) {
      loadDirectoryConflicts();
    }
  }, [viewMode, directoryConflicts.length, loadDirectoryConflicts]);

  const handleSelectGroup = useCallback((group: DuplicateGroup) => {
    setSelectedGroup(group);
    setSelectedFileId(null);
    setComparison(null);
  }, []);

  const handleSelectFile = useCallback(async (dupFile: DuplicateFile) => {
    const file = dupFile.file;
    setSelectedFileId(file.id);

    // If there's already a selected group, compare with the first file
    if (selectedGroup && selectedGroup.files.length > 1) {
      const otherDupFile = selectedGroup.files.find(f => f.file.id !== file.id);
      if (otherDupFile) {
        try {
          const result = await compareFiles(file.id, otherDupFile.file.id);
          setComparison({
            file1: result.file1,
            file2: result.file2,
            similarity: result.similarity,
            breakdown: result.breakdown,
          });
        } catch (err) {
          console.error('Failed to compare files:', err);
        }
      }
    }
  }, [selectedGroup]);

  const doDeleteFile = useCallback(async (fileId: number) => {
    if (!selectedGroup) return;

    try {
      await deleteFile(fileId);

      // Update the group locally - remove deleted file
      const remainingFiles = selectedGroup.files.filter(f => f.file.id !== fileId);

      if (remainingFiles.length <= 1) {
        // Group is resolved - no longer a duplicate
        setSelectedGroup(null);
        setSelectedFileId(null);
        setComparison(null);
      } else {
        // Update the selected group with remaining files
        setSelectedGroup({
          ...selectedGroup,
          files: remainingFiles,
          fileCount: remainingFiles.length,
        });
      }

      // Refresh to get updated groups from server
      await loadDuplicates();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete file');
    }
  }, [selectedGroup, loadDuplicates]);

  const handleDeleteFile = useCallback((fileId: number) => {
    if (!selectedGroup) return;

    const fileToDelete = selectedGroup.files.find(f => f.file.id === fileId);
    const fileName = fileToDelete?.file.title || 'this file';

    setConfirmModal({
      isOpen: true,
      title: 'Delete File',
      message: (
        <>
          <p>Delete "<strong>{fileName}</strong>"?</p>
          <p style={{ marginTop: '1rem', color: 'var(--error)' }}>This cannot be undone.</p>
        </>
      ),
      confirmText: 'Delete',
      variant: 'danger',
      onConfirm: () => {
        setConfirmModal(prev => ({ ...prev, isOpen: false }));
        doDeleteFile(fileId);
      }
    });
  }, [selectedGroup, doDeleteFile]);

  // Opens preview mode - shows what files will be deleted
  const handleAutoResolve = useCallback(async () => {
    try {
      setAutoResolving(true);
      setError(null);

      const previewResult = await previewAutoResolution();
      setPreview(previewResult);
      setPreviewMode(true);
      setExcludedFileIds(new Set());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to preview auto-resolution');
    } finally {
      setAutoResolving(false);
    }
  }, []);

  // Toggle whether a file should be excluded from deletion
  const toggleExcludeFile = useCallback((fileId: number) => {
    setExcludedFileIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(fileId)) {
        newSet.delete(fileId);
      } else {
        newSet.add(fileId);
      }
      return newSet;
    });
  }, []);

  // Toggle swap for an item (swap which file gets deleted vs kept)
  const toggleSwapItem = useCallback((groupId: number, fileId: number) => {
    const key = `${groupId}-${fileId}`;
    setSwappedItems(prev => {
      const newSet = new Set(prev);
      if (newSet.has(key)) {
        newSet.delete(key);
      } else {
        newSet.add(key);
      }
      return newSet;
    });
  }, []);

  // Helper to check if an item is swapped
  const isItemSwapped = useCallback((groupId: number, fileId: number) => {
    return swappedItems.has(`${groupId}-${fileId}`);
  }, [swappedItems]);

  // Execute the resolution (actually delete files)
  const doExecuteResolution = useCallback(async () => {
    if (!preview) return;

    try {
      setAutoResolving(true);
      setError(null);

      // For swapped items: exclude the original fileToDelete (don't delete it)
      // and we'll manually delete the original fileToKeep
      const swappedResolutions = preview.resolutions.filter(r =>
        swappedItems.has(`${r.groupId}-${r.fileToDelete.id}`) && !excludedFileIds.has(r.fileToDelete.id)
      );

      // Build exclusion list: user exclusions + swapped items' original fileToDelete
      const allExclusions = new Set(excludedFileIds);
      swappedResolutions.forEach(r => allExclusions.add(r.fileToDelete.id));

      // Execute main auto-resolution
      const result = await executeAutoResolution(Array.from(allExclusions));

      // Now delete the swapped items' original fileToKeep
      for (const item of swappedResolutions) {
        try {
          await deleteFile(item.fileToKeep.id);
        } catch (err) {
          console.error(`Failed to delete swapped file ${item.fileToKeep.id}:`, err);
        }
      }

      setAutoResolutionResult(result);
      setPreviewMode(false);
      setPreview(null);
      setExcludedFileIds(new Set());
      setSwappedItems(new Set());

      // If there are "hold my hand" groups, show them
      if (result.holdMyHandGroups.length > 0) {
        setGroups(result.holdMyHandGroups);
        setDuplicateCount(result.holdMyHandGroups.length);
      } else {
        // All resolved, refresh the list
        await loadDuplicates();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to execute auto-resolution');
    } finally {
      setAutoResolving(false);
    }
  }, [preview, excludedFileIds, swappedItems, loadDuplicates]);

  const handleExecuteResolution = useCallback(() => {
    if (!preview) return;

    // Calculate effective deletes accounting for exclusions (swaps still result in one deletion)
    const effectiveDeletes = preview.resolutions.filter(r => {
      return !excludedFileIds.has(r.fileToDelete.id);
    }).length;

    setConfirmModal({
      isOpen: true,
      title: 'Auto-Resolve Duplicates',
      message: (
        <>
          <p>This will permanently delete <strong>{effectiveDeletes} file(s)</strong>.</p>
          <p style={{ marginTop: '1rem', color: 'var(--error)' }}>This cannot be undone.</p>
        </>
      ),
      confirmText: `Delete ${effectiveDeletes} Files`,
      variant: 'danger',
      onConfirm: () => {
        setConfirmModal(prev => ({ ...prev, isOpen: false }));
        doExecuteResolution();
      }
    });
  }, [preview, excludedFileIds, doExecuteResolution]);

  // Cancel preview mode
  const handleCancelPreview = useCallback(() => {
    setPreviewMode(false);
    setPreview(null);
    setExcludedFileIds(new Set());
    setSwappedItems(new Set());
    setShowingKeptFile(null);
  }, []);

  // Open file's folder in OS file manager
  const handleOpenFolder = useCallback(async (filePath: string) => {
    try {
      const result = await openFileFolder(filePath);
      if (!result.success) {
        setError(result.error || 'Failed to open folder');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to open folder');
    }
  }, []);

  // Show the file being kept (for comparison)
  const handleShowKeptFile = useCallback((file: MusicFile) => {
    setShowingKeptFile(showingKeptFile?.id === file.id ? null : file);
  }, [showingKeptFile]);

  const clearAutoResolutionResult = useCallback(() => {
    setAutoResolutionResult(null);
    loadDuplicates();
  }, [loadDuplicates]);

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  // Audio playback handlers
  const playFile = useCallback((dupFile: DuplicateFile, e: React.MouseEvent) => {
    e.stopPropagation();
    const file = dupFile.file;
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
    }
  }, [playingFile, isPlaying]);

  // Play a MusicFile directly (used in preview panel)
  const playMusicFile = useCallback((file: MusicFile) => {
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
  const handleAudioEnded = useCallback(() => setIsPlaying(false), []);

  return (
    <div className="duplicate-manager">
      <div className="duplicate-header">
        <div className="duplicate-stats">
          <span className="stat-item">
            <span className="stat-label">Duplicate Groups</span>
            <span className="stat-value">{duplicateCount}</span>
          </span>
          <span className="stat-item">
            <span className="stat-label">Total Duplicates</span>
            <span className="stat-value">
              {groups.reduce((acc, g) => acc + g.fileCount, 0)}
            </span>
          </span>
        </div>

        <div className="duplicate-actions">
          <button
            className="action-button scan"
            onClick={handleStartScan}
            disabled={scanning}
          >
            {scanning ? 'Scanning...' : 'Scan for Duplicates'}
          </button>
          <button
            className="action-button auto-resolve"
            onClick={handleAutoResolve}
            disabled={loading || scanning || autoResolving || groups.length === 0}
          >
            {autoResolving ? 'Resolving...' : 'Auto-Resolve'}
          </button>
          <button
            className="action-button refresh"
            onClick={handleRefresh}
            disabled={loading || scanning}
          >
            Refresh
          </button>
          <HelpButton onClick={() => setShowHelp(true)} />
        </div>
      </div>

      {/* View Mode Toggle */}
      <div className="view-mode-toggle">
        <span className="view-mode-label">View:</span>
        <button
          className={`view-mode-btn ${viewMode === 'similarity' ? 'active' : ''}`}
          onClick={() => setViewMode('similarity')}
        >
          By Similarity
        </button>
        <button
          className={`view-mode-btn ${viewMode === 'directory' ? 'active' : ''}`}
          onClick={() => setViewMode('directory')}
        >
          By Directory
        </button>
      </div>

      {scanning && scanStatus && (
        <div className="scan-progress">
          <div className="scan-progress-bar">
            <div
              className="scan-progress-fill"
              style={{ width: `${scanStatus.percentComplete}%` }}
            />
          </div>
          <div className="scan-progress-text">
            <span>{scanStatus.stage}</span>
            <span>{scanStatus.percentComplete}%</span>
            <span>Groups found: {scanStatus.groupsFound}</span>
          </div>
        </div>
      )}

      {/* Preview Mode Panel */}
      {previewMode && preview && (
        <div className="preview-panel">
          <div className="preview-header">
            <h3>Auto-Resolution Preview</h3>
            <div className="preview-summary">
              <span className="preview-stat">
                <span className="stat-number">{preview.totalFilesToDelete - excludedFileIds.size}</span> files to delete
              </span>
              <span className="preview-stat">
                <span className="stat-number">{preview.totalFilesToKeep}</span> files to keep
              </span>
              {preview.totalGroupsNeedingReview > 0 && (
                <span
                  className="preview-stat hold-my-hand"
                  title="These duplicate groups have no clear winner (same bitrate, metadata, and no matching directory). They cannot be auto-resolved and must be handled manually in the Duplicates view."
                >
                  <span className="stat-number">{preview.totalGroupsNeedingReview}</span> need review
                </span>
              )}
            </div>
          </div>

          <div className="preview-content">
            <div className="preview-list">
              {preview.resolutions.map((item, index) => {
                const isExcluded = excludedFileIds.has(item.fileToDelete.id);
                const isSwapped = isItemSwapped(item.groupId, item.fileToDelete.id);
                // When swapped, the roles reverse
                const deleteFile = isSwapped ? item.fileToKeep : item.fileToDelete;
                const keepFile = isSwapped ? item.fileToDelete : item.fileToKeep;

                return (
                  <div
                    key={`${item.fileToDelete.id}-${index}`}
                    className={`preview-item ${isExcluded ? 'excluded' : ''} ${isSwapped ? 'swapped' : ''}`}
                  >
                    <div className="preview-item-header">
                      <label className="exclude-checkbox">
                        <input
                          type="checkbox"
                          checked={isExcluded}
                          onChange={() => toggleExcludeFile(item.fileToDelete.id)}
                        />
                        <span className="checkbox-label">Keep both</span>
                      </label>
                      <button
                        className={`swap-btn ${isSwapped ? 'swapped' : ''}`}
                        onClick={() => toggleSwapItem(item.groupId, item.fileToDelete.id)}
                        title="Swap which file to delete vs keep"
                        disabled={isExcluded}
                      >
                        ⇄ Swap
                      </button>
                      <span className="resolution-reason">{isSwapped ? 'User swapped' : item.reason}</span>
                    </div>

                    <div className="preview-file-info">
                      <div className="file-to-delete">
                        <div className="file-header-row">
                          <span className="file-label">Will delete:</span>
                          <button
                            className={`preview-play-btn ${playingFile?.id === deleteFile.id && isPlaying ? 'playing' : ''}`}
                            onClick={() => playMusicFile(deleteFile)}
                            title={playingFile?.id === deleteFile.id && isPlaying ? 'Pause' : 'Play'}
                          >
                            {playingFile?.id === deleteFile.id && isPlaying ? '❚❚' : '▶'}
                          </button>
                          {item.similarity !== null && (
                            <span className={`similarity-badge ${item.similarity >= 0.95 ? 'high' : item.similarity >= 0.85 ? 'medium' : 'low'}`}>
                              {(item.similarity * 100).toFixed(0)}% match
                            </span>
                          )}
                        </div>
                        <span className="file-path" title={deleteFile.filePath}>
                          {deleteFile.filePath}
                        </span>
                        <div className="file-meta-inline">
                          <span>{deleteFile.bitRate} kbps</span>
                          <span>{formatFileSize(deleteFile.fileSizeBytes)}</span>
                        </div>
                      </div>

                      <div className="preview-actions">
                        <button
                          className="preview-action-btn"
                          onClick={() => handleOpenFolder(deleteFile.filePath)}
                          title="Open folder in file manager"
                        >
                          Open Folder
                        </button>
                        <button
                          className={`preview-action-btn ${showingKeptFile?.id === keepFile.id ? 'active' : ''}`}
                          onClick={() => handleShowKeptFile(keepFile)}
                          title="Show the file being kept"
                        >
                          {showingKeptFile?.id === keepFile.id ? 'Hide Kept' : 'Show Kept'}
                        </button>
                      </div>
                    </div>

                    {showingKeptFile?.id === keepFile.id && (
                      <div className="kept-file-info">
                        <div className="file-header-row">
                          <span className="file-label">Keeping:</span>
                          <button
                            className={`preview-play-btn ${playingFile?.id === keepFile.id && isPlaying ? 'playing' : ''}`}
                            onClick={() => playMusicFile(keepFile)}
                            title={playingFile?.id === keepFile.id && isPlaying ? 'Pause' : 'Play'}
                          >
                            {playingFile?.id === keepFile.id && isPlaying ? '❚❚' : '▶'}
                          </button>
                        </div>
                        <span className="file-path" title={keepFile.filePath}>
                          {keepFile.filePath}
                        </span>
                        <div className="file-meta-inline">
                          <span>{keepFile.bitRate} kbps</span>
                          <span>{formatFileSize(keepFile.fileSizeBytes)}</span>
                        </div>
                        <button
                          className="preview-action-btn"
                          onClick={() => handleOpenFolder(keepFile.filePath)}
                          title="Open folder in file manager"
                        >
                          Open Folder
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}

              {preview.resolutions.length === 0 && (
                <div className="preview-empty">
                  No files to auto-resolve. All duplicates require manual review.
                </div>
              )}
            </div>
          </div>

          <div className="preview-footer">
            <button
              className="action-button cancel"
              onClick={handleCancelPreview}
            >
              Cancel
            </button>
            <button
              className="action-button execute"
              onClick={handleExecuteResolution}
              disabled={autoResolving || preview.resolutions.length === 0 ||
                        preview.resolutions.every(r => excludedFileIds.has(r.fileToDelete.id))}
            >
              {autoResolving ? 'Deleting...' : `Delete ${preview.totalFilesToDelete - excludedFileIds.size} Files`}
            </button>
          </div>
        </div>
      )}

      {autoResolutionResult && (
        <div className="auto-resolution-result">
          <div className="result-summary">
            <span className="result-icon">&#10003;</span>
            <span className="result-text">{autoResolutionResult.summary}</span>
          </div>
          <div className="result-stats">
            <span className="result-stat">
              <span className="stat-number">{autoResolutionResult.filesDeleted}</span> deleted
            </span>
            <span className="result-stat">
              <span className="stat-number">{autoResolutionResult.filesKept}</span> kept
            </span>
            {autoResolutionResult.holdMyHandGroups.length > 0 && (
              <span className="result-stat hold-my-hand">
                <span className="stat-number">{autoResolutionResult.holdMyHandGroups.length}</span> need review
              </span>
            )}
          </div>
          <button className="dismiss-result" onClick={clearAutoResolutionResult}>
            Dismiss
          </button>
        </div>
      )}

      {successMessage && (
        <div className="success-message">
          <span className="success-icon">✓</span>
          {successMessage}
          <button className="dismiss-message" onClick={() => setSuccessMessage(null)}>×</button>
        </div>
      )}

      {error && (
        <div className="error-message">
          <span className="error-icon">!</span>
          {error}
        </div>
      )}

      {/* Similarity View */}
      {viewMode === 'similarity' && (
        <>
          {loading ? (
            <div className="loading-state">
              <div className="loading-spinner">@</div>
              <p>Loading duplicate groups...</p>
            </div>
          ) : groups.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">$</div>
              <h3>No duplicates found</h3>
              <p>Your collection appears to be free of duplicates. Run a scan to check again.</p>
            </div>
          ) : (
            <div className="duplicate-content">
              <div className="duplicate-list">
                <div className="list-header">
                  <h3>Duplicate Groups ({groups.length})</h3>
                </div>
                <div className="group-list">
                  {groups.map(group => (
                    <div
                      key={group.groupId}
                      className={`group-item ${selectedGroup?.groupId === group.groupId ? 'selected' : ''}`}
                      onClick={() => handleSelectGroup(group)}
                    >
                      <div className="group-info">
                        <span className="group-title">{group.representativeTitle || 'Unknown Title'}</span>
                        <span className="group-artist">{group.representativeArtist || 'Unknown Artist'}</span>
                      </div>
                      <span className="group-count">{group.fileCount} files</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="duplicate-detail">
                {selectedGroup ? (
                  <>
                    <div className="detail-header">
                      <h3>Files in Group</h3>
                      <span className="detail-hint">Select a file to see details, delete duplicates you don't want</span>
                    </div>
                    <div className="file-list">
                      {selectedGroup.files.map((dupFile, index) => {
                        const file = dupFile.file;
                        const isReference = index === 0;
                        return (
                          <div
                            key={file.id}
                            className={`file-item ${selectedFileId === file.id ? 'selected' : ''} ${isReference ? 'reference' : ''}`}
                            onClick={() => handleSelectFile(dupFile)}
                          >
                            <div className="file-main">
                              <div className="file-title-row">
                                <span className="file-title">{file.title || 'Unknown'}</span>
                                {dupFile.similarity !== null && (
                                  <span className={`similarity-badge ${dupFile.similarity >= 0.95 ? 'high' : dupFile.similarity >= 0.85 ? 'medium' : 'low'}`}>
                                    {(dupFile.similarity * 100).toFixed(0)}% match
                                  </span>
                                )}
                                {isReference && <span className="reference-badge">Reference</span>}
                              </div>
                              <span className="file-artist">{file.artist || 'Unknown'}</span>
                              <span className="file-album">{file.album || 'Unknown'}</span>
                            </div>
                            <div className="file-meta">
                              <span className="file-duration">{formatDuration(file.durationSeconds)}</span>
                              <span className="file-bitrate">{file.bitRate} kbps</span>
                              <span className="file-size">{formatFileSize(file.fileSizeBytes)}</span>
                            </div>
                            <div className="file-path">
                              {file.filePath}
                            </div>
                            <div className="file-actions">
                              <button
                                className={`play-button ${playingFile?.id === file.id && isPlaying ? 'playing' : ''}`}
                                onClick={(e) => playFile(dupFile, e)}
                                title={playingFile?.id === file.id && isPlaying ? 'Pause' : 'Play'}
                              >
                                {playingFile?.id === file.id && isPlaying ? '❚❚' : '▶'}
                              </button>
                              <button
                                className="delete-button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDeleteFile(file.id);
                                }}
                              >
                                Delete This File
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>

                    {comparison && (
                      <div className="comparison-panel">
                        <h4>Comparison Details</h4>
                        <div className="comparison-similarity">
                          <span className="similarity-label">Similarity:</span>
                          <span className="similarity-value">{(comparison.similarity * 100).toFixed(1)}%</span>
                        </div>
                        <pre className="comparison-breakdown">{comparison.breakdown}</pre>
                      </div>
                    )}
                  </>
                ) : (
                  <div className="detail-placeholder">
                    <div className="placeholder-icon">@</div>
                    <p>Select a duplicate group to see files</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      )}

      {/* Directory View */}
      {viewMode === 'directory' && (
        <>
          {directoryLoading ? (
            <div className="loading-state">
              <div className="loading-spinner">@</div>
              <p>Loading directory conflicts...</p>
            </div>
          ) : directoryConflicts.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">$</div>
              <h3>No directory conflicts found</h3>
              <p>No duplicate files exist across different directories.</p>
            </div>
          ) : (
            <div className="duplicate-content directory-view">
              <div className="duplicate-list">
                <div className="list-header">
                  <h3>Directory Conflicts ({directoryConflicts.length})</h3>
                </div>
                <div className="group-list">
                  {directoryConflicts.map((conflict, index) => (
                    <div
                      key={`${conflict.directoryA}-${conflict.directoryB}-${index}`}
                      className={`group-item directory-conflict-item ${selectedConflict?.directoryA === conflict.directoryA && selectedConflict?.directoryB === conflict.directoryB ? 'selected' : ''}`}
                      onClick={() => handleSelectConflict(conflict)}
                    >
                      <div className="conflict-dirs">
                        <span className="conflict-dir" title={conflict.directoryA}>
                          {conflict.directoryA.split('/').slice(-2).join('/')}
                        </span>
                        <span className="conflict-vs">vs</span>
                        <span className="conflict-dir" title={conflict.directoryB}>
                          {conflict.directoryB.split('/').slice(-2).join('/')}
                        </span>
                      </div>
                      <div className="conflict-stats">
                        <span className="conflict-count">{conflict.totalDuplicatePairs} duplicates</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="duplicate-detail">
                {selectedConflict ? (
                  <div className="directory-conflict-detail">
                    <div className="detail-header">
                      <h3>Choose Directory to Keep</h3>
                      <span className="detail-hint">Select which directory's files you want to keep</span>
                    </div>

                    <div className="directory-choice-panel">
                      <label
                        className={`directory-option ${preferredDirectory === selectedConflict.directoryA ? 'selected' : ''}`}
                        onClick={() => handleSelectPreferredDirectory(selectedConflict.directoryA)}
                      >
                        <input
                          type="radio"
                          name="preferred-directory"
                          checked={preferredDirectory === selectedConflict.directoryA}
                          onChange={() => handleSelectPreferredDirectory(selectedConflict.directoryA)}
                        />
                        <div className="directory-option-content">
                          <span className="directory-path" title={selectedConflict.directoryA}>
                            {selectedConflict.directoryA}
                          </span>
                          <span className="directory-file-count">{selectedConflict.filesInA} files</span>
                        </div>
                      </label>

                      <label
                        className={`directory-option ${preferredDirectory === selectedConflict.directoryB ? 'selected' : ''}`}
                        onClick={() => handleSelectPreferredDirectory(selectedConflict.directoryB)}
                      >
                        <input
                          type="radio"
                          name="preferred-directory"
                          checked={preferredDirectory === selectedConflict.directoryB}
                          onChange={() => handleSelectPreferredDirectory(selectedConflict.directoryB)}
                        />
                        <div className="directory-option-content">
                          <span className="directory-path" title={selectedConflict.directoryB}>
                            {selectedConflict.directoryB}
                          </span>
                          <span className="directory-file-count">{selectedConflict.filesInB} files</span>
                        </div>
                      </label>
                    </div>

                    {directoryPreview && (
                      <div className="directory-preview">
                        <div className="preview-summary-row">
                          <span className="preview-keep">
                            Keeping {directoryPreview.filesToKeep.length} files from {directoryPreview.directoryToKeep.split('/').slice(-2).join('/')}
                          </span>
                          <span className="preview-delete">
                            Deleting {directoryPreview.totalFilesToDelete} files from {directoryPreview.directoryToDelete.split('/').slice(-2).join('/')}
                          </span>
                        </div>

                        <div className="files-to-delete-list">
                          <h4>Files to be deleted:</h4>
                          {directoryPreview.filesToDelete.slice(0, 10).map(file => (
                            <div key={file.id} className="file-to-delete-item">
                              <span className="file-title">{file.title || 'Unknown'}</span>
                              <span className="file-artist">{file.artist || 'Unknown'}</span>
                              <span className="file-meta-inline">
                                <span>{file.bitRate} kbps</span>
                                <span>{formatFileSize(file.fileSizeBytes)}</span>
                              </span>
                            </div>
                          ))}
                          {directoryPreview.filesToDelete.length > 10 && (
                            <div className="more-files">
                              ...and {directoryPreview.filesToDelete.length - 10} more files
                            </div>
                          )}
                        </div>

                        <button
                          className="action-button execute directory-execute"
                          onClick={handleExecuteDirectoryResolution}
                          disabled={executingDirectoryResolution}
                        >
                          {executingDirectoryResolution
                            ? 'Deleting...'
                            : `Delete ${directoryPreview.totalFilesToDelete} Files`}
                        </button>
                      </div>
                    )}

                    <div className="duplicate-pairs-section">
                      <h4>Duplicate Pairs ({selectedConflict.totalDuplicatePairs})</h4>
                      <div className="pairs-list">
                        {selectedConflict.pairs.slice(0, 20).map((pair, idx) => (
                          <div key={`${pair.fileA.id}-${pair.fileB.id}-${idx}`} className="pair-item">
                            <div className="pair-file">
                              <span className="pair-title">{pair.fileA.title || 'Unknown'}</span>
                              <span className="pair-meta">
                                {pair.fileA.bitRate} kbps · {formatFileSize(pair.fileA.fileSizeBytes)}
                              </span>
                              <span className="pair-path" title={pair.fileA.filePath}>
                                {pair.fileA.filePath.split('/').slice(-1)[0]}
                              </span>
                            </div>
                            {pair.similarity !== null && (
                              <span className={`similarity-badge ${pair.similarity >= 0.95 ? 'high' : pair.similarity >= 0.85 ? 'medium' : 'low'}`}>
                                {(pair.similarity * 100).toFixed(0)}%
                              </span>
                            )}
                            <div className="pair-file">
                              <span className="pair-title">{pair.fileB.title || 'Unknown'}</span>
                              <span className="pair-meta">
                                {pair.fileB.bitRate} kbps · {formatFileSize(pair.fileB.fileSizeBytes)}
                              </span>
                              <span className="pair-path" title={pair.fileB.filePath}>
                                {pair.fileB.filePath.split('/').slice(-1)[0]}
                              </span>
                            </div>
                          </div>
                        ))}
                        {selectedConflict.pairs.length > 20 && (
                          <div className="more-pairs">
                            ...and {selectedConflict.pairs.length - 20} more pairs
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="detail-placeholder">
                    <div className="placeholder-icon">@</div>
                    <p>Select a directory conflict to see details</p>
                  </div>
                )}
              </div>
            </div>
          )}
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
        title="Duplicate Manager Help"
        sections={duplicatesHelp}
      />

      <ConfirmModal
        isOpen={confirmModal.isOpen}
        onConfirm={confirmModal.onConfirm}
        onCancel={() => setConfirmModal(prev => ({ ...prev, isOpen: false }))}
        title={confirmModal.title}
        message={confirmModal.message}
        confirmText={confirmModal.confirmText}
        variant={confirmModal.variant}
      />
    </div>
  );
}
