package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class EmbeddingStoreDefinition {

    private String key;

    private String provider;

    private boolean enabled = true;

    private Integer dimension;

    private List<String> metadataKeys = new ArrayList<String>();

    private Map<String, Object> properties = new LinkedHashMap<String, Object>();
}
