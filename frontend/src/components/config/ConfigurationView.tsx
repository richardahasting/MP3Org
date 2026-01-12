import { useState, useEffect, useCallback } from 'react';
import type {
  FuzzySearchConfig,
  FileTypes,
  DatabaseProfile,
  DatabaseInfo,
} from '../../api/configApi';
import {
  getFuzzySearchConfig,
  updateFuzzySearchConfig,
  resetFuzzySearchConfig,
  applyPreset,
  getFileTypes,
  updateFileTypes,
  getProfiles,
  createProfile,
  deleteProfile,
  activateProfile,
  getDatabaseInfo,
} from '../../api/configApi';

type ConfigTab = 'fuzzy' | 'filetypes' | 'profiles';

export default function ConfigurationView() {
  const [activeTab, setActiveTab] = useState<ConfigTab>('fuzzy');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');

  // Fuzzy search state
  const [fuzzyConfig, setFuzzyConfig] = useState<FuzzySearchConfig | null>(null);

  // File types state
  const [fileTypes, setFileTypes] = useState<FileTypes | null>(null);

  // Profiles state
  const [profiles, setProfiles] = useState<DatabaseProfile[]>([]);
  const [databaseInfo, setDatabaseInfo] = useState<DatabaseInfo | null>(null);
  const [showNewProfileForm, setShowNewProfileForm] = useState(false);
  const [newProfile, setNewProfile] = useState({ name: '', description: '', databasePath: '' });

  // Load initial data
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [fuzzy, types, profileList, dbInfo] = await Promise.all([
          getFuzzySearchConfig(),
          getFileTypes(),
          getProfiles(),
          getDatabaseInfo(),
        ]);

        setFuzzyConfig(fuzzy);
        setFileTypes(types);
        setProfiles(profileList);
        setDatabaseInfo(dbInfo);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load configuration');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  // Save fuzzy config with debounce
  const saveFuzzyConfig = useCallback(async (config: FuzzySearchConfig) => {
    try {
      setSaveStatus('saving');
      await updateFuzzySearchConfig(config);
      setSaveStatus('saved');
      setTimeout(() => setSaveStatus('idle'), 2000);
    } catch {
      setSaveStatus('error');
    }
  }, []);

  const handleFuzzyChange = (field: keyof FuzzySearchConfig, value: number | boolean | string) => {
    if (!fuzzyConfig) return;
    const updated = { ...fuzzyConfig, [field]: value };
    setFuzzyConfig(updated);
    saveFuzzyConfig(updated);
  };

  const handlePresetApply = async (preset: 'strict' | 'balanced' | 'lenient') => {
    try {
      setSaveStatus('saving');
      const updated = await applyPreset(preset);
      setFuzzyConfig(updated);
      setSaveStatus('saved');
      setTimeout(() => setSaveStatus('idle'), 2000);
    } catch {
      setSaveStatus('error');
    }
  };

  const handleResetFuzzy = async () => {
    if (!confirm('Reset all fuzzy search settings to defaults?')) return;
    try {
      setSaveStatus('saving');
      const updated = await resetFuzzySearchConfig();
      setFuzzyConfig(updated);
      setSaveStatus('saved');
      setTimeout(() => setSaveStatus('idle'), 2000);
    } catch {
      setSaveStatus('error');
    }
  };

  const handleFileTypeToggle = async (type: string) => {
    if (!fileTypes) return;
    const newEnabled = fileTypes.enabledTypes.includes(type)
      ? fileTypes.enabledTypes.filter(t => t !== type)
      : [...fileTypes.enabledTypes, type];

    try {
      const updated = await updateFileTypes(newEnabled);
      setFileTypes(updated);
    } catch {
      setError('Failed to update file types');
    }
  };

  const handleCreateProfile = async () => {
    if (!newProfile.name.trim()) {
      alert('Profile name is required');
      return;
    }
    try {
      const created = await createProfile(newProfile.name, newProfile.description, newProfile.databasePath);
      setProfiles([...profiles, created]);
      setShowNewProfileForm(false);
      setNewProfile({ name: '', description: '', databasePath: '' });
    } catch {
      setError('Failed to create profile');
    }
  };

  const handleActivateProfile = async (profileId: string) => {
    try {
      await activateProfile(profileId);
      const updatedProfiles = await getProfiles();
      setProfiles(updatedProfiles);

      // Reload fuzzy config for new profile
      const newFuzzy = await getFuzzySearchConfig();
      setFuzzyConfig(newFuzzy);
    } catch {
      setError('Failed to activate profile');
    }
  };

  const handleDeleteProfile = async (profileId: string) => {
    if (!confirm('Are you sure you want to delete this profile?')) return;
    try {
      await deleteProfile(profileId);
      setProfiles(profiles.filter(p => p.id !== profileId));
    } catch {
      setError('Failed to delete profile');
    }
  };

  if (loading) {
    return (
      <div className="config-view">
        <div className="loading-state">
          <div className="loading-spinner">◎</div>
          <p>Loading configuration...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="config-view">
      <div className="config-header">
        <h2 className="config-title">Configuration</h2>
        <p className="config-subtitle">
          Manage application settings, fuzzy search parameters, and database profiles
        </p>
        {saveStatus !== 'idle' && (
          <span className={`save-status ${saveStatus}`}>
            {saveStatus === 'saving' && 'Saving...'}
            {saveStatus === 'saved' && 'Saved'}
            {saveStatus === 'error' && 'Save failed'}
          </span>
        )}
      </div>

      {error && (
        <div className="config-error">
          <span className="error-icon">⚠</span>
          {error}
          <button className="dismiss-btn" onClick={() => setError(null)}>×</button>
        </div>
      )}

      <div className="config-tabs">
        <button
          className={`config-tab ${activeTab === 'fuzzy' ? 'active' : ''}`}
          onClick={() => setActiveTab('fuzzy')}
        >
          Fuzzy Search
        </button>
        <button
          className={`config-tab ${activeTab === 'filetypes' ? 'active' : ''}`}
          onClick={() => setActiveTab('filetypes')}
        >
          File Types
        </button>
        <button
          className={`config-tab ${activeTab === 'profiles' ? 'active' : ''}`}
          onClick={() => setActiveTab('profiles')}
        >
          Profiles
        </button>
      </div>

      <div className="config-content">
        {activeTab === 'fuzzy' && fuzzyConfig && (
          <div className="config-panel fuzzy-panel">
            <div className="panel-header">
              <h3 className="panel-title">Fuzzy Search Settings</h3>
              <div className="panel-actions">
                <button className="preset-btn" onClick={() => handlePresetApply('strict')}>Strict</button>
                <button className="preset-btn" onClick={() => handlePresetApply('balanced')}>Balanced</button>
                <button className="preset-btn" onClick={() => handlePresetApply('lenient')}>Lenient</button>
                <button className="reset-btn" onClick={handleResetFuzzy}>Reset</button>
              </div>
            </div>

            <div className="config-section">
              <h4 className="section-title">Similarity Thresholds</h4>
              <div className="config-grid">
                <div className="config-field">
                  <label>Title Similarity</label>
                  <div className="slider-group">
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={fuzzyConfig.titleSimilarityThreshold}
                      onChange={(e) => handleFuzzyChange('titleSimilarityThreshold', Number(e.target.value))}
                    />
                    <span className="slider-value">{fuzzyConfig.titleSimilarityThreshold}%</span>
                  </div>
                </div>

                <div className="config-field">
                  <label>Artist Similarity</label>
                  <div className="slider-group">
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={fuzzyConfig.artistSimilarityThreshold}
                      onChange={(e) => handleFuzzyChange('artistSimilarityThreshold', Number(e.target.value))}
                    />
                    <span className="slider-value">{fuzzyConfig.artistSimilarityThreshold}%</span>
                  </div>
                </div>

                <div className="config-field">
                  <label>Album Similarity</label>
                  <div className="slider-group">
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={fuzzyConfig.albumSimilarityThreshold}
                      onChange={(e) => handleFuzzyChange('albumSimilarityThreshold', Number(e.target.value))}
                    />
                    <span className="slider-value">{fuzzyConfig.albumSimilarityThreshold}%</span>
                  </div>
                </div>

                <div className="config-field">
                  <label>Minimum Fields to Match</label>
                  <div className="slider-group">
                    <input
                      type="range"
                      min="1"
                      max="4"
                      value={fuzzyConfig.minimumFieldsToMatch}
                      onChange={(e) => handleFuzzyChange('minimumFieldsToMatch', Number(e.target.value))}
                    />
                    <span className="slider-value">{fuzzyConfig.minimumFieldsToMatch}/4</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="config-section">
              <h4 className="section-title">Duration Tolerance</h4>
              <div className="config-grid">
                <div className="config-field">
                  <label>Absolute Tolerance (seconds)</label>
                  <input
                    type="number"
                    className="config-input"
                    min="0"
                    max="60"
                    value={fuzzyConfig.durationToleranceSeconds}
                    onChange={(e) => handleFuzzyChange('durationToleranceSeconds', Number(e.target.value))}
                  />
                </div>

                <div className="config-field">
                  <label>Percentage Tolerance</label>
                  <div className="slider-group">
                    <input
                      type="range"
                      min="0"
                      max="20"
                      step="0.5"
                      value={fuzzyConfig.durationTolerancePercent}
                      onChange={(e) => handleFuzzyChange('durationTolerancePercent', Number(e.target.value))}
                    />
                    <span className="slider-value">{fuzzyConfig.durationTolerancePercent}%</span>
                  </div>
                </div>

                <div className="config-field">
                  <label>Bitrate Tolerance (kbps)</label>
                  <input
                    type="number"
                    className="config-input"
                    min="0"
                    max="320"
                    value={fuzzyConfig.bitrateToleranceKbps}
                    onChange={(e) => handleFuzzyChange('bitrateToleranceKbps', Number(e.target.value))}
                  />
                </div>
              </div>
            </div>

            <div className="config-section">
              <h4 className="section-title">Text Normalization</h4>
              <div className="config-checkboxes">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={fuzzyConfig.ignoreCaseDifferences}
                    onChange={(e) => handleFuzzyChange('ignoreCaseDifferences', e.target.checked)}
                  />
                  Ignore Case Differences
                </label>

                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={fuzzyConfig.ignorePunctuation}
                    onChange={(e) => handleFuzzyChange('ignorePunctuation', e.target.checked)}
                  />
                  Ignore Punctuation
                </label>

                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={!fuzzyConfig.wordOrderSensitive}
                    onChange={(e) => handleFuzzyChange('wordOrderSensitive', !e.target.checked)}
                  />
                  Ignore Word Order
                </label>

                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={fuzzyConfig.ignoreArtistPrefixes}
                    onChange={(e) => handleFuzzyChange('ignoreArtistPrefixes', e.target.checked)}
                  />
                  Ignore Artist Prefixes (The, A, An)
                </label>

                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={fuzzyConfig.ignoreFeaturing}
                    onChange={(e) => handleFuzzyChange('ignoreFeaturing', e.target.checked)}
                  />
                  Ignore Featuring Credits (feat., ft.)
                </label>

                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={fuzzyConfig.ignoreAlbumEditions}
                    onChange={(e) => handleFuzzyChange('ignoreAlbumEditions', e.target.checked)}
                  />
                  Ignore Album Editions (Deluxe, Remastered)
                </label>
              </div>
            </div>

            <div className="config-section">
              <h4 className="section-title">Track Number Matching</h4>
              <div className="config-checkboxes">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={fuzzyConfig.trackNumberMustMatch}
                    onChange={(e) => handleFuzzyChange('trackNumberMustMatch', e.target.checked)}
                  />
                  Track Number Must Match
                </label>

                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={fuzzyConfig.ignoreMissingTrackNumber}
                    onChange={(e) => handleFuzzyChange('ignoreMissingTrackNumber', e.target.checked)}
                  />
                  Ignore Missing Track Numbers
                </label>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'filetypes' && fileTypes && (
          <div className="config-panel filetypes-panel">
            <div className="panel-header">
              <h3 className="panel-title">Enabled File Types</h3>
              <p className="panel-description">
                Select which audio file formats to include when scanning directories
              </p>
            </div>

            <div className="filetype-grid">
              {fileTypes.allTypes.map(type => (
                <label key={type} className="filetype-item">
                  <input
                    type="checkbox"
                    checked={fileTypes.enabledTypes.includes(type)}
                    onChange={() => handleFileTypeToggle(type)}
                  />
                  <span className="filetype-name">.{type}</span>
                </label>
              ))}
            </div>

            <div className="filetype-summary">
              {fileTypes.enabledTypes.length} of {fileTypes.allTypes.length} file types enabled
            </div>
          </div>
        )}

        {activeTab === 'profiles' && (
          <div className="config-panel profiles-panel">
            <div className="panel-header">
              <h3 className="panel-title">Database Profiles</h3>
              <button
                className="new-profile-btn"
                onClick={() => setShowNewProfileForm(!showNewProfileForm)}
              >
                {showNewProfileForm ? 'Cancel' : '+ New Profile'}
              </button>
            </div>

            {showNewProfileForm && (
              <div className="new-profile-form">
                <div className="form-field">
                  <label>Profile Name</label>
                  <input
                    type="text"
                    className="config-input"
                    value={newProfile.name}
                    onChange={(e) => setNewProfile({ ...newProfile, name: e.target.value })}
                    placeholder="My Music Collection"
                  />
                </div>
                <div className="form-field">
                  <label>Description</label>
                  <input
                    type="text"
                    className="config-input"
                    value={newProfile.description}
                    onChange={(e) => setNewProfile({ ...newProfile, description: e.target.value })}
                    placeholder="Optional description"
                  />
                </div>
                <div className="form-field">
                  <label>Database Path</label>
                  <input
                    type="text"
                    className="config-input"
                    value={newProfile.databasePath}
                    onChange={(e) => setNewProfile({ ...newProfile, databasePath: e.target.value })}
                    placeholder="/path/to/database"
                  />
                </div>
                <button className="create-profile-btn" onClick={handleCreateProfile}>
                  Create Profile
                </button>
              </div>
            )}

            <div className="profiles-list">
              {profiles.map(profile => (
                <div
                  key={profile.id}
                  className={`profile-card ${profile.active ? 'active' : ''}`}
                >
                  <div className="profile-info">
                    <div className="profile-name">
                      {profile.active && <span className="active-badge">Active</span>}
                      {profile.name}
                    </div>
                    {profile.description && (
                      <div className="profile-description">{profile.description}</div>
                    )}
                    <div className="profile-path">{profile.databasePath}</div>
                    {profile.lastUsedDate && (
                      <div className="profile-date">
                        Last used: {new Date(profile.lastUsedDate).toLocaleDateString()}
                      </div>
                    )}
                  </div>
                  <div className="profile-actions">
                    {!profile.active && (
                      <button
                        className="activate-btn"
                        onClick={() => handleActivateProfile(profile.id)}
                      >
                        Activate
                      </button>
                    )}
                    {profile.id !== 'default' && (
                      <button
                        className="delete-profile-btn"
                        onClick={() => handleDeleteProfile(profile.id)}
                      >
                        Delete
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {databaseInfo && (
              <div className="database-info">
                <h4 className="section-title">Database Information</h4>
                <div className="info-grid">
                  <div className="info-item">
                    <span className="info-label">Path:</span>
                    <span className="info-value">{databaseInfo.databasePath}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">JDBC URL:</span>
                    <span className="info-value">{databaseInfo.jdbcUrl}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Driver:</span>
                    <span className="info-value">{databaseInfo.jdbcDriver}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
