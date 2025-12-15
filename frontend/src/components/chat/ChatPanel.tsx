/**
 * 聊天面板组件
 *
 * 显示消息列表和输入框
 */
import { useEffect, useRef, useCallback } from 'react';
import { MessageSquare } from 'lucide-react';
import { useAppStore } from '../../store';
import { useSSEChat } from '../../hooks';
import { getConversationHistory } from '../../api';
import { MessageBubble } from './MessageBubble';
import { ChatInput } from './ChatInput';
import { ModelSelector } from './ModelSelector';
import type { Message } from '../../types';

/**
 * 空状态组件
 */
function EmptyState() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center text-[var(--text-secondary)]">
      <MessageSquare className="w-16 h-16 mb-4 opacity-50" />
      <h2 className="text-xl font-semibold mb-2">开始新对话</h2>
      <p className="text-sm">选择一个会话或创建新对话开始聊天</p>
    </div>
  );
}

/**
 * 聊天面板组件
 */
export function ChatPanel() {
  const {
    currentConversationId,
    messages,
    currentModelCode,
    streamingContent,
    setMessages,
    addMessage,
    setStreamingContent,
    clearStreamingContent,
    user,
  } = useAppStore();

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const latestStreamingRef = useRef<string>('');
  const latestMessageIdRef = useRef<number | null>(null);

  /** SSE 聊天 Hook */
  const { isLoading, sendMessage, abort } = useSSEChat({
    // 1. 增量回调：使用 ref 累积，避免闭包旧状态导致闪烁或丢字
    onChunk: (chunk) => {
      latestStreamingRef.current = `${latestStreamingRef.current}${chunk}`;
      setStreamingContent(latestStreamingRef.current);
    },
    // 2. 完成回调：无论是否有 messageId，都将最终内容落入消息列表
    onComplete: (event, finalContent) => {
      const contentToSave = finalContent || latestStreamingRef.current;
      if (contentToSave) {
        const messageId = event.messageId ?? latestMessageIdRef.current ?? Date.now();
        const aiMessage: Message = {
          id: messageId,
          conversationId: currentConversationId!,
          senderId: -1,
          role: 'assistant',
          content: contentToSave,
          contentType: 'TEXT',
          modelCode: currentModelCode || undefined,
          tokenCount: event.tokenCount,
          createTime: new Date().toISOString(),
        };
        addMessage(aiMessage);
        latestMessageIdRef.current = messageId;
      }
      latestStreamingRef.current = '';
      clearStreamingContent();
    },
    onError: (error) => {
      console.error('聊天错误:', error);
      latestStreamingRef.current = '';
      clearStreamingContent();
    },
  });

  /** 加载历史消息 */
  useEffect(() => {
    if (currentConversationId) {
      loadHistory();
    }
  }, [currentConversationId]);

  async function loadHistory() {
    if (!currentConversationId) return;
    try {
      const data = await getConversationHistory(currentConversationId);
      setMessages(data);
    } catch (error) {
      console.error('加载历史消息失败:', error);
    }
  }

  /** 滚动到底部 */
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamingContent]);

  /** 发送消息 */
  const handleSend = useCallback(
    (content: string) => {
      if (!currentConversationId || !user) return;

      // 添加用户消息到列表
      const userMessage: Message = {
        id: Date.now(),
        conversationId: currentConversationId,
        senderId: user.id,
        role: 'user',
        content,
        contentType: 'TEXT',
        createTime: new Date().toISOString(),
      };
      addMessage(userMessage);
      latestStreamingRef.current = '';
      latestMessageIdRef.current = null;
      clearStreamingContent();

      // 发送 SSE 请求
      sendMessage({
        conversationId: currentConversationId,
        content,
        modelCode: currentModelCode || undefined,
      });
    },
    [currentConversationId, currentModelCode, user, addMessage, sendMessage]
  );

  if (!currentConversationId) {
    return <EmptyState />;
  }

  return (
    <div className="flex-1 flex flex-col h-full">
      {/* 头部 */}
      <div className="px-4 py-3 border-b border-[var(--border-color)] flex items-center justify-between bg-[var(--bg-secondary)]">
        <h2 className="font-semibold">对话</h2>
        <ModelSelector />
      </div>

      {/* 消息列表 */}
      <div className="flex-1 overflow-y-auto">
        {messages.length === 0 && !streamingContent ? (
          <div className="h-full flex items-center justify-center text-[var(--text-secondary)]">
            <p>发送消息开始对话</p>
          </div>
        ) : (
          <div className="max-w-4xl mx-auto">
            {messages.map((message) => (
              <MessageBubble key={message.id} message={message} />
            ))}
            {/* 流式响应 */}
            {streamingContent && (
              <MessageBubble
                message={{
                  id: -1,
                  conversationId: currentConversationId,
                  senderId: -1,
                  role: 'assistant',
                  content: streamingContent,
                  contentType: 'TEXT',
                  createTime: new Date().toISOString(),
                }}
                isStreaming={true}
              />
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* 输入框 */}
      <ChatInput
        isLoading={isLoading}
        onSend={handleSend}
        onAbort={abort}
      />
    </div>
  );
}
