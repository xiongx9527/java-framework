package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;

/**
 * 故障转移候选：一个已解析好的对话模型端口，外加它的模型标识。
 *
 * <p>{@code modelId} 同时用于：</p>
 * <ul>
 *   <li>{@code ModelHealthPort} 的健康标记（按模型维度熔断 / 降权）；</li>
 *   <li>跨端点切换时把 {@code request.model} 重写为该候选自身的模型名——
 *       不同候选往往是不同端点（apiDomain/apiKey 各异），沿用主模型名会打到不存在的模型。</li>
 * </ul>
 */
public final class LlmModelCandidate {

	public final LlmPort port;

	public final String modelId;

	public LlmModelCandidate(LlmPort port, String modelId) {
		if (port == null) {
			throw new IllegalArgumentException("llm candidate port is required");
		}
		this.port = port;
		this.modelId = modelId;
	}
}
