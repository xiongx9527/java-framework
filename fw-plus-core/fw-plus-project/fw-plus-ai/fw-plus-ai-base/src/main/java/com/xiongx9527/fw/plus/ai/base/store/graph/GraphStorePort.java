package com.xiongx9527.fw.plus.ai.base.store.graph;

import com.xiongx9527.fw.plus.ai.base.store.RecordStore;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 图存储端口（GraphRAG 基础契约）。
 *
 * <p>由 adapter 用 Neo4j / Postgres / 内存等实现；多跳扩展、PageRank、实体链接等策略留待真正实现时再定。
 * 放在 base 而不是 support：它是检索的基础契约，任何后端都可以实现，不依赖增强层。</p>
 *
 * <p>图库支持按 id 随机读，因此继承 {@link RecordStore}，从而与向量库等一起登记到
 * {@link com.xiongx9527.fw.plus.ai.base.store.StoreRegistry} 统一取用；
 * 需要 CRUD 时按 {@code RecordStore} 类型取用即可。</p>
 */
public interface GraphStorePort extends RecordStore {

    /** 连通性探测；不可用时返回 {@code false}。 */
    @Override
    boolean health();

    /** 取节点的出边；{@code edgeTypes} 为空表示不过滤类型。 */
    List<GraphEdge> neighbors(String nodeId, Set<String> edgeTypes);

    /** 批量按 id 取节点（缺失的 id 不出现在结果中）。 */
    List<GraphNode> nodes(Collection<String> ids);

    /** 按文本召回节点（标签 / 属性包含匹配），最多 {@code topK} 个。 */
    List<GraphNode> searchNodes(String text, int topK);
}
