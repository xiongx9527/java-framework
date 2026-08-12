package com.xiongx9527.fw.plus.ai.askdata.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型路由 SPI。
 *
 * <p>默认方法只处理厂商无关的 key 优先级和失败降级；能力注册表、Spring AI、AgentScope
 * 以及 Java 17 运行时客户端均由外部适配层实现，避免 Core 绑定任一模型 SDK。</p>
 */
public interface AskDataModelRouter {

	/**
	 * 按固定优先级选择首个可用模型。
	 *
	 * <p>优先级依次为请求、项目、数据源、智能体任务和默认配置。该方法仅产出可审计的 Core 路由决定，
	 * 不调用模型 SDK；Java 17 Remote Runtime 适配服务应在收到该决定后执行模型，并将执行错误映射为
	 * {@link AskDataModelResult} 的稳定错误分类。</p>
	 *
	 * @param request 含项目隔离、操作人审计上下文及候选模型 key 的请求，不能为空
	 * @return 已命中的模型及其配置层级
	 */
	default AskDataModelResult route(AskDataModelRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request 不能为空");
		}
		return routeAfter(request, null);
	}

	/**
	 * 对模型执行失败进行受控降级。
	 *
	 * <p>仅 {@link AskDataModelResult#infrastructureFailure(AskDataModelResult, String)} 产生的可重试
	 * 基础设施错误会从其保留的实际候选位置之后继续选择候选。候选链按实际模型 key 去重并保留首次出现的
	 * 路由层级，防止同一模型通过其他层级或显式兜底配置被重复调用。
	 * 鉴权、参数、策略等不可重试拒绝原样返回。Java 17 Remote Runtime 的网络或 SDK 异常必须先在
	 * 适配边界分类，调用方与审计记录不得依赖其内部异常类型。</p>
	 *
	 * @param request 原始模型路由请求，不能为空
	 * @param failure 已分类的模型失败结果，不能为空
	 * @return 下一个候选的路由结果，或原不可重试失败结果
	 */
	default AskDataModelResult fallback(AskDataModelRequest request, AskDataModelResult failure) {
		if (request == null || failure == null) {
			throw new IllegalArgumentException("request 和 failure 不能为空");
		}
		if (failure.isRouted() || !failure.isRetryable()) {
			return failure;
		}
		List<Candidate> candidates = candidates(request);
		if (failure.getCandidateIndex() >= 0) {
			return routeFrom(candidates, failure.getCandidateIndex() + 1);
		}
		if (failure.getRoutePolicy() != null) {
			for (int index = 0; index < candidates.size(); index++) {
				Candidate candidate = candidates.get(index);
				if (candidate.policy == failure.getRoutePolicy()
						&& candidate.modelKey.equals(failure.getModelKey())) {
					return routeFrom(candidates, index + 1);
				}
			}
		}
		return routeAfter(request, failure.getModelKey());
	}

	/**
	 * 按失败模型 key 的首次出现位置选择后继候选。
	 *
	 * <p>这是兼容旧调用方的入口，无法区分多个层级使用同一 key 的实际失败位置。新适配实现应通过
	 * {@link AskDataModelResult#infrastructureFailure(AskDataModelResult, String)} 保留路由策略后再调用
	 * {@link #fallback(AskDataModelRequest, AskDataModelResult)}。此方法不执行远程调用，也不改变 Core
	 * 项目隔离、操作人审计或 Java 17 Remote Runtime 的职责边界。</p>
	 *
	 * @param request 原始模型路由请求，不能为空
	 * @param failedModelKey 已失败模型 key；为 {@code null} 时从最高优先级开始
	 * @return 后继候选的路由结果
	 */
	default AskDataModelResult routeAfter(AskDataModelRequest request, String failedModelKey) {
		List<Candidate> candidates = candidates(request);
		boolean passedFailure = failedModelKey == null;
		for (int index = 0; index < candidates.size(); index++) {
			Candidate candidate = candidates.get(index);
			if (!passedFailure) {
				if (failedModelKey.equals(candidate.modelKey)) {
					passedFailure = true;
				}
				continue;
			}
			return candidate.result(index);
		}
		throw new IllegalArgumentException("default 模型 key 不能为空，或不存在可用于基础设施故障降级的模型 key");
	}

	default AskDataModelResult routeFrom(List<Candidate> candidates, int startIndex) {
		if (candidates != null && startIndex >= 0 && startIndex < candidates.size()) {
			return candidates.get(startIndex).result(startIndex);
		}
		throw new IllegalArgumentException("不存在可用于基础设施故障降级的模型 key");
	}

	default List<Candidate> candidates(AskDataModelRequest request) {
		if (request == null) throw new IllegalArgumentException("request 不能为空");
		List<Candidate> candidates = new ArrayList<Candidate>();
		add(candidates, request.getRequestModelKey(), AskDataModelRoutePolicy.REQUEST_MODEL_KEY,
				"REQUEST_MODEL_KEY");
		add(candidates, request.getProjectKey(), AskDataModelRoutePolicy.PROJECT_KEY, "PROJECT_KEY");
		add(candidates, request.getDatasourceKey(), AskDataModelRoutePolicy.DATASOURCE_KEY, "DATASOURCE_KEY");
		add(candidates, request.getAgentTaskKey(), AskDataModelRoutePolicy.AGENT_TASK_KEY, "AGENT_TASK_KEY");
		add(candidates, request.getPolicyKey(), AskDataModelRoutePolicy.POLICY_TAG_KEY, "POLICY_TAG_KEY");
		for (int index = 0; index < request.getFallbackKeys().size(); index++) {
			add(candidates, request.getFallbackKeys().get(index), AskDataModelRoutePolicy.CONFIGURED_FALLBACK,
					"CONFIGURED_FALLBACK[" + index + "]");
		}
		add(candidates, request.getDefaultKey(), AskDataModelRoutePolicy.DEFAULT_KEY, "DEFAULT_KEY");
		return candidates;
	}

	default void add(List<Candidate> candidates, String modelKey, AskDataModelRoutePolicy policy, String reason) {
		if (modelKey == null || modelKey.length() == 0) {
			return;
		}
		for (Candidate candidate : candidates) {
			if (modelKey.equals(candidate.modelKey)) {
				return;
			}
		}
		candidates.add(new Candidate(modelKey, policy, reason));
	}

	final class Candidate {
		private final String modelKey;
		private final AskDataModelRoutePolicy policy;
		private final String reason;
		Candidate(String modelKey, AskDataModelRoutePolicy policy, String reason) {
			this.modelKey = modelKey;
			this.policy = policy;
			this.reason = reason;
		}
		AskDataModelResult result(int index) {
			return AskDataModelResult.routed(modelKey, policy, index, reason);
		}
	}
}
