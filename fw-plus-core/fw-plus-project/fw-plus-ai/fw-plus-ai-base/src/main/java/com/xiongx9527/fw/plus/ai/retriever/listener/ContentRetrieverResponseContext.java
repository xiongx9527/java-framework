package com.xiongx9527.fw.plus.ai.retriever.listener;

import com.xiongx9527.fw.plus.ai.rag.content.Content;
import com.xiongx9527.fw.plus.ai.retriever.ContentRetriever;
import com.xiongx9527.fw.plus.ai.retriever.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容检索响应上下文。
 *
 * <p><b>作用</b>：携带召回结果与原始查询，供质量分析与审计。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.rag.content.retriever.listener.ContentRetrieverResponseContext}。</p>
 *
 * @author xiongxiao
 */
public class ContentRetrieverResponseContext {

    private final List<Content> contents;
    private final Query query;
    private final ContentRetriever contentRetriever;
    private final Map<Object, Object> attributes;

    private ContentRetrieverResponseContext(Builder builder) {
        this.contents = builder.contents == null
                ? Collections.<Content>emptyList()
                : Collections.unmodifiableList(new ArrayList<Content>(builder.contents));
        this.query = builder.query;
        this.contentRetriever = builder.contentRetriever;
        this.attributes = builder.attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(builder.attributes));
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 召回内容。
     *
     * @return 只读内容列表
     */
    public List<Content> contents() {
        return contents;
    }

    /**
     * 原始查询。
     *
     * @return 查询
     */
    public Query query() {
        return query;
    }

    /**
     * 检索器。
     *
     * @return 检索器
     */
    public ContentRetriever contentRetriever() {
        return contentRetriever;
    }

    /**
     * 附加属性。
     *
     * @return 只读属性
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }

    /** {@link ContentRetrieverResponseContext} 构建器。 */
    public static final class Builder {
        private List<Content> contents;
        private Query query;
        private ContentRetriever contentRetriever;
        private Map<Object, Object> attributes;

        public Builder contents(List<Content> contents) { this.contents = contents; return this; }
        public Builder query(Query query) { this.query = query; return this; }
        public Builder contentRetriever(ContentRetriever contentRetriever) { this.contentRetriever = contentRetriever; return this; }
        public Builder attributes(Map<Object, Object> attributes) { this.attributes = attributes; return this; }
        public ContentRetrieverResponseContext build() { return new ContentRetrieverResponseContext(this); }
    }
}
