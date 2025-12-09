/**
 * AI 模型相关 API
 */
import apiClient from './client';
import type { AiModel, ApiResponse } from '../types';

/**
 * 获取可用模型列表
 *
 * @returns 模型列表
 */
export async function getModels(): Promise<AiModel[]> {
  const response = await apiClient.get<ApiResponse<AiModel[]>>('/api/model');
  return response.data.data || [];
}

/**
 * 获取默认模型
 *
 * @returns 默认模型
 */
export async function getDefaultModel(): Promise<AiModel | null> {
  const models = await getModels();
  return models.find((m) => m.isDefault) || models[0] || null;
}
