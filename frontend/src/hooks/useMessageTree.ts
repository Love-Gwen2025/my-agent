/**
 * 消息树管理 Hook
 *
 * 功能：
 * 1. 存储完整消息树
 * 2. 计算当前显示路径
 * 3. 本地切换分支
 * 4. 获取分支信息
 */
import { useState, useCallback, useMemo } from 'react';
import type { Message, SiblingInfo } from '../types';

interface UseMessageTreeOptions {
    /** 保存当前选中消息到后端的回调 */
    onSaveCurrentMessage?: (messageId: string) => Promise<void>;
}

interface UseMessageTreeReturn {
    /** 所有消息 */
    allMessages: Message[];
    /** 当前显示的消息列表 */
    displayMessages: Message[];
    /** 当前路径上的消息 ID 列表 */
    currentPath: string[];
    /** 设置消息树（从后端加载后调用） */
    setMessageTree: (messages: Message[], currentMessageId: string | null) => void;
    /** 切换分支 */
    switchBranch: (targetMessageId: string) => void;
    /** 获取消息的兄弟分支信息 */
    getSiblingInfo: (messageId: string) => SiblingInfo | null;
    /** 添加新消息（发送/收到消息时调用） */
    addMessage: (message: Message) => void;
    /** 更新消息（替换指定 ID 的消息） */
    updateMessage: (messageId: string, updater: (msg: Message) => Message) => void;
    /** 替换消息 ID（用于将临时 ID 替换为服务器返回的真实 ID） */
    replaceMessageId: (oldId: string, newId: string) => void;
    /** 清空消息树 */
    clear: () => void;
}

/**
 * 消息树管理 Hook
 */
