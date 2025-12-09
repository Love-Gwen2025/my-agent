/**
 * SSE 流式聊天 Hook
 *
 * 处理服务端推送的流式响应
 */
import { useState, useCallback, useRef } from 'react';
import type { StreamChatRequest, StreamChatEvent } from '../types';
import { API_BASE_URL } from '../api/client';

interface UseSSEChatOptions {
  /** 收到内容片段时的回调 */
  onChunk?: (content: string) => void;
  /** 流式完成时的回调 */
  onComplete?: (event: StreamChatEvent) => void;
  /** 发生错误时的回调 */
  onError?: (error: string) => void;
}

interface UseSSEChatReturn {
  /** 是否正在加载 */
  isLoading: boolean;
  /** 当前累积的响应内容 */
  content: string;
  /** 发送消息 */
  sendMessage: (request: StreamChatRequest) => void;
  /** 中止当前请求 */
  abort: () => void;
  /** 重置状态 */
  reset: () => void;
}

/**
 * SSE 流式聊天 Hook
 *
 * @param options 配置选项
 * @returns SSE 聊天控制器
 */
export function useSSEChat(options: UseSSEChatOptions = {}): UseSSEChatReturn {
  const { onChunk, onComplete, onError } = options;

  const [isLoading, setIsLoading] = useState(false);
  const [content, setContent] = useState('');
  const abortControllerRef = useRef<AbortController | null>(null);

  /**
   * 重置状态
   */
  const reset = useCallback(() => {
    setContent('');
    setIsLoading(false);
  }, []);

  /**
   * 中止当前请求
   */
  const abort = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsLoading(false);
  }, []);

  /**
   * 发送流式聊天请求
   */
  const sendMessage = useCallback(
    async (request: StreamChatRequest) => {
      // 中止之前的请求
      abort();
      reset();
      setIsLoading(true);

      const controller = new AbortController();
      abortControllerRef.current = controller;

      try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/api/chat/stream`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { token } : {}),
          },
          body: JSON.stringify(request),
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body?.getReader();
        if (!reader) {
          throw new Error('Response body is not readable');
        }

        const decoder = new TextDecoder();
        let buffer = '';
        let accumulatedContent = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            // 跳过空行和注释
            if (!line.trim() || line.startsWith(':')) continue;

            // 解析 SSE 数据
            if (line.startsWith('data:')) {
              const jsonStr = line.slice(5).trim();
              if (!jsonStr) continue;

              try {
                const event: StreamChatEvent = JSON.parse(jsonStr);

                if (event.type === 'chunk' && event.content) {
                  accumulatedContent += event.content;
                  setContent(accumulatedContent);
                  onChunk?.(event.content);
                } else if (event.type === 'done') {
                  setIsLoading(false);
                  onComplete?.(event);
                } else if (event.type === 'error') {
                  setIsLoading(false);
                  onError?.(event.error || '未知错误');
                }
              } catch {
                console.warn('Failed to parse SSE data:', jsonStr);
              }
            }
          }
        }
      } catch (error) {
        if ((error as Error).name === 'AbortError') {
          console.log('Request aborted');
        } else {
          const errorMessage =
            error instanceof Error ? error.message : '请求失败';
          onError?.(errorMessage);
        }
        setIsLoading(false);
      }
    },
    [abort, reset, onChunk, onComplete, onError]
  );

  return {
    isLoading,
    content,
    sendMessage,
    abort,
    reset,
  };
}
