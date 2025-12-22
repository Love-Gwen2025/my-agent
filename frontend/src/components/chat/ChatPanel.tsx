/**
 * 聊天面板组件
 *
 * Main chat interface with modern styling
 */
import { useEffect, useRef, useCallback, useState } from 'react';
import { MessageSquare, Sparkles } from 'lucide-react';
import { useAppStore } from '../../store';
import { useSSEChat, getToolDisplayName } from '../../hooks';
import { getConversationHistory } from '../../api';
import { getSiblingMessages, getMessageById, setCurrentMessage } from '../../api/branch';
import { MessageBubble } from './MessageBubble';
import { ChatInput } from './ChatInput';
import { ModelSelector } from './ModelSelector';
import type { Message, SiblingInfo } from '../../types';

/**
 * 空状态组件
 */
function EmptyState() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center text-[var(--text-secondary)] p-8">
      <div
        className="w-20 h-20 rounded-2xl flex items-center justify-center mb-6 animate-float"
        style={{ background: 'var(--accent-gradient)' }}
      >
        <Sparkles className="w-10 h-10 text-white" />
      </div>
      <h2 className="text-2xl font-bold text-[var(--text-primary)] mb-2">开始对话</h2>
      <p className="text-center max-w-md">
        选择一个会话或创建新对话，开始与 AI 助手交流
      </p>
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
    currentCheckpointId,
    currentModelCode,
    streamingContent,
    setMessages,
    addMessage,
    setStreamingContent,
    clearStreamingContent,
    user,
    setCurrentCheckpointId,
  } = useAppStore();

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const latestStreamingRef = useRef<string>('');
  const latestMessageIdRef = useRef<number | string | null>(null);
  const [siblingInfoMap, setSiblingInfoMap] = useState<Record<string, SiblingInfo>>({});
  const [navLoadingId, setNavLoadingId] = useState<string | null>(null);
  const [regeneratingId, setRegeneratingId] = useState<string | null>(null);
  const regeneratingIdRef = useRef<string | null>(null);
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editingContent, setEditingContent] = useState<string>('');

  const { isLoading, sendMessage, abort, activeTool } = useSSEChat({
    onChunk: (chunk) => {
      latestStreamingRef.current = `${latestStreamingRef.current}${chunk}`;
      setStreamingContent(latestStreamingRef.current);
    },
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
          checkpointId: undefined,
          parentId: event.userMessageId || event.parentId,
        };

        // 如果是 regenerate 模式，替换旧消息；否则追加
        console.log('[onComplete] regeneratingIdRef.current =', regeneratingIdRef.current);
        if (regeneratingIdRef.current) {
          const oldId = regeneratingIdRef.current;
          // 使用 getState() 获取最新消息列表，避免闭包陷阱
          const currentMessages = useAppStore.getState().messages;
          console.log('[onComplete] Replacing message, oldId =', oldId, ', message IDs in store =', currentMessages.map(m => String(m.id)));
          const found = currentMessages.some(msg => String(msg.id) === oldId);
          console.log('[onComplete] Found matching message:', found);
          setMessages(
            currentMessages.map((msg) => String(msg.id) === oldId ? aiMessage : msg)
          );
          // 验证替换后的状态并刷新分支信息
          setTimeout(() => {
            console.log('[onComplete] After setMessages, message IDs =', useAppStore.getState().messages.map(m => String(m.id)));
            // 刷新历史以更新分支导航信息
            loadHistory();
          }, 100);
        } else {
          console.log('[onComplete] Appending new AI message');
          // 获取当前消息列表
          const currentMsgs = useAppStore.getState().messages;

          // 同时：1) 更新用户消息的临时 ID 为真实 ID，2) 添加 AI 消息
          const updatedMsgs = currentMsgs.map(msg => {
            // 更新临时用户消息 ID（非 18 位数字）
            if (msg.role === 'user' && event.userMessageId && !String(msg.id).match(/^\d{18,}$/)) {
              console.log('[onComplete] Updating user message ID from', msg.id, 'to', event.userMessageId);
              return { ...msg, id: event.userMessageId };
            }
            return msg;
          });

          // 添加 AI 消息
          updatedMsgs.push(aiMessage);

          // 一次性更新状态
          setMessages(updatedMsgs);
        }

        latestMessageIdRef.current = messageId;
        regeneratingIdRef.current = null;
        setRegeneratingId(null);
        setNavLoadingId(null);
      }
      latestStreamingRef.current = '';
      clearStreamingContent();
    },
    onError: (error) => {
      console.error('聊天错误:', error);
      latestStreamingRef.current = '';
      clearStreamingContent();
      setRegeneratingId(null);
      setNavLoadingId(null);
    },
  });


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
      const lastCheckpoint = [...data].reverse().find((item) => item.checkpointId)?.checkpointId || null;
      setCurrentCheckpointId(lastCheckpoint);
      setSiblingInfoMap({});
      setNavLoadingId(null);
      setRegeneratingId(null);
      setEditingMessageId(null);
      setEditingContent('');

      // 预加载 AI 消息的分支信息（基于 messageId）
      const assistantMessages = data.filter(
        (msg) => msg.role === 'assistant'
      );
      if (assistantMessages.length > 0) {
        const siblingPromises = assistantMessages.map((msg) =>
          getSiblingMessages(currentConversationId!, String(msg.id))
            .then((info) => ({ messageId: String(msg.id), info }))
            .catch(() => null)
        );
        const results = await Promise.all(siblingPromises);
        const newSiblingMap: Record<string, SiblingInfo> = {};
        for (const result of results) {
          if (result) {
            newSiblingMap[result.messageId] = result.info;
          }
        }
        setSiblingInfoMap(newSiblingMap);
      }
    } catch (error) {
      console.error('加载历史消息失败:', error);
    }
  }

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamingContent]);

  /**
   * 获取或缓存兄弟分支信息（基于 messageId）
   */
  const ensureSiblingInfo = useCallback(
    async (messageId: string) => {
      if (siblingInfoMap[messageId]) return siblingInfoMap[messageId];
      const info = await getSiblingMessages(currentConversationId!, messageId);
      setSiblingInfoMap((prev) => ({ ...prev, [messageId]: info }));
      return info;
    },
    [currentConversationId, siblingInfoMap]
  );

  /**
   * 切换分支，替换当前 AI 消息
   */
  const handleNavigateBranch = useCallback(
    async (messageId: string, direction: 'prev' | 'next') => {
      if (!currentConversationId || navLoadingId) return;
      setNavLoadingId(messageId);
      try {
        const info = await ensureSiblingInfo(messageId);
        if (!info || info.total <= 1) return;

        const nextIndex =
          direction === 'prev' ? info.current - 1 : info.current + 1;
        if (nextIndex < 0 || nextIndex >= info.total) return;

        const targetMessageId = info.siblings[nextIndex];
        // 获取目标消息并替换当前消息
        const targetMessage = await getMessageById(currentConversationId, targetMessageId);
        if (targetMessage) {
          // 替换消息列表中对应的消息
          const newMessages = messages.map((msg) =>
            String(msg.id) === messageId ? targetMessage : msg
          );
          setMessages(newMessages);

          // 保存用户选择的分支状态（异步，不阻塞 UI）
          setCurrentMessage(currentConversationId, targetMessageId).catch(
            (err) => console.error('保存分支状态失败:', err)
          );
        }
        setSiblingInfoMap((prev) => ({
          ...prev,
          [messageId]: { ...info, current: nextIndex },
          [targetMessageId]: { ...info, current: nextIndex },
        }));
      } catch (error) {
        console.error('切换分支失败:', error);
      } finally {
        setNavLoadingId(null);
      }
    },
    [
      currentConversationId,
      ensureSiblingInfo,
      messages,
      navLoadingId,
      setMessages,
      setCurrentCheckpointId,
      setStreamingContent,
    ]
  );

  /**
   * 1. 重新生成当前 AI 回复，生成兄弟分支
   */
  const handleRegenerate = useCallback(
    (message: Message, index: number) => {
      if (!currentConversationId || !user) return;
      const lastUser = [...messages]
        .slice(0, index)
        .reverse()
        .find((item) => item.role === 'user');
      if (!lastUser) return;

      // 重新生成时，新的 AI 回复和当前 AI 回复共享同一个父节点（用户消息）
      // 使用用户消息的 ID 作为 parentMessageId
      const parentMessageId = String(lastUser.id);

      setRegeneratingId(String(message.id));
      regeneratingIdRef.current = String(message.id); // 设置 ref 供 onComplete 使用
      console.log('[handleRegenerate] Set regeneratingIdRef to', regeneratingIdRef.current);
      latestStreamingRef.current = '';
      latestMessageIdRef.current = null;
      clearStreamingContent();

      sendMessage({
        conversationId: currentConversationId,
        content: lastUser.content,
        modelCode: currentModelCode || undefined,
        parentMessageId: parentMessageId,
        regenerate: true,
      });
    },
    [
      clearStreamingContent,
      currentConversationId,
      currentModelCode,
      messages,
      sendMessage,
      user,
    ]
  );

  /**
   * 1. 开始编辑历史用户消息
   */
  const startEditMessage = useCallback((message: Message) => {
    setEditingMessageId(String(message.id));
    setEditingContent(message.content);
  }, []);

  /**
   * 1. 提交编辑后的消息，基于历史 checkpoint 分叉
   */
  const submitEditMessage = useCallback(
    async (message: Message) => {
      if (!currentConversationId || !user) return;
      // 编辑用户消息时，基于父消息创建新分支
      const parentId = message.parentId;
      try {
        latestStreamingRef.current = '';
        latestMessageIdRef.current = null;
        clearStreamingContent();
        sendMessage({
          conversationId: currentConversationId,
          content: editingContent,
          modelCode: currentModelCode || undefined,
          parentMessageId: parentId || undefined,
        });
      } catch (error) {
        console.error('编辑消息发送失败:', error);
      } finally {
        setEditingMessageId(null);
        setEditingContent('');
      }
    },
    [
      clearStreamingContent,
      currentConversationId,
      currentModelCode,
      editingContent,
      sendMessage,
      setCurrentCheckpointId,
      setMessages,
      setEditingMessageId,
      user,
    ]
  );

  const handleSend = useCallback(
    (content: string) => {
      if (!currentConversationId || !user) return;

      const userMessage: Message = {
        id: Date.now(),
        conversationId: currentConversationId,
        senderId: user.id,
        role: 'user',
        content,
        contentType: 'TEXT',
        createTime: new Date().toISOString(),
        checkpointId: currentCheckpointId || undefined,
      };
      // 1. 立即追加用户消息，保持本地流畅体验
      addMessage(userMessage);
      latestStreamingRef.current = '';
      latestMessageIdRef.current = null;
      clearStreamingContent();

      sendMessage({
        conversationId: currentConversationId,
        content,
        modelCode: currentModelCode || undefined,
      });
    },
    [currentConversationId, currentCheckpointId, currentModelCode, user, addMessage, sendMessage]
  );

  if (!currentConversationId) {
    return <EmptyState />;
  }

  return (
    <div className="flex-1 flex flex-col h-full bg-[var(--bg-primary)]">
      {/* Header */}
      <div className="px-6 py-4 border-b border-[var(--border-color)] flex items-center justify-between bg-[var(--bg-secondary)]">
        <div className="flex items-center gap-3">
          <div
            className="w-8 h-8 rounded-lg flex items-center justify-center"
            style={{ background: 'var(--accent-gradient)' }}
          >
            <MessageSquare className="w-4 h-4 text-white" />
          </div>
          <h2 className="font-semibold text-[var(--text-primary)]">对话</h2>
        </div>
        <ModelSelector />
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto">
        {messages.length === 0 && !streamingContent ? (
          <div className="h-full flex flex-col items-center justify-center text-[var(--text-secondary)] p-8">
            <div className="w-16 h-16 rounded-2xl glass-effect flex items-center justify-center mb-4">
              <MessageSquare className="w-8 h-8 opacity-50" />
            </div>
            <p className="text-center">发送消息开始对话</p>
          </div>
        ) : (
          <div className="max-w-4xl mx-auto py-4">
            {messages.map((message, index) => (
              <MessageBubble
                key={message.id}
                message={message}
                siblingInfo={
                  message.role === 'assistant' ? siblingInfoMap[String(message.id)] : undefined
                }
                onNavigateBranch={
                  message.role === 'assistant'
                    ? (direction) => handleNavigateBranch(String(message.id), direction)
                    : undefined
                }
                onRegenerate={
                  message.role === 'assistant'
                    ? () => handleRegenerate(message, index)
                    : undefined
                }
                isRegenerating={
                  regeneratingId === String(message.id) ||
                  navLoadingId === String(message.id)
                }
                isStreaming={streamingContent !== '' && message.id === -1}
                onEdit={
                  message.role === 'user'
                    ? () => startEditMessage(message)
                    : undefined
                }
                isEditing={editingMessageId === String(message.id)}
                editingContent={editingContent}
                onEditChange={setEditingContent}
                onEditSubmit={() => submitEditMessage(message)}
                onEditCancel={() => {
                  setEditingMessageId(null);
                  setEditingContent('');
                }}
              />
            ))}
            {/* 工具调用状态指示器 */}
            {activeTool && (
              <div className="tool-status-indicator">
                <div className="tool-status-icon">
                  <svg className="animate-spin" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M12 2v4m0 12v4m-8-10h4m12 0h4m-4.93-5.07l-2.83 2.83m-5.66 5.66l-2.83 2.83m11.32 0l-2.83-2.83m-5.66-5.66l-2.83-2.83" />
                  </svg>
                </div>
                <span className="tool-status-text">
                  正在{getToolDisplayName(activeTool)}...
                </span>
              </div>
            )}
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

      {/* Input */}
      <ChatInput
        isLoading={isLoading}
        onSend={handleSend}
        onAbort={abort}
      />
    </div>
  );
}
