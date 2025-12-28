/**
 * 聊天面板组件 - Refined Modern Edition
 */
import { useEffect, useRef, useCallback, useState } from 'react';
import { Compass, Lightbulb, Code, Pencil, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';
import { useAppStore } from '../../store';
import { useSSEChat, getToolDisplayName, useMessageTree } from '../../hooks';
import { getConversationHistory } from '../../api';
import { setCurrentMessage } from '../../api/branch';
import { MessageBubble } from './MessageBubble';
import { ChatInput } from './ChatInput';
import { ModelSelector } from './ModelSelector';
import type { Message } from '../../types';

// Refined Clean Suggestion Card
function SuggestionCard({ icon: Icon, text, onClick, delay = 0, colorClass = "text-indigo-500" }: {
  icon: any,
  text: string,
  onClick: () => void,
  delay?: number,
  colorClass?: string
}) {
  return (
    <motion.button
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: delay * 0.05, duration: 0.4 }}
      whileHover={{ scale: 1.02, y: -2 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className="flex flex-col justify-between p-4 h-40 text-left group relative overflow-hidden bg-surface-container rounded-2xl border border-border/60 hover:border-border hover:shadow-lg transition-all duration-300"
    >
      <div className="absolute inset-0 bg-gradient-to-br from-transparent to-surface-highlight/30 opacity-0 group-hover:opacity-100 transition-opacity duration-500" />

      <span className="text-foreground/80 font-medium text-[15px] leading-relaxed relative z-10 group-hover:text-foreground transition-colors">
        {text}
      </span>

      <div className="self-end relative z-10 mt-auto">
        <div className={`p-2 rounded-xl bg-surface-highlight/50 group-hover:bg-surface-highlight transition-colors duration-300`}>
          <Icon className={`w-5 h-5 ${colorClass}`} />
        </div>
      </div>
    </motion.button>
  );
}

/**
 * Refined Greeting Screen
 */
function GreetingScreen({ userName, onSuggestionClick }: { userName: string, onSuggestionClick: (text: string) => void }) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center p-6 max-w-5xl mx-auto w-full min-h-[60vh]">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="mb-16 text-center w-full relative z-10"
      >
        <div className="w-16 h-16 rounded-2xl bg-surface-container-high border border-border flex items-center justify-center mx-auto mb-6 shadow-sm">
          <Sparkles className="w-8 h-8 text-primary" />
        </div>
        <h1 className="text-4xl md:text-5xl font-semibold mb-3 tracking-tight text-foreground">
          Welcome back, {userName}
        </h1>
        <p className="text-xl text-muted font-light">
          How can I help you today?
        </p>
      </motion.div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 w-full relative z-10">
        <SuggestionCard
          icon={Compass}
          text="Plan a trip to explore hidden gems in Kyoto"
          onClick={() => onSuggestionClick("Plan a trip to explore hidden gems in Kyoto")}
          delay={1}
          colorClass="text-teal-500"
        />
        <SuggestionCard
          icon={Lightbulb}
          text="Brainstorm catchy taglines for a coffee brand"
          onClick={() => onSuggestionClick("Brainstorm catchy taglines for a coffee brand")}
          delay={2}
          colorClass="text-amber-500"
        />
        <SuggestionCard
          icon={Code}
          text="Explain how React useEffect works with examples"
          onClick={() => onSuggestionClick("Explain how React useEffect works with examples")}
          delay={3}
          colorClass="text-indigo-500"
        />
        <SuggestionCard
          icon={Pencil}
          text="Write a polite email declining a job offer"
          onClick={() => onSuggestionClick("Write a polite email declining a job offer")}
          delay={4}
          colorClass="text-rose-500"
        />
      </div>
    </div>
  );
}

