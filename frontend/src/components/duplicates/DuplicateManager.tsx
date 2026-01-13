import { useState, useEffect, useCallback, useRef } from 'react';
import type { DuplicateGroup, DuplicateFile, MusicFile, DuplicateScanStatus } from '../../types/music';
import {
  fetchDuplicateGroups,
  deleteFile,
  startDuplicateScan,
  refreshDuplicates,
  compareFiles,
} from '../../api/duplicatesApi';
import { getAudioStreamUrl } from '../../api/musicApi';
import { useDuplicateWebSocket } from '../../hooks/useDuplicateWebSocket';

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

  // Audio player state
  const [playingFile, setPlayingFile] = useState<MusicFile | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

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

  const handleDeleteFile = useCallback(async (fileId: number) => {
    if (!selectedGroup) return;

    const fileToDelete = selectedGroup.files.find(f => f.file.id === fileId);
    const fileName = fileToDelete?.file.title || 'this file';

    if (!confirm(`Delete "${fileName}"? This cannot be undone.`)) {
      return;
    }

    try {
      await deleteFile(fileId);

      // If this was the last file in the group, clear selection
      if (selectedGroup.fileCount <= 2) {
        setSelectedGroup(null);
      }
      setSelectedFileId(null);
      setComparison(null);
      await loadDuplicates();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete file');
    }
  }, [selectedGroup, loadDuplicates]);

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
            className="action-button refresh"
            onClick={handleRefresh}
            disabled={loading || scanning}
          >
            Refresh
          </button>
        </div>
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

      {error && (
        <div className="error-message">
          <span className="error-icon">!</span>
          {error}
        </div>
      )}

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
                        <div className="file-path" title={file.filePath}>
                          {file.filePath.split('/').slice(-2).join('/')}
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
    </div>
  );
}
