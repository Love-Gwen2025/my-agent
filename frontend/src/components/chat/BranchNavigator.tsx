/**
 * 分支导航器组件
 */
import { ChevronLeft, ChevronRight } from 'lucide-react';


export interface BranchNavigatorProps {
    currentIndex: number;
    totalCount: number;
    onNavigate: (direction: 'prev' | 'next') => void;
    isLoading?: boolean;
}

export function BranchNavigator({
    currentIndex,
    totalCount,
    onNavigate,
    isLoading = false,
}: BranchNavigatorProps) {
    if (totalCount <= 1) return null;

    const canGoPrev = currentIndex > 0 && !isLoading;
    const canGoNext = currentIndex < totalCount - 1 && !isLoading;

    return (
        <div className="flex items-center gap-1 bg-surface-highlight/50 rounded-lg p-0.5 border border-border/50 backdrop-blur-sm">
            <button
                onClick={() => onNavigate('prev')}
                disabled={!canGoPrev}
                className="p-1 rounded-md hover:bg-surface disabled:opacity-30 disabled:hover:bg-transparent transition-all"
                title="上一个版本"
            >
                <ChevronLeft className="w-3.5 h-3.5 text-muted hover:text-foreground" />
            </button>

            <span className="text-xs font-mono text-muted tabular-nums min-w-[2.5rem] text-center select-none">
                <span className="text-foreground font-medium">{currentIndex + 1}</span>
                <span className="opacity-50 mx-0.5">/</span>
                <span>{totalCount}</span>
            </span>

            <button
                onClick={() => onNavigate('next')}
                disabled={!canGoNext}
                className="p-1 rounded-md hover:bg-surface disabled:opacity-30 disabled:hover:bg-transparent transition-all"
                title="下一个版本"
            >
                <ChevronRight className="w-3.5 h-3.5 text-muted hover:text-foreground" />
            </button>
        </div>
    );
}
