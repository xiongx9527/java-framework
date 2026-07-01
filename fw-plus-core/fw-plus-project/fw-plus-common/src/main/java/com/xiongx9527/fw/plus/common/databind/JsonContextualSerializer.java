package com.xiongx9527.fw.plus.common.databind;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.lang.annotation.Annotation;

/**
 * @author xiongxiao
 */
public interface JsonContextualSerializer extends ContextualSerializer {

    /**
     * 根据上下文创建一个合适的序列化器
     *
     * @param provider SerializerProvider的实现，用于配置和获取序列化器
     * @param property 当前序列化器相关的Bean属性，可能为null
     * @return JsonSerializer<?> 根据上下文创建的序列化器，用于序列化特定类型的对象
     *
     * 此方法的主要作用是根据具体的序列化上下文（如特定的属性）来配置或获取一个合适的序列化器
     * 当property参数不为null时，调用getJsonSerializer方法获取一个特定的序列化器；
     * 否则，调用provider的findNullValueSerializer方法来获取处理null值的序列化器
     */
    @Override
    default JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) throws JsonMappingException {
        return null != property ? this.getJsonSerializer(provider, property) : provider.findNullValueSerializer((BeanProperty)null);
    }

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
            // 如果未找到注解，尝试从属性的上下文中获取注解
            a = property.getContextAnnotation(clazz);
        }

        // 返回找到的注解，如果没有找到则返回null
        return a;
    }

    /**
     * @param serializerProvider
     * @param beanProperty
     * @return
     * @throws JsonMappingException
     */
    JsonSerializer<?> getJsonSerializer(SerializerProvider serializerProvider, BeanProperty beanProperty) throws JsonMappingException;
}