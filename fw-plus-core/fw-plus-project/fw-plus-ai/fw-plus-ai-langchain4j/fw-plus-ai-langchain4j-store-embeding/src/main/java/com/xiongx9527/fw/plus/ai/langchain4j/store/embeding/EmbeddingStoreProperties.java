package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "fw-plus.ai.langchain4j.embedding-store")
public class EmbeddingStoreProperties {

    private boolean enabled = true;

    private String defaultStore;

    private Map<String, EmbeddingStoreDefinition> stores = new LinkedHashMap<String, EmbeddingStoreDefinition>();
}
