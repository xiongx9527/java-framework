package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class EmbeddingStoreProperties {

    private boolean enabled = true;

    private String defaultStore;

    private Map<String, EmbeddingStoreDefinition> stores = new LinkedHashMap<String, EmbeddingStoreDefinition>();
}
