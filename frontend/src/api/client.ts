/**
 * API 客户端配置
 *
 * 使用 axios 封装 HTTP 请求，支持拦截器和统一错误处理
 */
import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types';

/** API 基础地址 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/** 创建 axios 实例 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 请求拦截器
 *
 * 自动添加 Authorization 头
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

/**
 * 响应拦截器
 *
 * 统一处理响应和错误
 */
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    // 处理 401 未授权
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
export { API_BASE_URL };
