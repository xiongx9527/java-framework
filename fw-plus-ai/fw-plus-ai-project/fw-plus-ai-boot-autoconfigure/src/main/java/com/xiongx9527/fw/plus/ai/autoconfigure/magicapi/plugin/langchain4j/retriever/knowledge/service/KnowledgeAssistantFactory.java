package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.Assistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.KnowledgeContentRetriever;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 知识库对话助手工厂：按数据集与元数据过滤条件构建带 RAG 的 {@link SimpleAssistant}。
 *
 * <p>元数据过滤是<b>构建期固定</b>的（langchain4j 标准模式）：每个条件组合（如每个租户）
 * 构建一个 assistant 实例并缓存复用——实例构建轻量（一个代理 + 一个检索器），无需
 * ThreadLocal 隐式传参。租户 id 等系统级条件用户消息中不会出现，因此走本工厂，
 * 而非依赖用户消息提取。</p>
 *
 * <pre>{@code
 * // 每个租户构建一次并缓存
 * SimpleAssistant assistant = factory.create("ds1", chatModel, memoryProvider,
 *     JSON.parseObject("{\"logical_operator\":\"and\",\"conditions\":"
 *         + "[{\"name\":\"tenant_id\",\"comparison_operator\":\"is\",\"value\":\"t01\"}]}"));
 * String answer = assistant.chat(memoryId, message);   // 检索自动带租户过滤
 * }</pre>
 */
public class KnowledgeAssistantFactory {

	private final ObjectProvider<KbSearchService> searchServices;

	private final ObjectProvider<KbMetadataFieldService> metadataFieldServices;

	private final ContentRetrieverContext retrieverContext;

	public KnowledgeAssistantFactory(ObjectProvider<KbSearchService> searchServices,
			ObjectProvider<KbMetadataFieldService> metadataFieldServices,
			ContentRetrieverContext retrieverContext) {
		this.searchServices = searchServices;
		this.metadataFieldServices = metadataFieldServices;
		this.retrieverContext = retrieverContext;
	}

	/**
	 * 构建带元数据过滤 RAG 的对话助手（同步 chat；元数据条件在检索期下推/内存过滤）。
	 *
	 * @param datasetId          知识库数据集 id
	 * @param chatModel          对话模型（AiServices 底层）
	 * @param memoryProvider     会话记忆提供者（AiServices 按记忆键维护）
	 * @param metadataCondition  元数据过滤条件（Dify metadata_condition 格式，构建期固定；可 null）
	 * @param maxResults         召回数量（可 null 用数据集默认）
	 * @param minScore           分数阈值（可 null 用数据集默认）
	 */
	public SimpleAssistant create(String datasetId, ChatLanguageModel chatModel,
			dev.langchain4j.memory.chat.ChatMemoryProvider memoryProvider,
			com.alibaba.fastjson2.JSONObject metadataCondition, Integer maxResults, Double minScore) {
		KnowledgeContentRetriever retriever = new KnowledgeContentRetriever(searchServices, datasetId,
				maxResults, minScore, null, metadataCondition, "none", metadataFieldServices,
				retrieverContext);
		RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
				.contentRetriever(retriever)
				.build();
		dev.langchain4j.service.AiServices<Assistant> builder = AiServices.builder(Assistant.class)
				.chatLanguageModel(chatModel)
				.retrievalAugmentor(augmentor);
		if (memoryProvider != null) {
			builder.chatMemoryProvider(memoryProvider);
		}
		Assistant assistant = builder.build();
		return new SimpleAssistant(assistant, chatModel, null, null, augmentor);
	}

	/**
	 * 只检索助手（不调用对话大模型）：供业务方自行编排（检索 → 拼 prompt → 生成）。
	 */
	public SimpleAssistant createRetrievalOnly(String datasetId,
			com.alibaba.fastjson2.JSONObject metadataCondition, Integer maxResults, Double minScore) {
		KnowledgeContentRetriever retriever = new KnowledgeContentRetriever(searchServices, datasetId,
				maxResults, minScore, null, metadataCondition, "none", metadataFieldServices,
				retrieverContext);
		RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
				.contentRetriever(retriever)
				.build();
		return new SimpleAssistant(augmentor);
	}

	/** 带 @MemoryId/@UserMessage 注解的对话接口（AiServices 契约）。 */
	public interface KbAssistant {

		@UserMessage("{{message}}")
		String chat(@MemoryId String memoryId, @V("message") String message);
	}
}
