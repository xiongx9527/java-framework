package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** streaming-chat-model 资源存储：folder=langchain4j-streaming-chat-model。 */
public class Langchain4jStreamingChatModelMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jStreamingChatModelInfo> {

    public Langchain4jStreamingChatModelMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jStreamingChatModelMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.STREAMING_CHAT_MODEL, Langchain4jStreamingChatModelInfo.class, providers);
    }
}
