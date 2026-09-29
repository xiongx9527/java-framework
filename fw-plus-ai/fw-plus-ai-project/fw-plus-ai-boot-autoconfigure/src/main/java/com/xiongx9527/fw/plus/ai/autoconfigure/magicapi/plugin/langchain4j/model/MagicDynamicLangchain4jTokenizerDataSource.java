package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.Tokenizer;

/** {@code langchain4j-tokenizer} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jTokenizerDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<Tokenizer> {

    public MagicDynamicLangchain4jTokenizerDataSource() {
        super(Langchain4jModelTypes.TOKENIZER);
    }
}
