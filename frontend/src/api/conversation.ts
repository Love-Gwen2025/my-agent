/**
 * 会话相关 API
 */
import apiClient from './client';
import type {
  ApiResponse,
  Conversation,
  CreateConversationParams,
  Message,
} from '../types';

/**
 * 获取会话列表
 *
 * @returns 会话列表
 */
export async function getConversations(): Promise<Conversation[]> {
  const response = await apiClient.get<ApiResponse<Conversation[]>>(
    '/conversation/list'
  );
  return response.data.data || [];
}

/**
 * 创建新会话
 *
 * @param params 创建参数
 * @returns 新创建的会话
 */
export async function createConversation(
  params: CreateConversationParams
): Promise<Conversation> {
  // 后端 AI 会话创建接口
  const response = await apiClient.post<ApiResponse<Conversation>>(
    '/conversation/create/assistant',
    params
  );
  return response.data.data;
}

/**
 * 删除会话
 *
 * @param id 会话ID
 */
export async function deleteConversation(id: string): Promise<void> {
  await apiClient.delete(`/conversation/${id}`);
}

/**
 * 获取会话历史消息（返回完整消息树）
 *
 * @param conversationId 会话ID
 * @returns 包含所有消息和当前选中消息ID的响应
 */
export async function getConversationHistory(
  conversationId: string
): Promise<{ messages: Message[]; currentMessageId: string | null }> {
  const response = await apiClient.get<ApiResponse<{ messages: Message[]; currentMessageId: string | null }>>(
    `/conversation/history`,
    { params: { conversationId } }
  );
  return response.data.data || { messages: [], currentMessageId: null };
}

/**
 * 更新会话标题
 *
 * @param id 会话ID
 * @param title 新标题
 */
export async function updateConversationTitle(
  id: string,
  title: string
): Promise<void> {
  await apiClient.patch(`/conversation/modify`, { id, title });
}
