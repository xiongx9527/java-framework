package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LangChain4jTinyEnginePluginAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jTinyEnginePluginAutoConfiguration.class));

	@Test
	void shouldRegisterLangChain4jTinyEngineProviderWhenPluginIsOnClasspath() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineAiProvider.class);
			assertThat(context).hasSingleBean(LangChain4jTinyEngineAiProvider.class);
			assertThat(context.getBean(TinyEngineAiProvider.class).getKey()).isEqualTo("langchain4j");
		});
	}

	@Test
	void shouldNotRegisterProviderWhenPluginIsDisabled() {
		contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.plugin.langchain4j.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(TinyEngineAiProvider.class));
	}

	@Test
	void shouldExposePluginAutoConfigurationFromSpringFactories() {
		List<String> autoConfigurations = SpringFactoriesLoader.loadFactoryNames(
				EnableAutoConfiguration.class, getClass().getClassLoader());

		assertThat(autoConfigurations).contains(
				"com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.plugin.langchain4j.LangChain4jTinyEnginePluginAutoConfiguration");
	}
}
