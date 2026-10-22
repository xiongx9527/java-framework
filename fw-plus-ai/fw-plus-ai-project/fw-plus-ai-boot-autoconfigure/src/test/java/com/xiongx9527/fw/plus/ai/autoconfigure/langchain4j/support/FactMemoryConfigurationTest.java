package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts.FactStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts.Mem0FactStore;
import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 4.1：跨会话事实存储的 Spring 装配（{@code FactMemoryConfiguration}）——
 * 关闭不注册、开启注册独立 mem0 {@link FactStore}、base-url 缺失启动快速失败、
 * 自定义 {@code FactStore} Bean 优先（无需 base-url）。
 */
class FactMemoryConfigurationTest {

	private static final String PREFIX = Langchain4jAssistantProperties.PREFIX;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jSupportAutoConfiguration.class));

	@Test
	void disabledFactsRegistersNoStore() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(FactStore.class));
	}

	@Test
	void enabledWithoutBaseUrlFailsFast() {
		contextRunner.withPropertyValues(PREFIX + ".session.facts.enabled=true").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasStackTraceContaining("base-url");
		});
	}

	@Test
	void enabledWiresMem0FactStore() {
		contextRunner.withPropertyValues(
				PREFIX + ".session.facts.enabled=true",
				PREFIX + ".session.facts.base-url=http://localhost:8000",
				PREFIX + ".session.facts.api-key=secret").run(context -> {
			assertThat(context).hasSingleBean(FactStore.class);
			assertThat(context.getBean(FactStore.class)).isInstanceOf(Mem0FactStore.class);
		});
	}

	@Test
	void customStoreBeanWinsWithoutBaseUrl() {
		contextRunner.withBean(FactStore.class, StubFactStore::new)
				.withPropertyValues(PREFIX + ".session.facts.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(FactStore.class);
					assertThat(context.getBean(FactStore.class)).isInstanceOf(StubFactStore.class);
				});
	}

	/** 自定义 FactStore 覆盖用桩 */
	static final class StubFactStore implements FactStore {

		@Override
		public void addDialogue(List<ChatMessage> turnMessages, String userId) {
		}

		@Override
		public void addFacts(List<String> facts, String userId) {
		}

		@Override
		public List<String> search(String query, String userId, int topK) {
			return new ArrayList<String>();
		}

		@Override
		public List<String> listAll(String userId, int limit) {
			return new ArrayList<String>();
		}
	}

}
