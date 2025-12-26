/**
 * 聊天面板组件 - Premium Edition
 */
import { useEffect, useRef, useCallback, useState } from 'react';
import { Compass, Lightbulb, Code, Pencil } from 'lucide-react';
import { motion } from 'framer-motion';
import { useAppStore } from '../../store';
import { useSSEChat, getToolDisplayName, useMessageTree } from '../../hooks';
import { getConversationHistory } from '../../api';
import { setCurrentMessage } from '../../api/branch';
import { MessageBubble } from './MessageBubble';
import { ChatInput } from './ChatInput';
import { ModelSelector } from './ModelSelector';
import type { Message } from '../../types';

// Premium Colorful Suggestion Card
function SuggestionCard({ icon: Icon, text, onClick, delay = 0, colorFrom = "from-indigo-500", colorTo = "to-purple-500" }: {
  icon: any,
  text: string,
  onClick: () => void,
  delay?: number,
  colorFrom?: string,
  colorTo?: string
}) {
  return (
    <motion.button
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: delay * 0.1, duration: 0.4 }}
      whileHover={{ scale: 1.05, y: -6 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className="gemini-card flex flex-col justify-between p-5 h-52 text-left group relative overflow-hidden"
    >
      {/* Colorful glow effect on hover */}
      <div className={`absolute inset-0 bg-gradient-to-br ${colorFrom}/10 ${colorTo}/10 opacity-0 group-hover:opacity-100 transition-opacity duration-500`} />
      <div className={`absolute -top-10 -right-10 w-32 h-32 bg-gradient-to-br ${colorFrom} ${colorTo} rounded-full blur-2xl opacity-0 group-hover:opacity-20 transition-opacity duration-500`} />

      <span className="text-foreground font-medium text-base leading-relaxed relative z-10">{text}</span>

      <div className="self-end relative z-10">
        <div className={`p-3 rounded-xl bg-gradient-to-br ${colorFrom}/15 ${colorTo}/15 group-hover:${colorFrom}/25 group-hover:${colorTo}/25 transition-all duration-300 group-hover:shadow-lg`}>
          <Icon className={`w-5 h-5 bg-gradient-to-r ${colorFrom} ${colorTo} bg-clip-text text-transparent group-hover:scale-110 transition-transform duration-300`}
            style={{ color: 'transparent', fill: 'url(#icon-gradient)' }}
          />
          <svg width="0" height="0">
            <defs>
              <linearGradient id="icon-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style={{ stopColor: '#6366f1' }} />
                <stop offset="100%" style={{ stopColor: '#a855f7' }} />
              </linearGradient>
            </defs>
          </svg>
        </div>
      </div>
    </motion.button>
  );
}

/**
 * Premium Greeting Screen
 */
function GreetingScreen({ userName, onSuggestionClick }: { userName: string, onSuggestionClick: (text: string) => void }) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center p-8 max-w-4xl mx-auto w-full">
      {/* Premium Glow Background */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-secondary/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="mb-12 text-left self-start w-full relative z-10"
      >
        <h1 className="text-5xl md:text-6xl font-medium mb-4 tracking-tight">
          <motion.span
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="gradient-text-animated block"
          >
            Hello, {userName}
          </motion.span>
          <motion.span
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4 }}
            className="text-muted/50 font-medium block mt-2"
          >
            How can I help today?
          </motion.span>
        </h1>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 w-full relative z-10">
        <SuggestionCard
          icon={Compass}
          text="Plan a trip to explore the city's hidden gems"
          onClick={() => onSuggestionClick("Plan a trip to explore the city's hidden gems")}
          delay={1}
          colorFrom="from-teal-500"
          colorTo="to-cyan-500"
        />
        <SuggestionCard
          icon={Lightbulb}
          text="Brainstorm clear and catchy tagline for my product"
          onClick={() => onSuggestionClick("Brainstorm clear and catchy tagline for my product")}
          delay={2}
          colorFrom="from-amber-500"
          colorTo="to-orange-500"
        />
        <SuggestionCard
          icon={Code}
          text="Refactor this React component to be more performant"
          onClick={() => onSuggestionClick("Refactor this React component to be more performant")}
          delay={3}
          colorFrom="from-indigo-500"
          colorTo="to-purple-500"
        />
        <SuggestionCard
          icon={Pencil}
          text="Write a thank you note to my interviewer"
          onClick={() => onSuggestionClick("Write a thank you note to my interviewer")}
          delay={4}
          colorFrom="from-rose-500"
          colorTo="to-pink-500"
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

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [displayMessages, streamingContent]);

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
    const tempUserMessage: Message = {
      id: `temp-${Date.now()}`,
      conversationId: currentConversationId,
      senderId: user.id,
      role: 'user',
      content,
      contentType: 'TEXT',
      createTime: new Date().toISOString(),
      parentId: parentMessageId,
    };
    addMessage(tempUserMessage);

    latestStreamingRef.current = '';
    clearStreamingContent();
    sendMessage({
      conversationId: currentConversationId,
      content,
      modelCode: currentModelCode || undefined,
      parentMessageId,
    });
  }, [currentConversationId, currentModelCode, user, displayMessages, sendMessage, clearStreamingContent, addMessage]);

  if (!currentConversationId) {
    return <GreetingScreen userName={user?.userName || 'Traveler'} onSuggestionClick={() => { }} />;
  }

  return (
    <div className="flex-1 flex flex-col h-full relative">
      {/* Header with Model Selector */}
      <div className="absolute top-0 left-0 right-0 p-4 z-10 flex justify-between items-start pointer-events-none">
        <div className="pointer-events-auto">
          <ModelSelector />
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 overflow-y-auto w-full">
        {displayMessages.length === 0 && !streamingContent ? (
          <GreetingScreen userName={user?.userName || 'User'} onSuggestionClick={handleSend} />
        ) : (
          <div className="max-w-3xl mx-auto py-20 pb-48 px-4">
            {displayMessages.map((message: Message, index: number) => (
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
                />
              </motion.div>
            ))}

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
