/**
 * 消息分支 API 客户端
 * 
 * 提供基于消息 ID 的分支查询功能
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
        `/checkpoint/${conversationId}/message-siblings`,
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
        `/checkpoint/${conversationId}/message/${messageId}`
    );
    return response.data.data || null;
}

// 保留旧函数名作为别名，方便迁移
export const getSiblingCheckpoints = getSiblingMessages;
export const getCheckpointMessages = async (
    _conversationId: string,
    _checkpointId: string
): Promise<Message[]> => {
    // 已废弃：这个函数不再使用 checkpoint，直接返回空数组
    console.warn('getCheckpointMessages is deprecated, use getMessageById instead');
    return [];
};
