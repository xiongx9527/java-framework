package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TinyEngineAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class));

	private final ApplicationContextRunner nonWebContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class));

	private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class));

	@Test
	void disabledByDefaultDoesNotRegisterTinyEngineWebBeans() {
		contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(TinyEngineResourceController.class);
			assertThat(context).doesNotHaveBean(TinyEngineUserContext.class);
		});
	}

	@Test
	void enabledRegistersDefaultSupportBeans() {
		contextRunner.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(TinyEngineUserContext.class));
	}

	@Test
	void enabledRegistersControllerWhenResourceServiceExists() {
		contextRunner.withUserConfiguration(ResourceServiceConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(TinyEngineResourceController.class));
	}

	@Test
	void reactiveWebApplicationRegistersControllerWhenResourceServiceExists() {
		reactiveContextRunner.withUserConfiguration(ResourceServiceConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(TinyEngineResourceController.class));
	}

	@Test
	void hostResourceServiceWinsWhenSqlSessionFactoryExists() {
		contextRunner.withUserConfiguration(SqlSessionFactoryConfiguration.class, ResourceServiceConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(ResourceService.class);
					assertThat(context).getBean(ResourceService.class).isSameAs(context.getBean("resourceService"));
				});
	}

	@Test
	void hostResourceControllerWinsEvenWhenBeanNameDiffers() {
		contextRunner.withUserConfiguration(SqlSessionFactoryConfiguration.class, HostResourceControllerConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(TinyEngineResourceController.class);
					assertThat(context).getBean(TinyEngineResourceController.class)
							.isSameAs(context.getBean("customTinyEngineResourceController"));
				});
	}

	@Test
	void webDisabledDoesNotRegisterController() {
		contextRunner.withPropertyValues(
							"fw.plus.tiny-engine.enabled=true",
							"fw.plus.tiny-engine.web.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(TinyEngineResourceController.class));
	}

	@Test
	void missingCoreClassDoesNotRegisterTinyEngineBeans() {
		contextRunner.withClassLoader(new FilteredClassLoader(ResourceService.class))
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> assertThat(context).doesNotHaveBean(TinyEngineUserContext.class));
	}

	@Test
	void hostUserContextWins() {
		contextRunner.withUserConfiguration(HostTinyEngineConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(TinyEngineUserContext.class));
	}

	@Test
	void mapperScannerUsesTinyEngineMapperPackage() {
		contextRunner.withUserConfiguration(SqlSessionFactoryConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(MapperScannerConfigurer.class);
					MapperScannerConfigurer mapperScannerConfigurer = context.getBean(MapperScannerConfigurer.class);
					assertThat(ReflectionTestUtils.getField(mapperScannerConfigurer, "basePackage"))
							.isEqualTo("com.xiongx9527.fw.plus.tinyengine.mapper");
				});
	}

	@Test
	void mapperScannerBacksOffWithoutSqlSessionFactory() {
		contextRunner.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> assertThat(context).doesNotHaveBean(MapperScannerConfigurer.class));
	}

	@Test
	void enabledRegistersCoreServicesWhenSqlSessionFactoryExists() {
		contextRunner.withUserConfiguration(SqlSessionFactoryConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(ResourceService.class);
					assertThat(context).hasSingleBean(TinyEngineResourceController.class);
				});
	}

	@Test
	void mapperDisabledDoesNotRegisterDefaultCoreServicesOrController() {
		contextRunner.withUserConfiguration(SqlSessionFactoryConfiguration.class)
				.withPropertyValues(
						"fw.plus.tiny-engine.enabled=true",
						"fw.plus.tiny-engine.mapper.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(ResourceService.class);
					assertThat(context).doesNotHaveBean(TinyEngineResourceController.class);
				});
	}

	@Test
	void nonServletApplicationDoesNotRegisterTinyEngineBeans() {
		nonWebContextRunner.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> {
					assertThat(context).doesNotHaveBean(TinyEngineResourceController.class);
					assertThat(context).doesNotHaveBean(TinyEngineUserContext.class);
					assertThat(context).doesNotHaveBean(MapperScannerConfigurer.class);
					assertThat(context).doesNotHaveBean(TinyEngineResourceConfiguration.TinyEngineSqlResources.class);
				});
	}

	@Test
	void mapperLocationsAreMergedWithHostLocations() {
		contextRunner.withPropertyValues("fw.plus.tiny-engine.enabled=true")
				.run(context -> {
					MybatisPlusPropertiesCustomizer customizer = context.getBean(MybatisPlusPropertiesCustomizer.class);
					MybatisPlusProperties properties = new MybatisPlusProperties();
					properties.setMapperLocations(new String[] {"classpath:/mapper/host/*.xml"});
					properties.setTypeHandlersPackage("com.example.host.typehandler");

					customizer.customize(properties);

					assertThat(properties.getMapperLocations())
							.contains("classpath:/mapper/host/*.xml", "classpath*:mapper/tinyengine/**/*.xml");
					assertThat(properties.getTypeHandlersPackage())
							.isEqualTo("com.example.host.typehandler");
				});
	}

	@Test
	void sqlResourcesAreDiscoverableButNotAutoInitializedByDefault() {
		contextRunner.withPropertyValues(
						"fw.plus.tiny-engine.enabled=true",
						"fw.plus.tiny-engine.sql.database=h2")
				.run(context -> {
					assertThat(context).hasSingleBean(TinyEngineResourceConfiguration.TinyEngineSqlResources.class);
					TinyEngineResourceConfiguration.TinyEngineSqlResources resources =
							context.getBean(TinyEngineResourceConfiguration.TinyEngineSqlResources.class);
					assertThat(resources.isAutoInitialize()).isFalse();
					assertThat(resources.getManifest().exists()).isTrue();
					assertThat(resources.getScripts()).allMatch(TinyEngineAutoConfigurationTest::resourceExists);
					assertThat(resources.getScripts()).isNotEmpty();
				});
	}

	@Test
	void missingSqlManifestBacksOffWhenAutoInitializeDisabled() {
		contextRunner.withPropertyValues(
						"fw.plus.tiny-engine.enabled=true",
						"fw.plus.tiny-engine.sql.manifest-location=classpath:missing-tiny-engine-sql.properties")
				.run(context -> {
					assertThat(context).hasSingleBean(TinyEngineResourceConfiguration.TinyEngineSqlResources.class);
					TinyEngineResourceConfiguration.TinyEngineSqlResources resources =
							context.getBean(TinyEngineResourceConfiguration.TinyEngineSqlResources.class);
					assertThat(resources.isAutoInitialize()).isFalse();
					assertThat(resources.getManifest().exists()).isFalse();
					assertThat(resources.getScripts()).isEmpty();
				});
	}

	@Test
	void missingSqlManifestFailsWhenAutoInitializeEnabled() {
		contextRunner.withPropertyValues(
						"fw.plus.tiny-engine.enabled=true",
						"fw.plus.tiny-engine.sql.auto-initialize=true",
						"fw.plus.tiny-engine.sql.manifest-location=classpath:missing-tiny-engine-sql.properties")
				.run(context -> assertThat(context).hasFailed());
	}

	private static boolean resourceExists(Resource resource) {
		try {
			return resource.exists();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Configuration
	static class HostTinyEngineConfiguration {

		@Bean
		TinyEngineUserContext tinyEngineUserContext() {
			return new TinyEngineUserContext() {
				@Override
				public TinyEngineUser getCurrentUser() {
					return null;
				}
			};
		}
	}

	@Configuration
	static class ResourceServiceConfiguration {

		@Bean
		ResourceService resourceService() {
			return mock(ResourceService.class);
		}
	}

	@Configuration
	static class HostResourceControllerConfiguration {

		@Bean
		TinyEngineResourceController customTinyEngineResourceController(ResourceService resourceService) {
			return new TinyEngineResourceController(resourceService);
		}
	}

	@Configuration
	static class SqlSessionFactoryConfiguration {

		@Bean
		SqlSessionFactory sqlSessionFactory() {
			org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
			configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), mock(DataSource.class)));
			return new DefaultSqlSessionFactory(configuration);
		}
	}
}
