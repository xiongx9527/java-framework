package com.xiongx9527.fw.plus.ai.data.segment;

import com.xiongx9527.fw.plus.ai.data.document.Metadata;

/**
 * 文本片段：文本 + 元数据，是向量化与检索的最小单位。
 *
 * <p><b>作用</b>：文档切分后的产物；{@code EmbeddingStore} 存它、检索返回它、
 * RAG 内容（{@code rag.content.Content}）包它。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.segment.TextSegment}
 * （{@code text()/metadata()/from/textSegment}）。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #metadata()} 永不为 {@code null}。</p>
 *
 * @author xiongxiao
 */
public class TextSegment {

    private final String text;
    private final Metadata metadata;

    /**
     * 构造片段。
     *
     * @param text     文本
     * @param metadata 元数据，为 {@code null} 时使用空元数据
     */
    public TextSegment(String text, Metadata metadata) {
        this.text = text;
        this.metadata = metadata == null ? new Metadata() : metadata;
    }

    /**
     * 文本。
     *
     * @return 文本
     */
    public String text() {
        return text;
    }

    /**
     * 元数据。
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
        if (!(o instanceof TextSegment)) {
            return false;
        }
        TextSegment that = (TextSegment) o;
        if (text == null ? that.text != null : !text.equals(that.text)) {
            return false;
        }
        return metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        int result = text == null ? 0 : text.hashCode();
        return 31 * result + metadata.hashCode();
    }

    @Override
    public String toString() {
        return "TextSegment{text='" + text + "', metadata=" + metadata + '}';
    }

    /**
     * 由文本创建（空元数据）。
     *
     * @param text 文本
     * @return 片段
     */
    public static TextSegment from(String text) {
        return new TextSegment(text, new Metadata());
    }

    /**
     * 由文本与元数据创建。
     *
     * @param text     文本
     * @param metadata 元数据
     * @return 片段
     */
    public static TextSegment from(String text, Metadata metadata) {
        return new TextSegment(text, metadata);
    }

    /**
     * 由文本创建（{@link #from(String)} 的别名）。
     *
     * @param text 文本
     * @return 片段
     */
    public static TextSegment textSegment(String text) {
        return from(text);
    }

    /**
     * 由文本与元数据创建（{@link #from(String, Metadata)} 的别名）。
     *
     * @param text     文本
     * @param metadata 元数据
     * @return 片段
     */
    public static TextSegment textSegment(String text, Metadata metadata) {
        return from(text, metadata);
    }
}
