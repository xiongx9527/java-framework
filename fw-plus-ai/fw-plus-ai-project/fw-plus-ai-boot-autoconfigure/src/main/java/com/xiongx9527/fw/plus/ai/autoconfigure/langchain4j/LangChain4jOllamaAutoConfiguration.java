package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({OllamaChatModel.class, OllamaStreamingChatModel.class, OllamaEmbeddingModel.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.ollama", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.ollama.spring.AutoConfig.class)
public class LangChain4jOllamaAutoConfiguration {
}
