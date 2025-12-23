/**
 * 消息组件 - Premium Edition
 */
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Copy, Check, Sparkles, RefreshCw, Pencil, ThumbsUp, ThumbsDown, Share2, MoreVertical } from 'lucide-react';
import { useState } from 'react';
import { motion } from 'framer-motion';
import clsx from 'clsx';
import type { Message, SiblingInfo } from '../../types';
import { BranchNavigator } from './BranchNavigator';

interface MessageBubbleProps {
  message: Message;
  isStreaming?: boolean;
  siblingInfo?: SiblingInfo;
  onNavigateBranch?: (direction: 'prev' | 'next') => void;
  onRegenerate?: () => void;
  isRegenerating?: boolean;
  onEdit?: () => void;
  isEditing?: boolean;
  editingContent?: string;
  onEditChange?: (value: string) => void;
  onEditSubmit?: () => void;
  onEditCancel?: () => void;
}

function CodeBlock({ language, children }: { language: string; children: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(children);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="relative group/code rounded-2xl overflow-hidden my-4 glass-subtle">
      <div className="flex items-center justify-between px-4 py-2.5 border-b border-border/20">
        <span className="text-xs font-semibold text-primary uppercase tracking-wider">{language || 'code'}</span>
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          className="p-1.5 rounded-lg hover:bg-surface-container-high transition-all opacity-0 group-hover/code:opacity-100"
          onClick={handleCopy}
        >
          {copied ? <Check className="w-4 h-4 text-green-500" /> : <Copy className="w-4 h-4 text-muted" />}
        </motion.button>
      </div>
      <SyntaxHighlighter
        style={oneDark}
        language={language}
        PreTag="div"
        customStyle={{
          margin: 0,
          padding: '1.25rem',
          fontSize: '0.875rem',
          background: 'transparent',
          fontFamily: '"JetBrains Mono", "Fira Code", monospace'
        }}
      >
        {children}
      </SyntaxHighlighter>
    </div>
  );
}

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
  const showBranchNav = isAssistant && siblingInfo && siblingInfo.total > 1;

  return (
    <div className={clsx("flex gap-4 p-2 mb-6 group/row w-full", isUser ? "flex-row-reverse" : "flex-row")}>

      {/* Icon Area */}
      <div className="flex-shrink-0 mt-1">
        {isUser ? (
          <motion.div
            whileHover={{ scale: 1.1 }}
            className="w-9 h-9 rounded-full bg-gradient-to-br from-primary/20 to-secondary/20 flex items-center justify-center text-primary font-semibold text-sm border border-primary/20"
          >
            U
          </motion.div>
        ) : (
          <motion.div
            whileHover={{ scale: 1.1 }}
            className={clsx(
              "w-9 h-9 rounded-full flex items-center justify-center",
              isStreaming ? "animate-pulse-glow" : ""
            )}
          >
            <div className={clsx(
              "w-full h-full rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center",
              isStreaming ? "animate-breathe" : ""
            )}>
              <Sparkles className="w-5 h-5 text-white" />
            </div>
          </motion.div>
        )}
      </div>

      {/* Content Area */}
      <div className={clsx("flex flex-col max-w-[90%] md:max-w-[85%]", isUser ? "items-end" : "items-start")}>

        {/* Name */}
        <div className={clsx(
          "text-sm font-semibold mb-1.5",
          isUser ? "text-primary" : "gradient-text"
        )}>
          {isUser ? 'You' : 'AI Assistant'}
        </div>

        <div className={clsx(
          "w-full",
          // User messages get a premium bubble
          isUser && !isEditing ? "bg-gradient-to-br from-surface-container-high to-surface-container text-foreground rounded-3xl rounded-tr-lg px-5 py-3.5 shadow-sm border border-border/30" : "",
          // Assistant messages are plain text
          isAssistant ? "px-0 text-foreground" : ""
        )}>

          {isEditing ? (
            <div className="gradient-border">
              <div className="bg-surface-container rounded-2xl p-4 w-full">
                <textarea
                  className="w-full min-h-[100px] bg-transparent outline-none resize-none text-foreground"
                  value={editingContent}
                  onChange={(e) => onEditChange?.(e.target.value)}
                  autoFocus
                />
                <div className="flex justify-end gap-2 mt-3">
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={onEditCancel}
                    className="px-5 py-2 rounded-xl text-sm font-medium text-muted hover:bg-surface-container-high transition-colors"
                  >
                    Cancel
                  </motion.button>
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={onEditSubmit}
                    className="px-5 py-2 rounded-xl text-sm font-medium bg-gradient-to-r from-primary to-secondary text-white hover:shadow-glow-sm transition-all"
                  >
                    Update
                  </motion.button>
                </div>
              </div>
            </div>
          ) : (
            <div className={clsx(isUser ? "" : "markdown-body")}>
              {isUser ? (
                <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
              ) : (
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    code({ className, children, ...props }) {
                      const match = /language-(\w+)/.exec(className || '');
                      return !match ? (
                        <code className="bg-primary/10 text-primary px-1.5 py-0.5 rounded-md font-mono text-sm" {...props}>
                          {children}
                        </code>
                      ) : (
                        <CodeBlock language={match[1]}>{String(children).replace(/\n$/, '')}</CodeBlock>
                      );
                    }
                  }}
                >
                  {message.content}
                </ReactMarkdown>
              )}
            </div>
          )}

        </div>

        {/* Toolbar for AI Messages */}
        {isAssistant && !isStreaming && (
          <motion.div
            initial={{ opacity: 0, y: 5 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="flex items-center gap-1 mt-2 -ml-2 text-muted"
          >
            {showBranchNav && onNavigateBranch && (
              <BranchNavigator
                currentIndex={siblingInfo.current}
                totalCount={siblingInfo.total}
                onNavigate={onNavigateBranch}
              />
            )}

            <div className="flex items-center">
              {[
                { icon: ThumbsUp, title: "Good response" },
                { icon: ThumbsDown, title: "Bad response" },
              ].map(({ icon: Icon, title }, i) => (
                <motion.button
                  key={i}
                  whileHover={{ scale: 1.15 }}
                  whileTap={{ scale: 0.9 }}
                  className="p-2 hover:bg-surface-container-high hover:text-primary rounded-xl transition-all"
                  title={title}
                >
                  <Icon className="w-4 h-4" />
                </motion.button>
              ))}

              <motion.button
                whileHover={{ scale: 1.15 }}
                whileTap={{ scale: 0.9 }}
                className={clsx(
                  "p-2 hover:bg-surface-container-high hover:text-primary rounded-xl transition-all",
                  isRegenerating && "animate-spin"
                )}
                onClick={onRegenerate}
                title="Regenerate"
              >
                <RefreshCw className="w-4 h-4" />
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.15 }}
                whileTap={{ scale: 0.9 }}
                className="p-2 hover:bg-surface-container-high hover:text-primary rounded-xl transition-all"
                title="Share"
              >
                <Share2 className="w-4 h-4" />
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.15 }}
                whileTap={{ scale: 0.9 }}
                className="p-2 hover:bg-surface-container-high hover:text-primary rounded-xl transition-all"
                title="More"
              >
                <MoreVertical className="w-4 h-4" />
              </motion.button>
            </div>
          </motion.div>
        )}

        {/* Edit Button for User Messages */}
        {isUser && !isEditing && (
          <div className="mt-1 mr-1 self-end opacity-0 group-hover/row:opacity-100 transition-opacity duration-300">
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={onEdit}
              className="p-2 rounded-xl hover:bg-surface-container-high text-muted hover:text-primary transition-all"
            >
              <Pencil className="w-3.5 h-3.5" />
            </motion.button>
          </div>
        )}

      </div>
    </div>
  );
}
