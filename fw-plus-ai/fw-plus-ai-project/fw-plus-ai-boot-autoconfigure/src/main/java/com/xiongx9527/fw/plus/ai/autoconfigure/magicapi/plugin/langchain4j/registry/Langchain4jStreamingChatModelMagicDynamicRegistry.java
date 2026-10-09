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
import dev.langchain4j.model.chat.StreamingChatModel;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** streaming-chat-model 动态注册器。 */
public class Langchain4jStreamingChatModelMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jStreamingChatModelInfo,
        StreamingChatModel> {

    public Langchain4jStreamingChatModelMagicDynamicRegistry(
            Langchain4jStreamingChatModelMagicResourceStorage storage,
            MagicDynamicLangchain4jStreamingChatModelDataSource holder,
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<StreamingChatModel> defaultInstance) {
        super(storage, holder, factories, Langchain4jModelTypes.STREAMING_CHAT_MODEL, defaultInstance);
    }

    @Override
    protected StreamingChatModel buildInstance(Langchain4jStreamingChatModelInfo entity) {
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

    /** 仅返回无文件的 Spring 默认实例（key=default，只读展示）。控制台文件条目由控制器 listFiles 提供，这里不能再叠加 mappings()（会重复，且核心按 id/mappingKey 双键登记导致翻倍） */
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
        return result;
    }

    @Override
    protected String defaultProvider() {
        return "openai";
    }
}
