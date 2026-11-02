package com.xiongx9527.fw.plus.ai.support.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * 缓存端口：按命名空间读写缓存值，并支持「纪元」整体失效。
 *
 * <p><b>作用</b>：问数结果、语义向量、schema 摘要都能缓存，但它们各自有失效触发源
 * （表结构变更、术语变更、模型切换、权限变更）。逐 key 清理不可靠，因此引入
 * <b>纪元（epoch）</b>：每次触发源变化就把该命名空间的纪元 +1，旧纪元的 key 自然失效，
 * 无需遍历删除。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.35 与
 * 增强清单「结果缓存」「语义缓存」「cache_epoch 表 + 失效矩阵」。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #get(String, String)} 命中返回 {@code Optional.of(...)}，未命中返回空；<b>不得返回 {@code null}</b>。</li>
 *   <li>{@link #invalidate(String)} 只递增纪元，不做物理删除；物理回收交给实现的淘汰策略。</li>
 *   <li>缓存值必须自带版本信息，跨版本升级时旧值不应被反序列化成功。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface CachePort {

    /**
     * 读取缓存。
     *
     * @param namespace 命名空间（如 {@code sql-result} / {@code schema-summary}）
     * @param key       业务键
     * @return 命中值
     */
    Optional<String> get(String namespace, String key);

    /**
     * 写入缓存。
     *
     * @param namespace 命名空间
     * @param key       业务键
     * @param value     值
     * @param ttl       存活时间，{@code null} 表示用实现默认值
     */
    void put(String namespace, String key, String value, Duration ttl);

    /**
     * 整体失效一个命名空间（纪元 +1）。
     *
     * @param namespace 命名空间
     */
    void invalidate(String namespace);

    /**
     * 读取命名空间当前纪元。
     *
     * @param namespace 命名空间
     * @return 纪元号，从未失效过返回 0
     */
    long epoch(String namespace);
}
