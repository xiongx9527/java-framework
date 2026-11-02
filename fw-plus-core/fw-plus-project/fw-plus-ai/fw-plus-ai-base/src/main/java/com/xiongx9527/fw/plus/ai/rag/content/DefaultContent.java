package com.xiongx9527.fw.plus.ai.rag.content;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link Content} 的默认实现。
 *
 * <p><b>作用</b>：RAG 链路的通用内容载体，持有文本片段与内容元数据。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.content.DefaultContent}
 * （三个构造器 + {@code textSegment()/metadata()}）。</p>
 *
 * @author xiongxiao
 */
public class DefaultContent implements Content {

    private final TextSegment textSegment;
    private final Map<ContentMetadata, Object> metadata;

    /**
     * 由文本片段与内容元数据创建。
     *
     * @param textSegment 文本片段
     * @param metadata    内容元数据，为 {@code null} 时使用空元数据
     */
    public DefaultContent(TextSegment textSegment, Map<ContentMetadata, Object> metadata) {
        this.textSegment = textSegment == null ? TextSegment.from("") : textSegment;
        this.metadata = metadata == null
                ? Collections.<ContentMetadata, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<ContentMetadata, Object>(metadata));
    }

    /**
     * 由纯文本创建。
     *
     * @param text 文本
     */
    public DefaultContent(String text) {
        this(TextSegment.from(text), null);
    }

    /**
     * 由文本片段创建（空元数据）。
     *
     * @param textSegment 文本片段
     */
    public DefaultContent(TextSegment textSegment) {
        this(textSegment, null);
    }

    @Override
    public TextSegment textSegment() {
        return textSegment;
    }

    @Override
    public Map<ContentMetadata, Object> metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultContent)) {
            return false;
        }
        DefaultContent that = (DefaultContent) o;
        return textSegment.equals(that.textSegment) && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return 31 * textSegment.hashCode() + metadata.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultContent{textSegment=" + textSegment + ", metadata=" + metadata + '}';
    }
}
