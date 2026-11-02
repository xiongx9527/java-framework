package com.xiongx9527.fw.plus.ai.support.cost;

import java.math.BigDecimal;

/**
 * 单次调用的成本用量：token 数 + 折算金额。
 *
 * <p><b>作用</b>：成本核算需要「谁、用哪个模型、消耗多少、折多少钱」四要素。前两项在
 * {@link com.xiongx9527.fw.plus.ai.support.context.CallContext} 里，本类型补齐后两项，
 * 由 {@link CostPort#record(CostUsage)} 落库或上报。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer} 增强清单「成本核算（模型单价表 + 每次调用落库）」。
 * langchain4j 的 {@code TokenUsage} 只有 token 数、无金额与币种。</p>
 *
 * @author xiongxiao
 */
public final class CostUsage {

    private final String modelId;
    private final long inputTokens;
    private final long outputTokens;
    private final BigDecimal amount;
    private final String currency;

    /**
     * 构造用量。
     *
     * @param modelId      模型标识
     * @param inputTokens  输入 token 数
     * @param outputTokens 输出 token 数
     * @param amount       折算金额
     * @param currency     币种（如 {@code CNY} / {@code USD}）
     */
    public CostUsage(String modelId, long inputTokens, long outputTokens, BigDecimal amount, String currency) {
        this.modelId = modelId;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.amount = amount;
        this.currency = currency;
    }

    /** @return 模型标识 */
    public String modelId() { return modelId; }

    /** @return 输入 token 数 */
    public long inputTokens() { return inputTokens; }

    /** @return 输出 token 数 */
    public long outputTokens() { return outputTokens; }

    /** @return 折算金额，可能为 {@code null}（单价未知时） */
    public BigDecimal amount() { return amount; }

    /** @return 币种 */
    public String currency() { return currency; }

    /** @return token 总数 */
    public long totalTokens() { return inputTokens + outputTokens; }
}
