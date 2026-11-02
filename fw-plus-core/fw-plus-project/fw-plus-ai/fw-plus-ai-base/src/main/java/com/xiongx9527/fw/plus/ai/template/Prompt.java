package com.xiongx9527.fw.plus.ai.template;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.message.SystemMessage;
import com.xiongx9527.fw.plus.ai.message.UserMessage;

/** 渲染后的提示词，镜像 langchain4j 的 {@code Prompt}（Spring AI 等价 {@code Prompt}）。 */
public class Prompt {
    private final String text;
    public Prompt(String text) { this.text = text; }
    public String text() { return text; }
    public ChatMessage toSystemMessage() { return SystemMessage.from(text); }
    public ChatMessage toUserMessage() { return UserMessage.from(text); }
    public static Prompt from(String text) { return new Prompt(text); }
}
