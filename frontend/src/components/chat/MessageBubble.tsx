/**
 * 消息气泡组件
 *
 * Modern design with gradient user messages and glass AI messages
 */
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { User, Bot, Copy, Check, Sparkles } from 'lucide-react';
import { useState } from 'react';
import clsx from 'clsx';
import type { Message } from '../../types';

interface MessageBubbleProps {
  message: Message;
  isStreaming?: boolean;
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
export function MessageBubble({ message, isStreaming }: MessageBubbleProps) {
  const isUser = message.role === 'user';

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
      <div
        className={clsx(
          'max-w-[80%] rounded-2xl px-5 py-3 shadow-md',
          isUser
            ? 'text-white'
            : 'glass-effect text-[var(--text-primary)]'
        )}
        style={isUser ? { background: 'var(--user-bubble-bg)' } : undefined}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
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
    </div>
  );
}
