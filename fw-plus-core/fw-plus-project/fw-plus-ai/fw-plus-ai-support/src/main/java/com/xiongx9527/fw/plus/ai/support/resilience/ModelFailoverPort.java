package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.support.context.CallContext;

import java.util.List;

/**
 * 模型故障转移端口：给定首选模型，回答「这次该用哪个模型」。
 *
 * <p><b>作用</b>：欠费 / 限流 / 区域故障时，产品应当自动切到备用模型或备用 key，而不是直接报错。
 * 候选链由配置决定（同厂商不同 key、不同厂商同能力档位），选择策略由实现决定
 * （按健康状态过滤 → 按优先级取第一个可用）。本端口只暴露「选谁」与「回报结果」两件事，
 * 具体怎么调模型仍走 base 的 {@code ChatModel}。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.26
 * 与增强清单「模型故障转移：候选链 + 熔断 + 多 key（欠费自救）」。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #select(CallContext)} 必须永远返回一个模型标识；全部不可用时返回首选模型（让下游报出真实错误），
 *       不得返回 {@code null}。</li>
 *   <li>{@link #onSuccess(String)} / {@link #onFailure(String, Throwable)} 与 {@link ModelHealthPort} 联动，
 *       实现可以内部委托给健康端口。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface ModelFailoverPort {

    /**
     * 选出本次调用要用的模型。
     *
     * @param context 调用上下文（首选模型取 {@link CallContext#modelId()}）
     * @return 模型标识，永不为 {@code null}
     */
    String select(CallContext context);

    /**
     * 列出某首选模型的可选候选链（含首选本身，按优先级排序）。
     *
     * @param primaryModelId 首选模型标识
     * @return 只读候选列表，永不为 {@code null}
     */
    List<String> candidates(String primaryModelId);

    /**
     * 回报调用成功。
     *
     * @param modelId 模型标识
     */
    void onSuccess(String modelId);

    /**
     * 回报调用失败。
     *
     * @param modelId 模型标识
     * @param error   失败原因
     */
    void onFailure(String modelId, Throwable error);
}
