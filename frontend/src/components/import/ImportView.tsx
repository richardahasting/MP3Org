import { useState, useEffect } from 'react';
import { useWebSocket } from '../../hooks/useWebSocket';
import { startScan, cancelScan, browseDirectory, getScanDirectories } from '../../api/scanningApi';
import type { DirectoryEntry, ScanProgress } from '../../types/music';

/**
 * Import View - Phase 2 of Web UI Migration (Issue #69)
 *
 * Features:
 * - Directory browser for selecting folders to scan
 * - Real-time scanning progress via WebSocket
 * - Previously scanned directory history
 * - Cancel running scans
 */
export default function ImportView() {
  // Directory browser state
  const [currentPath, setCurrentPath] = useState<string | null>(null);
  const [entries, setEntries] = useState<DirectoryEntry[]>([]);
  const [selectedPaths, setSelectedPaths] = useState<Set<string>>(new Set());
  const [previousDirectories, setPreviousDirectories] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [browseError, setBrowseError] = useState<string | null>(null);

  // Scanning state
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanResult, setScanResult] = useState<ScanProgress | null>(null);

  // WebSocket hook for real-time progress
  const { progress, isConnected } = useWebSocket({
    sessionId,
    onProgress: (p) => {
      if (p.isComplete || p.isCancelled || p.error) {
        setIsScanning(false);
        setScanResult(p);
        setSessionId(null);
        // Refresh previously scanned directories
        loadPreviousDirectories();
      }
    },
  });

  // Load initial directory listing
  useEffect(() => {
    loadDirectory(null);
    loadPreviousDirectories();
  }, []);

  const loadDirectory = async (path: string | null) => {
    setIsLoading(true);
    setBrowseError(null);
    try {
      const response = await browseDirectory(path || undefined);
      setCurrentPath(response.currentPath);
      setEntries(response.entries);
    } catch (error) {
      setBrowseError('Failed to browse directory');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadPreviousDirectories = async () => {
    try {
      const dirs = await getScanDirectories();
      setPreviousDirectories(dirs);
    } catch (error) {
      console.error('Failed to load previous directories:', error);
    }
  };

  const handleEntryClick = (entry: DirectoryEntry) => {
    if (entry.isDirectory && entry.canRead) {
      loadDirectory(entry.path);
    }
  };

  const handleSelectPath = (path: string) => {
    setSelectedPaths((prev) => {
      const next = new Set(prev);
      if (next.has(path)) {
        next.delete(path);
      } else {
        next.add(path);
      }
      return next;
    });
  };

  const handleSelectCurrent = () => {
    if (currentPath) {
      setSelectedPaths((prev) => new Set([...prev, currentPath]));
    }
  };

  const handleStartScan = async () => {
    if (selectedPaths.size === 0) return;

    setIsScanning(true);
    setScanResult(null);

    try {
      const result = await startScan(Array.from(selectedPaths));
      setSessionId(result.sessionId);
    } catch (error) {
      setIsScanning(false);
      setScanResult({
        sessionId: '',
        stage: 'error',
        currentDirectory: '',
        currentFile: '',
        filesFound: 0,
        filesProcessed: 0,
        totalDirectories: 0,
        directoriesProcessed: 0,
        percentComplete: 0,
        message: error instanceof Error ? error.message : 'Unknown error',
        isComplete: true,
        isCancelled: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      });
    }
  };

  const handleCancelScan = async () => {
    if (sessionId) {
      try {
        await cancelScan(sessionId);
      } catch (error) {
        console.error('Failed to cancel scan:', error);
      }
    }
  };

  const handleClearSelection = () => {
    setSelectedPaths(new Set());
  };

  const handleRescanDirectory = (dir: string) => {
    setSelectedPaths(new Set([dir]));
  };

  return (
    <div className="import-view">
      <div className="import-header">
        <h2 className="import-title">Import Music Files</h2>
        <p className="import-description">
          Browse directories and select folders to scan for music files
        </p>
      </div>

      <div className="import-content">
        {/* Left Panel - Directory Browser */}
        <div className="browser-panel">
          <div className="browser-header">
            <span className="browser-icon">📁</span>
            <span className="browser-title">Directory Browser</span>
          </div>

          <div className="browser-path">
            <span className="path-label">Current:</span>
            <span className="path-value">{currentPath || 'Select a directory'}</span>
            {currentPath && (
              <button className="select-current-btn" onClick={handleSelectCurrent}>
                + Add to Queue
              </button>
            )}
          </div>

          {browseError && <div className="browser-error">{browseError}</div>}

          <div className="browser-entries">
            {isLoading ? (
              <div className="browser-loading">Loading...</div>
            ) : (
              entries.map((entry) => (
                <div
                  key={entry.path}
                  className={`browser-entry ${entry.isDirectory ? 'directory' : 'file'} ${
                    !entry.canRead ? 'disabled' : ''
                  } ${selectedPaths.has(entry.path) ? 'selected' : ''}`}
                  onClick={() => handleEntryClick(entry)}
                >
                  <span className="entry-icon">
                    {entry.name === '..' ? '⬆' : entry.isDirectory ? '📁' : '📄'}
                  </span>
                  <span className="entry-name">{entry.name}</span>
                  {entry.isDirectory && entry.canRead && entry.name !== '..' && (
                    <button
                      className="entry-add-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleSelectPath(entry.path);
                      }}
                    >
                      {selectedPaths.has(entry.path) ? '✓' : '+'}
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Panel - Scan Queue & Progress */}
        <div className="scan-panel">
          {/* Selection Queue */}
          <div className="queue-section">
            <div className="queue-header">
              <span className="queue-icon">📋</span>
              <span className="queue-title">Scan Queue ({selectedPaths.size})</span>
              {selectedPaths.size > 0 && (
                <button className="clear-queue-btn" onClick={handleClearSelection}>
                  Clear
                </button>
              )}
            </div>

            <div className="queue-list">
              {selectedPaths.size === 0 ? (
                <div className="queue-empty">
                  Select directories from the browser to add them to the scan queue
                </div>
              ) : (
                Array.from(selectedPaths).map((path) => (
                  <div key={path} className="queue-item">
                    <span className="queue-item-path">{path}</span>
                    <button
                      className="queue-item-remove"
                      onClick={() => handleSelectPath(path)}
                    >
                      ✕
                    </button>
                  </div>
                ))
              )}
            </div>

            <button
              className="start-scan-btn"
              onClick={handleStartScan}
              disabled={selectedPaths.size === 0 || isScanning}
            >
              {isScanning ? 'Scanning...' : 'Start Scan'}
            </button>
          </div>

          {/* Scan Progress */}
          {(isScanning || scanResult) && (
            <div className="progress-section">
              <div className="progress-header">
                <span className="progress-icon">
                  {progress?.stage === 'completed' ? '✓' : progress?.stage === 'error' ? '✕' : '◐'}
                </span>
                <span className="progress-title">
                  {isScanning ? 'Scanning Progress' : 'Scan Result'}
                </span>
                {isConnected && <span className="connected-badge">● Live</span>}
              </div>

              <div className="progress-content">
                {progress && (
                  <>
                    <div className="progress-bar-container">
                      <div
                        className="progress-bar"
                        style={{ width: `${progress.percentComplete}%` }}
                      />
                      <span className="progress-percent">{progress.percentComplete}%</span>
                    </div>

                    <div className="progress-stats">
                      <div className="stat">
                        <span className="stat-label">Stage:</span>
                        <span className="stat-value stage-badge" data-stage={progress.stage}>
                          {progress.stage.replace('_', ' ')}
                        </span>
                      </div>
                      <div className="stat">
                        <span className="stat-label">Files Found:</span>
                        <span className="stat-value">{progress.filesFound}</span>
                      </div>
                      <div className="stat">
                        <span className="stat-label">Files Processed:</span>
                        <span className="stat-value">{progress.filesProcessed}</span>
                      </div>
                    </div>

                    <div className="progress-message">{progress.message}</div>

                    {progress.currentFile && (
                      <div className="progress-current">
                        <span className="current-label">Current:</span>
                        <span className="current-file">{progress.currentFile}</span>
                      </div>
                    )}

                    {progress.error && (
                      <div className="progress-error">{progress.error}</div>
                    )}
                  </>
                )}

                {isScanning && (
                  <button className="cancel-scan-btn" onClick={handleCancelScan}>
                    Cancel Scan
                  </button>
                )}
              </div>
            </div>
          )}

          {/* Previously Scanned Directories */}
          <div className="history-section">
            <div className="history-header">
              <span className="history-icon">📜</span>
              <span className="history-title">Scan History</span>
            </div>

            <div className="history-list">
              {previousDirectories.length === 0 ? (
                <div className="history-empty">No previous scans</div>
              ) : (
                previousDirectories.map((dir) => (
                  <div key={dir} className="history-item">
                    <span className="history-path">{dir}</span>
                    <button
                      className="history-rescan-btn"
                      onClick={() => handleRescanDirectory(dir)}
                      disabled={isScanning}
                    >
                      Rescan
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
