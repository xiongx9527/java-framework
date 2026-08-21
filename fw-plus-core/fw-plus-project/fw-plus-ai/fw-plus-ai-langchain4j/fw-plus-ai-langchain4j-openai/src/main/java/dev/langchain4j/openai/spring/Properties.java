//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.openai.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(
    prefix = "langchain4j.open-ai"
)
public class Properties {
    static final String PREFIX = "langchain4j.open-ai";
    @NestedConfigurationProperty
    ChatModelProperties chatModel;
    @NestedConfigurationProperty
    ChatModelProperties streamingChatModel;
    @NestedConfigurationProperty
    LanguageModelProperties languageModel;
    @NestedConfigurationProperty
    LanguageModelProperties streamingLanguageModel;
    @NestedConfigurationProperty
    EmbeddingModelProperties embeddingModel;
    @NestedConfigurationProperty
    ModerationModelProperties moderationModel;
    @NestedConfigurationProperty
    ImageModelProperties imageModel;

    public ChatModelProperties getChatModel() {
        return this.chatModel;
    }

    public ChatModelProperties getStreamingChatModel() {
        return this.streamingChatModel;
    }

    public LanguageModelProperties getLanguageModel() {
        return this.languageModel;
    }

    public LanguageModelProperties getStreamingLanguageModel() {
        return this.streamingLanguageModel;
    }

    public EmbeddingModelProperties getEmbeddingModel() {
        return this.embeddingModel;
    }

    public ModerationModelProperties getModerationModel() {
        return this.moderationModel;
    }

    public ImageModelProperties getImageModel() {
        return this.imageModel;
    }

    public void setChatModel(ChatModelProperties chatModel) {
        this.chatModel = chatModel;
    }

    public void setStreamingChatModel(ChatModelProperties streamingChatModel) {
        this.streamingChatModel = streamingChatModel;
    }

    public void setLanguageModel(LanguageModelProperties languageModel) {
        this.languageModel = languageModel;
    }

    public void setStreamingLanguageModel(LanguageModelProperties streamingLanguageModel) {
        this.streamingLanguageModel = streamingLanguageModel;
    }

    public void setEmbeddingModel(EmbeddingModelProperties embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public void setModerationModel(ModerationModelProperties moderationModel) {
        this.moderationModel = moderationModel;
    }

    public void setImageModel(ImageModelProperties imageModel) {
        this.imageModel = imageModel;
    }
}
