package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;

/**
 * 会话级上下文来源：直读已维护好的会话摘要（读时**不调 LLM**，便宜）。
 *
 * <p>摘要以会话键（{@link ContextRequest#getSessionKey()}）为作用域——新会话以空摘要开始，
 * 不读取其它会话的摘要。无摘要或原文为空时返回空串，由注入器跳过。</p>
 */
public class SummaryContextProvider implements ContextProvider {

	private final SummaryStore store;

	public SummaryContextProvider(SummaryStore store) {
		this.store = store;
	}

	@Override
	public String provide(ContextRequest request) {
		if (request == null || request.getSessionKey() == null) {
			return "";
		}
		return store.get(request.getSessionKey()).getSummary();
	}

}