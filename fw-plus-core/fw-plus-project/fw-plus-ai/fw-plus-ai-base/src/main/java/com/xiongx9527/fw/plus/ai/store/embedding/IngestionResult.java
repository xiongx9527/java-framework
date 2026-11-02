package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.model.output.TokenUsage;

/**
 * 文档入库结果。
 *
 * <p><b>作用</b>：入库过程会调用嵌入模型，产生令牌消耗；把用量返回给调用方用于计费与配额核算。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.IngestionResult}。</p>
 *
 * @author xiongxiao
 */
public class IngestionResult {

    private final TokenUsage tokenUsage;

    /**
     * 构造入库结果。
     *
     * @param tokenUsage 嵌入过程消耗的令牌用量，可为 {@code null}
     */
    public IngestionResult(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    /**
     * 令牌用量。
     *
     * @return 令牌用量；未产生消耗或厂商未返回时为 {@code null}
     */
    public TokenUsage tokenUsage() {
        return tokenUsage;
    }
}
