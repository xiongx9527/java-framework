package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/** {@code langchain4j-streaming-chat-model} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jStreamingChatModelDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<StreamingChatLanguageModel> {

    public MagicDynamicLangchain4jStreamingChatModelDataSource() {
        super(Langchain4jModelTypes.STREAMING_CHAT_MODEL);
    }
}
