package com.xiongx9527.fw.plus.ai.retriever.listener;

import com.xiongx9527.fw.plus.ai.retriever.ContentRetriever;
import com.xiongx9527.fw.plus.ai.retriever.Query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内容检索异常上下文。
 *
 * <p><b>作用</b>：携带失败原因与原始查询，供降级决策（跳过检索 / 换检索器）与告警。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.rag.content.retriever.listener.ContentRetrieverErrorContext}。</p>
 *
 * @author xiongxiao
 */
public class ContentRetrieverErrorContext {

    private final Throwable error;
    private final Query query;
    private final ContentRetriever contentRetriever;
    private final Map<Object, Object> attributes;

    private ContentRetrieverErrorContext(Builder builder) {
        this.error = builder.error;
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
     * 异常。
     *
     * @return 异常
     */
    public Throwable error() {
        return error;
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

    /** {@link ContentRetrieverErrorContext} 构建器。 */
    public static final class Builder {
        private Throwable error;
        private Query query;
        private ContentRetriever contentRetriever;
        private Map<Object, Object> attributes;

        public Builder error(Throwable error) { this.error = error; return this; }
        public Builder query(Query query) { this.query = query; return this; }
        public Builder contentRetriever(ContentRetriever contentRetriever) { this.contentRetriever = contentRetriever; return this; }
        public Builder attributes(Map<Object, Object> attributes) { this.attributes = attributes; return this; }
        public ContentRetrieverErrorContext build() { return new ContentRetrieverErrorContext(this); }
    }
}
