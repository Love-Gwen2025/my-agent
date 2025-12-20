/**
 * 侧边栏组件
 *
 * Modern sidebar with gradient buttons and hover effects
 */
import { useEffect, useState } from 'react';
import { Plus, MessageSquare, Trash2, ChevronLeft, Sparkles, LogOut, Settings } from 'lucide-react';
import { useAppStore } from '../../store';
import { getConversations, createConversation, deleteConversation } from '../../api';
import { ThemePanel } from '../settings';
import type { Conversation } from '../../types';
import clsx from 'clsx';

/**
 * 格式化时间显示
 */
function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return days[date.getDay()];
  }

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
        'group flex items-center px-3 py-3 rounded-xl cursor-pointer transition-all duration-200',
        isActive
          ? 'bg-[var(--accent-primary)]/10 text-[var(--accent-primary)] border border-[var(--accent-primary)]/30'
          : 'hover:bg-[var(--bg-tertiary)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
      )}
      onClick={onClick}
    >
      <div className={clsx(
        'w-8 h-8 rounded-lg flex items-center justify-center mr-3 flex-shrink-0 transition-colors',
        isActive ? 'bg-[var(--accent-primary)]/20' : 'bg-[var(--bg-tertiary)]'
      )}>
        <MessageSquare className="w-4 h-4" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="truncate text-sm font-medium">{conversation.title || '新对话'}</div>
        {conversation.lastMessageAt && (
          <div className="text-xs text-[var(--text-secondary)] mt-0.5 opacity-70">
            {formatTime(conversation.lastMessageAt)}
          </div>
        )}
      </div>
      <button
        className="opacity-0 group-hover:opacity-100 p-1.5 hover:bg-red-500/10 hover:text-red-400 rounded-lg transition-all"
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
  // 主题设置面板状态
  const [isThemePanelOpen, setIsThemePanelOpen] = useState(false);

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
    logout,
    user,
  } = useAppStore();

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

  async function handleDeleteConversation(id: string) {
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
        className="fixed left-4 top-4 p-3 glass-effect rounded-xl hover:bg-[var(--bg-tertiary)] transition-all z-10 shadow-lg"
        onClick={toggleSidebar}
      >
        <ChevronLeft className="w-5 h-5 rotate-180 text-[var(--text-primary)]" />
      </button>
    );
  }

  return (
    <div className="w-72 h-full bg-[var(--bg-secondary)] border-r border-[var(--border-color)] flex flex-col">
      {/* Header */}
      <div className="p-4 border-b border-[var(--border-color)] flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div
            className="w-8 h-8 rounded-lg flex items-center justify-center"
            style={{ background: 'var(--accent-gradient)' }}
          >
            <Sparkles className="w-4 h-4 text-white" />
          </div>
          <h1 className="text-lg font-bold gradient-text">
            AI Chat
          </h1>
        </div>
        <button
          className="p-2 hover:bg-[var(--bg-tertiary)] rounded-lg transition-colors"
          onClick={toggleSidebar}
        >
          <ChevronLeft className="w-5 h-5 text-[var(--text-secondary)]" />
        </button>
      </div>

      {/* New chat button */}
      <div className="p-3">
        <button
          className="w-full flex items-center justify-center gap-2 px-4 py-3 text-white rounded-xl font-medium btn-gradient shadow-lg"
          onClick={handleCreateConversation}
        >
          <Plus className="w-5 h-5" />
          新建对话
        </button>
      </div>

      {/* Conversation list */}
      <div className="flex-1 overflow-y-auto p-2 space-y-1">
        {conversations.length === 0 ? (
          <div className="text-center text-[var(--text-secondary)] py-12 px-4">
            <MessageSquare className="w-12 h-12 mx-auto mb-3 opacity-30" />
            <p className="text-sm">暂无对话</p>
            <p className="text-xs mt-1 opacity-70">点击上方按钮开始</p>
          </div>
        ) : (
          conversations.map((conversation) => (
            <ConversationItem
              key={conversation.id}
              conversation={conversation}
              isActive={conversation.id === currentConversationId}
              onClick={() => setCurrentConversationId(conversation.id)}
              onDelete={() => handleDeleteConversation(conversation.id)}
            />
          ))
        )}
      </div>

      {/* User section */}
      <div className="p-3 border-t border-[var(--border-color)]">
        {/* 设置按钮 */}
        <button
          onClick={() => setIsThemePanelOpen(true)}
          className="w-full flex items-center gap-3 p-2 mb-2 rounded-xl hover:bg-[var(--bg-tertiary)] transition-colors text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
        >
          <div className="w-10 h-10 rounded-xl bg-[var(--bg-tertiary)] flex items-center justify-center">
            <Settings className="w-5 h-5" />
          </div>
          <span className="text-sm font-medium">外观设置</span>
        </button>

        {/* 用户信息 */}
        <div className="flex items-center gap-3 p-2 rounded-xl hover:bg-[var(--bg-tertiary)] transition-colors cursor-pointer group">
          <div
            className="w-10 h-10 rounded-xl flex items-center justify-center text-white font-medium"
            style={{ background: 'var(--accent-gradient)' }}
          >
            {user?.userName?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-sm font-medium text-[var(--text-primary)] truncate">
              {user?.userName || '用户'}
            </div>
            <div className="text-xs text-[var(--text-secondary)]">
              {user?.userCode || ''}
            </div>
          </div>
          <button
            className="p-2 opacity-0 group-hover:opacity-100 hover:bg-red-500/10 hover:text-red-400 rounded-lg transition-all"
            onClick={logout}
            title="退出登录"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* 主题设置面板 */}
      <ThemePanel isOpen={isThemePanelOpen} onClose={() => setIsThemePanelOpen(false)} />
    </div>
  );
}
