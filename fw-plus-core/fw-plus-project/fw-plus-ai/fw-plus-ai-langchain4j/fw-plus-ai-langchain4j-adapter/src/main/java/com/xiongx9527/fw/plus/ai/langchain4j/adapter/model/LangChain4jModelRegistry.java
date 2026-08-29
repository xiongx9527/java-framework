package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import java.util.Set;

public interface LangChain4jModelRegistry {

    LangChain4jModel get(String key);

    LangChain4jModel getDefault();

    default String getDefaultKey() {
        return null;
    }

    boolean contains(String key);

    Set<String> keys();
}
