package com.xiongx9527.fw.plus.ai.retriever.listener;

import com.xiongx9527.fw.plus.ai.retriever.ContentRetriever;
import com.xiongx9527.fw.plus.ai.retriever.Query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内容检索请求上下文。
 *
 * <p><b>作用</b>：携带查询、检索器实例与附加属性。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.rag.content.retriever.listener.ContentRetrieverRequestContext}。</p>
 *
 * @author xiongxiao
 */
public class ContentRetrieverRequestContext {

    private final Query query;
    private final ContentRetriever contentRetriever;
    private final Map<Object, Object> attributes;

    private ContentRetrieverRequestContext(Builder builder) {
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
     * 查询。
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

    /** {@link ContentRetrieverRequestContext} 构建器。 */
    public static final class Builder {
        private Query query;
        private ContentRetriever contentRetriever;
        private Map<Object, Object> attributes;

        public Builder query(Query query) { this.query = query; return this; }
        public Builder contentRetriever(ContentRetriever contentRetriever) { this.contentRetriever = contentRetriever; return this; }
        public Builder attributes(Map<Object, Object> attributes) { this.attributes = attributes; return this; }
        public ContentRetrieverRequestContext build() { return new ContentRetrieverRequestContext(this); }
    }
}
