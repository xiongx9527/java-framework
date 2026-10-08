package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.SystemPromptBuilder;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Alibaba（DashScope / 通义千问）链路装配。
 *
 * <p><b>按需装配</b>：仅当配置了非空 {@code spring.ai.dashscope.api-key}
 * （环境变量 {@code AI_DASHSCOPE_API_KEY}）时才创建 ChatClient 与服务 Bean——
 * 未配置时应用照常启动，{@code /api/chat/alibaba*} 接口返回 503 明确错误
 * （见 {@link SpringAiAlibabaChatController}），不会因缺少 key 导致启动失败。</p>
 *
 * <p>记忆与 Ollama 链路共享同一个 {@link org.springframework.ai.chat.memory.ChatMemory}
 * Bean，同一 sessionId 在两条 Spring AI 链路上能延续同一份会话；知识库 RAG 同样挂
 * {@link RetrievalAugmentationAdvisor}（检索器 Bean 也由 Ollama 侧配置统一装配，
 * 见 {@link RemoteKnowledgeBaseDocumentRetriever}），两条链路的检索行为一致。</p>
 */
@Configuration
@ConditionalOnExpression("!'${spring.ai.dashscope.api-key:}'.isEmpty()")
public class SpringAiAlibabaChatConfig {

	/**
	 * DashScope 链路的对话服务：ChatClient 与 Ollama 链路同构
	 * （记忆 Advisor 必挂 + 命中知识库配置时叠加原生 RAG Advisor）。
	 */
	@Bean
	public SpringAiChatService springAiDashScopeChatService(
			DashScopeChatModel dashScopeChatModel,
			WeatherTools weatherTools,
			SystemPromptBuilder systemPromptBuilder,
			ObjectProvider<RemoteKnowledgeBaseDocumentRetriever> knowledgeRetriever,
			org.springframework.ai.chat.memory.ChatMemory chatMemory) {
		ChatClient.Builder builder = ChatClient.builder(dashScopeChatModel)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
		RemoteKnowledgeBaseDocumentRetriever retriever = knowledgeRetriever.getIfAvailable();
		if (retriever != null) {
			builder.defaultAdvisors(RetrievalAugmentationAdvisor.builder()
					.documentRetriever(retriever)
					// 空检索结果保持 user message 原文（与 Ollama 链路、LangChain4j 链路一致）
					.queryAugmenter(ContextualQueryAugmenter.builder()
							.allowEmptyContext(Boolean.TRUE)
							.build())
					.build());
		}
		return new SpringAiChatService(builder.build(), weatherTools, systemPromptBuilder);
	}
}
