package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({VertexAiGeminiChatModel.class, VertexAiGeminiStreamingChatModel.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.vertex-ai-gemini", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.vertexai.spring.AutoConfig.class)
public class LangChain4jVertexAiGeminiAutoConfiguration {
}
