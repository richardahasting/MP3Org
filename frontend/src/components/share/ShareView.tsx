import { useState, useEffect } from 'react';

const API_BASE = '/api/v1/music';

interface Artist {
  artist: string;
  count: number;
}

interface Album {
  artist: string;
  album: string;
  count: number;
}

interface NetworkInfo {
  primaryIp: string;
  port: number;
}

export default function ShareView() {
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo>({
    primaryIp: window.location.hostname,
    port: parseInt(window.location.port) || 9090,
  });
  const [artists, setArtists] = useState<Artist[]>([]);
  const [albums, setAlbums] = useState<Album[]>([]);
  const [selectedArtist, setSelectedArtist] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [view, setView] = useState<'artists' | 'albums'>('artists');
  const [searchQuery, setSearchQuery] = useState('');

  const serverUrl = `http://${networkInfo.primaryIp}:${networkInfo.port}`;

  // Fetch network info
  useEffect(() => {
    fetch('/api/v1/config/network')
      .then(res => res.json())
      .then(data => {
        setNetworkInfo({
          primaryIp: data.primaryIp || window.location.hostname,
          port: data.port || 9090,
        });
      })
      .catch(() => {});
  }, []);

  // Fetch artists
  useEffect(() => {
    setLoading(true);
    fetch(`${API_BASE}/artists`)
      .then(res => res.json())
      .then(data => {
        setArtists(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);

  // Fetch albums when artist selected or view changes
  useEffect(() => {
    if (view === 'albums') {
      setLoading(true);
      const url = selectedArtist
        ? `${API_BASE}/albums?artist=${encodeURIComponent(selectedArtist)}`
        : `${API_BASE}/albums`;
      fetch(url)
        .then(res => res.json())
        .then(data => {
          setAlbums(data);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [view, selectedArtist]);

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(serverUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {}
  };

  const downloadArtist = (artist: string) => {
    setDownloading(artist);
    const link = document.createElement('a');
    link.href = `${API_BASE}/download/artist/${encodeURIComponent(artist)}`;
    link.download = `${artist}.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => setDownloading(null), 1000);
  };

  const downloadAlbum = (artist: string, album: string) => {
    const key = `${artist}|||${album}`;
    setDownloading(key);
    const link = document.createElement('a');
    link.href = `${API_BASE}/download/album?artist=${encodeURIComponent(artist)}&album=${encodeURIComponent(album)}`;
    link.download = `${artist} - ${album}.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => setDownloading(null), 1000);
  };

  // Filter items by search query
  const filteredArtists = artists.filter(a =>
    a.artist.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredAlbums = albums.filter(a =>
    a.album.toLowerCase().includes(searchQuery.toLowerCase()) ||
    a.artist.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="share-view-mobile">
      {/* Header with server URL */}
      <div className="share-mobile-header">
        <h2>Share Music</h2>
        <div className="server-url-box">
          <code>{serverUrl}</code>
          <button onClick={copyToClipboard} className="copy-btn-mobile">
            {copied ? '✓' : 'Copy'}
          </button>
        </div>
        <div className="qr-mobile">
          <img
            src={`https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=${encodeURIComponent(serverUrl)}`}
            alt="QR Code"
          />
        </div>
      </div>

      {/* View Toggle */}
      <div className="view-toggle">
        <button
          className={`toggle-btn ${view === 'artists' ? 'active' : ''}`}
          onClick={() => { setView('artists'); setSelectedArtist(null); }}
        >
          By Artist ({artists.length})
        </button>
        <button
          className={`toggle-btn ${view === 'albums' ? 'active' : ''}`}
          onClick={() => setView('albums')}
        >
          By Album ({albums.length})
        </button>
      </div>

      {/* Search */}
      <div className="search-bar-mobile">
        <input
          type="text"
          placeholder={view === 'artists' ? 'Search artists...' : 'Search albums...'}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        {searchQuery && (
          <button className="clear-search" onClick={() => setSearchQuery('')}>×</button>
        )}
      </div>

      {/* Artist filter for albums view */}
      {view === 'albums' && (
        <div className="artist-filter">
          <select
            value={selectedArtist || ''}
            onChange={(e) => setSelectedArtist(e.target.value || null)}
          >
            <option value="">All Artists</option>
            {artists.map(a => (
              <option key={a.artist} value={a.artist}>
                {a.artist} ({a.count})
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Content */}
      <div className="download-list">
        {loading ? (
          <div className="loading-mobile">Loading...</div>
        ) : view === 'artists' ? (
          filteredArtists.length === 0 ? (
            <div className="empty-mobile">No artists found</div>
          ) : (
            filteredArtists.map(artist => (
              <div key={artist.artist} className="download-item">
                <div className="item-info">
                  <div className="item-name">{artist.artist}</div>
                  <div className="item-count">{artist.count} files</div>
                </div>
                <button
                  className="download-btn-mobile"
                  onClick={() => downloadArtist(artist.artist)}
                  disabled={downloading === artist.artist}
                >
                  {downloading === artist.artist ? '...' : '⬇ ZIP'}
                </button>
              </div>
            ))
          )
        ) : (
          filteredAlbums.length === 0 ? (
            <div className="empty-mobile">No albums found</div>
          ) : (
            filteredAlbums.map(album => (
              <div key={`${album.artist}|||${album.album}`} className="download-item">
                <div className="item-info">
                  <div className="item-name">{album.album}</div>
                  <div className="item-artist">{album.artist}</div>
                  <div className="item-count">{album.count} files</div>
                </div>
                <button
                  className="download-btn-mobile"
                  onClick={() => downloadAlbum(album.artist, album.album)}
                  disabled={downloading === `${album.artist}|||${album.album}`}
                >
                  {downloading === `${album.artist}|||${album.album}` ? '...' : '⬇ ZIP'}
                </button>
              </div>
            ))
          )
        )}
      </div>
    </div>
  );
}
