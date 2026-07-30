package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import java.util.List;

public interface LangChain4jModelFactory {

    LangChain4jModel create(LangChain4jModelDefinition definition, List<LangChain4jModelProviderAdapter> adapters);
}
