package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jTokenizerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** tokenizer 资源存储：folder=langchain4j-tokenizer。 */
public class Langchain4jTokenizerMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jTokenizerInfo> {

    public Langchain4jTokenizerMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jTokenizerMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.TOKENIZER, Langchain4jTokenizerInfo.class, providers);
    }
}
