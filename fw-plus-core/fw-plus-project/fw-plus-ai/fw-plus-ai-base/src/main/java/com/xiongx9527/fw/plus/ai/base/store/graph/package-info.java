/**
 * 图存储契约（GraphRAG 基础契约）。
 *
 * <p>由 adapter 用 Neo4j / Postgres / 内存等实现；多跳扩展、PageRank、实体链接等策略留待真正实现时再定。
 * 放在 base 而不是 support：它是检索的基础契约，任何后端都可以实现，不依赖增强层。</p>
 */
package com.xiongx9527.fw.plus.ai.base.store.graph;
