import { useState, useEffect, useCallback } from 'react';
import type {
  OrganizationPreview,
  TextFormat,
} from '../../api/organizationApi';
import {
  previewAllOrganization,
  executeOrganization,
  getTemplates,
  getAvailableFields,
  getTextFormats,
} from '../../api/organizationApi';

export default function OrganizeView() {
  // Template configuration
  const [basePath, setBasePath] = useState('/Volumes/Music/Organized');
  const [template, setTemplate] = useState('');
  const [textFormat, setTextFormat] = useState('UNDERSCORE');
  const [useSubdirectories, setUseSubdirectories] = useState(true);
  const [subdirectoryLevels, setSubdirectoryLevels] = useState(7);

  // Available options
  const [templateExamples, setTemplateExamples] = useState<string[]>([]);
  const [defaultTemplate, setDefaultTemplate] = useState('');
  const [availableFields, setAvailableFields] = useState<string[]>([]);
  const [textFormats, setTextFormats] = useState<TextFormat[]>([]);

  // Preview state
  const [previews, setPreviews] = useState<OrganizationPreview[]>([]);
  const [previewPage, setPreviewPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);

  // Selection for execution
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [executing, setExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState<{
    success: number;
    failed: number;
    errors: string[];
  } | null>(null);

  // Load available options on mount
  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [templatesData, fields, formats] = await Promise.all([
          getTemplates(),
          getAvailableFields(),
          getTextFormats(),
        ]);
        setTemplateExamples(templatesData.examples);
        setDefaultTemplate(templatesData.default);
        setTemplate(templatesData.default);
        setAvailableFields(fields);
        setTextFormats(formats);
      } catch (err) {
        console.error('Failed to load options:', err);
      }
    };
    loadOptions();
  }, []);

  const handlePreview = useCallback(async () => {
    if (!basePath.trim()) {
      setPreviewError('Please enter a base path');
      return;
    }

    try {
      setPreviewLoading(true);
      setPreviewError(null);
      const result = await previewAllOrganization(
        basePath,
        template || undefined,
        textFormat,
        useSubdirectories,
        subdirectoryLevels,
        previewPage,
        25
      );
      setPreviews(result.previews);
      setTotalCount(result.totalCount);
      setTotalPages(result.totalPages);
    } catch (err) {
      setPreviewError(err instanceof Error ? err.message : 'Preview failed');
    } finally {
      setPreviewLoading(false);
    }
  }, [basePath, template, textFormat, useSubdirectories, subdirectoryLevels, previewPage]);

  const handlePageChange = useCallback((newPage: number) => {
    setPreviewPage(newPage);
  }, []);

  // Re-fetch when page changes
  useEffect(() => {
    if (previews.length > 0 || previewPage > 0) {
      handlePreview();
    }
  }, [previewPage]);

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

  const selectAllOnPage = useCallback(() => {
    const allOnPage = previews.filter(p => p.valid).map(p => p.id);
    const allSelected = allOnPage.every(id => selectedIds.has(id));

    if (allSelected) {
      setSelectedIds(prev => {
        const next = new Set(prev);
        allOnPage.forEach(id => next.delete(id));
        return next;
      });
    } else {
      setSelectedIds(prev => {
        const next = new Set(prev);
        allOnPage.forEach(id => next.add(id));
        return next;
      });
    }
  }, [previews, selectedIds]);

  const handleExecute = useCallback(async () => {
    if (selectedIds.size === 0) {
      alert('Please select files to organize');
      return;
    }

    if (!confirm(`Are you sure you want to organize ${selectedIds.size} files? This will copy them to the new locations.`)) {
      return;
    }

    try {
      setExecuting(true);
      setExecutionResult(null);
      const result = await executeOrganization(
        Array.from(selectedIds),
        basePath,
        template || undefined,
        textFormat,
        useSubdirectories,
        subdirectoryLevels
      );
      setExecutionResult({
        success: result.successCount,
        failed: result.failureCount,
        errors: result.errors,
      });
      setSelectedIds(new Set());
    } catch (err) {
      alert('Execution failed: ' + (err instanceof Error ? err.message : 'Unknown error'));
    } finally {
      setExecuting(false);
    }
  }, [selectedIds, basePath, template, textFormat, useSubdirectories, subdirectoryLevels]);

  const insertField = useCallback((field: string) => {
    const fieldPlaceholder = field === 'track_number' ? `{${field}:02d}` : `{${field}}`;
    setTemplate(prev => prev + fieldPlaceholder);
  }, []);

  return (
    <div className="organize-view">
      <div className="organize-header">
        <h2 className="organize-title">Organize Music Files</h2>
        <p className="organize-subtitle">
          Preview and execute file organization based on metadata templates
        </p>
      </div>

      <div className="organize-content">
        {/* Configuration Panel */}
        <div className="config-panel">
          <h3 className="panel-title">Configuration</h3>

          <div className="config-field">
            <label className="config-label">Base Output Path</label>
            <input
              type="text"
              className="config-input"
              value={basePath}
              onChange={(e) => setBasePath(e.target.value)}
              placeholder="/path/to/organized/music"
            />
          </div>

          <div className="config-field">
            <label className="config-label">Path Template</label>
            <textarea
              className="config-textarea"
              value={template}
              onChange={(e) => setTemplate(e.target.value)}
              placeholder={defaultTemplate}
              rows={3}
            />
            <div className="field-buttons">
              {availableFields.map(field => (
                <button
                  key={field}
                  type="button"
                  className="field-btn"
                  onClick={() => insertField(field)}
                >
                  {`{${field}}`}
                </button>
              ))}
            </div>
          </div>

          <div className="config-field">
            <label className="config-label">Example Templates</label>
            <select
              className="config-select"
              value=""
              onChange={(e) => {
                if (e.target.value) setTemplate(e.target.value);
              }}
            >
              <option value="">Select a template...</option>
              {templateExamples.map((t, i) => (
                <option key={i} value={t}>{t}</option>
              ))}
            </select>
          </div>

          <div className="config-field">
            <label className="config-label">Text Format</label>
            <select
              className="config-select"
              value={textFormat}
              onChange={(e) => setTextFormat(e.target.value)}
            >
              {textFormats.map(f => (
                <option key={f.name} value={f.name}>{f.name} - {f.description}</option>
              ))}
            </select>
          </div>

          <div className="config-field config-checkbox">
            <label>
              <input
                type="checkbox"
                checked={useSubdirectories}
                onChange={(e) => setUseSubdirectories(e.target.checked)}
              />
              Use Alphabetical Subdirectories
            </label>
          </div>

          {useSubdirectories && (
            <div className="config-field">
              <label className="config-label">Subdirectory Groups (1-12)</label>
              <input
                type="number"
                className="config-input config-input-small"
                value={subdirectoryLevels}
                onChange={(e) => setSubdirectoryLevels(Math.min(12, Math.max(1, parseInt(e.target.value) || 1)))}
                min={1}
                max={12}
              />
            </div>
          )}

          <button
            className="preview-btn"
            onClick={handlePreview}
            disabled={previewLoading}
          >
            {previewLoading ? 'Loading Preview...' : 'Preview Organization'}
          </button>
        </div>

        {/* Preview Panel */}
        <div className="preview-panel">
          <div className="preview-header">
            <h3 className="panel-title">Preview ({totalCount.toLocaleString()} files)</h3>
            {selectedIds.size > 0 && (
              <div className="preview-actions">
                <span className="selected-info">{selectedIds.size} selected</span>
                <button
                  className="execute-btn"
                  onClick={handleExecute}
                  disabled={executing}
                >
                  {executing ? 'Organizing...' : `Organize ${selectedIds.size} Files`}
                </button>
              </div>
            )}
          </div>

          {previewError && (
            <div className="preview-error">
              <span className="error-icon">⚠</span>
              {previewError}
            </div>
          )}

          {executionResult && (
            <div className={`execution-result ${executionResult.failed > 0 ? 'has-errors' : 'success'}`}>
              <div className="result-summary">
                <span className="result-success">{executionResult.success} files organized successfully</span>
                {executionResult.failed > 0 && (
                  <span className="result-failed">{executionResult.failed} failed</span>
                )}
              </div>
              {executionResult.errors.length > 0 && (
                <div className="result-errors">
                  {executionResult.errors.slice(0, 5).map((err, i) => (
                    <div key={i} className="error-item">{err}</div>
                  ))}
                  {executionResult.errors.length > 5 && (
                    <div className="error-item">...and {executionResult.errors.length - 5} more</div>
                  )}
                </div>
              )}
            </div>
          )}

          {previewLoading ? (
            <div className="loading-state">
              <div className="loading-spinner">◎</div>
              <p>Loading preview...</p>
            </div>
          ) : previews.length === 0 ? (
            <div className="empty-preview">
              <div className="empty-icon">📁</div>
              <h3>No Preview Available</h3>
              <p>Configure your settings and click "Preview Organization" to see how files will be organized.</p>
            </div>
          ) : (
            <>
              <div className="preview-table-container">
                <table className="preview-table">
                  <thead>
                    <tr>
                      <th className="col-select">
                        <input
                          type="checkbox"
                          onChange={selectAllOnPage}
                          checked={previews.filter(p => p.valid).every(p => selectedIds.has(p.id))}
                        />
                      </th>
                      <th>File Info</th>
                      <th>Current Path</th>
                      <th>→</th>
                      <th>Proposed Path</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previews.map(preview => (
                      <tr
                        key={preview.id}
                        className={`${!preview.valid ? 'invalid' : ''} ${selectedIds.has(preview.id) ? 'selected' : ''}`}
                      >
                        <td className="cell-select">
                          <input
                            type="checkbox"
                            checked={selectedIds.has(preview.id)}
                            onChange={() => toggleSelect(preview.id)}
                            disabled={!preview.valid}
                          />
                        </td>
                        <td className="cell-info">
                          <div className="file-title">{preview.title || 'Unknown Title'}</div>
                          <div className="file-meta">
                            {preview.artist || 'Unknown'} - {preview.album || 'Unknown'}
                          </div>
                        </td>
                        <td className="cell-path current">
                          <span className="path-text" title={preview.currentPath || ''}>
                            {preview.currentPath ? `...${preview.currentPath.slice(-40)}` : '—'}
                          </span>
                        </td>
                        <td className="cell-arrow">→</td>
                        <td className="cell-path proposed">
                          {preview.error ? (
                            <span className="path-error">{preview.error}</span>
                          ) : (
                            <span className="path-text" title={preview.proposedPath || ''}>
                              {preview.proposedPath ? `...${preview.proposedPath.slice(-50)}` : '—'}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <div className="pagination">
                  <button
                    className="pagination-btn"
                    onClick={() => handlePageChange(previewPage - 1)}
                    disabled={previewPage === 0}
                  >
                    ← Prev
                  </button>
                  <span className="pagination-info">
                    Page {previewPage + 1} of {totalPages}
                  </span>
                  <button
                    className="pagination-btn"
                    onClick={() => handlePageChange(previewPage + 1)}
                    disabled={previewPage >= totalPages - 1}
                  >
                    Next →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
