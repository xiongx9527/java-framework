package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

@Configuration
@ConditionalOnClass(name = {
		"org.apache.ibatis.session.SqlSessionFactory",
		"com.xiongx9527.fw.plus.tinyengine.service.impl.ResourceServiceImpl"
})
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.service", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(TinyEngineDynamicServiceConfiguration.class)
public class TinyEngineServiceConfiguration {

	@Bean
	@ConditionalOnBean(SqlSessionFactory.class)
	@ConditionalOnMissingBean(ResourceService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.mapper", name = "enabled", havingValue = "true", matchIfMissing = true)
	public static BeanDefinitionRegistryPostProcessor tinyEngineServiceBeanDefinitionRegistryPostProcessor(
			Environment environment) {
		return new BeanDefinitionRegistryPostProcessor() {
			@Override
			public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
				ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
				scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
				scanner.scan("com.xiongx9527.fw.plus.tinyengine.service.impl");
				if (isWebEnabled(environment)
						&& !containsBeanDefinitionForType(registry, TinyEngineResourceController.class)) {
					BeanDefinitionBuilder builder = BeanDefinitionBuilder
							.genericBeanDefinition(TinyEngineResourceController.class);
					builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					registry.registerBeanDefinition("tinyEngineResourceController", builder.getBeanDefinition());
				}
			}

			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			}
		};
	}

	private static boolean isWebEnabled(Environment environment) {
		return Boolean.parseBoolean(environment.getProperty("fw.plus.tiny-engine.web.enabled", "true"));
	}

	private static boolean containsBeanDefinitionForType(BeanDefinitionRegistry registry, Class<?> type) {
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (isBeanDefinitionForType(beanDefinition, type)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBeanDefinitionForType(BeanDefinition beanDefinition, Class<?> type) {
		if (beanDefinition instanceof AbstractBeanDefinition) {
			Class<?> resolvedType = ((AbstractBeanDefinition) beanDefinition).getResolvableType().resolve();
			if (resolvedType != null && type.isAssignableFrom(resolvedType)) {
				return true;
			}
		}
		if (beanDefinition instanceof AnnotatedBeanDefinition) {
			MethodMetadata methodMetadata = ((AnnotatedBeanDefinition) beanDefinition).getFactoryMethodMetadata();
			if (methodMetadata != null && isTypeNameAssignable(methodMetadata.getReturnTypeName(), type)) {
				return true;
			}
		}
		return isTypeNameAssignable(beanDefinition.getBeanClassName(), type);
	}

	private static boolean isTypeNameAssignable(String typeName, Class<?> type) {
		if (typeName == null) {
			return false;
		}
		if (type.getName().equals(typeName)) {
			return true;
		}
		try {
			return type.isAssignableFrom(ClassUtils.forName(typeName, TinyEngineServiceConfiguration.class.getClassLoader()));
		} catch (Throwable ex) {
			return false;
		}
	}
}
