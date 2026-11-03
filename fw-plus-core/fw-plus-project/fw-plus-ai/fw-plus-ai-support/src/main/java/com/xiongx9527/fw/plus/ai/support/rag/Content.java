package com.xiongx9527.fw.plus.ai.support.rag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 注入用的内容片段：文本 + 元数据（来源、分数等）。
 */
public final class Content {

    private final String text;

    private final Map<String, Object> metadata;

    private Content(String text, Map<String, Object> metadata) {
        this.text = text;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
    }

    public static Content of(String text) {
        return new Content(text, new LinkedHashMap<String, Object>());
    }

    public static Content of(String text, Map<String, Object> metadata) {
        return new Content(text, metadata == null ? new LinkedHashMap<String, Object>() : metadata);
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Content{text=" + text + '}';
    }
}
