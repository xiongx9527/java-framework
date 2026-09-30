package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

/** 模型资源的类型常量：同时作为 datasource type 与 storage folder。 */
public enum Langchain4jModelTypes {
    CHAT_MODEL("langchain4j-chat-model"),
    STREAMING_CHAT_MODEL("langchain4j-streaming-chat-model"),
    EMBEDDING_MODEL("langchain4j-embedding-model"),
    SCORING_MODEL("langchain4j-scoring-model"),
    TOKENIZER("langchain4j-tokenizer"),
    EMBEDDING_STORE("langchain4j-embedding-store");

    private final String type;

    Langchain4jModelTypes(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public static Langchain4jModelTypes of(String type) {
        for (Langchain4jModelTypes t : values()) {
            if (t.type.equals(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("不支持的资源类型: " + type);
    }
}
