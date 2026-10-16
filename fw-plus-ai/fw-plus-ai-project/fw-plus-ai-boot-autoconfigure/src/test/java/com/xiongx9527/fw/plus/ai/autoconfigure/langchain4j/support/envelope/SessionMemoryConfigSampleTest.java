package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用文档（{@code docs/openspec/changes/langchain4j-session-memory/usage.md} §5）中的配置样例
 * 可按属性绑定加载——防止文档与实现漂移。
 */
class SessionMemoryConfigSampleTest {

	private static final String PREFIX = "fw.plus.ai.langchain4j.assistant";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@EnableConfigurationProperties(Langchain4jAssistantProperties.class)
	static class TestConfiguration {
	}

	@Test
	void documentedSampleBinds() {
		contextRunner.withPropertyValues(
				PREFIX + ".session.enabled=true",
				PREFIX + ".session.window-size=20",
				PREFIX + ".session.session-id-resolver=daily",
				PREFIX + ".session.idle-timeout-minutes=30",
				PREFIX + ".session.context-strategy=memory-and-enriched",
				PREFIX + ".session.summary.enabled=true",
				PREFIX + ".session.summary.strategy=refine",
				PREFIX + ".session.summary.form=hybrid",
				PREFIX + ".session.summary.trigger-tokens=4000",
				PREFIX + ".session.summary.keep-tokens=3000",
				PREFIX + ".session.summary.max-summary-tokens=800",
				PREFIX + ".session.summary.rebuild-every=10",
				PREFIX + ".session.summary.summarize-model=").run(context -> {
					assertThat(context).hasNotFailed();
					Langchain4jAssistantProperties p = context.getBean(Langchain4jAssistantProperties.class);
					assertThat(p.getSession().isEnabled()).isTrue();
					assertThat(p.getSession().getSessionIdResolver()).isEqualTo("daily");
					assertThat(p.getSession().getIdleTimeoutMinutes()).isEqualTo(30);
					assertThat(p.getSession().getContextStrategy()).isEqualTo("memory-and-enriched");
					assertThat(p.getSession().getSummary().isEnabled()).isTrue();
					assertThat(p.getSession().getSummary().getStrategy()).isEqualTo("refine");
					assertThat(p.getSession().getSummary().getForm()).isEqualTo("hybrid");
					assertThat(p.getSession().getSummary().getRebuildEvery()).isEqualTo(10);
				});
	}
}
