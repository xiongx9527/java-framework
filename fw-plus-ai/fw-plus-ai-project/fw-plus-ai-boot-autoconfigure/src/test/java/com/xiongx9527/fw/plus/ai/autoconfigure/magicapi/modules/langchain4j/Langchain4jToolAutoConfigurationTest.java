package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool.Langchain4jToolConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool.Langchain4jToolProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool.MagicApiToolProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool.MagicToolInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool.ScriptManagerMagicToolInvoker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Langchain4jToolAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(Langchain4jToolConfiguration.class))
			.withBean(MagicResourceService.class, () -> mock(MagicResourceService.class));

	@Test
	void shouldRegisterToolProviderWhenMagicResourceServicePresent() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MagicApiToolProvider.class);
			assertThat(context).hasSingleBean(MagicToolInvoker.class);
			assertThat(context).hasSingleBean(Langchain4jToolProperties.class);
		});
	}

	@Test
	void shouldSkipToolProviderWhenDisabled() {
		contextRunner.withPropertyValues("fw.plus.magicapi.langchain4j.tool.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(MagicApiToolProvider.class));
	}

	@Test
	void shouldSkipToolProviderWhenMagicResourceServiceAbsent() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Langchain4jToolConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(MagicApiToolProvider.class));
	}

	@Test
	void shouldAllowCustomInvokerOverride() {
		contextRunner.withBean(MagicToolInvoker.class, () -> mock(MagicToolInvoker.class))
				.run(context -> {
					assertThat(context).hasSingleBean(MagicToolInvoker.class);
					assertThat(context).getBean(MagicToolInvoker.class)
							.isNotInstanceOf(ScriptManagerMagicToolInvoker.class);
				});
	}

	@Test
	void shouldBeImportedByOuterLangchain4jConfiguration() {
		Import importAnnotation = Langchain4jConfiguration.class.getAnnotation(Import.class);

		assertThat(importAnnotation).isNotNull();
		assertThat(Arrays.stream(importAnnotation.value()).map(Class::getName))
				.contains(Langchain4jToolConfiguration.class.getName());
	}
}
