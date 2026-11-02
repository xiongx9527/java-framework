package com.xiongx9527.fw.plus.common.debounce;

/**
 * 防抖原子占位槽（SPI）：跨实例后端的抽象。
 *
 * <p>框架默认提供 Spring Cache 单实例占位；引入 Redis 且存在 {@code StringRedisTemplate} 时，
 * 由 {@code fw-plus-boot} 装配 Redis 实现。业务侧也可自行提供实现接管占位逻辑。</p>
 *
 * <p>该接口不依赖任何 Redis 类型，保证未引入 spring-data-redis 的应用可以正常加载防抖组件。</p>
 */
@FunctionalInterface
public interface DebounceSlot {

    /**
     * 尝试占位。
     *
     * @param key      完整占位键（已包含前缀）
     * @param windowMs 本次窗口（毫秒，&gt; 0）
     * @return true 表示占位成功、本次放行；false 表示窗口内已存在占位、应拦截
     */
    boolean acquire(String key, long windowMs);
}
