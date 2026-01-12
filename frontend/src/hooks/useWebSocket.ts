import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { ScanProgress } from '../types/music';

const WS_URL = 'http://localhost:9090/ws';

interface UseWebSocketOptions {
  sessionId: string | null;
  onProgress?: (progress: ScanProgress) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: string) => void;
}

interface UseWebSocketReturn {
  isConnected: boolean;
  progress: ScanProgress | null;
  error: string | null;
  connect: () => void;
  disconnect: () => void;
}

/**
 * Hook for managing WebSocket connection for real-time scan progress.
 *
 * Usage:
 * const { isConnected, progress } = useWebSocket({ sessionId: 'abc-123' });
 */
export function useWebSocket({
  sessionId,
  onProgress,
  onConnect,
  onDisconnect,
  onError,
}: UseWebSocketOptions): UseWebSocketReturn {
  const [isConnected, setIsConnected] = useState(false);
  const [progress, setProgress] = useState<ScanProgress | null>(null);
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

        // Subscribe to the session topic
        client.subscribe(`/topic/scanning/${sessionId}`, (message: IMessage) => {
          try {
            const progressData: ScanProgress = JSON.parse(message.body);
            setProgress(progressData);
            onProgress?.(progressData);
          } catch (e) {
            console.error('Failed to parse progress message:', e);
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
  }, [sessionId, onProgress, onConnect, onDisconnect, onError]);

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
    progress,
    error,
    connect,
    disconnect,
  };
}

export default useWebSocket;
