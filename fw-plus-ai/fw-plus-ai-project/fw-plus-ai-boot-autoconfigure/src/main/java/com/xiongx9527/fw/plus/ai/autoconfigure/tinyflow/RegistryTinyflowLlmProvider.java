package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import dev.tinyflow.core.llm.LlmProvider;

import java.lang.reflect.Constructor;

public final class RegistryTinyflowLlmProvider {

	private static final String PROVIDER_TYPE = "com.xiongx9527.fw.plus.ai.autoconfigure."
			+ "lang" + "chain4j.tinyflow.LangChain4jTinyflowLlmProvider";

	private RegistryTinyflowLlmProvider() {
	}

	public static LlmProvider create(TinyflowModelProvider modelProvider) {
		try {
			Class<?> providerType = Class.forName(PROVIDER_TYPE, true,
					RegistryTinyflowLlmProvider.class.getClassLoader());
			Constructor<?> constructor = providerType.getConstructor(TinyflowModelProvider.class);
			return (LlmProvider) constructor.newInstance(modelProvider);
		} catch (Exception error) {
			throw new IllegalStateException("Unable to initialize Tinyflow LLM provider", error);
		}
	}
}
