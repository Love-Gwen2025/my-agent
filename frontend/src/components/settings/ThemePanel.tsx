/**
 * 主题设置面板组件
 *
 * 提供明暗模式切换和彩色主题选择功能
 * 面板定位在触发按钮上方，使用不透明背景
 */
import { Sun, Moon, Monitor, X } from 'lucide-react';
import { useAppStore } from '../../store';
import {
    THEME_MODE_OPTIONS,
    type ThemeMode,
} from '../../config/themes';
import clsx from 'clsx';
import { useEffect, useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

/** 锚点位置信息 */
interface AnchorRect {
    top: number;
    left: number;
    width: number;
    height: number;
}

/** 组件属性 */
interface ThemePanelProps {
    /** 是否显示面板 */
    isOpen: boolean;
    /** 关闭面板回调 */
    onClose: () => void;
    /** 触发按钮的位置信息 */
    anchorRect?: AnchorRect | null;
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
 * 定位在触发按钮上方，不透明背景
 */
export function ThemePanel({ isOpen, onClose, anchorRect }: ThemePanelProps) {
    const { themeMode, setThemeMode } = useAppStore();
    const panelRef = useRef<HTMLDivElement>(null);
    const [panelStyle, setPanelStyle] = useState<React.CSSProperties>({});

    // 根据锚点位置计算面板位置
    useEffect(() => {
        if (!isOpen || !anchorRect) {
            // 没有锚点位置时，使用默认左下角定位
            setPanelStyle({
                position: 'fixed',
                bottom: '16px',
                left: '16px',
            });
            return;
        }

        // 面板宽度
        const panelWidth = 320;
        // 面板高度（预估值，实际会根据内容变化）
        const panelHeight = 180;
        // 间距
        const gap = 8;

        // 计算面板位置：在按钮上方，左对齐
        let left = anchorRect.left;
        let bottom = window.innerHeight - anchorRect.top + gap;

        // 确保面板不超出屏幕右边界
        if (left + panelWidth > window.innerWidth - 16) {
            left = window.innerWidth - panelWidth - 16;
        }

        // 确保面板不超出屏幕左边界
        if (left < 16) {
            left = 16;
        }

        // 如果上方空间不足，显示在按钮下方
        if (anchorRect.top - panelHeight - gap < 16) {
            bottom = window.innerHeight - anchorRect.top - anchorRect.height - panelHeight - gap;
        }

        setPanelStyle({
            position: 'fixed',
            bottom: `${bottom}px`,
            left: `${left}px`,
        });
    }, [isOpen, anchorRect]);

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    {/* 遮罩层 - 透明，只用于点击关闭 */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.15 }}
                        className="fixed inset-0 z-40"
                        onClick={onClose}
                    />

                    {/* 设置面板 - 不透明背景 */}
                    <motion.div
                        ref={panelRef}
                        initial={{ opacity: 0, y: 10, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 10, scale: 0.95 }}
                        transition={{ duration: 0.2, ease: 'easeOut' }}
                        style={panelStyle}
                        className="w-80 bg-surface-container rounded-2xl shadow-2xl z-50 overflow-hidden border border-border/50"
                    >
                        {/* 标题栏 */}
                        <div className="flex items-center justify-between px-4 py-3 border-b border-border/30 bg-surface-container-high/50">
                            <h3 className="font-semibold text-foreground">外观设置</h3>
                            <button
                                onClick={onClose}
                                className="p-1.5 rounded-lg hover:bg-surface-container-high transition-colors"
                            >
                                <X className="w-4 h-4 text-muted" />
                            </button>
                        </div>

                        <div className="p-4 space-y-6 bg-surface-container">
                            {/* 明暗模式切换 */}
                            <div>
                                <label className="block text-sm font-medium text-muted mb-3">
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
                                                    ? 'bg-primary/15 text-primary border border-primary/30'
                                                    : 'bg-surface-container-high text-muted hover:text-foreground border border-transparent'
                                            )}
                                        >
                                            {getThemeModeIcon(option.value)}
                                            <span>{option.label}</span>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
}
