package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.context.annotation.Import;

@Import({LangChain4jCoreAutoConfiguration.class,
		LangChain4jOpenAiAutoConfiguration.class,
		LangChain4jAnthropicAutoConfiguration.class,
		LangChain4jOllamaAutoConfiguration.class,
		LangChain4jAzureAiSearchAutoConfiguration.class,
		LangChain4jAzureOpenAiAutoConfiguration.class,
		LangChain4jVoyageAiAutoConfiguration.class,
		LangChain4jGitHubModelsAutoConfiguration.class,
		LangChain4jVertexAiGeminiAutoConfiguration.class,
		LangChain4jElasticsearchEmbeddingStoreAutoConfiguration.class,
		LangChain4jRedisEmbeddingStoreAutoConfiguration.class,
		LangChain4jQianfanAutoConfiguration.class,
		LangChain4jMilvusEmbeddingStoreAutoConfiguration.class,
		LangChain4jDashScopeAutoConfiguration.class})
public class LangChain4jAutoConfiguration {
}
