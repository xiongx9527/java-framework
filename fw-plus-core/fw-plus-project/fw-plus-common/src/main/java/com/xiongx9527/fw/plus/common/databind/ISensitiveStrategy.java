package com.xiongx9527.fw.plus.common.databind;

import java.util.Map;
import java.util.function.Function;

/**
 * @author xiongxiao
 */
public interface ISensitiveStrategy {
    /**
     * 根据指定类型处理给定值，并返回处理结果
     * 该方法使用策略模式，根据类型从策略函数映射中获取对应的处理函数，
     * 然后使用该函数处理给定值，返回处理后的结果
     *
     * @param type 类型标识符，用于从策略函数映射中选择合适的处理函数
     * @param value 待处理的值，其具体处理方式取决于所选的处理函数
     * @return 处理后的字符串结果
     */
    default String handle(String type, String value) {
        if (!this.getStrategyFunctionMap().containsKey(type)) {
            return value;
        }
        // 从策略函数映射中获取与给定类型相对应的处理函数，并应用该函数处理给定值
        // 注意：这里假设getStrategyFunctionMap()返回一个Map，其中键为类型标识符，值为Function对象
        return (String)((Function)this.getStrategyFunctionMap().get(type)).apply(value);
    }

    /**
     * 获取策略函数映射，用于根据类型选择对应的处理函数
     *
     * @return 策略函数映射，键为类型标识符，值为Function对象
     */
    Map<String, Function<String, String>> getStrategyFunctionMap();
}