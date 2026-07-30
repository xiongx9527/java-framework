package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import org.apache.ibatis.session.SqlSessionFactory;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

@Configuration
@ConditionalOnClass(name = {
		"org.apache.ibatis.session.SqlSessionFactory",
		"com.xiongx9527.fw.plus.tinyengine.service.impl.ResourceServiceImpl"
})
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.service", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineServiceConfiguration {

	@Bean
	@ConditionalOnBean(SqlSessionFactory.class)
	@ConditionalOnMissingBean(ResourceService.class)
	public static BeanDefinitionRegistryPostProcessor tinyEngineServiceBeanDefinitionRegistryPostProcessor() {
		return new BeanDefinitionRegistryPostProcessor() {
			@Override
			public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
				ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
				scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
				scanner.scan("com.xiongx9527.fw.plus.tinyengine.service.impl");
			}

			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			}
		};
	}
}
