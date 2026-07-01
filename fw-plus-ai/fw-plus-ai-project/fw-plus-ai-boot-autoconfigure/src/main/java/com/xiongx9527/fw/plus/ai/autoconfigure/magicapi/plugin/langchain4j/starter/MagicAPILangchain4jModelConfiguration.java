package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@ConditionalOnClass(name = {"dev.langchain4j.model.chat.ChatLanguageModel",
        "dev.langchain4j.model.chat.StreamingChatLanguageModel",
        "dev.langchain4j.model.embedding.EmbeddingModel",
        "dev.langchain4j.model.image.ImageModel",
        "dev.langchain4j.model.language.LanguageModel",
        "dev.langchain4j.model.language.StreamingLanguageModel"})
@Configuration
public class MagicAPILangchain4jModelConfiguration {

    /**
     * 获取LangChain4j模型信息Bean
     *
     * @return LangChain4jModelInfo对象实例
     */
    @Bean
    public LangChain4jModelInfo getLangChain4jModelInfo() {
        return new LangChain4jModelInfo();
    }


}
