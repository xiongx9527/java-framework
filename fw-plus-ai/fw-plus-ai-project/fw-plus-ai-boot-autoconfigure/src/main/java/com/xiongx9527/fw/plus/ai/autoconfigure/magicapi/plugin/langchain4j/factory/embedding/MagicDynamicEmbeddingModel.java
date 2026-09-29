package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.embedding;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

public class MagicDynamicEmbeddingModel implements EmbeddingModel {

    private final Langchain4jEmbeddingModelModule embeddingModel;

    private final String key;

    public MagicDynamicEmbeddingModel(Langchain4jEmbeddingModelModule embeddingModel, String key) {
        this.embeddingModel = embeddingModel;
        this.key = key;
    }

    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        try {
            return embeddingModel.getDynamicAttribute(key).embedAll(textSegments);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(EmbeddingModel.class).embedAll(textSegments);
            }
            return SpringUtils.getBean(key,EmbeddingModel.class).embedAll(textSegments);
        }
    }
}
