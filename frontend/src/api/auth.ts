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
  const response = await apiClient.get<ApiResponse<User>>('/user/me');
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

/**
 * 上传用户头像
 *
 * @param file 图片文件
 * @returns 头像 URL
 */
export async function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiClient.post<ApiResponse<string>>(
    '/user/upload/avatar',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data.data;
}

/**
 * 更新用户资料
 *
 * @param data 用户资料（不含密码）
 * @returns 更新后的用户信息
 */
export async function updateProfile(data: {
  userName?: string;
  userPhone?: string;
  address?: string;
  userSex?: number;
}): Promise<User> {
  const response = await apiClient.post<ApiResponse<User>>(
    '/user/update-profile',
    data
  );
  return response.data.data;
}

/**
 * 修改密码
 *
 * @param oldPassword 旧密码
 * @param newPassword 新密码
 */
export async function changePassword(
  oldPassword: string,
  newPassword: string
): Promise<void> {
  await apiClient.post('/user/change-password', {
    oldPassword,
    newPassword,
  });
}

