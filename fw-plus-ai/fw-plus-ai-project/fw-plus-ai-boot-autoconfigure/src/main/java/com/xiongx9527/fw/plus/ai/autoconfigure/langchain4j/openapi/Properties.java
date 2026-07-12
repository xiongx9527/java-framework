package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.openapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
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
}
