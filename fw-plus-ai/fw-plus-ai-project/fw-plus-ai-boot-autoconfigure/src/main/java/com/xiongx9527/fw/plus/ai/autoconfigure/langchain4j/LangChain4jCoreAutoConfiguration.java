package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.spring.AiService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({ChatLanguageModel.class, StreamingChatLanguageModel.class,AiService.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.core", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.spring.LangChain4jAutoConfig.class)
public class LangChain4jCoreAutoConfiguration {

}
