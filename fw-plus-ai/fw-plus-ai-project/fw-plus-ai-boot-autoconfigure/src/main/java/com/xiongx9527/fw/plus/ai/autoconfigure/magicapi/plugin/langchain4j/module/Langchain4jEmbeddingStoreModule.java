package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 向量存储脚本模块：脚本用 {@code embeddingStore['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("langchain4jEmbeddingStore")
public class Langchain4jEmbeddingStoreModule extends AbstractLangchain4jModelModule<EmbeddingStore<TextSegment>>
        implements DynamicAttribute<EmbeddingStore<TextSegment>, Langchain4jEmbeddingStoreModule>, DynamicModule<EmbeddingStore<TextSegment>> {

    public Langchain4jEmbeddingStoreModule(MagicDynamicLangchain4jEmbeddingStoreDataSource holder,
            ObjectProvider<EmbeddingStore<TextSegment>> defaultInstance) {
        super(holder, Langchain4jModelTypes.EMBEDDING_STORE, defaultInstance);
    }

    @Override
    public Langchain4jEmbeddingStoreModule setDynamicAttribute(String key, EmbeddingStore<TextSegment> value) {
        throw new UnsupportedOperationException(I18nMessageResolver.resolveKey(
                "fw.ai.modelModule.scriptSetUnsupported", "langchain4j-embedding-store"));
    }

    @Override
    public List<EmbeddingStore<TextSegment>> defaultMappings() {
        return super.defaultMappings();
    }
}
