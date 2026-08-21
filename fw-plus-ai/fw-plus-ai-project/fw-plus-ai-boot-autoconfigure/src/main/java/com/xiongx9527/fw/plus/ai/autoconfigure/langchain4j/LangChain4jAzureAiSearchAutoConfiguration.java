package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchContentRetriever", "dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.azure-ai-search", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.azure.aisearch.spring.AutoConfig.class)
public class LangChain4jAzureAiSearchAutoConfiguration {
}
