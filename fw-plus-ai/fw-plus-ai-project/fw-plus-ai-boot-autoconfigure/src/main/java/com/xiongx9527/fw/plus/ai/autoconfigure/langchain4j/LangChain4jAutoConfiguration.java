package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name="dev.langchain4j.spring.LangChain4jAutoConfig")
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({LangChain4jCoreAutoConfiguration.class,
		LangChain4jOpenAiAutoConfiguration.class,
		LangChain4jAnthropicAutoConfiguration.class,
		LangChain4jOllamaAutoConfiguration.class,
		LangChain4jAzureAiSearchContentRetrieverAutoConfiguration.class,
		LangChain4jAzureOpenAiAutoConfiguration.class,
		LangChain4jVoyageAiAutoConfiguration.class,
		LangChain4jGitHubModelsAutoConfiguration.class,
		LangChain4jVertexAiGeminiAutoConfiguration.class,
		LangChain4jQianfanAutoConfiguration.class,
		LangChain4jDashScopeAutoConfiguration.class})
public class LangChain4jAutoConfiguration {
}
