package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import java.util.ArrayList;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import java.util.Collections;
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
            MagicDynamicLangchain4jEmbeddingModelDataSource holder, Langchain4jModelFactoryRegistry factories,
            ObjectProvider<EmbeddingModel> defaultInstance) {
        super(storage, holder, factories, Langchain4jModelTypes.EMBEDDING_MODEL, defaultInstance);
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

    /** 默认实例（Spring 注入，key=default）+ 控制台配置的 datasource，一并暴露给控制台与脚本 */
    @Override
    public List<Langchain4jEmbeddingModelInfo> defaultMappings() {
        List<Langchain4jEmbeddingModelInfo> result = new ArrayList<>();
        if (defaultInstance.getIfAvailable() != null) {
            Langchain4jEmbeddingModelInfo info = new Langchain4jEmbeddingModelInfo();
            info.setKey(AbstractLangchain4jModelMagicDynamicRegistry.DEFAULT_KEY);
            info.setName("默认数据源");
            info.setProvider(defaultProvider());
            info.setEnabled(true);
            result.add(info);
        }
        result.addAll(mappings());
        return result;
    }

    @Override
    protected String defaultProvider() {
        return "openai";
    }
}
