package com.xiongx9527.fw.plus.ai.service;

import java.lang.annotation.*;

/** 方法 / 参数上的用户消息模板，镜像 langchain4j 的 {@code @UserMessage}。 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface UserMessage {
    String value() default "";
}
