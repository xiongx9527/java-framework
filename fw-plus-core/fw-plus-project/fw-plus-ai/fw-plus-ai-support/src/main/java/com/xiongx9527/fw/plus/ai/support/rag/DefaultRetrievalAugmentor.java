package com.xiongx9527.fw.plus.ai.support.rag;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认检索增强：改写（可选）→ 路由（可选）→ 多路检索 → 聚合 → 注入。
 *
 * <p>无命中时返回空内容与「原样消息」：不注入、不改变提示词，避免模型把空资料当成结论。</p>
 */
public class DefaultRetrievalAugmentor implements RetrievalAugmentor {

    private final List<Retriever<Document>> retrievers;

    private final ContentAggregator aggregator;

    private final ContentInjector injector;

    private final QueryTransformer transformer;

    private final QueryRouter router;

    public DefaultRetrievalAugmentor(List<Retriever<Document>> retrievers) {
        this(retrievers, new DefaultContentAggregator(), new DefaultContentInjector(), null, null);
    }

    public DefaultRetrievalAugmentor(List<Retriever<Document>> retrievers, ContentAggregator aggregator,
                                     ContentInjector injector, QueryTransformer transformer, QueryRouter router) {
        if (retrievers == null || retrievers.isEmpty()) {
            throw new IllegalArgumentException("at least one retriever is required");
        }
        this.retrievers = retrievers;
        this.aggregator = aggregator == null ? new DefaultContentAggregator() : aggregator;
        this.injector = injector == null ? new DefaultContentInjector() : injector;
        this.transformer = transformer;
        this.router = router;
    }

    @Override
    public AugmentationResult augment(AugmentationRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return AugmentationResult.of(null, request == null ? null : request.getMessages());
        }
        List<String> queries = transformer == null
                ? Collections.singletonList(request.getQuery()) : transformer.transform(request.getQuery());
        if (queries == null || queries.isEmpty()) {
            queries = Collections.singletonList(request.getQuery());
        }
        List<Retriever<Document>> selected = router == null ? retrievers : router.route(request.getQuery());
        if (selected == null || selected.isEmpty()) {
            selected = retrievers;
        }
        List<List<Content>> perQuery = new ArrayList<List<Content>>();
        for (String query : queries) {
            List<Content> contents = new ArrayList<Content>();
            for (Retriever<Document> retriever : selected) {
                List<Retrieved<Document>> hits = retriever.retrieve(toQuery(query, request));
                if (hits == null) {
                    continue;
                }
                for (Retrieved<Document> hit : hits) {
                    if (hit == null || hit.getItem() == null) {
                        continue;
                    }
                    Map<String, Object> metadata = new LinkedHashMap<String, Object>(hit.getItem().getMetadata());
                    metadata.put("score", hit.getScore());
                    metadata.put("id", hit.getItem().getId());
                    contents.add(Content.of(hit.getItem().getContent(), metadata));
                }
            }
            perQuery.add(contents);
        }
        List<Content> aggregated = aggregator.aggregate(perQuery);
        List<ChatMessage> messages = injector.inject(aggregated, request.getMessages());
        return AugmentationResult.of(aggregated, messages);
    }

    private RetrievalQuery toQuery(String query, AugmentationRequest request) {
        RetrievalQuery.Builder builder = RetrievalQuery.builder().text(query);
        Object collection = request.getParameters().get("collection");
        if (collection != null) {
            builder.collection(String.valueOf(collection));
        }
        Object topK = request.getParameters().get("topK");
        if (topK instanceof Number) {
            builder.topK(((Number) topK).intValue());
        }
        return builder.build();
    }
}
