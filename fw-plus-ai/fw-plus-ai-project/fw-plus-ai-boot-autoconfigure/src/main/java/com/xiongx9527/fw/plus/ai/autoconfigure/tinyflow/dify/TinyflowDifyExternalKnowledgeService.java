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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class TinyflowDifyExternalKnowledgeService implements IKnowledgeService, DisposableBean {

	private static final String LOCAL_PROVIDER = "fw-plus-local";
	private static final Set<String> OPERATORS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"contains", "not contains", "start with", "end with", "is", "is not", "in", "not in",
			"empty", "not empty", "=", "\u2260", ">", "<", "\u2265", "\u2264", "before", "after")));

	private final TinyflowKnowledgeProviderRegistry providers;
	private final TinyflowKnowledgeRepository knowledgeRepository;
	private final TinyflowKnowledgeAccessKeyService accessKeyService;
	private final TinyflowDifyExternalProperties properties;
	private final ExecutorService executor;
	private final ConcurrentMap<String, RateWindow> rateWindows = new ConcurrentHashMap<String, RateWindow>();

	public TinyflowDifyExternalKnowledgeService(TinyflowKnowledgeProviderRegistry providers,
			TinyflowKnowledgeRepository knowledgeRepository, TinyflowKnowledgeAccessKeyService accessKeyService,
			TinyflowDifyExternalProperties properties) {
		if (providers == null || knowledgeRepository == null || accessKeyService == null || properties == null) {
			throw new IllegalArgumentException("Tinyflow Dify external dependencies must not be null");
		}
		this.providers = providers;
		this.knowledgeRepository = knowledgeRepository;
		this.accessKeyService = accessKeyService;
		this.properties = properties;
		this.executor = Executors.newFixedThreadPool(4, new ExternalThreadFactory());
	}

	@Override
	public KnowledgeResult retrieval(String apiKey, KnowledgeArgs args) {
		TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey = authenticate(apiKey);
		ValidatedRequest validated = validate(args, accessKey);
		checkRateLimit(accessKey.getAccessKeyId());

		TinyflowKnowledgeDefinition definition = knowledgeRepository.findKnowledge(
				accessKey.getTenantId(), validated.knowledgeId);
		if (definition == null || !definition.isEnabled()) {
			throw error(404, 2001, "Knowledge not found");
		}
		if ("dify".equalsIgnoreCase(definition.getProviderKey().trim())) {
			throw error(409, 2002, "Recursive Dify retrieval is not allowed");
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
		Future<TinyflowKnowledgeResult> future = executor.submit(() -> provider.retrieve(request));
		try {
			TinyflowKnowledgeResult result = future.get(properties.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				throw new IllegalStateException("Local knowledge provider returned null");
			}
			return map(result);
		} catch (TimeoutException exception) {
			future.cancel(true);
			throw error(504, 3002, "Knowledge retrieval timed out");
		} catch (InterruptedException exception) {
			future.cancel(true);
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

	private TinyflowKnowledgeAccessKeyRepository.AccessKey authenticate(String token) {
		try {
			return accessKeyService.authenticate(token);
		} catch (TinyflowKnowledgeAccessKeyService.AuthenticationException exception) {
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
		if (!accessKey.getKnowledgeId().equals(knowledgeId)) {
			throw error(401, 1002, "Invalid access key");
		}
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
					conditions.add(TinyflowMetadataCondition.builder().field(condition.getName().trim())
							.operator(condition.getComparison_operator()).value(condition.getValue()).build());
				}
			}
		}
		return new ValidatedRequest(knowledgeId, query, topK, threshold, conditions, options);
	}

	private void checkRateLimit(String accessKeyId) {
		long now = System.currentTimeMillis();
		RateWindow candidate = new RateWindow(now);
		RateWindow window = rateWindows.putIfAbsent(accessKeyId, candidate);
		if (window == null) {
			window = candidate;
		}
		synchronized (window) {
			if (now - window.startedAt >= TimeUnit.MINUTES.toMillis(1L)) {
				window.startedAt = now;
				window.count = 0;
			}
			if (window.count >= properties.getRateLimit()) {
				throw error(429, 3001, "Rate limit exceeded");
			}
			window.count++;
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
		executor.shutdownNow();
		rateWindows.clear();
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
