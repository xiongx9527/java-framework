package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.module.Langchain4jChatMemoryStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code simpleAssistant} 注入契约测试：按配置 key 从各类型 Module 一键装配 SimpleAssistant
 * bean、enabled 开关、专属 key 覆盖默认 key、模型 key 未配置时装配失败。
 */
class Langchain4jModuleSimpleAssistantConfigurationTest {

	private static final String PREFIX = "fw.plus.magicapi.langchain4j.simple-assistant";

	@Test
	void injectsSimpleAssistantBeanFromConfiguredKeys() {
		ChatLanguageModel chatModel = mockChatModel();
		ChatMemoryStore memoryStore = mock(ChatMemoryStore.class);
		Langchain4jChatModelModule chatModelModule = mock(Langchain4jChatModelModule.class);
		when(chatModelModule.getDynamicAttribute("my-assistant")).thenReturn(chatModel);
		Langchain4jChatMemoryStoreModule memoryStoreModule = mock(Langchain4jChatMemoryStoreModule.class);
		when(memoryStoreModule.getDynamicAttribute("redis-store")).thenReturn(memoryStore);

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withBean("chatModelModule", Langchain4jChatModelModule.class, () -> chatModelModule)
				.withBean("chatMemoryStoreModule", Langchain4jChatMemoryStoreModule.class,
						() -> memoryStoreModule)
				.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".key=my-assistant",
						PREFIX + ".keys.chatMemoryStore=redis-store")
				.run(context -> {
					assertThat(context).hasSingleBean(SimpleAssistant.class);
					SimpleAssistant assistant = context.getBean(SimpleAssistant.class);
					assertThat(assistant.chat("session-1", "你好")).isEqualTo("你好，我是助手");
				});
	}

	@Test
	void backsOffWhenDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withPropertyValues(PREFIX + ".enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(SimpleAssistant.class));
	}

	@Test
	void failsToStartWhenNoModelKeyConfigured() {
		// chat-model / streaming-chat-model 都未配置 key → AssistantBuilder.build() 报缺少模型
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withPropertyValues(PREFIX + ".enabled=true")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void usesTypeSpecificKeyOverDefaultKey() {
		// 专属 key 覆盖默认 key：chatModel 只对专属 key 返回模型，默认 key 返回 null
		ChatLanguageModel chatModel = mockChatModel();
		Langchain4jChatModelModule chatModelModule = mock(Langchain4jChatModelModule.class);
		when(chatModelModule.getDynamicAttribute("specific-key")).thenReturn(chatModel);

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withBean("chatModelModule", Langchain4jChatModelModule.class, () -> chatModelModule)
				.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".key=default-key",
						PREFIX + ".keys.chatModel=specific-key")
				.run(context -> {
					assertThat(context).hasSingleBean(SimpleAssistant.class);
					assertThat(context.getBean(SimpleAssistant.class)
							.chat("session-1", "你好")).isEqualTo("你好，我是助手");
				});
	}

	private static ChatLanguageModel mockChatModel() {
		ChatLanguageModel model = mock(ChatLanguageModel.class);
		when(model.generate(any(List.class))).thenReturn(Response.from(AiMessage.from("你好，我是助手")));
		return model;
	}
}
