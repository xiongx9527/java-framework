package com.xx9527.fw.plus.easytrans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TransMethodArg注解用于标记需要进行转换处理的方法参数。
 * 该注解可以在运行时被反射机制获取到，并且只能应用于方法上。
 *
 * 使用场景：
 * - 标记需要特殊转换逻辑的方法参数
 * - 配合AOP或拦截器实现参数的自动转换
 *
 * 注意事项：
 * - 该注解保留策略为RUNTIME，可在运行时通过反射获取
 * - 作用目标仅限于方法级别
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TransMethodArg {
}
