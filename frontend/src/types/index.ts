/**
 * 类型定义文件
 *
 * 定义前端使用的所有 TypeScript 类型
 */

/** 用户信息（后端字段命名） */
export interface User {
  id: number;
  userCode: string;
  userName?: string;
  avatar?: string;
}

/** 登录参数（后端要求 userCode/userPassword） */
export interface LoginParams {
  userCode: string;
  userPassword: string;
}

/** 登录响应：后端仅返回 token 字符串 */
export type LoginResponse = string;

/** 会话信息 */
export interface Conversation {
  id: number;
  title: string;
  userId?: number;
  modelCode?: string;
  lastMessageId?: number;
  lastMessageAt?: string;
  avatar?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 消息信息 */
export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  role: 'user' | 'assistant' | 'system';
  content: string;
  contentType: string;
  modelCode?: string;
  tokenCount?: number;
  createTime: string;
}

/** 会话历史消息视图（后端 history 接口返回） */
export interface HistoryMessage {
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

/** AI 模型信息 */
export interface AiModel {
  id: number;
  modelCode: string;
  modelName: string;
  provider: string;
  isDefault: boolean;
  status: number;
}

/** 创建会话参数 */
export interface CreateConversationParams {
  title?: string;
  modelCode?: string;
}

/** 流式聊天请求参数 */
export interface StreamChatRequest {
  conversationId: number;
  content: string;
  modelCode?: string;
  systemPrompt?: string;
}

/** 流式聊天事件 */
export interface StreamChatEvent {
  type: 'chunk' | 'done' | 'error';
  content?: string;
  messageId?: number;
  conversationId?: number;
  tokenCount?: number;
  error?: string;
}

/** API 响应包装 */
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

/** 分页参数 */
export interface PageParams {
  page?: number;
  size?: number;
}

/** 分页响应 */
export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
