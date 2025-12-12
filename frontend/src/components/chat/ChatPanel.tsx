/**
 * 聊天面板组件
 *
 * 显示消息列表和输入框
 */
import { useEffect, useRef, useCallback } from 'react';
import { useAppStore } from '../../store';
import { useSSEChat } from '../../hooks';
import { getConversationHistory } from '../../api';
import { MessageBubble } from './MessageBubble';
import { ChatInput } from './ChatInput';
import type { Message } from '../../types';

/**
 * 空状态组件
 */
function EmptyState() {
  const { user } = useAppStore();
  const getGreeting = () => {
      const hour = new Date().getHours();
      if (hour < 12) return "Good morning";
      if (hour < 18) return "Good afternoon";
      return "Good evening";
  };

  return (
    <div className="flex-1 flex flex-col items-start justify-center p-8 max-w-3xl mx-auto w-full animate-in fade-in duration-500">
      <h1 className="text-6xl font-medium tracking-tight mb-2 text-[#c4c7c5]">
        <span className="bg-gradient-to-r from-blue-500 via-purple-500 to-red-500 bg-clip-text text-transparent font-semibold">
          Hello, {user?.userName || 'Traveler'}
        </span>
      </h1>
      <h2 className="text-4xl font-medium text-[#c4c7c5] mb-12">
        How can I help you today?
      </h2>
      
      {/* Optional: Suggestion chips could go here */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 w-full">
         <div className="p-4 bg-[#f4f4f5] rounded-xl hover:bg-[#e4e4e7] cursor-pointer transition-colors">
            <p className="text-sm font-medium text-gray-700">Planning a trip</p>
            <p className="text-xs text-gray-500 mt-1">Suggest the best hiking trails near Tokyo</p>
         </div>
         <div className="p-4 bg-[#f4f4f5] rounded-xl hover:bg-[#e4e4e7] cursor-pointer transition-colors">
            <p className="text-sm font-medium text-gray-700">Write a story</p>
            <p className="text-xs text-gray-500 mt-1">About a cat who becomes a software engineer</p>
         </div>
      </div>
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
          sendTime: new Date().toISOString(),
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

      const userMessage: Message = {
        id: Date.now(),
        conversationId: currentConversationId,
        senderId: user.id,
        role: 'user',
        content,
        contentType: 'TEXT',
        sendTime: new Date().toISOString(),
      };
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
    [currentConversationId, currentModelCode, user, addMessage, sendMessage]
  );

  return (
    <div className="flex-1 flex flex-col h-full bg-white relative">
      {/* 头部 - 极简，只显示模型选择 */}
      <div className="absolute top-0 left-0 right-0 z-10 flex justify-between items-center px-6 py-4 bg-white/80 backdrop-blur-sm">
         <div className="text-lg font-medium text-gray-700 flex items-center gap-2 cursor-pointer hover:bg-gray-100 px-3 py-1 rounded-lg transition-colors">
            Gemini <span className="text-gray-400 text-sm">2.0 Flash</span>
         </div>
         {/* <ModelSelector /> 可以重构得更漂亮，暂时先放这里或隐藏 */}
      </div>

      {/* 消息列表区域 */}
      <div className="flex-1 overflow-y-auto pt-20 pb-32"> {/* Padding top for header, bottom for input */}
        {!currentConversationId || (messages.length === 0 && !streamingContent) ? (
          <EmptyState />
        ) : (
          <div className="flex flex-col gap-2 pb-4">
             {messages.map((message) => (
              <MessageBubble key={message.id} message={message} />
            ))}
            {streamingContent && (
              <MessageBubble
                message={{
                  id: -1,
                  conversationId: currentConversationId!,
                  senderId: -1,
                  role: 'assistant',
                  content: streamingContent,
                  contentType: 'TEXT',
                  sendTime: new Date().toISOString(),
                }}
                isStreaming={true}
              />
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* 输入框 - 悬浮在底部 */}
      <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-white via-white to-transparent">
        <ChatInput
            isLoading={isLoading}
            onSend={handleSend}
            onAbort={abort}
        />
      </div>
    </div>
  );
}
