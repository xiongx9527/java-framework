package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.starter;

import com.mongodb.client.MongoClient;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.module.MongoModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.service.MongoDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.service.MongoDataSourceMagicDynamicRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.ssssssss.magicapi.mongo.MongoFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIMongoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					MagicAPIMongoConfiguration.class, MagicAPIMongoModuleAutoConfiguration.class));
	private final WebApplicationContextRunner defaultMongoContextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class,
					MagicAPIMongoModuleAutoConfiguration.class, MongoAutoConfiguration.class,
					MongoDataAutoConfiguration.class))
			.withInitializer(context -> context.addBeanFactoryPostProcessor(
					new LazyInitializationBeanFactoryPostProcessor()))
			.withBean(MongoClient.class, () -> mock(MongoClient.class));

	@Test
	void registersMongoModuleWithDefaultMongoAutoConfiguration() {
		defaultMongoContextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(MongoTemplate.class);
			assertThat(context).hasSingleBean(MongoModule.class);
		});
	}

	@Test
	void registersPluginInfrastructureWithoutDefaultMongoTemplate() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(MagicAPIMongoConfiguration.class);
			assertThat(context.getBean(MagicAPIMongoConfiguration.class).plugin().getName()).isEqualTo("mongo");
			assertThat(context).hasSingleBean(MagicDynamicMongoDataSource.class);
			assertThat(context).hasSingleBean(MongoDataSourceInfoMagicResourceStorage.class);
			assertThat(context).hasSingleBean(MongoDataSourceMagicDynamicRegistry.class);
			assertThat(context).hasSingleBean(MongoFunction.class);
			assertThat(context).doesNotHaveBean(MongoTemplate.class);
			assertThat(context).doesNotHaveBean(MongoModule.class);
		});
	}
}
