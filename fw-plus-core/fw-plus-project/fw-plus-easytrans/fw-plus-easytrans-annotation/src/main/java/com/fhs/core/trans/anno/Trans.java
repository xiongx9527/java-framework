package com.fhs.core.trans.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Trans注解用于标记需要进行转换处理的字段
 * 该注解可以在运行时被反射获取，只能应用于字段上
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Trans {

    /**
     * 转换类型
     * @return 转换类型的字符串标识
     */
    String type();

    /**
     * 转换键值
     * @return 转换键值，默认为空字符串
     */
    String key() default "";

    /**
     * 引用值
     * @return 引用字符串，默认为空字符串
     */
    String ref() default "";

    /**
     * 多个引用值
     * @return 引用字符串数组，默认为空数组
     */
    String[] refs() default {};

    /**
     * 目标类类型
     * @return 目标类的Class对象，默认为Enum.class
     */
    Class<?> target() default Enum.class;

    /**
     * 字段数组
     * @return 字符串数组，默认为空数组
     */
    String[] fields() default {};

    /**
     * 别名
     * @return 字符串别名，默认为空字符串
     */
    String alias() default "";

    /**
     * 服务名称
     * @return 服务名称字符串，默认为空字符串
     */
    String serviceName() default "";

    /**
     * 服务上下文路径
     * @return 服务上下文路径字符串，默认为空字符串
     */
    String serviceContextPath() default "";

    /**
     * 目标类名
     * @return 目标类名字符串，默认为空字符串
     */
    String targetClassName() default "";

    /**
     * 自定义Bean函数名称
     * @return 自定义Bean函数名称字符串，默认为空字符串
     */
    String customeBeanFuncName() default "";

    /**
     * 数据源
     * @return 数据源字符串，默认为空字符串
     */
    String dataSource() default "";

    /**
     * 唯一字段
     * @return 唯一字段字符串，默认为空字符串
     */
    String uniqueField() default "";

    /**
     * 排序值
     * @return 整数排序值，默认为0
     */
    int sort() default 0;
}
