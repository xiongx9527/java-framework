package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chat;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;

public class MagicDynamicChatLanguageModel implements ChatModel {

    private final Langchain4jChatModelModule chatModel;

    private final String key;

    public MagicDynamicChatLanguageModel(Langchain4jChatModelModule chatModel, String key) {
        this.chatModel = chatModel;
        this.key = key;
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        try{
            return chatModel.getDynamicAttribute(key).chat(messages);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(ChatModel.class).chat(messages);
            }
            return SpringUtils.getBean(key,ChatModel.class).chat(messages);
        }
    }
}
