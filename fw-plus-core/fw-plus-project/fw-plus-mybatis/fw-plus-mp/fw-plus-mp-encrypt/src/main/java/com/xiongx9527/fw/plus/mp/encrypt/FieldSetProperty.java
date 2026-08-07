package com.xiongx9527.fw.plus.mp.encrypt;


import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * FieldSetProperty类用于封装字段设置属性信息
 * 包含字段名称和字段加密配置信息
 */
@AllArgsConstructor
@Data
public class FieldSetProperty {
    /**
     * 字段名称
     * 用于标识具体的字段
     */
    private String fieldName;

    /**
     * 字段加密配置
     * 用于定义该字段的加密策略和相关配置
     */
    private FieldEncrypt fieldEncrypt;
}
