package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.Langchain4jDatasourceConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.module.Langchain4jToolProviderModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.registry.Langchain4jToolProviderMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.resource.Langchain4jToolProviderMagicResourceStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Langchain4jToolProviderAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(Langchain4jToolProviderConfiguration.class))
			.withBean(MagicResourceService.class, () -> mock(MagicResourceService.class));

	@Test
	void shouldRegisterToolProviderInfrastructureWhenMagicResourceServicePresent() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MagicDynamicLangchain4jToolProviderDataSource.class);
			assertThat(context).hasSingleBean(MagicToolInvoker.class);
			assertThat(context).hasSingleBean(Langchain4jToolProviderMagicResourceStorage.class);
			assertThat(context).hasSingleBean(Langchain4jToolProviderMagicDynamicRegistry.class);
			assertThat(context).hasSingleBean(Langchain4jToolProviderModule.class);
			assertThat(context).hasSingleBean(MagicApiToolChangeListener.class);
		});
	}

	@Test
	void shouldSkipToolProviderWhenDisabled() {
		contextRunner.withPropertyValues("fw.plus.magicapi.langchain4j.tool-provider.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(MagicDynamicLangchain4jToolProviderDataSource.class));
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
	void shouldBeImportedByOuterDatasourceConfiguration() {
		Import importAnnotation = Langchain4jDatasourceConfiguration.class.getAnnotation(Import.class);

		assertThat(importAnnotation).isNotNull();
		assertThat(Arrays.stream(importAnnotation.value()).map(Class::getName))
				.contains(Langchain4jToolProviderConfiguration.class.getName());
	}
}
