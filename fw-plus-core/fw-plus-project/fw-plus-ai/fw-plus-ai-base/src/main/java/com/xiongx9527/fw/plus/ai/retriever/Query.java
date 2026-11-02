package com.xiongx9527.fw.plus.ai.retriever;

import com.xiongx9527.fw.plus.ai.rag.query.Metadata;

/**
 * 检索查询：查询文本 + 查询元数据。
 *
 * <p><b>作用</b>：检索器（ContentRetriever）、查询改写器（QueryTransformer）、路由（QueryRouter）
 * 之间传递的统一查询对象。元数据携带当前消息、对话记忆、调用上下文，供改写与路由决策。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.query.Query}
 * （{@code text()/metadata()/from(...)}）；检索条数与阈值属于检索器配置，不在查询对象上。</p>
 *
 * @author xiongxiao
 */
public class Query {

    private final String text;
    private final Metadata metadata;

    /**
     * 由文本创建（空元数据）。
     *
     * @param text 查询文本
     */
    public Query(String text) {
        this(text, new Metadata(null, null, null));
    }

    /**
     * 由文本与元数据创建。
     *
     * @param text     查询文本
     * @param metadata 查询元数据
     */
    public Query(String text, Metadata metadata) {
        this.text = text;
        this.metadata = metadata == null ? new Metadata(null, null, null) : metadata;
    }

    /**
     * 查询文本。
     *
     * @return 文本
     */
    public String text() {
        return text;
    }

    /**
     * 查询元数据。
     *
     * @return 元数据，永不为 {@code null}
     */
    public Metadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Query)) {
            return false;
        }
        Query that = (Query) o;
        return (text == null ? that.text == null : text.equals(that.text)) && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        int result = text == null ? 0 : text.hashCode();
        return 31 * result + metadata.hashCode();
    }

    @Override
    public String toString() {
        return "Query{text='" + text + "'}";
    }

    /**
     * 由文本创建。
     *
     * @param text 查询文本
     * @return 查询
     */
    public static Query from(String text) {
        return new Query(text);
    }

    /**
     * 由文本与元数据创建。
     *
     * @param text     查询文本
     * @param metadata 查询元数据
     * @return 查询
     */
    public static Query from(String text, Metadata metadata) {
        return new Query(text, metadata);
    }
}
