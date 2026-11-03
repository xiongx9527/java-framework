package com.xiongx9527.fw.plus.ai.base.store;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * 向量存储事件：写入 / 删除 / 检索。
 *
 * <p>检索事件带命中数而不带命中内容——内容可能很大，需要时由调用方自己取，
 * 监听器只做计数、耗时、失败这类观测。</p>
 */
public interface VectorStoreListener extends AiListener {

    /** 单条写入完成。 */
    default void onUpsert(String collection, String id) {
    }

    /** 单条删除完成。 */
    default void onDelete(String collection, String id) {
    }

    /** 检索完成。 */
    default void onSearch(String collection, int topK, int hitCount) {
    }
}
