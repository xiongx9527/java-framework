package com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.condition;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 属性值为空时满足条件
 *
 * @author Mr_wenpan@163.com 2021/8/22 10:30 下午
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class OnMissingPropertyCondition extends SpringBootCondition {

    /**
     * 获取条件匹配结果
     *
     * @param context 条件上下文，提供访问Spring环境和应用上下文的能力
     * @param metadata 带注解的类型元数据，包含注解信息
     * @return ConditionOutcome 匹配结果对象，包含是否匹配以及原因说明
     */
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取注解属性（key:注解属性名，value：注解属性值）
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(ConditionalOnMissingProperty.class.getName());
        // 获取注解中value属性的值
        assert annotationAttributes != null;
        String[] properties = (String[]) annotationAttributes.get("value");

        // 遍历所有指定的属性，检查是否存在非空值
        for (String property : properties) {
            // 从上下文环境中通过属性名获取配置值
            String value = context.getEnvironment().getProperty(property);
            // 一旦有获取到值，则说明不匹配
            if (StringUtils.hasText(value)) {
                return ConditionOutcome.noMatch(
                        // 构建不匹配的原因，方便打印日志
                        ConditionMessage.forCondition(ConditionalOnMissingProperty.class)
                                .because("property (" + property + ") is not empty, not matched.")
                );
            }
        }

        // 走到这里说明上下文环境中没有配置的值
        return ConditionOutcome.match(
                // 构建匹配的原因，方便打印日志
                ConditionMessage.forCondition(ConditionalOnMissingProperty.class)
                        .because("properties " + ArrayUtils.toString(properties) + " are all empty, matched."));
    }

}