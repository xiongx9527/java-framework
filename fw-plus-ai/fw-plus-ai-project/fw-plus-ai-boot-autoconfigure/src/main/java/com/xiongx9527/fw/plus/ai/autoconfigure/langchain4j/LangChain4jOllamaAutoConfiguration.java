package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.model.ollama.OllamaChatModel", "dev.langchain4j.model.ollama.OllamaStreamingChatModel", "dev.langchain4j.model.ollama.OllamaEmbeddingModel"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.ollama", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.ollama.spring.AutoConfig.class)
public class LangChain4jOllamaAutoConfiguration {
}
