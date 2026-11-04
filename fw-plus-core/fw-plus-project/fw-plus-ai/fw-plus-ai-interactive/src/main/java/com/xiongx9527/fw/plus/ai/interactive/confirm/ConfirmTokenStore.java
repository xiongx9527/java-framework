package com.xiongx9527.fw.plus.ai.interactive.confirm;

/**
 * 确认令牌的一次性使用记录（防止 token 被复用提交，见 §6.33.4 / §6.33.5）。
 *
 * <p>默认实现见 {@link InMemoryConfirmTokenStore}；生产可换成 Redis 等共享存储以支持多实例。</p>
 */
public interface ConfirmTokenStore {

    /** 标记令牌已使用；重复标记同一令牌应幂等。 */
    void markUsed(String token);

    /** 令牌是否已被使用过。 */
    boolean isUsed(String token);
}
