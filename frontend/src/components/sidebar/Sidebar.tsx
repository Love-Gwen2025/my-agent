/**
 * Gemini Sidebar - Premium Colorful Edition
 */
import { useEffect, useState } from 'react';
import { Plus, MessageSquare, Trash2, Menu, Settings, HelpCircle, Activity } from 'lucide-react';
import { motion } from 'framer-motion';
import { useAppStore } from '../../store';
import { getConversations, createConversation, deleteConversation } from '../../api';
import clsx from 'clsx';
import { ThemePanel } from '../settings';

function SidebarItem({
  icon: Icon,
  label,
  isActive,
  onClick,
  onDelete,
  isCollapsed,
  colorClass = "text-muted"
}: {
  icon: any;
  label: string;
  isActive?: boolean;
  onClick: () => void;
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
          isActive ? "font-semibold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent" : "font-medium"
        )}>
          {label}
        </span>
      )}

      {!isCollapsed && onDelete && (
        <div className="opacity-0 group-hover:opacity-100 absolute right-2 flex items-center transition-opacity duration-200">
          <button
            onClick={(e) => { e.stopPropagation(); onDelete(); }}
            className="p-1.5 hover:bg-red-500/20 hover:text-red-500 rounded-full transition-all duration-200"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      )}
    </motion.div>
  );
}

export function Sidebar() {
  const [isThemePanelOpen, setIsThemePanelOpen] = useState(false);

  const {
    conversations,
    currentConversationId,
    sidebarOpen,
    toggleSidebar,
    setConversations,
    addConversation,
    removeConversation,
    setCurrentConversationId,
    token,
    user,
  } = useAppStore();

  useEffect(() => {
    if (token) loadConversations();
  }, [token]);

  async function loadConversations() {
    try {
      const data = await getConversations();
      setConversations(data);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    }
  }

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
      <div className="flex-1 overflow-y-auto px-2 space-y-1 scrollbar-none">
        {conversations.map((conv, index) => (
          <motion.div
            key={conv.id}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.03 }}
          >
            <SidebarItem
              icon={MessageSquare}
              label={conv.title || 'Chat'}
              isActive={conv.id === currentConversationId}
              onClick={() => setCurrentConversationId(conv.id)}
              onDelete={() => handleDeleteConversation(conv.id)}
              isCollapsed={!sidebarOpen}
              colorClass="text-indigo-400"
            />
          </motion.div>
        ))}
      </div>

      {/* Bottom Section - Colorful Icons */}
      <div className="mt-auto px-2 space-y-1 pt-4 border-t border-gradient-to-r from-primary/20 to-secondary/20">
        <SidebarItem
          icon={HelpCircle}
          label="Help"
          onClick={() => { }}
          isCollapsed={!sidebarOpen}
          colorClass="text-teal-400"
        />
        <SidebarItem
          icon={Activity}
          label="Activity"
          onClick={() => { }}
          isCollapsed={!sidebarOpen}
          colorClass="text-amber-400"
        />
        <SidebarItem
          icon={Settings}
          label="Settings"
          onClick={() => setIsThemePanelOpen(true)}
          isCollapsed={!sidebarOpen}
          colorClass="text-rose-400"
        />

        {/* User Profile - Colorful Avatar */}
        {sidebarOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="mt-3 mx-2 pt-3 border-t border-border/30 flex items-center gap-3 px-2 py-2"
          >
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-full blur-sm opacity-60"></div>
              <div className="relative w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center text-white text-sm font-bold shadow-lg">
                {user?.userName?.charAt(0).toUpperCase()}
              </div>
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold text-foreground truncate">{user?.userName}</div>
              <div className="text-xs bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent font-medium">Premium User</div>
            </div>
          </motion.div>
        )}
      </div>

      <ThemePanel isOpen={isThemePanelOpen} onClose={() => setIsThemePanelOpen(false)} />
    </motion.div>
  );
}
