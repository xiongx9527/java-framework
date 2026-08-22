package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public final class LangChain4jChatModelAdapter {

    private LangChain4jChatModelAdapter() {
    }

    public static ChatModel asChatModel(Object model) {
        return model instanceof ChatModel ? (ChatModel) model : null;
    }

    public static StreamingChatModel asStreamingChatModel(Object model) {
        return model instanceof StreamingChatModel ? (StreamingChatModel) model : null;
    }
}
