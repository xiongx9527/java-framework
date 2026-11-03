package com.xiongx9527.fw.plus.ai.base.idempotency;

/**
 * 幂等端口：先 {@link #tryStart} 抢锁，执行完 {@link #complete} 写结果；
 * 重复请求直接拿 {@link #result} 返回，不再重复调用模型。
 */
public interface IdempotencyPort {

    /** 抢占执行权；返回 {@code false} 表示已有同 key 的请求在处理或已完成。 */
    boolean tryStart(String key, long ttlMillis);

    /** 写入结果。 */
    void complete(String key, String result);

    /** 已完成的结果；未完成返回 {@code null}。 */
    String result(String key);
}
