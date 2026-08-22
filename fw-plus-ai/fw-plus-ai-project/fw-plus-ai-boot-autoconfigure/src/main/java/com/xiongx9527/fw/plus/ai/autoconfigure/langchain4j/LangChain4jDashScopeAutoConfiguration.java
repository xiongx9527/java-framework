package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.community.model.dashscope.QwenChatModel", "dev.langchain4j.community.model.dashscope.QwenStreamingChatModel", "dev.langchain4j.community.model.dashscope.QwenEmbeddingModel"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.dashscope", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.dashscope.spring.AutoConfig.class)
public class LangChain4jDashScopeAutoConfiguration {
}
