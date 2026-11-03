package com.xiongx9527.fw.plus.ai.base.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户提示词模板。支持 {@code {{name}}} 变量（对应 {@link V}）与 {@code {{it}}}（唯一未命名参数）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UserMessage {

    String[] value();
}
