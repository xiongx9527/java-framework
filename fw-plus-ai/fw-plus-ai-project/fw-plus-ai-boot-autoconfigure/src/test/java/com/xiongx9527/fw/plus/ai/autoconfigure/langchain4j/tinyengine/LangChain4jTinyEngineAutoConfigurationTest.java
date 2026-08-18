package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class LangChain4jTinyEngineAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jTinyEngineAutoConfiguration.class));

	private final ApplicationContextRunner adapterEntryPointContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jAdapterAutoConfiguration.class));

	@Test
	void productionAdapterEntryPointShouldRegisterTinyEngineProvider() {
		adapterEntryPointContextRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineAiProvider.class);
			assertThat(context.getBean(TinyEngineAiProvider.class).getKey()).isEqualTo("langchain4j");
		});
	}

	@Test
	void productionAdapterEntryPointShouldHonorDisabledAdapter() {
		adapterEntryPointContextRunner
				.withPropertyValues("fw.plus.ai.langchain4j.adapter.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(TinyEngineAiProvider.class));
	}

	@Test
	void shouldRegisterLangChain4jTinyEngineProviderWhenAdapterIsOnClasspath() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineAiProvider.class);
			assertThat(context).hasSingleBean(LangChain4jTinyEngineAiProvider.class);
			assertThat(context.getBean(TinyEngineAiProvider.class).getKey()).isEqualTo("langchain4j");
		});
	}

	@Test
	void shouldNotRegisterProviderWhenAdapterIsDisabled() {
		contextRunner
				.withPropertyValues("fw.plus.ai.langchain4j.adapter.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(TinyEngineAiProvider.class));
	}

	@Test
	void shouldImportTinyEngineBridgeFromLangChain4jAdapterConfiguration() {
		Import importAnnotation = LangChain4jAdapterAutoConfiguration.class.getAnnotation(Import.class);
		assertThat(importAnnotation).isNotNull();
		assertThat(importAnnotation.value()).contains(LangChain4jTinyEngineAutoConfiguration.class);
	}
}
