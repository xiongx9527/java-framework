package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class LangChain4jAskDataModelRegistry implements AskDataModelRegistry {

	private final AskDataProperties properties;
	private final Map<String, ChatModel> chatModels;
	private final Map<String, EmbeddingModel> embeddingModels;
	private final AskDataModelRouter modelRouter = new AskDataModelRouter() {
	};
	private final List<AskDataModelRouteAuditRecord> routeAuditRecords = Collections
			.synchronizedList(new ArrayList<AskDataModelRouteAuditRecord>());

	public LangChain4jAskDataModelRegistry(AskDataProperties properties,
			Map<String, ChatModel> directChatModels,
			Map<String, EmbeddingModel> directEmbeddingModels,
			List<LangChain4jAskDataModelSource> modelSources) {
		this.properties = properties;
		this.chatModels = new LinkedHashMap<String, ChatModel>();
		this.embeddingModels = new LinkedHashMap<String, EmbeddingModel>();
		if (modelSources != null) {
			for (LangChain4jAskDataModelSource modelSource : modelSources) {
				if (modelSource.getChatModels() != null) {
					this.chatModels.putAll(modelSource.getChatModels());
				}
				if (modelSource.getEmbeddingModels() != null) {
					this.embeddingModels.putAll(modelSource.getEmbeddingModels());
				}
			}
		}
		if (directChatModels != null) {
			this.chatModels.putAll(directChatModels);
		}
		if (directEmbeddingModels != null) {
			this.embeddingModels.putAll(directEmbeddingModels);
		}
	}

	@Override
	public AskDataChatModel getChatModel(String modelKey) {
		final String resolvedKey = resolveModelKey(modelKey, properties.getModel().getChatCandidates(),
				getDefaultChatModelKey(), chatModels);
		final ChatModel chatModel = chatModels.get(resolvedKey);
		if (chatModel == null) {
			throw modelNotFound("聊天", resolvedKey);
		}
		return new AskDataChatModel() {
			@Override
			public AskDataModelResponse generate(String prompt) {
				return new AskDataModelResponse(chatModel.chat(prompt), null, resolvedKey);
			}
		};
	}

	@Override
	public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
		final String resolvedKey = resolveModelKey(modelKey, properties.getModel().getEmbeddingCandidates(),
				getDefaultEmbeddingModelKey(), embeddingModels);
		final EmbeddingModel embeddingModel = embeddingModels.get(resolvedKey);
		if (embeddingModel == null) {
			throw modelNotFound("向量", resolvedKey);
		}
		return new AskDataEmbeddingModel() {
			@Override
			public List<Float> embed(String text) {
				return embeddingModel.embed(text).content().vectorAsList();
			}
		};
	}

	@Override
	public String getDefaultChatModelKey() {
		return configuredOrOnlyKey(properties.getModel().getDefaultChatModel(), chatModels);
	}

	@Override
	public String getDefaultEmbeddingModelKey() {
		return configuredOrOnlyKey(properties.getModel().getDefaultEmbeddingModel(), embeddingModels);
	}

	@Override
	public AskDataModelResponse generate(AskDataRequest request, String prompt) {
		if (request == null) {
			return AskDataModelRegistry.super.generate(null, prompt);
		}
		return generate(defaultChatModelRequest(request.getProjectContext(), request.getModelKey()), prompt);
	}

	/**
	 * 使用 Core 的厂商无关路由结果执行聊天模型，并在基础设施错误时按配置进行受控降级。
	 *
	 * <p>项目和操作人隔离上下文完全由 {@code request} 携带并原样交给 Core，适配层不重解释权限范围。
	 * 仅网络、网关和超时等可重试错误切换候选；参数或鉴权类 {@link IllegalArgumentException} 保持原结果，
	 * 防止无效或无权限的请求借由模型切换绕过策略。</p>
	 *
	 * @param request 已由上层按请求、项目、数据源、任务及默认优先级构建的 Core 路由请求
	 * @param prompt 待发送给模型的提示词；不会被保存到路由审计记录
	 * @return 成功响应及实际命中的逻辑模型 key
	 */
	public AskDataModelResponse generate(AskDataModelRequest request, String prompt) {
		AskDataModelResult routedResult = modelRouter.route(request);
		while (true) {
			long startedAt = System.currentTimeMillis();
			try {
				ChatModel chatModel = chatModels.get(routedResult.getModelKey());
				if (chatModel == null) {
					throw modelNotFound("聊天", routedResult.getModelKey());
				}
				String response = chatModel.chat(prompt);
				recordRoute(routedResult);
				return new AskDataModelResponse(response, null, routedResult.getModelKey());
			} catch (AskDataModelException ex) {
				recordRoute(routedResult);
				throw ex;
			} catch (RuntimeException ex) {
				AskDataModelResult failure = classifyFailure(routedResult, ex,
						System.currentTimeMillis() - startedAt);
				recordRoute(failure);
				if (!failure.isRetryable() || !properties.getModel().isFallbackEnabled()) {
					throw toModelException(failure);
				}
				try {
					routedResult = modelRouter.fallback(request, failure);
				} catch (IllegalArgumentException noCandidate) {
					throw toModelException(failure);
				}
			}
		}
	}

	/**
	 * 使用自动配置的聊天候选创建默认 Core 路由请求。
	 *
	 * <p>Core 路由 SPI 固定支持五个优先级槽位；此入口按候选顺序填充，始终保留默认聊天 key 作为最后兜底。
	 * provider 仅作为适配层的诊断标签记录在审计结果中，不参与模型源或适配器选择。</p>
	 *
	 * @param projectContext 当前项目和操作人隔离上下文，不能为空
	 * @param prompt 待发送给模型的提示词；不会进入审计记录
	 * @return 成功响应及实际命中的逻辑模型 key
	 */
	public AskDataModelResponse generate(AskDataProjectContext projectContext, String prompt) {
		return generate(defaultChatModelRequest(projectContext), prompt);
	}

	/**
	 * 返回当前实例内的路由执行快照，供受控审计出口消费。
	 *
	 * <p>返回副本避免调用方改写共享记录。该快照仅用于本机诊断，且按配置容量裁剪；不得作为跨请求
	 * 审计查询接口，完整记录应由监控 SPI 在请求完成时持久化。</p>
	 *
	 * @return 不可修改的路由记录快照
	 */
	public List<AskDataModelRouteAuditRecord> getRouteAuditRecords() {
		synchronized (routeAuditRecords) {
			return Collections.unmodifiableList(new ArrayList<AskDataModelRouteAuditRecord>(routeAuditRecords));
		}
	}

	private String resolveModelKey(String modelKey, List<String> candidates, String defaultModelKey,
			Map<String, ?> registeredModels) {
		String resolvedKey = hasText(modelKey) ? modelKey : resolveConfiguredModelKey(candidates, defaultModelKey,
				registeredModels);
		if (!hasText(resolvedKey)) {
			throw new AskDataModelException(AskDataErrorCode.DEFAULT_MODEL_NOT_CONFIGURED, "未配置默认模型");
		}
		return resolvedKey;
	}

	private String resolveConfiguredModelKey(List<String> candidates, String defaultModelKey,
			Map<String, ?> registeredModels) {
		if (candidates != null) {
			for (String candidate : candidates) {
				if (hasText(candidate) && registeredModels.containsKey(candidate.trim())) {
					return candidate.trim();
				}
			}
		}
		return defaultModelKey;
	}

	private String configuredOrOnlyKey(String configuredKey, Map<String, ?> registeredModels) {
		if (hasText(configuredKey)) {
			return configuredKey.trim();
		}
		return registeredModels.size() == 1 ? registeredModels.keySet().iterator().next() : null;
	}

	private AskDataModelException modelNotFound(String modelType, String modelKey) {
		return new AskDataModelException(AskDataErrorCode.MODEL_NOT_FOUND,
				"未找到" + modelType + "模型: " + modelKey);
	}

	private AskDataModelResult classifyFailure(AskDataModelResult routedResult, RuntimeException exception,
			long elapsedMillis) {
		if (isNonRetryableFailure(exception)) {
			return AskDataModelResult.nonRetryableFailure(routedResult.getModelKey(), "模型请求被拒绝");
		}
		if (isInfrastructureTimeout(exception) || isInfrastructureTimeout(elapsedMillis)) {
			return AskDataModelResult.infrastructureFailure(routedResult, "模型基础设施暂不可用");
		}
		return AskDataModelResult.nonRetryableFailure(routedResult.getModelKey(), "模型请求被拒绝");
	}

	private AskDataModelException toModelException(AskDataModelResult failure) {
		return new AskDataModelException(failure.getErrorCode(), failure.getErrorMessage());
	}

	private void recordRoute(AskDataModelResult result) {
		synchronized (routeAuditRecords) {
			int capacity = getRouteAuditRecordCapacity();
			if (capacity <= 0) {
				return;
			}
			while (routeAuditRecords.size() >= capacity) {
				routeAuditRecords.remove(0);
			}
			routeAuditRecords.add(new AskDataModelRouteAuditRecord(result, properties.getModel().getProvider()));
		}
	}

	private AskDataModelRequest defaultChatModelRequest(AskDataProjectContext projectContext) {
		return defaultChatModelRequest(projectContext, null);
	}

	private AskDataModelRequest defaultChatModelRequest(AskDataProjectContext projectContext, String requestModelKey) {
		if (projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		Set<String> candidates = new LinkedHashSet<String>();
		if (hasText(requestModelKey)) {
			candidates.add(requestModelKey.trim());
		}
		if (properties.getModel().getChatCandidates() != null) {
			for (String candidate : properties.getModel().getChatCandidates()) {
				if (hasText(candidate)) {
					candidates.add(candidate.trim());
				}
			}
		}
		String defaultKey = getDefaultChatModelKey();
		boolean containsDefaultKey = hasText(defaultKey) && candidates.contains(defaultKey.trim());
		if (hasText(defaultKey)) {
			candidates.add(defaultKey.trim());
		}
		List<String> candidateList = new ArrayList<String>(candidates);
		String[] routeKeys = new String[] { "", "", "", "", "" };
		int candidateLimit = containsDefaultKey ? 5 : 4;
		for (int index = 0; index < candidateList.size() && index < candidateLimit; index++) {
			routeKeys[index] = candidateList.get(index);
		}
		if (hasText(defaultKey) && !contains(routeKeys, defaultKey)) {
			routeKeys[4] = defaultKey;
		}
		return new AskDataModelRequest(projectContext, routeKeys[0], routeKeys[1], routeKeys[2], routeKeys[3],
				routeKeys[4]);
	}

	private boolean isInfrastructureTimeout(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof TimeoutException || current instanceof java.net.SocketTimeoutException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private boolean isNonRetryableFailure(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof IllegalArgumentException || current instanceof IllegalStateException
					|| current instanceof SecurityException || current instanceof UnsupportedOperationException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private boolean isInfrastructureTimeout(long elapsedMillis) {
		Integer timeoutMillis = properties.getModel().getInfrastructureFailureTimeoutMillis();
		return timeoutMillis != null && timeoutMillis.intValue() > 0 && elapsedMillis >= timeoutMillis.intValue();
	}

	private int getRouteAuditRecordCapacity() {
		Integer capacity = properties.getModel().getRouteAuditRecordCapacity();
		return capacity == null ? 0 : capacity.intValue();
	}

	private boolean contains(String[] values, String value) {
		for (String item : values) {
			if (value.equals(item)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}
}
