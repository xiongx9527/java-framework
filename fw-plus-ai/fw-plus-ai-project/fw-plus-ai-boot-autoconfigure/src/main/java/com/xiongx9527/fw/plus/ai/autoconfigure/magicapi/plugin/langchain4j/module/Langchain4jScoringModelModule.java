package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jScoringModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 重排模型脚本模块：脚本用 {@code scoringModel['key']} 取 ScoringModel 实例，
 * 常见用法是配合 ReRankingContentAggregator 对检索结果重排。
 */
@MagicModule("langchain4jScoringModel")
public class Langchain4jScoringModelModule extends AbstractLangchain4jModelModule<ScoringModel>
        implements DynamicAttribute<ScoringModel, Langchain4jScoringModelModule>, DynamicModule<ScoringModel> {

    public Langchain4jScoringModelModule(MagicDynamicLangchain4jScoringModelDataSource holder,
            ObjectProvider<ScoringModel> defaultInstance) {
        super(holder, Langchain4jModelTypes.SCORING_MODEL, defaultInstance);
    }

    @Override
    public Langchain4jScoringModelModule setDynamicAttribute(String key, ScoringModel value) {
        throw new UnsupportedOperationException(I18nMessageResolver.resolveKey(
                "fw.ai.modelModule.scriptSetUnsupported", "langchain4j-scoring-model"));
    }

    @Override
    public List<ScoringModel> defaultMappings() {
        return super.defaultMappings();
    }
}
