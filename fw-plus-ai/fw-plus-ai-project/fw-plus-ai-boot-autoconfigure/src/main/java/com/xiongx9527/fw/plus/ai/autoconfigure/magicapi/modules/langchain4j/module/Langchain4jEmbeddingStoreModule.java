package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 向量存储脚本模块：脚本用 {@code embeddingStore['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("embeddingStore")
public class Langchain4jEmbeddingStoreModule extends AbstractLangchain4jModelModule<EmbeddingStore<TextSegment>>
        implements DynamicAttribute<EmbeddingStore<TextSegment>, Langchain4jEmbeddingStoreModule>, DynamicModule<Langchain4jEmbeddingStoreModule> {

    public Langchain4jEmbeddingStoreModule(MagicDynamicLangchain4jEmbeddingStoreDataSource holder) {
        super(holder, Langchain4jModelTypes.EMBEDDING_STORE);
    }

    @Override
    public Langchain4jEmbeddingStoreModule getDynamicModule(MagicScriptContext context) {
        return this;
    }

    @Override
    public Langchain4jEmbeddingStoreModule setDynamicAttribute(String key, EmbeddingStore<TextSegment> value) {
        throw new UnsupportedOperationException(
                "脚本不支持设置模型实例，请在控制台维护 langchain4j-embedding-store 数据源");
    }

    @Override
    public List<EmbeddingStore<TextSegment>> defaultMappings() {
        return super.defaultMappings();
    }
}
