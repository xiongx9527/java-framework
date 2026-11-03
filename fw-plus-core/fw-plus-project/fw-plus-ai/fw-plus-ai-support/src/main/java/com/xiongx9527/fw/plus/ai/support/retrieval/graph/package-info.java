/**
 * 图检索契约（GraphRAG 预留，未实现）。
 *
 * <p>只保留节点 / 边模型与存储端口，多跳扩展、PageRank、实体链接等策略等真正落地时再定。
 * 放在 base 而不是 support：它是检索的基础契约，任何后端都可以实现，不依赖增强层。</p>
 */
package com.xiongx9527.fw.plus.ai.support.retrieval.graph;
