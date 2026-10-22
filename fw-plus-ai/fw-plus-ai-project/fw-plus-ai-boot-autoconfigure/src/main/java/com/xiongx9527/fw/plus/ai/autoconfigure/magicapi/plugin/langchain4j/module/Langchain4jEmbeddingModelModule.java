package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 向量模型脚本模块：脚本用 {@code embeddingModel['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("langchain4jEmbeddingModel")
public class Langchain4jEmbeddingModelModule extends AbstractLangchain4jModelModule<EmbeddingModel>
        implements DynamicAttribute<EmbeddingModel, Langchain4jEmbeddingModelModule>, DynamicModule<EmbeddingModel> {

    public Langchain4jEmbeddingModelModule(MagicDynamicLangchain4jEmbeddingModelDataSource holder,
            ObjectProvider<EmbeddingModel> defaultInstance) {
        super(holder, Langchain4jModelTypes.EMBEDDING_MODEL, defaultInstance);
    }

    @Override
    public Langchain4jEmbeddingModelModule setDynamicAttribute(String key, EmbeddingModel value) {
        throw new UnsupportedOperationException(I18nMessageResolver.resolveKey(
                "fw.ai.modelModule.scriptSetUnsupported", "langchain4j-embedding-model"));
    }

    @Override
    public List<EmbeddingModel> defaultMappings() {
        return super.defaultMappings();
    }
}
