package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.intent.IntentEvent;
import com.xiongx9527.fw.plus.ai.base.model.ContextInjectEvent;
import com.xiongx9527.fw.plus.ai.base.model.LlmCapabilities;
import com.xiongx9527.fw.plus.ai.base.model.LlmException;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.StreamEventContext;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalEvent;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionResult;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelHealthPort;
import com.xiongx9527.fw.plus.ai.support.resilience.FailoverErrorClassifier;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 跨端点的对话模型故障转移：给定候选链（主模型在前，辅助模型依次在后），按顺序尝试，
 * 前一个失败则换下一个，直至成功或全部失败。
 *
 * <p>与 {@link FailoverLlmPort} 的区别：后者是在<b>同一个主端口</b>上按模型名切换，
 * 本类切换的是<b>不同的端口实例</b>——每个候选往往对应不同的端点 / 鉴权（如默认 OpenAI、
 * 辅助本地 Ollama），因此每个候选携带自己的 {@code modelId}，调用时把 {@code request.model}
 * 重写为该候选自身的模型名，避免用主模型名去问备用端点。</p>
 *
 * <p>候选列表通过 {@link Supplier} 在<b>每次调用</b>动态解析，数据库里模型配置的增删改无需重启即生效。</p>
 *
 * <p>非流式通道整体重试；流式通道按「吐字前 / 吐字后」区别处理：</p>
 * <ul>
 *   <li>候选在<b>尚未吐出任何 token 之前</b>报错 → 透明切到下一候选重新发流（调用方无感知）；</li>
 *   <li>候选<b>已经吐出 token 之后</b>才报错 → 如实向调用方上报错误，不再静默切换
 *       （否则已吐出的内容会与下一候选的输出交错重复）。</li>
 * </ul>
 */
public final class FallbackLlmPort implements LlmPort {

	private final Supplier<List<LlmModelCandidate>> candidates;

	private final ModelHealthPort health;

	private final FailoverErrorClassifier errorClassifier;

	/** 单候选内最大重试次数（瞬态错误的同一候选重试；0 表示不重试）。 */
	private final int maxRetries;

	/** 重试退避间隔（毫秒）。 */
	private final long retryBackoffMillis;

	public FallbackLlmPort(Supplier<List<LlmModelCandidate>> candidates) {
		this(candidates, null, null, 0, 0L);
	}

	public FallbackLlmPort(Supplier<List<LlmModelCandidate>> candidates, ModelHealthPort health) {
		this(candidates, health, null, 0, 0L);
	}

	public FallbackLlmPort(Supplier<List<LlmModelCandidate>> candidates, FailoverErrorClassifier errorClassifier) {
		this(candidates, null, errorClassifier, 0, 0L);
	}

	public FallbackLlmPort(Supplier<List<LlmModelCandidate>> candidates, ModelHealthPort health,
			FailoverErrorClassifier errorClassifier) {
		this(candidates, health, errorClassifier, 0, 0L);
	}

