//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.service.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class AiServiceScannerProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
    private Environment environment;

    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathAiServiceScanner classPathAiServiceScanner = new ClassPathAiServiceScanner(registry, false);

        for(String basePackage : this.getBasePackages((ConfigurableListableBeanFactory)registry)) {
            classPathAiServiceScanner.scan(new String[]{basePackage});
        }

        this.removeAiServicesWithInactiveProfiles(registry);
    }

    private Set<String> getBasePackages(ConfigurableListableBeanFactory beanFactory) {
        Set<String> basePackages = new LinkedHashSet();
        List<String> autoConfigPackages = AutoConfigurationPackages.get(beanFactory);
        basePackages.addAll(autoConfigPackages);
        String[] beanNames = beanFactory.getBeanNamesForAnnotation(ComponentScan.class);

        for(String beanName : beanNames) {
            Class<?> beanClass = beanFactory.getType(beanName);
            if (beanClass != null) {
                ComponentScan componentScan = (ComponentScan)beanClass.getAnnotation(ComponentScan.class);
                if (componentScan != null) {
                    Collections.addAll(basePackages, componentScan.value());
                    Collections.addAll(basePackages, componentScan.basePackages());

                    for(Class<?> basePackageClass : componentScan.basePackageClasses()) {
                        basePackages.add(basePackageClass.getPackage().getName());
                    }
                }
            }
        }

        String[] applicationBeans = beanFactory.getBeanNamesForAnnotation(SpringBootApplication.class);
        if (applicationBeans.length > 0) {
            Class<?> applicationBeanClass = beanFactory.getType(applicationBeans[0]);
            SpringBootApplication springBootApplication = (SpringBootApplication)AnnotationUtils.findAnnotation(applicationBeanClass, SpringBootApplication.class);
            if (springBootApplication != null) {
                Collections.addAll(basePackages, springBootApplication.scanBasePackages());

                for(Class<?> aClass : springBootApplication.scanBasePackageClasses()) {
                    basePackages.add(ClassUtils.getPackageName(aClass));
                }
            }
        }

        return basePackages;
    }

    private void removeAiServicesWithInactiveProfiles(BeanDefinitionRegistry registry) {
        Stream var10000 = Arrays.stream(registry.getBeanDefinitionNames()).filter((beanName) -> {
            try {
                BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
                if (beanDefinition.getBeanClassName() != null) {
                    Class<?> beanClass = Class.forName(beanDefinition.getBeanClassName());
                    if (beanClass.isAnnotationPresent(AiService.class) && beanClass.isAnnotationPresent(Profile.class)) {
                        Profile profileAnnotation = (Profile)beanClass.getAnnotation(Profile.class);
                        String[] profiles = profileAnnotation.value();
                        return !this.environment.matchesProfiles(profiles);
                    }
                }
            } catch (ClassNotFoundException var7) {
            }

            return false;
        });
        Objects.requireNonNull(registry);
        var10000.forEach(registry::removeBeanDefinition);
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }
}
