package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * {@link ChatLanguageModel} 的动态代理：持有脚本模块与资源 key，每次调用实时解析当前实例，
 * magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的模型 bean（key 空取默认、
 * 非空按 bean 名），与 {@code MagicDynamicChatMemoryStore} 的兜底语义一致。
 */
public class MagicDynamicChatModel implements ChatLanguageModel {

    private final Langchain4jChatModelModule chatModelModule;

    private final String key;

    public MagicDynamicChatModel(Langchain4jChatModelModule chatModelModule, String key) {
        this.chatModelModule = chatModelModule;
        this.key = key;
    }

    private ChatLanguageModel getChatModel() {
        try {
            return chatModelModule.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(ChatLanguageModel.class);
            }
            return SpringUtils.getBean(key, ChatLanguageModel.class);
        }
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return getChatModel().generate(messages);
    }
}
