/**
 * 消息气泡组件
 *
 * 显示单条聊天消息，支持 Markdown 渲染
 */
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { User, Bot, Copy, Check } from 'lucide-react';
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
    <div className="relative group">
      <button
        className="absolute right-2 top-2 p-1.5 rounded bg-[var(--bg-secondary)] opacity-0 group-hover:opacity-100 transition-opacity"
        onClick={handleCopy}
        title="复制代码"
      >
        {copied ? (
          <Check className="w-4 h-4 text-green-400" />
        ) : (
          <Copy className="w-4 h-4" />
        )}
      </button>
      <SyntaxHighlighter
        style={vscDarkPlus}
        language={language}
        PreTag="div"
        customStyle={{
          margin: 0,
          borderRadius: '0.5rem',
          fontSize: '0.875rem',
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
  // AI 模型的闪光图标
  const SparkleIcon = () => (
     <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-500 to-purple-500 flex items-center justify-center flex-shrink-0 mt-1">
         <Bot className="w-5 h-5 text-white" />
     </div>
  );

  return (
    <div
      className={clsx(
        'flex gap-4 p-4 w-full mx-auto max-w-3xl', // Limit width for readability
        isUser ? 'flex-row-reverse' : 'flex-row'
      )}
    >
      {/* AI 头像 (User 不显示头像，更像短信/IM) */}
      {!isUser && <SparkleIcon />}

      {/* 消息内容 */}
      <div
        className={clsx(
          'relative px-5 py-3.5 text-[0.95rem] leading-7', // Better typography
          isUser
            ? 'bg-[#f4f4f5] text-[#18181b] rounded-[2rem] rounded-tr-sm max-w-[85%]' // User: Light gray bubble
            : 'bg-transparent text-[#18181b] w-full' // AI: Transparent, full width
        )}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
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
              <span className="inline-block w-2.5 h-5 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full animate-pulse ml-1 align-middle" />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
