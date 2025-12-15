/**
 * 消息输入框组件
 * 
 * Modern input with glass effect and gradient send button
 */
import { useState, useRef, useEffect } from 'react';
import { Send, Loader2, StopCircle, Sparkles } from 'lucide-react';
import clsx from 'clsx';

interface ChatInputProps {
  isLoading?: boolean;
  disabled?: boolean;
  onSend: (content: string) => void;
  onAbort?: () => void;
}

/**
 * 消息输入框组件
 */
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
    <div className="p-4 border-t border-[var(--border-color)] bg-[var(--bg-secondary)]">
      <div className="max-w-4xl mx-auto">
        <div
          className={clsx(
            'relative flex items-end gap-3 glass-effect rounded-2xl p-3 transition-all duration-300',
            isFocused && 'ring-2 ring-[var(--accent-primary)]/50 shadow-lg'
          )}
        >
          <textarea
            ref={textareaRef}
            className="flex-1 bg-transparent text-[var(--text-primary)] resize-none outline-none px-2 py-1 max-h-[200px] placeholder:text-[var(--text-secondary)] text-base"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行..."
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            disabled={disabled}
          />
          <button
            className={clsx(
              'p-3 rounded-xl transition-all duration-300 flex-shrink-0',
              isLoading
                ? 'bg-red-500 hover:bg-red-600 shadow-lg'
                : input.trim() && !disabled
                  ? 'btn-gradient shadow-lg'
                  : 'bg-[var(--bg-tertiary)] cursor-not-allowed opacity-50'
            )}
            onClick={isLoading ? onAbort : handleSend}
            disabled={!isLoading && (!input.trim() || disabled)}
          >
            {isLoading ? (
              onAbort ? (
                <StopCircle className="w-5 h-5 text-white" />
              ) : (
                <Loader2 className="w-5 h-5 text-white animate-spin" />
              )
            ) : (
              <Send className="w-5 h-5 text-white" />
            )}
          </button>
        </div>
        <div className="flex items-center justify-center gap-1 text-xs text-[var(--text-secondary)] mt-3 opacity-70">
          <Sparkles className="w-3 h-3" />
          <span>AI 生成内容仅供参考，请核实重要信息</span>
        </div>
      </div>
    </div>
  );
}
