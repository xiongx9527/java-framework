package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.community.model.qianfan.QianfanChatModel", "dev.langchain4j.community.model.qianfan.QianfanStreamingChatModel", "dev.langchain4j.community.model.qianfan.QianfanEmbeddingModel"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.qianfan", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.qianfan.spring.AutoConfig.class)
public class LangChain4jQianfanAutoConfiguration {
}
