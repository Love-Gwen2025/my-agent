/**
 * 类型定义文件
 *
 * 定义前端使用的所有 TypeScript 类型
 */

/** 用户信息 */
export interface User {
  id: number;
  username: string;
  email?: string;
  avatar?: string;
}

/** 登录参数 */
export interface LoginParams {
  username: string;
  password: string;
}

/** 登录响应 */
export interface LoginResponse {
  token: string;
  user: User;
}

/** 会话信息 */
export interface Conversation {
  id: number;
  title: string;
  userId: number;
  modelCode: string;
  lastMessageId?: number;
  lastMessageTime?: string;
  createTime: string;
  updateTime: string;
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
  sendTime: string;
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
  code: number;
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
