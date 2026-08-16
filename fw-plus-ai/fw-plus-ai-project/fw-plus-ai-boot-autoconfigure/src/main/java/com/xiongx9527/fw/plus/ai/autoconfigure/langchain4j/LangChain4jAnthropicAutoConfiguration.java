package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.model.anthropic.AnthropicChatModel", "dev.langchain4j.model.anthropic.AnthropicStreamingChatModel"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.anthropic", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.anthropic.spring.AutoConfig.class)
public class LangChain4jAnthropicAutoConfiguration {
}
