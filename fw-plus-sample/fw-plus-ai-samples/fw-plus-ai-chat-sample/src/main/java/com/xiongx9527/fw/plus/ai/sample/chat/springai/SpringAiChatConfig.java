package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.SystemPromptBuilder;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.KnowledgeBaseProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI（通用）链路装配：Ollama ChatClient + 会话记忆 + 知识库 RAG + 服务 Bean。
 *
 * <p>ChatClient 走 Ollama starter 自动装配的 {@link OllamaChatModel}
 * （{@code spring.ai.ollama.*}），默认 Advisor 挂 {@link MessageChatMemoryAdvisor}
 * 实现按 sessionId 的窗口记忆；命中知识库配置（{@code base-url} 非空）时再挂
 * {@link RetrievalAugmentationAdvisor}（Spring AI 原生 RAG 管线，检索器为
 * {@link RemoteKnowledgeBaseDocumentRetriever}），未配置时不装配、链路退化为纯对话。</p>
 *
 * <p>两个 Advisor 的顺序由各自默认 order 保证：记忆（{@code Integer.MIN_VALUE + 2048}）
 * 先于检索（{@code 0}）执行 before——记忆先落盘<b>原始</b> user message，随后检索
 * 上下文才注入本轮 prompt，因此知识库片段不会沉淀进会话记忆。</p>
 *
 * <p>显式声明 {@code @Bean} 方法参数为 {@link OllamaChatModel} 具体类型，
 * 避免与 DashScope 链路的 {@code ChatModel} Bean 类型冲突。</p>
 */
@Configuration
@EnableConfigurationProperties(KnowledgeBaseProperties.class)
public class SpringAiChatConfig {

	private static final Logger log = LoggerFactory.getLogger(SpringAiChatConfig.class);

	/** 记忆窗口条数（与 langchain4j MessageWindowChatMemory 演示值一致）。 */
	private static final int MAX_MEMORY_MESSAGES = 20;

	/**
	 * Ollama 链路共用的 ChatClient：默认 Advisor 挂窗口记忆（按 CONVERSATION_ID 隔离），
	 * 命中知识库配置时叠加原生 RAG Advisor。
	 */
	@Bean
	public ChatClient springAiOllamaChatClient(OllamaChatModel chatModel, ChatMemory chatMemory,
			ObjectProvider<RemoteKnowledgeBaseDocumentRetriever> knowledgeRetriever) {
		ChatClient.Builder builder = ChatClient.builder(chatModel)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
		RemoteKnowledgeBaseDocumentRetriever retriever = knowledgeRetriever.getIfAvailable();
		if (retriever != null) {
			builder.defaultAdvisors(RetrievalAugmentationAdvisor.builder()
					.documentRetriever(retriever)
					// 空检索结果（无命中或远端故障降级）保持 user message 原文，
					// 与 LangChain4j 链路"无上下文不注入、纯对话继续"的降级语义一致
					.queryAugmenter(ContextualQueryAugmenter.builder()
							.allowEmptyContext(Boolean.TRUE)
							.build())
					.build());
			// 与 ChatAssistantConfig 装配日志同款文案，标明 Spring AI 侧的形态
			log.info("知识库检索增强已启用（Spring AI 原生 RetrievalAugmentationAdvisor）");
		} else {
			log.warn("未检测到知识库配置，本次对话不使用知识库上下文");
		}
		return builder.build();
	}

	/**
	 * 记忆实例（窗口 20 条，进程内存存储），由 Ollama / DashScope 两条 ChatClient 共享，
	 * 两侧按 sessionId 落在同一逻辑会话上——演示"同一 sessionId 跨实现连续对话"。
	 *
	 * <p>外层套 {@link EphemeralChatMemory} 装饰器（与 LangChain4j 链路的框架同款开关）：
	 * 上一轮输出按 {@link AiPersistenceStrategy} 选择性沉淀（默认：有工具往返的整轮不沉淀、
	 * 空白回复不沉淀），RAG 检索段与工具往返只存在于本轮请求窗口，不写入持久历史。</p>
	 */
	@Bean
	public ChatMemory springAiChatMemory() {
		ChatMemory windowMemory = MessageWindowChatMemory.builder()
				.maxMessages(MAX_MEMORY_MESSAGES)
				.build();
		return EphemeralChatMemory.builder(windowMemory)
				.build();
	}

	/**
	 * 远程知识库检索器（Spring AI 原生 {@code DocumentRetriever}）：与 {@code RagAutoConfig}
	 * 相同的启用条件（未配置 {@code base-url} 时不装配，样例退化为纯对话，开箱可跑）。
	 */
	@Bean
	@ConditionalOnExpression("!'${fw.plus.sample.chat.knowledge-base.base-url:}'.isEmpty()")
	public RemoteKnowledgeBaseDocumentRetriever remoteKnowledgeBaseDocumentRetriever(
			KnowledgeBaseProperties properties) {
		return RemoteKnowledgeBaseDocumentRetriever.of(properties);
	}

	/**
	 * Ollama 链路的对话服务。
	 */
	@Bean
	public SpringAiChatService springAiOllamaChatService(
			@Qualifier("springAiOllamaChatClient") ChatClient chatClient,
			WeatherTools weatherTools,
			SystemPromptBuilder systemPromptBuilder) {
		return new SpringAiChatService(chatClient, weatherTools, systemPromptBuilder);
	}
}
