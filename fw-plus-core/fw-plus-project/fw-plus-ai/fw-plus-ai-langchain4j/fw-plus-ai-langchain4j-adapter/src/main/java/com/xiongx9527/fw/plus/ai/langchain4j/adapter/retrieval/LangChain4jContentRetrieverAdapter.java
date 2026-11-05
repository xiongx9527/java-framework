package com.xiongx9527.fw.plus.ai.langchain4j.adapter.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.DocumentRetriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jStorePort;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 LangChain4j 0.35 的 {@code ContentRetriever.retrieve(Query)} 适配成 base 的
 * {@link DocumentRetriever}（即 {@code Retriever<Document>}），便于复用既有 RAG 管线。
 *
 * <p><b>语义差异</b>：上游 {@link Content} 不带相关度分数，因此所有命中统一使用
 * 构造时给定的 {@code defaultScore}（默认 1.0）；{@code scoreThreshold} 作用在该固定分数上。
 * 上游 {@code Query} 也无法承载 {@code collection / filters / candidateIds}，这些条件被忽略——
 * 需要过滤与集合语义时应走 {@link LangChain4jStorePort} + base 的
 * {@code VectorRetriever}。</p>
 */
public class LangChain4jContentRetrieverAdapter implements DocumentRetriever {

    private final ContentRetriever retriever;

    private final double defaultScore;

    private final String source;

    public LangChain4jContentRetrieverAdapter(ContentRetriever retriever) {
        this(retriever, 1.0, "langchain4j");
    }

    public LangChain4jContentRetrieverAdapter(ContentRetriever retriever, double defaultScore,
                                             String source) {
        if (retriever == null) {
            throw new IllegalArgumentException("content retriever is required");
        }
        this.retriever = retriever;
        this.defaultScore = defaultScore;
        this.source = source;
    }

    @Override
    public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
        List<Retrieved<Document>> out = new ArrayList<Retrieved<Document>>();
        if (query == null || query.getText() == null || query.getText().trim().isEmpty()) {
            return out;
        }
        if (defaultScore < query.getScoreThreshold()) {
            return out;
        }
        List<Content> contents;
        try {
            contents = retriever.retrieve(Query.from(query.getText()));
        } catch (RuntimeException e) {
            return out;
        }
        if (contents == null) {
            return out;
        }
        int limit = query.getTopN() > 0 ? query.getTopN() : contents.size();
        for (Content content : contents) {
            if (out.size() >= limit) {
                break;
            }
            if (content == null || content.textSegment() == null) {
                continue;
            }
            out.add(Retrieved.of(toDocument(content.textSegment()), defaultScore, source));
        }
        return out;
    }

    private static Document toDocument(TextSegment segment) {
        Metadata metadata = segment.metadata();
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        String id = null;
        if (metadata != null) {
            values.putAll(metadata.toMap());
            id = metadata.get(LangChain4jStorePort.KEY_ID);
            if (id == null) {
                id = metadata.get("id");
            }
            values.remove(LangChain4jStorePort.KEY_ID);
            values.remove(LangChain4jStorePort.KEY_COLLECTION);
        }
        return Document.builder().id(id).content(segment.text()).metadata(values).build();
    }
}
