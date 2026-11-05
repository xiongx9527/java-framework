package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

/** LangChain4j 聊天模型厂商。协议显式配置时由协议决定具体端口。 */
public enum LlmVendor {
    OPENAI,
    AZURE_OPENAI,
    ANTHROPIC,
    GEMINI,
    DASHSCOPE,
    QWEN,
    GITHUB_MODELS,
    OLLAMA,
    QIANFAN,
    VERTEX_AI_GEMINI,
    GOOGLE_AI_GEMINI,
    HUGGING_FACE,
    LOCAL_AI,
    ZHIPU_AI,
    COHERE
}
