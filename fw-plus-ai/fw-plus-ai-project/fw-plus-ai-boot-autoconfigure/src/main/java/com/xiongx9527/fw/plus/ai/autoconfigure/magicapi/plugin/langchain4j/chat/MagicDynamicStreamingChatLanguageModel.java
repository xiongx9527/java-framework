package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chat;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.List;

public class MagicDynamicStreamingChatLanguageModel implements StreamingChatModel {

    private final Langchain4jStreamingChatModelModule streamingChatModel;

    private final String key;

    public MagicDynamicStreamingChatLanguageModel(Langchain4jStreamingChatModelModule streamingChatModel, String key) {
        this.streamingChatModel = streamingChatModel;
        this.key = key;
    }


    @Override
    public void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        try{
            streamingChatModel.getDynamicAttribute(key).chat(messages, handler);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                SpringUtils.getBean(StreamingChatModel.class).chat(messages, handler);
            } else {
                SpringUtils.getBean(key,StreamingChatModel.class).chat(messages, handler);
            }
        }
    }
}
