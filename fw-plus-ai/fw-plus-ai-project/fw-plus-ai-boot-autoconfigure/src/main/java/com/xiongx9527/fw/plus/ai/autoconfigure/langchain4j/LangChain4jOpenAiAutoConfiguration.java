package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({OpenAiChatModel.class, OpenAiStreamingChatModel.class, OpenAiEmbeddingModel.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.open-ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.openai.spring.AutoConfig.class)
public class LangChain4jOpenAiAutoConfiguration {
}
