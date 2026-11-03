package com.xiongx9527.fw.plus.ai.base.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法可被模型调用。
 *
 * <p>{@code value} 是给模型看的用途说明（越具体越好）；{@code name} 留空时用方法名。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {

    /** 工具名；留空用方法名。 */
    String name() default "";

    /** 工具说明（给模型看）。 */
    String value() default "";
}
