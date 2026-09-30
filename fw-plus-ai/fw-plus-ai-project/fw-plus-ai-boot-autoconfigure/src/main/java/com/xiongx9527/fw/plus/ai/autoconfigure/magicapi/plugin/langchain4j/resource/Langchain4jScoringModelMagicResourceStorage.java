package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jScoringModelInfo;

import java.util.Collections;
import java.util.Set;

/** scoring-model 资源存储：folder=langchain4j-scoring-model，每份 JSON 一个 ScoringModel。 */
public class Langchain4jScoringModelMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jScoringModelInfo> {

    public Langchain4jScoringModelMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jScoringModelMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.SCORING_MODEL, Langchain4jScoringModelInfo.class, providers);
    }
}
