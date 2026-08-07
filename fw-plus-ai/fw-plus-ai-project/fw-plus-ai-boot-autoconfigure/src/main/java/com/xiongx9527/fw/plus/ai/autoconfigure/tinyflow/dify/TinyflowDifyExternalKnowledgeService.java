package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowMetadataCondition;
import com.xiongx9527.fw.plus.dify.dataset.IKnowledgeService;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeArgs;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeMetadataCondition;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeResult;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public class TinyflowDifyExternalKnowledgeService implements IKnowledgeService, DisposableBean {

	private static final String LOCAL_PROVIDER = "fw-plus-local";
	private static final int DEFAULT_WORKERS = 4;
	private static final int DEFAULT_QUEUE_CAPACITY = 16;
	private static final int DEFAULT_RATE_WINDOW_CAPACITY = 4096;
	private static final Set<String> OPERATORS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"contains", "not contains", "start with", "end with", "is", "is not", "in", "not in",
			"empty", "not empty", "=", "\u2260", ">", "<", "\u2265", "\u2264", "before", "after")));

	private final TinyflowKnowledgeProviderRegistry providers;
	private final TinyflowKnowledgeRepository knowledgeRepository;
	private final TinyflowKnowledgeAccessKeyService accessKeyService;
	private final TinyflowDifyExternalProperties properties;
	private final ThreadPoolExecutor executor;
	private final int rateWindowCapacity;
	private final LongSupplier nanoTime;
	private final Object rateWindowLock = new Object();
	private final LinkedHashMap<String, RateWindow> rateWindows =
			new LinkedHashMap<String, RateWindow>(16, 0.75F, true);
	private final AtomicBoolean closed = new AtomicBoolean();

	public TinyflowDifyExternalKnowledgeService(TinyflowKnowledgeProviderRegistry providers,
			TinyflowKnowledgeRepository knowledgeRepository, TinyflowKnowledgeAccessKeyService accessKeyService,
			TinyflowDifyExternalProperties properties) {
		this(providers, knowledgeRepository, accessKeyService, properties,
				createExecutor(DEFAULT_WORKERS, DEFAULT_QUEUE_CAPACITY), DEFAULT_RATE_WINDOW_CAPACITY,
				new LongSupplier() {
					@Override public long getAsLong() { return System.nanoTime(); }
				});
	}

	TinyflowDifyExternalKnowledgeService(TinyflowKnowledgeProviderRegistry providers,
			TinyflowKnowledgeRepository knowledgeRepository, TinyflowKnowledgeAccessKeyService accessKeyService,
			TinyflowDifyExternalProperties properties, ThreadPoolExecutor executor, int rateWindowCapacity,
			LongSupplier nanoTime) {
		if (providers == null || knowledgeRepository == null || accessKeyService == null || properties == null) {
			throw new IllegalArgumentException("Tinyflow Dify external dependencies must not be null");
		}
		if (executor == null || rateWindowCapacity <= 0 || nanoTime == null) {
			throw new IllegalArgumentException("Tinyflow Dify external executor and rate window settings must be valid");
		}
		this.providers = providers;
		this.knowledgeRepository = knowledgeRepository;
		this.accessKeyService = accessKeyService;
		this.properties = properties;
		this.executor = executor;
		this.rateWindowCapacity = rateWindowCapacity;
		this.nanoTime = nanoTime;
	}

	private static ThreadPoolExecutor createExecutor(int workers, int queueCapacity) {
		return new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(queueCapacity), new ExternalThreadFactory(),
				new ThreadPoolExecutor.AbortPolicy());
	}

	@Override
	public KnowledgeResult retrieval(String apiKey, KnowledgeArgs args) {
		ensureOpen();
		return retrieval(authenticateAccessKey(apiKey), args);
	}

	KnowledgeResult retrieval(TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey, KnowledgeArgs args) {
		ensureOpen();
		ValidatedRequest validated = validate(args, accessKey);
		checkRateLimit(accessKey.getAccessKeyId());

		TinyflowKnowledgeDefinition definition = knowledgeRepository.findKnowledge(
				accessKey.getTenantId(), validated.knowledgeId);
		if (definition == null || !definition.isEnabled()) {
			throw error(404, 2001, "Knowledge not found");
		}
		if (!LOCAL_PROVIDER.equals(definition.getProviderKey())) {
			throw error(409, 2002, "Only local knowledge retrieval is allowed");
		}
		TinyflowKnowledgeProvider provider = providers.get(LOCAL_PROVIDER);
		if (provider == null || provider.capabilities() == null
				|| !provider.capabilities().supportsRetrieval()) {
			throw error(503, 2003, "Local knowledge retrieval is unavailable");
		}

		final TinyflowKnowledgeRequest request = TinyflowKnowledgeRequest.builder()
				.providerKey(LOCAL_PROVIDER).tenantId(accessKey.getTenantId()).knowledgeId(validated.knowledgeId)
				.query(validated.query).topK(validated.topK).threshold(validated.threshold)
				.metadataConditions(validated.conditions).metadata(new LinkedHashMap<String, Object>())
				.retrievalOptions(validated.retrievalOptions).build();
		Future<TinyflowKnowledgeResult> future;
		try {
			future = executor.submit(() -> provider.retrieve(request));
		} catch (RejectedExecutionException exception) {
			throw error(503, 3003, "Knowledge retrieval is busy");
		}
		try {
			TinyflowKnowledgeResult result = future.get(properties.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				throw new IllegalStateException("Local knowledge provider returned null");
			}
			return map(result);
		} catch (TimeoutException exception) {
			cancel(future);
			throw error(504, 3002, "Knowledge retrieval timed out");
		} catch (InterruptedException exception) {
			cancel(future);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Knowledge retrieval was interrupted", exception);
		} catch (ExecutionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof ExternalKnowledgeException) {
				throw (ExternalKnowledgeException) cause;
			}
			throw new IllegalStateException("Knowledge retrieval failed", cause);
		}
	}

	private void cancel(Future<?> future) {
		future.cancel(true);
		if (future instanceof Runnable) {
			executor.remove((Runnable) future);
		}
	}

	TinyflowKnowledgeAccessKeyRepository.AccessKey authenticateAccessKey(String token) {
		ensureOpen();
		try {
			return accessKeyService.authenticate(token);
		} catch (TinyflowKnowledgeAccessKeyService.AuthenticationException exception) {
			throw error(401, 1002, "Invalid access key");
		}
	}

	void authorizeScope(TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey, String knowledgeId) {
		if (accessKey == null || knowledgeId == null || !accessKey.getKnowledgeId().equals(knowledgeId)) {
			throw error(401, 1002, "Invalid access key");
		}
	}

	private ValidatedRequest validate(KnowledgeArgs args,
			TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey) {
		if (args == null) {
			throw error(400, 1003, "Invalid request");
		}
		String knowledgeId = requireText(args.getKnowledge_id());
		if (knowledgeId == null) {
			throw error(400, 1003, "Invalid request");
		}
		authorizeScope(accessKey, knowledgeId);
		String query = requireText(args.getQuery());
		KnowledgeArgs.Setting setting = args.getRetrieval_setting();
		int topK = setting == null || setting.getTop_k() == null ? 10 : setting.getTop_k().intValue();
		Double threshold = setting == null ? null : setting.getScore_threshold();
		if (query == null || topK <= 0 || topK > properties.getMaxTopK()
				|| threshold != null && (Double.isNaN(threshold.doubleValue())
				|| Double.isInfinite(threshold.doubleValue()) || threshold.doubleValue() < 0D
				|| threshold.doubleValue() > 1D)) {
			throw error(400, 1003, "Invalid request");
		}
		List<TinyflowMetadataCondition> conditions = new ArrayList<TinyflowMetadataCondition>();
		Map<String, Object> options = new LinkedHashMap<String, Object>();
		KnowledgeMetadataCondition metadata = args.getMetadata_condition();
		if (metadata != null) {
			String logic = requireText(metadata.getLogical_operator());
			if (!"and".equals(logic) && !"or".equals(logic)) {
				throw error(400, 1003, "Invalid request");
			}
			options.put("metadataLogic", logic);
			if (metadata.getConditions() != null) {
				for (KnowledgeMetadataCondition.Condition condition : metadata.getConditions()) {
					if (condition == null || requireText(condition.getName()) == null
							|| !OPERATORS.contains(condition.getComparison_operator())) {
						throw error(400, 1003, "Invalid request");
					}
					validateMetadataValue(condition.getComparison_operator(), condition.getValue());
					conditions.add(TinyflowMetadataCondition.builder().field(condition.getName().trim())
							.operator(condition.getComparison_operator()).value(condition.getValue()).build());
				}
			}
		}
		return new ValidatedRequest(knowledgeId, query, topK, threshold, conditions, options);
	}

	private void validateMetadataValue(String operator, Object value) {
		if ("empty".equals(operator) || "not empty".equals(operator)) {
			if (value != null) {
				throw error(400, 1003, "Invalid request");
			}
			return;
		}
		if ("in".equals(operator) || "not in".equals(operator)) {
			if (!(value instanceof Collection<?>) && (value == null || !value.getClass().isArray())) {
				throw error(400, 1003, "Invalid request");
			}
			return;
		}
		if (">".equals(operator) || "<".equals(operator)
				|| "\u2265".equals(operator) || "\u2264".equals(operator)) {
			if (!(value instanceof Number) && !(value instanceof CharSequence)) {
				throw error(400, 1003, "Invalid request");
			}
			return;
		}
		if ("before".equals(operator) || "after".equals(operator)) {
			if (!(value instanceof CharSequence) || requireText(String.valueOf(value)) == null) {
				throw error(400, 1003, "Invalid request");
			}
			return;
		}
		if (value == null || value instanceof Map<?, ?> || value instanceof Collection<?>
				|| value.getClass().isArray()) {
			throw error(400, 1003, "Invalid request");
		}
	}

	private void checkRateLimit(String accessKeyId) {
		long now = nanoTime.getAsLong();
		synchronized (rateWindowLock) {
			ensureOpen();
			removeExpiredRateWindows(now);
			RateWindow window = rateWindows.get(accessKeyId);
			if (window == null) {
				if (rateWindows.size() >= rateWindowCapacity) {
					Iterator<Map.Entry<String, RateWindow>> iterator = rateWindows.entrySet().iterator();
					if (iterator.hasNext()) {
						iterator.next();
						iterator.remove();
					}
				}
				window = new RateWindow(now);
				rateWindows.put(accessKeyId, window);
			}
			if (window.count >= properties.getRateLimit()) {
				throw error(429, 3001, "Rate limit exceeded");
			}
			window.count++;
		}
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw error(503, 3003, "Knowledge retrieval is busy");
		}
	}

	private void removeExpiredRateWindows(long now) {
		Iterator<Map.Entry<String, RateWindow>> iterator = rateWindows.entrySet().iterator();
		while (iterator.hasNext()) {
			RateWindow window = iterator.next().getValue();
			if (now - window.startedAt >= TimeUnit.MINUTES.toNanos(1L)) {
				iterator.remove();
			}
		}
	}

	private KnowledgeResult map(TinyflowKnowledgeResult result) {
		List<KnowledgeResult.Record> records = new ArrayList<KnowledgeResult.Record>();
		for (TinyflowKnowledgeMatch match : result.getMatches()) {
			Map<String, Object> metadata = new LinkedHashMap<String, Object>();
			if (match.getMetadata() != null) {
				metadata.putAll(match.getMetadata());
			}
			metadata.put("source_id", match.getSourceId());
			metadata.put("source_type", match.getSourceType());
			metadata.put("document_id", match.getDocumentId());
			metadata.put("chunk_id", match.getChunkId());
			records.add(KnowledgeResult.Record.builder().content(match.getContent()).score(match.getScore())
					.title(match.getDocumentId()).metadata(metadata).build());
		}
		return new KnowledgeResult(records);
	}

	private String requireText(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}

	private ExternalKnowledgeException error(int status, int code, String message) {
		return new ExternalKnowledgeException(status, code, message);
	}

	@Override
	public void destroy() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}
		executor.shutdownNow();
		try {
			executor.awaitTermination(1L, TimeUnit.SECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		synchronized (rateWindowLock) {
			rateWindows.clear();
		}
	}

	static class ExternalKnowledgeException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final int status;
		private final int code;

		ExternalKnowledgeException(int status, int code, String message) {
			super(message);
			this.status = status;
			this.code = code;
		}

		int getStatus() { return status; }
		int getCode() { return code; }
	}

	private static final class ValidatedRequest {
		private final String knowledgeId;
		private final String query;
		private final int topK;
		private final Double threshold;
		private final List<TinyflowMetadataCondition> conditions;
		private final Map<String, Object> retrievalOptions;

		private ValidatedRequest(String knowledgeId, String query, int topK, Double threshold,
				List<TinyflowMetadataCondition> conditions, Map<String, Object> retrievalOptions) {
			this.knowledgeId = knowledgeId;
			this.query = query;
			this.topK = topK;
			this.threshold = threshold;
			this.conditions = conditions;
			this.retrievalOptions = retrievalOptions;
		}
	}

	private static final class RateWindow {
		private long startedAt;
		private int count;
		private RateWindow(long startedAt) { this.startedAt = startedAt; }
	}

	private static final class ExternalThreadFactory implements ThreadFactory {
		private final AtomicInteger sequence = new AtomicInteger();
		@Override public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "tinyflow-dify-external-" + sequence.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}
	}
}
