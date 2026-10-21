package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.embedding.EmbeddingModel;

/** dimension 决策链：options 配置值优先 → 关联 EmbeddingModel.dimension() → 抛中文异常。 */
final class EmbeddingStoreDimensions {

    private EmbeddingStoreDimensions() {
    }

    static Integer resolve(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer configured = o.integer("dimension", null);
        if (configured != null && configured != 0) {
            return configured;
        }
        if (embeddingModel != null) {
            return embeddingModel.dimension();
        }
        throw new IllegalArgumentException(
            I18nMessageResolver.resolveKey("fw.ai.EmbeddingStoreDimensions.dimensionMissing"));
    }
}
