package com.xiongx9527.fw.plus.common.debounce;

/**
 * 防抖键的当前用户解析 SPI。
 *
 * <p>框架不假设具体认证体系：业务侧（如 Sa-Token / Spring Security / 支线 token）提供实现，
 * 返回当前登录用户 id；取不到时返回 {@code null}，防抖键回退为匿名（anon）。</p>
 */
@FunctionalInterface
public interface DebounceUserResolver {

    /**
     * @return 当前用户标识；未登录或取不到时返回 {@code null}
     */
    String currentUserId();
}
