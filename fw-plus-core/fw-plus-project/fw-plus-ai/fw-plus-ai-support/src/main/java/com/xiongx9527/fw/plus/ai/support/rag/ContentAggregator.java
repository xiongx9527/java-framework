package com.xiongx9527.fw.plus.ai.support.rag;

import java.util.List;

/**
 * 内容聚合：多路检索结果 → 一份去重后的内容列表。
 */
public interface ContentAggregator {

    List<Content> aggregate(List<List<Content>> contents);
}
