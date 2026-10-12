package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
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
        throw new IllegalArgumentException("未配置 dimension，且关联 embedding-model 无法提供维度："
            + "请在 datasource options 配置 dimension，或确保 embeddingModelKey 指向的 embedding-model 已启用");
    }
}
