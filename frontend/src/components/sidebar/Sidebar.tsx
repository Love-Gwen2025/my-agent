/**
 * Gemini Sidebar - Premium Colorful Edition
 */
import { useEffect, useState, useRef, useCallback } from 'react';
import { Plus, MessageSquare, Trash2, Menu, Settings, Pencil } from 'lucide-react';
import { motion } from 'framer-motion';
import { useAppStore } from '../../store';
import { getConversations, createConversation, deleteConversation, updateConversationTitle } from '../../api';
import clsx from 'clsx';
import { ThemePanel, UserProfileModal } from '../settings';

function SidebarItem({
  icon: Icon,
  label,
  isActive,
  onClick,
  onEdit,
  onDelete,
  isCollapsed,
  colorClass = "text-muted"
}: {
  icon: any;
  label: string;
  isActive?: boolean;
  onClick: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
  isCollapsed: boolean;
  colorClass?: string;
}) {
  return (
    <motion.div
      whileHover={{ scale: 1.02, x: 4 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className={clsx(
        "flex items-center gap-3 px-3 py-2.5 rounded-2xl cursor-pointer transition-all duration-300 group relative",
        isActive
          ? "bg-gradient-to-r from-primary/20 via-secondary/15 to-accent/10 text-foreground shadow-sm border border-primary/20"
          : "hover:bg-surface-container-high/80 text-foreground",
        isCollapsed ? "justify-center w-12 h-12 px-0 mx-auto" : "w-full"
      )}
      title={isCollapsed ? label : undefined}
    >
      <Icon className={clsx(
        "w-5 h-5 flex-shrink-0 transition-all duration-300",
        isActive ? "text-primary scale-110" : `${colorClass} group-hover:text-primary group-hover:scale-105`
      )} />

      {!isCollapsed && (
        <span className={clsx(
          "text-sm truncate flex-1 transition-colors duration-300",
          isActive ? "font-semibold text-primary" : "font-medium"
        )}>
          {label}
        </span>
      )}

      {/* 编辑和删除按钮 */}
      {!isCollapsed && (onEdit || onDelete) && (
        <div className="opacity-0 group-hover:opacity-100 absolute right-2 flex items-center gap-1 transition-opacity duration-200">
          {onEdit && (
            <button
              onClick={(e) => { e.stopPropagation(); onEdit(); }}
              className="p-1.5 hover:bg-primary/20 hover:text-primary rounded-full transition-all duration-200"
              title="重命名"
            >
              <Pencil className="w-4 h-4" />
            </button>
          )}
          {onDelete && (
            <button
              onClick={(e) => { e.stopPropagation(); onDelete(); }}
              className="p-1.5 hover:bg-red-500/20 hover:text-red-500 rounded-full transition-all duration-200"
              title="删除"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      )}
    </motion.div>
  );
}

export function Sidebar() {
  const [isThemePanelOpen, setIsThemePanelOpen] = useState(false);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  // 编辑状态
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingTitle, setEditingTitle] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  // Settings按钮ref，用于定位ThemePanel
  const settingsButtonRef = useRef<HTMLDivElement>(null);
  // 滚动容器ref
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  // 分页状态
  const [hasMore, setHasMore] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  // 存储Settings按钮位置
  const [settingsAnchorRect, setSettingsAnchorRect] = useState<{
    top: number;
    left: number;
    width: number;
    height: number;
  } | null>(null);

  const {
    conversations,
    currentConversationId,
    sidebarOpen,
    toggleSidebar,
    setConversations,
    addConversation,
    removeConversation,
    updateConversation,
    setCurrentConversationId,
    token,
    user,
  } = useAppStore();

  useEffect(() => {
    if (token) loadConversations();
  }, [token]);

  // 编辑时自动聚焦
  useEffect(() => {
    if (editingId && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editingId]);

  async function loadConversations() {
    try {
      const { items, hasMore: more } = await getConversations(20, 0);
      setConversations(items);
      setHasMore(more);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    }
  }

  // 加载更多会话
  async function loadMoreConversations() {
    if (isLoadingMore || !hasMore) return;
    setIsLoadingMore(true);
    try {
      const { items, hasMore: more } = await getConversations(20, conversations.length);
      setConversations([...conversations, ...items]);
      setHasMore(more);
    } catch (error) {
      console.error('Failed to load more conversations:', error);
    } finally {
      setIsLoadingMore(false);
    }
  }

  // 滚动到底部时加载更多
  const handleScroll = useCallback(() => {
    const container = scrollContainerRef.current;
    if (!container || isLoadingMore || !hasMore) return;

    const { scrollTop, scrollHeight, clientHeight } = container;
    if (scrollHeight - scrollTop - clientHeight < 100) {
      loadMoreConversations();
    }
  }, [isLoadingMore, hasMore, conversations.length]);

  async function handleCreateConversation() {
    try {
      const newConversation = await createConversation({ title: 'New chat' });
      addConversation(newConversation);
      setCurrentConversationId(newConversation.id);
    } catch (error) {
      console.error('Failed to create conversation:', error);
    }
  }

  async function handleDeleteConversation(id: string) {
    try {
      await deleteConversation(id);
      removeConversation(id);
    } catch (error) {
      console.error('Failed to delete conversation:', error);
    }
  }

  // 开始编辑会话标题
  function handleEditConversation(id: string, currentTitle: string) {
    setEditingId(id);
    setEditingTitle(currentTitle || '');
  }

  // 保存编辑的标题
  async function handleSaveTitle() {
    if (!editingId || !editingTitle.trim()) {
      setEditingId(null);
      return;
    }
    try {
      await updateConversationTitle(editingId, editingTitle.trim());
      updateConversation(editingId, { title: editingTitle.trim() });
    } catch (error) {
      console.error('Failed to update conversation title:', error);
    } finally {
      setEditingId(null);
    }
  }

  // 取消编辑
  function handleCancelEdit() {
    setEditingId(null);
    setEditingTitle('');
  }

  // 打开设置面板时获取按钮位置
  const handleOpenThemePanel = useCallback(() => {
    if (settingsButtonRef.current) {
      const rect = settingsButtonRef.current.getBoundingClientRect();
      setSettingsAnchorRect({
        top: rect.top,
        left: rect.left,
        width: rect.width,
        height: rect.height,
      });
    }
    setIsThemePanelOpen(true);
  }, []);

  return (
    <motion.div
      initial={false}
      animate={{ width: sidebarOpen ? 280 : 80 }}
      transition={{ type: "spring", stiffness: 300, damping: 30 }}
      className="glass h-full flex flex-col pt-4 pb-4 z-20 flex-shrink-0"
    >
      {/* Header with Menu */}
      <div className={clsx("px-4 mb-4 flex items-center", sidebarOpen ? "justify-between" : "justify-center")}>
        <motion.button
          whileHover={{ scale: 1.1, rotate: 180 }}
          whileTap={{ scale: 0.9 }}
          onClick={toggleSidebar}
          className="p-2.5 hover:bg-gradient-to-r hover:from-primary/10 hover:to-secondary/10 rounded-xl transition-all text-muted hover:text-primary"
        >
          <Menu className="w-5 h-5" />
        </motion.button>
      </div>

      {/* ★★★ NEW CHAT BUTTON - Big, Colorful, Obvious ★★★ */}
      <div className={clsx("px-3 mb-6", sidebarOpen ? "" : "flex justify-center")}>
        <motion.button
          whileHover={{ scale: 1.05, boxShadow: "0 0 30px rgba(99, 102, 241, 0.5)" }}
          whileTap={{ scale: 0.95 }}
          onClick={handleCreateConversation}
          className={clsx(
            "flex items-center justify-center gap-3 cursor-pointer transition-all duration-300",
            "bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500",
            "hover:from-indigo-600 hover:via-purple-600 hover:to-pink-600",
            "rounded-2xl text-white font-semibold shadow-lg",
            "border-2 border-white/20",
            sidebarOpen ? "w-full px-5 py-4" : "w-14 h-14 p-0"
          )}
          title="New Chat"
        >
          <Plus className={clsx("transition-transform", sidebarOpen ? "w-5 h-5" : "w-6 h-6")} />
          {sidebarOpen && <span className="text-base tracking-wide">New Chat</span>}
        </motion.button>
      </div>

      {/* Recent Section Label */}
      {sidebarOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="px-6 mb-2 text-xs font-semibold text-transparent bg-gradient-to-r from-primary to-secondary bg-clip-text uppercase tracking-wider"
        >
          Recent Chats
        </motion.div>
      )}

      {/* Conversation List */}
      <div
        ref={scrollContainerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto px-2 space-y-1 scrollbar-none"
      >
        {conversations.map((conv, index) => (
          <motion.div
            key={conv.id}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.03 }}
          >
            {editingId === conv.id ? (
              /* 编辑模式 - 显示输入框 */
              <div className="flex items-center gap-2 px-3 py-2 rounded-2xl bg-surface-container-high">
                <MessageSquare className="w-5 h-5 text-indigo-400 flex-shrink-0" />
                <input
                  ref={inputRef}
                  type="text"
                  value={editingTitle}
                  onChange={(e) => setEditingTitle(e.target.value)}
                  onBlur={handleSaveTitle}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSaveTitle();
                    if (e.key === 'Escape') handleCancelEdit();
                  }}
                  className="flex-1 bg-transparent text-sm font-medium outline-none border-b-2 border-primary/50 focus:border-primary py-0.5"
                  placeholder="输入会话标题..."
                />
              </div>
            ) : (
              /* 正常模式 - 显示标题 */
              <SidebarItem
                icon={MessageSquare}
                label={conv.title || 'Chat'}
                isActive={conv.id === currentConversationId}
                onClick={() => setCurrentConversationId(conv.id)}
                onEdit={() => handleEditConversation(conv.id, conv.title || '')}
                onDelete={() => handleDeleteConversation(conv.id)}
                isCollapsed={!sidebarOpen}
                colorClass="text-indigo-400"
              />
            )}
          </motion.div>
        ))}
        {/* 加载更多提示 */}
        {isLoadingMore && (
          <div className="py-2 text-center text-xs text-muted">加载中...</div>
        )}
      </div>

      {/* Bottom Section - Settings Only */}
      <div className="mt-auto px-2 space-y-1 pt-4 border-t border-gradient-to-r from-primary/20 to-secondary/20">
        {/* Settings按钮包装器，用于获取位置 */}
        <div ref={settingsButtonRef}>
          <SidebarItem
            icon={Settings}
            label="Settings"
            onClick={handleOpenThemePanel}
            isCollapsed={!sidebarOpen}
            colorClass="text-rose-400"
          />
        </div>

        {/* User Profile - Colorful Avatar */}
        {sidebarOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            onClick={() => setIsProfileModalOpen(true)}
            className="mt-3 mx-2 pt-3 border-t border-border/30 flex items-center gap-3 px-2 py-2 cursor-pointer hover:bg-surface-container-high/50 rounded-xl transition-colors"
          >
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-full blur-sm opacity-60"></div>
              <div className="relative w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center text-white text-sm font-bold shadow-lg overflow-hidden">
                {user?.avatar ? (
                  <img src={user.avatar} alt="avatar" className="w-full h-full object-cover" />
                ) : (
                  user?.userName?.charAt(0).toUpperCase()
                )}
              </div>
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold text-foreground truncate">{user?.userName}</div>
              <div className="text-xs bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent font-medium">Premium User</div>
            </div>
          </motion.div>
        )}
      </div>

      <ThemePanel
        isOpen={isThemePanelOpen}
        onClose={() => setIsThemePanelOpen(false)}
        anchorRect={settingsAnchorRect}
      />

      <UserProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
      />
    </motion.div>
  );
}
