package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerEntity;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerProvider;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;

import jakarta.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * swagger 文档与 knife4j UI 配置的栈无关核心行为测试：
 * 文档 paths 反映已注册接口且不带 /magic-api/web 前缀；两条支线的 UI 配置 bean 确定性注入。
 */
class SwaggerEndpointSupportTest {

	private static final String GROUP_ID = "group-1";
	private static final String API_ID = "api-1";

	@Test
	void documentContainsRegisteredApiPathWithoutBasePrefix() {
		MagicResourceService magicResourceService = mockResourceService();
		SwaggerProvider provider = new SwaggerEndpointSupport(new SwaggerConfig(), magicResourceService, new MagicAPIProperties())
				.createProvider(() -> Collections.singletonList(apiInfo()), "");

		SwaggerEntity document = provider.swaggerJson();

		assertThat(document.getSwagger()).isEqualTo("2.0");
		assertThat(document.getBasePath()).isEmpty();
		assertThat(document.getPaths()).hasSize(1);
		String path = document.getPaths().keySet().iterator().next();
		assertThat(path).endsWith("/demo/hello").doesNotContain("/magic-api/web");
	}

	@Test
	void servletUiConfigBeansAreInjectedDeterministically() {
		new org.springframework.boot.test.context.runner.WebApplicationContextRunner()
				.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(MagicSwaggerConfiguration.class))
				.withBean(MagicResourceService.class, this::mockResourceService)
				.withBean(ServletContext.class, () -> mock(ServletContext.class))
				.withBean(MagicAPIProperties.class, MagicAPIProperties::new)
				.withUserConfiguration(SpringDocStubConfiguration.class)
				.run(context -> assertUiGroupInjected(context));
	}

	@Test
	void webfluxUiConfigBeansAreInjectedDeterministically() {
		new org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner()
				.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(MagicSwaggerConfiguration.class))
				.withBean(MagicResourceService.class, this::mockResourceService)
				.withBean(MagicAPIProperties.class, MagicAPIProperties::new)
				.withUserConfiguration(SpringDocStubConfiguration.class)
				.run(context -> assertUiGroupInjected(context));
	}

	private void assertUiGroupInjected(org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider context) {
		org.springframework.context.ConfigurableApplicationContext applicationContext =
				(org.springframework.context.ConfigurableApplicationContext) context.getSourceApplicationContext();
		assertThat(applicationContext.containsBean("magicSwaggerUiConfigProperties")).isTrue();
		assertThat(applicationContext.containsBean("magicSwaggerUiConfigParameters")).isTrue();
		Map<String, Object> params = applicationContext.getBean(SwaggerUiConfigParameters.class).getConfigParameters();
		@SuppressWarnings("unchecked")
		Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = (Set<AbstractSwaggerUiConfigProperties.SwaggerUrl>) params.get("urls");
		assertThat(urls).isNotNull();
		SwaggerConfig swaggerConfig = new SwaggerConfig();
		assertThat(urls).anyMatch(url -> swaggerConfig.getName().equals(url.getName())
				&& swaggerConfig.getLocation().equals(url.getUrl()));
	}

	private MagicResourceService mockResourceService() {
		MagicResourceService magicResourceService = mock(MagicResourceService.class);
		when(magicResourceService.getGroupName(GROUP_ID)).thenReturn("演示组");
		when(magicResourceService.getGroupPath(GROUP_ID)).thenReturn("demo");
		when(magicResourceService.getGroupsByFileId(API_ID)).thenReturn(Collections.emptyList());
		// SwaggerProvider.parseParameters 经静态入口取资源服务，需在测试中显式设置
		new MagicConfiguration().setMagicResourceService(magicResourceService);
		return magicResourceService;
	}

	private ApiInfo apiInfo() {
		ApiInfo apiInfo = new ApiInfo();
		apiInfo.setId(API_ID);
		apiInfo.setGroupId(GROUP_ID);
		apiInfo.setPath("/hello");
		apiInfo.setMethod("GET");
		apiInfo.setName("演示接口");
		apiInfo.setParameters(new ArrayList<>());
		apiInfo.setHeaders(new ArrayList<>());
		apiInfo.setPaths(new ArrayList<>());
		return apiInfo;
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class SpringDocStubConfiguration {

		@org.springframework.context.annotation.Bean
		SwaggerUiConfigProperties swaggerUiConfigProperties() {
			return new SwaggerUiConfigProperties();
		}

		@org.springframework.context.annotation.Bean
		SpringDocConfigProperties springDocConfigProperties() {
			return new SpringDocConfigProperties();
		}
	}
}
