/**
 * 消息输入框组件 - Premium Edition
 */
import { useState, useRef, useEffect } from 'react';
import { Send, StopCircle, Plus, Mic, Image as ImageIcon, Sparkles } from 'lucide-react';
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
    <div className="absolute bottom-0 left-0 right-0 p-4 pb-6 bg-gradient-to-t from-background via-background/95 to-transparent z-20">
      <div className="max-w-3xl mx-auto relative">
        <motion.div
          initial={false}
          animate={{
            boxShadow: isFocused
              ? '0 0 0 3px rgba(var(--primary), 0.15), 0 8px 32px rgba(var(--primary), 0.12)'
              : '0 4px 20px rgba(0, 0, 0, 0.08)'
          }}
          className={clsx(
            'flex items-end gap-2 p-3 rounded-[28px] transition-all duration-300',
            'glass border',
            isFocused ? 'border-primary/30' : 'border-transparent'
          )}
        >
          {/* Left Actions */}
          <motion.button
            whileHover={{ scale: 1.1, rotate: 90 }}
            whileTap={{ scale: 0.9 }}
            className="p-2.5 rounded-xl hover:bg-surface-container-high text-muted hover:text-primary transition-all"
          >
            <Plus className="w-5 h-5" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.9 }}
            className="p-2.5 rounded-xl hover:bg-surface-container-high text-muted hover:text-primary transition-all hidden sm:block"
          >
            <ImageIcon className="w-5 h-5" />
          </motion.button>

          <textarea
            ref={textareaRef}
            className="flex-1 bg-transparent text-foreground resize-none outline-none py-3 px-2 max-h-[200px] placeholder:text-muted/60 text-base min-h-[52px]"
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
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              className="p-2.5 rounded-xl hover:bg-surface-container-high text-muted hover:text-primary transition-all hidden sm:block"
            >
              <Mic className="w-5 h-5" />
            </motion.button>

            <AnimatePresence mode="wait">
              {isLoading ? (
                <motion.button
                  key="stop"
                  initial={{ scale: 0, rotate: -180 }}
                  animate={{ scale: 1, rotate: 0 }}
                  exit={{ scale: 0, rotate: 180 }}
                  whileHover={{ scale: 1.1 }}
                  whileTap={{ scale: 0.9 }}
                  className="p-2.5 rounded-xl bg-red-500 text-white hover:bg-red-600 transition-colors shadow-lg"
                  onClick={onAbort}
                >
                  <StopCircle className="w-5 h-5 fill-current" />
                </motion.button>
              ) : (
                input.trim() && (
                  <motion.button
                    key="send"
                    initial={{ scale: 0, rotate: -180 }}
                    animate={{ scale: 1, rotate: 0 }}
                    exit={{ scale: 0, rotate: 180 }}
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                    className="p-2.5 rounded-xl bg-gradient-to-r from-primary to-secondary text-white shadow-glow-sm hover:shadow-glow-md transition-all"
                    onClick={handleSend}
                  >
                    <Send className="w-5 h-5 ml-0.5" />
                  </motion.button>
                )
              )}
            </AnimatePresence>

            {/* AI indicator when idle and no input */}
            {!isLoading && !input.trim() && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="p-2.5 rounded-xl text-muted"
              >
                <Sparkles className="w-5 h-5" />
              </motion.div>
            )}
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="text-center mt-3"
        >
          <p className="text-xs text-muted/60">
            AI may generate inaccurate information. Verify important facts.
          </p>
        </motion.div>
      </div>
    </div>
  );
}
