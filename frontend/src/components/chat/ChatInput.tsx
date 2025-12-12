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
    <div className="max-w-3xl mx-auto w-full relative">
        <div className={clsx(
            "flex items-end gap-2 bg-[#f4f4f5] rounded-3xl p-3 transition-all duration-200",
            "focus-within:bg-white focus-within:shadow-[0_0_15px_rgba(0,0,0,0.1)] focus-within:ring-1 focus-within:ring-gray-100"
        )}>
            {/* 这里的 Plus 按钮可以用来上传文件，先占位 */}
            <button className="p-2 rounded-full text-gray-400 hover:bg-gray-200 hover:text-gray-600 transition-colors">
                <div className="w-5 h-5 flex items-center justify-center border-2 border-current rounded-full">
                    <span className="text-lg leading-none mb-0.5">+</span>
                </div>
            </button>

          <textarea
            ref={textareaRef}
            className="flex-1 bg-transparent text-[var(--text-primary)] resize-none outline-none px-2 py-2 max-h-[200px] placeholder:text-gray-400 leading-6"
            placeholder="Ask Gemini..."
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={disabled}
          />
          
          {isLoading ? (
             <button
                className="p-2 rounded-full bg-black text-white hover:opacity-80 transition-opacity"
                onClick={onAbort}
             >
                <StopCircle className="w-5 h-5" />
             </button>
          ) : (
            <button
                className={clsx(
                'p-2 rounded-full transition-all duration-200',
                input.trim() 
                    ? 'bg-[#3b82f6] text-white rotate-0 opacity-100' 
                    : 'bg-transparent text-gray-300 rotate-90 opacity-0 cursor-default'
                )}
                onClick={handleSend}
                disabled={!input.trim() || disabled}
            >
                <Send className="w-5 h-5" />
            </button>
          )}
        </div>
        <div className="text-[11px] text-[#9ca3af] text-center mt-3">
          Gemini may display inaccurate info, including about people, so double-check its responses.
        </div>
    </div>
  );
}
