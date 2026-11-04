package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

/** 显式区分不同 LLM 请求协议；厂商名称不能代替协议名称。 */
public enum LlmProtocol {
    OPENAI_CHAT_COMPLETIONS,
    AZURE_OPENAI_CHAT_COMPLETIONS,
    OPENAI_RESPONSES,
    ANTHROPIC_MESSAGES,
    GEMINI
}
