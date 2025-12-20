/**
 * 分支导航器组件
 * 
 * 用于在同一用户消息的多个 AI 回复之间切换
 * UI 类似 ChatGPT 的 "← 2/3 →"
 */
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface BranchNavigatorProps {
    /** 当前分支索引 (0-based) */
    currentIndex: number;
    /** 总分支数量 */
    totalCount: number;
    /** 切换分支回调 */
    onNavigate: (direction: 'prev' | 'next') => void;
    /** 加载中状态 */
    isLoading?: boolean;
}

/**
 * 分支导航器
 * 
 * 当只有一个分支时不渲染
 */
export function BranchNavigator({
    currentIndex,
    totalCount,
    onNavigate,
    isLoading = false,
}: BranchNavigatorProps) {
    // 只有一个分支时不显示
    if (totalCount <= 1) return null;

    const canGoPrev = currentIndex > 0 && !isLoading;
    const canGoNext = currentIndex < totalCount - 1 && !isLoading;

    return (
        <div className="flex items-center gap-1 text-xs">
            <button
                onClick={() => onNavigate('prev')}
                disabled={!canGoPrev}
                className="p-1 rounded hover:bg-[var(--bg-tertiary)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="上一个版本"
            >
                <ChevronLeft className="w-3.5 h-3.5 text-[var(--text-secondary)]" />
            </button>

            <span className="text-[var(--text-secondary)] tabular-nums min-w-[2.5rem] text-center">
                {currentIndex + 1} / {totalCount}
            </span>

            <button
                onClick={() => onNavigate('next')}
                disabled={!canGoNext}
                className="p-1 rounded hover:bg-[var(--bg-tertiary)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="下一个版本"
            >
                <ChevronRight className="w-3.5 h-3.5 text-[var(--text-secondary)]" />
            </button>
        </div>
    );
}
