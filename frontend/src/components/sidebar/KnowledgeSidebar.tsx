/**
 * 知识库侧边栏组件
 *
 * 集成在主侧边栏中，提供知识库列表、新建、管理功能
 */
import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    Database,
    Plus,
    ChevronDown,
    Trash2,
    TestTube,
    Loader2,
} from 'lucide-react';
import clsx from 'clsx';
import {
    getKnowledgeBases,
    createKnowledgeBase,
    deleteKnowledgeBase,
    type KnowledgeBase,
} from '../../api/knowledge';

interface KnowledgeSidebarProps {
    isCollapsed: boolean;
    onOpenDetail?: (kb: KnowledgeBase) => void;
    onOpenRecallTest?: (kb: KnowledgeBase) => void;
}

export function KnowledgeSidebar({
    isCollapsed,
    onOpenDetail,
    onOpenRecallTest,
}: KnowledgeSidebarProps) {
    const [isExpanded, setIsExpanded] = useState(false);
    const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isCreating, setIsCreating] = useState(false);
    const [newKbName, setNewKbName] = useState('');
    const [showCreateInput, setShowCreateInput] = useState(false);

    // 加载知识库列表
    useEffect(() => {
        if (isExpanded) {
            loadKnowledgeBases();
        }
    }, [isExpanded]);

    async function loadKnowledgeBases() {
        setIsLoading(true);
        try {
            const kbs = await getKnowledgeBases();
            setKnowledgeBases(kbs);
        } catch (error) {
            console.error('Failed to load knowledge bases:', error);
        } finally {
            setIsLoading(false);
        }
    }

    // 创建知识库
    async function handleCreateKnowledgeBase() {
        if (!newKbName.trim()) return;

        setIsCreating(true);
        try {
            const newKb = await createKnowledgeBase(newKbName.trim());
            setKnowledgeBases([newKb, ...knowledgeBases]);
            setNewKbName('');
            setShowCreateInput(false);
        } catch (error) {
            console.error('Failed to create knowledge base:', error);
        } finally {
            setIsCreating(false);
        }
    }

    // 删除知识库
    async function handleDeleteKnowledgeBase(id: string, e: React.MouseEvent) {
        e.stopPropagation();
        if (!confirm('确定要删除这个知识库吗？所有文档都将被删除。')) return;

        try {
            await deleteKnowledgeBase(id);
            setKnowledgeBases(knowledgeBases.filter((kb) => kb.id !== id));
        } catch (error) {
            console.error('Failed to delete knowledge base:', error);
        }
    }

    // 折叠模式下只显示图标
    if (isCollapsed) {
        return (
            <motion.div
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setIsExpanded(!isExpanded)}
                className="flex items-center justify-center w-12 h-12 mx-auto rounded-2xl cursor-pointer transition-all duration-300 hover:bg-surface-container-high/80 text-cyan-400 hover:text-cyan-300"
                title="知识库"
            >
                <Database className="w-5 h-5" />
            </motion.div>
        );
    }

    return (
        <div className="px-2 py-1">
            {/* 折叠标题 */}
            <motion.div
                whileHover={{ x: 4 }}
                onClick={() => setIsExpanded(!isExpanded)}
                className={clsx(
                    'flex items-center gap-3 px-3 py-2.5 rounded-2xl cursor-pointer transition-all duration-300',
                    'hover:bg-surface-container-high/80 text-foreground group'
                )}
            >
                <Database
                    className={clsx(
                        'w-5 h-5 transition-all duration-300 text-cyan-400 group-hover:text-cyan-300'
                    )}
                />
                <span className="text-sm font-medium flex-1">知识库</span>
                <motion.div
                    animate={{ rotate: isExpanded ? 0 : -90 }}
                    transition={{ duration: 0.2 }}
                >
                    <ChevronDown className="w-4 h-4 text-muted" />
                </motion.div>
            </motion.div>

            {/* 展开内容 */}
            <AnimatePresence>
                {isExpanded && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.2 }}
                        className="overflow-hidden"
                    >
                        <div className="pl-4 pr-2 py-2 space-y-1">
                            {/* 新建按钮 */}
                            {!showCreateInput ? (
                                <motion.button
                                    whileHover={{ scale: 1.02 }}
                                    whileTap={{ scale: 0.98 }}
                                    onClick={() => setShowCreateInput(true)}
                                    className="flex items-center gap-2 w-full px-3 py-2 rounded-xl text-sm text-muted hover:text-foreground hover:bg-primary/10 transition-all"
                                >
                                    <Plus className="w-4 h-4" />
                                    <span>新建知识库</span>
                                </motion.button>
                            ) : (
                                <div className="flex items-center gap-2 px-2 py-1">
                                    <input
                                        type="text"
                                        value={newKbName}
                                        onChange={(e) => setNewKbName(e.target.value)}
                                        onKeyDown={(e) => {
                                            if (e.key === 'Enter') handleCreateKnowledgeBase();
                                            if (e.key === 'Escape') {
                                                setShowCreateInput(false);
                                                setNewKbName('');
                                            }
                                        }}
                                        placeholder="输入名称..."
                                        className="flex-1 bg-surface-container-high/50 text-sm px-2 py-1.5 rounded-lg outline-none border border-border/50 focus:border-primary/50"
                                        autoFocus
                                    />
                                    <button
                                        onClick={handleCreateKnowledgeBase}
                                        disabled={isCreating || !newKbName.trim()}
                                        className="p-1.5 bg-primary/20 hover:bg-primary/30 rounded-lg text-primary disabled:opacity-50"
                                    >
                                        {isCreating ? (
                                            <Loader2 className="w-4 h-4 animate-spin" />
                                        ) : (
                                            <Plus className="w-4 h-4" />
                                        )}
                                    </button>
                                </div>
                            )}

                            {/* 加载状态 */}
                            {isLoading && (
                                <div className="flex items-center justify-center py-4">
                                    <Loader2 className="w-5 h-5 animate-spin text-muted" />
                                </div>
                            )}

                            {/* 知识库列表 */}
                            {!isLoading &&
                                knowledgeBases.map((kb) => (
                                    <motion.div
                                        key={kb.id}
                                        initial={{ opacity: 0, x: -10 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        className={clsx(
                                            'flex items-center gap-2 px-3 py-2 rounded-xl cursor-pointer group',
                                            'hover:bg-surface-container-high/80 transition-all'
                                        )}
                                        onClick={() => onOpenDetail?.(kb)}
                                    >
                                        <Database className="w-4 h-4 text-cyan-400 flex-shrink-0" />
                                        <div className="flex-1 min-w-0">
                                            <div className="text-sm font-medium truncate">
                                                {kb.name}
                                            </div>
                                            <div className="text-xs text-muted">
                                                {kb.documentCount} 文档 · {kb.chunkCount} 分块
                                            </div>
                                        </div>
                                        {/* 操作按钮 */}
                                        <div className="opacity-0 group-hover:opacity-100 flex items-center gap-1 transition-opacity">
                                            <button
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    onOpenRecallTest?.(kb);
                                                }}
                                                className="p-1 hover:bg-cyan-500/20 rounded-full text-muted hover:text-cyan-400"
                                                title="召回测试"
                                            >
                                                <TestTube className="w-3.5 h-3.5" />
                                            </button>
                                            <button
                                                onClick={(e) => handleDeleteKnowledgeBase(kb.id, e)}
                                                className="p-1 hover:bg-red-500/20 rounded-full text-muted hover:text-red-400"
                                                title="删除"
                                            >
                                                <Trash2 className="w-3.5 h-3.5" />
                                            </button>
                                        </div>
                                    </motion.div>
                                ))}

                            {/* 空状态 */}
                            {!isLoading && knowledgeBases.length === 0 && (
                                <div className="text-center py-4 text-sm text-muted">
                                    暂无知识库
                                </div>
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
