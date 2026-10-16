package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code session.facts.*} 属性绑定（任务 1.3）：默认值、显式覆盖与非法值快速失败。
 * 默认值依据正式设计 §6（`enabled=false` / `mem0` / `per-turn` / `semantic` / `extract-async=true` 等）。
 */
class FactsPropertiesConfigTest {

	private static final String PREFIX = "fw.plus.ai.langchain4j.assistant.session.facts";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@EnableConfigurationProperties(Langchain4jAssistantProperties.class)
	static class TestConfiguration {
	}

	@Test
	void defaultsMatchDesign() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			Langchain4jAssistantProperties.Session.Facts facts = facts(context);
			assertThat(facts.isEnabled()).isFalse();
			assertThat(facts.getBaseUrl()).isNull();
			assertThat(facts.getApiKey()).isEmpty();
			assertThat(facts.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
			assertThat(facts.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
			assertThat(facts.getExtractor()).isEqualTo("mem0");
			assertThat(facts.getWriteMode()).isEqualTo("per-turn");
			assertThat(facts.getBatchSize()).isEqualTo(5);
			assertThat(facts.getRetriever()).isEqualTo("semantic");
			assertThat(facts.getTopK()).isEqualTo(5);
			assertThat(facts.getMaxFactsTokens()).isEqualTo(500);
			assertThat(facts.isExtractAsync()).isTrue();
			assertThat(facts.getExtractModel()).isNull();
			assertThat(facts.getRerankModel()).isNull();
		});
	}

	@Test
	void explicitOverridesBind() {
		contextRunner.withPropertyValues(
				PREFIX + ".enabled=true",
				PREFIX + ".base-url=http://localhost:8899",
				PREFIX + ".api-key=secret-1",
				PREFIX + ".connect-timeout=2s",
				PREFIX + ".read-timeout=3s",
				PREFIX + ".extractor=rule",
				PREFIX + ".write-mode=batched",
				PREFIX + ".batch-size=3",
				PREFIX + ".retriever=rerank",
				PREFIX + ".top-k=8",
				PREFIX + ".max-facts-tokens=300",
				PREFIX + ".extract-async=false",
				PREFIX + ".extract-model=key-chat",
				PREFIX + ".rerank-model=key-score").run(context -> {
					assertThat(context).hasNotFailed();
					Langchain4jAssistantProperties.Session.Facts facts = facts(context);
					assertThat(facts.isEnabled()).isTrue();
					assertThat(facts.getBaseUrl()).isEqualTo("http://localhost:8899");
					assertThat(facts.getApiKey()).isEqualTo("secret-1");
					assertThat(facts.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
					assertThat(facts.getReadTimeout()).isEqualTo(Duration.ofSeconds(3));
					assertThat(facts.getExtractor()).isEqualTo("rule");
					assertThat(facts.getWriteMode()).isEqualTo("batched");
					assertThat(facts.getBatchSize()).isEqualTo(3);
					assertThat(facts.getRetriever()).isEqualTo("rerank");
					assertThat(facts.getTopK()).isEqualTo(8);
					assertThat(facts.getMaxFactsTokens()).isEqualTo(300);
					assertThat(facts.isExtractAsync()).isFalse();
					assertThat(facts.getExtractModel()).isEqualTo("key-chat");
					assertThat(facts.getRerankModel()).isEqualTo("key-score");
				});
	}

	@Test
	void invalidValuesFailFast() {
		assertRejected(PREFIX + ".top-k=0", "session.facts.top-k");
		assertRejected(PREFIX + ".batch-size=0", "session.facts.batch-size");
		assertRejected(PREFIX + ".max-facts-tokens=0", "session.facts.max-facts-tokens");
		assertRejected(PREFIX + ".extractor=bogus", "session.facts.extractor");
		assertRejected(PREFIX + ".write-mode=bogus", "session.facts.write-mode");
		assertRejected(PREFIX + ".retriever=bogus", "session.facts.retriever");
		assertRejected(PREFIX + ".connect-timeout=0s", "session.facts.connect-timeout");
		assertRejected(PREFIX + ".read-timeout=0s", "session.facts.read-timeout");
	}

	// -------------------------------------------------- 基础设施

	private void assertRejected(String property, String expectedKey) {
		contextRunner.withPropertyValues(property).run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasStackTraceContaining(expectedKey);
		});
	}

	private static Langchain4jAssistantProperties.Session.Facts facts(
			org.springframework.context.ApplicationContext context) {
		return context.getBean(Langchain4jAssistantProperties.class).getSession().getFacts();
	}
}
