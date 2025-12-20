/**
 * Checkpoint API 客户端
 * 
 * 提供分支查询和切换功能
 */
import apiClient from './client';
import type { ApiResponse, SiblingInfo } from '../types';

/** Checkpoint 历史项 */
export interface CheckpointItem {
    checkpointId: string;
    parentId: string | null;
    messageCount: number;
}

/** Checkpoint 消息 */
export interface CheckpointMessage {
    role: string;
    content: string;
}

/**
 * 获取会话的 checkpoint 历史
 */
export async function getCheckpointHistory(
    conversationId: string
): Promise<CheckpointItem[]> {
    const response = await apiClient.get<ApiResponse<CheckpointItem[]>>(
        `/checkpoint/${conversationId}/history`
    );
    return response.data.data || [];
}

/**
 * 获取指定 checkpoint 的兄弟分支
 * 用于显示 "1/2" 导航
 */
export async function getSiblingCheckpoints(
    conversationId: string,
    checkpointId: string
): Promise<SiblingInfo> {
    const response = await apiClient.get<ApiResponse<SiblingInfo>>(
        `/checkpoint/${conversationId}/siblings`,
        { params: { checkpoint_id: checkpointId } }
    );
    return response.data.data || { current: 0, total: 1, siblings: [] };
}

/**
 * 获取指定 checkpoint 的消息列表
 */
export async function getCheckpointMessages(
    conversationId: string,
    checkpointId: string
): Promise<CheckpointMessage[]> {
    const response = await apiClient.get<ApiResponse<CheckpointMessage[]>>(
        `/checkpoint/${conversationId}/messages`,
        { params: { checkpoint_id: checkpointId } }
    );
    return response.data.data || [];
}
