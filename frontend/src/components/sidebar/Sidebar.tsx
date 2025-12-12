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
        'group flex items-center px-3 py-2 rounded-full cursor-pointer transition-colors text-sm',
        isActive
          ? 'bg-[#dbeafe] text-[#1e40af] font-medium' // Light blue bg, dark blue text
          : 'hover:bg-[var(--bg-tertiary)] text-[var(--text-primary)]'
      )}
      onClick={onClick}
    >
      <MessageSquare className={clsx("w-4 h-4 mr-3 flex-shrink-0", isActive ? "text-[#3b82f6]" : "text-gray-400")} />
      <div className="flex-1 min-w-0">
        <div className="truncate">{conversation.title || 'New Chat'}</div>
      </div>
      {/* 只有在 hover 时才显示删除按钮，且不干扰布局 */}
      <div className="w-6 h-6 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          className="p-1 hover:text-red-500 hover:bg-gray-200 rounded-full"
          onClick={(e) => {
            e.stopPropagation();
            onDelete();
          }}
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      </div>
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
        title: 'New Chat',
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
        className="fixed left-4 top-4 p-2 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors z-20"
        onClick={toggleSidebar}
      >
        <div className="w-5 h-5 flex flex-col gap-1 justify-center items-center">
            <span className="block w-4 h-0.5 bg-current rounded-full"></span>
            <span className="block w-4 h-0.5 bg-current rounded-full"></span>
            <span className="block w-4 h-0.5 bg-current rounded-full"></span>
        </div>
      </button>
    );
  }

  return (
    <div className="w-[280px] h-full bg-[var(--bg-secondary)] flex flex-col transition-all duration-300 ease-in-out">
      {/* 头部 - 极简 */}
      <div className="px-4 pt-4 pb-2 flex items-center justify-between">
         <button
          className="p-2 text-gray-500 hover:bg-gray-200 rounded-lg transition-colors"
          onClick={toggleSidebar}
        >
          <div className="w-5 h-5 flex flex-col gap-1 justify-center items-center">
            <span className="block w-4 h-0.5 bg-current rounded-full"></span>
            <span className="block w-4 h-0.5 bg-current rounded-full"></span>
            <span className="block w-4 h-0.5 bg-current rounded-full"></span>
          </div>
        </button>
      </div>

      {/* 新建会话按钮 - 胶囊状 */}
      <div className="px-4 py-4">
        <button
          className="w-full sm:w-auto flex items-center gap-3 px-4 py-3 bg-[var(--bg-tertiary)] hover:bg-gray-300 text-[var(--text-primary)] rounded-full transition-all duration-200 shadow-sm"
          onClick={handleCreateConversation}
        >
          <Plus className="w-5 h-5 text-gray-500" />
          <span className="text-sm font-medium">New chat</span>
        </button>
      </div>

      {/* 会话列表标题 */}
      <div className="px-6 py-2">
        <h3 className="text-xs font-semibold text-[var(--text-secondary)] uppercase tracking-wider">Recent</h3>
      </div>

      {/* 会话列表 */}
      <div className="flex-1 overflow-y-auto px-3 pb-4 space-y-1">
        {conversations.length === 0 ? (
          <div className="text-center text-[var(--text-secondary)] py-8 text-sm">
            No recent chats
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
      
      {/* 底部用户信息区域 (可选，占位) */}
      <div className="p-4 mt-auto border-t border-[var(--bg-tertiary)]">
          <div className="flex items-center gap-3 text-sm text-[var(--text-primary)] cursor-pointer hover:bg-[var(--bg-tertiary)] p-2 rounded-lg transition-colors">
             <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-400 to-purple-500"></div>
             <div className="font-medium">Settings</div>
          </div>
      </div>
    </div>
  );
}

