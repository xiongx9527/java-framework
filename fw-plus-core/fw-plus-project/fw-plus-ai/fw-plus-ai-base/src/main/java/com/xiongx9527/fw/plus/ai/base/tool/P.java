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

    /**
     * 是否必填。默认必填，与 schema 生成一致；设为 {@code false} 表示可选参数，
     * 缺省时执行器不会抛错（交由工具方法自身处理）。
     */
    boolean required() default true;

    /**
     * 值格式校验正则（Java 正则）。非空时，执行器会对字符串化后的参数值做匹配，
     * 不匹配则抛 {@link ToolArgumentException}（路由到参数错误处理器后回灌模型）。
     */
    String pattern() default "";

    /**
     * Semantic format hint (e.g. date/email/url). Only written into the schema "format" field to
     * help the model; not enforced at execution time.
     */
    String format() default "";

    /**
     * Minimum string length (inclusive). Negative means unlimited; a shorter non-empty string throws
     * ToolArgumentException.
     */
    int minLength() default -1;

    /**
     * Maximum string length (inclusive). Negative means unlimited; a longer string throws
     * ToolArgumentException.
     */
    int maxLength() default -1;

    /**
     * Numeric lower bound (inclusive), as a string to allow decimals/negatives. Empty means unlimited;
     * if the value parses as a number below this, throws ToolArgumentException.
     */
    String min() default "";

    /**
     * Numeric upper bound (inclusive). Empty means unlimited.
     */
    String max() default "";

    /**
     * JSON shape validation: "object" requires a JSON object (braces, balanced), "array" requires a
     * JSON array (brackets, balanced); empty means no check. A regex cannot reliably validate JSON
     * structure, so this uses a lightweight bracket-balance scan that skips string contents; the
     * actual deserialization is still done downstream by the tool method.
     */
    String jsonShape() default "";
}
