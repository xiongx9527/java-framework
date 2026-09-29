package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chat;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.language.StreamingLanguageModel;

public class MagicDynamicStreamingLanguageModel implements StreamingLanguageModel {

    private final Langchain4jStreamingLanguageModelModule streamingLanguageModel;

    private final String key;

    public MagicDynamicStreamingLanguageModel(Langchain4jStreamingLanguageModelModule streamingLanguageModel, String key) {
        this.streamingLanguageModel = streamingLanguageModel;
        this.key = key;
    }
    @Override
    public void generate(String prompt, StreamingResponseHandler<String> handler) {
        try{
            streamingLanguageModel.getDynamicAttribute(key).generate(prompt, handler);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                SpringUtils.getBean(StreamingLanguageModel.class).generate(prompt, handler);
            }
            SpringUtils.getBean(key,StreamingLanguageModel.class).generate(prompt, handler);
        }

    }
}
