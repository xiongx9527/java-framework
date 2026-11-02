package com.xiongx9527.fw.plus.ai.service;

import java.lang.annotation.*;

/** 声明式 AI 服务标记注解，镜像 langchain4j 的 {@code @AiService}（Spring AI 无直接等价，由 ChatClient 流式 API 承担）。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AiService {}
