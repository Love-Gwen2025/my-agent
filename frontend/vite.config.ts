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
    proxy: {
      // 将所有后端接口代理到包含 context-path 的目标，避免浏览器跨域
      '/couple-agent': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 保持路径前缀，直接转发 /couple-agent/xxx
      },
    },
  },
});
