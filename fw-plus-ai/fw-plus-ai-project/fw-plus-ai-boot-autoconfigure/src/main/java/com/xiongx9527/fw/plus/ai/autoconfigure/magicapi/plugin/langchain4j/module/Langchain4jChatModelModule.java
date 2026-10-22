package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import dev.langchain4j.model.chat.ChatModel;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 对话模型脚本模块：脚本用 {@code chatModel['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("langchain4jChatModel")
public class Langchain4jChatModelModule extends AbstractLangchain4jModelModule<ChatModel>
        implements DynamicAttribute<ChatModel, Langchain4jChatModelModule>, DynamicModule<ChatModel> {

    public Langchain4jChatModelModule(MagicDynamicLangchain4jChatModelDataSource holder,
            ObjectProvider<ChatModel> defaultInstance) {
        super(holder, Langchain4jModelTypes.CHAT_MODEL, defaultInstance);
    }

    @Override
    public Langchain4jChatModelModule setDynamicAttribute(String key, ChatModel value) {
        throw new UnsupportedOperationException(I18nMessageResolver.resolveKey(
                "fw.ai.modelModule.scriptSetUnsupported", "langchain4j-chat-model"));
    }

    @Override
    public List<ChatModel> defaultMappings() {
        return super.defaultMappings();
    }
}
