package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.starter.MagicAPIAiaConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.starter.MagicAPIQdrantConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.redisstack.starter.MagicAPIRedisStackConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.starter.MagicAPIVectorConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.starter.MagicAPIWeaviateConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureAfter(name = "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration")
@Import({MagicAPIAiaConfiguration.class,
        MagicAPIVectorConfiguration.class,
        MagicAPIQdrantConfiguration.class,
        MagicAPIWeaviateConfiguration.class,
        MagicAPIRedisStackConfiguration.class
})
public class MagicApiPluginAutoConfiguration {
}
