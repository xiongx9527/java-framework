package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import org.springframework.beans.factory.ObjectProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingChatModelMagicResourceStorage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** streaming-chat-model 动态注册器。 */
public class Langchain4jStreamingChatModelMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jStreamingChatModelInfo,
        StreamingChatLanguageModel> {

    public Langchain4jStreamingChatModelMagicDynamicRegistry(
            Langchain4jStreamingChatModelMagicResourceStorage storage,
            MagicDynamicLangchain4jStreamingChatModelDataSource holder,
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<StreamingChatLanguageModel> defaultInstance) {
        super(storage, holder, factories, Langchain4jModelTypes.STREAMING_CHAT_MODEL, defaultInstance);
    }

    @Override
    protected StreamingChatLanguageModel buildInstance(Langchain4jStreamingChatModelInfo entity) {
        return factories.buildStreamingChatModel(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-streaming-chat-model'")
    public void onFileEvent(FileEvent event) {
        safelyProcess(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-streaming-chat-model'")
    public void onGroupEvent(GroupEvent event) {
        safelyProcess(event);
    }

    /** 默认实例（Spring 注入，key=default）+ 控制台配置的 datasource，一并暴露给控制台与脚本 */
    @Override
    public List<Langchain4jStreamingChatModelInfo> defaultMappings() {
        List<Langchain4jStreamingChatModelInfo> result = new ArrayList<>();
        if (defaultInstance.getIfAvailable() != null) {
            Langchain4jStreamingChatModelInfo info = new Langchain4jStreamingChatModelInfo();
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
