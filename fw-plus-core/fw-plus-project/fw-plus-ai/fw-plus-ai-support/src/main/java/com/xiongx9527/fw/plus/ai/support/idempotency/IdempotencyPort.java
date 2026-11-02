package com.xiongx9527.fw.plus.ai.support.idempotency;

import java.time.Duration;
import java.util.Optional;

/**
 * 幂等端口：同一个幂等键只允许一次真实执行，重复请求拿回同一结果。
 *
 * <p><b>作用</b>：用户连点、网关重试、前端超时重发都会造成重复扣费与重复执行。
 * 端口把「占位 → 执行 → 落结果」三段式暴露出来：先 {@link #tryBegin} 抢锁，
 * 抢不到说明已有同键请求在执行或已完成，直接查 {@link #result(String)} 复用。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer} 增强清单「请求幂等（Idempotency-Key）」。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #tryBegin(String, Duration)} 必须原子，且在 {@code ttl} 内保持占用。</li>
 *   <li>执行失败必须调用 {@link #abort(String)} 释放，否则该键在 ttl 内不可重试。</li>
 *   <li>{@link #result(String)} 返回的是结果引用（如缓存键或记录 ID），不返回大对象本体。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface IdempotencyPort {

    /**
     * 尝试占用幂等键。
     *
     * @param key 幂等键
     * @param ttl 占用有效期
     * @return 首次占用返回 {@code true}；已存在返回 {@code false}
     */
    boolean tryBegin(String key, Duration ttl);

    /**
     * 记录执行结果。
     *
     * @param key         幂等键
     * @param resultRef   结果引用（缓存键 / 记录 ID）
     */
    void complete(String key, String resultRef);

    /**
     * 查询已有结果引用。
     *
     * @param key 幂等键
     * @return 结果引用；未完成或不存在时为空
     */
    Optional<String> result(String key);

    /**
     * 放弃本次占用（执行失败时调用）。
     *
     * @param key 幂等键
     */
    void abort(String key);
}
