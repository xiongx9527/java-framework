package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.LangChain4jSupportAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.InMemorySummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

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

	private ChatLanguageModel mockChatModel() {
		ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
		when(chatModel.generate(any(List.class))).thenReturn(Response.from(AiMessage.from("你好，我是助手")));
		return chatModel;
	}

	private ApplicationContextRunner runner() {
		ChatLanguageModel chatModel = mockChatModel();
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

	/**
	 * 容器 {@link ChatMemoryStore} 必须真正接到 {@code simpleAssistant} 的会话记忆上。
	 * 未接线时 builder 会自带一份进程内 store——{@code chat-memory.type=mem0} 配置失效、
	 * 与 envelope 通道各写各的历史、重启即丢。
	 */
	@Test
	void containerChatMemoryStoreIsWiredIntoSimpleAssistant() {
		InMemoryChatMemoryStore containerStore = new InMemoryChatMemoryStore();
		runner()
				.withBean("containerChatMemoryStore", ChatMemoryStore.class, () -> containerStore)
				.run(context -> {
					SimpleAssistant assistant = context.getBean(SimpleAssistant.class);
					assertThat(assistant.chat("s-wire-1", "你好")).isEqualTo("你好，我是助手");
					// 消息落进容器 store；未接线时这里为空（历史写进了 builder 自带的 store）
					assertThat(containerStore.getMessages("s-wire-1")).isNotEmpty();
				});
	}

}
