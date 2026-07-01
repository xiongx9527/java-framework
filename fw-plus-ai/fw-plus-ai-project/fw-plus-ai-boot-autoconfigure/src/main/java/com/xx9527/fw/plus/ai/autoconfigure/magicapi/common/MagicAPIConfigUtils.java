package com.xx9527.fw.plus.ai.autoconfigure.magicapi.common;

public class MagicAPIConfigUtils extends com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils{

    private static final String AI_PREFIX = "magic.api.";

    public static String getLangchain4jConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(AI_PREFIX + "langchain4j.", key, configKey, defaultValue);
    }

    public static String getMilvusConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(AI_PREFIX + "milvus.", key, configKey, defaultValue);
    }

    public static String getQdrantConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(AI_PREFIX + "qdrant.", key, configKey, defaultValue);
    }

    public static String getWeaviateConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(AI_PREFIX + "weaviate.", key, configKey, defaultValue);
    }
}