package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({AzureOpenAiChatModel.class, AzureOpenAiStreamingChatModel.class, AzureOpenAiEmbeddingModel.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.azure-open-ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.azure.openai.spring.AutoConfig.class)
public class LangChain4jAzureOpenAiAutoConfiguration {
}
