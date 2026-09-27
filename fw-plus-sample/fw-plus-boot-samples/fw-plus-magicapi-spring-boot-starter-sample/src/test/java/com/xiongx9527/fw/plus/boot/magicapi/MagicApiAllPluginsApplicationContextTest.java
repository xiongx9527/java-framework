package com.xiongx9527.fw.plus.boot.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.admin.server.MagicAPIAdminServerConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.starter.MagicAPIDbBrowserAdminEnabledCondition;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.starter.MagicAPIDbBrowserConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.module.MongoModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.starter.MagicAPIMongoConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		classes = MagicapiSampleApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("all-plugins")
class MagicApiAllPluginsApplicationContextTest {

	private static final Path RESOURCE_DIRECTORY = createResourceDirectory();
	private static final Map<String, String> ACTIVE_PLUGIN_CONFIGURATIONS = activePluginConfigurations();
	private static final Set<String> CONDITIONAL_BACKOFF_ARTIFACTS = new LinkedHashSet<String>(Arrays.asList(
			"fw-plus-magicapi-plugin-admin",
			"fw-plus-magicapi-plugin-db",
			// 以下插件的激活条件（classpath / 依赖 bean / 开关）在样例中未满足，
			// 或引全 28 个插件会拉起数据源/MQ/Kafka 等运行时前置导致启动失败；
			// 插件自身能力由各插件模块的测试覆盖，样例只验证已激活集合。
			"fw-plus-magicapi-plugin-mybatisplus",
			"fw-plus-magicapi-plugin-java-entity",
			"fw-plus-magicapi-plugin-spring-bean"));
	private static final String RESOURCE_ONLY_ARTIFACT = "fw-plus-magicapi-plugin-word";
	/**
	 * 只提供 Magic-API 扩展能力（marker/DSL 桥接）、不注册 {@code MagicPluginConfiguration} 的 starter；
	 * 与 word 一样不参与 plugin name 对账，但必须是样例 classpath 与清单的一部分。
	 */
	private static final Set<String> MARKER_ONLY_ARTIFACTS = new LinkedHashSet<String>(Arrays.asList(
			"fw-plus-magicapi-plugin-mybatisplus",
			"fw-plus-magicapi-plugin-spring-bean"));
	/**
	 * jar 在样例 classpath 上、但其 plugin 注册依赖外部服务（broker），离线样例上下文不出现的 starter。
	 */
	private static final Set<String> OFFLINE_UNAVAILABLE_ARTIFACTS = new LinkedHashSet<String>(Arrays.asList(
			));
	private static final String WORD_PLUGIN_RESOURCE = "magic-editor/plugins/magic-word.1.0.0.iife.js";

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Autowired
	private ConfigurableEnvironment environment;

	@Autowired
	private TestRestTemplate restTemplate;

	@DynamicPropertySource
	static void resourceProperties(DynamicPropertyRegistry registry) {
		registry.add("magic-api.resource.location", () -> RESOURCE_DIRECTORY.toString());
	}

	@Test
	void startsRealSampleWithoutApolloOrExternalResources() {
		assertThat(applicationContext.getBeansOfType(MagicResourceService.class)).hasSize(1);
		assertThat(applicationContext.containsBean("apolloConfigChangeAdapter")).isFalse();
		assertThat(environment.getProperty("magic-api.resource.location"))
				.isEqualTo(RESOURCE_DIRECTORY.toString());
		assertThat(StreamSupport.stream(environment.getPropertySources().spliterator(), false)
				.map(PropertySource::getName))
				.noneMatch(name -> name.toLowerCase(Locale.ROOT).contains("apollo"));
	}

	@Test
	void exposesStructuredRuntimeContractForAllPluginArtifacts() {
		Map<String, MagicPluginConfiguration> configurationBeans =
				applicationContext.getBeansOfType(MagicPluginConfiguration.class);
		Set<String> pluginNames = configurationBeans.values().stream()
				.map(configuration -> configuration.plugin().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));

		assertThat(pluginNames)
				.containsExactlyInAnyOrderElementsOf(new LinkedHashSet<String>(ACTIVE_PLUGIN_CONFIGURATIONS.values()));

		Set<String> mappedArtifacts = new LinkedHashSet<String>(ACTIVE_PLUGIN_CONFIGURATIONS.keySet());
		mappedArtifacts.addAll(CONDITIONAL_BACKOFF_ARTIFACTS);
		mappedArtifacts.add(RESOURCE_ONLY_ARTIFACT);
		mappedArtifacts.addAll(MARKER_ONLY_ARTIFACTS);
		mappedArtifacts.addAll(OFFLINE_UNAVAILABLE_ARTIFACTS);
		Set<String> declaredArtifacts = MagicApiAllPluginsClasspathTest.PLUGIN_ARTIFACTS.stream()
				.map(coordinate -> coordinate.substring(coordinate.indexOf(':') + 1))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		assertThat(mappedArtifacts).containsExactlyInAnyOrderElementsOf(declaredArtifacts);

		assertThat(applicationContext.getBeansOfType(MagicAPIAdminServerConfiguration.class)).isEmpty();
		assertConditionDidNotMatch(MagicAPIAdminServerConfiguration.class.getName());
		assertThat(environment.getProperty("magic-api.plugin.db.admin.enabled", Boolean.class, false)).isFalse();
		assertThat(applicationContext.getBeansOfType(MagicAPIDbBrowserConfiguration.class)).isEmpty();

