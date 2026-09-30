package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** language-model 资源存储：folder=langchain4j-language-model。 */
public class Langchain4jLanguageModelMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jLanguageModelInfo> {

    public Langchain4jLanguageModelMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jLanguageModelMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.LANGUAGE_MODEL, Langchain4jLanguageModelInfo.class, providers);
    }
}
