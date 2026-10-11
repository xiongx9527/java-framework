package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 意图识别自动配置：默认装配工厂、enabled=false 不装配、属性绑定、可选模型/LLM 纳入。
 */
class LangChain4jIntentAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jIntentAutoConfiguration.class));

	@Test
	void registersFactoryByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(IntentClassifierFactory.class);
			assertThat(context).hasSingleBean(IntentProperties.class);
			IntentClassifierFactory factory = context.getBean(IntentClassifierFactory.class);
			assertThat(factory.hasEmbeddingModel()).isFalse();
			assertThat(factory.hasLlm()).isFalse();
		});
	}

	@Test
	void notRegisteredWhenDisabled() {
		contextRunner.withPropertyValues("fw.plus.ai.langchain4j.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(IntentClassifierFactory.class));
	}

	@Test
	void bindsConfiguredThresholds() {
		contextRunner.withPropertyValues(
						IntentProperties.PREFIX + ".embedding-threshold=0.5",
						IntentProperties.PREFIX + ".embedding-margin=0.1",
						IntentProperties.PREFIX + ".llm-confidence-threshold=0.7")
				.run(context -> {
					IntentProperties properties = context.getBean(IntentProperties.class);
					assertThat(properties.getEmbeddingThreshold()).isEqualTo(0.5d);
					assertThat(properties.getEmbeddingMargin()).isEqualTo(0.1d);
					assertThat(properties.getLlmConfidenceThreshold()).isEqualTo(0.7d);
				});
	}

	@Test
	void picksUpOptionalModels() {
		contextRunner.withUserConfiguration(ModelConfig.class)
				.run(context -> {
					IntentClassifierFactory factory = context.getBean(IntentClassifierFactory.class);
					assertThat(factory.hasEmbeddingModel()).isTrue();
					assertThat(factory.hasLlm()).isTrue();
				});
	}

	@Configuration
	static class ModelConfig {

		@Bean
		EmbeddingModel embeddingModel() {
			return mock(EmbeddingModel.class);
		}

		@Bean
		IntentLlm intentLlm() {
			return (system, user) -> "{\"intent\":\"NONE\",\"confidence\":0}";
		}
	}
}
