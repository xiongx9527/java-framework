package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.scoring.ScoringModel;

/** {@code langchain4j-scoring-model} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jScoringModelDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<ScoringModel> {

    public MagicDynamicLangchain4jScoringModelDataSource() {
        super(Langchain4jModelTypes.SCORING_MODEL);
    }
}
