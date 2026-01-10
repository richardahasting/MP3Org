import { useState } from 'react';
import type { TabId } from './types/music';
import MetadataEditor from './components/metadata/MetadataEditor';

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
        {activeTab === 'duplicates' && <PlaceholderView title="Duplicate Manager" description="Find and manage duplicate music files" />}
        {activeTab === 'import' && <PlaceholderView title="Import" description="Scan directories for music files" />}
        {activeTab === 'organize' && <PlaceholderView title="Organize" description="Organize your music collection" />}
        {activeTab === 'config' && <PlaceholderView title="Configuration" description="Manage application settings" />}
      </main>

      <footer className="footer">
        <span className="footer-text">◉ MP3Org — Crafted for music lovers</span>
      </footer>
    </div>
  );
}

function PlaceholderView({ title, description }: { title: string; description: string }) {
  return (
    <div className="placeholder-view">
      <div className="placeholder-content">
        <h2 className="placeholder-title">{title}</h2>
        <p className="placeholder-description">{description}</p>
        <div className="placeholder-icon">◎</div>
        <p className="placeholder-note">Coming soon in Phase 2+</p>
      </div>
    </div>
  );
}

export default App;
