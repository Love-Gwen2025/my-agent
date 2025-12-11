package com.couple.agent.utils;

import cn.hutool.core.util.StrUtil;
import com.couple.agent.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TreeBuildUtil {
    @SafeVarargs
    public static <T> List<T> buildRecursiveTree(
            List<T> rawList,
            Function<T, Long> getId,
            Function<T, Long> getParentId,
            BiConsumer<T, List<T>> setChildren,
            String keyword,
            Function<T, String>... matchers) {

        // 1. 空值检查
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 校验树结构
        validateTreeStructure(rawList, getId, getParentId);

        // 2. 数据预处理：构建ID到对象的映射
        Map<Long, T> nodeMap = rawList.stream()
                .collect(Collectors.toMap(getId, item -> item, (existing, replacement) -> existing));

        // 3. 关键词过滤：找到所有匹配关键词的节点
        List<T> matchedNodes = new ArrayList<>();
        if (StrUtil.isNotBlank(keyword) && matchers != null && matchers.length > 0) {
            // 使用增强for循环，避免复杂的stream链式调用
            for (T item : rawList) {
                for (Function<T, String> matcher : matchers) {
                    if (StrUtil.containsIgnoreCase(matcher.apply(item), keyword)) {
                        matchedNodes.add(item);
                        break; // 只要有一个匹配器匹配即可
                    }
                }
            }
        } else {
            // 无关键词过滤时，所有节点都视为匹配
            matchedNodes.addAll(rawList);
        }

        // 4. 找到所有需要保留的节点（匹配节点 + 它们的祖先节点）
        Set<Long> requiredNodeIds = new HashSet<>();

        // 遍历所有匹配的节点，向上追溯，将所有祖先节点加入到 requiredNodeIds
        for (T matchedNode : matchedNodes) {
            Long currentNodeId = getId.apply(matchedNode);
            Long currentParentId = getParentId.apply(matchedNode);

            // 1. 先加入匹配节点本身
            requiredNodeIds.add(currentNodeId);

            // 2. 向上追溯祖先节点
            while (currentParentId != null && currentParentId != 0) {
                // 如果祖先节点已经被记录，则停止追溯（因为它的所有祖先也必然已被记录）
                if (requiredNodeIds.contains(currentParentId)) {
                    break;
                }

                T parentNode = nodeMap.get(currentParentId);
                if (parentNode == null) {
                    // 父节点不存在，可能在校验时已经被捕捉到（父节点不存在），这里跳出即可
                    break;
                }

                requiredNodeIds.add(currentParentId);
                // 继续向上追溯
                currentParentId = getParentId.apply(parentNode);
            }
        }

        // 5. 最终的过滤列表（包含匹配节点及其所有祖先）
        List<T> filteredList = rawList.stream()
                .filter(item -> requiredNodeIds.contains(getId.apply(item)))
                .collect(Collectors.toList());

        // 6. 查找根节点（parentId为null或0，并且在过滤列表中）
        List<T> roots = filteredList.stream()
                .filter(item -> {
                    Long parentId = getParentId.apply(item);
                    return parentId == null || parentId == 0;
                })
                .collect(Collectors.toList());

        // 7. 按父节点分组子节点
        Map<Long, List<T>> childrenMap = filteredList.stream()
                .filter(item -> {
                    Long parentId = getParentId.apply(item);
                    return parentId != null && parentId != 0;
                })
                .collect(Collectors.groupingBy(getParentId));
        // 注意：原代码中的第7步“补充被过滤掉的必要父节点”现在不再需要，因为我们已经在第4步中完成了对所有祖先节点的保留。

        // 8. 递归构建树形结构
        roots.forEach(root -> buildTreeRecursively(
                root,
                childrenMap,
                getId,
                setChildren
        ));

        // 去除null节点
        roots.removeIf(Objects::isNull);
        return roots;
    }


    /**
     * 层序遍历展开树形结构
     *
     * @param roots       树的根节点列表
     * @param getChildren 获取子节点的方法
     * @param <T>         节点类型
     * @return 扁平化后的列表，按层序遍历顺序
     */
    public static <T> List<T> flattenTree(
            List<T> roots,
            Function<T, List<T>> getChildren) {

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        Queue<T> queue = new LinkedList<>(roots);

        while (!queue.isEmpty()) {
            T node = queue.poll();
            result.add(node);

            List<T> children = getChildren.apply(node);
            if (children != null && !children.isEmpty()) {
                queue.addAll(children);
            }
        }

        return result;
    }

    /**
     * 递归构建子树
     */
    private static <T> void buildTreeRecursively(
            T currentNode,
            Map<Long, List<T>> childrenMap,
            Function<T, Long> getId,
            BiConsumer<T, List<T>> setChildren) {

        Long currentId = getId.apply(currentNode);
        List<T> children = childrenMap.getOrDefault(currentId, new ArrayList<>());

        // 处理每个子节点
        children.forEach(child -> buildTreeRecursively(
                child,
                childrenMap,
                getId,
                setChildren
        ));

        setChildren.accept(currentNode, children);
    }

    /**
     * 校验节点列表是否存在循环引用或ID冲突
     *
     * @param nodes       节点列表
     * @param getId       获取节点ID的方法
     * @param getParentId 获取父节点ID的方法
     * @param <T>         节点类型
     * @throws IllegalArgumentException 如果存在以下情况：
     *                                  1. 节点ID重复
     *                                  3. 存在循环引用
     *                                  4. 父节点不存在
     *                                  循环引用示例：若不处理会导致上面递归调用无限循环
     *                                  List<TreeNode> nodes = Arrays.asList(
     *                                  new TreeNode(1, null),   // 根节点 (parentId=null)
     *                                  new TreeNode(2, 1),      // 父节点是1
     *                                  new TreeNode(3, 2),      // 父节点是2
     *                                  new TreeNode(1, 3)       // 父节点是3 → 循环指向根节点1
     *                                  );
     */
    public static <T> void validateTreeStructure(
            List<T> nodes,
            Function<T, Long> getId,
            Function<T, Long> getParentId) {

        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 1. 检测重复ID
        Map<Long, Long> idToParentId = nodes.stream()
                .filter(node -> getParentId.apply(node) != null)
                .collect(Collectors.toMap(
                        getId,
                        getParentId,
                        (id1, id2) -> {
                            throw new BizException(null,"重复的节点ID: " + id1);
                        }
                ));

        // 3. 检测循环引用
        idToParentId.forEach((id, parentId) -> {
            Set<Long> path = new HashSet<>();
            Long currentId = id;
            while (currentId != null) {
                if (!path.add(currentId)) {
                    throw new BizException(null,"检测到循环引用路径: " + path);
                }
                currentId = idToParentId.get(currentId);
            }
        });
    }
}
