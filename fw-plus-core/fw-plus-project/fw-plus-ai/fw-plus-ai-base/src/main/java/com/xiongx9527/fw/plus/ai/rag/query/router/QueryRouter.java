package com.xiongx9527.fw.plus.ai.rag.query.router;

import com.xiongx9527.fw.plus.ai.retriever.ContentRetriever;
import com.xiongx9527.fw.plus.ai.retriever.Query;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** 查询路由端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.rag.query.router.QueryRouter}（按 query 选择 ContentRetriever）。 */
public interface QueryRouter {
    Collection<ContentRetriever> route(Query query);
    default CompletableFuture<Collection<ContentRetriever>> routeAsync(Query query) {
        return CompletableFuture.completedFuture(route(query));
    }
}
