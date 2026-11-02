package com.xiongx9527.fw.plus.ai.rag.aggregator;

import com.xiongx9527.fw.plus.ai.rag.content.Content;
import com.xiongx9527.fw.plus.ai.retriever.Query;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** 内容聚合端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.rag.content.aggregator.ContentAggregator}（Spring AI 等价 DocumentJoiner / Advisor）。 */
public interface ContentAggregator {
    List<Content> aggregate(Map<Query, Collection<List<Content>>> contents);
    default CompletableFuture<List<Content>> aggregateAsync(Map<Query, Collection<List<Content>>> contents) {
        return CompletableFuture.completedFuture(aggregate(contents));
    }
}
