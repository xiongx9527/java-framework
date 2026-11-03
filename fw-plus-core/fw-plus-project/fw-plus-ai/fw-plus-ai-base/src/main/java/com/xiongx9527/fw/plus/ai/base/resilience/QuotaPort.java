package com.xiongx9527.fw.plus.ai.base.resilience;

/**
 * 配额：按作用域（租户 / 模型 / 应用）限流。
 */
public interface QuotaPort {

    /**
     * 尝试占用配额。
     *
     * @param scope  作用域标识
     * @param amount 需要的配额（如 token 数或请求数）
     * @return 是否允许本次调用
     */
    boolean tryAcquire(String scope, long amount);
}
