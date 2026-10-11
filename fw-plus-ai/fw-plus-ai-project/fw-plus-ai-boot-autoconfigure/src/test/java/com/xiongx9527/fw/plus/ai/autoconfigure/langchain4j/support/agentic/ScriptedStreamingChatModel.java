package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 测试用可编排 StreamingChatModel：按请求内容把「子 agent 调用」分流，
 * 各自按预设队列返回 token 流，避免相互串号。
 */
final class ScriptedStreamingChatModel implements StreamingChatModel {

    private static final String PLANNER_MARKER = "planner expert";

    private final Deque<List<String>> agentResponses = new ArrayDeque<>();

    ScriptedStreamingChatModel agent(String... tokenSets) {
        for (String tokenSet : tokenSets) {
            agentResponses.add(List.of(tokenSet.split("")));
        }
        return this;
    }

    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        String text = flatten(chatRequest);
        Deque<List<String>> queue = text.contains(PLANNER_MARKER) ? new ArrayDeque<>() : agentResponses;
        List<String> tokens = queue.poll();
        if (tokens == null) {
            tokens = List.of("");
        }
        for (String token : tokens) {
            handler.onPartialResponse(token);
        }
        handler.onCompleteResponse(ChatResponse.builder()
                .aiMessage(AiMessage.from(String.join("", tokens)))
                .build());
    }

    private static String flatten(ChatRequest chatRequest) {
        StringBuilder builder = new StringBuilder();
        for (var message : chatRequest.messages()) {
            if (message instanceof dev.langchain4j.data.message.SystemMessage systemMessage) {
                builder.append(systemMessage.text()).append('\n');
            } else if (message instanceof dev.langchain4j.data.message.UserMessage userMessage) {
                builder.append(userMessage.singleText()).append('\n');
            } else if (message instanceof AiMessage aiMessage) {
                builder.append(aiMessage.text()).append('\n');
            }
        }
        return builder.toString();
    }
}
