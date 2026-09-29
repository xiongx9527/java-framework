package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import org.springframework.beans.factory.ObjectProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
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
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<StreamingLanguageModel> defaultInstance) {
        super(storage, holder, factories, Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL, defaultInstance);
    }

    @Override
    protected StreamingLanguageModel buildInstance(Langchain4jStreamingLanguageModelInfo entity) {
        return factories.buildStreamingLanguageModel(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-streaming-language-model'")
    public void onFileEvent(FileEvent event) {
        safelyProcess(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-streaming-language-model'")
    public void onGroupEvent(GroupEvent event) {
        safelyProcess(event);
    }

    /** 默认实例（Spring 注入，key=default）+ 控制台配置的 datasource，一并暴露给控制台与脚本 */
    @Override
    public List<Langchain4jStreamingLanguageModelInfo> defaultMappings() {
        List<Langchain4jStreamingLanguageModelInfo> result = new ArrayList<>();
        if (defaultInstance.getIfAvailable() != null) {
            Langchain4jStreamingLanguageModelInfo info = new Langchain4jStreamingLanguageModelInfo();
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
