package com.xiongx9527.fw.plus.common.mate.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.xiongx9527.fw.plus.common.databind.SensitiveSerializer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 脱敏注解
 * @author xiongxiao
 */
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(
    using = SensitiveSerializer.class
)
public @interface FieldSensitive {
    /**
     * 脱敏类型
     */
    String value();
}