export function useMessageTree(
    options: UseMessageTreeOptions = {}
): UseMessageTreeReturn {
    const { onSaveCurrentMessage } = options;

    // 完整消息列表
    const [allMessages, setAllMessages] = useState<Message[]>([]);
    // 当前显示的消息 ID 路径
    const [currentPath, setCurrentPath] = useState<string[]>([]);

    // 辅助结构：id -> Message
    const messageMap = useMemo(() => {
        const map = new Map<string, Message>();
        allMessages.forEach((m) => map.set(String(m.id), m));
        return map;
    }, [allMessages]);

    // 辅助结构：parentId -> children[]
    const childrenMap = useMemo(() => {
        const map = new Map<string | null, Message[]>();
        allMessages.forEach((m) => {
            const parentId = m.parentId ?? null;
            const children = map.get(parentId) || [];
            children.push(m);
            map.set(parentId, children);
        });
        return map;
    }, [allMessages]);

    /**
     * 从指定消息回溯到根节点，返回路径
     */
    const traceToRoot = useCallback(
        (messageId: string): string[] => {
            const path: string[] = [];
            let current = messageMap.get(messageId);
            while (current) {
                path.unshift(String(current.id));
                current = current.parentId
                    ? messageMap.get(String(current.parentId))
                    : undefined;
            }
            return path;
        },
        [messageMap]
    );

    /**
     * 从指定消息向下延伸到叶子节点（选择最新的子节点）
     */
    const extendToLeaf = useCallback(
        (messageId: string): string[] => {
            const path: string[] = [];
            let currentId = messageId;
            while (true) {
                const children = childrenMap.get(currentId) || [];
                if (children.length === 0) break;
                // 选择最新的子消息
                const newest = children.sort(
                    (a, b) =>
                        new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
                )[0];
                path.push(String(newest.id));
                currentId = String(newest.id);
            }
            return path;
        },
        [childrenMap]
    );

    /**
     * 设置消息树（从后端加载后调用）
     */
    const setMessageTree = useCallback(
        (messages: Message[], currentMessageId: string | null) => {
            setAllMessages(messages);

            if (messages.length === 0) {
                setCurrentPath([]);
                return;
            }

            // 构建临时辅助结构（因为 state 更新是异步的，不能依赖 useMemo 的 map）
            const tempMsgMap = new Map<string, Message>();
            messages.forEach((m) => tempMsgMap.set(String(m.id), m));

            const tempChildrenMap = new Map<string | null, Message[]>();
            messages.forEach((m) => {
                const parentId = m.parentId ?? null;
                const children = tempChildrenMap.get(parentId) || [];
                children.push(m);
                tempChildrenMap.set(parentId, children);
            });

            // 本地版本的 traceToRoot
            const localTraceToRoot = (msgId: string): string[] => {
                const path: string[] = [];
                let current = tempMsgMap.get(msgId);
                while (current) {
                    path.unshift(String(current.id));
                    current = current.parentId
                        ? tempMsgMap.get(String(current.parentId))
                        : undefined;
                }
                return path;
            };

            // 本地版本的 extendToLeaf
            const localExtendToLeaf = (msgId: string): string[] => {
                const path: string[] = [];
                let currentId = msgId;
                while (true) {
                    const children = tempChildrenMap.get(currentId) || [];
                    if (children.length === 0) break;
                    const newest = [...children].sort(
                        (a, b) =>
                            new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
                    )[0];
                    path.push(String(newest.id));
                    currentId = String(newest.id);
                }
                return path;
            };

            // 计算当前路径
            if (currentMessageId && tempMsgMap.has(currentMessageId)) {
                // 从 currentMessageId 回溯到根，再向下延伸到叶子
                const pathToRoot = localTraceToRoot(currentMessageId);
                const pathToLeaf = localExtendToLeaf(currentMessageId);
                setCurrentPath([...pathToRoot, ...pathToLeaf]);
            } else {
                // 使用默认策略
                const roots = tempChildrenMap.get(null) || [];
                if (roots.length === 0) {
                    setCurrentPath([]);
                    return;
                }

                const firstRoot = [...roots].sort(
                    (a, b) =>
                        new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
                )[0];
                const path = [String(firstRoot.id)];
                path.push(...localExtendToLeaf(String(firstRoot.id)));
                setCurrentPath(path);
            }
        },
        []
    );

    /**
     * 切换分支
     */
    const switchBranch = useCallback(
        (targetMessageId: string) => {
            // 1. 从目标回溯到根
            const pathToRoot = traceToRoot(targetMessageId);
            // 2. 从目标向下延伸到叶子
            const pathToLeaf = extendToLeaf(targetMessageId);
            // 3. 合并路径
            const newPath = [...pathToRoot, ...pathToLeaf];
            setCurrentPath(newPath);

            // 4. 异步保存叶子节点到后端
            const leafId = newPath[newPath.length - 1];
            onSaveCurrentMessage?.(leafId);
        },
        [traceToRoot, extendToLeaf, onSaveCurrentMessage]
    );

    /**
     * 获取消息的兄弟分支信息
     */
    const getSiblingInfo = useCallback(
        (messageId: string): SiblingInfo | null => {
            const message = messageMap.get(messageId);
            if (!message) return null;

            const parentId = message.parentId ?? null;
            const siblings = childrenMap.get(parentId) || [];

            if (siblings.length <= 1) return null;

            // 按创建时间排序
            const sortedSiblings = [...siblings].sort(
                (a, b) =>
                    new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
            );

            const currentIndex = sortedSiblings.findIndex(
                (s) => String(s.id) === messageId
            );

            return {
                current: currentIndex,
                total: sortedSiblings.length,
                siblings: sortedSiblings.map((s) => String(s.id)),
            };
        },
        [messageMap, childrenMap]
    );

    /**
     * 添加新消息
     */
    const addMessage = useCallback((message: Message) => {
        setAllMessages((prev) => [...prev, message]);
        setCurrentPath((prev) => [...prev, String(message.id)]);
    }, []);

    /**
     * 更新消息
     */
    const updateMessage = useCallback(
        (messageId: string, updater: (msg: Message) => Message) => {
            setAllMessages((prev) =>
                prev.map((msg) =>
                    String(msg.id) === messageId ? updater(msg) : msg
                )
            );
        },
        []
    );

    /**
     * 替换消息 ID（用于将临时 ID 替换为服务器返回的真实 ID）
     */
    const replaceMessageId = useCallback(
        (oldId: string, newId: string) => {
            setAllMessages((prev) =>
                prev.map((msg) => {
                    // 替换消息自身的 ID
                    if (String(msg.id) === oldId) {
                        return { ...msg, id: newId };
                    }
                    // 替换其他消息的 parentId 引用
                    if (msg.parentId === oldId) {
                        return { ...msg, parentId: newId };
                    }
                    return msg;
                })
            );
            // 同时更新 currentPath
            setCurrentPath((prev) =>
                prev.map((id) => (id === oldId ? newId : id))
            );
        },
        []
    );

    /**
     * 清空消息树
     */
    const clear = useCallback(() => {
        setAllMessages([]);
        setCurrentPath([]);
    }, []);

    // 当前显示的消息列表
    const displayMessages = useMemo(
        () =>
            currentPath
                .map((id) => messageMap.get(id))
                .filter((m): m is Message => m !== undefined),
        [currentPath, messageMap]
    );

    return {
        allMessages,
        displayMessages,
        currentPath,
        setMessageTree,
        switchBranch,
        getSiblingInfo,
        addMessage,
        updateMessage,
        replaceMessageId,
        clear,
    };
}
