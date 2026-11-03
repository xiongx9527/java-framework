package com.xiongx9527.fw.plus.ai.support.idempotency;

import com.xiongx9527.fw.plus.ai.base.idempotency.IdempotencyPort;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 幂等执行：把「抢锁 → 执行 → 写结果 → 重复请求直取结果」的样板收敛成一次调用。
 *
 * <p>{@code IdempotencyPort} 的实现负责把结果序列化落库；这里只负责流程编排，
 * 因此天然属于 support 而非 base。</p>
 */
public final class IdempotentCalls {

    private IdempotentCalls() {
    }

    /**
     * 幂等执行。
     *
     * @param port        幂等端口
     * @param key         业务唯一键（如 prompt 指纹）
     * @param ttlMillis   锁与结果存活时间
     * @param op          实际执行（仅在抢到锁时调用）
     * @param serialize   结果 → 字符串（落库用）
     * @param deserialize 字符串 → 结果（重复请求直取用）
     */
    public static <T> T execute(IdempotencyPort port, String key, long ttlMillis,
                                Supplier<T> op, Function<T, String> serialize,
                                Function<String, T> deserialize) {
        if (port == null) {
            return op.get();
        }
        String existing = port.result(key);
        if (existing != null) {
            return deserialize.apply(existing);
        }
        if (!port.tryStart(key, ttlMillis)) {
            String retry = port.result(key);
            if (retry != null) {
                return deserialize.apply(retry);
            }
            return op.get();
        }
        T value = op.get();
        port.complete(key, serialize.apply(value));
        return value;
    }
}