export function ChatPanel() {
  const {
    currentConversationId,
    currentModelCode,
    streamingContent,
    setStreamingContent,
    clearStreamingContent,
    user,
    setCurrentCheckpointId,
    updateConversation,
  } = useAppStore();

  const {
    displayMessages,
    setMessageTree,
    switchBranch,
    getSiblingInfo,
    addMessage,
    replaceMessageId,
  } = useMessageTree({
    onSaveCurrentMessage: async (messageId) => {
      if (currentConversationId) {
        await setCurrentMessage(currentConversationId, messageId);
      }
    },
  });

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const latestStreamingRef = useRef<string>('');
  const latestMessageIdRef = useRef<number | string | null>(null);
  const pendingTempUserIdRef = useRef<string | null>(null); // 跟踪待替换的临时用户消息 ID
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
    onComplete: async (event, finalContent) => {
      const contentToSave = finalContent || latestStreamingRef.current;
      if (contentToSave) {
        latestMessageIdRef.current = event.messageId ?? null;

        // 替换临时用户消息 ID 为服务器返回的真实 ID
        if (pendingTempUserIdRef.current && event.userMessageId) {
          replaceMessageId(pendingTempUserIdRef.current, String(event.userMessageId));
          pendingTempUserIdRef.current = null;
        }

        // 先静默刷新历史，等加载完成后再清空流式内容，实现平滑过渡
        await loadHistory();
        setRegeneratingId(null);
        setNavLoadingId(null);
      }
      // 如果后端返回了新标题，更新会话列表中的标题
      if (event.title && event.conversationId) {
        updateConversation(event.conversationId, { title: event.title });
      }
      // 历史加载完成后再清空流式内容，避免闪烁
      latestStreamingRef.current = '';
      clearStreamingContent();
    },
    onError: (error) => {
      console.error('Chat Error:', error);
      latestStreamingRef.current = '';
      clearStreamingContent();
      setRegeneratingId(null);
      setNavLoadingId(null);
    },
  });

  useEffect(() => {
    if (currentConversationId) loadHistory();
  }, [currentConversationId]);

  async function loadHistory() {
    if (!currentConversationId) return;
    try {
      const data = await getConversationHistory(currentConversationId);
      setMessageTree(data.messages, data.currentMessageId);
      const lastCheckpoint = [...data.messages].reverse().find((item) => item.checkpointId)?.checkpointId || null;
      setCurrentCheckpointId(lastCheckpoint);
      setNavLoadingId(null);
      setRegeneratingId(null);
      setEditingMessageId(null);
      setEditingContent('');
    } catch (error) {
      console.error('Failed to load history:', error);
    }
  }


  const handleNavigateBranch = useCallback((messageId: string, direction: 'prev' | 'next') => {
    if (!currentConversationId || navLoadingId) return;
    setNavLoadingId(messageId);
    try {
      const info = getSiblingInfo(messageId);
      if (!info || info.total <= 1) return;
      const nextIndex = direction === 'prev' ? info.current - 1 : info.current + 1;
      if (nextIndex < 0 || nextIndex >= info.total) return;
      switchBranch(info.siblings[nextIndex]);
    } catch (error) {
      console.error('Branch switch failed:', error);
    } finally {
      setNavLoadingId(null);
    }
  }, [currentConversationId, navLoadingId, getSiblingInfo, switchBranch]);

  const handleRegenerate = useCallback((message: Message, index: number) => {
    if (!currentConversationId || !user) return;
    const lastUser = [...displayMessages].slice(0, index).reverse().find((item) => item.role === 'user');
    if (!lastUser) return;
    const parentMessageId = String(lastUser.id);
    setRegeneratingId(String(message.id));
    regeneratingIdRef.current = String(message.id);
    latestStreamingRef.current = '';
    clearStreamingContent();
    sendMessage({
      conversationId: currentConversationId,
      content: lastUser.content,
      modelCode: currentModelCode || undefined,
      parentMessageId: parentMessageId,
      regenerate: true,
    });
  }, [clearStreamingContent, currentConversationId, currentModelCode, displayMessages, sendMessage, user]);

  const startEditMessage = useCallback((message: Message) => {
    setEditingMessageId(String(message.id));
    setEditingContent(message.content);
  }, []);

  const submitEditMessage = useCallback(async (message: Message) => {
    if (!currentConversationId || !user) return;
    try {
      latestStreamingRef.current = '';
      clearStreamingContent();
      sendMessage({
        conversationId: currentConversationId,
        content: editingContent,
        modelCode: currentModelCode || undefined,
        parentMessageId: message.parentId || undefined,
      });
    } catch (error) {
      console.error('Edit failed:', error);
    } finally {
      setEditingMessageId(null);
      setEditingContent('');
    }
  }, [clearStreamingContent, currentConversationId, currentModelCode, editingContent, sendMessage, user]);

  const handleSend = useCallback((content: string) => {
    if (!currentConversationId || !user) return;
    const lastMessage = displayMessages.length > 0 ? displayMessages[displayMessages.length - 1] : null;
    const parentMessageId = lastMessage ? String(lastMessage.id) : undefined;

    // 乐观更新：立即显示用户消息，无需等待服务器
    const tempId = `temp-${Date.now()}`;
    const tempUserMessage: Message = {
      id: tempId,
      conversationId: currentConversationId,
      senderId: user.id,
      role: 'user',
      content,
      contentType: 'TEXT',
      createTime: new Date().toISOString(),
      parentId: parentMessageId,
    };
    addMessage(tempUserMessage);
    pendingTempUserIdRef.current = tempId; // 保存临时 ID 以便后续替换

    latestStreamingRef.current = '';
    clearStreamingContent();
    sendMessage({
      conversationId: currentConversationId,
      content,
      modelCode: currentModelCode || undefined,
      parentMessageId,
    });
  }, [currentConversationId, currentModelCode, user, displayMessages, sendMessage, clearStreamingContent, addMessage]);

  // 滚动相关
  const containerRef = useRef<HTMLDivElement>(null);

  // 消息变化时滚动到底部
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [displayMessages.length, streamingContent]);

  if (!currentConversationId) {
    return <GreetingScreen userName={user?.userName || 'Traveler'} onSuggestionClick={handleSend} />;
  }

  return (
    <div className="flex-1 flex flex-col h-full relative bg-surface">
      {/* Header with Model Selector */}
      <div className="absolute top-0 left-0 right-0 p-4 z-10 flex justify-between items-start pointer-events-none">
        <div className="pointer-events-auto">
          <ModelSelector />
        </div>
      </div>

      {/* Main Content Area */}
      <div ref={containerRef} className="flex-1 overflow-hidden w-full pt-16">
        {displayMessages.length === 0 && !streamingContent ? (
          <GreetingScreen userName={user?.userName || 'User'} onSuggestionClick={handleSend} />
        ) : (
          <div className="h-full overflow-y-auto scrollbar-thin pb-8">
            <div className="max-w-3xl mx-auto px-4 py-4 space-y-4">
              {displayMessages.map((message, index) => (
                <motion.div
                  key={message.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3 }}
                >
                  <MessageBubble
                    message={message}
                    siblingInfo={message.role === 'assistant' ? getSiblingInfo(String(message.id)) ?? undefined : undefined}
                    onNavigateBranch={message.role === 'assistant' ? (direction) => handleNavigateBranch(String(message.id), direction) : undefined}
                    onRegenerate={message.role === 'assistant' ? () => handleRegenerate(message, index) : undefined}
                    isRegenerating={regeneratingId === String(message.id) || navLoadingId === String(message.id)}
                    isStreaming={streamingContent !== '' && message.id === -1}
                    onEdit={message.role === 'user' ? () => startEditMessage(message) : undefined}
                    isEditing={editingMessageId === String(message.id)}
                    editingContent={editingContent}
                    onEditChange={setEditingContent}
                    onEditSubmit={() => submitEditMessage(message)}
                    onEditCancel={() => { setEditingMessageId(null); setEditingContent(''); }}
                    userAvatar={message.role === 'user' ? user?.avatar : undefined}
                  />
                </motion.div>
              ))}

              {/* 流式内容和工具状态 */}
              {activeTool && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="flex items-center gap-3 p-4 text-sm text-muted"
                >
                  <div className="w-5 h-5 rounded-full border-2 border-primary border-t-transparent animate-spin" />
                  <span className="gradient-text font-medium">Running {getToolDisplayName(activeTool)}...</span>
                </motion.div>
              )}

              {streamingContent && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                >
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
                </motion.div>
              )}
              <div ref={messagesEndRef} />
            </div>
          </div>
        )}
      </div>

      {/* Footer / Input Area */}
      <ChatInput
        isLoading={isLoading}
        onSend={handleSend}
        onAbort={abort}
      />
    </div>
  );
}
