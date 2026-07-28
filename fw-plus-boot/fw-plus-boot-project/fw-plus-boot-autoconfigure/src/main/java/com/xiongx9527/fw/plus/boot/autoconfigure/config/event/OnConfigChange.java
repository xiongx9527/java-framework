package com.xiongx9527.fw.plus.boot.autoconfigure.config.event;

import java.lang.annotation.*;

/**
 * 通用配置变更监听注解，按 prefix 过滤感兴趣的变更事件。
 * 兼容 Spring Cloud Config、Apollo、Nacos 等配置中心。
 * 方法签名：void methodName(Map<String, Object> properties) 或 void methodName(ConfigChangeEvent event)
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnConfigChange {

    /**
     * 监听的配置前缀，为空时监听所有变更。
     */
    String prefix() default "";
}
