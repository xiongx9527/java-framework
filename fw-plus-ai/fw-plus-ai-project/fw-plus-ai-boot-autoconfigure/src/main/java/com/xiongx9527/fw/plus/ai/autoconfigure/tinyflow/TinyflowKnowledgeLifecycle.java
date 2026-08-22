package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeManagerAdapter;
import dev.tinyflow.core.knowledge.KnowledgeManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class TinyflowKnowledgeLifecycle implements InitializingBean, DisposableBean {

	private final TinyflowKnowledgeManagerAdapter adapter;
	private boolean registered;

	public TinyflowKnowledgeLifecycle(TinyflowKnowledgeManagerAdapter adapter) {
		if (adapter == null) {
			throw new IllegalArgumentException("Tinyflow knowledge adapter must not be null");
		}
		this.adapter = adapter;
	}

	@Override
	public synchronized void afterPropertiesSet() {
		if (!registered) {
			KnowledgeManager.getInstance().registerProvider(adapter);
			registered = true;
		}
	}

	@Override
	public synchronized void destroy() {
		if (registered) {
			KnowledgeManager.getInstance().removeProvider(adapter);
			registered = false;
		}
	}
}