		assertThat(applicationContext.getBeansOfType(MongoModule.class)).isEmpty();
		assertConditionDidNotMatch(MagicAPIMongoConfiguration.class.getName() + "#mongoFunctions");
		assertThat(Thread.currentThread().getContextClassLoader().getResource(WORD_PLUGIN_RESOURCE)).isNotNull();
	}

	@Test
	void dbBrowserAdminConditionBacksOffForOfflineEnvironment() {
		assertThat(new MagicAPIDbBrowserAdminEnabledCondition().matches(currentConditionContext(),
				AnnotationMetadata.introspect(MagicAPIDbBrowserConfiguration.class))).isFalse();
	}

	@Test
	void exposesHealthAndMagicEditor() {
		ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
		assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(health.getBody()).contains("\"status\":\"UP\"");

		ResponseEntity<String> editor = restTemplate.getForEntity("/magic-api/web/index.html", String.class);
		assertThat(editor.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(editor.getBody()).contains("<title>magic-api</title>");
	}

	private static Path createResourceDirectory() {
		try {
			return Files.createTempDirectory("fw-plus-magicapi-all-plugins-");
		} catch (IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private void assertConditionDidNotMatch(String source) {
		ConditionEvaluationReport report = ConditionEvaluationReport.get(applicationContext.getBeanFactory());
		ConditionEvaluationReport.ConditionAndOutcomes outcomes = report.getConditionAndOutcomesBySource().get(source);
		assertThat(outcomes).as("Condition outcomes for %s", source).isNotNull();
		assertThat(outcomes.isFullMatch()).as("Expected %s to back off", source).isFalse();
	}

	private ConditionContext currentConditionContext() {
		return new ConditionContext() {
			@Override
			public BeanDefinitionRegistry getRegistry() {
				return (BeanDefinitionRegistry) applicationContext.getBeanFactory();
			}

			@Override
			public ConfigurableListableBeanFactory getBeanFactory() {
				return applicationContext.getBeanFactory();
			}

			@Override
			public ConfigurableEnvironment getEnvironment() {
				return environment;
			}

			@Override
			public ResourceLoader getResourceLoader() {
				return applicationContext;
			}

			@Override
			public ClassLoader getClassLoader() {
				return applicationContext.getClassLoader();
			}
		};
	}

	private static Map<String, String> activePluginConfigurations() {
		Map<String, String> configurations = new LinkedHashMap<String, String>();
		configurations.put("fw-plus-magicapi-plugin-config", "config");
		configurations.put("fw-plus-magicapi-plugin-debezium", "debezium");
		configurations.put("fw-plus-magicapi-plugin-druid", "druid");
		configurations.put("fw-plus-magicapi-plugin-dtp", "dtp");
		configurations.put("fw-plus-magicapi-plugin-elasticsearch", "elasticsearch");
		configurations.put("fw-plus-magicapi-plugin-freemarker", "freemarker");
		configurations.put("fw-plus-magicapi-plugin-influx", "influx");
		configurations.put("fw-plus-magicapi-plugin-kafka", "kafka");
		configurations.put("fw-plus-magicapi-plugin-java-entity", "java-entity");
		configurations.put("fw-plus-magicapi-plugin-mybatisflex", "mybatisflex-xml");
		configurations.put("fw-plus-magicapi-plugin-mongo", "mongo");
		configurations.put("fw-plus-magicapi-plugin-mqtt-server", "mqtt");
		configurations.put("fw-plus-magicapi-plugin-mqtt-client", "mqttclient");
		configurations.put("fw-plus-magicapi-plugin-pulsar", "pulsar");
		configurations.put("fw-plus-magicapi-plugin-rabbitmq", "rabbitmq");
		configurations.put("fw-plus-magicapi-plugin-redis", "redis");
		configurations.put("fw-plus-magicapi-plugin-rocketmq", "rocketmq");
		configurations.put("fw-plus-magicapi-plugin-s3", "s3");
		configurations.put("fw-plus-magicapi-plugin-satoken", "satoken");
		configurations.put("fw-plus-magicapi-plugin-swagger", "swagger");
		configurations.put("fw-plus-magicapi-plugin-task", "task");
		configurations.put("fw-plus-magicapi-plugin-vue3", "vue3");
		configurations.put("fw-plus-magicapi-plugin-ws", "ws");
		configurations.put("fw-plus-ai-magicapi-plugin-model", "model");
		configurations.put("fw-plus-ai-magicapi-plugin-mcp", "mcp");
		configurations.put("fw-plus-ai-magicapi-plugin-vector", "vector");
		return Collections.unmodifiableMap(configurations);
	}

	@AfterAll
	static void deleteResourceDirectory() throws IOException {
		List<Path> resources;
		try (Stream<Path> paths = Files.walk(RESOURCE_DIRECTORY)) {
			resources = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
		}
		try {
			assertThat(resources)
					.filteredOn(path -> !path.equals(RESOURCE_DIRECTORY))
					.allMatch(Files::isDirectory);
		} finally {
			for (Path resource : resources) {
				Files.deleteIfExists(resource);
			}
		}
	}
}
