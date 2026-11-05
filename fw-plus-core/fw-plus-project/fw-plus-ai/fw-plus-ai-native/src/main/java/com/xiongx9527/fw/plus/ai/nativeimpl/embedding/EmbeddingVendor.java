package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

/** 向量模型厂商。协议显式配置时由协议决定具体端口。 */
public enum EmbeddingVendor {
    OPENAI,
    AZURE_OPENAI,
    DASHSCOPE,
    QWEN,
    OLLAMA,
    QIANFAN,
    LOCAL_AI,
    HUGGING_FACE,
    COHERE,
    VOYAGE,
    ZHIPU_AI
}
