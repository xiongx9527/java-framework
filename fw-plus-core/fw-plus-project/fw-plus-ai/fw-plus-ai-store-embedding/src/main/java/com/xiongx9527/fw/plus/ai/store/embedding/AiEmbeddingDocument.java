package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AiEmbeddingDocument {

    private final String id;

    private final String text;

    private final Map<String, Object> metadata;

    private final AiEmbeddingVector vector;

    public AiEmbeddingDocument(String id, String text, Map<String, Object> metadata, AiEmbeddingVector vector) {
        this.id = id;
        this.text = text;
        this.metadata = immutableCopy(metadata);
        this.vector = vector;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public AiEmbeddingVector getVector() {
        return vector;
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(source));
    }
}
