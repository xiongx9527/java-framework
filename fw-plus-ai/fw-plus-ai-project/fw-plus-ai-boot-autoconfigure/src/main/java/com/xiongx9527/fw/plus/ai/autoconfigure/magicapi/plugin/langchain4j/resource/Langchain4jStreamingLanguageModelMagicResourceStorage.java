package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** streaming-language-model 资源存储：folder=langchain4j-streaming-language-model。 */
public class Langchain4jStreamingLanguageModelMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jStreamingLanguageModelInfo> {

    public Langchain4jStreamingLanguageModelMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jStreamingLanguageModelMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL, Langchain4jStreamingLanguageModelInfo.class, providers);
    }
}
