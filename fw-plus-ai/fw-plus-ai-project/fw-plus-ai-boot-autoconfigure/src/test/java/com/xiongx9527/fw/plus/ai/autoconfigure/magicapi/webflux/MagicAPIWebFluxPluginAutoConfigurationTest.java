package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * AI 层 magicapi 插件 WebFlux 注册契约测试：reactive 下 langchain4j / mcp 功能 bean
 * 装配、控制器端点注册；servlet runner 下 Base 入口条件激活（引擎缺席时插件回退，行为与
 * 改动前一致）。
 */
class MagicAPIWebFluxPluginAutoConfigurationTest {

	private static final String WEB_BASE = "/magic-api/web";
	private static final String LANGCHAIN4J_TEST_PATH = WEB_BASE + "/langchain4j/{type}/datasource/test";
	private static final String MCP_SERVER_LIST_PATH = WEB_BASE + "/mcp/server/list";
	private static final String MCP_DIAGNOSTICS_PATH = WEB_BASE + "/mcp/diagnostics/capabilities";

	@Test
	void registersLangchain4jAndMcpControllersInReactiveHandlerMapping() {
		reactiveRunner().run(context -> {
			assertThat(context).hasBean("magicAPIWebFluxPluginControllerRegistrar");
			RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping",
					RequestMappingHandlerMapping.class);
			assertThat(mapping.getHandlerMethods().keySet())
					.anyMatch(info -> hasPath(info, LANGCHAIN4J_TEST_PATH))
					.anyMatch(info -> hasPath(info, MCP_SERVER_LIST_PATH))
					.anyMatch(info -> hasPath(info, MCP_DIAGNOSTICS_PATH));
		});
	}

	@Test
	void assemblesLangchain4jAndMcpPluginBeansInReactiveApplication() {
		reactiveRunner().run(context -> {
			assertThat(context).getBean(MagicDynamicLangchain4jChatModelDataSource.class).isNotNull();
			assertThat(context).getBean(MagicApiMcpProperties.class).isNotNull();
			assertThat(context).getBean(MagicResourceService.class).isNotNull();
		});
	}

	@Test
	void baseConfigurationActivatesInServletApplication() {
		// servlet 下由 fork 引擎提供 MagicResourceService；runner 以 stub 代替以验证装配链激活，
		// 与生产环境改动前后的激活语义一致。
		new WebApplicationContextRunner()
				.withBean(MagicResourceService.class, () -> mock(MagicResourceService.class))
				.withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class))
				.run(context -> assertThat(context)
						.hasSingleBean(MagicAPIBaseAutoConfiguration.class));
	}

	private static ReactiveWebApplicationContextRunner reactiveRunner() {
		return new ReactiveWebApplicationContextRunner()
				.withPropertyValues("magic-api.resource.location=target/magic-api-ai-webflux-test")
				.withUserConfiguration(ReactiveTestRoot.class);
	}

	private static boolean hasPath(RequestMappingInfo info, String path) {
		return info.getPatternsCondition() != null
				&& info.getPatternsCondition().getPatterns().stream()
						.anyMatch(pattern -> path.equals(pattern.getPatternString()));
	}

	/**
	 * 聚合根：{@code @Import} 声明顺序即处理顺序——spring 的 {@code WebFluxAutoConfiguration}
	 * 先提供 {@code requestMappingHandlerMapping}，Boot 层 reactive 引擎再提供
	 * {@code MagicResourceService} / {@code MagicAPIWebFluxMapping}，最后 AI 层链路
	 * {@code @ConditionalOnBean} 评估（等价于生产环境经 {@code @AutoConfigureAfter} 达成的排序）。
	 */
	@Configuration(proxyBeanMethods = false)
	@Import({org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.class,
			MagicAPIWebFluxAutoConfiguration.class, MagicAPIBaseAutoConfiguration.class})
	static class ReactiveTestRoot {
	}
}
