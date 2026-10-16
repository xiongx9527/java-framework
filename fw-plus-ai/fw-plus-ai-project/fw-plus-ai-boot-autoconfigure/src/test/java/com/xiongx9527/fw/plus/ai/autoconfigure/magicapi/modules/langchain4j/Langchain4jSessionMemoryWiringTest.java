package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.LangChain4jSupportAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.InMemorySummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 会话/摘要配置接线测试：{@code fw.plus.ai.langchain4j.assistant.session.*} 必须真正落到
 * {@code Langchain4jModuleConfiguration} 装配出的 {@link SimpleAssistant} 上——
 * 否则配置全是死配置（类已建、无消费方）。
 */
class Langchain4jSessionMemoryWiringTest {

	private static final String ASSISTANT_PREFIX = "fw.plus.magicapi.langchain4j.simple-assistant";

	private static final String SESSION_PREFIX = "fw.plus.ai.langchain4j.assistant.session";

	private ChatModel mockChatModel() {
		ChatModel chatModel = mock(ChatModel.class);
		// MagicDynamicChatModel.doChat 直委托 getChatModel().doChat(request)：桩打在 doChat 上
		when(chatModel.doChat(any(ChatRequest.class)))
				.thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("你好，我是助手")).build());
		return chatModel;
	}

	private ApplicationContextRunner runner() {
		ChatModel chatModel = mockChatModel();
		Langchain4jChatModelModule chatModelModule = mock(Langchain4jChatModelModule.class);
		when(chatModelModule.getDynamicAttribute("my-assistant")).thenReturn(chatModel);
		return new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jModuleConfiguration.class,
						LangChain4jSupportAutoConfiguration.class))
				.withBean("chatModelModule", Langchain4jChatModelModule.class, () -> chatModelModule)
				.withPropertyValues(
						ASSISTANT_PREFIX + ".enabled=true",
						ASSISTANT_PREFIX + ".use-default-key=true",
						ASSISTANT_PREFIX + ".key=my-assistant");
	}

	@Test
	void defaultSummaryStoreBeanIsRegistered() {
		runner().run(context -> {
			assertThat(context).hasSingleBean(SummaryStore.class);
			assertThat(context.getBean(SummaryStore.class)).isInstanceOf(InMemorySummaryStore.class);
		});
	}

	@Test
	void summaryConfigActuallyDrivesSummarization() {
		runner()
				.withPropertyValues(
						SESSION_PREFIX + ".window-size=1",
						SESSION_PREFIX + ".summary.enabled=true",
						SESSION_PREFIX + ".summary.trigger-tokens=1",
						SESSION_PREFIX + ".summary.keep-tokens=50")
				.run(context -> {
					SimpleAssistant assistant = context.getBean(SimpleAssistant.class);
					assistant.dimensions("t1", "crm", "bot", "u1");
					assistant.session("s1");

					// 长于触发阈值的窗口外历史（window-size=1；写时维护在第二轮起产出摘要）
					StringBuilder longText = new StringBuilder();
					for (int i = 0; i < 200; i++) {
						longText.append('好');
					}
					assertThat(assistant.chat(longText.toString())).isEqualTo("你好，我是助手");
					assertThat(assistant.chat(longText.toString())).isEqualTo("你好，我是助手");

					// 会话级摘要已按 5 段会话键写入（证明 session.* 配置真正落到运行时）
					SummaryStore store = context.getBean(SummaryStore.class);
					assertThat(assistant.effectiveMemoryId()).isEqualTo("t1:crm:bot:u1:s1");
					assertThat(store.get("t1:crm:bot:u1:s1").getSummary()).isNotBlank();
				});
	}

	@Test
	void customSummaryStoreBeanOverridesDefault() {
		InMemorySummaryStore custom = new InMemorySummaryStore();
		runner()
				.withBean("mySummaryStore", SummaryStore.class, () -> custom)
				.run(context -> assertThat(context.getBean(SummaryStore.class)).isSameAs(custom));
	}

	@Test
	void sessionDisabledStillBuildsAssistantWithoutSummaryHooks() {
		runner()
				.withPropertyValues(SESSION_PREFIX + ".enabled=false")
				.run(context -> {
					assertThat(context).hasSingleBean(SimpleAssistant.class);
					assertThat(context.getBean(SimpleAssistant.class)
							.chat("session-1", "你好")).isEqualTo("你好，我是助手");
				});
	}

	@Test
	void chatModelTypeIsNotBrokenBySummaryModelResolution() {
		// summarize-model 留空 → 抽取式回退（不额外调 LLM），装配不应受影响
		runner()
				.withPropertyValues(SESSION_PREFIX + ".summary.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(SimpleAssistant.class);
					assertThat(context.getBean(SimpleAssistant.class)
							.chat("session-1", "你好")).isEqualTo("你好，我是助手");
				});
	}

}
