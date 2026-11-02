package com.xiongx9527.fw.plus.ai.template;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 系统 + 用户双模板，渲染为消息列表，镜像 langchain4j 的 {@code ChatPromptTemplate}（Spring AI 等价 {@code ChatPromptTemplate}）。 */
public class ChatPromptTemplate {
    private final String systemTemplate;
    private final String userTemplate;
    public ChatPromptTemplate(String systemTemplate, String userTemplate) { this.systemTemplate = systemTemplate; this.userTemplate = userTemplate; }
    public List<ChatMessage> render(Map<String, Object> variables) {
        List<ChatMessage> msgs = new ArrayList<>();
        if (systemTemplate != null && !systemTemplate.isEmpty()) {
            msgs.add(new PromptTemplate(systemTemplate).render(variables).toSystemMessage());
        }
        msgs.add(new PromptTemplate(userTemplate).render(variables).toUserMessage());
        return msgs;
    }
    public static ChatPromptTemplate of(String systemTemplate, String userTemplate) { return new ChatPromptTemplate(systemTemplate, userTemplate); }
}
