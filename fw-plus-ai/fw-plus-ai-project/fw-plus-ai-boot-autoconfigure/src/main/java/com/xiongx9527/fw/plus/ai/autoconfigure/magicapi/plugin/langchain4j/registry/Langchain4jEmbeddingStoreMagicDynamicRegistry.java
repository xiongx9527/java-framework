package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** embedding-store 动态注册器：构建时按 embeddingModelKey 从 embedding-model 持有者取模型。 */
public class Langchain4jEmbeddingStoreMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jEmbeddingStoreInfo,
        EmbeddingStore<TextSegment>> {

    private final MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;

    public Langchain4jEmbeddingStoreMagicDynamicRegistry(Langchain4jEmbeddingStoreMagicResourceStorage storage,
            MagicDynamicLangchain4jEmbeddingStoreDataSource holder,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels,
            Langchain4jModelFactoryRegistry factories) {
        super(storage, holder, factories, Langchain4jModelTypes.EMBEDDING_STORE);
        this.embeddingModels = embeddingModels;
    }

    @Override
    protected EmbeddingStore<TextSegment> buildInstance(Langchain4jEmbeddingStoreInfo entity) {
        EmbeddingModel embeddingModel = requireEmbeddingModel(entity,
                embeddingModels.get(entity.getEmbeddingModelKey()));
        return factories.buildEmbeddingStore(entity, embeddingModel);
    }

    /**
     * embedding-store 必须引用一个已启用的 embedding-model：缺失即注册失败，并给出可操作的提示
     * （即使 options 已配置 dimension 也不放行，避免产生无法向量化的孤立 store）。
     */
    public static EmbeddingModel requireEmbeddingModel(Langchain4jEmbeddingStoreInfo entity,
            EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            throw new IllegalArgumentException(String.format(
                    "请先配置并启用 embedding-model: %s（embedding-store 依赖它提供向量能力与默认维度）",
                    entity.getEmbeddingModelKey()));
        }
        return embeddingModel;
    }

    @EventListener(condition = "#event.type == 'langchain4j-embedding-store'")
    public void onFileEvent(FileEvent event) {
        processEvent(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-embedding-store'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }
}
