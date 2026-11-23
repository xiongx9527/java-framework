package com.xiongx9527.fw.plus.ai.nativeimpl.rerank;

/** 显式区分不同重排模型请求协议；厂商名称不能代替协议名称。 */
public enum RerankProtocol {
    TEI,
    /** Cohere 风格，遵循本项目对外统一规范 §3：{query,documents,top_n,return_documents} → {results:[{index,relevance_score}]}。 */
    COHERE
}
