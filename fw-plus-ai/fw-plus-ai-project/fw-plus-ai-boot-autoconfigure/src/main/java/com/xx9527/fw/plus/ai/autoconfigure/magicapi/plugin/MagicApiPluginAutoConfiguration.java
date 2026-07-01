package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin;

import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.starter.MagicAPIAiaConfiguration;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.starter.MagicAPIQdrantConfiguration;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.redisstack.starter.MagicAPIRedisStackConfiguration;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.starter.MagicAPIWeaviateConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MagicAPIAiaConfiguration.class,
        MagicAPIQdrantConfiguration.class,
        MagicAPIWeaviateConfiguration.class,
        MagicAPIRedisStackConfiguration.class
})
public class MagicApiPluginAutoConfiguration {
}
