package com.xiongx9527.fw.plus.ai.askdata.model;

/**
 * 模型路由 SPI。
 *
 * <p>默认方法只处理厂商无关的 key 优先级和失败降级；能力注册表、Spring AI、AgentScope
 * 以及 Java 17 运行时客户端均由外部适配层实现，避免 Core 绑定任一模型 SDK。</p>
 */
public interface AskDataModelRouter {

	default AskDataModelResult route(AskDataModelRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request 不能为空");
		}
		return routeAfter(request, null);
	}

	default AskDataModelResult fallback(AskDataModelRequest request, AskDataModelResult failure) {
		if (request == null || failure == null) {
			throw new IllegalArgumentException("request 和 failure 不能为空");
		}
		if (failure.isRouted() || !failure.isRetryable()) {
			return failure;
		}
		return routeAfter(request, failure.getModelKey());
	}

	default AskDataModelResult routeAfter(AskDataModelRequest request, String failedModelKey) {
		String[] modelKeys = { request.getRequestModelKey(), request.getProjectKey(), request.getDatasourceKey(),
				request.getAgentTaskKey(), request.getDefaultKey() };
		AskDataModelRoutePolicy[] policies = AskDataModelRoutePolicy.values();
		boolean passedFailure = failedModelKey == null;
		for (int index = 0; index < modelKeys.length; index++) {
			if (!passedFailure) {
				if (failedModelKey.equals(modelKeys[index])) {
					passedFailure = true;
				}
				continue;
			}
			if (modelKeys[index].length() > 0) {
				return AskDataModelResult.routed(modelKeys[index], policies[index]);
			}
		}
		throw new IllegalArgumentException("default 模型 key 不能为空，或不存在可用于基础设施故障降级的模型 key");
	}
}
