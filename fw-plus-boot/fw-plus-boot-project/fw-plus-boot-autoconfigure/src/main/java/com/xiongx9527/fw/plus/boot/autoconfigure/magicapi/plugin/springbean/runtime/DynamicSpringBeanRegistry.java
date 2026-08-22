package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.CompiledGeneration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceUnit;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.model.SpringBeanInfo;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class DynamicSpringBeanRegistry implements Closeable {

    private final ConfigurableApplicationContext applicationContext;
    private final DefaultListableBeanFactory beanFactory;
    private final JavaSourceAnalyzer analyzer;
    private final InMemoryJavaEntityCompiler compiler;
    private final AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
    private final ReentrantLock rebuildLock = new ReentrantLock();
    private final AtomicLong versionSequence = new AtomicLong();
    private final Map<String, String> registeredBeanNames = new LinkedHashMap<>();
    private volatile DynamicSpringBeanStatus status;
    private volatile CompiledGeneration generation;

    public DynamicSpringBeanRegistry(ConfigurableApplicationContext applicationContext,
                                     JavaSourceAnalyzer analyzer,
                                     InMemoryJavaEntityCompiler compiler) {
        this.applicationContext = applicationContext;
        this.beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        this.analyzer = analyzer;
        this.compiler = compiler;
        this.status = compiler.isAvailable() ? DynamicSpringBeanStatus.success(0L)
                : DynamicSpringBeanStatus.unavailable();
    }

    public boolean rebuild(Collection<SpringBeanInfo> sources) {
        rebuildLock.lock();
        try {
            List<SpringBeanInfo> snapshot = new ArrayList<>();
            if (sources != null) {
                for (SpringBeanInfo source : sources) {
                    snapshot.add(source.copy());
                }
            }
            long candidateVersion = versionSequence.incrementAndGet();
            CompiledGeneration candidate = null;
            try {
                List<JavaSourceUnit> units = new ArrayList<>();
                for (SpringBeanInfo source : snapshot) {
                    units.add(analyzer.analyze(source.getId(), source.getName(), source.getScript()));
                }
                candidate = compiler.compile(candidateVersion, units);
                Map<String, BeanDefinition> candidateDefinitions = beanDefinitions(candidate);
                replaceBeanDefinitions(candidateDefinitions, candidate.getClassLoader());
                closeCurrentGeneration();
                generation = candidate;
                status = DynamicSpringBeanStatus.success(candidateVersion);
                return true;
            } catch (JavaEntityCompilationException exception) {
                closeCandidate(candidate);
                status = DynamicSpringBeanStatus.failure(currentVersion(), exception.getDiagnostics());
                return false;
            } catch (RuntimeException exception) {
                closeCandidate(candidate);
                status = DynamicSpringBeanStatus.failure(currentVersion(), Collections.singletonList(
                        new JavaEntityDiagnostic("", 0, 0, "spring-bean.register.failed", safeMessage(exception))));
                return false;
            }
        } finally {
            rebuildLock.unlock();
        }
    }

    public void validateCandidate(Collection<SpringBeanInfo> sources) {
        List<JavaSourceUnit> units = new ArrayList<>();
        if (sources != null) {
            for (SpringBeanInfo source : sources) {
                units.add(analyzer.analyze(source.getId(), source.getName(), source.getScript()));
            }
        }
        CompiledGeneration candidate = compiler.compile(0L, units);
        closeCandidate(candidate);
    }

    public Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    public long currentVersion() {
        DynamicSpringBeanStatus current = status;
        return current == null ? 0L : current.getVersion();
    }

    public DynamicSpringBeanStatus status() {
        return status;
    }

    private Map<String, BeanDefinition> beanDefinitions(CompiledGeneration candidate) {
        Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
        for (Class<?> beanClass : candidate.getClasses().values()) {
            AnnotatedGenericBeanDefinition definition = new AnnotatedGenericBeanDefinition(beanClass);
            String beanName = beanNameGenerator.generateBeanName(definition, (BeanDefinitionRegistry) beanFactory);
            beanName = StringUtils.hasText(beanName) ? beanName : decapitalize(beanClass.getSimpleName());
            definitions.put(beanName, definition);
        }
        return definitions;
    }

    private void replaceBeanDefinitions(Map<String, BeanDefinition> definitions, ClassLoader beanClassLoader) {
        Map<String, String> previousBeanNames = new LinkedHashMap<>(registeredBeanNames);
        Map<String, BeanDefinition> previousDefinitions = currentBeanDefinitions(previousBeanNames.values());
        ClassLoader previousGenerationClassLoader = generation == null ? null : generation.getClassLoader();
        try {
            registerBeanDefinitions(definitions, beanClassLoader);
        } catch (RuntimeException exception) {
            rollbackBeanDefinitions(definitions.keySet(), previousBeanNames, previousDefinitions, previousGenerationClassLoader);
            throw exception;
        }
    }

    private void registerBeanDefinitions(Map<String, BeanDefinition> definitions, ClassLoader beanClassLoader) {
        for (String beanName : registeredBeanNames.values()) {
            removeBean(beanName);
        }
        registeredBeanNames.clear();
        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            removeBean(entry.getKey());
            beanFactory.registerBeanDefinition(entry.getKey(), entry.getValue());
            registeredBeanNames.put(entry.getKey(), entry.getKey());
        }
        preInstantiateDynamicBeans(registeredBeanNames.values(), beanClassLoader);
    }

    private void preInstantiateDynamicBeans(Collection<String> beanNames, ClassLoader beanClassLoader) {
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader originalBeanClassLoader = beanFactory.getBeanClassLoader();
        Map<String, AbstractAutoProxyCreator> autoProxyCreators =
                beanFactory.getBeansOfType(AbstractAutoProxyCreator.class, false, false);
        try {
            Thread.currentThread().setContextClassLoader(beanClassLoader);
            beanFactory.setBeanClassLoader(beanClassLoader);
            for (AbstractAutoProxyCreator autoProxyCreator : autoProxyCreators.values()) {
                autoProxyCreator.setBeanClassLoader(beanClassLoader);
            }
            for (String beanName : beanNames) {
                applicationContext.getBean(beanName);
            }
        } finally {
            for (AbstractAutoProxyCreator autoProxyCreator : autoProxyCreators.values()) {
                autoProxyCreator.setBeanClassLoader(originalBeanClassLoader);
            }
            beanFactory.setBeanClassLoader(originalBeanClassLoader);
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private Map<String, BeanDefinition> currentBeanDefinitions(Collection<String> beanNames) {
        Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
        for (String beanName : beanNames) {
            if (beanFactory.containsBeanDefinition(beanName)) {
                definitions.put(beanName, beanFactory.getBeanDefinition(beanName));
            }
        }
        return definitions;
    }

    private void rollbackBeanDefinitions(Collection<String> candidateBeanNames,
                                         Map<String, String> previousBeanNames,
                                         Map<String, BeanDefinition> previousDefinitions,
                                         ClassLoader previousGenerationClassLoader) {
        for (String beanName : candidateBeanNames) {
            removeBean(beanName);
        }
        registeredBeanNames.clear();
        for (Map.Entry<String, BeanDefinition> entry : previousDefinitions.entrySet()) {
            beanFactory.registerBeanDefinition(entry.getKey(), entry.getValue());
        }
        registeredBeanNames.putAll(previousBeanNames);
        if (!previousBeanNames.isEmpty() && previousGenerationClassLoader != null) {
            try {
                preInstantiateDynamicBeans(previousBeanNames.values(), previousGenerationClassLoader);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void removeBean(String beanName) {
        if (beanFactory.containsSingleton(beanName)) {
            beanFactory.destroySingleton(beanName);
        }
        try {
            if (beanFactory.containsBeanDefinition(beanName)) {
                beanFactory.removeBeanDefinition(beanName);
            }
        } catch (NoSuchBeanDefinitionException ignored) {
        }
    }

    private void closeCurrentGeneration() {
        closeCandidate(generation);
        generation = null;
    }

    private static void closeCandidate(CompiledGeneration candidate) {
        if (candidate != null) {
            candidate.close();
        }
    }

    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0))
                && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        String sanitized = message == null ? exception.getClass().getSimpleName()
                : message.replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }

    @Override
    public void close() {
        rebuildLock.lock();
        try {
            for (String beanName : registeredBeanNames.values()) {
                removeBean(beanName);
            }
            registeredBeanNames.clear();
            closeCurrentGeneration();
        } finally {
            rebuildLock.unlock();
        }
    }
}
