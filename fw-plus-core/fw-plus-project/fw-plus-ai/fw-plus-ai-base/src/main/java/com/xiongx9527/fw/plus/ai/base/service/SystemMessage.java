package com.xiongx9527.fw.plus.ai.base.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统提示词（可放类型或方法上）。支持 {@code {{name}}} 变量，与方法参数上的 {@link V} 对应。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SystemMessage {

    String[] value();
}
