import { useState, useEffect, useCallback, useRef } from 'react';
import type { DuplicateGroup, MusicFile, DuplicateScanStatus } from '../../types/music';
import {
  fetchDuplicateGroups,
  getDuplicateCount,
  keepFileDeleteOthers,
  startDuplicateScan,
  getScanStatus,
  refreshDuplicates,
  compareFiles,
} from '../../api/duplicatesApi';
import { getAudioStreamUrl } from '../../api/musicApi';

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
  const [scanStatus, setScanStatus] = useState<DuplicateScanStatus | null>(null);
  const [comparison, setComparison] = useState<ComparisonDetail | null>(null);
  const [duplicateCount, setDuplicateCount] = useState(0);

  // Audio player state
  const [playingFile, setPlayingFile] = useState<MusicFile | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  const loadDuplicates = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [groupsData, count] = await Promise.all([
        fetchDuplicateGroups(),
        getDuplicateCount(),
      ]);
      setGroups(groupsData);
      setDuplicateCount(count);

      // If we had a selected group, try to re-select it
      if (selectedGroup) {
        const updated = groupsData.find(g => g.groupId === selectedGroup.groupId);
        if (updated) {
          setSelectedGroup(updated);
        } else {
          setSelectedGroup(null);
          setSelectedFileId(null);
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load duplicates');
    } finally {
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
      const sessionId = await startDuplicateScan();

      // Poll for status
      const pollStatus = async () => {
        try {
          const status = await getScanStatus(sessionId);
          setScanStatus(status);

          if (status.isComplete || status.isCancelled || status.stage === 'error') {
            setScanning(false);
            if (status.isComplete) {
              await loadDuplicates();
            }
            if (status.error) {
              setError(status.error);
            }
          } else {
            setTimeout(pollStatus, 500);
          }
        } catch (err) {
          setScanning(false);
          setError(err instanceof Error ? err.message : 'Scan failed');
        }
      };

      pollStatus();
    } catch (err) {
      setScanning(false);
      setError(err instanceof Error ? err.message : 'Failed to start scan');
    }
  }, [loadDuplicates]);

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

  const handleSelectFile = useCallback(async (file: MusicFile) => {
    setSelectedFileId(file.id);

    // If there's already a selected group, compare with the first file
    if (selectedGroup && selectedGroup.files.length > 1) {
      const otherFile = selectedGroup.files.find(f => f.id !== file.id);
      if (otherFile) {
        try {
          const result = await compareFiles(file.id, otherFile.id);
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

  const handleKeepFile = useCallback(async (fileId: number) => {
    if (!selectedGroup) return;

    if (!confirm(`Keep this file and delete the ${selectedGroup.fileCount - 1} other duplicate(s)?`)) {
      return;
    }

    try {
      const result = await keepFileDeleteOthers(selectedGroup.groupId, fileId);
      alert(`Kept file and deleted ${result.deletedCount} duplicate(s)`);
      setSelectedGroup(null);
      setSelectedFileId(null);
      setComparison(null);
      await loadDuplicates();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process duplicates');
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
  const playFile = useCallback((file: MusicFile, e: React.MouseEvent) => {
    e.stopPropagation();
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
                  <span className="detail-hint">Click a file to keep it, others will be deleted</span>
                </div>
                <div className="file-list">
                  {selectedGroup.files.map(file => (
                    <div
                      key={file.id}
                      className={`file-item ${selectedFileId === file.id ? 'selected' : ''}`}
                      onClick={() => handleSelectFile(file)}
                    >
                      <div className="file-main">
                        <span className="file-title">{file.title || 'Unknown'}</span>
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
                          onClick={(e) => playFile(file, e)}
                          title={playingFile?.id === file.id && isPlaying ? 'Pause' : 'Play'}
                        >
                          {playingFile?.id === file.id && isPlaying ? '❚❚' : '▶'}
                        </button>
                        <button
                          className="keep-button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleKeepFile(file.id);
                          }}
                        >
                          Keep This File
                        </button>
                      </div>
                    </div>
                  ))}
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
