package com.xiongx9527.fw.plus.common.debounce;

/**
 * 防抖后端选择 SPI。
 *
 * <p>框架默认在 Redis 原子占位可用时优先使用 Redis（跨实例严格只放行一次）；
 * 业务侧若把「缓存后端」与「防抖后端」绑定（例如统一由 {@code askdata.cache.type} 控制），
 * 可提供实现覆盖默认选择。</p>
 */
@FunctionalInterface
public interface DebounceBackendResolver {

    /**
     * @return true 表示使用 Redis 原子占位；false 表示使用 Spring Cache 占位
     */
    boolean useRedis();
}
