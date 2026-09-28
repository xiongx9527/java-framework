package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jChatModelMagicResourceStorage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** chat-model 动态注册器：资源变更即重建 ChatLanguageModel 实例。 */
public class Langchain4jChatModelMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jChatModelInfo, ChatLanguageModel> {

    public Langchain4jChatModelMagicDynamicRegistry(Langchain4jChatModelMagicResourceStorage storage,
            MagicDynamicLangchain4jChatModelDataSource holder, Langchain4jModelFactoryRegistry factories) {
        super(storage, holder, factories, Langchain4jModelTypes.CHAT_MODEL);
    }

    @Override
    protected ChatLanguageModel buildInstance(Langchain4jChatModelInfo entity) {
        return factories.buildChatModel(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-chat-model'")
    public void onFileEvent(FileEvent event) {
        safelyProcess(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-chat-model'")
    public void onGroupEvent(GroupEvent event) {
        safelyProcess(event);
    }
}
