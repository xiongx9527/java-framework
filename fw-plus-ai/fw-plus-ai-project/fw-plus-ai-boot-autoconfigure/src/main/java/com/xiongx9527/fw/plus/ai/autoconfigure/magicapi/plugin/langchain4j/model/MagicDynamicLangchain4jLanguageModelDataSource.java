package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.language.LanguageModel;

/** {@code langchain4j-language-model} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jLanguageModelDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<LanguageModel> {

    public MagicDynamicLangchain4jLanguageModelDataSource() {
        super(Langchain4jModelTypes.LANGUAGE_MODEL);
    }
}
