package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = "dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore")
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.milvus", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.store.embedding.milvus.spring.MilvusEmbeddingStoreAutoConfiguration.class)
public class LangChain4jMilvusEmbeddingStoreAutoConfiguration {
}
