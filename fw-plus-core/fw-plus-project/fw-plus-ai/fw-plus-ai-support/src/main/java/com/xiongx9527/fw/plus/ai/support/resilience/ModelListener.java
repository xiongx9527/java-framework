package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.support.context.CallContext;

/**
 * 模型调用边界监听器：在「请求前 / 响应后 / 异常」三个点做横切处理。
 *
 * <p><b>作用</b>：日志、指标、成本、审计、脱敏、灰度打标这些能力都不该侵入生成逻辑，统一挂在这里。
 * 三后端（native / langchain4j / spring ai）的调用点各不相同，靠这个中性接口把它们对齐到同一组回调。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.11 ModelListener。
 * Spring AI 侧由 Flux 回调 / ObservationHandler 反调实现；langchain4j 侧由装饰 {@code ChatModel} 实现。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>全部方法默认空实现，实现类只覆写关心的回调。</li>
 *   <li><b>监听器不得抛异常</b>：任何失败必须自行吞掉并记日志，否则会污染正常调用链。</li>
 *   <li>回调可能并发执行，实现需线程安全。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface ModelListener {

    /**
     * 请求发出前回调。
     *
     * @param context  调用上下文
     * @param modelId  目标模型标识
     * @param messages 已序列化的消息摘要（实现可截断），只读
     */
    default void onRequest(CallContext context, String modelId, Object messages) {
    }

    /**
     * 收到完整响应后回调。
     *
     * @param context  调用上下文
     * @param modelId  模型标识
     * @param response 响应对象（后端各自的响应 DTO）
     * @param latencyMs 端到端耗时（毫秒）
     */
    default void onResponse(CallContext context, String modelId, Object response, long latencyMs) {
    }

    /**
     * 调用失败后回调。
     *
     * @param context   调用上下文
     * @param modelId   模型标识
     * @param error     失败原因
     * @param latencyMs 失败前耗时（毫秒）
     */
    default void onError(CallContext context, String modelId, Throwable error, long latencyMs) {
    }
}
