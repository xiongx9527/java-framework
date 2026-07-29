package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.qianfan.QianfanChatModel;
import dev.langchain4j.model.qianfan.QianfanEmbeddingModel;
import dev.langchain4j.model.qianfan.QianfanStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({QianfanChatModel.class, QianfanStreamingChatModel.class, QianfanEmbeddingModel.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.qianfan", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.qianfan.spring.AutoConfig.class)
public class LangChain4jQianfanAutoConfiguration {
}
