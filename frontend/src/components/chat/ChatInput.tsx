/**
 * 消息输入框组件 - Refined Modern Edition
 */
import { useState, useRef, useEffect } from 'react';
import { Send, StopCircle, Plus } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';

interface ChatInputProps {
  isLoading?: boolean;
  disabled?: boolean;
  onSend: (content: string) => void;
  onAbort?: () => void;
}

export function ChatInput({
  isLoading,
  disabled,
  onSend,
  onAbort,
}: ChatInputProps) {
  const [input, setInput] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`;
    }
  }, [input]);

  const handleSend = () => {
    const content = input.trim();
    if (!content || isLoading || disabled) return;
    onSend(content);
    setInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="shrink-0 p-4 pb-6 bg-background border-t border-border/30">
      <div className="max-w-3xl mx-auto relative">
        <motion.div
          initial={false}
          animate={{
            boxShadow: isFocused
              ? '0 0 0 2px rgba(var(--primary), 0.15), 0 8px 32px rgba(0, 0, 0, 0.08)'
              : '0 4px 12px rgba(0, 0, 0, 0.05)'
          }}
          className={clsx(
            'flex items-end gap-2 p-2 rounded-[26px] transition-all duration-300',
            'glass bg-surface/80 backdrop-blur-xl', // Stronger blur
            isFocused ? 'border-primary/20' : 'border-border/50'
          )}
        >
          {/* Left Actions */}
          <button
            className="p-2.5 rounded-full hover:bg-surface-highlight text-muted hover:text-foreground transition-colors"
          >
            <Plus className="w-5 h-5" />
          </button>

          <textarea
            ref={textareaRef}
            className="flex-1 bg-transparent text-foreground resize-none outline-none py-3 px-1 max-h-[200px] placeholder:text-muted/60 text-[15px] min-h-[48px] leading-relaxed"
            placeholder="Ask anything..."
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            disabled={disabled}
          />

          {/* Right Actions */}
          <div className="flex items-center gap-1 pb-1">
            <AnimatePresence mode="wait">
              {isLoading ? (
                <motion.button
                  key="stop"
                  initial={{ scale: 0, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  exit={{ scale: 0, opacity: 0 }}
                  className="p-2 rounded-full bg-red-500/10 text-red-500 hover:bg-red-500/20 transition-colors"
                  onClick={onAbort}
                >
                  <StopCircle className="w-6 h-6 fill-current" />
                </motion.button>
              ) : (
                <motion.button
                  key="send"
                  disabled={!input.trim()}
                  initial={{ scale: 0.9, opacity: 0.5 }}
                  animate={{
                    scale: input.trim() ? 1 : 0.9,
                    opacity: input.trim() ? 1 : 0.5
                  }}
                  className={clsx(
                    "p-2 rounded-full transition-all",
                    input.trim()
                      ? "bg-primary text-primary-foreground shadow-sm hover:shadow-glow-sm"
                      : "bg-surface-highlight text-muted"
                  )}
                  onClick={handleSend}
                >
                  <Send className="w-5 h-5 ml-0.5" />
                </motion.button>
              )}
            </AnimatePresence>
          </div>
        </motion.div>

        <div className="text-center mt-3">
          <p className="text-[10px] text-muted/50 font-medium tracking-wide uppercase">
            AI Generated Content • Verify Important Info
          </p>
        </div>
      </div>
    </div>
  );
}
