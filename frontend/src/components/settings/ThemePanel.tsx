/**
 * 主题设置面板组件
 *
 * 提供明暗模式切换和彩色主题选择功能
 */
import { Sun, Moon, Monitor, X } from 'lucide-react';
import { useAppStore } from '../../store';
import {
    THEME_MODE_OPTIONS,
    type ThemeMode,
} from '../../config/themes';
import clsx from 'clsx';

/** 组件属性 */
interface ThemePanelProps {
    /** 是否显示面板 */
    isOpen: boolean;
    /** 关闭面板回调 */
    onClose: () => void;
}

/**
 * 获取主题模式图标组件
 */
function getThemeModeIcon(mode: ThemeMode) {
    switch (mode) {
        case 'light':
            return <Sun className="w-4 h-4" />;
        case 'dark':
            return <Moon className="w-4 h-4" />;
        case 'system':
            return <Monitor className="w-4 h-4" />;
    }
}

/**
 * 主题设置面板组件
 */
export function ThemePanel({ isOpen, onClose }: ThemePanelProps) {
    const { themeMode, setThemeMode } = useAppStore();

    // 面板未打开时不渲染
    if (!isOpen) return null;

    return (
        <>
            {/* 遮罩层 - 点击关闭 */}
            <div
                className="fixed inset-0 bg-black/30 z-40 animate-fade-in-up"
                onClick={onClose}
            />

            {/* 设置面板 */}
            <div className="fixed bottom-4 left-4 w-80 bg-[var(--bg-secondary)] border border-[var(--border-color)] rounded-2xl shadow-lg z-50 overflow-hidden animate-fade-in-up">
                {/* 标题栏 */}
                <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--border-color)]">
                    <h3 className="font-semibold text-[var(--text-primary)]">外观设置</h3>
                    <button
                        onClick={onClose}
                        className="p-1.5 rounded-lg hover:bg-[var(--bg-tertiary)] transition-colors"
                    >
                        <X className="w-4 h-4 text-[var(--text-secondary)]" />
                    </button>
                </div>

                <div className="p-4 space-y-6">
                    {/* 明暗模式切换 */}
                    <div>
                        <label className="block text-sm font-medium text-[var(--text-secondary)] mb-3">
                            主题模式
                        </label>
                        <div className="flex gap-2">
                            {THEME_MODE_OPTIONS.map((option) => (
                                <button
                                    key={option.value}
                                    onClick={() => setThemeMode(option.value)}
                                    className={clsx(
                                        'flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl text-sm font-medium transition-all',
                                        themeMode === option.value
                                            ? 'bg-[var(--accent-primary)]/15 text-[var(--accent-primary)] border border-[var(--accent-primary)]/30'
                                            : 'bg-[var(--bg-tertiary)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-transparent'
                                    )}
                                >
                                    {getThemeModeIcon(option.value)}
                                    <span>{option.label}</span>
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}
