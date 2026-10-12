package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** chat-model 资源存储：folder=langchain4j-chat-model，每份 JSON 一个 ChatModel。 */
public class Langchain4jChatModelMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jChatModelInfo> {

    public Langchain4jChatModelMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jChatModelMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.CHAT_MODEL, Langchain4jChatModelInfo.class, providers);
    }
}
