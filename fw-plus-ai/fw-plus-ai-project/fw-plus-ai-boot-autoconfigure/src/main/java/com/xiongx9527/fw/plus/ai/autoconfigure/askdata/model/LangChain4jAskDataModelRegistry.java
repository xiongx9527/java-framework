package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

public class LangChain4jAskDataModelRegistry implements AskDataModelRegistry {

	private final AskDataProperties properties;
	private final Map<String, ChatLanguageModel> chatModels;
	private final Map<String, EmbeddingModel> embeddingModels;
	private final AskDataModelRouter modelRouter = new AskDataModelRouter() {
	};
	private final List<AskDataModelRouteAuditRecord> routeAuditRecords = Collections
			.synchronizedList(new ArrayList<AskDataModelRouteAuditRecord>());

	public LangChain4jAskDataModelRegistry(AskDataProperties properties,
			Map<String, ChatLanguageModel> directChatModels,
			Map<String, EmbeddingModel> directEmbeddingModels,
			List<LangChain4jAskDataModelSource> modelSources) {
		this.properties = properties;
		this.chatModels = new LinkedHashMap<String, ChatLanguageModel>();
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
		final String resolvedKey = resolveModelKey(modelKey, getDefaultChatModelKey());
		final ChatLanguageModel chatLanguageModel = chatModels.get(resolvedKey);
		if (chatLanguageModel == null) {
			throw modelNotFound("聊天", resolvedKey);
		}
		return new AskDataChatModel() {
			@Override
			public AskDataModelResponse generate(String prompt) {
				return new AskDataModelResponse(chatLanguageModel.generate(prompt), null, resolvedKey);
			}
		};
	}

	@Override
	public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
		final String resolvedKey = resolveModelKey(modelKey, getDefaultEmbeddingModelKey());
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
		return properties.getModel().getDefaultChatModel();
	}

	@Override
	public String getDefaultEmbeddingModelKey() {
		return properties.getModel().getDefaultEmbeddingModel();
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
			try {
				ChatLanguageModel chatLanguageModel = chatModels.get(routedResult.getModelKey());
				if (chatLanguageModel == null) {
					throw modelNotFound("聊天", routedResult.getModelKey());
				}
				String response = chatLanguageModel.generate(prompt);
				recordRoute(routedResult);
				return new AskDataModelResponse(response, null, routedResult.getModelKey());
			} catch (AskDataModelException ex) {
				recordRoute(routedResult);
				throw ex;
			} catch (RuntimeException ex) {
				AskDataModelResult failure = classifyFailure(routedResult, ex);
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
	 * 返回当前实例内的路由执行快照，供受控审计出口消费。
	 *
	 * <p>返回副本避免调用方改写共享记录；生产环境应在请求完成后将该快照交给合规审计存储，而不是把
	 * 记录作为长期内存队列。</p>
	 *
	 * @return 不可修改的路由记录快照
	 */
	public List<AskDataModelRouteAuditRecord> getRouteAuditRecords() {
		synchronized (routeAuditRecords) {
			return Collections.unmodifiableList(new ArrayList<AskDataModelRouteAuditRecord>(routeAuditRecords));
		}
	}

	private String resolveModelKey(String modelKey, String defaultModelKey) {
		String resolvedKey = hasText(modelKey) ? modelKey : defaultModelKey;
		if (!hasText(resolvedKey)) {
			throw new AskDataModelException(AskDataErrorCode.DEFAULT_MODEL_NOT_CONFIGURED, "未配置默认模型");
		}
		return resolvedKey;
	}

	private AskDataModelException modelNotFound(String modelType, String modelKey) {
		return new AskDataModelException(AskDataErrorCode.MODEL_NOT_FOUND,
				"未找到" + modelType + "模型: " + modelKey);
	}

	private AskDataModelResult classifyFailure(AskDataModelResult routedResult, RuntimeException exception) {
		if (exception instanceof IllegalArgumentException) {
			return AskDataModelResult.nonRetryableFailure(routedResult.getModelKey(), "模型请求被拒绝");
		}
		return AskDataModelResult.infrastructureFailure(routedResult, "模型基础设施暂不可用");
	}

	private AskDataModelException toModelException(AskDataModelResult failure) {
		return new AskDataModelException(failure.getErrorCode(), failure.getErrorMessage());
	}

	private void recordRoute(AskDataModelResult result) {
		routeAuditRecords.add(new AskDataModelRouteAuditRecord(result));
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}
}
