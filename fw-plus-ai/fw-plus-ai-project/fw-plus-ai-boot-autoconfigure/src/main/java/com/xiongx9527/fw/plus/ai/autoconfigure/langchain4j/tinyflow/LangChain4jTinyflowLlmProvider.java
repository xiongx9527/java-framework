package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyflow;

import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.llm.LlmProvider;
import dev.tinyflow.langchain4j.provider.Langchain4jLlm;

public class LangChain4jTinyflowLlmProvider implements LlmProvider {

	private final TinyflowModelProvider modelProvider;

	public LangChain4jTinyflowLlmProvider(TinyflowModelProvider modelProvider) {
		this.modelProvider = modelProvider;
	}

	@Override
	public Llm getChatModel(Object key) {
		if (key != null && !(key instanceof String)) {
			return null;
		}
		return new Langchain4jLlm(modelProvider.getChatModel((String) key));
	}
}
