package com.xiongx9527.fw.plus.easytrans.annotation;

/**
 * fw-plus 扩展的转换类型常量接口。
 *
 * <p>继承上游 {@link org.dromara.core.trans.constant.TransType}，在标准转换类型
 * （{@link #AUTO_TRANS}、{@link #DICTIONARY}、{@link #SIMPLE}、{@link #RPC}、{@link #ENUM}）之外，
 * 补充 fw-plus 扩展的 {@link #BOOLEAN}（布尔翻译，对应 {@code MateEasyTransBooleanTransService}）。</p>
 *
 * <p>引用本接口即可同时使用标准与扩展转换类型，例如：
 * {@code @Trans(type = TransType.BOOLEAN, ref = "delFlagShow")}。</p>
 */
public interface TransType extends org.dromara.core.trans.constant.TransType {

    /**
     * 布尔转换类型标识符（fw-plus 扩展）
     */
    String BOOLEAN = "boolean";

}
