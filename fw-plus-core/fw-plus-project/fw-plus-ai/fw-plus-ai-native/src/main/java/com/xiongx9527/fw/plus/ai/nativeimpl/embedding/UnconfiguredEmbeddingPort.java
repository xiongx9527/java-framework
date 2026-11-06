package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;

/**
 * 未配置占位端口：向量模型未配置任何厂商 / 协议时暴露，保证应用缺省也能启动；
 * {@link #embed(String)} 返回 {@code null}（按 {@link EmbeddingPort} 契约表达「该条不可用」）。
 */
public class UnconfiguredEmbeddingPort implements EmbeddingPort {

    @Override
    public double[] embed(String text) {
        return null;
    }
}
