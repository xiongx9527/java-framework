package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.Mem0ChatMemoryStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code chat-memory.type} 对 {@link ChatMemoryStore} 实现的选择：
 * 默认 in-memory 兜底、type=mem0 装配 mem0、使用方自定义 Bean 优先。
 */
class LangChain4jSupportChatMemoryStoreSelectionTest {

	private static final String PREFIX = "fw.plus.ai.langchain4j.chat-memory";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jSupportAutoConfiguration.class));

	@Test
	void defaultsToInMemoryStore() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ChatMemoryStore.class);
			assertThat(context.getBean(ChatMemoryStore.class)).isInstanceOf(InMemoryChatMemoryStore.class);
		});
	}

	@Test
	void mem0StoreWhenTypeConfigured() {
		contextRunner.withPropertyValues(PREFIX + ".type=mem0")
				.run(context -> {
					assertThat(context).hasSingleBean(ChatMemoryStore.class);
					assertThat(context.getBean(ChatMemoryStore.class)).isInstanceOf(Mem0ChatMemoryStore.class);
				});
	}

	@Test
	void explicitInMemoryTypeKeepsInMemory() {
		contextRunner.withPropertyValues(PREFIX + ".type=in-memory")
				.run(context -> assertThat(context.getBean(ChatMemoryStore.class))
						.isInstanceOf(InMemoryChatMemoryStore.class));
	}

	@Test
	void customStoreBeanWins() {
		InMemoryChatMemoryStore custom = new InMemoryChatMemoryStore();
		contextRunner.withPropertyValues(PREFIX + ".type=mem0")
				.withBean(ChatMemoryStore.class, () -> custom)
				.run(context -> {
					assertThat(context).hasSingleBean(ChatMemoryStore.class);
					assertThat(context.getBean(ChatMemoryStore.class)).isSameAs(custom);
				});
	}

	@Test
	void mem0WithoutBaseUrlFailsFast() {
		contextRunner.withPropertyValues(PREFIX + ".type=mem0", PREFIX + ".mem0.base-url=")
				.run(context -> assertThat(context).getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class));
	}

	@Test
	void unknownTypeFailsFast() {
		// 旧配置残留（type=redis/jdbc 等）：启动即失败，避免 ChatMemoryStore 静默缺失
		contextRunner.withPropertyValues(PREFIX + ".type=redis")
				.run(context -> assertThat(context).getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasStackTraceContaining("仅支持")
						.hasStackTraceContaining("in-memory"));
	}
}
