package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 助手配置契约测试：默认值、绑定、三层覆盖顺序（单次 > 业务 > 全局）、启动期 fail-fast 与运行期保旧值。
 */
class Langchain4jAssistantPropertiesTest {

	private static final String PREFIX = "fw.plus.ai.langchain4j.assistant";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	private static Langchain4jAssistantProperties properties() {
		return new Langchain4jAssistantProperties();
	}

	/** 把异常因果链上的信息拼起来，便于断言“指出具体配置项” */
	private static String describe(Throwable failure) {
		StringBuilder text = new StringBuilder();
		for (Throwable current = failure; current != null; current = current.getCause()) {
			text.append(current.getMessage()).append('\n');
		}
		return text.toString();
	}

	// -------------------------------------------------- 默认值与绑定

	@Test
	void defaultsMatchDocumentedValues() {
		contextRunner.run(context -> {
			Langchain4jAssistantProperties properties = context.getBean(Langchain4jAssistantProperties.class);
			assertThat(properties.getEnvelope().isEnabled()).isFalse();
			assertThat(properties.getStructured().isDefaultEnabled()).isFalse();
			assertThat(properties.getStructured().getMaxAttempts()).isEqualTo(3);
			assertThat(properties.getClassifier().isEnabled()).isTrue();
			assertThat(properties.getClassifier().getHistoryUserMessages()).isEqualTo(3);
			assertThat(properties.getClassifier().getMaxAttempts()).isEqualTo(1);
			assertThat(properties.getSession().getWindowSize()).isEqualTo(20);
		});
	}

	@Test
	void bindsCustomValues() {
		contextRunner.withPropertyValues(
				PREFIX + ".envelope.enabled=true",
				PREFIX + ".structured.default-enabled=true",
				PREFIX + ".structured.max-attempts=5",
				PREFIX + ".classifier.enabled=false",
				PREFIX + ".classifier.history-user-messages=0",
				PREFIX + ".classifier.max-attempts=2",
				PREFIX + ".session.window-size=50").run(context -> {
					Langchain4jAssistantProperties properties = context.getBean(Langchain4jAssistantProperties.class);
					assertThat(properties.getEnvelope().isEnabled()).isTrue();
					assertThat(properties.getStructured().isDefaultEnabled()).isTrue();
					assertThat(properties.getStructured().getMaxAttempts()).isEqualTo(5);
					assertThat(properties.getClassifier().isEnabled()).isFalse();
					assertThat(properties.getClassifier().getHistoryUserMessages()).isZero();
					assertThat(properties.getClassifier().getMaxAttempts()).isEqualTo(2);
					assertThat(properties.getSession().getWindowSize()).isEqualTo(50);
				});
	}

	// -------------------------------------------------- 三层覆盖顺序

	@Test
	void overrideOrderIsCallThenContractThenGlobal() {
		Langchain4jAssistantProperties properties = properties();
		Langchain4jAssistantProperties.Structured structured = properties.getStructured();

		// 全局默认 false：三层都没声明 → 不产出结构化结果
		assertThat(structured.resolveEnabled(null, null)).isFalse();
		// 业务契约覆盖全局
		assertThat(structured.resolveEnabled(null, Boolean.TRUE)).isTrue();
		// 单次调用覆盖业务契约
		assertThat(structured.resolveEnabled(Boolean.FALSE, Boolean.TRUE)).isFalse();
		assertThat(structured.resolveEnabled(Boolean.TRUE, Boolean.FALSE)).isTrue();

		// 全局默认打开：业务契约仍可覆盖
		structured.setDefaultEnabled(true);
		assertThat(structured.resolveEnabled(null, null)).isTrue();
		assertThat(structured.resolveEnabled(null, Boolean.FALSE)).isFalse();
		assertThat(structured.resolveEnabled(Boolean.FALSE, null)).isFalse();
	}

	// -------------------------------------------------- 启动期非法配置 fail-fast

	@Test
	void startupFailsFastOnUnrecognizableToggleValue() {
		contextRunner.withPropertyValues(PREFIX + ".envelope.enabled=maybe").run(context -> {
			assertThat(context).hasFailed();
			// 必须指出具体配置项，而不是笼统报错
			assertThat(describe(context.getStartupFailure())).contains(PREFIX + ".envelope.enabled");
		});
	}

	@Test
	void startupFailsFastOnRangeViolation() {
		contextRunner.withPropertyValues(PREFIX + ".structured.max-attempts=0").run(context -> {
			assertThat(context).hasFailed();
			assertThat(describe(context.getStartupFailure())).contains(PREFIX + ".structured.max-attempts");
		});
		contextRunner.withPropertyValues(PREFIX + ".classifier.max-attempts=0").run(context -> {
			assertThat(context).hasFailed();
			assertThat(describe(context.getStartupFailure())).contains(PREFIX + ".classifier.max-attempts");
		});
		contextRunner.withPropertyValues(PREFIX + ".classifier.history-user-messages=-1").run(context -> {
			assertThat(context).hasFailed();
			assertThat(describe(context.getStartupFailure())).contains(PREFIX + ".classifier.history-user-messages");
		});
		contextRunner.withPropertyValues(PREFIX + ".session.window-size=0").run(context -> {
			assertThat(context).hasFailed();
			assertThat(describe(context.getStartupFailure())).contains(PREFIX + ".session.window-size");
		});
	}

	@Test
	void validatePointsAtTheOffendingKey() {
		Langchain4jAssistantProperties properties = properties();
		properties.getStructured().setMaxAttempts(0);

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(PREFIX + ".structured.max-attempts");
	}

	// -------------------------------------------------- 运行期非法配置保旧值

	@Test
	void runtimeIllegalValueKeepsOldValueAndWarns() {
		Langchain4jAssistantProperties properties = properties();

		assertThat(properties.getStructured().trySetMaxAttempts(0)).isFalse();
		assertThat(properties.getStructured().getMaxAttempts()).isEqualTo(3);
		assertThat(properties.getClassifier().trySetMaxAttempts(0)).isFalse();
		assertThat(properties.getClassifier().getMaxAttempts()).isEqualTo(1);
		assertThat(properties.getClassifier().trySetHistoryUserMessages(-1)).isFalse();
		assertThat(properties.getClassifier().getHistoryUserMessages()).isEqualTo(3);
		assertThat(properties.getSession().trySetWindowSize(0)).isFalse();
		assertThat(properties.getSession().getWindowSize()).isEqualTo(20);

		// 合法值照常生效；分类历史条数 0 表示“只看本轮”，属合法值
		assertThat(properties.getStructured().trySetMaxAttempts(5)).isTrue();
		assertThat(properties.getStructured().getMaxAttempts()).isEqualTo(5);
		assertThat(properties.getClassifier().trySetHistoryUserMessages(0)).isTrue();
		assertThat(properties.getClassifier().getHistoryUserMessages()).isZero();
		assertThat(properties.getSession().trySetWindowSize(30)).isTrue();
		assertThat(properties.getSession().getWindowSize()).isEqualTo(30);
	}

	@EnableConfigurationProperties(Langchain4jAssistantProperties.class)
	private static class TestConfiguration {
	}
}
