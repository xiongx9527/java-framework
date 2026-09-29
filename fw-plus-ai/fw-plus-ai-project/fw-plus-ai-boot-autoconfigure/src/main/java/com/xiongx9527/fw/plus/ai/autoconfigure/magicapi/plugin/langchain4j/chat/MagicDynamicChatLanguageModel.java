package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chat;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.List;

public class MagicDynamicChatLanguageModel implements ChatLanguageModel {

    private final Langchain4jChatModelModule chatModel;

    private final String key;

    public MagicDynamicChatLanguageModel(Langchain4jChatModelModule chatModel, String key) {
        this.chatModel = chatModel;
        this.key = key;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        try{
            return chatModel.getDynamicAttribute(key).generate(messages);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(ChatLanguageModel.class).generate(messages);
            }
            return SpringUtils.getBean(key,ChatLanguageModel.class).generate(messages);
        }
    }
}
