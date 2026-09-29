package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.language.StreamingLanguageModel;

/** {@code langchain4j-streaming-language-model} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jStreamingLanguageModelDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<StreamingLanguageModel> {

    public MagicDynamicLangchain4jStreamingLanguageModelDataSource() {
        super(Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL);
    }
}
