package com.xiongx9527.fw.plus.ai.autoconfigure.store;

import com.xiongx9527.fw.plus.ai.base.store.Store;

/**
 * 向量库 / 图库自动装配的共用骨架：把「未配置占位」与「已配置真实端口」的二选一逻辑收敛到一处，
 * 由子类提供端口类型与单实例的构建方式。
 *
 * <p>多实例由子类决定如何取「默认实例」与「命名实例集合」，本类只负责每种实例
 * {@code configured()=false → 占位端口} / {@code 已配置 → 真实端口} 的统一判定。</p>
 *
 * @param <P> 端口类型（{@code VectorStorePort} / {@code GraphStorePort}），均继承自 {@link Store}
 */
public abstract class AbstractStoreAutoConfiguration<P extends Store> {

    /** 未配置占位端口（子类按端口类型提供，如 {@code UnconfiguredVectorStorePort}）。 */
    protected abstract P buildPlaceholder(AbstractStoreInstance instance);

    /** 由单实例配置构建真实端口（子类按厂商 / 协议 / 个性化字段装配）。 */
    protected abstract P buildConfigured(AbstractStoreInstance instance);

    /** 统一的二选一：未配置 → 占位；已配置 → 真实端口。 */
    protected P resolve(AbstractStoreInstance instance) {
        if (instance == null || !instance.isConfigured()) {
            return buildPlaceholder(instance);
        }
        return buildConfigured(instance);
    }
}
