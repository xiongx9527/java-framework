package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import dev.tinyflow.core.llm.LlmManager;
import dev.tinyflow.core.llm.LlmProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class TinyflowLlmLifecycle implements InitializingBean, DisposableBean {

	private final LlmProvider provider;
	private boolean registered;

	public TinyflowLlmLifecycle(LlmProvider provider) {
		this.provider = provider;
	}

	@Override
	public synchronized void afterPropertiesSet() {
		if (!registered) {
			LlmManager.getInstance().registerProvider(provider);
			registered = true;
		}
	}

	@Override
	public synchronized void destroy() {
		if (registered) {
			LlmManager.getInstance().removeProvider(provider);
			registered = false;
		}
	}
}
