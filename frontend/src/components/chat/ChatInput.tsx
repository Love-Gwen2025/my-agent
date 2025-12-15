/**
 * 消息输入框组件
 */
import { useState, useRef, useEffect } from 'react';
import { Send, Loader2, StopCircle } from 'lucide-react';
import clsx from 'clsx';

interface ChatInputProps {
  /** 是否正在加载 */
  isLoading?: boolean;
  /** 是否禁用 */
  disabled?: boolean;
  /** 发送消息回调 */
  onSend: (content: string) => void;
  /** 中止请求回调 */
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
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  /** 自动调整高度 */
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`;
    }
  }, [input]);

  /** 处理发送 */
  const handleSend = () => {
    const content = input.trim();
    if (!content || isLoading || disabled) return;
    onSend(content);
    setInput('');
  };

  /** 处理键盘事件 */
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="p-4 border-t border-[var(--border-color)] bg-[var(--bg-secondary)]">
      <div className="max-w-4xl mx-auto">
        <div className="relative flex items-end gap-2 bg-[var(--bg-primary)] rounded-2xl border border-[var(--border-color)] p-2">
          <textarea
            ref={textareaRef}
            className="flex-1 bg-transparent text-[var(--text-primary)] resize-none outline-none px-2 py-1 max-h-[200px] placeholder:text-[var(--text-secondary)]"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={disabled}
          />
          <button
            className={clsx(
              'p-2 rounded-xl transition-colors',
              isLoading
                ? 'bg-red-500 hover:bg-red-600'
                : input.trim() && !disabled
                ? 'bg-[var(--accent-primary)] hover:opacity-90'
                : 'bg-[var(--bg-tertiary)] cursor-not-allowed'
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
        <div className="text-xs text-[var(--text-secondary)] text-center mt-2">
          AI 生成内容仅供参考，请核实重要信息
        </div>
      </div>
    </div>
  );
}
