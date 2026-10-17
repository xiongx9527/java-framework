package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * 测试用可编排 ChatModel：按请求内容把「Planner 决策调用」「上下文摘要调用」与「子 agent 调用」分流，
 * 各自按预设队列返回文本，避免相互串号；并记录全部请求文本供上下文 / 摘要注入断言。
 */
final class ScriptedChatModel implements ChatModel {

    private static final String PLANNER_MARKER = "planner expert";

    private static final String SUMMARIZER_MARKER = "Create a short summary";

    private final Deque<String> plannerResponses = new ArrayDeque<>();
    private final Deque<String> summarizerResponses = new ArrayDeque<>();
    private final Deque<String> agentResponses = new ArrayDeque<>();
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());

    ScriptedChatModel planner(String... responses) {
        Collections.addAll(plannerResponses, responses);
        return this;
    }

    /** 上下文摘要调用预设响应，通常传 {@code {"summary":"..."}} JSON。 */
    ScriptedChatModel summarizer(String... responses) {
        Collections.addAll(summarizerResponses, responses);
        return this;
    }

    ScriptedChatModel agent(String... responses) {
        Collections.addAll(agentResponses, responses);
        return this;
    }

    /** 迄今收到的全部请求文本（flatten 后），供上下文 / 摘要注入断言。 */
    List<String> requests() {
        return new ArrayList<>(requests);
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        String text = flatten(chatRequest);
        requests.add(text);
        Deque<String> queue;
        if (text.contains(PLANNER_MARKER)) {
            queue = plannerResponses;
        } else if (text.contains(SUMMARIZER_MARKER)) {
            queue = summarizerResponses;
        } else {
            queue = agentResponses;
        }
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
