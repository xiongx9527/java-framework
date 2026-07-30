package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

public interface LangChain4jModelProviderAdapter {

    boolean supports(LangChain4jModelDefinition definition);

    LangChain4jModel create(LangChain4jModelDefinition definition);
}
