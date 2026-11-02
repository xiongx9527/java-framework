package com.xiongx9527.fw.plus.ai.support.resilience;

/**
 * 模型健康与熔断端口：记录每次调用的成败，并回答「这个模型现在还能不能用」。
 *
 * <p><b>作用</b>：欠费、限流、服务抖动都表现为连续失败。熔断的意义是「失败足够多就别再打」，
 * 既保护下游也避免把超时层层放大。健康状态需要多实例共享（否则每个实例各自熔断，流量打散后依然打满），
 * 因此本端口只定义语义，状态存储交给实现（内存 / 共享缓存 / 库）。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.11.3 ModelHealthPort
 * 与增强清单「模型故障转移：候选链 + 熔断 + 多 key」。langchain4j 与 Spring AI 均无等价端口
 * （Spring AI 的 {@code RetryTemplate} 只做重试，不做跨调用熔断）。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #record(String, Outcome, long)} 必须幂等且线程安全，允许高并发写入。</li>
 *   <li>{@link #isAvailable(String)} 只读、快速返回，禁止在此做远程调用以外的阻塞等待。</li>
 *   <li>{@link #reset(String)} 供运维在人工确认恢复后手动放行。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface ModelHealthPort {

    /** 单次调用结果分类。 */
    enum Outcome {
        /** 调用成功。 */
        SUCCESS,
        /** 调用失败（可重试或不可重试由实现结合异常判断）。 */
        FAILURE,
        /** 调用超时。 */
        TIMEOUT
    }

    /**
     * 记录一次调用结果。
     *
     * @param modelId   模型标识
     * @param outcome   结果分类
     * @param latencyMs 耗时（毫秒）
     */
    void record(String modelId, Outcome outcome, long latencyMs);

    /**
     * 判断模型当前是否可用（未熔断）。
     *
     * @param modelId 模型标识
     * @return 可用返回 {@code true}
     */
    boolean isAvailable(String modelId);

    /**
     * 读取模型当前健康状态快照，用于监控与运维展示。
     *
     * @param modelId 模型标识
     * @return 状态快照，永不为 {@code null}
     */
    HealthSnapshot snapshot(String modelId);

    /**
     * 重置模型健康状态（清除熔断标记与失败计数）。
     *
     * @param modelId 模型标识
     */
    void reset(String modelId);

    /** 健康状态快照。 */
    final class HealthSnapshot {
        private final String modelId;
        private final boolean available;
        private final int consecutiveFailures;
        private final long lastFailureAt;
        private final long lastLatencyMs;

        public HealthSnapshot(String modelId, boolean available, int consecutiveFailures, long lastFailureAt, long lastLatencyMs) {
            this.modelId = modelId;
            this.available = available;
            this.consecutiveFailures = consecutiveFailures;
            this.lastFailureAt = lastFailureAt;
            this.lastLatencyMs = lastLatencyMs;
        }

        /** @return 模型标识 */
        public String modelId() { return modelId; }

        /** @return 是否可用 */
        public boolean available() { return available; }

        /** @return 连续失败次数 */
        public int consecutiveFailures() { return consecutiveFailures; }

        /** @return 最近一次失败时间戳（毫秒），无失败为 0 */
        public long lastFailureAt() { return lastFailureAt; }

        /** @return 最近一次调用耗时（毫秒） */
        public long lastLatencyMs() { return lastLatencyMs; }
    }
}
