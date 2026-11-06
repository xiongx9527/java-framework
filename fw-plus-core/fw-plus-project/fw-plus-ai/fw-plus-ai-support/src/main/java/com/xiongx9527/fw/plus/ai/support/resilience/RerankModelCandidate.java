package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;

/**
 * 重排故障转移候选：一个已解析好的重排模型端口，外加它的模型标识。
 *
 * <p>语义同 {@link LlmModelCandidate}：{@code modelId} 用于健康标记与按候选切换端点。</p>
 */
public final class RerankModelCandidate {

	public final RerankPort port;

	public final String modelId;

	public RerankModelCandidate(RerankPort port, String modelId) {
		if (port == null) {
			throw new IllegalArgumentException("rerank candidate port is required");
		}
		this.port = port;
		this.modelId = modelId;
	}
}
