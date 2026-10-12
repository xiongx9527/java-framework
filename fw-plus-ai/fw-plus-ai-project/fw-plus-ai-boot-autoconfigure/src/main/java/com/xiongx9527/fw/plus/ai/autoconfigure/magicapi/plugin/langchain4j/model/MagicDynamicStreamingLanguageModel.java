package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.language.StreamingLanguageModel;

/**
 * {@link StreamingLanguageModel} 的动态代理：持有脚本模块与资源 key，每次调用实时解析当前
 * 实例，magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的流式语言模型 bean
 * （key 空取默认、非空按 bean 名）。
 */
public class MagicDynamicStreamingLanguageModel implements StreamingLanguageModel {

    private final Langchain4jStreamingLanguageModelModule streamingLanguageModelModule;

    private final String key;

    public MagicDynamicStreamingLanguageModel(Langchain4jStreamingLanguageModelModule streamingLanguageModelModule,
                                              String key) {
        this.streamingLanguageModelModule = streamingLanguageModelModule;
        this.key = key;
    }

    private StreamingLanguageModel getStreamingLanguageModel() {
        try {
            return streamingLanguageModelModule.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(StreamingLanguageModel.class);
            }
            return SpringUtils.getBean(key, StreamingLanguageModel.class);
        }
    }

    @Override
    public void generate(String message, StreamingResponseHandler<String> handler) {
        getStreamingLanguageModel().generate(message, handler);
    }
}
