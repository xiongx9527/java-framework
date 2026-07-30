package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.adapter", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {
		"com.xiongx9527.fw.plus.ai.langchain4j.adapter.DefaultLangChain4jModelFactory",
		"com.xiongx9527.fw.plus.ai.langchain4j.adapter.DefaultEmbeddingStoreFactory",
		"dev.langchain4j.store.embedding.EmbeddingStore"
})
@Import({LangChain4jModelAdapterAutoConfiguration.class,
		LangChain4jEmbeddingStoreAdapterAutoConfiguration.class})
public class LangChain4jAdapterAutoConfiguration {
}
