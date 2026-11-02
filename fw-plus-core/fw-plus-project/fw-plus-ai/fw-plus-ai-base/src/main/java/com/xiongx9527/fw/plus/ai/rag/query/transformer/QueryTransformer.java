package com.xiongx9527.fw.plus.ai.rag.query.transformer;

import com.xiongx9527.fw.plus.ai.retriever.Query;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** 查询改写端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.rag.query.transformer.QueryTransformer}（Spring AI 等价 QueryTransformer）。 */
public interface QueryTransformer {
    Collection<Query> transform(Query query);
    default CompletableFuture<Collection<Query>> transformAsync(Query query) {
        return CompletableFuture.completedFuture(transform(query));
    }
}
