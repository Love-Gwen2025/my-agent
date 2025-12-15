/**
 * 应用根组件
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAppStore } from './store';
import { MainLayout, LoginPage } from './components/layout';

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
  const { token } = useAppStore();

  // 未登录显示登录页
  if (!token) {
    return <LoginPage />;
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
