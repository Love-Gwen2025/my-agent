/**
 * 模型选择器组件
 */
import { useEffect, useState } from 'react';
import { ChevronDown, Bot } from 'lucide-react';
import { useAppStore } from '../../store';
import { getModels } from '../../api';
import clsx from 'clsx';

/**
 * 模型选择器组件
 */
export function ModelSelector() {
  const { models, currentModelCode, setModels, setCurrentModelCode, token } =
    useAppStore();
  const [isOpen, setIsOpen] = useState(false);

  /** 加载模型列表 */
  useEffect(() => {
    if (token) {
      loadModels();
    }
  }, [token]);

  async function loadModels() {
    try {
      const data = await getModels();
      setModels(data);
      // 设置默认模型
      if (!currentModelCode && data.length > 0) {
        const defaultModel = data.find((m) => m.isDefault) || data[0];
        setCurrentModelCode(defaultModel.modelCode);
      }
    } catch (error) {
      console.error('加载模型列表失败:', error);
    }
  }

  const currentModel = models.find((m) => m.modelCode === currentModelCode);

  if (models.length === 0) {
    return null;
  }

  return (
    <div className="relative">
      <button
        className="flex items-center gap-2 px-3 py-1.5 bg-[var(--bg-secondary)] rounded-lg hover:bg-[var(--bg-tertiary)] transition-colors text-sm"
        onClick={() => setIsOpen(!isOpen)}
      >
        <Bot className="w-4 h-4 text-[var(--accent-primary)]" />
        <span>{currentModel?.modelName || '选择模型'}</span>
        <ChevronDown
          className={clsx(
            'w-4 h-4 transition-transform',
            isOpen && 'rotate-180'
          )}
        />
      </button>

      {isOpen && (
        <>
          {/* 背景遮罩 */}
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />
          {/* 下拉菜单 */}
          <div className="absolute top-full left-0 mt-1 w-48 bg-[var(--bg-secondary)] border border-[var(--border-color)] rounded-lg shadow-lg z-20 py-1">
            {models.map((model) => (
              <button
                key={model.id}
                className={clsx(
                  'w-full px-3 py-2 text-left text-sm hover:bg-[var(--bg-tertiary)] transition-colors flex items-center justify-between',
                  model.modelCode === currentModelCode &&
                    'text-[var(--accent-primary)]'
                )}
                onClick={() => {
                  setCurrentModelCode(model.modelCode);
                  setIsOpen(false);
                }}
              >
                <span>{model.modelName}</span>
                {model.isDefault && (
                  <span className="text-xs text-[var(--text-secondary)]">
                    默认
                  </span>
                )}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
