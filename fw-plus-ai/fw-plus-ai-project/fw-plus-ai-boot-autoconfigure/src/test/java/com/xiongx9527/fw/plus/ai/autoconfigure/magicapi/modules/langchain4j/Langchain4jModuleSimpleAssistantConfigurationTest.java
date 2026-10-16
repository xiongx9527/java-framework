package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
		Langchain4jChatModelModule chatModelModule = mock(Langchain4jChatModelModule.class);
		when(chatModelModule.getDynamicAttribute("my-assistant")).thenReturn(chatModel);

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withBean("chatModelModule", Langchain4jChatModelModule.class, () -> chatModelModule)
				.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".use-default-key=true",
						PREFIX + ".key=my-assistant")
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

	@Test
	void appliesTunableParametersFromProperties() {
		// memory-max-messages / max-tokens / rerank-min-score 三个可调参数全部配置 → 装配成功
		ChatLanguageModel chatModel = mockChatModel();
		Langchain4jChatModelModule chatModelModule = mock(Langchain4jChatModelModule.class);
		when(chatModelModule.getDynamicAttribute("my-assistant")).thenReturn(chatModel);
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withBean("chatModelModule", Langchain4jChatModelModule.class, () -> chatModelModule)
				.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".keys.chatModel=my-assistant",
						PREFIX + ".memory-max-messages=10",
						PREFIX + ".max-tokens=500",
						PREFIX + ".rerank-min-score=0.5")
				.run(context -> {
					assertThat(context).hasSingleBean(SimpleAssistant.class);
					assertThat(context.getBean(SimpleAssistant.class)
							.chat("session-1", "你好")).isEqualTo("你好，我是助手");
				});
	}

	@Test
	void failsWhenRequiredButSimpleAssistantAbsent() {
		// simple-assistant.required=true 且 enabled=false（SimpleAssistant 缺席）→ langchain4jModule 创建失败
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withPropertyValues(
						PREFIX + ".enabled=false",
						PREFIX + ".simple-assistant-required=true")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void skipsTypeWhenUseDefaultKeyDisabled() {
		// use-default-key=false：只有专属 key 生效——chatModel 配专属 key 正常装配，
		// 且 chatModelModule 对默认 key 返回 null 也无影响（不走默认 key）
		ChatLanguageModel chatModel = mockChatModel();
		Langchain4jChatModelModule chatModelModule = mock(Langchain4jChatModelModule.class);
		when(chatModelModule.getDynamicAttribute("specific-key")).thenReturn(chatModel);

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withBean("chatModelModule", Langchain4jChatModelModule.class, () -> chatModelModule)
				.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".use-default-key=false",
						PREFIX + ".key=default-key",
						PREFIX + ".keys.chatModel=specific-key")
				.run(context -> {
					assertThat(context).hasSingleBean(SimpleAssistant.class);
					assertThat(context.getBean(SimpleAssistant.class)
							.chat("session-1", "你好")).isEqualTo("你好，我是助手");
				});
	}

	@Test
	void createsFourSimpleAssistantObjectsInModuleConstructor() {
		// langchain4jModule 构造里创建 4 个 SimpleAssistant：3 单能力 + 1 全能力
		ChatLanguageModel chatModel = mockChatModel();
		Langchain4jChatModelModule chatModelModule = mock(Langchain4jChatModelModule.class);
		when(chatModelModule.getDynamicAttribute("my-assistant")).thenReturn(chatModel);
		Langchain4jStreamingChatModelModule streamingModule = mock(Langchain4jStreamingChatModelModule.class);
		when(streamingModule.getDynamicAttribute("my-assistant")).thenReturn(mock(StreamingChatLanguageModel.class));

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class))
				.withBean("chatModelModule", Langchain4jChatModelModule.class, () -> chatModelModule)
				.withBean("streamingModule", Langchain4jStreamingChatModelModule.class, () -> streamingModule)
				.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".key=my-assistant",
						PREFIX + ".use-default-key=true")
				.run(context -> {
					Langchain4jModule module = context.getBean(Langchain4jModule.class);
					assertThat(module.chatAssistant()).isNotNull();
					assertThat(module.simpleAssistant()).isNotNull();
					assertThat(module.simpleAssistant().chat("session-1", "你好")).isEqualTo("你好，我是助手");
					// languageModel / streamingLanguageModel 插件缺席 → 对象为 null，方法提示
					assertThatThrownBy(() -> module.languageAssistant())
							.hasMessageContaining("language-model");
				});
	}

	private static ChatLanguageModel mockChatModel() {
		ChatLanguageModel model = mock(ChatLanguageModel.class);
		when(model.generate(any(List.class))).thenReturn(Response.from(AiMessage.from("你好，我是助手")));
		return model;
	}
}
