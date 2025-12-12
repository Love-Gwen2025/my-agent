/**
 * 主布局组件
 */
import { Sidebar } from '../sidebar';
import { ChatPanel } from '../chat';

/**
 * 主布局组件
 *
 * 包含侧边栏和聊天面板
 */
export function MainLayout() {
  return (
    <div className="h-screen flex bg-[var(--bg-primary)] overflow-hidden">
      <Sidebar />
      <main className="flex-1 flex flex-col relative h-full">
        <ChatPanel />
      </main>
    </div>
  );
}
