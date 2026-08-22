package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.model.azure.AzureOpenAiChatModel", "dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel", "dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.azure-open-ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.azure.openai.spring.AutoConfig.class)
public class LangChain4jAzureOpenAiAutoConfiguration {
}
