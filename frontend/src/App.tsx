/**
 * 应用根组件
 */
import { useEffect, useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAppStore } from './store';
import { MainLayout, LoginPage, WelcomePage } from './components/layout';

/** React Query 客户端 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

/**
 * 应用入口组件
 */
function AppContent() {
  const { token, themeMode, accentColor } = useAppStore();
  const [view, setView] = useState<'welcome' | 'login'>('welcome');

  /**
   * 主题初始化效果
   * 根据 store 中的主题设置，更新 document 上的 data 属性
   */
  useEffect(() => {
    const root = document.documentElement;

    // 设置强调色
    root.setAttribute('data-accent', accentColor);

    // 设置明暗模式
    if (themeMode === 'system') {
      // 跟随系统偏好
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const updateSystemTheme = () => {
        root.setAttribute('data-theme', mediaQuery.matches ? 'dark' : 'light');
      };

      // 初始设置
      updateSystemTheme();

      // 监听系统偏好变化
      mediaQuery.addEventListener('change', updateSystemTheme);
      return () => mediaQuery.removeEventListener('change', updateSystemTheme);
    } else {
      // 手动设置
      root.setAttribute('data-theme', themeMode);
    }
  }, [themeMode, accentColor]);

  // 未登录显示欢迎页或登录页
  if (!token) {
    if (view === 'welcome') {
      return <WelcomePage onSignIn={() => setView('login')} onSignUp={() => setView('login')} />;
    }
    return <LoginPage onBack={() => setView('welcome')} />;
  }

  // 已登录显示主界面
  return <MainLayout />;
}

/**
 * 应用根组件
 */
function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
    </QueryClientProvider>
  );
}

export default App;

