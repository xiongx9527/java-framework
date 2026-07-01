package com.github.yulichang.autoconfigure.conditional;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * JoinSqlInjectorCondition注解用于标记需要进行SQL注入条件判断的类或方法。
 * 该注解结合了条件化配置，只有在满足OnSqlInjectorCondition条件时才会生效。
 *
 * <p>该注解可以应用于以下目标：
 * <ul>
 *   <li>类型（类、接口等）</li>
 *   <li>方法</li>
 * </ul>
 *
 * <p>注解特性：
 * <ul>
 *   <li>使用@Documented标记，表示该注解应该被javadoc工具记录</li>
 *   <li>运行时保留策略，可以在运行时通过反射获取该注解信息</li>
 *   <li>结合@Conditional条件注解，只有当OnSqlInjectorCondition条件满足时才应用此注解标记的组件</li>
 * </ul>
 *
 * @see OnSqlInjectorCondition 条件判断类
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(OnSqlInjectorCondition.class)
public @interface JoinSqlInjectorCondition {
}
