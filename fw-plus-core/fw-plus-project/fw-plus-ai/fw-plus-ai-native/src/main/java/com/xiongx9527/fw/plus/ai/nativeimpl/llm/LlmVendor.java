package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

/** LLM 服务厂商。厂商与具体请求协议分开表达，由 resolver 校验二者关系。 */
public enum LlmVendor {
    OPENAI,
    AZURE_OPENAI,
    ANTHROPIC,
    GEMINI
}
