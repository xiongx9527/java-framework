package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

/**
 * 测试用可编排 ChatModel：按请求内容把「Planner 决策调用」与「子 agent 调用」分流，
 * 各自按预设队列返回文本，避免相互串号。
 */
final class ScriptedChatModel implements ChatModel {

    private static final String PLANNER_MARKER = "planner expert";

    private final Deque<String> plannerResponses = new ArrayDeque<>();
    private final Deque<String> agentResponses = new ArrayDeque<>();

    ScriptedChatModel planner(String... responses) {
        Collections.addAll(plannerResponses, responses);
        return this;
    }

    ScriptedChatModel agent(String... responses) {
        Collections.addAll(agentResponses, responses);
        return this;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        String text = flatten(chatRequest);
        Deque<String> queue = text.contains(PLANNER_MARKER) ? plannerResponses : agentResponses;
        String response = queue.poll();
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(response == null ? "" : response))
                .build();
    }

    private static String flatten(ChatRequest chatRequest) {
        StringBuilder builder = new StringBuilder();
        for (ChatMessage message : chatRequest.messages()) {
            if (message instanceof SystemMessage systemMessage) {
                builder.append(systemMessage.text()).append('\n');
            } else if (message instanceof UserMessage userMessage) {
                builder.append(userMessage.singleText()).append('\n');
            } else if (message instanceof AiMessage aiMessage) {
                builder.append(aiMessage.text()).append('\n');
            }
        }
        return builder.toString();
    }
}
