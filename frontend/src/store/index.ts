/**
 * 全局状态管理
 *
 * 使用 Zustand 管理应用状态
 */
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User, Conversation, Message, AiModel } from '../types';
import type { ThemeMode, AccentColor } from '../config/themes';

/** 应用状态接口 */
interface AppState {
  /** 当前用户 */
  user: User | null;
  /** 认证 Token */
  token: string | null;
  /** 会话列表 */
  conversations: Conversation[];
  /** 当前选中的会话ID */
  currentConversationId: string | null;
  /** 当前会话的消息列表 */
  messages: Message[];
  /** 可用的 AI 模型列表 */
  models: AiModel[];
  /** 当前选中的模型编码 */
  currentModelCode: string | null;
  /** 侧边栏是否展开 */
  sidebarOpen: boolean;
  /** 是否正在加载 */
  isLoading: boolean;
  /** 流式响应的临时内容 */
  streamingContent: string;
  /** 主题模式 (亮色/暗色/跟随系统) */
  themeMode: ThemeMode;
  /** 强调色主题 */
  accentColor: AccentColor;
}

/** 应用操作接口 */
interface AppActions {
  /** 设置用户信息 */
  setUser: (user: User | null) => void;
  /** 设置 Token */
  setToken: (token: string | null) => void;
  /** 登出 */
  logout: () => void;
  /** 设置会话列表 */
  setConversations: (conversations: Conversation[]) => void;
  /** 添加新会话 */
  addConversation: (conversation: Conversation) => void;
  /** 删除会话 */
  removeConversation: (id: string) => void;
  /** 设置当前会话 */
  setCurrentConversationId: (id: string | null) => void;
  /** 设置消息列表 */
  setMessages: (messages: Message[]) => void;
  /** 添加消息 */
  addMessage: (message: Message) => void;
  /** 设置模型列表 */
  setModels: (models: AiModel[]) => void;
  /** 设置当前模型 */
  setCurrentModelCode: (code: string | null) => void;
  /** 切换侧边栏 */
  toggleSidebar: () => void;
  /** 设置加载状态 */
  setIsLoading: (loading: boolean) => void;
  /** 设置流式内容 */
  setStreamingContent: (content: string) => void;
  /** 追加流式内容 */
  appendStreamingContent: (chunk: string) => void;
  /** 清空流式内容 */
  clearStreamingContent: () => void;
  /** 设置主题模式 */
  setThemeMode: (mode: ThemeMode) => void;
  /** 设置强调色 */
  setAccentColor: (color: AccentColor) => void;
}

/** 应用 Store */
export const useAppStore = create<AppState & AppActions>()(
  persist(
    (set) => ({
      // 初始状态
      user: null,
      token: null,
      conversations: [],
      currentConversationId: null,
      messages: [],
      models: [],
      currentModelCode: null,
      sidebarOpen: true,
      isLoading: false,
      streamingContent: '',
      themeMode: 'system',
      accentColor: 'blue',

      // 用户相关操作
      setUser: (user) => set({ user }),
      setToken: (token) => {
        if (token) {
          localStorage.setItem('token', token);
        } else {
          localStorage.removeItem('token');
        }
        set({ token });
      },
      logout: () => {
        localStorage.removeItem('token');
        set({
          user: null,
          token: null,
          conversations: [],
          currentConversationId: null,
          messages: [],
        });
      },

      // 会话相关操作
      setConversations: (conversations) => set({ conversations }),
      addConversation: (conversation) =>
        set((state) => ({
          conversations: [conversation, ...state.conversations],
        })),
      removeConversation: (id) =>
        set((state) => ({
          conversations: state.conversations.filter((c) => c.id !== id),
          currentConversationId:
            state.currentConversationId === id
              ? null
              : state.currentConversationId,
        })),
      setCurrentConversationId: (id) =>
        set({ currentConversationId: id, messages: [], streamingContent: '' }),

      // 消息相关操作
      setMessages: (messages) => set({ messages }),
      addMessage: (message) =>
        set((state) => ({
          messages: [...state.messages, message],
        })),

      // 模型相关操作
      setModels: (models) => set({ models }),
      setCurrentModelCode: (code) => set({ currentModelCode: code }),

      // UI 相关操作
      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      setIsLoading: (loading) => set({ isLoading: loading }),

      // 流式内容操作
      setStreamingContent: (content) => set({ streamingContent: content }),
      appendStreamingContent: (chunk) =>
        set((state) => ({
          streamingContent: state.streamingContent + chunk,
        })),
      clearStreamingContent: () => set({ streamingContent: '' }),

      // 主题相关操作
      setThemeMode: (mode) => set({ themeMode: mode }),
      setAccentColor: (color) => set({ accentColor: color }),
    }),
    {
      name: 'app-storage',
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        currentModelCode: state.currentModelCode,
        themeMode: state.themeMode,
        accentColor: state.accentColor,
      }),
    }
  )
);
