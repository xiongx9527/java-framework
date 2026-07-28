package com.xiongx9527.fw.plus.common.validator;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: zhangxin
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CommonsValidatorAnn.Validator.class)
public @interface CommonsValidatorAnn {

    /**
     * 提示信息
     * @return
     */
    String message() ;

    /**
     * 验证分组，条件必须加上否则无效
     * @return
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     * @return
     */
    Class<? extends Payload>[] payload() default { };

    /**
     * 条件表达式(默认spring el), 返回true or false
     * @return
     */
    String conditionExpressions() default StrUtil.EMPTY;

    /**
     * 验证类型
     * @return
     */
    DataValidType dataValidType();

    /**
     * 描述
     * @return
     */
    String description() default StrUtil.EMPTY;

    /**
     * (正则)表达式 , 返回true or false
     * @return
     */
    String regularExpressions() default StrUtil.EMPTY;

    /**
     * 数据验证表达式，返回true or false
     * @return
     */
    String dataValidExpressions() default StrUtil.EMPTY;

    /**
     * 枚举类型
     * @return
     */
    Class<?> enumClass() default Void.class;

    /**
     * 字典key
     * @return
     */
    String dictKey() default StrUtil.EMPTY;

    /**
     * 枚举值字段
     * @return
     */
    String enumValueField() default StrUtil.EMPTY;

    /**
     * 是否忽略空值
     * @return
     */
    boolean ignoreEmpty() default true;

    /**
     * 是否包含最小值
     * @return
     */
    boolean includeMin() default false;

    /**
     * 最小值，空表示没有最小值
     * @return
     */
    String min() default StrUtil.EMPTY;

    /**
     * 是否包含最大值
     * @return
     */
    boolean includeMax() default false;

    /**
     * 最大值 ，空表示没有最大值
     * @return
     */
    String max() default StrUtil.EMPTY;

    /**
     * 验证器
     */
    @Slf4j
    class Validator implements ConstraintValidator<CommonsValidatorAnn, Object> {
        private CommonsValidatorAnn annotation;

        /**
         * 初始化验证器注解处理器
         *
         * @param annotation 验证器注解，用于配置验证规则
         */
        @Override
        public void initialize(CommonsValidatorAnn annotation) {
            this.annotation = annotation;
        }

        /**
         * 校验给定字段值是否有效的核心方法
         * 此方法实现了ConstraintValidator接口的isValid方法，用于自定义约束的验证逻辑
         * 它首先尝试调用debugLog方法记录调试信息，然后尝试使用CommonsValidator进行验证
         * 如果验证过程中出现异常，则记录错误日志并返回true，表示验证通过
         *
         * @param fieldValue       待验证的字段值
         * @param validatorContext 验证器上下文，用于在验证过程中提供配置信息和上下文数据
         * @return boolean 如果验证成功或发生异常，则返回true；否则返回false
         */
        @Override
        public boolean isValid(Object fieldValue, ConstraintValidatorContext validatorContext) {
            try {
                // 尝试使用CommonsValidator进行验证
                return CommonsValidator.isValid(annotation, fieldValue, validatorContext);
            } catch (Throwable e) {
                // 验证过程中出现异常，记录错误日志
                log.error("valid error ", e);
                // 即使出现异常，也认为验证通过，返回true
                return Boolean.TRUE;
            }
        }
    }

    /**
     * 验证类型枚举类
     */
    public static enum DataValidType {
        //正则表达式
        REGULAR_EXPRESSIONS ,
        //数据验证表达式
        VALID_EXPRESSIONS ,
        //枚举值
        IS_ENUM_VALUE,
        //字典值
        IS_DICT_VALUE,
        //对象不为空
        OBJECT_IS_NOT_EMPTY,
        //字符串不为空
        STRING_IS_NOT_BLANK,
        //邮箱
        STRING_IS_EMAIL,
        //手机号
        STRING_IS_PHONE,
        //身份证
        STRING_IS_ID_CARD,
        //统一社会信用代码
        STRING_IS_CREDITCODE,
        //URL
        STRING_IS_URL,
        //IP
        STRING_IS_IP,
        //MAC
        STRING_IS_MAC,
        //数字
        STRING_IS_NUMBER,
        //整数
        STRING_IS_INTEGER,
        //正整数
        STRING_IS_POSITIVE_INTEGER,
        //负整数
        STRING_IS_NEGATIVE_INTEGER,
        //小数
        STRING_IS_FLOAT,
        //正小数
        STRING_IS_POSITIVE_FLOAT,
        //负小数
        STRING_IS_NEGATIVE_FLOAT,
        //日期
        STRING_IS_DATE,
        //JSON
        STRING_IS_JSON_VALUE,
        //JSON对象
        STRING_IS_JSONOBJECT,
        //JSON数组
        STRING_IS_JSONARRAY,
        //汉字
        STRING_IS_CHINESE,
        //字符串长度范围
        STRING_OF_LENGTH_RANGE,
        //数值范围
        NUMBER_OF_VALUE_RANGE ,
        //整数
        NUMBER_IS_INTEGER,
        //正整数
        NUMBER_IS_POSITIVE_INTEGER,
        //负整数
        NUMBER_IS_NEGATIVE_INTEGER,
        //小数
        NUMBER_IS_FLOAT,
        //正小数
        NUMBER_IS_POSITIVE_FLOAT,
        //负小数
        NUMBER_IS_NEGATIVE_FLOAT,
        //数组长度范围
        ARRAY_OF_SIZE_RANGE,
        //集合大小范围
        COLLECTION_OF_SIZE_RANGE
    }

}
