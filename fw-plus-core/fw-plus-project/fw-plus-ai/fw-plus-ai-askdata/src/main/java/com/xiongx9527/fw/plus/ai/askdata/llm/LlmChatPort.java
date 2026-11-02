package com.xiongx9527.fw.plus.ai.askdata.llm;

/**
 * 中立的聊天完成口屏（§6.38.8）。与具体 LLM 框架无关，
 * 由各后端 adapter（native / langchain4j / spring-ai）落地实现。
 */
public interface LlmChatPort {
    /** 给定 system 与 user 提示词，返回模型原始输出。 */
    String complete(String systemPrompt, String userPrompt);
}
