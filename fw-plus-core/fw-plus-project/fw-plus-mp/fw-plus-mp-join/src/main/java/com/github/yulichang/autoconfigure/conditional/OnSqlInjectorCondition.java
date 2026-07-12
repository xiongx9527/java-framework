package com.github.yulichang.autoconfigure.conditional;

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * @author yulichang
 * @since 1.4.5
 */
public class OnSqlInjectorCondition implements Condition {

    /**
     * 判断是否匹配条件
     *
     * @param conditionContext 条件上下文，提供访问Bean工厂、环境等信息
     * @param annotatedTypeMetadata 注解类型元数据，包含类型上的注解信息
     * @return 如果匹配返回true，否则返回false
     */
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        // 检查Bean工厂是否存在
        if (Objects.nonNull(conditionContext.getBeanFactory())) {
            // 获取所有ISqlInjector类型的Bean名称
            String[] names = conditionContext.getBeanFactory().getBeanNamesForType(ISqlInjector.class);
            for (String name : names) {
                // 获取Bean定义
                BeanDefinition definition = conditionContext.getBeanFactory().getBeanDefinition(name);
                // 检查是否为注解Bean定义
                if (definition instanceof AnnotatedBeanDefinition) {
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) definition;
                    // 获取工厂方法元数据
                    MethodMetadata metadata = annotatedBeanDefinition.getFactoryMethodMetadata();
                    if (Objects.nonNull(metadata)) {
                        // 遍历方法上的所有注解
                        MergedAnnotations annotations = metadata.getAnnotations();
                        for (MergedAnnotation<Annotation> it : annotations) {
                            // 如果发现@Primary注解，则返回false
                            if (Objects.equals(it.getType(), Primary.class)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        // 默认返回true
        return true;
    }


}