package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingModelMagicResourceStorage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** embedding-model 动态注册器。 */
public class Langchain4jEmbeddingModelMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jEmbeddingModelInfo, EmbeddingModel> {

    public Langchain4jEmbeddingModelMagicDynamicRegistry(Langchain4jEmbeddingModelMagicResourceStorage storage,
            MagicDynamicLangchain4jEmbeddingModelDataSource holder, Langchain4jModelFactoryRegistry factories) {
        super(storage, holder, factories, Langchain4jModelTypes.EMBEDDING_MODEL);
    }

    @Override
    protected EmbeddingModel buildInstance(Langchain4jEmbeddingModelInfo entity) {
        return factories.buildEmbeddingModel(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-embedding-model'")
    public void onFileEvent(FileEvent event) {
        safelyProcess(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-embedding-model'")
    public void onGroupEvent(GroupEvent event) {
        safelyProcess(event);
    }
}
