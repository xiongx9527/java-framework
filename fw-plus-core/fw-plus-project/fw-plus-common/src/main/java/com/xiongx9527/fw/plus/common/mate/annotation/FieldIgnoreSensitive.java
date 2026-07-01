package com.xiongx9527.fw.plus.common.mate.annotation;

import java.lang.annotation.*;

/**
 * 忽略脱敏
 * @author xiongxiao
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldIgnoreSensitive {
    /**
     * 忽略脱敏的类型，默认忽略所有的类型
     */
    String[] values() default {};
}
