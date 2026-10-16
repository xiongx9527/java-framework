package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.logging.ChatContextLogger;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 装配层测试：本类只测样例自建模型 Bean 的配置读取与端点归一。
 *
 * <p>助手装配已改走框架 {@code AssistantBuilder}（记忆 / 系统提示 / 工具 / RAG 由 builder
 * 内置承担）：记忆行为回归见 {@link EphemeralChatMemoryIntegrationTest}（真实链路），
 * 容器内装配由 {@code ChatControllerTest} 的 {@code @SpringBootTest} 上下文锁定。</p>
 */
class ChatAssistantConfigTest {

	/** 流式模型 Bean 读的是 {@code langchain4j.ollama.streaming-chat-model} 下的配置 */
	@Test
	void streamingChatModelUsesOllamaProperties() {
		OllamaProperties properties = new OllamaProperties();
		properties.getStreamingChatModel().setBaseUrl("http://localhost:11434");
		properties.getStreamingChatModel().setModelName("gemma4:31b-cloud");

		OllamaStreamingChatModel model = new ChatAssistantConfig().streamingChatModel(properties, new ChatContextLogger());

		assertNotNull(model);
	}

	/** OpenAI 兼容端点地址：缺 /v1 要补上，已有 /v1 或尾斜杠要归一 */
	@Test
	void openAiCompatibleBaseUrlAppendsV1() {
		assertEquals("http://localhost:11434/v1", ChatAssistantConfig.openAiCompatibleBaseUrl("http://localhost:11434"));
		assertEquals("http://localhost:11434/v1", ChatAssistantConfig.openAiCompatibleBaseUrl("http://localhost:11434/"));
		assertEquals("http://localhost:11434/v1", ChatAssistantConfig.openAiCompatibleBaseUrl("http://localhost:11434/v1"));
		assertNull(ChatAssistantConfig.openAiCompatibleBaseUrl(null), "未配置时保持 null，交给 OpenAI 客户端默认值");
	}
}
