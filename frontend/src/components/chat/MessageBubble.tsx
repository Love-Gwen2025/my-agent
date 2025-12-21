/**
 * 消息气泡组件
 *
 * Modern design with gradient user messages and glass AI messages
 */
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { User, Copy, Check, Sparkles, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import clsx from 'clsx';
import type { Message, SiblingInfo } from '../../types';
import { BranchNavigator } from './BranchNavigator';

interface MessageBubbleProps {
  message: Message;
  isStreaming?: boolean;
  /** 分支信息（AI 消息需要） */
  siblingInfo?: SiblingInfo;
  /** 切换分支回调 */
  onNavigateBranch?: (direction: 'prev' | 'next') => void;
  /** 重新生成回调 */
  onRegenerate?: () => void;
  /** 是否正在重新生成 */
  isRegenerating?: boolean;
  /** 开启编辑回调（仅用户消息） */
  onEdit?: () => void;
  /** 是否处于编辑状态 */
  isEditing?: boolean;
  /** 编辑中的内容 */
  editingContent?: string;
  /** 编辑内容变更回调 */
  onEditChange?: (value: string) => void;
  /** 提交编辑回调 */
  onEditSubmit?: () => void;
  /** 取消编辑回调 */
  onEditCancel?: () => void;
}

/**
 * 代码块组件
 */
function CodeBlock({
  language,
  children,
}: {
  language: string;
  children: string;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(children);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="relative group rounded-xl overflow-hidden my-3">
      {/* Language label */}
      <div className="absolute top-0 left-0 right-0 flex items-center justify-between px-4 py-2 bg-[var(--bg-tertiary)] border-b border-[var(--border-color)]">
        <span className="text-xs font-medium text-[var(--text-secondary)]">{language}</span>
        <button
          className="p-1.5 rounded-lg hover:bg-[var(--bg-secondary)] transition-colors"
          onClick={handleCopy}
          title="复制代码"
        >
          {copied ? (
            <Check className="w-4 h-4 text-green-400" />
          ) : (
            <Copy className="w-4 h-4 text-[var(--text-secondary)]" />
          )}
        </button>
      </div>
      <SyntaxHighlighter
        style={oneDark}
        language={language}
        PreTag="div"
        customStyle={{
          margin: 0,
          paddingTop: '3rem',
          borderRadius: '0.75rem',
          fontSize: '0.875rem',
          background: 'var(--bg-tertiary)',
        }}
      >
        {children}
      </SyntaxHighlighter>
    </div>
  );
}

/**
 * 消息气泡组件
 */
export function MessageBubble({
  message,
  isStreaming,
  siblingInfo,
  onNavigateBranch,
  onRegenerate,
  isRegenerating = false,
  onEdit,
  isEditing = false,
  editingContent = '',
  onEditChange,
  onEditSubmit,
  onEditCancel,
}: MessageBubbleProps) {
  const isUser = message.role === 'user';
  const isAssistant = message.role === 'assistant';
  // 分支导航器只在 AI 消息上显示，用户消息分支通过编辑功能操作
  const showBranchNav = isAssistant && siblingInfo && siblingInfo.total > 1;
  const showRegenerate = isAssistant && onRegenerate && !isStreaming;

  return (
    <div
      className={clsx(
        'flex gap-4 p-4 animate-fade-in-up',
        isUser ? 'flex-row-reverse' : 'flex-row'
      )}
    >
      {/* Avatar */}
      <div
        className={clsx(
          'w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg',
          isUser
            ? 'bg-gradient-to-br from-blue-500 to-purple-600'
            : 'bg-gradient-to-br from-cyan-400 to-purple-600'
        )}
      >
        {isUser ? (
          <User className="w-5 h-5 text-white" />
        ) : (
          <Sparkles className="w-5 h-5 text-white" />
        )}
      </div>

      {/* Message content */}
      <div className="flex flex-col max-w-[80%]">
        <div
          className={clsx(
            'rounded-2xl px-5 py-3 shadow-md',
            isUser
              ? 'text-white'
              : 'glass-effect text-[var(--text-primary)]'
          )}
          style={isUser ? { background: 'var(--user-bubble-bg)' } : undefined}
        >
          {isUser ? (
            isEditing ? (
              <div className="flex flex-col gap-2">
                <textarea
                  className="w-full min-h-[80px] rounded-lg bg-[var(--bg-secondary)] text-[var(--text-primary)] p-3 outline-none focus:ring-2 focus:ring-[var(--accent-primary)]"
                  value={editingContent}
                  onChange={(e) => onEditChange?.(e.target.value)}
                  placeholder="编辑消息内容"
                />
                <div className="flex gap-2">
                  <button
                    className="px-3 py-1 rounded bg-[var(--accent-primary)] text-white text-xs disabled:opacity-60"
                    onClick={onEditSubmit}
                    disabled={isRegenerating}
                  >
                    保存并生成
                  </button>
                  <button
                    className="px-3 py-1 rounded border border-[var(--border-color)] text-xs text-[var(--text-secondary)]"
                    onClick={onEditCancel}
                  >
                    取消
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex items-start justify-between gap-3">
                <p className="whitespace-pre-wrap leading-relaxed flex-1">{message.content}</p>
                {onEdit && (
                  <button
                    className="text-xs text-[var(--text-secondary)] hover:text-[var(--text-primary)] px-2 py-1 rounded transition-colors"
                    onClick={onEdit}
                    title="编辑并分支"
                  >
                    编辑
                  </button>
                )}
              </div>
            )
          ) : (
            <div className="markdown-body">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                  code({ className, children, ...props }) {
                    const match = /language-(\w+)/.exec(className || '');
                    const isInline = !match;
                    return isInline ? (
                      <code className={className} {...props}>
                        {children}
                      </code>
                    ) : (
                      <CodeBlock language={match[1]}>
                        {String(children).replace(/\n$/, '')}
                      </CodeBlock>
                    );
                  },
                }}
              >
                {message.content}
              </ReactMarkdown>
              {isStreaming && (
                <span className="inline-flex items-center gap-1 ml-1">
                  <span className="w-2 h-2 rounded-full bg-[var(--accent-primary)] animate-pulse" />
                  <span className="w-2 h-2 rounded-full bg-[var(--accent-primary)] animate-pulse" style={{ animationDelay: '0.2s' }} />
                  <span className="w-2 h-2 rounded-full bg-[var(--accent-primary)] animate-pulse" style={{ animationDelay: '0.4s' }} />
                </span>
              )}
            </div>
          )}
        </div>


        {/* AI 消息底部工具栏 */}
        {isAssistant && !isStreaming && (
          <div className="flex items-center gap-3 mt-2 ml-2">
            {/* 分支导航器 */}
            {showBranchNav && onNavigateBranch && (
              <BranchNavigator
                currentIndex={siblingInfo.current}
                totalCount={siblingInfo.total}
                onNavigate={onNavigateBranch}
                isLoading={isRegenerating}
              />
            )}

            {/* 重新生成按钮 */}
            {showRegenerate && (
              <button
                onClick={onRegenerate}
                disabled={isRegenerating}
                className="flex items-center gap-1 px-2 py-1 text-xs text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-tertiary)] rounded transition-colors disabled:opacity-50"
                title="重新生成"
              >
                <RefreshCw className={clsx('w-3.5 h-3.5', isRegenerating && 'animate-spin')} />
                <span>重新生成</span>
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

