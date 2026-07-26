package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration.class)
public class LangChain4jRedisEmbeddingStoreAutoConfiguration {
}
