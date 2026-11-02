package com.xiongx9527.fw.plus.ai.tool;

import java.lang.annotation.*;

/** 工具方法标记，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.agent.tool.Tool}。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {
    String name() default "";
    String[] value() default {};
}
