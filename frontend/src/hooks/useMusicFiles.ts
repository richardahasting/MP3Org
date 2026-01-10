import { useState, useEffect, useCallback } from 'react';
import type { MusicFile, PageResponse } from '../types/music';
import { fetchMusicFiles, searchMusicFiles, getMusicFileCount } from '../api/musicApi';

interface UseMusicFilesOptions {
  initialPage?: number;
  pageSize?: number;
}

export function useMusicFiles(options: UseMusicFilesOptions = {}) {
  const { initialPage = 0, pageSize = 50 } = options;

  const [data, setData] = useState<PageResponse<MusicFile> | null>(null);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(initialPage);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchType, setSearchType] = useState<'all' | 'title' | 'artist' | 'album'>('all');

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let result: PageResponse<MusicFile>;
      if (searchQuery.trim()) {
        result = await searchMusicFiles(searchQuery, searchType, page, pageSize);
      } else {
        result = await fetchMusicFiles(page, pageSize);
      }
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, searchQuery, searchType]);

  const loadTotalCount = useCallback(async () => {
    try {
      const count = await getMusicFileCount();
      setTotalCount(count);
    } catch {
      // Silently fail for count
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    loadTotalCount();
  }, [loadTotalCount]);

  const search = useCallback((query: string, type: 'all' | 'title' | 'artist' | 'album' = 'all') => {
    setSearchQuery(query);
    setSearchType(type);
    setPage(0);
  }, []);

  const clearSearch = useCallback(() => {
    setSearchQuery('');
    setSearchType('all');
    setPage(0);
  }, []);

  const goToPage = useCallback((newPage: number) => {
    setPage(newPage);
  }, []);

  const refresh = useCallback(() => {
    loadData();
    loadTotalCount();
  }, [loadData, loadTotalCount]);

  return {
    files: data?.content ?? [],
    page,
    totalPages: data?.totalPages ?? 0,
    totalElements: data?.totalElements ?? 0,
    totalCount,
    loading,
    error,
    searchQuery,
    searchType,
    search,
    clearSearch,
    goToPage,
    refresh,
  };
}
