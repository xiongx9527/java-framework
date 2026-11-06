package com.xiongx9527.fw.plus.ai.nativeimpl.rerank;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;

import java.util.Collections;
import java.util.List;

/**
 * 未配置占位端口：重排模型未配置任何厂商 / 协议时暴露，保证应用缺省也能启动；
 * {@link #score(String, List)} 返回空列表（表达「无法重排」，由调用方降级）。
 */
public class UnconfiguredRerankPort implements RerankPort {

    @Override
    public List<Double> score(String query, List<String> documents) {
        return Collections.emptyList();
    }
}
