package com.xiongx9527.fw.plus.sample.tinyengine;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineResourceConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TinyEngineStarterSampleTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					DataSourceAutoConfiguration.class,
					MybatisPlusAutoConfiguration.class,
					WebMvcAutoConfiguration.class,
					TinyEngineAutoConfiguration.class))
			.withPropertyValues(
					"spring.datasource.url=jdbc:h2:mem:tiny_engine_sample;MODE=MySQL;DB_CLOSE_DELAY=-1",
					"spring.datasource.driver-class-name=org.h2.Driver",
					"spring.datasource.username=sa",
					"fw.plus.tiny-engine.enabled=true",
					"fw.plus.tiny-engine.sql.database=h2");

	@Test
	void enabledSampleStartsWithTinyEngineBeansAndResources() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ResourceService.class);
			assertThat(context).hasSingleBean(TinyEngineResourceController.class);
			assertThat(context).hasBean("resourceMapper");
			assertThat(context.getBean("resourceMapper")).isInstanceOf(ResourceMapper.class);
			assertThat(context).hasSingleBean(TinyEngineResourceConfiguration.TinyEngineSqlResources.class);
			TinyEngineResourceConfiguration.TinyEngineSqlResources resources =
					context.getBean(TinyEngineResourceConfiguration.TinyEngineSqlResources.class);
			assertThat(resources.getScripts()).isNotEmpty();
			assertThat(resources.isAutoInitialize()).isFalse();
			assertTinyEngineResourcesRouteRegistered(context.getBean(RequestMappingHandlerMapping.class));
		});
	}

	@Test
	void disabledSampleDoesNotRegisterTinyEngineBeans() {
			contextRunner.withPropertyValues(
							"fw.plus.tiny-engine.enabled=false",
							"mybatis-plus.mapper-locations=classpath:/missing-tinyengine-disabled/*.xml")
					.run(context -> {
						assertThat(context).doesNotHaveBean(ResourceService.class);
						assertThat(context).doesNotHaveBean(TinyEngineResourceController.class);
					});
	}

	private static void assertTinyEngineResourcesRouteRegistered(RequestMappingHandlerMapping handlerMapping) {
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
			RequestMappingInfo mappingInfo = entry.getKey();
			HandlerMethod handlerMethod = entry.getValue();
			if (containsPath(mappingInfo, "/tiny-engine/resources")
					&& containsMethod(mappingInfo, RequestMethod.GET)
					&& TinyEngineResourceController.class.equals(handlerMethod.getBeanType())) {
				return;
			}
		}
		throw new AssertionError("TinyEngine resources route was not registered");
	}

	private static boolean containsPath(RequestMappingInfo mappingInfo, String path) {
		if (mappingInfo.getPatternsCondition() != null
				&& mappingInfo.getPatternsCondition().getPatterns().contains(path)) {
			return true;
		}
		if (mappingInfo.getPathPatternsCondition() != null
				&& mappingInfo.getPathPatternsCondition().getPatternValues().contains(path)) {
			return true;
		}
		return false;
	}

	private static boolean containsMethod(RequestMappingInfo mappingInfo, RequestMethod method) {
		Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
		return methods.contains(method);
	}
}
