/**
 * 侧边栏组件
 *
 * 显示会话列表和操作按钮
 */
import { useEffect } from 'react';
import { Plus, MessageSquare, Trash2, ChevronLeft } from 'lucide-react';
import { useAppStore } from '../../store';
import { getConversations, createConversation, deleteConversation } from '../../api';
import type { Conversation } from '../../types';
import clsx from 'clsx';

/**
 * 格式化时间显示
 */
function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  // 一天内显示时间
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  // 一周内显示星期
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return days[date.getDay()];
  }

  // 其他显示日期
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
  });
}

/**
 * 会话项组件
 */
function ConversationItem({
  conversation,
  isActive,
  onClick,
  onDelete,
}: {
  conversation: Conversation;
  isActive: boolean;
  onClick: () => void;
  onDelete: () => void;
}) {
  return (
    <div
      className={clsx(
        'group flex items-center px-3 py-2 rounded-lg cursor-pointer transition-colors',
        isActive
          ? 'bg-[var(--bg-tertiary)] text-[var(--text-primary)]'
          : 'hover:bg-[var(--bg-secondary)] text-[var(--text-secondary)]'
      )}
      onClick={onClick}
    >
      <MessageSquare className="w-4 h-4 mr-3 flex-shrink-0" />
      <div className="flex-1 min-w-0">
        <div className="truncate text-sm">{conversation.title || '新对话'}</div>
        {conversation.lastMessageAt && (
          <div className="text-xs text-[var(--text-secondary)] mt-0.5">
            {formatTime(conversation.lastMessageAt)}
          </div>
        )}
      </div>
      <button
        className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-400 transition-opacity"
        onClick={(e) => {
          e.stopPropagation();
          onDelete();
        }}
      >
        <Trash2 className="w-4 h-4" />
      </button>
    </div>
  );
}

/**
 * 侧边栏组件
 */
export function Sidebar() {
  const {
    conversations,
    currentConversationId,
    sidebarOpen,
    setConversations,
    addConversation,
    removeConversation,
    setCurrentConversationId,
    toggleSidebar,
    token,
  } = useAppStore();

  /** 加载会话列表 */
  useEffect(() => {
    if (token) {
      loadConversations();
    }
  }, [token]);

  async function loadConversations() {
    try {
      const data = await getConversations();
      setConversations(data);
    } catch (error) {
      console.error('加载会话列表失败:', error);
    }
  }

  /** 创建新会话 */
  async function handleCreateConversation() {
    try {
      const newConversation = await createConversation({
        title: '新对话',
      });
      addConversation(newConversation);
      setCurrentConversationId(newConversation.id);
    } catch (error) {
      console.error('创建会话失败:', error);
    }
  }

  /** 删除会话 */
  async function handleDeleteConversation(id: number) {
    try {
      await deleteConversation(id);
      removeConversation(id);
    } catch (error) {
      console.error('删除会话失败:', error);
    }
  }

  if (!sidebarOpen) {
    return (
      <button
        className="fixed left-4 top-4 p-2 bg-[var(--bg-secondary)] rounded-lg hover:bg-[var(--bg-tertiary)] transition-colors z-10"
        onClick={toggleSidebar}
      >
        <ChevronLeft className="w-5 h-5 rotate-180" />
      </button>
    );
  }

  return (
    <div className="w-64 h-full bg-[var(--bg-secondary)] border-r border-[var(--border-color)] flex flex-col">
      {/* 头部 */}
      <div className="p-4 border-b border-[var(--border-color)] flex items-center justify-between">
        <h1 className="text-lg font-semibold text-[var(--accent-primary)]">
          AI Chat
        </h1>
        <button
          className="p-1 hover:bg-[var(--bg-tertiary)] rounded transition-colors"
          onClick={toggleSidebar}
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
      </div>

      {/* 新建会话按钮 */}
      <div className="p-3">
        <button
          className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-[var(--accent-primary)] text-white rounded-lg hover:opacity-90 transition-opacity"
          onClick={handleCreateConversation}
        >
          <Plus className="w-4 h-4" />
          新建对话
        </button>
      </div>

      {/* 会话列表 */}
      <div className="flex-1 overflow-y-auto p-2">
        {conversations.length === 0 ? (
          <div className="text-center text-[var(--text-secondary)] py-8 text-sm">
            暂无对话，点击上方按钮创建
          </div>
        ) : (
          <div className="space-y-1">
            {conversations.map((conversation) => (
              <ConversationItem
                key={conversation.id}
                conversation={conversation}
                isActive={conversation.id === currentConversationId}
                onClick={() => setCurrentConversationId(conversation.id)}
                onDelete={() => handleDeleteConversation(conversation.id)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
