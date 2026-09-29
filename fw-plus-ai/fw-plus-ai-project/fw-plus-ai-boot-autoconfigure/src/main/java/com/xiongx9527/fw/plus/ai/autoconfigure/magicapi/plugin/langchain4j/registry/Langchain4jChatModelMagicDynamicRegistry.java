package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import java.util.ArrayList;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import java.util.Collections;
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
            MagicDynamicLangchain4jChatModelDataSource holder, Langchain4jModelFactoryRegistry factories,
            ObjectProvider<ChatLanguageModel> defaultInstance) {
        super(storage, holder, factories, Langchain4jModelTypes.CHAT_MODEL, defaultInstance);
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

    /** 默认实例（Spring 注入，key=default）+ 控制台配置的 datasource，一并暴露给控制台与脚本 */
    @Override
    public List<Langchain4jChatModelInfo> defaultMappings() {
        List<Langchain4jChatModelInfo> result = new ArrayList<>();
        if (defaultInstance.getIfAvailable() != null) {
            Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
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