	public FallbackLlmPort(Supplier<List<LlmModelCandidate>> candidates, ModelHealthPort health,
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

	private List<LlmModelCandidate> resolve() {
		List<LlmModelCandidate> list = candidates.get();
		return list == null ? Collections.<LlmModelCandidate>emptyList() : list;
	}

	@Override
	public LlmResponse complete(LlmRequest request) {
		List<LlmModelCandidate> list = resolve();
		if (list.isEmpty()) {
			throw new LlmException("no candidate llm port available");
		}
		RuntimeException last = null;
		for (LlmModelCandidate c : list) {
			if (health != null && !health.available(c.modelId)) {
				continue;
			}
			// 同一候选内重试（瞬态错误：限流 / 网关 5xx / 超时等）
			int attempt = 0;
			while (true) {
				try {
					LlmResponse response = c.port.complete(withModel(request, c.modelId));
					if (health != null) {
						health.record(c.modelId, ModelHealthPort.Outcome.SUCCESS);
					}
					return response;
				} catch (RuntimeException e) {
					if (health != null) {
						health.record(c.modelId, ModelHealthPort.Outcome.FAILURE);
					}
					last = e;
					FailoverAction action = actionOf(e.getMessage());
					if (action == FailoverAction.ABORT) {
						// 调用方自身错误：重试 / 切换都无济于事，立即上抛
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
		throw last == null ? new LlmException("all candidate llm ports failed") : last;
	}

	@Override
	public LlmTokenStream stream(LlmRequest request) {
		return LlmTokenStream.create(handler -> runStream(request, handler));
	}

	private void runStream(LlmRequest request, LlmStreamHandler handler) {
		List<LlmModelCandidate> list = resolve();
		if (list.isEmpty()) {
			handler.onError("no candidate llm port available");
			return;
		}
		final AtomicReference<RuntimeException> lastError = new AtomicReference<>();
		final AtomicBoolean forwarded = new AtomicBoolean(false);
		// 遍历候选；每个候选内按策略重试（瞬态错误 RETRY）
		for (int i = 0; i < list.size(); i++) {
			LlmModelCandidate c = list.get(i);
			if (health != null && !health.available(c.modelId)) {
				continue;
			}
			boolean switched = false; // 本候选是否因 FAILOVER / 重试耗尽而需切下一候选
			for (int attempt = 0; attempt <= maxRetries; attempt++) {
				final AtomicBoolean emitted = new AtomicBoolean(false);
				final AtomicBoolean succeeded = new AtomicBoolean(false);
				final AtomicReference<String> attemptError = new AtomicReference<>();
				final CountDownLatch done = new CountDownLatch(1);
				try {
					c.port.stream(withModel(request, c.modelId)).handler(new LlmStreamHandler() {
						@Override
						public void onToken(String content, String reasoningContent) {
							emitted.set(true);
							handler.onToken(content, reasoningContent);
						}

						@Override
						public void onToken(String content, String reasoningContent, StreamEventContext context) {
							emitted.set(true);
							handler.onToken(content, reasoningContent, context);
						}

						@Override
						public void onComplete() {
							succeeded.set(true);
							handler.onComplete();
							done.countDown();
						}

						@Override
						public void onError(String message) {
							// 不直接转发：由外层按策略（重试 / 切换 / 终止）统一裁决
							attemptError.set(message);
							done.countDown();
						}

						@Override
						public void onToolCall(ToolExecutionRequest call) {
							handler.onToolCall(call);
						}

						@Override
						public void onToolCall(ToolExecutionRequest call, StreamEventContext ctx) {
							handler.onToolCall(call, ctx);
						}

						@Override
						public void onPartialToolCall(String id, String name, String argumentsDelta) {
							handler.onPartialToolCall(id, name, argumentsDelta);
						}

						@Override
						public void onPartialToolCall(String id, String name, String argumentsDelta, StreamEventContext ctx) {
							handler.onPartialToolCall(id, name, argumentsDelta, ctx);
						}

						@Override
						public void onCompleteToolCall(ToolExecutionRequest call) {
							handler.onCompleteToolCall(call);
						}

						@Override
						public void onCompleteToolCall(ToolExecutionRequest call, StreamEventContext ctx) {
							handler.onCompleteToolCall(call, ctx);
						}

						@Override
						public void onToolResponse(ToolExecutionResult result) {
							handler.onToolResponse(result);
						}

						@Override
						public void onToolResponse(ToolExecutionResult result, StreamEventContext ctx) {
							handler.onToolResponse(result, ctx);
						}

						@Override
						public void onIntent(IntentEvent event) {
							handler.onIntent(event);
						}

						@Override
						public void onRetrieval(RetrievalEvent event) {
							handler.onRetrieval(event);
						}

						@Override
						public void onContextInject(ContextInjectEvent event) {
							handler.onContextInject(event);
						}

						@Override
						public void onUsage(long totalTokens) {
							handler.onUsage(totalTokens);
						}

						@Override
						public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
							handler.onUsageDetail(inputTokens, outputTokens, totalTokens);
						}

						@Override
						public void onFinish(String finishReason) {
							handler.onFinish(finishReason);
						}

						@Override
						public void onUnmappedRawEvent(Object rawEvent) {
							handler.onUnmappedRawEvent(rawEvent);
						}
					}).start();
				} catch (RuntimeException e) {
					// 启动即失败：等同「吐字前报错」
					attemptError.set(e.getMessage() == null ? e.toString() : e.getMessage());
				}
				try {
					done.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					handler.onError("interrupted while waiting for llm stream");
					return;
				}
				if (succeeded.get()) {
					return;
				}
				if (emitted.get()) {
					// 已吐字后失败：任何重试 / 切换都会导致内容交错重复，如实上抛一次并停止
					if (!forwarded.get()) {
						forwarded.set(true);
						handler.onError(attemptError.get());
					}
					return;
				}
				// 吐字前失败：按分类策略裁决
				FailoverAction action = actionOf(attemptError.get());
				if (action == FailoverAction.ABORT) {
					forwarded.set(true);
					handler.onError(attemptError.get());
					return;
				}
				if (action == FailoverAction.RETRY && attempt < maxRetries) {
					backoff();
					continue; // 同一候选重试
				}
				// FAILOVER 或重试次数耗尽：静默切下一候选（不转发错误）
				lastError.set(new LlmException(attemptError.get()));
				switched = true;
				break;
			}
			if (!switched) {
				// 重试耗尽但既没成功也没切走（理论上不会到这里），兜底
				break;
			}
		}
		if (!forwarded.get()) {
			handler.onError(lastError.get() == null ? "all candidate llm ports failed" : lastError.get().getMessage());
		}
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
	public List<ModelInfo> listModels() {
		for (LlmModelCandidate c : resolve()) {
			try {
				return c.port.listModels();
			} catch (RuntimeException ignored) {
				// 该候选不可用，试下一个
			}
		}
		return Collections.emptyList();
	}

	@Override
	public ModelValidation validateModel(String model) {
		for (LlmModelCandidate c : resolve()) {
			try {
				return c.port.validateModel(model);
			} catch (RuntimeException ignored) {
				// 试下一个
			}
		}
		return ModelValidation.invalid(model, "no available candidate to validate", 0L);
	}

	@Override
	public LlmCapabilities capabilities() {
		for (LlmModelCandidate c : resolve()) {
			try {
				return c.port.capabilities();
			} catch (RuntimeException ignored) {
				// 试下一个
			}
		}
		return LlmCapabilities.basic();
	}

	private static LlmRequest withModel(LlmRequest request, String modelId) {
		return request.toBuilder().model(modelId).build();
	}
}
