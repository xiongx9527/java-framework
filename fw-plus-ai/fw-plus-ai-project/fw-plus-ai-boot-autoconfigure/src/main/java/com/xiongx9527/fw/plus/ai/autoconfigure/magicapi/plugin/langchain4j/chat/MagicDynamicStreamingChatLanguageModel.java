package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chat;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.util.List;

public class MagicDynamicStreamingChatLanguageModel implements StreamingChatLanguageModel {

    private final Langchain4jStreamingChatModelModule streamingChatModel;

    private final String key;

    public MagicDynamicStreamingChatLanguageModel(Langchain4jStreamingChatModelModule streamingChatModel, String key) {
        this.streamingChatModel = streamingChatModel;
        this.key = key;
    }


    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        try{
            streamingChatModel.getDynamicAttribute(key).generate(messages, handler);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                SpringUtils.getBean(StreamingChatLanguageModel.class).generate(messages, handler);
            }
            SpringUtils.getBean(key,StreamingChatLanguageModel.class).generate(messages, handler);
        }
    }
}
