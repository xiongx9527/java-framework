//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.service.spring;

import dev.langchain4j.internal.Utils;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import java.util.List;
import org.springframework.beans.factory.FactoryBean;

class AiServiceFactory implements FactoryBean<Object> {
    private final Class<Object> aiServiceClass;
    private ChatLanguageModel chatLanguageModel;
    private StreamingChatLanguageModel streamingChatLanguageModel;
    private ChatMemory chatMemory;
    private ChatMemoryProvider chatMemoryProvider;
    private ContentRetriever contentRetriever;
    private RetrievalAugmentor retrievalAugmentor;
    private List<Object> tools;

    public AiServiceFactory(Class<Object> aiServiceClass) {
        this.aiServiceClass = aiServiceClass;
    }

    public void setChatLanguageModel(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public void setStreamingChatLanguageModel(StreamingChatLanguageModel streamingChatLanguageModel) {
        this.streamingChatLanguageModel = streamingChatLanguageModel;
    }

    public void setChatMemory(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public void setChatMemoryProvider(ChatMemoryProvider chatMemoryProvider) {
        this.chatMemoryProvider = chatMemoryProvider;
    }

    public void setContentRetriever(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    public void setRetrievalAugmentor(RetrievalAugmentor retrievalAugmentor) {
        this.retrievalAugmentor = retrievalAugmentor;
    }

    public void setTools(List<Object> tools) {
        this.tools = tools;
    }

    public Object getObject() {
        AiServices<Object> builder = AiServices.builder(this.aiServiceClass);
        if (this.chatLanguageModel != null) {
            builder = builder.chatLanguageModel(this.chatLanguageModel);
        }

        if (this.streamingChatLanguageModel != null) {
            builder = builder.streamingChatLanguageModel(this.streamingChatLanguageModel);
        }

        if (this.chatMemory != null) {
            builder.chatMemory(this.chatMemory);
        }

        if (this.chatMemoryProvider != null) {
            builder.chatMemoryProvider(this.chatMemoryProvider);
        }

        if (this.retrievalAugmentor != null) {
            builder = builder.retrievalAugmentor(this.retrievalAugmentor);
        } else if (this.contentRetriever != null) {
            builder = builder.contentRetriever(this.contentRetriever);
        }

        if (!Utils.isNullOrEmpty(this.tools)) {
            builder = builder.tools(this.tools);
        }

        return builder.build();
    }

    public Class<?> getObjectType() {
        return this.aiServiceClass;
    }

    public boolean isSingleton() {
        return true;
    }
}
