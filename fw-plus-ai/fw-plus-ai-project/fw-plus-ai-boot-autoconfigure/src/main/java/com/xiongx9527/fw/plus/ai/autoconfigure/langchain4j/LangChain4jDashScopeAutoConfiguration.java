package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({QwenChatModel.class, QwenStreamingChatModel.class, QwenEmbeddingModel.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.dashscope", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.dashscope.spring.AutoConfig.class)
public class LangChain4jDashScopeAutoConfiguration {
}
