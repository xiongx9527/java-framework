package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@ConditionalOnClass(name="dev.langchain4j.Experimental")
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jSupportAutoConfiguration {

    @Bean
    public ChatContextLogger chatContextLogger() {
        return new ChatContextLogger();
    }


}
