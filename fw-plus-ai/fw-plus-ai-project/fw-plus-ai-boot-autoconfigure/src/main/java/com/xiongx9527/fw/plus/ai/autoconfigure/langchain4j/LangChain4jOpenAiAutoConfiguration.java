package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.model.openai.OpenAiChatModel", "dev.langchain4j.model.openai.OpenAiStreamingChatModel", "dev.langchain4j.model.openai.OpenAiEmbeddingModel"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.open-ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.openai.spring.AutoConfig.class)
public class LangChain4jOpenAiAutoConfiguration {
}
