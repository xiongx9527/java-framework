package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelHealthPort;
import com.xiongx9527.fw.plus.ai.support.resilience.FailoverErrorClassifier;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 跨端点的重排（打分）模型故障转移：给定候选链（主模型在前，辅助模型依次在后），按顺序尝试，
 * 前一个抛错则换下一个，直至成功或全部失败。
 *
 * <p>与 {@link FailoverRerankPort} 的区别同 {@link FallbackLlmPort} 与 {@link FailoverLlmPort}：
 * 本类切换不同的端口实例（不同端点），每个候选携带自己的 {@code modelId}。</p>
 *
 * <p>重排是一次性打分（非流式），整体重试即可。候选列表通过 {@link Supplier} 每次调用动态解析。</p>
 */
public final class FallbackRerankPort implements RerankPort {

	private final Supplier<List<RerankModelCandidate>> candidates;

	private final ModelHealthPort health;

	private final FailoverErrorClassifier errorClassifier;

	/** 单候选内最大重试次数（瞬态错误的同一候选重试；0 表示不重试）。 */
	private final int maxRetries;

	/** 重试退避间隔（毫秒）。 */
	private final long retryBackoffMillis;

	public FallbackRerankPort(Supplier<List<RerankModelCandidate>> candidates) {
		this(candidates, null, null, 0, 0L);
	}

	public FallbackRerankPort(Supplier<List<RerankModelCandidate>> candidates, ModelHealthPort health) {
		this(candidates, health, null, 0, 0L);
	}

	public FallbackRerankPort(Supplier<List<RerankModelCandidate>> candidates, FailoverErrorClassifier errorClassifier) {
		this(candidates, null, errorClassifier, 0, 0L);
	}

	public FallbackRerankPort(Supplier<List<RerankModelCandidate>> candidates, ModelHealthPort health,
			FailoverErrorClassifier errorClassifier) {
		this(candidates, health, errorClassifier, 0, 0L);
	}

	public FallbackRerankPort(Supplier<List<RerankModelCandidate>> candidates, ModelHealthPort health,
			FailoverErrorClassifier errorClassifier, int maxRetries, long retryBackoffMillis) {
		if (candidates == null) {
			throw new IllegalArgumentException("candidate supplier is required");
		}
		this.candidates = candidates;
		this.health = health;
		this.errorClassifier = errorClassifier == null ? FailoverErrorClassifier.failOverAll() : errorClassifier;
		this.maxRetries = Math.max(0, maxRetries);
		this.retryBackoffMillis = Math.max(0L, retryBackoffMillis);
	}

	private List<RerankModelCandidate> resolve() {
		List<RerankModelCandidate> list = candidates.get();
		return list == null ? Collections.<RerankModelCandidate>emptyList() : list;
	}

	private FailoverAction actionOf(String message) {
		FailoverAction action = errorClassifier.classify(message);
		return action == null ? FailoverAction.FAILOVER : action;
	}

	private void backoff() {
		if (retryBackoffMillis > 0L) {
			try {
				Thread.sleep(retryBackoffMillis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public List<Double> score(String query, List<String> documents) {
		return score(null, query, documents);
	}

	@Override
	public List<Double> score(String model, String query, List<String> documents) {
		List<RerankModelCandidate> list = resolve();
		if (list.isEmpty()) {
			throw new IllegalStateException("no candidate rerank port available");
		}
		RuntimeException last = null;
		for (RerankModelCandidate c : list) {
			if (health != null && !health.available(c.modelId)) {
				continue;
			}
			// 同一候选内重试（瞬态错误：限流 / 网关 5xx / 超时等）
			int attempt = 0;
			while (true) {
				try {
					List<Double> scores = c.port.score(c.modelId, query, documents);
					if (health != null) {
						health.record(c.modelId, ModelHealthPort.Outcome.SUCCESS);
					}
					return scores;
				} catch (RuntimeException e) {
					if (health != null) {
						health.record(c.modelId, ModelHealthPort.Outcome.FAILURE);
					}
					last = e;
					FailoverAction action = actionOf(e.getMessage());
					if (action == FailoverAction.ABORT) {
						throw e;
					}
					if (action == FailoverAction.RETRY && attempt < maxRetries) {
						attempt++;
						backoff();
						continue; // 同一候选重试
					}
					// FAILOVER 或重试次数耗尽：切到下一候选
					break;
				}
			}
		}
		throw last == null ? new IllegalStateException("all candidate rerank ports failed") : last;
	}

	@Override
	public List<ModelInfo> listModels() {
		for (RerankModelCandidate c : resolve()) {
			try {
				return c.port.listModels();
			} catch (RuntimeException ignored) {
				// 试下一个
			}
		}
		return Collections.emptyList();
	}

	@Override
	public ModelValidation validateModel(String model) {
		for (RerankModelCandidate c : resolve()) {
			try {
				return c.port.validateModel(model);
			} catch (RuntimeException ignored) {
				// 试下一个
			}
		}
		return ModelValidation.invalid(model, "no available candidate to validate", 0L);
	}
}
