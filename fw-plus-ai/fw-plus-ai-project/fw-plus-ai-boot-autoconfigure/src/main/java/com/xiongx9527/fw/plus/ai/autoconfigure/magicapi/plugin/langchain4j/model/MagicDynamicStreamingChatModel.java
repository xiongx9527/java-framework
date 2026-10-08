package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.util.List;

/**
 * {@link StreamingChatLanguageModel} 的动态代理：持有脚本模块与资源 key，每次调用实时解析
 * 当前实例，magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的流式模型 bean
 * （key 空取默认、非空按 bean 名）。
 */
public class MagicDynamicStreamingChatModel implements StreamingChatLanguageModel {

    private final Langchain4jStreamingChatModelModule streamingChatModelModule;

    private final String key;

    public MagicDynamicStreamingChatModel(Langchain4jStreamingChatModelModule streamingChatModelModule,
                                          String key) {
        this.streamingChatModelModule = streamingChatModelModule;
        this.key = key;
    }

    private StreamingChatLanguageModel getStreamingChatModel() {
        try {
            return streamingChatModelModule.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(StreamingChatLanguageModel.class);
            }
            return SpringUtils.getBean(key, StreamingChatLanguageModel.class);
        }
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        getStreamingChatModel().generate(messages, handler);
    }
}
