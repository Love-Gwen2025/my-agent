/**
 * 主布局组件
 */
import { Sidebar } from '../sidebar';
import { ChatPanel } from '../chat';

/**
 * 主布局组件
 *
 * Gemini Layout: 
 * - Sidebar: Collapsible, #f0f4f9 (light) / #1e1f20 (dark)
 * - Main: White (light) / #131314 (dark), rounded corners
 */
export function MainLayout() {
  return (
    <div className="h-screen flex bg-surface text-foreground overflow-hidden selection:bg-primary/20">
      <Sidebar />
      {/* 
        Gemini style: Main content area usually has a slightly different background 
        or rounded corners in some views, but broadly it's clean.
        We'll use 'bg-background' for the chat area to contrast with 'bg-surface' of sidebar if needed.
      */}
      <main className="flex-1 flex flex-col relative z-0 overflow-hidden bg-background">
        <ChatPanel />
      </main>
    </div>
  );
}
