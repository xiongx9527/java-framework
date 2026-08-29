package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;

public final class RegistryTinyflowModelProvider {

	private static final String REGISTRY_TYPE = "com.xiongx9527.fw.plus.ai." + "lang" + "chain4j.adapter.model."
			+ "LangChain4jModelRegistry";
	private static final String PROVIDER_TYPE = "com.xiongx9527.fw.plus.ai.autoconfigure." + "lang" + "chain4j.tinyflow."
			+ "LangChain4jRegistryTinyflowModelProvider";

	private RegistryTinyflowModelProvider() {
	}

	public static TinyflowModelProvider create(AiModelRegistry modelRegistry, ApplicationContext applicationContext) {
		try {
			Class<?> registryType = Class.forName(REGISTRY_TYPE, false,
					RegistryTinyflowModelProvider.class.getClassLoader());
			Object registry = applicationContext.getBeanProvider(registryType).getIfAvailable();
			Class<?> providerType = Class.forName(PROVIDER_TYPE, true,
					RegistryTinyflowModelProvider.class.getClassLoader());
			Constructor<?> constructor = providerType.getConstructor(AiModelRegistry.class, registryType);
			return (TinyflowModelProvider) constructor.newInstance(modelRegistry, registry);
		} catch (Exception error) {
			throw new IllegalStateException("Unable to initialize Tinyflow model provider", error);
		}
	}
}
