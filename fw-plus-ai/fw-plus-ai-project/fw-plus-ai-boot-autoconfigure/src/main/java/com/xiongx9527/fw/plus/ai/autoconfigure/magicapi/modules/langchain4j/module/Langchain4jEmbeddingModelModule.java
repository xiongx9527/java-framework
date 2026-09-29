package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 向量模型脚本模块：脚本用 {@code embeddingModel['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("embeddingModel")
public class Langchain4jEmbeddingModelModule extends AbstractLangchain4jModelModule<EmbeddingModel>
        implements DynamicAttribute<EmbeddingModel, Langchain4jEmbeddingModelModule>, DynamicModule<Langchain4jEmbeddingModelModule> {

    public Langchain4jEmbeddingModelModule(MagicDynamicLangchain4jEmbeddingModelDataSource holder) {
        super(holder, Langchain4jModelTypes.EMBEDDING_MODEL);
    }

    @Override
    public Langchain4jEmbeddingModelModule getDynamicModule(MagicScriptContext context) {
        return this;
    }

    @Override
    public Langchain4jEmbeddingModelModule setDynamicAttribute(String key, EmbeddingModel value) {
        throw new UnsupportedOperationException(
                "脚本不支持设置模型实例，请在控制台维护 langchain4j-embedding-model 数据源");
    }

    @Override
    public List<EmbeddingModel> defaultMappings() {
        return super.defaultMappings();
    }
}
