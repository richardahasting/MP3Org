import { useState } from 'react';
import type { TabId } from './types/music';
import MetadataEditor from './components/metadata/MetadataEditor';
import ImportView from './components/import/ImportView';
import DuplicateManager from './components/duplicates/DuplicateManager';
import OrganizeView from './components/organize/OrganizeView';
import ConfigurationView from './components/config/ConfigurationView';

const tabs: { id: TabId; label: string; icon: string }[] = [
  { id: 'duplicates', label: 'Duplicates', icon: '◎' },
  { id: 'metadata', label: 'Metadata', icon: '♫' },
  { id: 'import', label: 'Import', icon: '⬇' },
  { id: 'organize', label: 'Organize', icon: '⬡' },
  { id: 'config', label: 'Config', icon: '⚙' },
];

function App() {
  const [activeTab, setActiveTab] = useState<TabId>('metadata');

  return (
    <div className="app">
      <header className="header">
        <div className="logo">
          <span className="logo-icon">◉</span>
          <h1 className="logo-text">MP3Org</h1>
        </div>
        <p className="tagline">Music Collection Manager</p>
      </header>

      <nav className="tab-nav">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            <span className="tab-icon">{tab.icon}</span>
            <span className="tab-label">{tab.label}</span>
          </button>
        ))}
      </nav>

      <main className="main-content">
        {activeTab === 'metadata' && <MetadataEditor />}
        {activeTab === 'duplicates' && <DuplicateManager />}
        {activeTab === 'import' && <ImportView />}
        {activeTab === 'organize' && <OrganizeView />}
        {activeTab === 'config' && <ConfigurationView />}
      </main>

      <footer className="footer">
        <span className="footer-text">◉ MP3Org — Crafted for music lovers</span>
      </footer>
    </div>
  );
}

export default App;
