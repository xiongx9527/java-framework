package com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.condition;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * 存在property条件判断
 *
 * @author Mr_wenpan@163.com 2021/8/22 11:24 下午
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class OnExistingPropertyCondition extends SpringBootCondition {

    /**
     * 获取条件匹配结果
     *
     * @param context 条件上下文，提供访问Spring应用上下文的能力
     * @param metadata 带注解的类型元数据，包含类或方法上的注解信息
     * @return ConditionOutcome 匹配结果，包含是否匹配以及匹配原因
     */
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取@ConditionalOnExistingProperty注解上的注解信息
        // 一个@ConditionalOnExistingProperty注解对应一个AnnotationAttributes
        // 如果Bean上配置了多个ConditionalOnExistingProperty，这里就有多个AnnotationAttributes
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(ConditionalOnExistingProperty.class.getName());
        // 获取注解上对应的值
        assert annotationAttributes != null;
        String[] properties = (String[]) annotationAttributes.get("value");

        // 遍历所有配置的属性名，检查每个属性是否存在且不为空
        for (String property : properties) {
            // 通过配置属性名去上下文环境中获取配置值
            String value = context.getEnvironment().getProperty(property);
            // 只要有一个没有获取到，则说明匹配条件不满足
            if (ObjectUtils.isEmpty(value)) {
                return ConditionOutcome.noMatch(
                        // 构建不匹配的原因，方便打印日志
                        ConditionMessage.forCondition(ConditionalOnExistingProperty.class)
                                .because("property (" + property + ") is empty, not matched."));
            }
        }
        // 走到这里说明所有匹配条件都满足了
        return ConditionOutcome.match(
                // 构建匹配的原因，方便打印日志
                ConditionMessage.forCondition(ConditionalOnMissingProperty.class)
                        .because("properties " + ArrayUtils.toString(properties) + " are all not empty, matched."));
    }


}