package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.MagicSwaggerConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ClassUtils;
import org.springframework.util.StreamUtils;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * swagger 插件 webflux 栈支线契约测试：
 * 文档端点注册与 HTTP 访问、Swagger 2.0 文档生成、location 覆盖、开关、
 * 控制台插件清单、两条栈支线互斥、自动配置引导登记、knife4j webflux UI 依赖。
 */
class MagicSwaggerWebFluxTest {

	private static final String SWAGGER_LOCATION = "/v2/api-docs/magic-api/swagger2.json";

	private static ReactiveWebApplicationContextRunner reactiveRunner(String... extraProperties) {
		String[] baseProperties = {"magic-api.resource.location=target/magic-api-swagger-webflux-test"};
		String[] allProperties = new String[baseProperties.length + extraProperties.length];
		System.arraycopy(baseProperties, 0, allProperties, 0, baseProperties.length);
		System.arraycopy(extraProperties, 0, allProperties, baseProperties.length, extraProperties.length);
		return new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.class,
						com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxAutoConfiguration.class,
						MagicSwaggerWebFluxConfiguration.class))
				.withUserConfiguration(SpringDocStubConfiguration.class)
				.withPropertyValues(allProperties);
	}

	@Test
	void registersSwaggerDocEndpointInReactiveHandlerMapping() {
		reactiveRunner().run(context -> {
			RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping",
					RequestMappingHandlerMapping.class);
			assertThat(mapping.getHandlerMethods().keySet())
					.anyMatch(info -> hasPath(info, SWAGGER_LOCATION));
		});
	}

	@Test
	void servesSwaggerDocumentOverHttpInReactiveApplication() {
		reactiveRunner().run(context -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
			client.get().uri(SWAGGER_LOCATION)
					.exchange()
					.expectStatus().isOk()
					.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
					.expectBody()
					.jsonPath("$.swagger").isEqualTo("2.0")
					.jsonPath("$.info.title").isEqualTo("MagicAPI Swagger Docs");
		});
	}

	@Test
	void buildsSwaggerDocumentFromReactiveRegistry() {
		reactiveRunner().run(context -> {
			MagicSwaggerWebFluxController controller = new MagicSwaggerWebFluxController(
					context.getBean(SwaggerConfig.class),
					context.getBean(MagicResourceService.class),
					context.getBean(MagicAPIProperties.class),
					context);
			SwaggerEntity document = controller.swaggerJson();
			assertThat(document.getSwagger()).isEqualTo("2.0");
			assertThat(document.getInfo()).isNotNull();
			assertThat(document.getBasePath()).isEmpty();
		});
	}

	@Test
	void honorsCustomSwaggerLocationInReactiveApplication() {
		reactiveRunner("magic-api.swagger.location=/custom/magic-docs.json").run(context -> {
			RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping",
					RequestMappingHandlerMapping.class);
			assertThat(mapping.getHandlerMethods().keySet())
					.anyMatch(info -> hasPath(info, "/custom/magic-docs.json"))
					.noneMatch(info -> hasPath(info, SWAGGER_LOCATION));
		});
	}

	@Test
	void registersNoSwaggerEndpointWhenPluginDisabled() {
		reactiveRunner("magic-api.plugin.swagger.enabled=false").run(context -> {
			assertThat(context).doesNotHaveBean(MagicSwaggerWebFluxConfiguration.class);
			RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping",
					RequestMappingHandlerMapping.class);
			assertThat(mapping.getHandlerMethods().keySet())
					.noneMatch(info -> hasPath(info, SWAGGER_LOCATION));
		});
	}

	@Test
	void exposesSwaggerPluginInWorkbenchPluginsResponse() {
		reactiveRunner().run(context -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
			client.get().uri("/magic-api/web/plugins")
					.exchange()
					.expectStatus().isOk()
					.expectBody(String.class)
					.value(body -> assertThat(body).contains("\"name\":\"swagger\""));
		});
	}

	@Test
	void collectsSwaggerPluginDescriptorForWebfluxConsole() {
		reactiveRunner().run(context -> assertThat(
				context.getBean(MagicSwaggerWebFluxConfiguration.class).plugin().getName()).isEqualTo("swagger"));
	}

	@Test
	void keepsSwaggerWebFluxConfigurationOutOfServletApplications() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MagicSwaggerWebFluxConfiguration.class))
				.withUserConfiguration(SpringDocStubConfiguration.class)
				.run(context -> assertThat(context)
						.doesNotHaveBean(MagicSwaggerWebFluxConfiguration.class));
	}

	@Test
	void keepsServletSwaggerConfigurationOutOfReactiveApplications() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MagicSwaggerConfiguration.class))
				.withUserConfiguration(SpringDocStubConfiguration.class)
				.run(context -> assertThat(context)
						.doesNotHaveBean(MagicSwaggerConfiguration.class));
	}

	@Test
	void autoConfigurationImportsRegisterWebfluxSwaggerConfig() throws Exception {
		ClassPathResource imports = new ClassPathResource(
				"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
		assertThat(imports.exists()).isTrue();
		try (InputStream in = imports.getInputStream()) {
			String content = new String(StreamUtils.copyToByteArray(in), StandardCharsets.UTF_8);
			assertThat(content).contains(MagicSwaggerWebFluxConfiguration.class.getName());
		}
	}

	@Test
	void knife4jWebfluxUiDependencyIsOnClasspath() {
		// org.springdoc.webflux.ui.SwaggerConfig 仅存在于 springdoc-openapi-webflux-ui（knife4j webflux 变体传递引入）
		assertThat(ClassUtils.isPresent("org.springdoc.webflux.ui.SwaggerConfig", getClass().getClassLoader()))
				.isTrue();
		assertThat(getClass().getClassLoader().getResource("META-INF/resources/doc.html")).isNotNull();
	}

	@Test
	void servesKnife4jDocHtmlInReactiveApplication() {
		reactiveRunner().run(context -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
			client.get().uri("/doc.html")
					.exchange()
					.expectStatus().isOk();
		});
	}

	private static boolean hasPath(RequestMappingInfo info, String path) {
		return info.getPatternsCondition() != null
				&& info.getPatternsCondition().getPatterns().stream()
						.anyMatch(pattern -> path.equals(pattern.getPatternString()));
	}

	@Configuration(proxyBeanMethods = false)
	static class SpringDocStubConfiguration {

		@Bean
		SwaggerUiConfigProperties swaggerUiConfigProperties() {
			return new SwaggerUiConfigProperties();
		}

		@Bean
		SpringDocConfigProperties springDocConfigProperties() {
			return new SpringDocConfigProperties();
		}
	}
}
