import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { DuplicateGroup, DuplicateScanStatus } from '../types/music';

const WS_URL = 'http://localhost:9090/ws';

interface DuplicateGroupBatch {
  groups: DuplicateGroup[];
  totalGroupsFound: number;
}

interface UseDuplicateWebSocketOptions {
  sessionId: string | null;
  onProgress?: (status: DuplicateScanStatus) => void;
  onGroupsReceived?: (groups: DuplicateGroup[], totalFound: number) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: string) => void;
}

interface UseDuplicateWebSocketReturn {
  isConnected: boolean;
  status: DuplicateScanStatus | null;
  error: string | null;
  connect: () => void;
  disconnect: () => void;
}

/**
 * Hook for managing WebSocket connection for real-time duplicate scan progress.
 * Subscribes to both progress updates and progressive group results.
 *
 * Usage:
 * const { isConnected, status } = useDuplicateWebSocket({
 *   sessionId: 'abc-123',
 *   onGroupsReceived: (groups, total) => setGroups(prev => [...prev, ...groups])
 * });
 */
export function useDuplicateWebSocket({
  sessionId,
  onProgress,
  onGroupsReceived,
  onConnect,
  onDisconnect,
  onError,
}: UseDuplicateWebSocketOptions): UseDuplicateWebSocketReturn {
  const [isConnected, setIsConnected] = useState(false);
  const [status, setStatus] = useState<DuplicateScanStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    if (!sessionId || clientRef.current?.active) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (_str) => {
        // Uncomment for debugging
        // console.log('STOMP:', _str);
      },
      onConnect: () => {
        setIsConnected(true);
        setError(null);
        onConnect?.();

        // Subscribe to progress updates
        client.subscribe(`/topic/duplicates/${sessionId}`, (message: IMessage) => {
          try {
            const statusData: DuplicateScanStatus = JSON.parse(message.body);
            setStatus(statusData);
            onProgress?.(statusData);
          } catch (e) {
            console.error('Failed to parse status message:', e);
          }
        });

        // Subscribe to progressive group results
        client.subscribe(`/topic/duplicates/${sessionId}/groups`, (message: IMessage) => {
          try {
            const batch: DuplicateGroupBatch = JSON.parse(message.body);
            onGroupsReceived?.(batch.groups, batch.totalGroupsFound);
          } catch (e) {
            console.error('Failed to parse groups message:', e);
          }
        });
      },
      onDisconnect: () => {
        setIsConnected(false);
        onDisconnect?.();
      },
      onStompError: (frame) => {
        const errorMsg = frame.headers?.message || 'WebSocket error';
        setError(errorMsg);
        onError?.(errorMsg);
      },
    });

    clientRef.current = client;
    client.activate();
  }, [sessionId, onProgress, onGroupsReceived, onConnect, onDisconnect, onError]);

  const disconnect = useCallback(() => {
    if (clientRef.current?.active) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
    setIsConnected(false);
  }, []);

  // Auto-connect when sessionId changes
  useEffect(() => {
    if (sessionId) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [sessionId, connect, disconnect]);

  return {
    isConnected,
    status,
    error,
    connect,
    disconnect,
  };
}

export default useDuplicateWebSocket;
