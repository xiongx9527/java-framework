package com.xiongx9527.fw.plus.ai.autoconfigure.store.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.store.AbstractStoreInstance;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.PointIdStrategy;

import java.util.Locale;

/**
 * 单个向量库实例配置（多实例模型下的一个条目）。
 *
 * <p>继承 {@link AbstractStoreInstance} 的公共连接 / 鉴权 / 超时字段，并补充向量库特有的
 * {@code pointIdStrategy}（{@code UUID} / {@code RAW}）。</p>
 */
public class VectorStoreInstance extends AbstractStoreInstance {

    /**
     * point id 生成策略：{@code UUID}（默认）或 {@code RAW}（直接以原始字符串 id 作 point id）。
     */
    private String pointIdStrategy = "UUID";

    public String getPointIdStrategy() {
        return pointIdStrategy;
    }

    public void setPointIdStrategy(String pointIdStrategy) {
        this.pointIdStrategy = pointIdStrategy;
    }

    /** 解析为枚举；非法值回退到 {@code UUID}。 */
    public PointIdStrategy resolvePointIdStrategy() {
        if (pointIdStrategy == null || pointIdStrategy.trim().isEmpty()) {
            return PointIdStrategy.UUID;
        }
        try {
            return PointIdStrategy.valueOf(pointIdStrategy.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return PointIdStrategy.UUID;
        }
    }
}
