/**
 * 消息分支 API 客户端
 * 
 * 提供基于消息 ID 的分支查询和导航功能
 */
import apiClient from './client';
import type { ApiResponse, SiblingInfo, Message } from '../types';

/**
 * 获取消息的兄弟分支
 * 用于显示 "1/2" 导航
 * 
 * @param conversationId 会话 ID
 * @param messageId 当前消息 ID
 */
export async function getSiblingMessages(
    conversationId: string,
    messageId: string
): Promise<SiblingInfo> {
    const response = await apiClient.get<ApiResponse<SiblingInfo>>(
        `/branch/${conversationId}/siblings`,
        { params: { message_id: messageId } }
    );
    return response.data.data || { current: 0, total: 1, siblings: [] };
}

/**
 * 根据 ID 获取消息详情
 * 
 * @param conversationId 会话 ID
 * @param messageId 消息 ID
 */
export async function getMessageById(
    conversationId: string,
    messageId: string
): Promise<Message | null> {
    const response = await apiClient.get<ApiResponse<Message>>(
        `/branch/${conversationId}/message/${messageId}`
    );
    return response.data.data || null;
}

/**
 * 保存用户选择的当前分支消息 ID
 * 
 * 当用户通过分支导航器切换分支时调用，
 * 刷新页面后会恢复到用户选择的分支。
 * 
 * @param conversationId 会话 ID
 * @param messageId 当前选中的消息 ID
 */
export async function setCurrentMessage(
    conversationId: string,
    messageId: string
): Promise<void> {
    await apiClient.put<ApiResponse<string>>(
        `/branch/${conversationId}/current`,
        { messageId }
    );
}
