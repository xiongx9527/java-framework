package com.xiongx9527.fw.plus.ai.base.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数说明（给模型看）。参数名取自形参名；需要重命名时用 {@link #name()}。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface P {

    /** 参数说明。 */
    String value() default "";

    /** 参数名；留空用形参名。 */
    String name() default "";
}
