package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import dev.langchain4j.model.language.StreamingLanguageModel;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 流式语言模型脚本模块：脚本用 {@code streamingLanguageModel['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("langchain4jStreamingLanguageModel")
public class Langchain4jStreamingLanguageModelModule extends AbstractLangchain4jModelModule<StreamingLanguageModel>
        implements DynamicAttribute<StreamingLanguageModel, Langchain4jStreamingLanguageModelModule>, DynamicModule<Langchain4jStreamingLanguageModelModule> {

    public Langchain4jStreamingLanguageModelModule(MagicDynamicLangchain4jStreamingLanguageModelDataSource holder) {
        super(holder, Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL);
    }

    @Override
    public Langchain4jStreamingLanguageModelModule getDynamicModule(MagicScriptContext context) {
        return this;
    }

    @Override
    public Langchain4jStreamingLanguageModelModule setDynamicAttribute(String key, StreamingLanguageModel value) {
        throw new UnsupportedOperationException(
                "脚本不支持设置模型实例，请在控制台维护 langchain4j-streaming-language-model 数据源");
    }

    @Override
    public List<StreamingLanguageModel> defaultMappings() {
        return super.defaultMappings();
    }
}
