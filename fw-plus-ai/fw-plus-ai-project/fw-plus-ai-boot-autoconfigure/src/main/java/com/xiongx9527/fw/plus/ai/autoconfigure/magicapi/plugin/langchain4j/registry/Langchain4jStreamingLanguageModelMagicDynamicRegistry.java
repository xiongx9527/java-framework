package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingLanguageModelMagicResourceStorage;
import dev.langchain4j.model.language.StreamingLanguageModel;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** streaming-language-model 动态注册器。 */
public class Langchain4jStreamingLanguageModelMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jStreamingLanguageModelInfo,
        StreamingLanguageModel> {

    public Langchain4jStreamingLanguageModelMagicDynamicRegistry(
            Langchain4jStreamingLanguageModelMagicResourceStorage storage,
            MagicDynamicLangchain4jStreamingLanguageModelDataSource holder,
            Langchain4jModelFactoryRegistry factories) {
        super(storage, holder, factories, Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL);
    }

    @Override
    protected StreamingLanguageModel buildInstance(Langchain4jStreamingLanguageModelInfo entity) {
        return factories.buildStreamingLanguageModel(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-streaming-language-model'")
    public void onFileEvent(FileEvent event) {
        processEvent(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-streaming-language-model'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }
}
