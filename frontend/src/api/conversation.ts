/**
 * 会话相关 API
 */
import apiClient from './client';
import type {
  ApiResponse,
  Conversation,
  CreateConversationParams,
  Message,
  PageResponse,
} from '../types';

/**
 * 获取会话列表
 *
 * @returns 会话列表
 */
export async function getConversations(): Promise<Conversation[]> {
  const response = await apiClient.get<ApiResponse<PageResponse<Conversation>>>(
    '/api/conversation',
    { params: { page: 1, size: 100 } }
  );
  return response.data.data.records || [];
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
  const response = await apiClient.post<ApiResponse<Conversation>>(
    '/api/conversation',
    params
  );
  return response.data.data;
}

/**
 * 删除会话
 *
 * @param id 会话ID
 */
export async function deleteConversation(id: number): Promise<void> {
  await apiClient.delete(`/api/conversation/${id}`);
}

/**
 * 获取会话历史消息
 *
 * @param conversationId 会话ID
 * @returns 消息列表
 */
export async function getConversationHistory(
  conversationId: number
): Promise<Message[]> {
  const response = await apiClient.get<ApiResponse<PageResponse<Message>>>(
    `/api/conversation/${conversationId}/history`,
    { params: { page: 1, size: 100 } }
  );
  return response.data.data.records || [];
}

/**
 * 更新会话标题
 *
 * @param id 会话ID
 * @param title 新标题
 */
export async function updateConversationTitle(
  id: number,
  title: string
): Promise<void> {
  await apiClient.put(`/api/conversation/${id}`, { title });
}
