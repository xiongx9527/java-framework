package com.xiongx9527.fw.plus.ai.langchain4j.model;

import java.util.Set;

public interface LangChain4jModelRegistry {

    LangChain4jModel get(String key);

    LangChain4jModel getDefault();

    boolean contains(String key);

    Set<String> keys();
}
