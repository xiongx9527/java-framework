package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 装配 {@link Assistant}：模型 + 工具 + RAG。
 *
 * <p>{@link RetrievalAugmentor} 通过 {@link ObjectProvider} 注入——未配置知识库时它是空的，
 * {@code AiServices} 便不启用检索增强，样例退化为纯对话。</p>
 */
@Configuration
public class ChatAssistantConfig {

	private static final Logger log = LoggerFactory.getLogger(ChatAssistantConfig.class);

	@Bean
	public WeatherTools weatherTools() {
		return new WeatherTools();
	}

	@Bean
	public Assistant assistant(ChatLanguageModel chatModel,
			StreamingChatLanguageModel streamingChatModel,
			WeatherTools weatherTools,
			ObjectProvider<RetrievalAugmentor> retrievalAugmentor) {

		AiServices<Assistant> builder = AiServices.builder(Assistant.class)
				.chatLanguageModel(chatModel)
				.streamingChatLanguageModel(streamingChatModel)
				.tools(weatherTools);

		RetrievalAugmentor augmentor = retrievalAugmentor.getIfAvailable();
		if (augmentor != null) {
			builder.retrievalAugmentor(augmentor);
			log.info("知识库检索增强已启用");
		} else {
			log.warn("未检测到知识库配置，本次对话不使用知识库上下文");
		}

		return builder.build();
	}
}
