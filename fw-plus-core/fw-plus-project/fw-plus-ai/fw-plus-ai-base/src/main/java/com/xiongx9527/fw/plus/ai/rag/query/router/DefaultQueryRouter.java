package com.xiongx9527.fw.plus.ai.rag.query.router;

import com.xiongx9527.fw.plus.ai.retriever.ContentRetriever;
import com.xiongx9527.fw.plus.ai.retriever.Query;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/** 默认查询路由器，镜像 langchain4j 1.23.0 的 {@code DefaultQueryRouter}（对所有 query 返回全部 retriever）。 */
public class DefaultQueryRouter implements QueryRouter {
    private final Collection<ContentRetriever> retrievers;
    public DefaultQueryRouter(ContentRetriever... retrievers) { this(Arrays.asList(retrievers)); }
    public DefaultQueryRouter(Collection<ContentRetriever> retrievers) {
        this.retrievers = retrievers == null ? Collections.<ContentRetriever>emptyList() : retrievers;
    }
    @Override public Collection<ContentRetriever> route(Query query) { return retrievers; }
}
