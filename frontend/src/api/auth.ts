/**
 * 认证相关 API
 */
import apiClient from './client';
import type { ApiResponse, LoginParams, LoginResponse, User } from '../types';

/**
 * 用户登录
 *
 * @param params 登录参数
 * @returns 登录响应（包含 token 和用户信息）
 */
export async function login(params: LoginParams): Promise<LoginResponse> {
  const response = await apiClient.post<ApiResponse<LoginResponse>>(
    '/user/login',
    params
  );
  return response.data.data;
}

/**
 * 用户登出
 */
export async function logout(): Promise<void> {
  await apiClient.post('/user/logout');
}

/**
 * 获取当前用户信息
 *
 * @returns 用户信息
 */
export async function getCurrentUser(): Promise<User> {
  const response = await apiClient.get<ApiResponse<User>>('/user/detail/0');
  return response.data.data;
}

/**
 * 用户注册
 *
 * @param params 注册参数
 * @returns 用户信息
 */
export async function register(params: LoginParams): Promise<User> {
  const response = await apiClient.post<ApiResponse<User>>(
    '/user/register',
    params
  );
  return response.data.data;
}
