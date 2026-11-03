package com.xiongx9527.fw.plus.ai.base.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个接口是 AI 服务；类型上的 {@link SystemMessage} 作为所有方法的默认系统提示。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AiService {
}
