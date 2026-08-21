package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
@AutoConfigureAfter(name = {
		"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
		"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
		"com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.tinyengine.service.ResourceService")
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine", name = "enabled", havingValue = "true")
@ConditionalOnWebApplication
@EnableConfigurationProperties(TinyEngineProperties.class)
@Import({TinyEngineCoreConfiguration.class, TinyEngineServiceConfiguration.class, TinyEngineMapperConfiguration.class,
		TinyEngineResourceConfiguration.class, TinyEngineApiConfiguration.class,
		TinyEngineAutoConfiguration.TinyEngineWebConfigurationImportSelector.class})
public class TinyEngineAutoConfiguration {

	public static class TinyEngineWebConfigurationImportSelector implements ImportSelector, BeanClassLoaderAware {

		private static final String SERVLET_CONFIGURER =
				"org.springframework.web.servlet.config.annotation.WebMvcConfigurer";
		private static final String REACTIVE_CONFIGURER =
				"org.springframework.web.reactive.config.WebFluxConfigurer";
		private static final String REACTOR_SCHEDULER = "reactor.core.scheduler.Scheduler";
		private static final String SERVLET_CONFIGURATION =
				"com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineServletWebConfiguration";
		private static final String REACTIVE_CONFIGURATION =
				"com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux.TinyEngineReactiveWebConfiguration";

		private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.beanClassLoader = classLoader;
		}

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			List<String> configurations = new ArrayList<String>();
			if (ClassUtils.isPresent(SERVLET_CONFIGURER, beanClassLoader)) {
				configurations.add(SERVLET_CONFIGURATION);
			}
			if (ClassUtils.isPresent(REACTIVE_CONFIGURER, beanClassLoader)
					&& ClassUtils.isPresent(REACTOR_SCHEDULER, beanClassLoader)) {
				configurations.add(REACTIVE_CONFIGURATION);
			}
			return configurations.toArray(new String[configurations.size()]);
		}
	}
}
