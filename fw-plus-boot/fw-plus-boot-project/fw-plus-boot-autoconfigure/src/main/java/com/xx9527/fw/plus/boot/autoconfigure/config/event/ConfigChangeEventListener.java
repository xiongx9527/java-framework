package com.xx9527.fw.plus.boot.autoconfigure.config.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class ConfigChangeEventListener implements ApplicationListener<ConfigChangeEvent>, BeanPostProcessor {

    private final Map<String, List<ListenerMethod>> registry = new LinkedHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (Method method : targetClass.getDeclaredMethods()) {
            OnConfigChange annotation = AnnotationUtils.findAnnotation(method, OnConfigChange.class);
            if (annotation == null) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                log.warn("@OnConfigChange 方法应有且仅有一个参数: {}.{}", targetClass.getName(), method.getName());
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!ConfigChangeEvent.class.isAssignableFrom(paramType) && !Map.class.isAssignableFrom(paramType)) {
                log.warn("@OnConfigChange 方法参数类型只能是 ConfigChangeEvent 或 Map<String, Object>: {}.{}",
                        targetClass.getName(), method.getName());
                continue;
            }
            method.setAccessible(true);
            String prefix = annotation.prefix();
            registry.computeIfAbsent(prefix, k -> new ArrayList<>())
                    .add(new ListenerMethod(bean, method, paramType));
            log.debug("注册 @OnConfigChange 监听: {}#{}, prefix={}", beanName, method.getName(), prefix);
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(ConfigChangeEvent event) {
        Map<String, Object> properties = event.getProperties();
        if (properties.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<ListenerMethod>> entry : registry.entrySet()) {
            String prefix = entry.getKey();
            Map<String, Object> filtered = filterByPrefix(properties, prefix);
            if (filtered.isEmpty()) {
                continue;
            }
            for (ListenerMethod lm : entry.getValue()) {
                invoke(lm, event, filtered);
            }
        }
    }

    private Map<String, Object> filterByPrefix(Map<String, Object> source, String prefix) {
        if (prefix.isEmpty()) {
            return source;
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void invoke(ListenerMethod lm, ConfigChangeEvent event, Map<String, Object> filtered) {
        try {
            if (ConfigChangeEvent.class.isAssignableFrom(lm.paramType)) {
                lm.method.invoke(lm.bean, event);
            } else {
                lm.method.invoke(lm.bean, filtered);
            }
        } catch (Exception e) {
            log.error("调用 @OnConfigChange 方法失败: {}#{}, error={}",
                    lm.bean.getClass().getName(), lm.method.getName(), e.getMessage(), e);
        }
    }

    private static class ListenerMethod {
        final Object bean;
        final Method method;
        final Class<?> paramType;

        ListenerMethod(Object bean, Method method, Class<?> paramType) {
            this.bean = bean;
            this.method = method;
            this.paramType = paramType;
        }
    }
}
