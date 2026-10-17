package com.xiongx9527.fw.plus.common.databind;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.lang.annotation.Annotation;

/**
 * @author xiongxiao
 */
public interface JsonContextualSerializer {

    /**
     * 获取指定Bean属性上的注解或其上下文中的注解
     * 此方法用于处理注解的获取逻辑，首先尝试直接从属性上获取注解，
     * 如果未找到，则尝试从属性的上下文中获取该注解
     *
     * @param property 要检查的Bean属性
     * @param clazz 注解的类类型，用于指定想要获取的注解类型
     * @param <A> 注解的泛型参数，允许方法返回指定类型的注解
     * @return 返回找到的注解，如果没有找到则返回null
     */
    default <A extends Annotation> A getAnnotation(BeanProperty property, Class<A> clazz) {
        // 尝试直接从属性上获取注解
        A a = property.getAnnotation(clazz);
        if (null == a) {
            // 如果未找到注解，尝试从属性的上下文中获取该注解
            a = property.getContextAnnotation(clazz);
        }

        // 返回找到的注解，如果没有找到则返回null
        return a;
    }

    /**
     * 根据上下文获取（或创建）合适的字段序列化器
     *
     * @param serializerProvider SerializationContext的实现，用于配置和获取序列化器
     * @param beanProperty 当前序列化器相关的Bean属性
     * @return 根据上下文创建的序列化器，用于序列化特定类型的对象
     */
    ValueSerializer<?> getJsonSerializer(SerializationContext serializerProvider, BeanProperty beanProperty);
}
