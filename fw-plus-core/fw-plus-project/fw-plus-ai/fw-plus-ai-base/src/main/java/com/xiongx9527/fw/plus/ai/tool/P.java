package com.xiongx9527.fw.plus.ai.tool;

import java.lang.annotation.*;

/** 工具参数描述，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.agent.tool.P}。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface P {
    String value() default "";
}
