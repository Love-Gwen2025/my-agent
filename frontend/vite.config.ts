import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    // 后端直接运行在 localhost:8080，前端通过 VITE_API_BASE_URL 环境变量配置
    // 开发时前端默认连接 http://localhost:8080
  },
});
