package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag.DifyContentRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag.DifyRagProperties;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@code dify-rag.enabled} 对 Dify 知识库检索器的条件装配：
 * 缺省不装配、配置开启装配、缺关键配置/非法段名快速失败、自定义 Bean 优先。
 */
class DifyRagAutoConfigurationTest {

	private static final String PREFIX = DifyRagProperties.PREFIX;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jSupportAutoConfiguration.class));

	@Test
	void notRegisteredByDefault() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ContentRetriever.class));
	}

	@Test
	void registersWhenEnabled() {
		contextRunner.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".base-url=http://127.0.0.1:8080",
						PREFIX + ".api-key=dataset-key",
						PREFIX + ".dataset-ids=ds-1,ds-2",
						PREFIX + ".pool.size=4",
						PREFIX + ".pool.thread-name-prefix=dify-pool")
				.run(context -> {
					assertThat(context).hasSingleBean(ContentRetriever.class);
					assertThat(context.getBean(ContentRetriever.class)).isInstanceOf(DifyContentRetriever.class);
					// 线程池与元数据过滤配置完成绑定
					DifyRagProperties properties = context.getBean(DifyRagProperties.class);
					assertThat(properties.getPool().getSize()).isEqualTo(4);
					assertThat(properties.getPool().getThreadNamePrefix()).isEqualTo("dify-pool");
					assertThat(properties.getMetaFields().isEnabled()).isFalse();
				});
	}

	@Test
	void failsFastWhenBaseUrlMissing() {
		contextRunner.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".api-key=dataset-key",
						PREFIX + ".dataset-ids=ds-1")
				.run(context -> assertThat(context).getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasStackTraceContaining("base-url"));
	}

	@Test
	void failsFastWhenDatasetIdsMissing() {
		contextRunner.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".base-url=http://127.0.0.1:8080",
						PREFIX + ".api-key=dataset-key")
				.run(context -> assertThat(context).getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasStackTraceContaining("dataset-ids"));
	}

	@Test
	void failsFastWhenIncludeSegmentInvalid() {
		// 段名拼错若等到检索期才暴露，会被降级吞掉 → 启动期快速失败
		contextRunner.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".base-url=http://127.0.0.1:8080",
						PREFIX + ".api-key=dataset-key",
						PREFIX + ".dataset-ids=ds-1",
						PREFIX + ".meta-fields.enabled=true",
						PREFIX + ".meta-fields.include=tenant,tenat")
				.run(context -> assertThat(context).getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasStackTraceContaining("meta-fields.include"));
	}

	@Test
	void customContentRetrieverBeanWins() {
		ContentRetriever custom = mock(ContentRetriever.class);
		contextRunner.withPropertyValues(
						PREFIX + ".enabled=true",
						PREFIX + ".base-url=http://127.0.0.1:8080",
						PREFIX + ".api-key=dataset-key",
						PREFIX + ".dataset-ids=ds-1")
				.withBean(ContentRetriever.class, () -> custom)
				.run(context -> assertThat(context.getBean(ContentRetriever.class)).isSameAs(custom));
	}
}
