package com.xiongx9527.fw.plus.ai.service;

import java.lang.annotation.*;

/** 参数变量名，镜像 langchain4j 的 {@code @V}（绑定到 PromptTemplate 的 {{key}}）。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface V {
    String value() default "";
}
