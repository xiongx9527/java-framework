package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

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
            Langchain4jModelFactoryRegistry factories) {
        super(storage, holder, factories, Langchain4jModelTypes.STREAMING_CHAT_MODEL);
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

    /** 本插件无静态默认数据源：实例均来自控制台保存的 datasource；保留 mongo 插件同构语义 */
    @Override
    public List<Langchain4jStreamingChatModelInfo> defaultMappings() {
        return Collections.emptyList();
    }
}