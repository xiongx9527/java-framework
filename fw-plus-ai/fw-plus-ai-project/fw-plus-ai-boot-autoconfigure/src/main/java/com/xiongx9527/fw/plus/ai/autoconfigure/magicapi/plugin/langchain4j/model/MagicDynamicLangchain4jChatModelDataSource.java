package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.chat.ChatModel;

/** {@code langchain4j-chat-model} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jChatModelDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<ChatModel> {

    public MagicDynamicLangchain4jChatModelDataSource() {
        super(Langchain4jModelTypes.CHAT_MODEL);
    }
}
