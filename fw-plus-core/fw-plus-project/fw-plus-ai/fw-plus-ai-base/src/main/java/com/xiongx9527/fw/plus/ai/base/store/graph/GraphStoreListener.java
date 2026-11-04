package com.xiongx9527.fw.plus.ai.base.store.graph;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * 图存储事件：取邻居 / 取节点 / 检索节点。
 *
 * <p>检索事件带命中数而不带命中内容——内容可能很大，监听器只做计数、耗时、失败这类观测。</p>
 */
public interface GraphStoreListener extends AiListener {

    /** 取邻居完成。 */
    default void onNeighbors(String nodeId, int edgeCount) {
    }

    /** 批量取节点完成。 */
    default void onNodes(int requested, int hitCount) {
    }

    /** 节点检索完成。 */
    default void onSearch(int topK, int hitCount) {
    }
}
