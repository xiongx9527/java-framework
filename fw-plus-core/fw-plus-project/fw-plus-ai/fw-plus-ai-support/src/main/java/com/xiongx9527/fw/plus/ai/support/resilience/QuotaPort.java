package com.xiongxiao.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.support.context.CallContext;

/**
 * 配额与限流端口：按租户 / 用户 / 模型维度发放调用许可。
 *
 * <p><b>作用</b>：多租户产品必须回答「谁把额度用完了」。配额是<b>长期累计</b>（日 / 月 token 或调用数），
 * 限流是<b>瞬时速率</b>（每秒请求数、并发数）。两者判定逻辑不同但都是「能不能放行」，
 * 收在同一个端口，由实现决定用令牌桶、滑动窗口还是库表计数。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.16 QuotaPort
 * 与增强清单「配额与限流」。langchain4j 与 Spring AI 均无等价端口。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #tryAcquire(CallContext, int)} 必须原子：并发下不得超发。</li>
 *   <li>被拒绝时返回的 {@link QuotaDecision} 必须带原因与建议（提示用户何时可重试）。</li>
 *   <li>许可消耗后若调用失败需要退还，调用 {@link #release(CallContext, int)}。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface QuotaPort {

    /**
     * 尝试获取许可。
     *
     * @param context 调用上下文（租户 / 用户 / 模型维度取自此处）
     * @param permits 申请量（调用数或 token 预估）
     * @return 判定结果，永不为 {@code null}
     */
    QuotaDecision tryAcquire(CallContext context, int permits);

    /**
     * 退还许可（调用失败或提前终止时调用）。
     *
     * @param context 调用上下文
     * @param permits 退还量
     */
    void release(CallContext context, int permits);

    /**
     * 读取剩余额度，用于前端展示与告警。
     *
     * @param context 调用上下文
     * @return 剩余额度，未知时返回 -1
     */
    long remaining(CallContext context);

    /** 配额判定结果。 */
    final class QuotaDecision {
        private final boolean allowed;
        private final String reason;
        private final long retryAfterMs;

        private QuotaDecision(boolean allowed, String reason, long retryAfterMs) {
            this.allowed = allowed;
            this.reason = reason;
            this.retryAfterMs = retryAfterMs;
        }

        /**
         * 放行。
         *
         * @return 放行结果
         */
        public static QuotaDecision allow() { return new QuotaDecision(true, null, 0L); }

        /**
         * 拒绝。
         *
         * @param reason       拒绝原因（i18n key 或文案）
         * @param retryAfterMs 建议重试等待毫秒数，0 表示不限期
         * @return 拒绝结果
         */
        public static QuotaDecision deny(String reason, long retryAfterMs) {
            return new QuotaDecision(false, reason, retryAfterMs);
        }

        /** @return 是否放行 */
        public boolean allowed() { return allowed; }

        /** @return 拒绝原因 */
        public String reason() { return reason; }

        /** @return 建议重试等待毫秒数 */
        public long retryAfterMs() { return retryAfterMs; }
    }
}
