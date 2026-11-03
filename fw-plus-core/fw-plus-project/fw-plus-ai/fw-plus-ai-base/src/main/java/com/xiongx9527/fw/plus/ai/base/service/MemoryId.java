package com.xiongx9527.fw.plus.ai.base.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「会话 id」参数：有它才会读写 {@code ChatMemory}。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface MemoryId {
}
