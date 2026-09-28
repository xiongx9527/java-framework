package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jLanguageModelMagicResourceStorage;
import dev.langchain4j.model.language.LanguageModel;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** language-model 动态注册器。 */
public class Langchain4jLanguageModelMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jLanguageModelInfo, LanguageModel> {

    public Langchain4jLanguageModelMagicDynamicRegistry(Langchain4jLanguageModelMagicResourceStorage storage,
            MagicDynamicLangchain4jLanguageModelDataSource holder, Langchain4jModelFactoryRegistry factories) {
        super(storage, holder, factories, Langchain4jModelTypes.LANGUAGE_MODEL);
    }

    @Override
    protected LanguageModel buildInstance(Langchain4jLanguageModelInfo entity) {
        return factories.buildLanguageModel(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-language-model'")
    public void onFileEvent(FileEvent event) {
        processEvent(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-language-model'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }
}
